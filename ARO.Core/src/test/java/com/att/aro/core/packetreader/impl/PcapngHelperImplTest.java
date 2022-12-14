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
package com.att.aro.core.packetreader.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.packetreader.IPcapngHelper;

public class PcapngHelperImplTest extends BaseTest {
	
	@InjectMocks
	PcapngHelperImpl helper;
	FileInputStream stream;
	
	@Spy
	FileManagerImpl fileManager;
	
	byte[] sampledata = new byte[] { 10, 13, 13, 10, -108, 0, 0, 0, 77, 60, 43, 26, 1, 0, 0, 0, -1, -1, -1, -1, -1, -1,
			-1, -1, 1, 0, 21, 0, 115, 101, 99, 116, 105, 111, 110, 32, 104, 101, 97, 100, 101, 114, 32, 98, 108, 111,
			99, 107, 0, 0, 0, 0, 2, 0, 7, 0, 120, 56, 54, 95, 54, 52, 0, 0, 3, 0, 14, 0, 68, 97, 114, 119, 105, 110, 32,
			49, 51, 46, 48, 46, 48, 0, 0, 0, 4, 0, 51, 0, 116, 99, 112, 100, 117, 109, 112, 32, 40, 108, 105, 98, 112,
			99, 97, 112, 32, 118, 101, 114, 115, 105, 111, 110, 32, 49, 46, 51, 46, 48, 32, 45, 32, 65, 112, 112, 108,
			101, 32, 118, 101, 114, 115, 105, 111, 110, 32, 52, 49, 41, 0, 0, 0, 0, 0, 0, -108, 0, 0, 0, 1, 0, 0, 0, 36,
			0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 2, 0, 8, 0, 112, 100, 112, 95, 105, 112, 48, 0, 0, 0, 0, 0, 36, 0, 0, 0,
			1, 0, 0, -128, 40, 0, 0, 0, 51, 0, 0, 0, 2, 0, 14, 0, 109, 68, 78, 83, 82, 101, 115, 112, 111, 110, 100,
			101, 114, 0, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0, 6, 0, 0, 0, -124, 0, 0, 0, 0, 0, 0, 0, -119, -15, 4, 0, 44,
			-119, 94, -56, 70, 0, 0, 0, 70, 0, 0, 0, 2, 0, 0, 0, 69, 0, 0, 66, -80, -108, 0, 0, -1, 17, 9, 14, 10, 95,
			37, -114, -84, 26, 38, 1, -7, -64, 0, 53, 0, 46, 109, -93, -127, 9, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, 119,
			119, 119, 3, 99, 110, 110, 3, 99, 111, 109, 4, 118, 103, 116, 102, 3, 110, 101, 116, 0, 0, 1, 0, 1, 0, 0, 1,
			-128, 4, 0, 0, 0, 0, 0, 2, 0, 4, 0, 2, 0, 0, 0, 2, -128, 4, 0, -124, 3, 0, 0, 0, 0, 0, 0, -124, 0, 0, 0, 6,
			0, 0, 0, -124, 0, 0, 0, 0, 0, 0, 0, -119, -15, 4, 0, 120, -3, 124, -56, 70, 0, 0, 0, 70, 0, 0, 0, 2, 0, 0,
			0, 69, 0, 0, 66, -76, -42, 0, 0, -1, 17, 4, -52, 10, 95, 37, -114, -84, 26, 38, 1, -7, -64, 0, 53, 0, 46,
			109, -93, -127, 9, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, 119, 119, 119, 3, 99, 110, 110, 3, 99, 111, 109, 4, 118,
			103, 116, 102, 3, 110, 101, 116, 0, 0, 1, 0, 1, 0, 0, 1, -128, 4, 0, 0, 0, 0, 0, 2, 0, 4, 0, 2, 0, 0, 0, 2,
			-128, 4, 0, -124, 3, 0, 0, 0, 0, 0, 0, -124, 0, 0, 0, 6, 0, 0, 0, -32, 0, 0, 0, 0, 0, 0, 0, -119, -15, 4, 0,
			-25, 70, -116, -56, -95, 0, 0, 0, -95, 0, 0, 0, 2, 0, 0, 0, 69, 88, 0, -99, 45, 89, 64, 0, -3, 17, 77, -106,
			-84, 26, 38, 1, 10, 95, 37, -114, 0, 53, -7, -64, 0, -119, 114, -106, -127, 9, -127, -128, 0, 1, 0, 5, 0, 0,
			0, 0, 3, 119, 119, 119, 3, 99, 110, 110, 3, 99, 111, 109, 4, 118, 103, 116, 102, 3, 110, 101, 116, 0, 0, 1,
			0, 1, -64, 12, 0, 5, 0, 1, 0, 0, 0, 67, 0, 15, 7, 99, 110, 110, 45, 99, 111, 112, 4, 103, 115, 108, 98, -64,
			24, -64, 50, 0, 1, 0, 1, 0, 0, 0, 111, 0, 4, -99, -90, -18, 17, -64, 50, 0, 1, 0, 1, 0, 0, 0, 111, 0, 4,
			-99, -90, -17, -79, -64, 50, 0, 1, 0, 1, 0, 0, 0, 111, 0, 4, -99, -90, -18, 48, -64, 50, 0, 1, 0, 1, 0, 0,
			0, 111, 0, 4, -99, -90, -18, -123, 0, 0, 0, 1, -128, 4, 0, 0, 0, 0, 0, 2, 0, 4, 0, 1, 0, 0, 0, 2, -128, 4,
			0, 0, 0, 0, 0, 0, 0, 0, 0, -32, 0, 0, 0, 6, 0, 0, 0, -32, 0, 0, 0, 0, 0, 0, 0, -119, -15, 4, 0, -120, 72,
			-116, -56, -95, 0, 0, 0, -95, 0, 0, 0, 2, 0, 0, 0, 69, 88, 0, -99, 45, 90, 64, 0, -3, 17, 77, -107, -84, 26,
			38, 1, 10, 95, 37, -114, 0, 53, -7, -64, 0, -119, 114, -106, -127, 9, -127, -128, 0, 1, 0, 5, 0, 0, 0, 0, 3,
			119, 119, 119, 3, 99, 110, 110, 3, 99, 111, 109, 4, 118, 103, 116, 102, 3, 110, 101, 116, 0, 0, 1, 0, 1,
			-64, 12, 0, 5, 0, 1, 0, 0, 0, 67, 0, 15, 7, 99, 110, 110, 45, 99, 111, 112, 4, 103, 115, 108, 98, -64, 24,
			-64, 50, 0, 1, 0, 1, 0, 0, 0, 111, 0, 4, -99, -90, -18, -123, -64, 50, 0, 1, 0, 1, 0, 0, 0, 111, 0, 4, -99,
			-90, -18, 17, -64, 50, 0, 1, 0, 1, 0, 0, 0, 111, 0, 4, -99, -90, -17, -79, -64, 50, 0, 1, 0, 1, 0, 0, 0,
			111, 0, 4, -99, -90, -18, 48, 0, 0, 0, 1, -128, 4, 0, 0, 0, 0, 0, 2, 0, 4, 0, 1, 0, 0, 0, 2, -128, 4, 0, 0,
			0, 0, 0, 0, 0, 0, 0, -32, 0, 0, 0, 1, 0, 0, -128, 40, 0, 0, 0, -113, 0, 0, 0, 2, 0, 13, 0, 77, 111, 98, 105,
			108, 101, 83, 97, 102, 97, 114, 105, 0, 0, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0, 6, 0, 0, 0, -128, 0, 0, 0, 0, 0,
			0, 0, -119, -15, 4, 0, -19, 73, -116, -56, 68, 0, 0, 0, 68, 0, 0, 0, 2, 0, 0, 0, 69, 0, 0, 64, -125, -25,
			64, 0, 64, 6, -5, 43, 10, 95, 37, -114, -99, -90, -18, 17, -64, -48, 0, 80, 85, 62, 39, -61, 0, 0, 0, 0,
			-80, 2, -1, -1, 100, 107, 0, 0, 2, 4, 5, 90, 1, 3, 3, 4, 1, 1, 8, 10, 26, 7, -65, 30, 0, 0, 0, 0, 4, 2, 0,
			0, 1, -128, 4, 0, 1, 0, 0, 0, 2, 0, 4, 0, 2, 0, 0, 0, 2, -128, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128, 0, 0, 0,
			6, 0, 0, 0, 124, 0, 0, 0, 0, 0, 0, 0, -119, -15, 4, 0, 38, 75, -116, -56, 64, 0, 0, 0, 64, 0, 0, 0, 2, 0, 0,
			0, 69, 88, 0, 60, 0, 0, 64, 0, 62, 6, -128, -65, -99, -90, -18, 17, 10, 95, 37, -114, 0, 80, -64, -48, -67,
			-48, 4, -5, 85, 62, 39, -60, -96, 18, 53, -44, 14, -94, 0, 0, 2, 4, 5, 110, 4, 2, 8, 10, 44, -56, 65, 60,
			26, 7, -65, 30, 1, 3, 3, 9, 1, -128, 4, 0, 1, 0, 0, 0, 2, 0, 4, 0, 1, 0, 0, 0, 2, -128, 4, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 124, 0, 0, 0, 6, 0, 0, 0, 116, 0, 0, 0, 0, 0, 0, 0, -119, -15, 4, 0, 78, 76, -116, -56, 56, 0, 0,
			0, 56, 0, 0, 0, 2, 0, 0, 0, 69, 0, 0, 52, -58, -69, 64, 0, 64, 6, -72, 99, 10, 95, 37, -114, -99, -90, -18,
			17, -64, -48, 0, 80, 85, 62, 39, -60, -67, -48, 4, -4, -128, 16, 32, 40, 82, 124, 0, 0, 1, 1, 8, 10, 26, 7,
			-65, 120, 44, -56, 65, 60, 1, -128, 4, 0, 1, 0, 0, 0, 2, 0, 4, 0, 2, 0, 0, 0, 2, -128, 4, 0, -124, 3, 0, 0,
			0, 0, 0, 0, 116, 0, 0, 0, 6, 0, 0, 0, -108, 3, 0, 0, 0, 0, 0, 0, -119, -15, 4, 0, 107, 77, -116, -56, 88, 3,
			0, 0, 88, 3, 0, 0, 2, 0, 0, 0, 69, 0, 3, 84, -49, 113, 64, 0, 64, 6, -84, -115, 10, 95, 37, -114, -99, -90,
			-18, 17, -64, -48, 0, 80, 85, 62, 39, -60, -67, -48, 4, -4, -128, 24, 32, 40, -88, 45, 0, 0, 1, 1, 8, 10,
			26, 7, -65, 121, 44, -56, 65, 60, 71, 69, 84, 32, 47, 32, 72, 84, 84, 80, 47, 49, 46, 49, 13, 10, 72, 111,
			115, 116, 58, 32, 119, 119, 119, 46, 99, 110, 110, 46, 99, 111, 109, 13, 10, 65, 99, 99, 101, 112, 116, 58,
			32, 116, 101, 120, 116, 47, 104, 116, 109, 108, 44, 97, 112, 112, 108, 105, 99, 97, 116, 105, 111, 110, 47,
			120, 104, 116, 109, 108, 43, 120, 109, 108, 44, 97, 112, 112, 108, 105, 99, 97, 116, 105, 111, 110, 47, 120,
			109, 108, 59, 113, 61, 48, 46, 57, 44, 42, 47, 42, 59, 113, 61, 48, 46, 56, 13, 10, 67, 111, 110, 110, 101,
			99, 116, 105, 111, 110, 58, 32, 107, 101, 101, 112, 45, 97, 108, 105, 118, 101, 13, 10, 67, 111, 111, 107,
			105, 101, 58, 32, 67, 71, 61, 85, 83, 58, 45, 45, 58, 45, 45, 59, 32, 95, 95, 118, 114, 102, 61, 49, 51, 57,
			49, 52, 55, 51, 54, 57, 48, 50, 52, 55, 81, 54, 104, 98, 120, 56, 79, 89, 73, 115, 65, 117, 48, 81, 85, 67,
			52, 112, 56, 78, 89, 71, 107, 55, 55, 54, 56, 54, 88, 108, 111, 73, 59, 32, 111, 112, 116, 105, 109, 105,
			122, 101, 108, 121, 66, 117, 99, 107, 101, 116, 115, 61, 37, 55, 66, 37, 55, 68, 59, 32, 111, 112, 116, 105,
			109, 105, 122, 101, 108, 121, 69, 110, 100, 85, 115, 101, 114, 73, 100, 61, 111, 101, 117, 49, 51, 56, 55,
			51, 57, 49, 54, 48, 57, 51, 51, 54, 114, 48, 46, 50, 57, 51, 55, 51, 50, 55, 48, 53, 48, 54, 48, 51, 57, 50,
			54, 59, 32, 111, 112, 116, 105, 109, 105, 122, 101, 108, 121, 83, 101, 103, 109, 101, 110, 116, 115, 61, 37,
			55, 66, 37, 50, 50, 49, 55, 48, 56, 54, 52, 56, 49, 48, 37, 50, 50, 37, 51, 65, 37, 50, 50, 116, 114, 117,
			101, 37, 50, 50, 37, 50, 67, 37, 50, 50, 49, 55, 49, 56, 53, 52, 51, 57, 50, 37, 50, 50, 37, 51, 65, 37, 50,
			50, 110, 111, 110, 101, 37, 50, 50, 37, 50, 67, 37, 50, 50, 49, 55, 50, 51, 51, 50, 49, 53, 53, 37, 50, 50,
			37, 51, 65, 37, 50, 50, 100, 105, 114, 101, 99, 116, 37, 50, 50, 37, 50, 67, 37, 50, 50, 49, 55, 50, 52, 50,
			54, 55, 51, 57, 37, 50, 50, 37, 51, 65, 37, 50, 50, 115, 97, 102, 97, 114, 105, 37, 50, 50, 37, 55, 68, 59,
			32, 115, 95, 99, 99, 61, 116, 114, 117, 101, 59, 32, 115, 95, 102, 105, 100, 61, 54, 56, 51, 65, 52, 67, 66,
			54, 57, 67, 48, 54, 51 };

	@Before
	public void setup() {
		stream = Mockito.mock(FileInputStream.class);
		fileManager = Mockito.mock(FileManagerImpl.class);
		helper = (PcapngHelperImpl) context.getBean(IPcapngHelper.class);
		ReflectionTestUtils.setField(helper, "fileManager", fileManager);
	}

	@Test
	public void isNoLinkLayer() throws IOException {
		
		byte[] sampledata = new byte[]{-44, -61, -78, -95, 2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0};
		File file = Mockito.mock(File.class);
		String fileName = "/File/Path/traffic.pcap";

		doReturn(0L).when(file).lastModified();
		doReturn(240L).when(file).length();
		doReturn(stream).when(fileManager).getFileInputStream(fileName);
		
		Mockito.doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				byte[] data = (byte[]) invocation.getArguments()[0];
				for (int i = 0; i < data.length && i < sampledata.length; i++) {
					data[i] = sampledata[i];
				}
				return null;
			}
		}).when(stream).read(Mockito.any(byte[].class));

		helper = (PcapngHelperImpl) context.getBean(IPcapngHelper.class);
		boolean ispcapng = helper.isNoLinkLayer(fileName);
		assertEquals(true, ispcapng);

	}

	@Test
	public void isApplePcapngFile() throws IOException {
		
		File file = Mockito.mock(File.class);

		doReturn(0L).when(file).lastModified();
		doReturn("/File/Path/traffic.pcap").when(file).getAbsolutePath();
		doReturn(240L).when(file).length();
		doReturn(stream).when(fileManager).getFileInputStream(file);
		
		Mockito.doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				byte[] data = (byte[]) invocation.getArguments()[0];
				for (int i = 0; i < data.length && i < sampledata.length; i++) {
					data[i] = sampledata[i];
				}
				return null;
			}
		}).when(stream).read(Mockito.any(byte[].class));

		helper = (PcapngHelperImpl) context.getBean(IPcapngHelper.class);
		boolean ispcapng = helper.isApplePcapng(file);
		assertEquals(true, ispcapng);
		
		int max = 2048;
		int length = (sampledata.length < max ? sampledata.length : max);
		byte[] streamedData = helper.getByteArrayFromStream(stream, length);
		
		ispcapng = helper.isApplePcapng(streamedData);
		assertEquals(true, ispcapng);

		helper.getHardware();
		helper.getOs();
	}

	@Ignore
	@Test
	public void isApplePcapng() throws IOException {
		
		Mockito.doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				byte[] data = (byte[]) invocation.getArguments()[0];
				for (int i = 0; i < data.length && i < sampledata.length; i++) {
					data[i] = sampledata[i];
				}
				return null;
			}
		}).when(stream).read(Mockito.any(byte[].class));

		helper = (PcapngHelperImpl) context.getBean(IPcapngHelper.class);
		boolean ispcapng = helper.isApplePcapng(sampledata);
		assertEquals(true, ispcapng);

		int max = 2048;
		int length = (sampledata.length < max ? sampledata.length : max);
		byte[] streamedData = helper.getByteArrayFromStream(stream, length);
		ispcapng = helper.isApplePcapng(streamedData);
		
		assertEquals(true, ispcapng);
		assertTrue("x86_64 ".equals(helper.getHardware()));
		assertTrue("Darwin 13.0.0 ".equals(helper.getOs()));
	}

	@Test
	public void errorTest() {
		
		try {
			Mockito.doAnswer(new Answer<Void>() {

				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					throw new IOException("test ioexception");
				}
			}).when(stream).read(Mockito.any(byte[].class));
		} catch (IOException e) {
		}

		helper = (PcapngHelperImpl) context.getBean(IPcapngHelper.class);
		byte[] data = helper.getByteArrayFromStream(stream, 128);
		assertFalse(helper.isApplePcapng(data));

	}
}
