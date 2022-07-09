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
package com.att.aro.core.cloud;

import static org.mockito.Matchers.any;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.amazonaws.ClientConfiguration;
import com.att.aro.core.cloud.aws.AwsRepository;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

public class TraceManagerTest {
	private String trace = "trace";

	@Mock
	private AwsRepository repository;
	
	@Mock
	private IFileManager filemanager;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	TraceManager manager;

	String currentdir;

	@Before
	public void setUp() throws Exception {
		repository = Mockito.mock(AwsRepository.class);
		filemanager = Mockito.mock(FileManagerImpl.class);		
		manager = new TraceManager(repository);
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void tearDown() throws Exception {
		folder.delete();
	}

	@Test
	public void testDownload() {
 		manager.download("", "");
	}

	@Test
	public void testDownloadwithFile() {
		ZipParameters parameters = new ZipParameters();
		ZipFile zipFile;
		try {
			File testfile = folder.newFile("temp.txt");
			zipFile = new ZipFile(folder.getRoot() + System.getProperty("file.separator") + "temp.zip");
			zipFile.addFile(testfile, parameters);
		} catch (IOException e) {
			e.printStackTrace();
		}

		manager.setRepository(repository);
		Mockito.when(repository.get(any(String.class), any(String.class)))
				.thenReturn(folder.getRoot() + System.getProperty("file.separator") + "temp.zip");
 		manager.download(trace, "folder");
	}

	@Test
	public void testUpload() {
		manager.upload("");
	}

	@Test
	public void testUploadWithFile() {
		try {
			File tracefolder = folder.newFolder("trace");
			File testfile = new File(folder.getRoot() + System.getProperty("file.separator") + tracefolder.getName()
					+ System.getProperty("file.separator") + "temp.txt");
			testfile.createNewFile();
			manager.setRepository(repository);
			manager.upload(tracefolder.getAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testList() {
		new AwsRepository(new HashMap<>(),new ClientConfiguration()).list();
	}

}
