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
package com.att.aro.core.peripheral.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.ICpuTemperatureReader;
import com.att.aro.core.peripheral.pojo.TemperatureEvent;

public class CpuTemperatureReaderImplTest extends BaseTest {

	CpuTemperatureReaderImpl traceDataReader;

	private IFileManager filereader;
	private String traceFolder = "traceFolder";

	@Before
	public void setup() {
		filereader = Mockito.mock(IFileManager.class);
		traceDataReader = (CpuTemperatureReaderImpl)context.getBean(ICpuTemperatureReader.class);
		traceDataReader.setFileReader(filereader);
	}

	@Test
	public void readData() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(new String[] {             
				 "1414108436 21"            // 00	0
				,"1414108436 22"          // 01
				,"1414108463 23"         // 02	1
				,"1414108464 24"       // 03
				,"1414108466 25"       // 04	2
				,"1414108466 26"     // 05
				,"1414108473 27"         // 06	3
				,"1414108473 28"       // 07
				,"1414108473 29"          // 08	4
				,"1414108473 30"        // 09
				,"1414108473 31"          // 20	5
				,"1414108473 32"        // 21
				,"1414108473 33"          // 22	6
				,"1414108473 34"        // 23
				,
		});
		
		List<TemperatureEvent> listTemperatureEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(14.0, listTemperatureEvent.size(), 0);
		
		assertEquals( 21, listTemperatureEvent.get(0).getcelciusTemperature());
		assertEquals( 22, listTemperatureEvent.get(1).getcelciusTemperature());
		assertEquals( 23, listTemperatureEvent.get(2).getcelciusTemperature());
		assertEquals( 24, listTemperatureEvent.get(3).getcelciusTemperature());
		assertEquals( 25, listTemperatureEvent.get(4).getcelciusTemperature());
		assertEquals( 26, listTemperatureEvent.get(5).getcelciusTemperature());
		assertEquals( 27, listTemperatureEvent.get(6).getcelciusTemperature());
		assertEquals( 28, listTemperatureEvent.get(7).getcelciusTemperature());
		assertEquals( 29, listTemperatureEvent.get(8).getcelciusTemperature());
		assertEquals( 30, listTemperatureEvent.get(9).getcelciusTemperature());
		assertEquals( 31, listTemperatureEvent.get(10).getcelciusTemperature());
		assertEquals( 32, listTemperatureEvent.get(11).getcelciusTemperature());
		assertEquals( 33, listTemperatureEvent.get(12).getcelciusTemperature());
		assertEquals( 34, listTemperatureEvent.get(13).getcelciusTemperature());

		assertEquals(1414108436, listTemperatureEvent.get(0).getTimeRecorded(), 0);
		assertEquals(1414108436, listTemperatureEvent.get(1).getTimeRecorded(), 0);
		assertEquals(1414108463, listTemperatureEvent.get(2).getTimeRecorded(), 0);
		assertEquals(1414108464, listTemperatureEvent.get(3).getTimeRecorded(), 0);
		assertEquals(1414108466, listTemperatureEvent.get(4).getTimeRecorded(), 0);
		assertEquals(1414108466, listTemperatureEvent.get(5).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(6).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(7).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(8).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(9).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(10).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(11).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(12).getTimeRecorded(), 0);
		assertEquals(1414108473, listTemperatureEvent.get(13).getTimeRecorded(), 0);
	}
	
	
	@Test
	public void readData_InvalidTemperatureEvent() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(new String[] {             
				 "1414108436.923000 "            // 00	0
				,"1414108436.957000 21"          // 01
		});
		
		List<TemperatureEvent> listTemperatureEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(1.0, listTemperatureEvent.size(), 0);
	}

	/**
	 * Test shows a problem with code.<p>
	 * Parsing from String into a Number should have try/catch handling<li>
	 * See UserEventReaderImpl.java:74
	 * @throws IOException
	 */
	@Ignore
	@Test(expected=NumberFormatException.class)
	public void readData_FailedToParse() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(new String[] {             
				 "14141z8436 21"
				,"1414108436 22"
				,"1414108436 23"
				,"1414108463 24"
				,"1414108464 25"
		});
		
		List<TemperatureEvent> listTemperatureEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(0, listTemperatureEvent.size(), 0);
	}

	@Test
	public void readData_NoFile() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(false);

		List<TemperatureEvent> listTemperatureEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(0, listTemperatureEvent.size(), 0);
	}

	@Test
	public void readData_IOException() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenThrow(new IOException("test exception"));
		List<TemperatureEvent> listTemperatureEvent = null;
		listTemperatureEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(0, listTemperatureEvent.size(), 0);
	}

}           
