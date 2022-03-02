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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoStartUpDelayResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoStartup;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

/**
 * <pre>
 * VBP #2 Video Start-up Delay
 * 
 * Criteria: ARO will identify video stream startup delays and their length in seconds.
 * 
 * About: Streaming video requires a startup delay for smooth delivery. In order to manage buffer occupancy, it is important to understand the startup delay and
 * determine a way to cover this delay for the user with messaging.
 * 
 * startUpDelay.results=
 * Your video had {0} second{1} of startup delay, your defined delay is {2} second{3}.
 * You can determine buffer occupancy and manage the user experience..
 * 
 * startUpDelay.pass=
 * Your video had {0} second{1} of startup delays and passes the test.
 * 
 * Link: goes to a view of video startup delays.
 * 
 */
public class VideoStartUpDelayImpl implements IBestPractice {
	@Value("${startUpDelay.title}")
	private String overviewTitle;

	@Value("${startUpDelay.detailedTitle}")
	private String detailTitle;

	@Value("${startUpDelay.desc}")
	private String aboutText;

	@Value("${startUpDelay.url}")
	private String learnMoreUrl;

	@Value("${startUpDelay.pass}")
	private String textResultPass;

	@Value("${startUpDelay.results}")
	private String textResults;

	@Value("${startUpDelay.excel.results}")
    private String textExcelResults;

	@Value("${startUpDelay.init}")
	private String startUpDelayNotSet;

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
	
	@Autowired
	private IVideoUsagePrefsManager videoPref;

	@Value("${video.noData}")
	private String noData;

	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();

	private StreamingVideoData streamingVideoData;
	
	private double startupDelay;
	private double manifestRequestTime;
	private double playDelay;
	private double manifestDeliveredTime;

	boolean hasSelectedManifest = false;
	boolean startupDelaySet = false;

	private double warningValue;

	private int selectedManifestCount;
	private VideoStartUpDelayResult result;

	private int invalidCount;
			
	private static final Logger LOG = LogManager.getLogger(VideoStartUpDelayImpl.class.getName());

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		
		BPResultType bpResultType = BPResultType.SELF_TEST;
		result = new VideoStartUpDelayResult();

		init(result);

		warningValue = videoPref.getVideoUsagePreference().getStartUpDelayWarnVal();
		
		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {
			
			selectedManifestCount = streamingVideoData.getSelectedManifestCount();
			hasSelectedManifest = (selectedManifestCount > 0);
			invalidCount = streamingVideoData.getInvalidManifestCount();

			startupDelay = videoPref.getVideoUsagePreference().getStartupDelay(); // default startup delay
			startupDelaySet = false;
			
			bpResultType = BPResultType.CONFIG_REQUIRED;
            result.setResultExcelText(BPResultType.CONFIG_REQUIRED.getDescription());
				
			if (selectedManifestCount == 0) {
				if (invalidCount == videoStreamCollection.size()) {
					result.setResultText(invalidManifestsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noManifestsSelectedMixed);
				} else {
					if (videoStreamCollection.size() > 0 && MapUtils.isEmpty(streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList())) {
						result.setResultText(startUpDelayNotSet);
					} else {
						result.setResultText(noManifestsSelected);
					}
				}
			} else if (MapUtils.isEmpty(streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList())) {
				result.setResultText(startUpDelayNotSet);
			} else if (selectedManifestCount > 1) {
				result.setResultText(multipleManifestsSelected);
			} else if (hasSelectedManifest) {
				startupDelaySet = true;

				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream.isSelected() && MapUtils.isNotEmpty(videoStream.getVideoEventMap()) && videoStream.getManifest().getStartupVideoEvent() != null) {
						Manifest manifest = videoStream.getManifest();
						manifestRequestTime = manifest.getRequestTime();
						// - tracedata.getTraceresult().getPcapTimeOffset();
						manifestDeliveredTime = manifest.getEndTime() - manifestRequestTime;
						startupDelay = manifest.getStartupDelay() - manifestRequestTime;
						playDelay = manifest.getStartupDelay() - manifestRequestTime;
						LOG.info(String.format("startup segment = %s", manifest.getStartupVideoEvent()));
						LOG.info(String.format("segment startupDelay = %.03f", startupDelay));
						LOG.info(String.format("videoStream request to segment_plays = %.03f", manifest.getStartupVideoEvent().getPlayTime() - manifestRequestTime));
						LOG.info(String.format("segment_plays = %.03f", manifest.getStartupVideoEvent().getPlayTime()));

						List<VideoStartup> compApps = new ArrayList<>();
						compApps.add(new VideoStartup("RefApp 1", 0.914, 3.423));
						compApps.add(new VideoStartup("RefApp 2", 3.27, 8.400));
						compApps.add(new VideoStartup("RefApp 3", 2.409, 3.969));
						VideoStartup testedApp = new VideoStartup("Tested", manifestDeliveredTime, playDelay);
						compApps.add(testedApp);

						result.setResults(compApps);

						bpResultType = Util.checkPassFailorWarning(startupDelay, warningValue);
						if (bpResultType.equals(BPResultType.PASS)) {
							result.setResultText(MessageFormat.format(textResultPass, startupDelay, startupDelay == 1 ? "" : "s"));
							result.setResultExcelText(BPResultType.PASS.getDescription());
						} else {
							result.setResultText(MessageFormat.format(
									textResults
									, String.format("%.03f", startupDelay)
									, startupDelay == 1 ? "" : "s"
									, String.format("%.04f", warningValue)
									, warningValue == 1 ? "" : "s"
								));
							result.setResultExcelText(
						        MessageFormat.format(textExcelResults, bpResultType.getDescription(), String.format("%.03f", startupDelay))
					        );
						}

						break;
					}
				}
				
			} else {
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultText(novalidManifestsFound);
				result.setResultExcelText(BPResultType.CONFIG_REQUIRED.getDescription());
			}

			result.setStartUpDelay(startupDelay);
			
		} else {
			// No Data
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
			result.setResultExcelText(BPResultType.NO_DATA.getDescription());
		}
		result.setResultType(bpResultType);
		return result;
	}


	public void init(AbstractBestPracticeResult result) {
		selectedManifestCount = 0;
		invalidCount = 0;
		startupDelay = 0;
		manifestRequestTime = 0;
		playDelay = 0;
		manifestDeliveredTime = 0;
		warningValue = 0;
		hasSelectedManifest = false;
		startupDelaySet = false;
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
	}

}// end class