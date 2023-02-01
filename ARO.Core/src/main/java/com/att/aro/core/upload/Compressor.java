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
package com.att.aro.core.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.cloud.State;
import com.att.aro.core.cloud.TraceManager;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.util.IResultSubscriber;
import com.att.aro.core.util.Util;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

public class Compressor implements Runnable {
	private String targetFolder;
	private IResultSubscriber subscriber;

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final Logger LOG = LogManager.getLogger(Compressor.class);
	
	public void prepare(IResultSubscriber subscriber, String targetFolder, String[] exclude, String fileName) {
		this.subscriber = subscriber;
		this.targetFolder = targetFolder;
	}
	
	@Override
	public void run() {
		LOG.info("preparingPayload");
		notifyListeners(new StatusResult(null, null, State.COMPRESSING.toString() + ": " + new File(targetFolder).getName())); // message status update
		String base64WrappedZipFile = null;
		try {
			base64WrappedZipFile = zipBase64();
		} catch (Exception e1) {
			String error = "Exception :" + e1.getMessage();
			LOG.error(error);
			notifyListeners(new StatusResult(false, null, error)); // message status failed
			return;
		}
		notifyListeners(new StatusResult(true, null, base64WrappedZipFile)); // message completed success, zip base64 filename
	}

	public void setSubscriber(IResultSubscriber subscriber) {
		this.subscriber = subscriber;
	}

	private void notifyListeners(StatusResult status) {
		if (subscriber != null) {
			LOG.debug(String.format("%s : notifyListeners(%s, %B, %s)"
							, subscriber.getClass().getName()
							, this.getClass().getName()
							, status.isSuccess()
							, status.getData().toString()));
			subscriber.receiveResults(this.getClass(), status.isSuccess(), status.getData().toString());
		}
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
		TraceManager tm = new TraceManager(null);
		delete(".zip");		
		delete(".zip64");
		String zipPath = tm.compress(targetFolder);
		return encode(zipPath);
	}

	private void delete(String fileExt) {
		File file = new File(targetFolder + Util.FILE_SEPARATOR + folderName(targetFolder) + fileExt);
		if (file.exists()) {
			file.delete();
		}
	}
	
	private String folderName(String trace) {
		String pattern = Pattern.quote(FILE_SEPARATOR);
		String[] split = trace.split(pattern);
		String traceName = trace;
		if (split.length > 0) {
			traceName = split[split.length - 1];
		}
 		return traceName;
	}

	public String encode(String zipPath) throws IOException {
		String zip64 = zipPath + "64";
		Encoder encoder = Base64.getEncoder();
		try (FileOutputStream fos = new FileOutputStream(zip64);
				FileInputStream fis = new FileInputStream(new File(zipPath))) {
			int size;
			final int capacity = 3 * 1024 * 1024;// Keep it mutliple of 3 so it could be decoded 4/3 size
			byte[] buffer = new byte[capacity];
			while ((size = fis.read(buffer)) != -1) {
				if(size < capacity) {
					fos.write(encoder.encode(Arrays.copyOfRange(buffer, 0, size)));
				} else {
					fos.write(encoder.encode(buffer));
				}
			}
			fos.flush();
		}
		return zip64;
	}
	
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
		parameters.setCompressionMethod(CompressionMethod.DEFLATE);
		parameters.setCompressionLevel(CompressionLevel.ULTRA);
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