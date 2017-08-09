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

package com.att.aro.core.bestpractice.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSessionResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoConcurrentSessionImpl implements IBestPractice {
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		int maxConcurrentSessions = 0;
		VideoUsage videoUsage = tracedata.getVideoUsage();
		if (videoUsage != null 
				&& videoUsage.getAroManifestMap() != null 
				&& videoUsage.getAroManifestMap().size() > 0) {
			maxConcurrentSessions = maxOverlapVideoSessions(videoUsage.getAroManifestMap());
		}

		VideoConcurrentSessionResult result = new VideoConcurrentSessionResult();
		result.setConcurrentSessions(maxConcurrentSessions);
		result.setResultType(BPResultType.NONE);
		return result;
	}

	public int maxOverlapVideoSessions(TreeMap<Double, AROManifest> aroManifestMap) {

		int maxConcurrentSessions = 0;
		ArrayList<Double> sessionStartTimes = new ArrayList<Double>();
		ArrayList<Double> sessionEndTimes = new ArrayList<Double>();
		List<Session> sessionList = new ArrayList<Session>();

		if (aroManifestMap != null && aroManifestMap.size() > 0) {
			for (AROManifest manifest : aroManifestMap.values()) {
				if (manifest.isSelected()) {
					TreeMap<String, VideoEvent> videoEventList = manifest.getVideoEventList();
					for (Map.Entry<String, VideoEvent> veEntry : videoEventList.entrySet()) {
						if (!sessionList.contains(((VideoEvent) veEntry.getValue()).getSession())) {
							sessionList.add(((VideoEvent) veEntry.getValue()).getSession());
							sessionStartTimes.add(((VideoEvent) veEntry.getValue()).getSession().getSessionStartTime());
							sessionEndTimes.add(((VideoEvent) veEntry.getValue()).getSession().getSessionEndTime());
						}
					}
				}
			}

			if (sessionList.size() > 0) {
				int startTimeCounter = 0;
				int endTimeCounter = 0;
				int currentOverlap = 0;
				Collections.sort(sessionStartTimes);
				Collections.sort(sessionEndTimes);
				int startTimePointer = sessionStartTimes.size(), endTimePointer = sessionEndTimes.size();
				while (startTimeCounter < startTimePointer && endTimeCounter < endTimePointer) {
					if (sessionStartTimes.get(startTimeCounter) < sessionEndTimes.get(endTimeCounter)) {
						currentOverlap++;
						startTimeCounter++;
						if (currentOverlap > maxConcurrentSessions)
							maxConcurrentSessions = currentOverlap;
					} else {
						currentOverlap--;
						endTimeCounter++;
					}
				}
			}
		}
		return maxConcurrentSessions;
	}
}
