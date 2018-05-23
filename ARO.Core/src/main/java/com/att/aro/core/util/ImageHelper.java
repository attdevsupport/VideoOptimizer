/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;

import com.android.ddmlib.RawImage;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.video.pojo.QuickTimeOutputStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codecimpl.util.ImagingException;

/**
 * Helper class for image rotation, conversion and resizing.
 *
 * Date: March 28, 2014
 *
 */
public class ImageHelper {

	public static boolean imageDecoderStatus = true;
	private final static String PNG_IMAGE_HEADER = "89 50 4E 47 0D 0A 1A 0A";

	/**
	 * Converts raw image in to buffered image object which will be provided to
	 * quickstream for creating video {@link QuickTimeOutputStream}
	 * 
	 * @param rawImage
	 *            {@link RawImage} object which is captured from the emulator
	 *            device.
	 * @param image
	 *            {@link BufferedImage} object which is used in quickstream
	 *            output
	 */
	public static void convertImage(RawImage rawImage, BufferedImage image) {
		int index = 0;
		int indexInc = rawImage.bpp >> 3;
		
		/*
		 * For Marshmallow and N Preview, when the phone is
		 * in landscape mode, RawImage presents the rotated 
		 * version of the image with height and width values 
		 * swapped. That breaks BufferedImage's height and 
		 * width boundary. We need to counter-do the rotation.
		 * 
		 * TODO: With this fix, there is inconsistency with 
		 * the image orientation when device is in landscape
		 * mode, between versions below Marshmallow and 
		 * versions starting from Marshmallow. We will add
		 * getOrientation() to the IAroDevice class to
		 * address the inconsistency issue.
		 */
		if (rawImage.height < rawImage.width) {
			rawImage = rawImage.getRotated();
		}
		
		// RawImage is redrawn in to BufferedImage.
		for (int y = 0; y < rawImage.height; y++) {
			for (int x = 0; x < rawImage.width; x++, index += indexInc) {
				int value = rawImage.getARGB(index);
				image.setRGB(x, y, value);
			}
		}
	}

	/**
	 * resize image to fit target size
	 * @param image
	 * @param targetWidth
	 * @param targetHeight
	 * @return
	 */
	public static BufferedImage resize(BufferedImage image, int targetWidth, int targetHeight){
		int imageType = BufferedImage.TYPE_INT_ARGB;
		
		BufferedImage result = image;
		
		BufferedImage tmp = new BufferedImage(targetWidth, targetHeight, imageType);
		Object hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		Graphics2D g2 = tmp.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g2.drawImage(image, 0, 0, targetWidth, targetHeight, 0, 0, image.getWidth(), image.getHeight(), null);
        g2.dispose();
        
        result = tmp;
		return result;
	}
	/**
	 * convert byte array of image to BufferedImge
	 * @param array data of image
	 * @return new instance of BufferedImage
	 * @throws IOException
	 */
	public static BufferedImage getImageFromByte(byte[] array) throws IOException {
		InputStream instream = new ByteArrayInputStream(array);
		String imageType = getImageType(array);
		ImageDecoder dec = ImageCodec.createImageDecoder(imageType, instream, null);
		BufferedImage image = null;
		try {
			RenderedImage renderedImg = dec.decodeAsRenderedImage(0);
			RenderedImage rendering = new NullOpImage(renderedImg, null, null, OpImage.OP_IO_BOUND);
			image = convertRenderedImage(rendering);
		} catch (ImagingException exp) {
			setImageDecoderStatus(false);
		}
		return image;
	}

	public static String getImageType(byte[] array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8 && array.length >= 8; i++) {
			sb.append(String.format("%02X ", array[i]));
		}
		if (sb.toString().trim().equals(PNG_IMAGE_HEADER)) {
			return "png";
		} 
		return "tiff";
	}
	public static BufferedImage convertRenderedImage(RenderedImage img) {
	    if (img instanceof BufferedImage) {
	        return (BufferedImage)img;  
	    }   
	    ColorModel cm = img.getColorModel();
	    int width = img.getWidth();
	    int height = img.getHeight();
	    WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
	    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
	    Hashtable<String, Object> properties = new Hashtable<>();
	    String[] keys = img.getPropertyNames();
	    if (keys!=null) {
	        for (int i = 0; i < keys.length; i++) {
	            properties.put(keys[i], img.getProperty(keys[i]));
	        }
	    }
	    BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
	    img.copyData(raster);
	    return result;
	}
	/**
	 * rotate BufferedImage to a certain angle
	 * @param image instance of BufferedImage
	 * @param angle integer value of desired angle to rotate
	 * @return instance of BufferedImage
	 */
	public static BufferedImage rorateImage(BufferedImage image, int angle){
		
	    AffineTransform tx = new AffineTransform();
	    int x = image.getWidth() / 2;
	    int y = image.getHeight() / 2;
	    tx.translate(y, x);
	    tx.rotate(Math.toRadians(angle));
	    tx.translate(-x, -y);
	    AffineTransformOp op = new AffineTransformOp(tx,
	        AffineTransformOp.TYPE_BILINEAR);
	    image = op.filter(image, null);
	    return image;
	}
	/**
	 * rotate BufferedImage by 90 degree clockwise
	 * @param img
	 * @return
	 */
	public static BufferedImage rotate90DegreeRight(BufferedImage img) {
	    int w = img.getWidth();
	    int h = img.getHeight();

	    BufferedImage rot = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);

	    double theta = Math.PI / 2;

	    AffineTransform xform = new AffineTransform();
	    xform.translate(0.5*h, 0.5*w);
	    xform.rotate(theta);
	    xform.translate(-0.5*w, -0.5*h);

	    Graphics2D g = (Graphics2D) rot.createGraphics();
	    g.drawImage(img, xform, null);
	    g.dispose();

	    return rot;
	}
	
	/**
	 * Returns buffered image of current viewport in provided JScrollPane.
	 * 
	 * @param pane
	 * @return Graph image.
	 */
	public static BufferedImage createImage(int width, int height) {
		BufferedImage bimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		return bimage;
	}

	public static String extractFullNameFromRRInfo(HttpRequestResponseInfo hrri) {
		HttpRequestResponseInfo rsp = hrri.getAssocReqResp();
		String extractedImageName = "";
		String imageName = "";
		if (rsp != null && rsp.getObjName() != null) {
			String imagefromReq = rsp.getObjName();
			imageName = imagefromReq.substring(imagefromReq.lastIndexOf(Util.FILE_SEPARATOR) + 1);
			int pos = imageName.lastIndexOf("/") + 1;
			extractedImageName = imageName.substring(pos);
		}
		return extractedImageName;
	}

	public static boolean isImageDecoderStatus() {
		return imageDecoderStatus;
	}

	public static void setImageDecoderStatus(boolean imageDecoderStatus) {
		ImageHelper.imageDecoderStatus = imageDecoderStatus;
	}
}