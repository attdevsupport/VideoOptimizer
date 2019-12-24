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

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.AudioStreamResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
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
public class AudioStreamImpl implements IBestPractice {
	@Value("${audioStream.title}")
	private String overviewTitle;
	
	@Value("${audioStream.detailedTitle}")
	private String detailTitle;
	
	@Value("${audioStream.desc}")
	private String aboutText;
	
	@Value("${audioStream.url}")
	private String learnMoreUrl;
	
	@Value("${audioStream.pass}")
	private String textResultPass;
	
	@Value("${audioStream.results}")
	private String textResults;
	
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

	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();
	
	private StreamingVideoData streamingVideoData;

	private int selectedManifestCount;
	private boolean hasSelectedManifest;
	private BPResultType bpResultType;

	private AudioStreamResult result;

	private int invalidCount;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		result = new AudioStreamResult();
		init(result);

		int count = 0;
		
		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {
			
			bpResultType = BPResultType.CONFIG_REQUIRED;
			
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
				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream != null && videoStream.isSelected()) {
						count += videoStream.getAudioEventList().size();
					}
				}
				bpResultType = BPResultType.SELF_TEST;
				result.setResultText(MessageFormat.format(
						textResults
						,count == 1 ? "was" : "were"
						,count == 0 ? "no" : count
						,count == 1 ? "" : "s"
						));
				result.setCount(count);
				result.setSelfTest(true);
			}
		} else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
		}

		result.setResultType(bpResultType);
		return result;
	}

	private void init(AudioStreamResult result) {
		bpResultType = BPResultType.SELF_TEST;
		selectedManifestCount = 0;
		hasSelectedManifest = false;
		invalidCount = 0;

		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setOverviewTitle(overviewTitle);
		result.setLearnMoreUrl(learnMoreUrl);
	}

}