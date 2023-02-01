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
import java.util.SortedMap;
import java.util.TreeMap;


import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSession;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSessionResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

public class VideoConcurrentSessionImpl implements IBestPractice {
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

	@Value("${videoConcurrentSession.excel.results}")
    private String textExcelResults;

	@Value("${videoConcurrentSession.results0}")
	private String nonConcurrent;

	@Value("${video.noData}")
	private String noData;

	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;	

	@Value("${videoManifest.multipleManifestsSelected}")
	private String multipleManifestsSelected;	

	@Value("${videoManifest.noManifestsSelected}")
	private String noManifestsSelected;	

	@Value("${videoManifest.noManifestsSelectedMixed}")
	private String noManifestsSelectedMixed;	

	@Value("${videoManifest.invalid}")
	private String invalidManifestsFound;

	private VideoConcurrentSessionResult result;

	private int selectedManifestCount;
	private int invalidCount;
	private boolean hasSelectedManifest;
	private BPResultType bpResultType;
	
	SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();

	private StreamingVideoData streamingVideoData;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		result = new VideoConcurrentSessionResult();
		init(result);

		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {

			bpResultType = BPResultType.CONFIG_REQUIRED;
			result.setResultExcelText(bpResultType.getDescription());
			
			selectedManifestCount = streamingVideoData.getSelectedManifestCount();
			hasSelectedManifest = (selectedManifestCount > 0);
			invalidCount = streamingVideoData.getInvalidManifestCount();
			
			if (selectedManifestCount == 0) {
				if (invalidCount == videoStreamCollection.size()) {
					result.setResultText(invalidManifestsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noManifestsSelectedMixed);
				} else {
					result.setResultText(noManifestsSelected);
				}
			} else if (selectedManifestCount > 1) {
				result.setResultText(multipleManifestsSelected);
			} else if (hasSelectedManifest) {
				bpResultType = BPResultType.SELF_TEST;
				int maxManifestConcurrentSessions = 0;
				List<VideoConcurrentSession> manifestConcurrency = new ArrayList<VideoConcurrentSession>();
				manifestConcurrency = manifestConcurrentSessions(videoStreamCollection);
				result.setResults(manifestConcurrency);
				for (VideoConcurrentSession manifestSession : manifestConcurrency) {
					if (maxManifestConcurrentSessions < manifestSession.getConcurrentSessionCount()) {
						maxManifestConcurrentSessions = manifestSession.getConcurrentSessionCount();
					}
				}
				result.setMaxConcurrentSessionCount(maxManifestConcurrentSessions);
				if (maxManifestConcurrentSessions>0) {
					result.setResultText(MessageFormat.format(textResults, maxManifestConcurrentSessions));
				} else {
					result.setResultText(nonConcurrent);
				}

				result.setResultExcelText(
			        MessageFormat.format(textExcelResults, bpResultType.getDescription(), maxManifestConcurrentSessions)
		        );
				result.setSelfTest(true);
			}
		} else {
			result.setSelfTest(false);
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
			result.setResultExcelText(bpResultType.getDescription());
		}

		result.setResultType(bpResultType);
		return result;
	}

	public void init(AbstractBestPracticeResult result) {
		bpResultType = BPResultType.SELF_TEST;
		invalidCount = 0;
		selectedManifestCount = 0;
		hasSelectedManifest = false;

		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setOverviewTitle(overviewTitle);
		result.setLearnMoreUrl(learnMoreUrl);
	}

	public List<VideoConcurrentSession> manifestConcurrentSessions(SortedMap<Double, VideoStream> videoStreamMap) {
		List<VideoConcurrentSession> concurrentSessionList = new ArrayList<VideoConcurrentSession>();
		if (MapUtils.isNotEmpty(videoStreamCollection)) {
			for (VideoStream videoStream : videoStreamMap.values()) {
				if (videoStream.isSelected()) {
					ArrayList<Double> sessionStartTimes = new ArrayList<>();
					ArrayList<Double> sessionEndTimes = new ArrayList<>();
					ArrayList<Session> sessionList = new ArrayList<>();
					SortedMap<String, VideoEvent> videoEventList = videoStream.getVideoEventMap();
					for (VideoEvent veEntry : videoEventList.values()) {
						Session session = veEntry.getSession();
						if (!sessionList.contains(session)) {
							sessionList.add(session);
							sessionStartTimes.add(session.getSessionStartTime());
							sessionEndTimes.add(session.getSessionEndTime());
						}
					}
					VideoConcurrentSession videoConcurrentSession = findConcurrency(sessionStartTimes, sessionEndTimes);
					if (videoConcurrentSession != null && videoConcurrentSession.getConcurrentSessionCount() > 0) {
						videoConcurrentSession.setVideoName(videoStream.getManifest().getVideoName());
						concurrentSessionList.add(videoConcurrentSession);
					}
				}
			}
		}
		return concurrentSessionList;
	}

	private VideoConcurrentSession findConcurrency(ArrayList<Double> sessionStartTimes, ArrayList<Double> sessionEndTimes) {
		int maxConcurrentSessions = 0;
		VideoConcurrentSession concurrentSession = null;
		if (sessionStartTimes.size() > 0) {
			int startTimeCounter = 0;
			int endTimeCounter = 0;
			int currentOverlap = 0;
			Collections.sort(sessionStartTimes);
			Collections.sort(sessionEndTimes);
			int startTimePointer = sessionStartTimes.size();
			int endTimePointer = sessionEndTimes.size();
			
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
