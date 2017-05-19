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
import com.att.aro.core.peripheral.LocationReader;
import com.att.aro.core.peripheral.pojo.LocationEvent;

public class LocationReaderImplTest extends BaseTest {

	LocationReaderImpl traceDataReader;

	private IFileManager filereader;
	private String traceFolder = "traceFolder";

	@Before
	public void setup() {
		filereader = Mockito.mock(IFileManager.class);
		traceDataReader = (LocationReaderImpl)context.getBean(LocationReader.class);
		traceDataReader.setFileReader(filereader);
	}

	@Test
	public void readData() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(new String[] {
				"1.487031029351E9 47.7943242 -122.2030202 Loction network",
				"1.487031045307E9 47.7943242 -122.2030202 Loction network",
				"1.487031067622E9 47.7943242 -122.2030202 Loction network",
				"1.487031088777E9 47.7943242 -122.2030202 Loction network",
				"1.487031108855E9 47.7943242 -122.2030202 Loction network",
				"1.487031128833E9 47.7942768 -122.2030345 Loction network",
				"1.48703115131E9 47.7943242 -122.2030202 Loction network",
				"1.487031172095E9 47.7942768 -122.2030345 Loction network",
				"1.487031190478E9 47.7942768 -122.2030345 Loction network",
				"1.487031213376E9 47.7942768 -122.2030345 Loction network",
				"1.487031231069E9 47.7942768 -122.2030345 Loction network",
				"1.487031251292E9 47.7942768 -122.2030345 Loction network",
				"1.487031272251E9 47.7942768 -122.2030345 Loction network",
		});
		
		List<LocationEvent> listLocationEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(13.0, listLocationEvent.size(), 0);
		
		assertEquals( 47.7943242, listLocationEvent.get(0).getLatitude(), 0);
		assertEquals( 47.7943242, listLocationEvent.get(1).getLatitude(), 0);
		assertEquals( 47.7943242, listLocationEvent.get(2).getLatitude(), 0);
		assertEquals( 47.7943242, listLocationEvent.get(3).getLatitude(), 0);
		assertEquals( 47.7943242, listLocationEvent.get(4).getLatitude(), 0);
		assertEquals( 47.7942768, listLocationEvent.get(5).getLatitude(), 0);
		assertEquals( 47.7943242, listLocationEvent.get(6).getLatitude(), 0);
		assertEquals( 47.7942768, listLocationEvent.get(7).getLatitude(), 0);
		assertEquals( 47.7942768, listLocationEvent.get(8).getLatitude(), 0);
		assertEquals( 47.7942768, listLocationEvent.get(9).getLatitude(), 0);
		assertEquals( 47.7942768, listLocationEvent.get(10).getLatitude(), 0);
		assertEquals( 47.7942768, listLocationEvent.get(11).getLatitude(), 0);
		assertEquals( 47.7942768, listLocationEvent.get(12).getLatitude(), 0);
		
		assertEquals( -122.2030202, listLocationEvent.get(0).getLongitude(), 0);
		assertEquals( -122.2030202, listLocationEvent.get(1).getLongitude(), 0);
		assertEquals( -122.2030202, listLocationEvent.get(2).getLongitude(), 0);
		assertEquals( -122.2030202, listLocationEvent.get(3).getLongitude(), 0);
		assertEquals( -122.2030202, listLocationEvent.get(4).getLongitude(), 0);
		assertEquals( -122.2030345, listLocationEvent.get(5).getLongitude(), 0);
		assertEquals( -122.2030202, listLocationEvent.get(6).getLongitude(), 0);
		assertEquals( -122.2030345, listLocationEvent.get(7).getLongitude(), 0);
		assertEquals( -122.2030345, listLocationEvent.get(8).getLongitude(), 0);
		assertEquals( -122.2030345, listLocationEvent.get(9).getLongitude(), 0);
		assertEquals( -122.2030345, listLocationEvent.get(10).getLongitude(), 0);
		assertEquals( -122.2030345, listLocationEvent.get(11).getLongitude(), 0);
		assertEquals( -122.2030345, listLocationEvent.get(12).getLongitude(), 0);
	}
	
	
	@Test
	public void readData_InvalidLocationEvent() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(new String[] {
				"1.487031029351E9 ",
				"1.487031045307E9 47.7943242 -122.2030202 Loction network",
		});
		
		List<LocationEvent> listLocationEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(1.0, listLocationEvent.size(), 0);
	}

	/**
	 * Test shows a problem with code.<p>
	 * Parsing from String into a Number should have try/catch handling<li>
	 * See LocationEventReaderImpl.java
	 * @throws IOException
	 */
	@Ignore
	@Test(expected=Exception.class)
	public void readData_FailedToParse() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);

		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenReturn(new String[] {             
				"1.487031029351E9 47.794dummy3242 -122.2030202 Loction network",
		});
		
		List<LocationEvent> listLocationEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(0, listLocationEvent.size(), 0);
	}

	@Test
	public void readData_NoFile() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(false);

		List<LocationEvent> listLocationEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(0, listLocationEvent.size(), 0);
	}

	@Test
	public void readData_IOException() throws IOException {
		Mockito.when(filereader.fileExist(Mockito.anyString())).thenReturn(true);
		Mockito.when(filereader.readAllLine(Mockito.anyString())).thenThrow(new IOException("test exception"));
		List<LocationEvent> listLocationEvent = null;
		listLocationEvent = traceDataReader.readData(traceFolder, 0.0);
		assertEquals(0, listLocationEvent.size(), 0);
	}

}           
