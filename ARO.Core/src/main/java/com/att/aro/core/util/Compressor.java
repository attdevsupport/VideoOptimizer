/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.cloud.State;
import com.att.aro.core.datacollector.pojo.StatusResult;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class Compressor implements Runnable {

	private String targetFolder;

	private ArrayList<String> excluded;

	private IResultSubscriber subscriber;

	private String fileName;

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final Logger LOG = LoggerFactory.getLogger(Compressor.class);
	
	public void prepare(IResultSubscriber subscriber, String targetFolder, String[] exclude, String fileName) {
		this.subscriber = subscriber;
		this.targetFolder = targetFolder;
		this.fileName = fileName;
		this.excluded = sArrayToArrayList(exclude);
	}
	
	@Override
	public void run() {
		LOG.error("preparePayload()");
		String bzip = null;
		try {
			bzip = zipBase64();
		} catch (Exception e1) {
			String error = "Exception :" + e1.getMessage();
			LOG.error(error);
			notifyListeners(new StatusResult(false, null, error));
			return;
		}

		File bZipped = new File(targetFolder, fileName);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(bZipped));
			out.write(bzip);
		} catch (Exception e) {
			String error = "Exception :" + e.getMessage();
			LOG.error(error);
			notifyListeners(new StatusResult(false, null, error));
			return;
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				LOG.error("Failed to close IOException", e);
			}
		}

		notifyListeners(new StatusResult(true, null, bZipped.toString()));
	}

	public void setSubscriber(IResultSubscriber subscriber) {
		this.subscriber = subscriber;
	}

	private void notifyListeners(StatusResult status) {
		subscriber.receiveResults(this.getClass(), status.isSuccess(), (String)status.getData());
	}
	
	/**
	 * String[] to ArrayList<String>
	 * 
	 * @param sArray
	 * @return
	 */
	private ArrayList<String> sArrayToArrayList(String[] sArray) {
		ArrayList<String> arrayList = new ArrayList<>(sArray.length);
		if (sArray.length > 0) {
			for (String file : sArray) {
				arrayList.add(file);
			}
		}
		return arrayList;
	}

	/**
	 * Zip a folder into Base64 format, exclude files listed in String array
	 * 
	 * @param targetFolder
	 * @param exclude
	 * @return
	 */
	public String zipBase64() throws Exception{
		File folder = new File(targetFolder);
		ArrayList<File> sourceFileList = new ArrayList<>();
		if (folder.exists()) {
			File[] fileList = folder.listFiles();
			if (fileList==null) {
				throw new Exception("Nothing to compress");
			}
			for (File file : fileList) {
				if (file.isFile() && !excluded.contains(file.getName())) {
					sourceFileList.add(file);
				}
			}
		}

		try {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 8)) {
				try (OutputStream baseos = Base64.getEncoder().wrap(baos)) {
					try (ZipOutputStream zos = new ZipOutputStream(baseos)) {
						for (File file : sourceFileList) {
							ZipEntry entry = new ZipEntry(file.getPath());
							zos.putNextEntry(entry);
							Files.copy(file.toPath(), zos);
							zos.closeEntry();
						}
					}
				}
				return baos.toString("ISO-8859-1");
			}
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to zip and wrap in base-64", ex);
		}
	}

	/**
	 * very very near future, coming soon
	 * Unpack downloaded payload to a trace folder
	 */
//	String unZipBase64(String b64) {
//		String path = "empty";
//		ByteArrayInputStream bais = new ByteArrayInputStream(b64.getBytes());
//		InputStream zis = Base64.getDecoder().wrap(bais);
//
//		return path;
//	}

	/**
	 * Create a zip file of a folder while filtering out certain folders/files
	 * 
	 * @param targetFolder
	 *            - folder to zip
	 * @param exclude
	 *            - String arry of files to skip/exclude
	 * @param zippedName
	 *            - name of resulting zip file
	 * @return
	 */
	public String zipFolder(String targetFolder, String[] exclude, String zippedName) {
		notifyListeners(new StatusResult(null, null, State.COMPRESSING));

		File folder = new File(targetFolder);
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null) {
			notifyListeners(new StatusResult(false, null, State.FAILURE));
			return "";
		}

		ArrayList<String> excluded = sArrayToArrayList(exclude);

		ArrayList<File> sourceFileList = new ArrayList<>();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				if (!excluded.contains(listOfFiles[i].getName())) {
					sourceFileList.add(listOfFiles[i]);
				}
			}
		}

		if (sourceFileList.size() <= 0) {
			notifyListeners(new StatusResult(false, null, State.FAILURE));
			return "";
		}

		ZipParameters parameters = new ZipParameters();
		parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
		parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
		ZipFile zipfile;
		try {
			zipfile = new ZipFile(targetFolder + FILE_SEPARATOR + zippedName);
			zipfile.addFiles(sourceFileList, parameters);
		} catch (ZipException e) {
			e.printStackTrace();
		}
		notifyListeners(new StatusResult(true, null, State.DONE));
		return targetFolder + FILE_SEPARATOR + zippedName;
	}

}