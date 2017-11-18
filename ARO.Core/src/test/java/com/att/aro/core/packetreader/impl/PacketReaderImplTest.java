package com.att.aro.core.packetreader.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.att.aro.core.BaseTest;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.packetreader.INativePacketSubscriber;
import com.att.aro.core.packetreader.IPacketListener;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.pcap.PCapAdapter;

public class PacketReaderImplTest extends BaseTest {

	File file;
	IPacketListener listener;
	PCapAdapter adapter;
	PacketReaderImpl reader;
	PcapngHelperImpl pcapNgHelper;
	ExternalProcessRunnerImpl extrunner;
	
	FileManagerImpl mockFileManager = Mockito.mock(FileManagerImpl.class);
	File tempFile = Mockito.mock(File.class);
	File backupCap = Mockito.mock(File.class);
	File tempCap = Mockito.mock(File.class);
	
	Packet tPacket = null;
	
	@Before
	public void setup() {
		
		reader = (PacketReaderImpl) context.getBean("packetReader");
		
		adapter = Mockito.mock(PCapAdapter.class);
		Mockito.doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(adapter).setSubscriber(Mockito.any(INativePacketSubscriber.class));
		Mockito.when(adapter.readData(Mockito.anyString())).thenReturn(null);

		file = Mockito.mock(File.class);
		Mockito.when(file.getAbsolutePath()).thenReturn("myPath/traffic.cap");

		listener = Mockito.mock(IPacketListener.class);
		Mockito.doNothing().when(listener).packetArrived(Mockito.anyString(), Mockito.any(Packet.class));

		pcapNgHelper = Mockito.mock(PcapngHelperImpl.class);
		extrunner = Mockito.mock(ExternalProcessRunnerImpl.class);
		try {
			Mockito.doAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					return true;
				}
			}).when(pcapNgHelper).isApplePcapng(Mockito.any(File.class));
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		
		Mockito.when(mockFileManager.createFile("myPath/traffic.cap")).thenReturn(file);
		Mockito.when(file.getAbsolutePath()).thenReturn("myPath/traffic.cap");
		Mockito.when(file.getName()).thenReturn("traffic.cap");
		Mockito.when(file.toString()).thenReturn("traffic.cap");
		Mockito.when(file.delete()).thenReturn(true);
		Mockito.when(file.exists()).thenReturn(true);
		
		// tempFile
		String myPathTest = "mypath/test";
		Mockito.when(mockFileManager.createFile(myPathTest)).thenReturn(tempFile);
		Mockito.when(tempFile.getAbsolutePath()).thenReturn("myPath/test");
		Mockito.when(tempFile.getName()).thenReturn("test");
		Mockito.when(tempFile.toString()).thenReturn("test");
		Mockito.when(tempFile.delete()).thenReturn(true);
		Mockito.when(tempFile.exists()).thenReturn(true);
		
		// backupCap
		String backupCapName = "backup.cap";
		Mockito.when(mockFileManager.createFile("myPath/", backupCapName)).thenReturn(backupCap);
		Mockito.when(backupCap.getAbsolutePath()).thenReturn(backupCapName);
		Mockito.when(backupCap.getName()).thenReturn(backupCapName);
		Mockito.when(backupCap.toString()).thenReturn(backupCapName);
		Mockito.when(backupCap.delete()).thenReturn(true);
		Mockito.when(backupCap.exists()).thenReturn(false);		
		
		// tempCap
		String tempCapName = "temp.cap";
		Mockito.when(mockFileManager.createFile(tempCapName)).thenReturn(tempCap);
		Mockito.when(mockFileManager.createFile("myPath/", tempCapName)).thenReturn(tempCap);
		Mockito.when(tempCap.getAbsolutePath()).thenReturn("myPath/tempCap");
		Mockito.when(tempCap.getName()).thenReturn("temp.cap");
		Mockito.when(tempCap.toString()).thenReturn("temp.cap");
		Mockito.when(tempCap.delete()).thenReturn(true);
		Mockito.when(tempCap.exists()).thenReturn(false);		

	}

	@Test
	public void setAroJpcapLibNameTest() {
		reader.setAroJpcapLibName("Windows", "64");
		String libname = reader.getAroJpcapLibFileName();
		assertEquals("jpcap64.dll", libname);

		reader.setAroJpcapLibName("Windows", "86");
		libname = reader.getAroJpcapLibFileName();
		assertEquals("jpcap.dll", libname);

		reader.setAroJpcapLibName("Linux", "amd64");
		libname = reader.getAroJpcapLibFileName();
		assertEquals("libjpcap64.so", libname);

		reader.setAroJpcapLibName("Linux", "i386");
		libname = reader.getAroJpcapLibFileName();
		assertEquals("libjpcap32.so", libname);

		reader.setAroJpcapLibName("MacOS", "64");
		libname = reader.getAroJpcapLibFileName();
		assertEquals("libjpcap.jnilib", libname);

		reader.setAroJpcapLibName(null, null);

	}

	@Test
	public void setAroWebPLib() {
		reader.setAroWebPLib("Windows", "64");
		String libname = reader.getAroWebPLibFileName();
		assertEquals("webp-imageio.dll", libname);

		reader.setAroWebPLib("Windows", "86");
		libname = reader.getAroWebPLibFileName();
		assertEquals("webp-imageio32.dll", libname);

		reader.setAroWebPLib("Linux", "amd64");
		libname = reader.getAroWebPLibFileName();
		assertEquals("libwebp-imageio.so", libname);

		reader.setAroWebPLib("Linux", "i386");
		libname = reader.getAroWebPLibFileName();
		assertEquals("libwebp-imageio32.so", libname);

		reader.setAroWebPLib("MacOS", "64");
		libname = reader.getAroWebPLibFileName();
		assertEquals("libwebp-imageio.dylib", libname);

		reader.setAroWebPLib(null, null);
		libname = reader.getAroWebPLibFileName();
		assertEquals("libwebp-imageio.dylib", libname);

	}

	@Test
	public void readPacket_PcapNG() throws IOException {

		listener = Mockito.mock(IPacketListener.class);

		reader.setAdapter(adapter);
				
		ReflectionTestUtils.setField(reader, "filemanager", mockFileManager);
		ReflectionTestUtils.setField(reader, "pcapngHelper", pcapNgHelper);
		ReflectionTestUtils.setField(reader, "extrunner", extrunner);

		Mockito.when(extrunner.executeCmd(Mockito.anyString())).thenReturn("mocked shell response");
		Mockito.when(pcapNgHelper.isApplePcapng(Mockito.any(File.class))).thenReturn(true);
		Mockito.when(mockFileManager.renameFile(Mockito.any(File.class), Mockito.anyString())).thenReturn(true);

		Mockito.when(tempCap.exists()).thenReturn(true);		
		
		reader.readPacket(file.getAbsolutePath(), new IPacketListener() {
			
			@Override
			public void packetArrived(String appName, Packet packet) {
				tPacket = packet;
			}
		});

		String nativeLibname = reader.getAroJpcapLibFileName();
		
		tPacket = null;
		byte[] data = new byte[20];
		reader.receive(12, 10, 11, 12, data);

		assertNotEquals(tPacket, null);
		assertEquals(tPacket.getMicroSeconds(), 11L);
		assertNotEquals(nativeLibname, null);
	}


	@Test
	public void readPacket() throws IOException {

		listener = Mockito.mock(IPacketListener.class);

		reader.setAdapter(adapter);


//		File tempFile = Mockito.mock(File.class);
//		File tempFile2 = Mockito.mock(File.class);

		ReflectionTestUtils.setField(reader, "filemanager", mockFileManager);
		ReflectionTestUtils.setField(reader, "pcapngHelper", pcapNgHelper);
		ReflectionTestUtils.setField(reader, "extrunner", extrunner);

		Mockito.when(extrunner.executeCmd(Mockito.anyString())).thenReturn("mocked shell response");
		Mockito.when(pcapNgHelper.isApplePcapng(Mockito.any(File.class))).thenReturn(false);

		Mockito.when(mockFileManager.createFile(Mockito.anyString(), Mockito.anyString())).thenReturn(backupCap);
		Mockito.when(mockFileManager.createFile("mypath:test")).thenReturn(tempFile);
		Mockito.when(mockFileManager.renameFile(Mockito.any(File.class), Mockito.anyString())).thenReturn(true);

		// file
		Mockito.when(file.getAbsolutePath()).thenReturn("mypath:test");
		Mockito.when(tempFile.getAbsolutePath()).thenReturn("myPath/test");
		Mockito.when(tempFile.getName()).thenReturn("test");
		Mockito.when(tempFile.delete()).thenReturn(true);
		Mockito.when(tempFile.exists()).thenReturn(true);
		
		// file2
		Mockito.when(backupCap.getAbsolutePath()).thenReturn("myPath/test2");
		Mockito.when(backupCap.getName()).thenReturn("test2");
		Mockito.when(backupCap.delete()).thenReturn(true);
		Mockito.when(backupCap.exists()).thenReturn(false);
		
		reader.readPacket(file.getAbsolutePath(), new IPacketListener() {

			@Override
			public void packetArrived(String appName, Packet packet) {
				tPacket = packet;
			}
		});

		String nativeLibname = reader.getAroJpcapLibFileName();

		byte[] data = new byte[20];
		reader.receive(12, 1, 2, 3, data);

		assertNotEquals(tPacket, null);
		assertEquals(tPacket.getMicroSeconds(), 2L);
		assertNotEquals(nativeLibname, null);
	}

	@Test
	public void readPacketNoListener() throws IOException {
		listener = null;
		reader.setAdapter(adapter);
		try {
			reader.readPacket(file.getAbsolutePath(), listener);
		} catch (Exception e) {

		}
		reader.getAroJpcapLibFileName();
		byte[] data = new byte[20];
		reader.receive(12, 1, 1, 1, data);
	}

	@Test(expected = IllegalArgumentException.class)
	public void readPacketError() throws IOException {
		reader.setAdapter(adapter);
		reader.readPacket(file.getAbsolutePath(), null);
		assertTrue(false);
	}

	@Test(expected = IOException.class)
	public void readPacketError2() throws IOException {
		reader.setAdapter(adapter);
		Mockito.when(adapter.readData(Mockito.anyString())).thenReturn("not null");
		
		reader.readPacket(file.getAbsolutePath(), listener);
		assertTrue(false);
	}

	@Test
	public void getAroWebPLibFileName_not_set() throws IOException {
		reader.setAdapter(adapter);
		String name = reader.getAroWebPLibFileName();
		assertNotEquals(name, null);
	}

}
