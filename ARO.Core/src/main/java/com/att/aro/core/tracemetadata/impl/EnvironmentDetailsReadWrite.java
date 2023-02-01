/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.tracemetadata.impl;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.datacollector.pojo.EnvironmentDetails;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.tracemetadata.IEnvironmentDetailsReadWrite;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

public class EnvironmentDetailsReadWrite implements IEnvironmentDetailsReadWrite {	
	private static final Logger LOG = LogManager.getLogger(EnvironmentDetailsReadWrite.class.getName());
	
	@Autowired 
	private IFileManager filemanager;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private static final String JSON_FILE = "environment_details.json";

	@Getter
	private EnvironmentDetails environmentDetails;

	private String jsonDataSaved = ""; // for comparison purposes before saving, do not want to save if no changes to file

	private String tracePath;
	
	@Override
	public EnvironmentDetails readData(String tracePath) {
		File jsonFile = filemanager.createFile(tracePath, JSON_FILE);
		this.tracePath = tracePath;
		if (jsonFile.exists()) {
			try {
				String jsonDataString = filemanager.readAllData(jsonFile.toString());
				if ((jsonDataSaved.replaceAll("\n", "").compareTo(jsonDataString.replaceAll("\n", ""))) != 0) {
					environmentDetails = mapper.readValue(jsonDataString, EnvironmentDetails.class);
					jsonDataSaved = jsonDataString;
				}
			} catch (Exception e) {
				LOG.debug("failed to load environment_details.json :" + e.getMessage());
			}
		} else {
			environmentDetails = new EnvironmentDetails();
			save();
			jsonDataSaved = "";
		}
		return environmentDetails;
	}

	@Override
	public boolean save() {
		if (tracePath != null) {
			try {
				return save(new File(tracePath), environmentDetails);
			} catch (Exception e) {
				LOG.error("Failed to store JSON", e);
				e.printStackTrace();
			}
		}
		return false;
	}
			
	@Override
	public boolean save(File traceFolder, EnvironmentDetails environmentDetails) throws Exception {
		
		if (environmentDetails != null) {
			File jsonFile = filemanager.createFile(traceFolder, JSON_FILE);
			String jsonData = serialize(environmentDetails);
			if ((jsonData.compareTo(jsonDataSaved)) == 0) {
				LOG.debug("EnvironmentDetails has no changes, so not saving");
				return true;
			}
			this.environmentDetails = environmentDetails;
			String jsonPath = null;
			
			jsonPath = jsonFile.toString();
			
			if (!jsonData.isEmpty() && jsonPath != null && !jsonPath.isEmpty()) {
				FileOutputStream output = new FileOutputStream(jsonPath);
				output.write(jsonData.getBytes());
				output.flush();
				output.close();
				LOG.debug("EnvironmentDetails saved");
				jsonDataSaved = jsonData;
				return true;
			}
		}
		return false;
	}

	private String serialize(EnvironmentDetails metaDataModel) throws JsonProcessingException {
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataModel);
	}

}
