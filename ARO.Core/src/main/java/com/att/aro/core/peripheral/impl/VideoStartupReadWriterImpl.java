/*
 *  Copyright 2018 AT&T
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
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Method to read times from the video time trace file and store video time
 * variables.
 * Date: October 10, 2018
 */
public class VideoStartupReadWriterImpl extends PeripheralBase implements IVideoStartupReadWrite {

	private static final Logger LOG = LogManager.getLogger(VideoStartupReadWriterImpl.class.getName());

	@Autowired
	private IFileManager filemanager;
	
	private static final String JSON_FILE = "video_stream_startup.json";

	private ObjectMapper mapper = new ObjectMapper();

	VideoStreamStartup videoStreamStartup;
	
	@Override
	public VideoStreamStartup readData(String tracePath) {

		if (tracePath == null) {
			videoStreamStartup = new VideoStreamStartup();
		} else {

			File jsonFile = filereader.createFile(tracePath, JSON_FILE);
			try {
				String temp = filereader.readAllData(jsonFile.toString());
				videoStreamStartup = mapper.readValue(temp, VideoStreamStartup.class);
			} catch (IOException e) {
				LOG.debug("failed to load startup data: " + e.getMessage());
				videoStreamStartup = new VideoStreamStartup();
			}
		}
		return videoStreamStartup;
	}
	
	/**
	 * @param path
	 * Path can be a fully qualified file path or just the trace folder
	 * The method saves data into the metadata.json file.
	 * @throws Exception
	 */
	@Override
	public void save(String path, VideoStreamStartup videoStreamStartup) throws Exception {
		if (videoStreamStartup != null) {
			this.videoStreamStartup = videoStreamStartup;
		}
		String localPath = null;

		if (!path.isEmpty() && !filemanager.isFile(path)) {
			localPath = filemanager.createFile(path, JSON_FILE).toString();
		}

		String jsonData = getJson();

		if (jsonData != null && !jsonData.isEmpty() && localPath != null && !localPath.isEmpty()) {
			FileOutputStream output = new FileOutputStream(localPath);
			output.write(jsonData.getBytes());
			output.flush();
			output.close();
		}
	}
	

	private String getJson() throws Exception {
		String serialized = "";
		serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(videoStreamStartup);
		return serialized;
	}

}
