/**
 * 
 */
package com.att.aro.core.peripheral.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.ISpeedThrottleEventReader;
import com.att.aro.core.peripheral.pojo.SpeedThrottleEvent;
import com.att.aro.core.peripheral.pojo.SpeedThrottleEvent.SpeedThrottleFlow;

public class SpeedThrottleEventReaderImplTest extends BaseTest{
	
	SpeedThrottleEventReaderImpl speedThrottleReader;
	private IFileManager fileReader;
	private String traceFolder = "folder";
	
	@Before
	public void setUp() throws Exception {
		fileReader = Mockito.mock(IFileManager.class);
		speedThrottleReader = (SpeedThrottleEventReaderImpl)context.getBean(ISpeedThrottleEventReader.class);
		speedThrottleReader.setFileReader(fileReader);
	}
	

	@Test
	public void testReadDataNoFile(){
		Mockito.when(fileReader.fileExist(traceFolder)).thenReturn(false);
		List<SpeedThrottleEvent> throttleInfos = speedThrottleReader.readData(traceFolder);
		assertNotNull(throttleInfos);
		assertEquals(0, throttleInfos.size());
		
	}
	
	@Test
	public void testReadData()throws IOException {
		Mockito.when(fileReader.fileExist(Mockito.anyString())).thenReturn(true);
		Mockito.when(fileReader.readAllLine(Mockito.anyString())).thenReturn(new String[] {
				"DLT , 12288 , 1488927413524",
				"ULT , 12288 , 1488927433783",
				"DLT , 102400 , 1488927450230",

		});
		
		List<SpeedThrottleEvent> throttleInfos = speedThrottleReader.readData(traceFolder);
		
		assertNotNull(throttleInfos);
		assertEquals(3, throttleInfos.size());
		assertEquals(SpeedThrottleFlow.DLT, throttleInfos.get(0).getThrottleFlow());
		assertEquals(12288, throttleInfos.get(0).getThrottleSpeed());
		assertEquals(1488927413524L, throttleInfos.get(0).getTimeStamp());
 	}
	
	@Test
	public void testReadDataThrowException() throws IOException {
		Mockito.when(fileReader.fileExist(Mockito.anyString())).thenReturn(true);
		
		Mockito.when(fileReader.readAllLine(Mockito.anyString())).thenReturn(new String[] {
					"DLT  1488927413524" });
		
		
		List<SpeedThrottleEvent> throttleInfos = speedThrottleReader.readData(traceFolder);
		
		assertNotNull(throttleInfos);
		 

	}


}
