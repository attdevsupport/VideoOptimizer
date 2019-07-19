
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

package com.att.aro.core.videoanalysis.impl;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.preferences.IPreferenceHandler;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class VideoPrefsController {
	private static final Logger LOG = LogManager.getLogger(VideoPrefsController.class);
	private IPreferenceHandler prefs = PreferenceHandlerImpl.getInstance();
	private VideoUsagePrefs videoUsagePrefs;
	
	public VideoPrefsController(){
		videoUsagePrefs = loadPrefs();
	}
	
	public boolean save() {
		ObjectMapper mapper = new ObjectMapper();
		String temp;
		try {
			temp = mapper.writeValueAsString(videoUsagePrefs);
		} catch (IOException e) {
			LOG.error("Failed to serialize VideoUsagePrefs: " + videoUsagePrefs);
			return false;
		}
		prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
		return true;
	}
	
	/**
	 * Loads video preferences set in the config file onto VideoUsagePrefs
	 *
	 */
	public VideoUsagePrefs loadPrefs() {
		ObjectMapper mapper = new ObjectMapper();
		videoUsagePrefs = null;
		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null && !temp.equals("null")) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
			}
		} else {
			try {
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
			}
		}
		return videoUsagePrefs;
	}
}

