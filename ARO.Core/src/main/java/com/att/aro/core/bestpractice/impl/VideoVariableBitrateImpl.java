
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

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoVariableBitrateResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

public class VideoVariableBitrateImpl implements IBestPractice {

	@Value("${videoVariableBitrate.title}")
	private String overviewTitle;

	@Value("${videoVariableBitrate.detailedTitle}")
	private String detailTitle;

	@Value("${videoVariableBitrate.desc}")
	private String aboutText;

	@Value("${videoVariableBitrate.url}")
	private String learnMoreUrl;

	@Value("${videoVariableBitrate.pass}")
	private String textResultPass;

	@Value("${videoVariableBitrate.results}")
	private String textResults;

	@Value("${videoVariableBitrate.init}")
	private String textResultInit;

	@Value("${videoVariableBitrate.empty}")
	private String textResultEmpty;

	@Value("${video.noData}")
	private String noData;
	
	@Value("${videoVariableBitrate.drmBlocking}")
	private String drmBlocking;
	
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

	private StreamingVideoData streamingVideoData;
	
	private VideoVariableBitrateResult result;
	private boolean vbrUsed = true;

	private int selectedManifestCount;
	private boolean hasSelectedManifest;
	private BPResultType bpResultType;

	private int invalidCount;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		bpResultType = BPResultType.SELF_TEST;
		result = new VideoVariableBitrateResult();

		init(result);
		
		if (tracedata == null) {
			return result;	
		}

		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {
			
			selectedManifestCount = streamingVideoData.getSelectedManifestCount();
			hasSelectedManifest = (selectedManifestCount > 0);
			invalidCount = streamingVideoData.getInvalidManifestCount();
			
			bpResultType = BPResultType.CONFIG_REQUIRED;
			
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
				bpResultType = BPResultType.NONE;
				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream.isValid() && videoStream.isSelected()) {
						if (videoStream.getManifest().isVideoMetaDataExtracted()) {
							Collection<VideoEvent> videoEventList = videoStream.getVideoEventsBySegment();
							double[] uniqueBitRates = videoEventList.stream().mapToDouble(ve -> ve.getBitrate()).distinct().toArray();
							if (uniqueBitRates.length == 1 && videoEventList.size() != 1) {
								vbrUsed = false;
								break;
							}
						} else {
							result.setResultText(drmBlocking);
							result.setResultType(BPResultType.SELF_TEST);
							return result;
						}
					}
				}
				if (vbrUsed) {
					result.setResultText(textResultPass);
					bpResultType = BPResultType.PASS;
				} else {
					result.setResultText(textResults);
					bpResultType = BPResultType.WARNING;
				}
			}
		} else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
		}
		result.setResultType(bpResultType);
		return result;
	}

	public void init(AbstractBestPracticeResult result) {
		vbrUsed = true;
		selectedManifestCount = 0;
		hasSelectedManifest = false;
		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
	}
}