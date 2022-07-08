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

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.IAppInfoReader;
import com.att.aro.core.peripheral.pojo.AppInfo;

public class AppInfoReaderImplTest extends BaseTest {
	AppInfoReaderImpl reader;
	private IFileManager filereader;

	@Before
	public void setup() {
		filereader = Mockito.mock(IFileManager.class);
		reader = (AppInfoReaderImpl)context.getBean(IAppInfoReader.class);
		reader.setFileReader(filereader);
	}
	
	@Test
	public void readData() throws IOException{
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[]{"\"/system/bin/rild\"1.1","./data/data/com.att.android.arodatacollector/tcpdump 4.1"};
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		AppInfo info = reader.readData("/");
		assertTrue(info.getAppInfos().size() == 2);
		assertTrue(info.getAppInfos().get(1).contains("tcpdump"));
	}
	
	@Test
	public void readDataIoException() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenThrow(new IOException("test exception"));
		AppInfo info = null;
		info = reader.readData("/");
		assertTrue(info.getAppInfos().size() == 0);
	}
	
	@Test
	public void readDataNoFile() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(false);
		AppInfo info = null;
		info = reader.readData("/");
		assertTrue(info.getAppInfos().size() == 0);
	}
}
