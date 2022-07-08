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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.IGpsInfoReader;
import com.att.aro.core.peripheral.pojo.GpsInfo;

public class GpsInfoReaderImplTest extends BaseTest {
	GpsInfoReaderImpl gpsEventReader;
	private IFileManager filereader;

	@Before
	public void setup() {
		filereader = Mockito.mock(IFileManager.class);
		gpsEventReader = (GpsInfoReaderImpl)context.getBean(IGpsInfoReader.class);
		gpsEventReader.setFileReader(filereader);
	}

	@Test
	public void readData() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"1.414011237411E9 OFF",         //  1414011237411	45542
				"1.414011282953E9 ACTIVE",      //  1414011282953	12933
				"1.414011295886E9 PhonyState",  //  1414011295886	 4149
				"1.414011300035E9 ACTIVE",      //  1414011300035	11889
				"1.414011311924E9 STANDBY"      //  1414011311924
			};

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		gpsEventReader.setFileReader(filereader);
		List<GpsInfo> info = gpsEventReader.readData("/", 0, 0);
		
		assertTrue(info.size() > 0);
	}

	@Test
	public void readData1() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"1.414011237411E9 UNKNOWN",         //  1414011237411	45542
				"1.414011237411E9 ACTIVE",         //  1414011237411	45542
				"1.414011282953E9 ACTIVE",      //  1414011282953	12933
				"1.414011295886E9 PhonyState",  //  1414011295886	 4149
				"1.414011300035E9 ACTIVE",      //  1414011300035	11889
				"1.414011311924E9 STANDBY"      //  1414011311924
			};

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		gpsEventReader.setFileReader(filereader);
		List<GpsInfo> info = gpsEventReader.readData("/", 0, 0);
		
		assertTrue(info.size() > 0);
	}

	@Test
	public void readData2() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"1.414011237411E9 UNKNOWN",         //  1414011237411	45542
				"1.414011237411E9 ACTIVE",         //  1414011237411	45542
				"1.414011282953E9 ACTIVE",      //  1414011282953	12933
				"bad data PhonyState",  //  1414011295886	 4149
				"1.414011300035E9 ACTIVE",      //  1414011300035	11889
				"1.414011311924E9 STANDBY"      //  1414011311924
			};

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		gpsEventReader.setFileReader(filereader);
		List<GpsInfo> info = gpsEventReader.readData("/", 0, 0);

		assertTrue(info.size() > 0);
	}

	@Test
	public void readData3() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"1.414011237411E9 ACTIVE",         //  1414011237411	45542
				"1.414011237411E9 ACTIVE",         //  1414011237411	45542
				"1.414011282953E9 ACTIVE",      //  1414011282953	12933
				"1.414011295886E9 PhonyState",  //  1414011295886	 4149
				"1.414011300035E9 ACTIVE",      //  1414011300035	11889
				"1.414011311924E9 STANDBY"      //  1414011311924
			};

			Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		gpsEventReader.setFileReader(filereader);
		List<GpsInfo> info = gpsEventReader.readData("/", 0, 0);
		
		assertTrue(info.size() > 0);

	}
	
	@Test
	public void readData_firstInStandby() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"1.414011237411E9 STANDBY",         //  1414011237411	45542
				"1.414011237411E9 DISABLED",         //  1414011237411	45542
				"1.414011282953E9 ACTIVE",      //  1414011282953	12933
				"1.414011295886E9 PhonyState",  //  1414011295886	 4149
				"1.414011300035E9 ACTIVE",      //  1414011300035	11889
				"1.414011311924E9 STANDBY"      //  1414011311924
			};

			Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		gpsEventReader.setFileReader(filereader);
		List<GpsInfo> info = gpsEventReader.readData("/", 0, 0);
		
		assertTrue(info.size() > 0);

	}
	
	@Test
	public void getActiveDuration() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] {
				"1.414011237411E9 OFF",         //  1414011237411	45542
				"1.414011282953E9 ACTIVE",      //  1414011282953	12933
				"1.414011295886E9 PhonyState",  //  1414011295886	 4149
				"1.414011300035E9 ACTIVE",      //  1414011300035	11889
				"1.414011311924E9 STANDBY"      //  1414011311924
			};

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		gpsEventReader.setFileReader(filereader);
		double activeDuration = gpsEventReader.getGpsActiveDuration();
		assertEquals(24.822, ((double)Math.round(activeDuration*1000.0))/1000, 0); // bcn faked
	}

	@Test
	public void readData_Exception_readAllLine() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenThrow(new IOException("Exception_readAllLine"));

		List<GpsInfo> info = gpsEventReader.readData("/", 0, 0);
		assertTrue(info.size() == 0);

	}

}
