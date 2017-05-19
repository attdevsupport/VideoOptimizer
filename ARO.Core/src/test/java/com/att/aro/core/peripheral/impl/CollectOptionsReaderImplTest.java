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
package com.att.aro.core.peripheral.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.att.aro.core.BaseTest;
import com.att.aro.core.ILogger;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.pojo.CollectOptions;

public class CollectOptionsReaderImplTest extends BaseTest {
	
	@InjectMocks
	CollectOptionsReaderImpl collectOptionsReaderImpl;
	@Mock
	private IFileManager filereader;
	@Mock
	private ILogger logger;

	@Before
	public void setup() {		
		logger = Mockito.mock(ILogger.class);
		collectOptionsReaderImpl = new CollectOptionsReaderImpl();
		collectOptionsReaderImpl.setLogger(logger);
		MockitoAnnotations.initMocks(this);
	}


	@Test
	public void readData() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"Orientation:PORTRAIT"};
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		CollectOptions options = collectOptionsReaderImpl.readData("/");
		assertEquals( "PORTRAIT", options.getOrientation());
 
 	}
}