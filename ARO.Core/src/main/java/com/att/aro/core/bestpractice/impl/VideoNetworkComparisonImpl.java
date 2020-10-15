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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoNetworkComparisonResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.SegmentComparison;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

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
 * Added a new table for video network comparison BP. The table displayed four
 * columns, segment track, usage, segment declared bitrate and network
 * calculated throughput (Kbps) Each segment download throughput formula :
 * (Total byte * 8 (bit) / 1000 (K)) / (durationInMilliseconds(ms) / 1000). When traces have
 * multiple segments, VO added it up and calculated the average.
 * 
 * Link: goes to a view of network
 */
public class VideoNetworkComparisonImpl extends PlotHelperAbstract implements IBestPractice {

	private static final Logger LOGGER = LogManager.getLogger(VideoNetworkComparisonImpl.class.getName());

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
	
	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();

	private int selectedCount;
	private int invalidCount;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		BPResultType bpResultType = BPResultType.SELF_TEST;
		double avgKbps = 0.0;
		double avgBitRate = 0.0;
		double summaryBitRate = 0.0;

		VideoNetworkComparisonResult result = new VideoNetworkComparisonResult();
		init(result);
		SortedMap<Integer, SegmentComparison> qualityMap = new TreeMap<>();

		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {

			selectedCount = streamingVideoData.getSelectedManifestCount();
			invalidCount = streamingVideoData.getInvalidManifestCount();

			List<VideoEvent> filteredVideoSegment = filterVideoSegment(streamingVideoData);

			if (selectedCount == 0) {
				if (invalidCount == videoStreamCollection.size()) {
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
				SegmentComparison segmentComparison;
				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream.isSelected() && MapUtils.isNotEmpty(videoStream.getVideoEventList())) {
						for (VideoEvent videoEvent : videoStream.getVideoEventList().values()) {
							if (videoEvent.isNormalSegment() && videoEvent.isSelected()) {
								Integer track = StringParse.stringToDouble(videoEvent.getQuality(), 0).intValue();
								double endTS = videoEvent.getEndTS();
								double startTS = videoEvent.getStartTS();
								double durationInMilliseconds = endTS - startTS;
								double throughput = 0.0;
								if (durationInMilliseconds > 0) {
									throughput = (videoEvent.getTotalBytes() * 8) / durationInMilliseconds;
								}
								if ((segmentComparison = qualityMap.get(track)) != null) {
									int count = segmentComparison.getCount();
									segmentComparison.setCount(++count);
									segmentComparison.getCalculatedThroughputList().add(throughput);
								} else {
									List<Double> throughputs = new ArrayList<Double>();
									throughputs.add(throughput);
									segmentComparison = new SegmentComparison(videoEvent.getManifest().getVideoName(),
											1, track, videoEvent.getChildManifest().getBandwidth() / 1000.0 // declaredBitrate (kbps)
											, throughputs);
									qualityMap.put(track, segmentComparison);
								}
							}
						}
					}
				}
				result.setResults(qualityMap);
				avgBitRate = getAvgBitRate(summaryBitRate, filteredVideoSegment);
				avgKbps = getAvgThroughput(tracedata);
				result.setAvgBitRate(avgBitRate);
				result.setAvgKbps(avgKbps);
				result.setSelfTest(true);
				bpResultType = BPResultType.SELF_TEST;
				result.setResultText(MessageFormat.format(textResults, avgKbps, avgBitRate));
			}
		}else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
		}

		result.setResultType(bpResultType);
		return result;
	}

	public void init(VideoNetworkComparisonResult result) {
		selectedCount = 0;
		invalidCount = 0;
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
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
						LOGGER.debug("packetInfo: " + packetInfo.getPacketId() + " packet Time: " + packetInfo.getTimeStamp() + " find the bigger max: " + curMaxLength);
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
			// logger.debug(" BitRate: "+videoEvent.getBitrate());
			summaryBitRate += videoEvent.getBitrate();
		}
		avgBitRate = summaryBitRate / (1024 * filteredVideoSegment.size());// kbps
		LOGGER.debug("avgBitRate: " + avgBitRate);
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
