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
import com.att.aro.core.peripheral.pojo.ThermalStatus;
import com.att.aro.core.peripheral.pojo.ThermalStatusInfo;

public class ThermalStatusReaderImplTest extends BaseTest {
	ThermalStatusReaderImpl thermalStatusReaderImpl;
	private IFileManager filereader;

	@Before
	public void setup() {
		filereader = Mockito.mock(IFileManager.class);
		thermalStatusReaderImpl = new ThermalStatusReaderImpl(filereader);
	}

	@Test
	public void readData1() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr = new String[] { "1.623109245875E9 0", "1.62310930503E9 1", "1.623109324965E9 2",
				"1.623109354968E9 3", "1.623109474986E9 4" };
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr);
		List<ThermalStatusInfo> thermalStatusInfos = thermalStatusReaderImpl.readData("/", 1.623109248368E9,
				778.3899998664856);
		assertEquals(5, thermalStatusInfos.size(), 0);
		assertEquals(0.0, thermalStatusInfos.get(0).getBeginTimeStamp(), 0);
		assertEquals(56.66199994087219, thermalStatusInfos.get(0).getEndTimeStamp(), 0);
		assertEquals(226.61800003051758, thermalStatusInfos.get(4).getBeginTimeStamp(), 0);
		assertEquals(778.3899998664856, thermalStatusInfos.get(4).getEndTimeStamp(), 0);
		assertEquals(ThermalStatus.THROTTLING_NONE, thermalStatusInfos.get(0).getThermalStatus());
		assertEquals(ThermalStatus.THROTTLING_LIGHT, thermalStatusInfos.get(1).getThermalStatus());
		assertEquals(ThermalStatus.THROTTLING_MODERATE, thermalStatusInfos.get(2).getThermalStatus());
		assertEquals(ThermalStatus.THROTTLING_SEVERE, thermalStatusInfos.get(3).getThermalStatus());
		assertEquals(ThermalStatus.THROTTLING_CRITICAL, thermalStatusInfos.get(4).getThermalStatus());

	}

	@Test
	public void readData_formatException() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr2 = new String[] { "1.623109245875E9 0", "1.62310930503E9 1", "1.623109324965E9 xx",
				"1.623109354968E9 3" };
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr2);
		List<ThermalStatusInfo> thermalStatusInfos = thermalStatusReaderImpl.readData("/", 0, 0);
		assertEquals(4, thermalStatusInfos.size(), 0);
		assertEquals(1.623109324965E9, thermalStatusInfos.get(2).getBeginTimeStamp(), 0);
		assertEquals(1.623109354968E9, thermalStatusInfos.get(2).getEndTimeStamp(), 0);
		assertEquals(ThermalStatus.UNKNOWN, thermalStatusInfos.get(2).getThermalStatus());
	}

	@Test
	public void readData_invalidNumber() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr3 = new String[] { "1.623109245875E9 0", "1.62310930503E9 9", "1.623109324965E9 2",
				"1.623109354968E9 3" };
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr3);
		List<ThermalStatusInfo> thermalStatusInfos = thermalStatusReaderImpl.readData("/", 0, 0);
		assertEquals(4, thermalStatusInfos.size(), 0);
		assertEquals(ThermalStatus.UNKNOWN, thermalStatusInfos.get(1).getThermalStatus());
	}

	@Test
	public void readData_onlyTimeStamp() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr3 = new String[] { "1.623109245875E9 0", "1.62310930503E9 ", "1.623109324965E9 2" };
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr3);
		List<ThermalStatusInfo> thermalStatusInfos = thermalStatusReaderImpl.readData("/", 0, 0);
		assertEquals(2, thermalStatusInfos.size(), 0);
		assertEquals(ThermalStatus.THROTTLING_MODERATE, thermalStatusInfos.get(1).getThermalStatus());
	}

	@Test
	public void readData_arrayOnlyOneData() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		String[] arr4 = new String[] { "1.623109245875E9 0" };
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(arr4);
		List<ThermalStatusInfo> thermalStatusInfos = thermalStatusReaderImpl.readData("/", 0, 0);
		assertEquals(ThermalStatus.THROTTLING_NONE, thermalStatusInfos.get(0).getThermalStatus());
	}

}
