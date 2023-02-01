/*
 *  Copyright 2022 AT&T
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import com.att.aro.core.fileio.impl.FileManagerImpl;

public class ImageHelperTest {

	FileManagerImpl fm = new FileManagerImpl();

	//small tiff image that was converted to byte array
	//Size: 600 bytes
	//Dimension: 10x10 pixel
	byte[] imagedata = new byte[] { 73, 73, 42, 0, 120, 0, 0, 0, -128, 57, 31, -128, 16, 3, -4, 6, 22, 0, 66, 92, 1, 7, -16, 0, 6, -17, 10, -127, -33, -32, 7, 80, 76, 0, -3, 2, 59,
			64, -79, 112, 35, -92, 36, 121, 62, -125, 93, 111, -16, 64, 37, -28, -10, 86, -99, -46, -60, -9, 64, 16, 16, -77, 35, 59, -116, -83, 18, -70, 17, -60, 22, 117, 18,
			-108, 15, 18, -53, 52, -122, -103, 1, -71, 20, 12, 50, 10, -107, 8, 103, 9, 45, -47, -114, -107, -128, 68, 30, 69, 102, -119, -33, -51, 50, 50, -66, 18, 0, 104, 60, 31,
			96, 16, 40, 40, 92, -4, 0, 0, 96, 32, 21, 0, 0, 1, 3, 0, 1, 0, 0, 0, 10, 0, 0, 0, 1, 1, 3, 0, 1, 0, 0, 0, 10, 0, 0, 0, 2, 1, 3, 0, 1, 0, 0, 0, 8, 0, 0, 0, 3, 1, 3, 0,
			1, 0, 0, 0, 5, 0, 0, 0, 6, 1, 3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 10, 1, 3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 13, 1, 2, 0, 124, 0, 0, 0, -54, 1, 0, 0, 14, 1, 2, 0, 18, 0, 0, 0, 70,
			2, 0, 0, 17, 1, 4, 0, 1, 0, 0, 0, 8, 0, 0, 0, 18, 1, 3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 21, 1, 3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 22, 1, 3, 0, 1, 0, 0, 0, 51, 3, 0, 0, 23, 1, 4,
			0, 1, 0, 0, 0, 112, 0, 0, 0, 26, 1, 5, 0, 1, 0, 0, 0, 122, 1, 0, 0, 27, 1, 5, 0, 1, 0, 0, 0, -126, 1, 0, 0, 28, 1, 3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 40, 1, 3, 0, 1, 0, 0,
			0, 3, 0, 0, 0, 41, 1, 3, 0, 2, 0, 0, 0, 0, 0, 1, 0, 61, 1, 3, 0, 1, 0, 0, 0, 2, 0, 0, 0, 62, 1, 5, 0, 2, 0, 0, 0, -70, 1, 0, 0, 63, 1, 5, 0, 6, 0, 0, 0, -118, 1, 0, 0,
			0, 0, 0, 0, -1, -1, -1, -1, 96, -33, 42, 2, -1, -1, -1, -1, 96, -33, 42, 2, 0, 10, -41, -93, -1, -1, -1, -1, -128, -31, 122, 84, -1, -1, -1, -1, 0, -51, -52, 76, -1,
			-1, -1, -1, 0, -102, -103, -103, -1, -1, -1, -1, -128, 102, 102, 38, -1, -1, -1, -1, -16, 40, 92, 15, -1, -1, -1, -1, -128, 27, 13, 80, -1, -1, -1, -1, 0, 88, 57, 84,
			-1, -1, -1, -1, 47, 115, 114, 118, 47, 119, 119, 119, 47, 118, 104, 111, 115, 116, 115, 47, 111, 110, 108, 105, 110, 101, 45, 99, 111, 110, 118, 101, 114, 116, 46, 99,
			111, 109, 47, 115, 97, 118, 101, 47, 113, 117, 101, 117, 101, 100, 47, 48, 47, 56, 47, 97, 47, 48, 56, 97, 100, 53, 56, 99, 57, 100, 55, 99, 56, 50, 97, 52, 102, 53,
			102, 101, 49, 97, 54, 98, 97, 51, 100, 100, 102, 98, 101, 54, 99, 47, 105, 110, 116, 101, 114, 109, 101, 100, 105, 97, 116, 101, 49, 47, 111, 95, 98, 51, 98, 50, 48,
			97, 100, 97, 48, 99, 48, 101, 49, 51, 57, 52, 46, 116, 105, 102, 102, 0, 67, 114, 101, 97, 116, 101, 100, 32, 119, 105, 116, 104, 32, 71, 73, 77, 80, 0 };

	byte[] pngdata = new byte[] { -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 14, 0, 0, 0, 17, 8, 6, 0, 0, 0, -19, -56, -99, -97, 0, 0, 0, 1, 115, 82,
			71, 66, 0, -82, -50, 28, -23, 0, 0, 0, 9, 112, 72, 89, 115, 0, 0, 11, 19, 0, 0, 11, 19, 1, 0, -102, -100, 24, 0, 0, 4, 34, 105, 84, 88, 116, 88, 77, 76, 58, 99, 111,
			109, 46, 97, 100, 111, 98, 101, 46, 120, 109, 112, 0, 0, 0, 0, 0, 60, 120, 58, 120, 109, 112, 109, 101, 116, 97, 32, 120, 109, 108, 110, 115, 58, 120, 61, 34, 97, 100,
			111, 98, 101, 58, 110, 115, 58, 109, 101, 116, 97, 47, 34, 32, 120, 58, 120, 109, 112, 116, 107, 61, 34, 88, 77, 80, 32, 67, 111, 114, 101, 32, 53, 46, 52, 46, 48, 34,
			62, 10, 32, 32, 32, 60, 114, 100, 102, 58, 82, 68, 70, 32, 120, 109, 108, 110, 115, 58, 114, 100, 102, 61, 34, 104, 116, 116, 112, 58, 47, 47, 119, 119, 119, 46, 119,
			51, 46, 111, 114, 103, 47, 49, 57, 57, 57, 47, 48, 50, 47, 50, 50, 45, 114, 100, 102, 45, 115, 121, 110, 116, 97, 120, 45, 110, 115, 35, 34, 62, 10, 32, 32, 32, 32, 32,
			32, 60, 114, 100, 102, 58, 68, 101, 115, 99, 114, 105, 112, 116, 105, 111, 110, 32, 114, 100, 102, 58, 97, 98, 111, 117, 116, 61, 34, 34, 10, 32, 32, 32, 32, 32, 32,
			32, 32, 32, 32, 32, 32, 120, 109, 108, 110, 115, 58, 116, 105, 102, 102, 61, 34, 104, 116, 116, 112, 58, 47, 47, 110, 115, 46, 97, 100, 111, 98, 101, 46, 99, 111, 109,
			47, 116, 105, 102, 102, 47, 49, 46, 48, 47, 34, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 120, 109, 108, 110, 115, 58, 101, 120, 105, 102, 61, 34, 104, 116,
			116, 112, 58, 47, 47, 110, 115, 46, 97, 100, 111, 98, 101, 46, 99, 111, 109, 47, 101, 120, 105, 102, 47, 49, 46, 48, 47, 34, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
			32, 32, 120, 109, 108, 110, 115, 58, 100, 99, 61, 34, 104, 116, 116, 112, 58, 47, 47, 112, 117, 114, 108, 46, 111, 114, 103, 47, 100, 99, 47, 101, 108, 101, 109, 101,
			110, 116, 115, 47, 49, 46, 49, 47, 34, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 120, 109, 108, 110, 115, 58, 120, 109, 112, 61, 34, 104, 116, 116, 112, 58,
			47, 47, 110, 115, 46, 97, 100, 111, 98, 101, 46, 99, 111, 109, 47, 120, 97, 112, 47, 49, 46, 48, 47, 34, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 116, 105, 102,
			102, 58, 82, 101, 115, 111, 108, 117, 116, 105, 111, 110, 85, 110, 105, 116, 62, 50, 60, 47, 116, 105, 102, 102, 58, 82, 101, 115, 111, 108, 117, 116, 105, 111, 110,
			85, 110, 105, 116, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 116, 105, 102, 102, 58, 67, 111, 109, 112, 114, 101, 115, 115, 105, 111, 110, 62, 53, 60, 47, 116,
			105, 102, 102, 58, 67, 111, 109, 112, 114, 101, 115, 115, 105, 111, 110, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 116, 105, 102, 102, 58, 88, 82, 101, 115, 111,
			108, 117, 116, 105, 111, 110, 62, 55, 50, 60, 47, 116, 105, 102, 102, 58, 88, 82, 101, 115, 111, 108, 117, 116, 105, 111, 110, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32,
			32, 60, 116, 105, 102, 102, 58, 79, 114, 105, 101, 110, 116, 97, 116, 105, 111, 110, 62, 49, 60, 47, 116, 105, 102, 102, 58, 79, 114, 105, 101, 110, 116, 97, 116, 105,
			111, 110, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 116, 105, 102, 102, 58, 89, 82, 101, 115, 111, 108, 117, 116, 105, 111, 110, 62, 55, 50, 60, 47, 116, 105,
			102, 102, 58, 89, 82, 101, 115, 111, 108, 117, 116, 105, 111, 110, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 101, 120, 105, 102, 58, 80, 105, 120, 101, 108, 88,
			68, 105, 109, 101, 110, 115, 105, 111, 110, 62, 49, 52, 60, 47, 101, 120, 105, 102, 58, 80, 105, 120, 101, 108, 88, 68, 105, 109, 101, 110, 115, 105, 111, 110, 62, 10,
			32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 101, 120, 105, 102, 58, 67, 111, 108, 111, 114, 83, 112, 97, 99, 101, 62, 49, 60, 47, 101, 120, 105, 102, 58, 67, 111, 108, 111,
			114, 83, 112, 97, 99, 101, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 101, 120, 105, 102, 58, 80, 105, 120, 101, 108, 89, 68, 105, 109, 101, 110, 115, 105, 111,
			110, 62, 49, 55, 60, 47, 101, 120, 105, 102, 58, 80, 105, 120, 101, 108, 89, 68, 105, 109, 101, 110, 115, 105, 111, 110, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60,
			100, 99, 58, 115, 117, 98, 106, 101, 99, 116, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 114, 100, 102, 58, 66, 97, 103, 47, 62, 10, 32, 32, 32, 32,
			32, 32, 32, 32, 32, 60, 47, 100, 99, 58, 115, 117, 98, 106, 101, 99, 116, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 120, 109, 112, 58, 77, 111, 100, 105, 102,
			121, 68, 97, 116, 101, 62, 50, 48, 49, 55, 45, 49, 48, 45, 50, 53, 84, 49, 55, 58, 49, 48, 58, 50, 57, 60, 47, 120, 109, 112, 58, 77, 111, 100, 105, 102, 121, 68, 97,
			116, 101, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 120, 109, 112, 58, 67, 114, 101, 97, 116, 111, 114, 84, 111, 111, 108, 62, 80, 105, 120, 101, 108, 109, 97,
			116, 111, 114, 32, 51, 46, 54, 60, 47, 120, 109, 112, 58, 67, 114, 101, 97, 116, 111, 114, 84, 111, 111, 108, 62, 10, 32, 32, 32, 32, 32, 32, 60, 47, 114, 100, 102, 58,
			68, 101, 115, 99, 114, 105, 112, 116, 105, 111, 110, 62, 10, 32, 32, 32, 60, 47, 114, 100, 102, 58, 82, 68, 70, 62, 10, 60, 47, 120, 58, 120, 109, 112, 109, 101, 116,
			97, 62, 10, -106, -81, -21, 5, 0, 0, 2, 90, 73, 68, 65, 84, 40, 21, -99, 83, 75, 72, 84, 81, 24, -2, -50, 125, -52, -5, -50, -99, -79, -21, -125, -92, -84, 32, -55, 20,
			-33, 48, 16, -108, -114, 21, 4, -82, 90, -28, -62, 54, 109, 42, -45, 22, 45, 3, 11, -20, 69, 81, 45, 35, 72, -118, 104, 33, 20, -44, -94, -123, -83, 52, -107, 10, 44,
			123, -48, -8, -102, -102, 114, -58, -47, -102, 123, -25, -95, 51, -93, 35, 51, -50, 56, -89, 123, -125, 11, -30, 106, -16, -33, 124, -25, -4, -1, -9, -3, -33, -31, -4,
			-25, -112, 30, 55, -114, 82, 2, 55, 40, -54, 80, 72, 16, -56, -124, 98, 4, -35, 109, -72, 89, 8, 127, 51, 71, -41, -48, -51, -55, 2, -41, 20, -35, 110, 60, -34, 74,
			-98, 27, -20, -83, 80, -58, 111, 29, -89, -2, -89, -114, -83, 53, 109, -33, -43, -126, 11, 68, 69, -51, 81, -61, -1, -15, 110, -32, -74, 83, -98, -11, -12, 82, 118,
			-11, -4, -114, 61, -26, -50, -74, 51, 7, 7, 9, -23, -53, -21, 117, 13, 47, -75, -62, -63, -88, -114, 79, -12, 36, -91, -45, 6, 33, 39, -75, -121, -1, 24, 78, 15, 15,
			122, 108, -97, -58, 103, -70, -122, 6, -2, 86, 83, -38, -57, -24, 28, 13, -85, 42, -111, 98, 39, 2, 120, -83, -82, -81, 81, 80, -14, 70, 124, -47, 16, -12, -123, -17,
			37, 66, 116, 95, 78, 61, -61, 90, 78, -34, -97, -89, 66, 17, -49, -74, 76, 60, 122, -10, 42, -82, -117, 75, -91, -51, -114, -98, 73, 71, 58, -109, -23, -15, -3, -16,
			-41, -60, -106, -41, 96, -32, -84, -32, 89, 17, -39, -76, -95, -29, -25, -92, -17, -84, -41, -5, 94, -48, -123, 73, 59, 18, -70, -29, -11, 19, -19, 77, 29, -21, -87,
			-107, 43, -39, 84, -102, 113, -118, 118, 16, 46, -115, -30, -35, 78, 24, 77, 14, -62, 25, -118, 106, -30, -47, -20, -94, -46, 80, 63, 21, 24, 29, -51, -49, -52, 32,
			-49, -100, 59, -122, -118, -88, 60, -26, -98, -11, 125, -72, 63, -27, 29, -26, -86, 27, -118, 81, 94, -63, -61, 96, 89, -121, -39, 104, -122, -43, 42, 32, -98, 72, 56,
			-65, 125, -7, 126, -9, 106, -19, -95, -109, -102, -85, 58, -57, 7, 92, -1, 16, 22, 46, 39, -107, -105, -79, 88, 80, 82, -94, 1, 8, -30, 17, 20, -39, 36, -56, 75, 115,
			-32, 120, 2, 66, 54, 32, 43, 10, -126, -95, -92, 100, 18, -8, 27, -85, -31, -80, -49, 86, 82, 114, -111, 85, 27, -112, -54, -67, 66, -1, 111, -81, 23, -94, -55, -114,
			-44, 82, 22, 38, 70, 68, 99, 99, 13, -120, 37, 3, -1, -62, 2, -28, -24, 18, 66, -111, 8, 50, -71, 13, 9, 44, -78, 66, -24, -7, 20, -89, 89, 7, 126, 45, 99, 37, -63,
			-125, 101, 8, -58, 62, 127, -59, -84, 61, -122, -86, -70, 114, -52, -57, 61, -16, 47, 70, -112, -25, 108, -40, -56, -13, 80, -62, -13, 32, 108, 115, -39, -61, 17, -100,
			-30, -70, 91, -47, -31, 114, -75, -35, -119, -108, -20, -126, -35, 104, 33, -30, -31, -99, 48, 25, 75, -95, 4, 63, -62, -78, 110, 100, 92, -82, 102, -62, 91, 37, 100,
			96, -58, -127, 122, 23, 106, -101, -22, -90, -43, -39, 39, 53, -61, 109, -67, 85, 86, -67, 33, 126, -62, -113, -73, 90, -121, 66, 67, -45, -112, -19, -2, -57, 127, -68,
			-50, -22, -22, -118, 37, 122, 9, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126 };


	//	/**
	//	 * with existing JDK 18.0.2 and JDK 8.311
	//	 * 
	//	 * Workspace default JRE 
	//	 * path="org.eclipse.jdt.launching.JRE_CONTAINER"
	//	 * JRE-JDK 8	WORKS
	//	 * JRE-JDK 18	WORKS
	//	 * 
	//	 * path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.launching.macosx.MacOSXType/Java SE 8 [1.8.0_311] "
	//	 * JRE-JDK 8	WORKS
	//	 * JRE-JDK 18	WORKS
	//	 * 
	//	 * path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8"
	//	 * jre 1.8 
	//	 * JRE-JDK 8	WORKS
	//	 * JRE-JDK 18	WORKS
	//	 * 
	//	 */
	@Test
	public void testConvertFormatToRaw() {
		System.err.println("testConvertFormatToRaw()");
		String imgfile = "src/main/resources/aro_24.png";

		File fimage = new File(imgfile);
		System.out.println(String.format("fimage :%b", fimage.exists()));

		try {
			System.out.print("converting...");
			ByteArrayOutputStream byteArrayOutputStream = ImageHelper.convertFormat(imgfile, "gif");
			byte[] bArray = byteArrayOutputStream.toByteArray();
			assertTrue(bArray.length == 1340);
			assertTrue(byteArrayOutputStream.size() == 1340);
			// fm.saveFile("/Users/bn153x/VideoOptimizerTraceIOS/screenShots/temp/pic.gif", bArray);
			System.out.println(" converted to ByteArrayOutputStream");
		} catch (Exception e) {
			System.out.println(" failed" + e.getMessage());
		}
	}

	@Test
	public void testByteArray_To_BufferedImage() throws IOException {

		System.out.println("Java :" + Util.JDK_VERSION);
		byte[] imageByteArray = pngdata;

		BufferedImage bufferedImage = ImageHelper.convertToBufferedImage(imageByteArray);

		System.out.println("Width  :" + bufferedImage.getWidth());
		System.out.println("Height :" + bufferedImage.getHeight());
		assertEquals(14, bufferedImage.getWidth());
		assertEquals(17, bufferedImage.getHeight());
	}

	@Test
	public void testGetImage_To_BufferedImage() throws IOException {
		System.err.println("testGetImageFromByte");
		String screenShotName = "src/main/resources/aro_24.png";

		ByteArrayOutputStream byteArrayOutputStream = ImageHelper.convertFormat(screenShotName, "png");
		BufferedImage bufferedImage = ImageHelper.convertToBufferedImage(byteArrayOutputStream.toByteArray());

		System.out.println("Width  :" + bufferedImage.getWidth());
		System.out.println("Height :" + bufferedImage.getHeight());
		assertEquals(24, bufferedImage.getWidth());
		assertEquals(24, bufferedImage.getHeight());
	}

	@Test
	public void testConvertToBufferedImage() throws IOException {
		String screenShotName;
		screenShotName = "src/main/resources/aro_24.png";

		ByteArrayOutputStream byteArrayOutputStream = ImageHelper.convertFormat(screenShotName, "jpg");
		BufferedImage image = ImageHelper.convertToBufferedImage(byteArrayOutputStream.toByteArray());
		assertTrue(image != null);
		System.out.println(image.getWidth());
		System.out.println(image.getHeight());
		assertEquals(24, image.getWidth());
		assertEquals(24, image.getHeight());
	}

	@Test
	public void resizeTest() {
		BufferedImage img = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
		BufferedImage newimg = ImageHelper.resize(img, 50, 40);
		assertEquals(50, newimg.getWidth());
		assertEquals(40, newimg.getHeight());
	}

	@Test
	public void rorateImageTest() {
		BufferedImage img = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
		BufferedImage newimg = ImageHelper.rorateImage(img, 90);
		assertEquals(128, newimg.getWidth());
		assertEquals(256, newimg.getHeight());
	}

	@Test
	public void rorate90ImageTest() {
		BufferedImage img = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
		BufferedImage newimg = ImageHelper.rotate90DegreeRight(img);
		assertEquals(128, newimg.getWidth());
		assertEquals(256, newimg.getHeight());
	}

	@Test
	public void convertRenderedImageTest() {
		RenderedImage renderimage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		BufferedImage newimage = ImageHelper.convertRenderedImage(renderimage);
		assertTrue(newimage instanceof BufferedImage);
	}

	@Test
	public void createImageTest() {
		BufferedImage newimage = ImageHelper.createImage(10, 10);
		assertEquals(10, newimage.getWidth());
		assertEquals(10, newimage.getWidth());
	}
}
