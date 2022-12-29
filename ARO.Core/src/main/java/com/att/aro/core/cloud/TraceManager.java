/*
 *  Copyright 2017 AT&T
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.apache.commons.collections4.CollectionUtils;

import com.amazonaws.services.s3.transfer.Transfer.TransferState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;


@SuppressFBWarnings({ "NP_UNWRITTEN_FIELD", "UWF_UNWRITTEN_FIELD" })
public class TraceManager {
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	private Repository repository;
	private Collection<Listener> listeners;

	public TraceManager(){
		
	}
  
    public TraceManager(Repository repository) {
		this.repository = repository;
		listeners = new ArrayList<Listener>();
	}
 	public State upload(String trace) {
		clean(trace);
		String zipFile = compress(trace);
		File file = new File(zipFile);
		TransferState state = repository.put(file);
		if(state ==TransferState.Completed) {
			removeZip(zipFile);
			return State.COMPLETE;	
		}else {
			return State.FAILURE;
		}
 	}

	public String compress(String trace) {
		notifyListeners(State.COMPRESSING);

		File folder = new File(trace);
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null) {
			notifyListeners(State.FAILURE);
			return "";
		}
		
		ArrayList<File> sourceFileList = new ArrayList<>();
		for (int i = 0; i < listOfFiles.length; i++) {
			String fileName = listOfFiles[i].getName();
			if (listOfFiles[i].isFile() && !fileName.endsWith("mp4") && !fileName.endsWith("mov")) {
				sourceFileList.add(listOfFiles[i]);
			}
		}
		if(sourceFileList.size() <= 0) {
			notifyListeners(State.FAILURE);
			return "";
		}
		ZipParameters parameters = new ZipParameters();
		parameters.setCompressionMethod(CompressionMethod.DEFLATE);
		parameters.setCompressionLevel(CompressionLevel.ULTRA);
		String zipFileName = folderName(trace);
		ZipFile zipfile;
		zipfile = new ZipFile(trace + FILE_SEPARATOR + zipFileName + ".zip");
		try {
			zipfile.addFiles(sourceFileList, parameters);
		} catch (net.lingala.zip4j.exception.ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		notifyListeners(State.DONE);
		return trace + FILE_SEPARATOR + zipFileName + ".zip";
	}

	public State download(String remoteSelectedTrace, String saveTo) {

 		String path = repository.get(remoteSelectedTrace, saveTo);
 		if(path == null) {
 			return State.FAILURE;
 		}
 		String folderName = "RemoteTrace";
 		if(remoteSelectedTrace!=null && remoteSelectedTrace.length()>4) {
 			folderName = remoteSelectedTrace.substring(0,remoteSelectedTrace.length()-4);	// .zip 
 		}
		try {
			unZip(path,folderName);
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		removeZip(path);
		return State.COMPLETE;
	}

	public void unZip(String zipFile, String saveTo) throws ZipException {
		notifyListeners(State.UNCOMPRESSING);
		if (zipFile == null) {
			return;
		}
		File tempFile = new File(zipFile);
		ZipFile zipfile = new ZipFile(zipFile);
		String savedFolder = tempFile.getParent() + FILE_SEPARATOR + saveTo;
		File folder = new File(savedFolder);
		folder.mkdir();
		try {
			zipfile.extractAll(savedFolder);
		} catch (net.lingala.zip4j.exception.ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		notifyListeners(State.DONE);

	}

	private void clean(String trace) {
		notifyListeners(State.PROCESSING);
	}

	private void removeZip(String zip) {
		if (zip == null) {
			return;
		}
		// Initiate ZipFile object with the path/name of the zip file.
		File zipFile = new File(zip);
		zipFile.delete();

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

	private void notifyListeners(State state) {
		if (CollectionUtils.isNotEmpty(listeners)) {
			listeners.forEach((l) -> l.stateChanged(state));
		}
	}

	public void addListener(Listener l) {
 			listeners.add(l);
 	}

	public void removeListener(Listener l) {
		listeners.remove(l);
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}
