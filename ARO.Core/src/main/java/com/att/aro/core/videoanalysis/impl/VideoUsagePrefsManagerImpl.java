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

import org.codehaus.jackson.map.ObjectMapper;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

public class VideoUsagePrefsManagerImpl implements IVideoUsagePrefsManager {
	
	@InjectLogger
	private static ILogger log;
	
	public VideoUsagePrefs getVideoUsagePreference(){
		VideoUsagePrefs videoUsagePrefs = new VideoUsagePrefs();
		
		PreferenceHandlerImpl prefs = PreferenceHandlerImpl.getInstance();
		String videoPref = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if(videoPref != null){
			ObjectMapper mapper = new ObjectMapper();
			try {
				videoUsagePrefs = mapper.readValue(videoPref, VideoUsagePrefs.class);
				
			} catch (IOException e) {
				log.error("VideoPreference Mapper Exception" + e.getMessage());
			}
		}
		return videoUsagePrefs;
	}
	
	
}
