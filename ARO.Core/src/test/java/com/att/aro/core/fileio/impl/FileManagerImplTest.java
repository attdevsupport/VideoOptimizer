package com.att.aro.core.fileio.impl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.fileio.IFileManager;

public class FileManagerImplTest{
	
	File tempDirectory;
	
	FileManagerImpl fileManager = (FileManagerImpl) SpringContextUtil.getInstance().getContext().getBean(IFileManager.class);

	boolean isfileclose = false;

	private File pcapFile;

	private String traceFolder;

	private String trafficFile;

	private File tempFolder;

	private File traceFolderFile;
	
	@Before
	public void setup() {
		if (tempFolder == null) {
			try {
				tempFolder = Files.createTempDirectory(null).toFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			traceFolderFile = new File(tempFolder, "traceFolder");
			traceFolderFile.mkdir();
			traceFolder = traceFolderFile.toString();
		}
		fileManager.deleteFolderContents(traceFolder);
		pcapFile = fileManager.createEmptyFile(traceFolder, "traffic.pcap");
		trafficFile = pcapFile.toString();
		MockitoAnnotations.initMocks(this);
	}
	
	@After
	public void tearDown() {
		org.assertj.core.util.Files.delete(tempFolder);
	}

	byte[] sampledata = new byte[] { 1, 2, 3, 5, 7, 11, 13, 17};

	@Test
	public void testMove_Valid_Source_Destination() throws Exception {
		String destinationFolder = traceFolder + "/wrap";
		fileManager.mkDir(destinationFolder);

		String sourceFile = trafficFile;
		String destination = fileManager.createFile(destinationFolder, pcapFile.getName()).toString();

		boolean success = fileManager.move(sourceFile, destination);
		assertTrue(success);
	}

	@Test
	public void testCopy_Valid_Source_Destination() throws Exception {
		String destinationFolder = traceFolder + "/wrap";
		fileManager.mkDir(destinationFolder);
		String sourceFile = trafficFile;
		String destination = fileManager.createFile(destinationFolder, pcapFile.getName()).toString();

		boolean success = fileManager.copy(sourceFile, destination);
		assertTrue(success);
	}
	
	@Test
	public void testCreateLink_Valid_Source_Destination() throws Exception {
		String destinationFolder = traceFolder + "/wrap";
		fileManager.mkDir(destinationFolder);

		String target = trafficFile;
		String link = fileManager.createFile(destinationFolder, pcapFile.getName()).toString();

		boolean success = fileManager.createLink(link, target);
		assertTrue(success);
	}
	
	@Test
	public void testCreateSymbolicLink_Valid_Source_Destination() throws Exception {
		String destinationFolder = traceFolder + "/wrap";
		fileManager.mkDir(destinationFolder);

		String target = trafficFile;
		String link = fileManager.createFile(destinationFolder, pcapFile.getName()).toString();

		boolean success = fileManager.createSymbolicLink(link, target);
		assertTrue(success);
	}
}
