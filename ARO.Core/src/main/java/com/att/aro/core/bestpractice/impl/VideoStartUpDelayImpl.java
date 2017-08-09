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
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoStartUpDelayResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.VideoChunkPlotterImpl;
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
public class VideoStartUpDelayImpl implements IBestPractice{

	@InjectLogger
	private static ILogger log;

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
	private String textResultInit;
	
	@Value("${startUpDelay.empty}")
	private String textResultEmpty;
	
	@Autowired
	private IVideoUsagePrefsManager videoPref;

	private double startupDelay; // = 9999;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		VideoUsage videoUsage = tracedata.getVideoUsage();
		TreeMap<Double, AROManifest> videoEventList = videoUsage.getAroManifestMap(); //getVideoEventList();

		double count = 0;
		double definedDelay = videoPref.getVideoUsagePreference().getStartupDelay();
		startupDelay = definedDelay;
		

		VideoStartUpDelayResult result = new VideoStartUpDelayResult();
		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setResultType(BPResultType.SELF_TEST);	// this VideoBestPractice is to be reported as a selftest until further notice
		if(videoEventList.isEmpty()){
			result.setResultText(textResultEmpty);
		}else{
			if(PlotHelperAbstract.chunkPlayTimeList.size() ==0){
				result.setResultText(MessageFormat.format(textResultInit, startupDelay, startupDelay ==1 ?"":"s"));
			} else {
				// if (videoEventList != null) {
				double delay = 0;
				for (AROManifest aroManifest : videoEventList.values()) {
					if (aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty()) {
						// don't count if no videos with manifest
						// locate shortest startupDelay
						delay += aroManifest.getDelay();
						count++;
						startupDelay = aroManifest.getDelay();
					}
				}

				result.setResultText(MessageFormat.format(textResults, startupDelay // 0
						, startupDelay == 1 ? "" : "s" // 1
						, definedDelay // 2
						, definedDelay == 1 ? "" : "s" // 3
				));
			}
		}
		
		result.setStartUpDelay(startupDelay);

		/*
		 * startUpDelay.results=
		 * Your video had {0} second{1} of startup delay, your defined delay is {2} second{3}. 
		 * You can determine buffer occupancy and manage the user experience.. 
		 * 
		 * startUpDelay.pass=
		 * Your video had {0} second{1} of startup delays and passes the test.
		 */
		
		

		return result;
	}
}// end class
