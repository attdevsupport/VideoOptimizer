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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoStartUpDelayResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.AROManifest;

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

	@Value("${startUpDelay.init}")
	private String startUpDelayNotSet;

	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;

	@Autowired
	private IVideoUsagePrefsManager videoPref;

	private double startupDelay;

	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		VideoStartUpDelayResult result = new VideoStartUpDelayResult();
		
		VideoUsage videoUsage = tracedata.getVideoUsage();
		TreeMap<Double, AROManifest> manifestCollection = null;
		
		if (videoUsage != null) {
			manifestCollection = videoUsage.getAroManifestMap(); // getVideoEventList();
		}
		
		double definedDelay = videoPref.getVideoUsagePreference().getStartupDelay();
		startupDelay = definedDelay;

		boolean isStartupDelaySet = false;
		// result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		BPResultType bpResultType = BPResultType.SELF_TEST;
		// this VideoBestPractice is to be reported as a selftest until further
		// notice
		if (manifestCollection == null || manifestCollection.isEmpty()) {
			result.setResultText(novalidManifestsFound);
		} else {
			if (tracedata.getVideoUsage() != null && tracedata.getVideoUsage().getChunkPlayTimeList().isEmpty()) {
				result.setResultText(
						MessageFormat.format(startUpDelayNotSet, startupDelay, startupDelay == 1 ? "" : "s"));
			} else {
				
				for (AROManifest aroManifest : manifestCollection.values()) {
					if (aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty()) {
						startupDelay = aroManifest.getDelay();
					}
				}
				isStartupDelaySet = true;
				result.setResultText(MessageFormat.format(textResults, startupDelay // 0
						, startupDelay == 1 ? "" : "s" // 1
						, definedDelay // 2
						, definedDelay == 1 ? "" : "s" // 3
				));
			}
		}
		result.setStartUpDelay(startupDelay);

		if (isStartupDelaySet) {
			bpResultType = Util.checkPassFailorWarning(startupDelay,
					Double.parseDouble(videoPref.getVideoUsagePreference().getStartUpDelayWarnVal()),
					Double.parseDouble(videoPref.getVideoUsagePreference().getStartUpDelayFailVal()));
		} else if (!isStartupDelaySet && Util.isTraceWithValidManifestsSelected(tracedata.getVideoUsage())) {
			bpResultType = BPResultType.CONFIG_REQUIRED;
			result.setResultText(
					MessageFormat.format(startUpDelayNotSet, startupDelay, startupDelay == 1 ? "" : "s"));
		} else if (!Util.isTraceWithValidManifestsSelected(tracedata.getVideoUsage())) {
			result.setResultText(novalidManifestsFound);
		}
		result.setResultType(bpResultType);
		return result;
	}
}// end class
