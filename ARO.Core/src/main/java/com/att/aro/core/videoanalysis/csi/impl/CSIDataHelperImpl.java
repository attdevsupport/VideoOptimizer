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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis.csi.impl;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.videoanalysis.csi.ICSIDataHelper;
import com.att.aro.core.videoanalysis.csi.pojo.CSIManifestAndState;

public class CSIDataHelperImpl implements ICSIDataHelper {
	
	private static final Logger LOGGER = LogManager.getLogger(CSIDataHelperImpl.class.getName());
	
	private static final String JSON_FILE = "CSIState.json";

	@Autowired
	private IFileManager fileManager;
	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public CSIManifestAndState readData(String tracePath) {
		File jsonFile = fileManager.createFile(tracePath, JSON_FILE);
		try {
			if (jsonFile.exists()) {
				return mapper.readValue(fileManager.readAllData(jsonFile.toString()), CSIManifestAndState.class);
			}
		} catch (IOException ioe) {
			LOGGER.debug ("IOException thrown while reading CSI State File", ioe);
		}
		return new CSIManifestAndState();
	}

	@Override
	public void saveData(String manifestath, CSIManifestAndState CSIData) {
		try {
			fileManager.saveFile(manifestath + System.getProperty("file.separator") + JSON_FILE, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(CSIData).getBytes());
		} catch (IOException ioe) {
			LOGGER.debug ("IOException thrown while saving CSI State File", ioe);
		}
	}

	@Override
	public boolean doesCSIFileExist(String tracePath) {
		return fileManager.createFile(tracePath + System.getProperty("file.separator") + "CSI", JSON_FILE).exists();
	}

	@Override
	public File generateManifestPath(String traceDirectory, String fileName) {
		return fileManager.createFile(traceDirectory + System.getProperty("file.separator") + "CSI", fileName);
	}
	
}
