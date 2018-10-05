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
import java.util.TreeMap;

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


	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		int sessionCount = 0;

		VideoUsage videoUsage = tracedata.getVideoUsage();
		if (videoUsage != null) {
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
		}
		
		VideoTcpConnectionResult result = new VideoTcpConnectionResult();
		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setResultType(BPResultType.SELF_TEST); // this VideoBestPractice is to be reported as a selftest until further notice
		result.setResultText(MessageFormat.format(textResults, 
													ApplicationConfig.getInstance().getAppShortName(), 
													sessionCount, 
													sessionCount == 1 ? "" : "s"));
		result.setTcpConnections(sessionCount);
		return result;
	}

}// end class
