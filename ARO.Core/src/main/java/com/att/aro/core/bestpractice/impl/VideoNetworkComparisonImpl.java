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
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoNetworkComparisonResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

/**
 * <pre>
 * VBP #4 Video File and Network Comparison
 * 
 * Criteria: ARO will compare the bitrate of a streaming file with the
 * bitrate/bandwidth of the network.
 * 
 * About: Deliver video at a rate within the network capability, while factoring
 * in the HTTP/TCP protocol behavior.
 * 
 * Result: Your video was delivered at X rate of speed. The network was capable
 * of X bitrate. Your video could be X% faster ( or should be X% slower) for
 * this type network.
 * 
 * Link: goes to a view of network
 */
public class VideoNetworkComparisonImpl extends PlotHelperAbstract implements IBestPractice {

	@InjectLogger
	private static ILogger logger;
 
	@Value("${networkComparison.title}")
	private String overviewTitle;

	@Value("${networkComparison.detailedTitle}")
	private String detailTitle;

	@Value("${networkComparison.desc}")
	private String aboutText;

	@Value("${networkComparison.url}")
	private String learnMoreUrl;

	@Value("${networkComparison.pass}")
	private String textResultPass;

	@Value("${networkComparison.results}")
	private String textResults;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
 
		double avgKbps = 0.0;
		double avgBitRate = 0.0;
		double summaryBitRate = 0.0;

		VideoUsage videoUsage = tracedata.getVideoUsage();
		if(videoUsage!=null){
			List<VideoEvent> filteredVideoSegment = filterVideoSegment(videoUsage);
			if (filteredVideoSegment.isEmpty()) {
				logger.debug("there is no filtered video segment available");
			} else {
				avgBitRate = getAvgBitRate(summaryBitRate, filteredVideoSegment);
			}
		}

		avgKbps = getAvgThroughput(tracedata);

		VideoNetworkComparisonResult result = new VideoNetworkComparisonResult();
		result.setAvgBitRate(avgBitRate);
		result.setAvgKbps(avgKbps);
		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setResultType(BPResultType.SELF_TEST); // this VideoBestPractice is to be reported as a selftest until further notice
		result.setResultText(MessageFormat.format(textResults, avgKbps, avgBitRate));
		
		return result;
	}

	private double getAvgThroughput(PacketAnalyzerResult tracedata) {
		double avgKbps;
		int curMaxLength = 0;

		for (Session session : tracedata.getSessionlist()) {
			double startTime = 0;
			int tempLength = 0;

			for (PacketInfo packetInfo : session.getPackets()) {
				if (startTime == 0) {// give the initial value
					startTime = packetInfo.getTimeStamp();
				}
				if (startTime + 1 > packetInfo.getTimeStamp()) {
					tempLength += packetInfo.getPayloadLen();
				} else {
					if (curMaxLength < tempLength) {
						curMaxLength = tempLength;
						logger.debug("packetInfo: " + packetInfo.getPacketId() + " packet Time: "
								+ packetInfo.getTimeStamp() + " find the bigger max: " + curMaxLength);
					}
					tempLength = packetInfo.getPayloadLen();// reset value
					startTime = packetInfo.getTimeStamp();
				}
			}
		}
		avgKbps = (double) curMaxLength * 8 / 1024; // kbps
		return avgKbps;
	}

	private double getAvgBitRate(double summaryBitRate, List<VideoEvent> filteredVideoSegment) {
		double avgBitRate;
		for (VideoEvent videoEvent : filteredVideoSegment) {
//					logger.debug(" BitRate: "+videoEvent.getBitrate());
			summaryBitRate += videoEvent.getBitrate();
		}
		avgBitRate = summaryBitRate / (1024*filteredVideoSegment.size());//kbps
		logger.debug("avgBitRate: "+avgBitRate);
		return avgBitRate;
	}
 	
	public void setTextResults(String textResults) {
		this.textResults = textResults;
	}

	public void setTextResultPass(String textResultPass) {
		this.textResultPass = textResultPass;
	}

	public void setLearnMoreUrl(String learnMoreUrl) {
		this.learnMoreUrl = learnMoreUrl;
	}

	
}// end class
