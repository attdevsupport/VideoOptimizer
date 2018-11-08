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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoStallResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;


/**
 * <pre>
 * VBP #1
 * Video Stalls
 * 
 * ARO will measure the number and duration of stalls
 * 
 * About:
 * Stalling occurs when either a user’s device or their network cannot keep up with a video file when streaming. 
 * This results in a total pause of video playback.
 * 
 * Results:
 *	Your video had X # of streaming stalls.  
 *  By reducing the stalls (X instances, Y megs), your application should be X% faster and save X bandwidth.
 * 
 * Link goes to a view of video stalls.
 */
public class VideoStallImpl implements IBestPractice{
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

	@Value("${startUpDelay.init}")
	private String startUpDelayNotSet;

	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;

	@Autowired
	private IVideoUsagePrefsManager videoPref;
	
	private BPResultType stallState;
	private int warningCount, passCount, failCount;
    
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		List<VideoStall> videoStallResult = tracedata.getVideoStalls();
		List<VideoStall> stallResult = new ArrayList<VideoStall>();
		stallState = BPResultType.SELF_TEST;
		warningCount = 0;
		passCount = 0;
		failCount = 0;
		VideoStallResult result = new VideoStallResult();
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);

		double stallTriggerTime = videoPref.getVideoUsagePreference().getStallTriggerTime();
		int stallCount = 0;
		if (videoStallResult != null) {
			for (VideoStall stall : videoStallResult) {
				if (stall.getDuration() >= stallTriggerTime) {
					stallCount++;
					stallResult.add(updateStallResult(stall));
				}
			}
		}
		
		int count = 0;
		if (warningCount != 0 || failCount != 0) {
			if (failCount > 0) {
				stallState = BPResultType.FAIL;
				count = failCount;
			} else if (warningCount > 0) {
				stallState = BPResultType.WARNING;
				count = warningCount;
			}
		} else {
			stallState = BPResultType.PASS;
			count = passCount;
		}

		double startupDelay = videoPref.getVideoUsagePreference().getStartupDelay();
		
		if (Util.isTraceWithValidManifestsSelected(tracedata.getVideoUsage())
				&& !Util.isStartupDelaySet(tracedata.getVideoUsage())) {
			// Meaning startup delay is not set yet
			stallState = BPResultType.CONFIG_REQUIRED;
			result.setResultText(
					MessageFormat.format(startUpDelayNotSet, startupDelay, startupDelay == 1 ? "" : "s"));
		} else if (!Util.isTraceWithValidManifestsSelected(tracedata.getVideoUsage())) {
			result.setResultText(MessageFormat.format(novalidManifestsFound, stallCount));
			stallState = BPResultType.SELF_TEST;
		} else {
			result.setResultText(MessageFormat.format(this.textResults, stallCount, stallCount == 1 ? "" : "s",
					count == 1 ? "was" : "were", count, count == 1 ? "" : "s", stallState == BPResultType.FAIL ? "fail"
							: (stallState == BPResultType.WARNING ? "warning" : "pass")));
		}
		result.setResultType(stallState);
		result.setVideoStallResult(stallCount);
		result.setResults(stallResult);
		return result;
	}

	private VideoStall updateStallResult(VideoStall stall) {
		BPResultType bpResultType = Util.checkPassFailorWarning(stall.getDuration(),
				Double.parseDouble(videoPref.getVideoUsagePreference().getStallDurationWarnVal()),
				Double.parseDouble(videoPref.getVideoUsagePreference().getStallDurationFailVal()));
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

}// end class