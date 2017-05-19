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
package com.att.aro.core.packetanalysis;

import java.util.List;
import java.util.TreeMap;

import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.preferences.IPreferenceHandler;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

public interface IVideoUsageAnalysis {
	
	/**
	 * Collect trace data for Video Best Practices for streaming video apps that use HLS & DASH.
	 * 
	 * @param result
	 * @param sessionlist
	 * @return
	 */
	VideoUsage analyze(AbstractTraceResult result, List<Session> sessionlist);

	IPreferenceHandler getPrefs();
	
	VideoUsagePrefs getVideoUsagePrefs();
	
	void loadPrefs();

	VideoUsage getVideoUsage();

	TreeMap<Double, HttpRequestResponseInfo> getReqMap();

}
