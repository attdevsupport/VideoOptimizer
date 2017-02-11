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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoStallResult;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
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

	@InjectLogger
	private static ILogger log;
	
	private boolean done;

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
	
    @Autowired
	private IVideoUsagePrefsManager videoPref;

    
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		List<VideoStall> videoStallResult = tracedata.getVideoStalls();
		done = true;
	
		VideoStallResult result = new VideoStallResult();
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		
		
		// TODO fix this
			double stallTriggerTime = videoPref.getVideoUsagePreference().getStallTriggerTime(); // BufferInSecondsCalculatorImpl.getStallTriggerTime();
			int stallCount=0;
			if(videoStallResult != null){
				for(VideoStall stall: videoStallResult){
					if(stall.getDuration()>=stallTriggerTime){
						stallCount++;
					}
				}
			}	
			result.setResultType((stallCount==0)? BPResultType.PASS : BPResultType.FAIL);
			result.setResultText(MessageFormat.format(this.textResults, 
					stallCount, 
					stallCount == 1? "" : "s",
					stallCount == 0? "passes the test" : "fails the test"
		     ));

		return result;
	}
	
}//end class
