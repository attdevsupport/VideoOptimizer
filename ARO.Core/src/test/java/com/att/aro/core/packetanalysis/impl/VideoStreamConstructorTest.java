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
package com.att.aro.core.packetanalysis.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.fileio.IFileManager;
import com.google.common.io.Files;

public class VideoStreamConstructorTest {

	ApplicationContext context = SpringContextUtil.getInstance().getContext();
	VideoStreamConstructor videoStreamConstructor = context.getBean(VideoStreamConstructor.class);
	IFileManager filemanager = context.getBean(IFileManager.class);
	
	File tempFolder = Files.createTempDir();
	String pathName1 = tempFolder + "/file1";
	String pathName1exten = tempFolder + "/file1.xyz";
	byte[] content = "dummy data".getBytes();
	
	@Before
	public void init() {
		videoStreamConstructor.savePayload(content, pathName1);
		videoStreamConstructor.savePayload(content, pathName1exten);
	}
	
	@After
	public void destroy() {
		filemanager.directoryDeleteInnerFiles(tempFolder.toString());
		filemanager.deleteFile(tempFolder.toString());
	}

	@Test
	public void testFindPathNameTiebreaker_when_no_duplicates() throws Exception {
		String pathName = tempFolder + "/fileNotThere";
		String incrementedName = videoStreamConstructor.findPathNameTiebreaker(pathName);
		assertThat(incrementedName).isEqualTo(pathName);

		System.out.println("tempFolder :" + tempFolder);
	}

	@Test
	public void testFindPathNameTiebreaker_when_noExtension() throws Exception {
		String incrementedName = videoStreamConstructor.findPathNameTiebreaker(pathName1);
		assertThat(incrementedName).isEqualTo(pathName1 + "(001)");
	}

	@Test
	public void testFindPathNameTiebreaker_when_Extension() throws Exception {
		String incrementedName = videoStreamConstructor.findPathNameTiebreaker(pathName1 + ".xyz");
		assertThat(incrementedName).isEqualTo(pathName1 + "(001).xyz");
	}

	@Test
	public void testFindPathNameTiebreaker_when_201_duplicate_names() throws Exception {
		String pathName = tempFolder + "/file.xyz";
		byte[] content = pathName.getBytes();
		for (int idx = 0; idx < 200; idx++) {
			videoStreamConstructor.savePayload(content, pathName);
		}
		String incrementedName = videoStreamConstructor.findPathNameTiebreaker(pathName);
		assertThat(incrementedName).isEqualTo(tempFolder + "/file.xyz(duplicated)");
	}

	@Test
	public void testShortenNameByParts() throws Exception {
		String fullString = "one:two:three:four:five";
		String shortString = videoStreamConstructor.shortenNameByParts(fullString, ":", 2);
		assertThat(shortString).isEqualTo("four:five");
	}

	@Test
	public void testShortenNameByParts_when_full_count() throws Exception {
		String fullString = "one:two:three:four:five";
		String shortString = videoStreamConstructor.shortenNameByParts(fullString, ":", 5);
		assertThat(shortString).isEqualTo("one:two:three:four:five");
	}

	@Test
	public void testShortenNameByParts_when_over_count() throws Exception {
		String fullString = "one:two:three:four:five";
		String shortString = videoStreamConstructor.shortenNameByParts(fullString, ":", 6);
		assertThat(shortString).isEqualTo("one:two:three:four:five");
	}

	@Test
	public void testShortenNameByParts_when_no_count() throws Exception {
		String fullString = "one:two:three:four:five";
		String shortString = videoStreamConstructor.shortenNameByParts(fullString, ":", 0);
		assertThat(shortString).isEqualTo("one:two:three:four:five");
	}


}

