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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSession;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSessionResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoConcurrentSessionImpl implements IBestPractice {
	@InjectLogger
	private static ILogger logger;
	@Value("${videoConcurrentSession.title}")
	private String overviewTitle;
	@Value("${videoConcurrentSession.detailedTitle}")
	private String detailTitle;
	@Value("${videoConcurrentSession.desc}")
	private String aboutText;
	@Value("${videoConcurrentSession.url}")
	private String learnMoreUrl;
	@Value("${videoConcurrentSession.results}")
	private String textResults;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		int maxManifestConcurrentSessions = 0;
		VideoConcurrentSessionResult result = new VideoConcurrentSessionResult();
		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setOverviewTitle(overviewTitle);
		result.setResultType(BPResultType.SELF_TEST);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBaseNew())); // http://developer.att.com/ARO/BestPractices/VideoConcurrentSession
		List<VideoConcurrentSession> manifestConcurrency = new ArrayList<VideoConcurrentSession>();
		VideoUsage videoUsage = tracedata.getVideoUsage();
		if (videoUsage != null && videoUsage.getAroManifestMap() != null && videoUsage.getAroManifestMap().size() > 0) {
			manifestConcurrency = manifestConcurrentSessions(videoUsage.getAroManifestMap());
			result.setResults(manifestConcurrency);
			for (VideoConcurrentSession manifestSession : manifestConcurrency) {
				if (maxManifestConcurrentSessions < manifestSession.getConcurrentSessionCount()) {
					maxManifestConcurrentSessions = manifestSession.getConcurrentSessionCount();
				}
			}
			result.setMaxConcurrentSessionsCount(maxManifestConcurrentSessions);
		}
		result.setResultText(MessageFormat.format(textResults, maxManifestConcurrentSessions));
		return result;
	}

	public List<VideoConcurrentSession> manifestConcurrentSessions(TreeMap<Double, AROManifest> aroManifestMap) {
		List<VideoConcurrentSession> concurrentSessionList = new ArrayList<VideoConcurrentSession>();
		if (aroManifestMap != null && aroManifestMap.size() > 0) {
			for (AROManifest manifest : aroManifestMap.values()) {
				if (manifest != null && manifest.isSelected()) {
					ArrayList<Double> sessionStartTimes = new ArrayList<Double>();
					ArrayList<Double> sessionEndTimes = new ArrayList<Double>();
					List<Session> sessionList = new ArrayList<Session>();
					TreeMap<String, VideoEvent> videoEventList = manifest.getVideoEventList();
					for (Map.Entry<String, VideoEvent> veEntry : videoEventList.entrySet()) {
						if (!sessionList.contains(veEntry.getValue().getSession())) {
							sessionList.add(veEntry.getValue().getSession());
							sessionStartTimes.add(veEntry.getValue().getSession().getSessionStartTime());
							sessionEndTimes.add(veEntry.getValue().getSession().getSessionEndTime());
						}
					}
					VideoConcurrentSession concurrency = findConcurrency(sessionStartTimes, sessionEndTimes);
					if (concurrency != null && concurrency.getConcurrentSessionCount() > 0) {
						concurrency.setVideoName(manifest.getVideoName());
						concurrentSessionList.add(concurrency);
					}
				}
			}
		}
		return concurrentSessionList;
	}

	private VideoConcurrentSession findConcurrency(ArrayList<Double> sessionStartTimes,
			ArrayList<Double> sessionEndTimes) {
		int maxConcurrentSessions = 0;
		VideoConcurrentSession concurrentSession = null;
		if (sessionStartTimes.size() > 0) {
			int startTimeCounter = 0;
			int endTimeCounter = 0;
			int currentOverlap = 0;
			Collections.sort(sessionStartTimes);
			Collections.sort(sessionEndTimes);
			int startTimePointer = sessionStartTimes.size(), endTimePointer = sessionEndTimes.size();
			while (startTimeCounter < startTimePointer && endTimeCounter < endTimePointer) {
				if (sessionStartTimes.get(startTimeCounter) < sessionEndTimes.get(endTimeCounter)) {
					Double duration = sessionEndTimes.get(endTimeCounter) - sessionStartTimes.get(startTimeCounter);
					currentOverlap++;
					startTimeCounter++;
					if (maxConcurrentSessions <= currentOverlap && currentOverlap > 1) {
						if (concurrentSession != null) {
							if (concurrentSession.getConcurrentSessionCount() == currentOverlap) {
								duration = duration + concurrentSession.getConcurrencyDuration();
							}
							concurrentSession.setCountDuration(currentOverlap, duration);
						} else {
							concurrentSession = new VideoConcurrentSession(currentOverlap, duration);
						}
					}
					if (maxConcurrentSessions < currentOverlap) {
						maxConcurrentSessions = currentOverlap;
					}
				} else {
					currentOverlap--;
					endTimeCounter++;
				}
			}
		}
		return concurrentSession;
	}
}
