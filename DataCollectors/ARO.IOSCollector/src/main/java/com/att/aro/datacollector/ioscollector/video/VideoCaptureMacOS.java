/*
 *  Copyright 2017 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.datacollector.ioscollector.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.android.ddmlib.IDevice;
import com.att.aro.core.datacollector.IVideoImageSubscriber;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.core.video.IVideoCapture;
import com.att.aro.core.video.IVideoWriter;
import com.att.aro.core.video.impl.VideoWriterImpl;
import com.att.aro.core.video.pojo.QuickTimeOutputStream;
import com.att.aro.core.video.pojo.QuickTimeOutputStream.VideoFormat;
import com.att.aro.datacollector.ioscollector.IScreenCapture;
import com.att.aro.datacollector.ioscollector.ImageSubscriber;
import com.att.aro.datacollector.ioscollector.utilities.ErrorCodeRegistry;

public class VideoCaptureMacOS extends Thread implements IVideoCapture {
	private static final Logger LOG = LogManager.getLogger(VideoCaptureMacOS.class);
	private List<ImageSubscriber> subscribers;
	private List<IVideoImageSubscriber> vImageSubscribers = new ArrayList<IVideoImageSubscriber>();
	private static ResourceBundle defaultBundle = ResourceBundle.getBundle("messages");

	private IVideoWriter videowriter = new VideoWriterImpl();

	private volatile boolean stop = false;
	private volatile boolean hasQuit = false;

	private String workingFolder = "";
	private StatusResult statusResult = null;
	private Date videoStartTime;

	IScreenCapture capt = null;
	ScreenshotManager smanage = null;

	int videoWidth = 0;
	int videoHeight = 0;
	private String udid = "";

	public VideoCaptureMacOS(File file, String udid) throws IOException {
		subscribers = new ArrayList<ImageSubscriber>();
		((VideoWriterImpl) this.videowriter).setFileManager(new FileManagerImpl());
		this.videowriter.init(file.getAbsolutePath(), VideoFormat.JPG, 0.2f, 10);
		this.udid  = udid;

		//		qos = new QuickTimeOutputStream(file,
		//				QuickTimeOutputStream.VideoFormat.JPG);
		//		qos.setVideoCompressionQuality(0.2f); // orig 1f
		//		qos.setTimeScale(10);

	}

	//for use in unit test
	public VideoCaptureMacOS(QuickTimeOutputStream qt, IScreenCapture screencapture) {
		subscribers = new ArrayList<ImageSubscriber>();
		this.capt = screencapture;
	}

	public void setWorkingFolder(String folder) {
		this.workingFolder = folder;
		LOG.info("set working folder: " + this.workingFolder);
	}

	/**
	 * Asynchronous operation will execute doWork() in the background
	 */
	public void run() {
		doWork();
	}

	/**
	 * Synchronous operation that will do the heavy work of capturing screenshot
	 * and compose video. This method should never be called directly, use run()
	 * instead. (created for junit test)
	 */
	public void doWork() {
		stop = false;
		hasQuit = false;
		LOG.info("Init Screencapture...");
		LOG.info("workingfolder :"+this.workingFolder);

		smanage = new ScreenshotManager(this.workingFolder, this.udid);
		smanage.start();
		LOG.info("started ScreenshotManager.");
		int timeoutcounter = 0;
		while (!smanage.isReady()) {
			try {
				LOG.info("waiting for ScreenshotManager to be ready");
				Thread.sleep(200);
				timeoutcounter++;
			} catch (InterruptedException e) {
				LOG.debug("InterruptedException:", e);
			}
			if (timeoutcounter > 30) {//give it 6 seconds to start up
				LOG.info("Timeout on screenshotmanager");
				break;
			}
		}
		LOG.info("ScreenshotManager is ready: " + smanage.isReady());

		Date lastFrameTime = this.videoStartTime = new Date();
		while (!stop) {

			try {
				BufferedImage image = smanage.getImage();// ImageHelper.getImageFromByte(data);
				if (image != null) {

					Date timestamp = new Date();
					int duration = Math.round((float) (timestamp.getTime() - lastFrameTime.getTime()) * videowriter.getTimeUnits() / 1000f);

					if (duration > 0) {
						videowriter.writeFrame(image, duration);
						lastFrameTime = timestamp;
						callSubscriber(image);
					}
				} else if (!stop && (!ImageHelper.isImageDecoderStatus())) {
					stopCapture();
					statusResult = new StatusResult();
					statusResult.setSuccess(false);
					statusResult.setError(
							ErrorCodeRegistry.getImageDecoderError(defaultBundle.getString("Error.imagedecoder")));
					LOG.error("Failed to get screenshot image, ImageDecoder error");
					break;
				}else if (!stop) {
					LOG.info("Failed to get screenshot image, pause for 1/2 second");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						LOG.debug("InterruptedException:", e);
					}
				}
			} catch (IOException e) {
				LOG.debug("IOException:", e);
				break;
			}

		}
		try {
			videowriter.close();
			if (smanage != null) {
				smanage.shutDown();
			}
		} catch (IOException ioExp) {
			LOG.warn("Exception closing video output stream", ioExp);
		}
		hasQuit = true;
		stop = false;//signal waiter to stop waiting
		LOG.info("stopped screencapture");

	}

	public void signalStop() {
		stop = true;
		LOG.info("signal video capture to stop. no waiting for now");
	}

	public void stopCapture() {
		if (!hasQuit) {//in case video is already stopped
			stop = true;
			LOG.info("sent signal to stop long running task and now wait");
			int waitcount = 0;
			while (stop) {//run() should reset it to false before it quit
				try {
					LOG.info("Waiting for videocapture to stop, counter: " + waitcount);
					Thread.sleep(100);
					waitcount++;
					if (waitcount > 20) {
						LOG.info("Timeout on wait, force exit on counter: " + waitcount);
						break;
					}
				} catch (InterruptedException e) {
					LOG.debug("InterruptedException:", e);
					break;
				}
			}
		} else {
			LOG.info("capture engine already quit, proceed to next step");
		}
		if (capt != null) {
			try {
				capt.stopCapture();
			} catch (UnsatisfiedLinkError er) {
			}
			capt = null;
			LOG.info("disposed screencapture");
		}
		if (smanage != null) {
			try {
				smanage.signalShutdown();
			} catch (IOException e) {
				LOG.debug("IOException:", e);
			}
			smanage = null;
		}
		//properly close video creator
		try {
			videowriter.close();
		} catch (IOException ioExp) {
			LOG.warn("Exception closing video output stream", ioExp);
		}
		LOG.info("finished video capture");
		System.gc();
	}

	/**
	 * passing image to subscribers
	 * 
	 * @param image
	 */
	private void callSubscriber(final BufferedImage image) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				for (ImageSubscriber sub : subscribers) {
					sub.receiveImage(image);
				}
				for (IVideoImageSubscriber newSub:vImageSubscribers){
					newSub.receiveImage(image);
				}
			}
		});
	}

	public void addSubscriber(ImageSubscriber sub) {
		this.subscribers.add(sub);
	}
	
	@Override
	public void addSubscriber(IVideoImageSubscriber vImageSubscriber) {
		vImageSubscribers.add(vImageSubscriber);
	}

	/**
	 * Finalizes the VideoCaptureThread object. This method overrides the
	 * java.lang.Object.Finalize method.
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		try {
			videowriter.close();
		} catch (IOException ioExp) {
			LOG.warn("Exception closing video output stream", ioExp);
		}
	}

	/**
	 * Gets the start time of the video capture.
	 * 
	 * @return The start time of the video.
	 */
	public Date getVideoStartTime() {
		return videoStartTime;
	}

	@Override
	public void init(IDevice device, String videoOutputFile) throws IOException {
		
	}

	@Override
	public void setDeviceManufacturer(String deviceManufacturer) {
		
	}

	@Override
	public void stopRecording() {
		
	}

	@Override
	public boolean isVideoCaptureActive() {
		return false;
	}

	public StatusResult getStatusResult() {
		return statusResult;
	}
}