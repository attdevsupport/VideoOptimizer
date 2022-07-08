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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.peripheral.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.IScreenRotationReader;
import com.att.aro.core.peripheral.pojo.UserEvent;

public class ScreenRotationReaderImplTest extends BaseTest {
	ScreenRotationReaderImpl screenRotationReader;
	private IFileManager filereader;

	@Before
	public void setup() {
		filereader = Mockito.mock(IFileManager.class);
		screenRotationReader = (ScreenRotationReaderImpl)context.getBean(IScreenRotationReader.class);
		screenRotationReader.setFileReader(filereader);
	}

	@Test
	public void readData() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"1.411421173004E9 portrait",	//  0
				"1.411421193602E9 landscape",	//  1
				"1.411421282459E9 portrait",	//  2
				"1.411421287928E9 landscape"	//  3
		};

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		screenRotationReader.setFileReader(filereader);
		List<UserEvent> listScreenRotation = screenRotationReader.readData("/", 0);
			
		assertEquals(4, listScreenRotation.size(), 0);
		
		assertEquals("SCREEN_PORTRAIT", listScreenRotation.get(0).getEventType().name());	
		assertEquals("SCREEN_LANDSCAPE", listScreenRotation.get(1).getEventType().name());	
		assertEquals("SCREEN_PORTRAIT", listScreenRotation.get(2).getEventType().name());	
		assertEquals("SCREEN_LANDSCAPE", listScreenRotation.get(3).getEventType().name());	
		
		assertEquals(1.411421173004E9, listScreenRotation.get(0).getPressTime(), 0);	
		assertEquals(1.411421287928E9, listScreenRotation.get(3).getPressTime(), 0);

	}
}
