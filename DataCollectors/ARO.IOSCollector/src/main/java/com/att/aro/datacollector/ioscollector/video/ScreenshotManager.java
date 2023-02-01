/*
 *  Copyright 2017, 2022 AT&T
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
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.core.util.Util;
import com.att.aro.datacollector.ioscollector.IScreenshotPubSub;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;

public class ScreenshotManager extends Thread implements IScreenshotPubSub {
	
	private static final Logger LOG = LogManager.getLogger(ScreenshotManager.class.getName());
	
	IExternalProcessRunner extRunner = new ExternalProcessRunnerImpl();
	Process proc = null;
	String lastmessage = "";
	
	int imageRetrieveCounter = 0;	// counter for images processed (and deleted)
	int newImageCounter = 0;		// counter for images taken
	String imagefolder = "";
	String udid = "";
	boolean screenShotActive = true;
	volatile boolean isReadyForRead = false;

	File tmpfolder;

	private String exeIdeviceScreenShot;
	private String exten = ".png";

	public void setIsReady(boolean isReady) {
		screenShotActive = isReady;
	}

	public ScreenshotManager(String folder, String udid) {
		
		imagefolder = folder + Util.FILE_SEPARATOR + "tmp";
		tmpfolder = new File(imagefolder);
		this.udid = udid;
		if (!tmpfolder.exists()) {
			LOG.debug("tmpfolder.mkdirs()" + imagefolder);
			tmpfolder.mkdirs();
			LOG.debug("exists :" + tmpfolder.exists());
		}
		
		checkScreenshot();
	}

	public boolean checkScreenshot() {

		screenShotActive = false;
		exeIdeviceScreenShot = Util.getIdeviceScreenshot();
		if (!new File(exeIdeviceScreenShot).exists()) {
			String spath = extRunner.executeCmd("which " + exeIdeviceScreenShot);
			if (spath.startsWith("/")) {
				exeIdeviceScreenShot = spath.trim();
			}
		}

		File imgfile = new File(imagefolder, "imageTest");

		String result = extRunner.executeCmd(String.format("%s -u %s %s", exeIdeviceScreenShot, udid, imgfile.toString()));

		if (!result.contains("screenshotr")) {
			File screenshotTest = new File(result.trim().substring(result.indexOf(imagefolder)));
			if (screenshotTest.exists()) {
				// Will remember extention used by (Xcode & device)
				String temp = Util.getExtension(screenshotTest.toString());
				if (temp != null) {
					exten = "." + temp;
				}
				FileInputStream inputstream;
				try {
					inputstream = new FileInputStream(screenshotTest);
					byte[] imagedata = new byte[(int) screenshotTest.length()];
					inputstream.read(imagedata);
					inputstream.close();

					BufferedImage image = ImageHelper.convertToBufferedImage(imagedata);
					screenShotActive = (image != null);
					screenshotTest.delete();
				} catch (Exception e) {
					LOG.error("Failed to obtain screenshot:" + e.getMessage());
					screenShotActive = false;
				}
			}
		}
		return screenShotActive;
	}

	@Override
	public void run() {
		String screenshotResponse;

		if (StringUtils.isNotBlank(udid) && !exeIdeviceScreenShot.contains("-u")) {
			exeIdeviceScreenShot += " -u " + udid;
		}

		// while loop to capture screen shots into folder
		while (screenShotActive) {
			String screenShotImage = this.imagefolder + Util.FILE_SEPARATOR + "image" + newImageCounter + exten;
			File file = new File(imagefolder);
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					LOG.error("Failed to create image folder", e);
				}
			}

			screenshotResponse = extRunner.executeCmd(exeIdeviceScreenShot + " " + screenShotImage);
			if (!screenshotResponse.isEmpty() && !screenshotResponse.contains("Screenshot saved to")) {
				screenShotActive = false;
				isReadyForRead = false;
				LOG.error("iOS screenshot failure: " + screenshotResponse);
				shutDown();
				break;
			}
			File imgFile = new File(screenShotImage);
			if (imgFile.exists() || this.lastmessage.contains("Connect success")) {
				LOG.debug("Connect success");
				screenShotActive = true;
				isReadyForRead = true;
				newImageCounter++;
			}
		}
	}

	public boolean isReady() {
		return screenShotActive;
	}

	/**
	 * 
	 * @return
	 */
	public BufferedImage getImage() {
		BufferedImage bufferedImage = null;
		File screenShotFile = null;
		String screenShotName = this.imagefolder + Util.FILE_SEPARATOR + "image" + imageRetrieveCounter + exten ;
		int tempCounter = imageRetrieveCounter;

		while (screenShotActive && !isReadyForRead) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				LOG.debug("InterruptedException:", e);
			}
		}
		try {
			screenShotFile = new File(screenShotName);
			if (screenShotFile.exists() && isReadyForRead) {
				
				FileInputStream inputstream = new FileInputStream(screenShotFile);
				byte[] imagedata = new byte[(int) screenShotFile.length()];
				inputstream.read(imagedata);
				inputstream.close();
				bufferedImage = ImageHelper.convertToBufferedImage(imagedata);
				
				imageRetrieveCounter++;
				// Making sure that the Live Screen Thread is at least one behind the Screenshot Capture.
				if (imageRetrieveCounter >= newImageCounter - 1) {
					isReadyForRead = false;
				}

				try {
					screenShotFile.delete();
				} catch (Exception e) {
					LOG.error("Error deleting image file:" + screenShotName, e);
				}
			}
		} catch (IOException ioe) {
			LOG.error("Error reading image file:" + screenShotName, ioe);

			if (ExceptionUtils.indexOfThrowable(ioe, NullPointerException.class) != -1) {
				imageRetrieveCounter = tempCounter;
			}
		}

		return bufferedImage;
	}

	/**
	 * convert byte array of image to BufferedImge
	 * 
	 * @param byteArray data of image
	 * @return new instance of BufferedImage
	 * @throws IOException
	 */
	public static BufferedImage getImageFromByte(byte[] byteArray) throws IOException {
		InputStream instream = new java.io.ByteArrayInputStream(byteArray);
		ImageDecoder dec = ImageCodec.createImageDecoder("jpeg", instream, null);
		RenderedImage rendering = new NullOpImage(dec.decodeAsRenderedImage(0), null, null, OpImage.OP_IO_BOUND);
		BufferedImage image = convertRenderedImage(rendering);
		return image;
	}

	public static BufferedImage convertRenderedImage(RenderedImage img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}
		ColorModel cm = img.getColorModel();
		int width = img.getWidth();
		int height = img.getHeight();
		WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		String[] keys = img.getPropertyNames();
		if (keys != null) {
			for (int i = 0; i < keys.length; i++) {
				properties.put(keys[i], img.getProperty(keys[i]));
			}
		}
		BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
		img.copyData(raster);
		return result;
	}

	/**
	 * sending shutdown signal to the child process which then callback to
	 * ScreenshotManager shutDown() function, which finally self-destroy
	 * 
	 * @throws IOException
	 */
	public void signalShutdown() throws IOException {

	}

	/**
	 * stop everything and exit
	 */
	public void shutDown() {
		screenShotActive = false;
		if (proc != null) {
			proc.destroy();
			proc = null;
		}
		// delete left-over image file in this tmp dir
		File[] files = tmpfolder.listFiles();
		if (files != null && files.length > 0) {
			for (File file : files) {
				file.delete();
			}
			LOG.info("deleted left-over image files.");
		}
		tmpfolder.delete();
		LOG.info("ScreenshotManager.shutDown() finished");
	}

	@Override
	public void newMessage(String message) {
		// new message from screenshot reader
		// log.info(message);
		this.lastmessage = message.trim();
		// log.debug("RECVD:" + lastmessage);
	}

	@Override
	public void willExit() {
		LOG.info("willExit() called");
		// screenshot service will exit now
		this.shutDown();

	}
	
	
	public boolean isReadyForRead() {
		return isReadyForRead;
	}

	public void setReadyForRead(boolean isReadyForRead) {
		this.isReadyForRead = isReadyForRead;
	}
}
