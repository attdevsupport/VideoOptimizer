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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoTcpConnectionResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

/**
 * <pre>
 * VBP #5 Video Streams and TCP Connections
 * 
 * Criteria: ARO will determine (1) whether TCP connections carrying video are persistent, or not (2) if single video chunks is being sent over one TCP
 * connection or multiple connections, to help developers manage the connection strategy..
 * 
 * About: It is a good practice to understand how effectively you are using TCP connections to stream video.
 * 
 * Results: ARO detected X # of separate TCP connections for a single video. The connections were Persistent or Not persistent.
 * 
 * Link goes to a view of the TCP connections carrying video streams.
 *
 */
public class VideoTcpConnectionImpl implements IBestPractice{

	@Value("${tcpConnection.title}")
	private String overviewTitle;

	@Value("${tcpConnection.detailedTitle}")
	private String detailTitle;

	@Value("${tcpConnection.desc}")
	private String aboutText;

	@Value("${tcpConnection.url}")
	private String learnMoreUrl;

	@Value("${tcpConnection.pass}")
	private String textResultPass;

	@Value("${tcpConnection.results}")
	private String textResults;

	@Value("${tcpConnection.excel.results}")
    private String textExcelResults;

	@Value("${video.noData}")
	private String noData;

	@Value("${videoSegment.empty}")
	private String novalidStreamsFound;

	@Value("${videoManifest.multipleManifestsSelected}")
	private String multipleStreamsSelected;

	@Value("${videoManifest.noManifestsSelected}")
	private String noStreamsSelected;
	
	@Value("${videoManifest.noManifestsSelectedMixed}")
	private String noStreamsSelectedMixed;
	
	@Value("${videoManifest.invalid}")
	private String invalidStreamsFound;

	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();

	private StreamingVideoData streamingVideoData;
	
	private int selectedCount;
	private int invalidCount;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		BPResultType bpResultType = BPResultType.SELF_TEST;
		VideoTcpConnectionResult result = new VideoTcpConnectionResult();
		int sessionCount = 0;
		init(result);
		
		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {

			selectedCount = streamingVideoData.getSelectedManifestCount();
			invalidCount = streamingVideoData.getInvalidManifestCount();
			
			if (selectedCount == 0) {
				if (invalidCount == videoStreamCollection.size()) {
					result.setResultText(invalidStreamsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noStreamsSelectedMixed);
				} else {
					result.setResultText(noStreamsSelected);
				}
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultExcelText(bpResultType.getDescription());
				result.setSelfTest(false);
			} else if (selectedCount > 1) {
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultText(multipleStreamsSelected);
				result.setResultExcelText(bpResultType.getDescription());
				result.setSelfTest(false);
			} else {
				ArrayList<Session> uniqVideoSessions = new ArrayList<>();
				ArrayList<Session> uniqAudioSessions = new ArrayList<>();
				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream.isSelected() && !videoStream.getVideoEventMap().isEmpty()) {
						for (VideoEvent videoEvent : videoStream.getVideoEventMap().values()) {
							if (!uniqVideoSessions.contains(videoEvent.getSession())) {
								uniqVideoSessions.add(videoEvent.getSession());
							}
						}
						for (VideoEvent videoEvent : videoStream.getAudioSegmentEventList().values()) {
							if (!uniqAudioSessions.contains(videoEvent.getSession())) {
								uniqAudioSessions.add(videoEvent.getSession());
							}
						}
					
					}
				}

				sessionCount = uniqVideoSessions.size() > uniqAudioSessions.size() ? uniqVideoSessions.size() : uniqAudioSessions.size();

				bpResultType = BPResultType.SELF_TEST;
				result.setResultText(MessageFormat.format(textResults,
						ApplicationConfig.getInstance().getAppShortName(), sessionCount, sessionCount == 1 ? "" : "s"));
				result.setResultExcelText(MessageFormat.format(textExcelResults, bpResultType.getDescription(), sessionCount));
				result.setTcpConnections(sessionCount);
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
	
	public void init(VideoTcpConnectionResult result) {
		selectedCount = 0;
		invalidCount = 0;
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
	}
}
