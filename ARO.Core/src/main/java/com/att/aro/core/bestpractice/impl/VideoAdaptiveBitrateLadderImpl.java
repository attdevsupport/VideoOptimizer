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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoAdaptiveBitrateLadderResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.pojo.QualityTime;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

public class VideoAdaptiveBitrateLadderImpl implements IBestPractice {
	@Value("${adaptiveBitrateLadder.title}")
	private String overviewTitle;

	@Value("${adaptiveBitrateLadder.detailedTitle}")
	private String detailTitle;

	@Value("${adaptiveBitrateLadder.desc}")
	private String aboutText;

	@Value("${adaptiveBitrateLadder.url}")
	private String learnMoreUrl;

	@Value("${adaptiveBitrateLadder.results}")
	private String textResults;

	@Value("${adaptiveBitrateLadder.excel.results}")
    private String textExcelResults;

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
	
	@Value("${video.noData}")
	private String noData;

	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();

	private StreamingVideoData streamingVideoData;

	private VideoAdaptiveBitrateLadderResult result;
	
	List<QualityTime> adaptiveBitrateLadderList = new ArrayList<>();
	SortedMap<Integer, QualityTime> qualityMap = new TreeMap<>();

	private int selectedManifestCount;

	private int invalidCount;

	private boolean hasSelectedManifest;

	public static final double PERCENTILE_LINE = 100;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		BPResultType bpResultType = BPResultType.SELF_TEST;
		result = new VideoAdaptiveBitrateLadderResult();

		init(result);
		qualityMap.clear();

		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null
				&& MapUtils.isNotEmpty(videoStreamCollection)) {
			
			selectedManifestCount = streamingVideoData.getSelectedManifestCount();
			hasSelectedManifest = (selectedManifestCount > 0);
			invalidCount = streamingVideoData.getInvalidManifestCount();
			
			bpResultType = BPResultType.CONFIG_REQUIRED;
			result.setResultExcelText(bpResultType.getDescription());
				
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
				QualityTime qualityTime;
				double maxTime = 0;
				QualityTime maxSection = null;
				double videoRatio = 1;
				double durationTotal = 0;
				Set<String> resolutions = new TreeSet<>();

				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream.isSelected() && MapUtils.isNotEmpty(videoStream.getVideoEventMap())) {
						videoRatio = PERCENTILE_LINE / videoStream.getDuration();

						for (VideoEvent videoEvent : videoStream.getVideoEventMap().values()) {
							if (videoEvent.isNormalSegment() && videoEvent.isSelected()) {
								resolutions.add(String.valueOf(videoEvent.getResolutionHeight()));
								double duration = videoEvent.getDuration();
								durationTotal  += duration;
								Integer track = StringParse.stringToDouble(videoEvent.getQuality(), 0).intValue();
								if ((qualityTime = qualityMap.get(track)) != null) {
									int count = qualityTime.getCount();
									qualityTime.setDuration(qualityTime.getDuration() + duration);
									qualityTime.setPercentage(qualityTime.getDuration() * videoRatio);
									qualityTime.setBitrateAverage((qualityTime.getBitrateAverage() * count + videoEvent.getBitrate()) / ++count);
									qualityTime.setCount(count);
								} else {
									qualityTime = new QualityTime(videoEvent.getManifest().getVideoName()
																, 1
																, track
																, duration
																, duration * videoRatio
																, videoEvent.getResolutionHeight()
																, videoEvent.getSegmentStartTime()
																, videoEvent.getChildManifest().getBandwidth() / 1000	// bitrateDeclared (kbps)
																, videoEvent.getBitrate()	// bitrateAverage (kbps)
											);
									qualityMap.put(track, qualityTime);
								}

								if (maxTime < qualityTime.getDuration()) {
									maxTime = qualityTime.getDuration();
									maxSection = qualityTime;
								}
							}
						}
					}
				}

				bpResultType = BPResultType.SELF_TEST;
				int count = qualityMap.size();
				double maxDuration = 0;
				double percentage = 0;
				int track = 0;
				if (maxSection != null) {
					maxDuration = maxSection.getDuration();
					percentage = maxSection.getPercentage();
					track = maxSection.getTrack();
				}

				result.setResultText(MessageFormat.format(textResults
						, count == 1 ? "was" : "were", count
						, count == 1 ? "" : "s"
						, track
						, String.format("%.2f", maxDuration)
						, String.format("%.2f", percentage)
						, String.format("%.3f", durationTotal)
						));
				result.setResultExcelText(MessageFormat.format(textExcelResults, bpResultType.getDescription(), String.join("p, ", resolutions) + "p"));
			} else {
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultText(novalidManifestsFound);
				result.setResultExcelText(bpResultType.getDescription());
			}

			result.setResults(qualityMap);
			
		} else {
			// No Data
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
			result.setResultExcelText(bpResultType.getDescription());
		}
		result.setResultType(bpResultType);
		result.setResults(qualityMap);
		return result;
	}

	public void init(AbstractBestPracticeResult result) {
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
	}

}// end class