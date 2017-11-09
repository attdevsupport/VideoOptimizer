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

package com.att.aro.core.peripheral.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aro.core.BaseTest;
import com.att.aro.core.ILogger;
import com.att.aro.core.fileio.IFileManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CollectOptionsReaderImpl.class)
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
		collectOptionsReaderImpl = PowerMockito.mock(CollectOptionsReaderImpl.class);
		MockitoAnnotations.initMocks(this);
	}


	@Test
	public void readData_OldFormat() throws Exception {
//		File mockedFile = Mockito.mock(File.class);
//		Path mockedPath = Mockito.mock(Path.class);
//		Mockito.when(mockedFile.exists()).thenReturn(true);
//		Mockito.when(mockedFile.toPath()).thenReturn(mockedPath);
//		PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(mockedFile);
//		String[] arr = new String[] {
//				"Down Stream Delay 125",
//				"Up Stream Delay 10",
//				"Secure false",
//				"Orientation:PORTRAIT"};
//		PowerMockito.mockStatic(Files.class);
//		PowerMockito.when(Files.readAllLines(mockedPath)).thenReturn(Arrays.asList(arr));
//		CollectOptions options = collectOptionsReaderImpl.readData("/");
//		assertEquals( 125,  options.getDsDelay());
//		assertEquals( 10, options.getUsDelay());
//		assertEquals( SecureStatus.FALSE, options.getSecureStatus());
//		assertEquals( "PORTRAIT", options.getOrientation());
 
 	}
	
	@Test
	public void readData_NewFormat()throws Exception {
//		File mockedFile = Mockito.mock(File.class);
//		Path mockedPath = Mockito.mock(Path.class);
//		Mockito.when(mockedFile.exists()).thenReturn(true);
//		Mockito.when(mockedFile.toPath()).thenReturn(mockedPath);
//		PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(mockedFile);
//		String[] arr = new String[] {
//				"dsDelay=125",
//				"usDelay=10",
//				"attnProfile=true",
//				"attnProfileName=newFile",
//				"secure=false",
//				"orientation=PORTRAIT"};
//		Optional<String> str = Arrays.asList(arr).stream().reduce((a,b)->a+System.lineSeparator()+b);
//		Properties properties = new Properties();
//		ByteArrayInputStream inStream = new ByteArrayInputStream(str.get().getBytes());
//		properties.load(inStream);		
//		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
//		Mockito.when(collectOptionsReaderImpl.readNewFormat(Mockito.any())).thenReturn(new CollectOptions(properties));
//		Method method = method(CollectOptionsReaderImpl.class, "readNewFormat", File.class);
//		
//		PowerMockito.when(collectOptionsReaderImpl, method).withArguments(mockedFile)
//				.thenReturn(new CollectOptions(properties));
//		CollectOptions options = collectOptionsReaderImpl.readData("/");
//		assertEquals( 125,  options.getDsDelay());
//		assertEquals( 10, options.getUsDelay());
//		assertEquals( SecureStatus.FALSE, options.getSecureStatus());
//		assertEquals( "PORTRAIT", options.getOrientation());
//		assertEquals( "newFile", options.getAttnrProfileName());
//		assertEquals( true, options.isAttnrProfile());

	}
}
