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
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoTcpConnectionResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

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

	@Nonnull
	private SortedMap<Double, AROManifest> manifestCollection = new TreeMap<>();
	
	@Nonnull
	VideoUsage videoUsage;

	private int selectedCount;
	private int invalidCount;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		BPResultType bpResultType = BPResultType.SELF_TEST;
		VideoTcpConnectionResult result = new VideoTcpConnectionResult();
		int sessionCount = 0;
		init(result);
		
		videoUsage = tracedata.getVideoUsage();

		if (videoUsage != null) {
			manifestCollection = videoUsage.getAroManifestMap();
		}

		if (MapUtils.isNotEmpty(manifestCollection)) {
			selectedCount = videoUsage.getSelectedManifestCount();
			invalidCount = videoUsage.getInvalidManifestCount();
			
			if (selectedCount == 0) {
				if (invalidCount == manifestCollection.size()) {
					result.setResultText(invalidManifestsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noManifestsSelectedMixed);
				} else {
					result.setResultText(noManifestsSelected);
				}
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setSelfTest(false);
			} else if (selectedCount > 1) {
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultText(multipleManifestsSelected);
				result.setSelfTest(false);
			} else {
				TreeMap<Session, Integer> uniqSessions = new TreeMap<>();
				for (AROManifest aroManifest : videoUsage.getManifests()) {
					if (aroManifest != null && aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty()) {
						int count = 0;
						for (VideoEvent videoEvent : aroManifest.getVideoEventList().values()) {
							if (uniqSessions.containsKey(videoEvent.getSession())) {
								count = uniqSessions.get(videoEvent.getSession());
							}
							uniqSessions.put(videoEvent.getSession(), ++count);
						}
					}
					sessionCount = uniqSessions.size();
				}
				bpResultType = BPResultType.SELF_TEST;
				result.setResultText(MessageFormat.format(textResults,
						ApplicationConfig.getInstance().getAppShortName(), sessionCount, sessionCount == 1 ? "" : "s"));
				result.setTcpConnections(sessionCount);
				result.setSelfTest(true);
			}
		} else {
			result.setSelfTest(false);
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
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
}// end class
