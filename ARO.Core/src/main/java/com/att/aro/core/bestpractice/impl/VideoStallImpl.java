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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoStallResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

/**
 * <pre>
 * VBP #1 Video Stalls
 * 
 * ARO will measure the number and duration of stalls
 * 
 * About: Stalling occurs when either a user’s device or their network cannot keep up with a video file when streaming. This results in a total pause of video
 * playback.
 * 
 * Results: Your video had X # of streaming stalls. By reducing the stalls (X instances, Y megs), your application should be X% faster and save X bandwidth.
 * 
 * Link goes to a view of video stalls.
 */
public class VideoStallImpl implements IBestPractice {
	@Value("${videoStall.title}")
	private String overviewTitle;

	@Value("${videoStall.detailedTitle}")
	private String detailTitle;

	@Value("${videoStall.desc}")
	private String aboutText;

	@Value("${videoStall.url}")
	private String learnMoreUrl;

	@Value("${videoStall.pass}")
	private String textResultPass;

	@Value("${videoStall.results}")
	private String textResults;

	@Value("${videoStall.excel.results}")
    private String textExcelResults;

	@Value("${startUpDelay.init}")
	private String startUpDelayNotSet;

	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;

	@Value("${video.noData}")
	private String noData;

	@Value("${videoManifest.multipleManifestsSelected}")
	private String multipleManifestsSelected;
	@Value("${videoManifest.noManifestsSelected}")
	private String noManifestsSelected;
	
	@Value("${videoManifest.noManifestsSelectedMixed}")
	private String noManifestsSelectedMixed;
	@Value("${videoManifest.invalid}")
	private String invalidManifestsFound;

	@Autowired
	private IVideoUsagePrefsManager videoPref;

	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();
	
	private StreamingVideoData streamingVideoData;

	private int warningCount, passCount, failCount;

	private int selectedManifestCount;
	private boolean hasSelectedManifest;
	private BPResultType bpResultType;

	private VideoStallResult result;

	private int invalidCount;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {	
		List<VideoStall> stallResult = new ArrayList<VideoStall>();

		result = new VideoStallResult();
		init(result);

		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {
			
			bpResultType = BPResultType.CONFIG_REQUIRED;
			result.setResultExcelText(BPResultType.CONFIG_REQUIRED.getDescription());

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
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultText(multipleManifestsSelected);
				result.setResultExcelText(bpResultType.getDescription());
				result.setSelfTest(false);
			} else if (hasSelectedManifest) {
				int stallCount = 0;
				bpResultType = BPResultType.SELF_TEST;
				double stallTriggerTime = videoPref.getVideoUsagePreference().getStallTriggerTime();
				Optional<Entry<Double, VideoStream>> vsFound = videoStreamCollection.entrySet()
						.stream()
						.filter(f -> f.getValue().isSelected() && !f.getValue().getVideoActiveMap().isEmpty())
						.findFirst();
				
				if (vsFound.isPresent()) {
					VideoStream videoStream = vsFound.get().getValue();

					for (VideoStall stall : videoStream.getVideoStallList()) {
						if (stall.getDuration() >= stallTriggerTime) {
							stallCount++;
							stallResult.add(updateStallResult(stall));
						}
					}
				}

				int count = 0;
				if (warningCount != 0 || failCount != 0) {
					if (failCount > 0) {
						bpResultType = BPResultType.FAIL;
						count = failCount;
					} else if (warningCount > 0) {
						bpResultType = BPResultType.WARNING;
						count = warningCount;
					}
				} else {
					bpResultType = BPResultType.PASS;
					count = passCount;
				}

				double startupDelay = videoPref.getVideoUsagePreference().getStartupDelay();

				if (MapUtils.isEmpty(streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList())) {
					// Meaning startup delay is not set yet
					bpResultType = BPResultType.CONFIG_REQUIRED;
					result.setResultText(MessageFormat.format(startUpDelayNotSet, startupDelay, startupDelay == 1 ? "" : "s"));
					result.setResultExcelText(BPResultType.CONFIG_REQUIRED.getDescription());
				} else {
					result.setResultText(MessageFormat.format(this.textResults
							, stallCount
							, stallCount == 1 ? "" : "s"
							, count == 1 ? "was" : "were"
							, count, count == 1 ? "" : "s"
							, bpResultType.toString().toLowerCase()));

					switch (bpResultType) {
					    case PASS:
					        result.setResultExcelText(BPResultType.PASS.getDescription());
					        break;
					    case SELF_TEST:
					        result.setResultExcelText(BPResultType.SELF_TEST.getDescription());
					    case WARNING:
					    case FAIL:
					        result.setResultExcelText(MessageFormat.format(textExcelResults, bpResultType.getDescription(), stallCount));
					        break;
                        default:
                            break;
					}
				}
				result.setVideoStallResult(stallCount);
				result.setResults(stallResult);
			}
		} else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
			result.setResultExcelText(BPResultType.NO_DATA.getDescription());
		}

		result.setResultType(bpResultType);
		return result;
	}

	private void init(VideoStallResult result) {
		warningCount = 0;
		passCount = 0;
		failCount = 0;

		bpResultType = BPResultType.SELF_TEST;
		selectedManifestCount = 0;
		hasSelectedManifest = false;
		invalidCount = 0;

		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setOverviewTitle(overviewTitle);
		result.setLearnMoreUrl(learnMoreUrl);
	}

	private VideoStall updateStallResult(VideoStall stall) {
		BPResultType bpResultType = Util.checkPassFailorWarning(stall.getDuration()
									, videoPref.getVideoUsagePreference().getStallDurationWarnVal()
									, videoPref.getVideoUsagePreference().getStallDurationFailVal());
		if (bpResultType == BPResultType.FAIL) {
			failCount = failCount + 1;
		} else if (bpResultType == BPResultType.PASS) {
			passCount = passCount + 1;
		} else {
			warningCount = warningCount + 1;
		}
		stall.setStallState(bpResultType.toString());

		return stall;
	}
}