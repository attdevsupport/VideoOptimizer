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

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VideoUsagePrefsManagerImpl implements IVideoUsagePrefsManager {

	private static final Logger LOG = LogManager.getLogger(VideoUsagePrefsManagerImpl.class.getName());

	@Autowired
	VideoUsagePrefs videoUsagePrefs;

	public void setVideoUsagePrefs(VideoUsagePrefs videoUsagePrefs) {
		this.videoUsagePrefs = videoUsagePrefs;
	}

	public VideoUsagePrefs getVideoUsagePreference() {
		PreferenceHandlerImpl prefs = PreferenceHandlerImpl.getInstance();
		String videoPref = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (videoPref != null && !videoPref.equals("null")) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				videoUsagePrefs = mapper.readValue(videoPref, VideoUsagePrefs.class);

			} catch (IOException e) {
				LOG.error("VideoPreference Mapper Exception" + e.getMessage());
			}
		}
		return videoUsagePrefs;
	}
	
	
}
