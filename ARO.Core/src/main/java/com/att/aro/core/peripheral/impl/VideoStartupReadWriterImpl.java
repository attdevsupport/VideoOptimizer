/*
 *  Copyright 2018, 2021 AT&T
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
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.peripheral.IVideoStartupReadWrite;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup;
import com.att.aro.core.peripheral.pojo.VideoStreamStartupData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Load and Save StartupDelay data, from jason file video_stream_startup.json
 */
public class VideoStartupReadWriterImpl extends PeripheralBase implements IVideoStartupReadWrite {

	private static final Logger LOG = LogManager.getLogger(VideoStartupReadWriterImpl.class.getName());

	@Autowired
	private IFileManager filemanager;
	
	private static final String JSON_FILE = "video_stream_startup.json";

	private ObjectMapper mapper = new ObjectMapper();

	private VideoStreamStartupData videoStreamStartupData;

	private String jsonDataSaved = ""; // for comparison purposes before saving, do not want to save if no changes to file
	
	@Override
	public VideoStreamStartupData readData(String tracePath) {

		File jsonFile = filereader.createFile(tracePath, JSON_FILE);
		if (jsonFile.exists()) {
			try {
				String jsonDataString = filereader.readAllData(jsonFile.toString());
				videoStreamStartupData = mapper.readValue(jsonDataString, VideoStreamStartupData.class);
				if (videoStreamStartupData.getStreams().isEmpty()) { // imports older formats
					VideoStreamStartup videoStreamStartup = mapper.readValue(jsonDataString, VideoStreamStartup.class);
					videoStreamStartupData.getStreams().add(videoStreamStartup);
				}
				jsonDataSaved = serialize(videoStreamStartupData);
			} catch (IOException e) {
				LOG.debug("failed to load startup data: " + e.getMessage());
			}
		} else {
			videoStreamStartupData = new VideoStreamStartupData();
		}
		return videoStreamStartupData;
	}

	@Override
	public boolean save(String path, VideoStreamStartupData videoStreamStartupData) throws Exception {
		if (videoStreamStartupData != null) {
			
			String jsonData = serialize(videoStreamStartupData);		
			if (jsonData.equals(jsonDataSaved)) {
				LOG.debug("Startup delay has no changes, so not saving");
				return true;
			}
			
			this.videoStreamStartupData = videoStreamStartupData;
			String jsonPath = null;
			File jsonFile = filemanager.createFile(path, JSON_FILE);
			jsonPath = jsonFile.toString();

			LOG.debug("Startup delay saving");
			
			if (!jsonData.isEmpty() && jsonPath != null && !jsonPath.isEmpty()) {
				FileOutputStream output = new FileOutputStream(jsonPath);
				output.write(jsonData.getBytes());
				output.flush();
				output.close();
				LOG.debug("Startup delay saved");
				jsonDataSaved = jsonData;
				return true;
			}
		}
		return false;
	}

	public String serialize(VideoStreamStartupData videoStreamStartupData) throws JsonProcessingException {
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(videoStreamStartupData);
	}
}
