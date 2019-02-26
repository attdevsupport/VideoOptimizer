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
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.core.util.Util;
import com.att.aro.datacollector.ioscollector.IScreenshotPubSub;
import com.att.aro.datacollector.ioscollector.utilities.Tiff2JpgUtil;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;

public class ScreenshotManager extends Thread implements IScreenshotPubSub {
	// private static final Logger logger =
	// LogManager.getLogger(ScreenshotManager.class.getName());

	private static final Logger LOG = LogManager.getLogger(ScreenshotManager.class.getName());
	Process proc = null;
	String lastmessage = "";
	int counter = 0;
	int count = 0;
	String imagefolder = "";
	String udid = "";
	boolean isready = true;
	volatile boolean isReadyForRead = false;

	File tmpfolder;

	public void setIsReady(boolean isReady) {
		isready = isReady;
	}

	public ScreenshotManager(String folder, String udid) {
		imagefolder = folder + Util.FILE_SEPARATOR + "tmp";
		tmpfolder = new File(imagefolder);
		this.udid= udid;
		if (!tmpfolder.exists()) {
			LOG.debug("tmpfolder.mkdirs()" + imagefolder);
			tmpfolder.mkdirs();
			LOG.debug("exists :" + tmpfolder.exists());
		}
	}

	@Override
	public void run() {
		String exepath = Util.getIdeviceScreenshot();
		File exefile = new File(exepath);
		if (!exefile.exists()) {
			LOG.info("Not found exepath: " + exepath);
		}

		
		while (isready) {
			String img = this.imagefolder + Util.FILE_SEPARATOR + "image" + count + ".tiff";
			File file = new File(imagefolder);
			if(!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					LOG.error("Failed to create image folder", e);
				}
			}
			if(StringUtils.isNotBlank(udid) && !exepath.contains("-u")){
				exepath = exepath + " -u "+ udid; 
			}
			String[] cmds = new String[] { "bash", "-c", exepath + " " + img };
			ProcessBuilder builder = new ProcessBuilder(cmds);
			builder.redirectErrorStream(true);

			try {
				proc = builder.start();
			} catch (IOException e) {
				LOG.error("Error starting idevicescreenshot:", e);
				return;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			File imgFile = new File(img);
			if (imgFile.exists() || this.lastmessage.contains("Connect success")) {
				LOG.debug("Connect success");
				isready = true;
				isReadyForRead = true;
				count++;
			}
		}
	}

	public boolean isReady() {
		return isready;
	}

	public BufferedImage getImage() {
		BufferedImage imgdata = null;
		File imgfile = null;
		String img = this.imagefolder + Util.FILE_SEPARATOR + "image" + counter + ".tiff";
		int tempCounter = counter;
		
		try {
			imgfile = new File(img);
			if (imgfile.exists() && isReadyForRead) {
				
				FileInputStream inputstream = new FileInputStream(imgfile);
				byte[] imgdataarray = new byte[(int) imgfile.length()];
				inputstream.read(imgdataarray);
				inputstream.close();

				imgdata = ImageHelper.getImageFromByte(imgdataarray);
				ByteArrayOutputStream byteArrayOutputStream = Tiff2JpgUtil.tiff2Jpg(imgfile.getAbsolutePath());
				imgdataarray = byteArrayOutputStream.toByteArray();
				imgdata = getImageFromByte(imgdataarray);
				
				counter++;
				// Making sure that the Live Screen Thread is at least one behind the Screenshot Capture.
				if(counter >= count-1) {
					isReadyForRead = false;
				}
				
				try {
					imgfile.delete();
				} catch (Exception e) {
					LOG.error("Error deleting image file:" + img, e);
				}
			}
		} catch (IOException ioe) {
			LOG.error("Error reading image file:" + img, ioe);
			imgdata = null;
			
			if(ExceptionUtils.indexOfThrowable(ioe, NullPointerException.class) != -1) {
				counter = tempCounter;
			}
		}

		return imgdata;
	}

	/**
	 * convert byte array of image to BufferedImge
	 * 
	 * @param array
	 *            data of image
	 * @return new instance of BufferedImage
	 * @throws IOException
	 */
	public static BufferedImage getImageFromByte(byte[] array) throws IOException {
		InputStream instream = new ByteArrayInputStream(array);
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
		isready = false;
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
}// end class
