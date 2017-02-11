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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.BufferOccupancyResult;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.BufferOccupancyBPResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;



/**
 * <pre>
 * VBP #3
 * Video Buffer Occupancy
 * 
 * Criteria:
 *  ARO will measure frequency and duration of video stream buffers
 *  
 * About:
 *  Buffer occupancy is the amount of video stored in RAM to help prevent interruption due to transmission delays, known as "buffering".
 * 
 * Result:
 *  Your video had X size buffer occupancy By  managing  buffer occupancy you can make sure it is not too long or too short.
 * 
 * Link:
 *  goes to a view of buffers.
 * 

 */
public class VideoBufferOccupancyImpl implements IBestPractice{

	@InjectLogger
	private static ILogger log;
	
	@Value("${bufferOccupancy.title}")
	private String overviewTitle;
	
	@Value("${bufferOccupancy.detailedTitle}")
	private String detailTitle;
	
	@Value("${bufferOccupancy.desc}")
	private String aboutText;
	
	@Value("${bufferOccupancy.url}")
	private String learnMoreUrl;
	
	@Value("${bufferOccupancy.pass}")
	private String textResultPass;
	
	@Value("${bufferOccupancy.results}")
	private String textResults;
	
	//private PreferenceHandlerImpl prefs;
	
	@Autowired
    private IVideoUsagePrefsManager videoUsagePrefs;
	
	private double maxBufferSet;
	double maxBufferReached;
	
	public void updateVideoPrefMaxBuffer(){
		if(videoUsagePrefs.getVideoUsagePreference() != null){
			maxBufferSet=videoUsagePrefs.getVideoUsagePreference().getMaxBuffer();
		}
	}
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		BufferOccupancyBPResult bufferBPResult = tracedata.getBufferOccupancyResult();
		if(bufferBPResult != null){
			maxBufferReached = bufferBPResult.getMaxBuffer();// maxBufferReached is in KB (1024)
			maxBufferReached = maxBufferReached/1024; //change to MB (2^20)
		}

	/*	VideoUsage videoUsage = tracedata.getVideoUsage();
		TreeMap<Double, AROManifest> videoEventList = videoUsage.getVideoEventList();
		
		int count = 0;
		for (AROManifest aroManifest : videoEventList.values()) {
			if (!aroManifest.getVideoEventList().isEmpty()) { // don't count if no videos with manifest
				count += 2;
			}
		}
		*/
		BufferOccupancyResult result = new BufferOccupancyResult();
		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setResultType(BPResultType.SELF_TEST);

		//TODO fix this
	/*	if (count == 0) {
			result.setResultText(textResultPass);
		} else {
			result.setResultText(MessageFormat.format(this.textResults, count));
		}*/
		updateVideoPrefMaxBuffer();
		double percentage = 0;
		if(maxBufferSet != 0){
			percentage = (maxBufferReached/maxBufferSet)*100; 
		}
		result.setResultText(MessageFormat.format(this.textResults,String.format("%.2f",percentage),String.format("%.2f", maxBufferReached),String.format("%.2f",maxBufferSet)));
		return result;
	}


}//end class
