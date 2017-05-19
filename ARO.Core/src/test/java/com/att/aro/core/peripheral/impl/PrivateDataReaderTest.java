package com.att.aro.core.peripheral.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.IPrivateDataReader;
import com.att.aro.core.peripheral.pojo.PrivateDataInfo;

public class PrivateDataReaderTest extends BaseTest {

	PrivateDataReaderImpl reader;
	
	private IFileManager fileReader;
	private String traceFolder = "folder";
	
	@Before
	public void setUp() throws Exception {
		fileReader = Mockito.mock(IFileManager.class);
		reader = (PrivateDataReaderImpl) context.getBean(IPrivateDataReader.class);
		reader.setFileReader(fileReader);
	}

	@Test
	public void testFileNotExist() {
		Mockito.when(fileReader.fileExist(traceFolder)).thenReturn(false);
		List<PrivateDataInfo> infos = reader.readData(traceFolder);
		assertNotNull(infos);
		assertEquals(0, infos.size());
	}

	@Test
	public void testReadFile() throws IOException {
		Mockito.when(fileReader.fileExist(Mockito.anyString())).thenReturn(true);
		Mockito.when(fileReader.readAllLine(Mockito.anyString())).thenReturn(new String[] {
				"KEYWORD,Phone Type,Android,Y"
		});
		
		List<PrivateDataInfo> infos = reader.readData(traceFolder);
		
		assertNotNull(infos);
		assertEquals(1, infos.size());
		assertEquals("KEYWORD", infos.get(0).getCategory());
		assertEquals("Phone Type", infos.get(0).getType());
		assertEquals("Android", infos.get(0).getValue());
		assertEquals(true, infos.get(0).isSelected());
	}
}
