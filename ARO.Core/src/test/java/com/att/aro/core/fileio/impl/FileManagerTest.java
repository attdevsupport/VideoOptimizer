package com.att.aro.core.fileio.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.android.ddmlib.AndroidDebugBridge;
import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.util.Util;

public class FileManagerTest extends BaseTest {
	
	@Spy	
	FileManagerImpl fileManager;
	boolean isfileclose = false;

	@Before
	public void setup() {
		fileManager = (FileManagerImpl) context.getBean(IFileManager.class);
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void getFileInputStream() throws Exception{
		FileInputStream fileInputStream = Mockito.mock(FileInputStream.class);
		PowerMockito.whenNew(FileInputStream.class).withArguments(Mockito.anyString()).thenReturn(fileInputStream);
		boolean result = fileManager.fileDirExist("parent/mockPath");
	}
	
	
	@Test
	public void findFilesByExtention() throws Exception{
		File file = Mockito.mock(File.class);
		File targetFile = Mockito.mock(File.class);
		
		PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(file);
		Mockito.when(file.getParentFile()).thenReturn(targetFile);
		Mockito.when(targetFile.exists()).thenReturn(true);
		
		String[] result = fileManager.findFilesByExtention("mockPath", "ext");
		assertTrue(result.length==0);
	}
	
	@Test
	public void directoryDeleteInnerFilesTest() throws Exception{
		String path="myTestFolder";
		File file = Mockito.mock(File.class);
		File directory = Mockito.mock(File.class);
		String[] fileList={"one","two","three"};
		PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(directory);
		Mockito.when(file.getParentFile()).thenReturn(directory);
		Mockito.when(directory.exists()).thenReturn(true);
		Mockito.when(directory.isDirectory()).thenReturn(true);
		Mockito.when(directory.list()).thenReturn(fileList);
		Mockito.when(directory.toString()).thenReturn(path);

		doReturn(directory).when(fileManager).createFile(path);
		doReturn(true).when(fileManager).deleteFile(Mockito.anyString());
		
		boolean result = fileManager.directoryDeleteInnerFiles(path);
		assertTrue(result);
	}
	
	@Test
	public void failed_directoryDeleteInnerFilesTest() throws Exception{
		String path="myTestFolder";
		File file = Mockito.mock(File.class);
		File directory = Mockito.mock(File.class);
		String[] fileList={"one","two","three"};
		PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(directory);
		Mockito.when(file.getParentFile()).thenReturn(directory);
		Mockito.when(directory.exists()).thenReturn(true);
		Mockito.when(directory.isDirectory()).thenReturn(true);
		Mockito.when(directory.list()).thenReturn(fileList);
		Mockito.when(directory.toString()).thenReturn(path);

		doReturn(directory).when(fileManager).createFile(path);
		doReturn(false).when(fileManager).deleteFile(Mockito.anyString());
		
		boolean result = fileManager.directoryDeleteInnerFiles(path);
		assertTrue(!result);
	}
	
	@Test
	public void badPath_directoryDeleteInnerFilesTest() throws Exception {
		String path = null;
		if (Util.OS_NAME.contains("Windows")) {
			path = "C:\\";
		} else {
			path = "/";
		}
		File file = Mockito.mock(File.class);
		File directory = Mockito.mock(File.class);
		String[] fileList = { "one", "two", "three" };
		PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(directory);
		Mockito.when(file.getParentFile()).thenReturn(directory);
		Mockito.when(directory.exists()).thenReturn(true);
		Mockito.when(directory.isDirectory()).thenReturn(true);
		Mockito.when(directory.list()).thenReturn(fileList);
		Mockito.when(directory.toString()).thenReturn(path);

		doReturn(directory).when(fileManager).createFile(path);
		doReturn(true).when(fileManager).deleteFile(Mockito.anyString());

		boolean result = fileManager.directoryDeleteInnerFiles(path);
		assertTrue(!result);
	}
	
	@Test
	public void renameFile() throws Exception {

		File origFileName = Mockito.mock(File.class);
		File newfullfile = Mockito.mock(File.class);
		File renameFile = Mockito.mock(File.class);

		String path = "myFullPath";
		String origFullPath = "myFullPath/o_filename";
		String newfileName = "new";


		doReturn(newfullfile).when(fileManager).createFile(path, newfileName);
		
		doReturn(path).when(origFileName).getParent();
		doReturn(false).when(newfullfile).exists();
		doReturn(true).when(origFileName).renameTo(newfullfile);

		boolean result = fileManager.renameFile(origFileName, newfileName);
		assertTrue(result);
	}

	
	@Test
	public void fileDirExist() throws Exception{
		File file = Mockito.mock(File.class);
		File targetFile = Mockito.mock(File.class);
		
		PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(file);
		Mockito.when(file.getParentFile()).thenReturn(targetFile);
		Mockito.when(targetFile.exists()).thenReturn(true);
		boolean result = fileManager.fileDirExist("parent/mockPath");
	}
	
	
	@Test
	public void mkDirTest() {
		File file = Mockito.mock(File.class);
		Mockito.when(file.exists()).thenReturn(false);
		Mockito.doAnswer(new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(file).mkdir();
		fileManager.mkDir(file);
	}

	@Test
	public void dirExistNotEmpty_1_Test() throws Exception {
		String[] files = new String[0];
		String sdir = "one";
		File dir = Mockito.mock(File.class);
			
		Mockito.when(dir.exists()).thenReturn(true);
		Mockito.when(dir.isDirectory()).thenReturn(true);
		Mockito.when(dir.list()).thenReturn(files);
		
		doReturn(dir).when(fileManager).createFile(sdir);

		boolean exist = fileManager.directoryExistAndNotEmpty(sdir);
		assertEquals(false, exist);

		files = new String[1];
		Mockito.when(dir.list()).thenReturn(files);
		exist = fileManager.directoryExistAndNotEmpty(sdir);
		assertEquals(true, exist);

		Mockito.when(dir.exists()).thenReturn(false);
		exist = fileManager.directoryExistAndNotEmpty(sdir);
		assertEquals(false, exist);
	}

	@Test
	public void dirExistNotEmpty_2_Test() {
		String[] files = new String[0];
		File dir = Mockito.mock(File.class);
		Mockito.when(dir.exists()).thenReturn(true);
		Mockito.when(dir.isDirectory()).thenReturn(true);
		Mockito.when(dir.list()).thenReturn(files);
		boolean exist = fileManager.directoryExistAndNotEmpty(dir);
		assertEquals(false, exist);

		files = new String[1];
		Mockito.when(dir.list()).thenReturn(files);
		exist = fileManager.directoryExistAndNotEmpty(dir);
		assertEquals(true, exist);

		Mockito.when(dir.exists()).thenReturn(false);
		exist = fileManager.directoryExistAndNotEmpty(dir);
		assertEquals(false, exist);
	}

	@Test
	public void closeFileTest() throws IOException {
		FileOutputStream output = Mockito.mock(FileOutputStream.class);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				isfileclose = true;
				return null;
			}
		}).when(output).close();
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				isfileclose = true;
				return null;
			}
		}).when(output).flush();
		fileManager.closeFile(output);
		assertTrue(isfileclose);
	}

	@Test
	public void readAllLineTest() throws IOException {

		BufferedReader buffreader = Mockito.mock(BufferedReader.class);
		Mockito.when(buffreader.readLine()).thenReturn("line1").thenReturn("line2").thenReturn(null);
		String[] lines = fileManager.readAllLine(buffreader);
		int count = lines.length;
		assertEquals(2, count);
	}

	@Test
	public void fileExistTest() {
		String currentdir = Util.getCurrentRunningDir();
		boolean exist = fileManager.fileExist(currentdir);
		assertTrue(exist);
		exist = fileManager.directoryExist(currentdir);
		assertTrue(exist);
		boolean isfile = fileManager.isFile(currentdir);
		assertFalse(isfile);
	}

	@Test
	public void listTest() {
		String currentdir = Util.getCurrentRunningDir();
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return true;
			}
		};
		String[] list = fileManager.list(currentdir, filter);
		boolean exist = list.length > 0;
		assertTrue(exist);
		list = fileManager.list(currentdir + "-not-found", filter);
		assertTrue(list.length == 0);
	}

	@Test
	public void getLastModifyTest() {
		String currentdir = Util.getCurrentRunningDir();
		long date = fileManager.getLastModified(currentdir);
		boolean ok = date > 0;
		assertTrue(ok);
	}

	@Test
	public void getDirectoryTest() {
		String currentdir = Util.getCurrentRunningDir();
		String dir = fileManager.getDirectory(currentdir);
		boolean found = dir != null;
		assertTrue(found);
		dir = fileManager.getDirectory(currentdir + "-not-found");
		found = dir == null;
		assertTrue(found);

		//get dir from a file
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return true;
			}
		};
		String[] list = fileManager.list(currentdir, filter);
		String file = list[0];
		dir = fileManager.getDirectory(currentdir + Util.FILE_SEPARATOR + file);
		found = dir != null;
		assertTrue(found);
	}
	
	
	@Test
	public void saveFile_resultIsNoError() throws IOException{
		InputStream istream = new ByteArrayInputStream(new byte[]{1,2});
		File mockFile = Mockito.mock(File.class);
		
		doReturn(mockFile).when(fileManager).createFile(any(String.class));
		Mockito.when(mockFile.exists()).thenReturn(true);
		Mockito.when(mockFile.createNewFile()).thenReturn(true);
		FileOutputStream outputStreamMock = mock(FileOutputStream.class);
		doReturn(outputStreamMock).when(fileManager).getFileOutputStream(any(String.class));
		
		fileManager.saveFile(istream, Util.getCurrentRunningDir());
	}
	
	@Test
	public void deleteFile_testresultIsFalse(){
		File mockFile = Mockito.mock(File.class);
		doReturn(mockFile).when(fileManager).createFile(any(String.class));
		doReturn(true).when(fileManager).fileExist(any(String.class));
		boolean testResult = fileManager.deleteFile(Util.getCurrentRunningDir());
		assertFalse(testResult);
	}
	
	@Test
	public void deleteFile_testresultIsTrue(){
		File mockFile = Mockito.mock(File.class);
		doReturn(mockFile).when(fileManager).createFile(any(String.class));
		doReturn(true).when(fileManager).fileExist(any(String.class));
		Mockito.when(mockFile.delete()).thenReturn(true);
		boolean testResult = fileManager.deleteFile(Util.getCurrentRunningDir());
		assertTrue(testResult);
	}
	
	@Test
	public void testGetCreatedTime() throws IOException {
		Date before = new Date();
		File createdFile = fileManager.createFile("test.txt");
		createdFile.createNewFile();
		Date after = new Date();
		long createdTime = fileManager.getCreatedTime(createdFile.getPath());
		createdFile.deleteOnExit();
		assertTrue(createdTime <= (after.getTime()/1000) && createdTime >= (before.getTime()/1000));
	}

}
