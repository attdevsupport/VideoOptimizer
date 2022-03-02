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
package com.att.aro.core.peripheral.impl;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.IMetaDataReadWrite;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetaDataReadWrite implements IMetaDataReadWrite {	
	private static final Logger LOG = LogManager.getLogger(MetaDataReadWrite.class.getName());
	
	@Autowired private IFileManager filemanager;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private static final String JSON_FILE = "metadata.json";

	private MetaDataModel metaData;

	private String jsonDataSaved = ""; // for comparison purposes before saving, do not want to save if no changes to file

	@Override
	public MetaDataModel readData(String tracePath) {

		File jsonFile = filemanager.createFile(tracePath, JSON_FILE);
		if (jsonFile.exists()) {
				try {
					String jsonDataString = filemanager.readAllData(jsonFile.toString());
					metaData = mapper.readValue(jsonDataString, MetaDataModel.class);
					jsonDataSaved = serialize(metaData);
				} catch (Exception e) {
					LOG.debug("failed to load time-range data: " + e.getMessage());
				}
		} else {
			metaData = null;
		}
		return metaData;
	}

	@Override
	public boolean save(File traceFolder, MetaDataModel metaDataModel) throws Exception {
		
		if (metaDataModel != null) {
			File jsonFile = filemanager.createFile(traceFolder, JSON_FILE);
			String jsonData = serialize(metaDataModel);
			if (jsonData.equals(jsonDataSaved)) {
				LOG.debug("MetaDataModel has no changes, so not saving");
				return true;
			}
			this.metaData = metaDataModel;
			String jsonPath = null;
			
			jsonPath = jsonFile.toString();
			
			if (!jsonData.isEmpty() && jsonPath != null && !jsonPath.isEmpty()) {
				FileOutputStream output = new FileOutputStream(jsonPath);
				output.write(jsonData.getBytes());
				output.flush();
				output.close();
				LOG.debug("MetaDataModel saved");
				jsonDataSaved = jsonData;
				return true;
			}
		}
		return false;
	}

	private String serialize(MetaDataModel metaDataModel) throws JsonProcessingException {
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataModel);
	}

}
