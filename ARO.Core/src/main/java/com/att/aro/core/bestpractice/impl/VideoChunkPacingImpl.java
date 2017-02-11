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

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoChunkPacingResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;


/**
 * <pre>
 * VBP #7
 * Video Chunk Pacing
 * 
 * Criteria:
 *  ARO will measure the pacing of chunks in video stream, in order to help developers determine the most efficient timing.
 *  
 * About:
 *  Understanding the pacing of chunks in a video stream can help to determine the most efficient delivery.
 *  
 * Result:
 *  There were X different chunks which were delivered with pace of 2 seconds. 
 *  Consider reducing/increasing the pace of chunks you are sending.
 *  
 * Link:
 *  goes to a view of chunk pacing measured in seconds.
 */
public class VideoChunkPacingImpl implements IBestPractice{

	@InjectLogger
	private static ILogger log;

	@Value("${chunkPacing.title}")
	private String overviewTitle;
	
	@Value("${chunkPacing.detailedTitle}")
	private String detailTitle;
	
	@Value("${chunkPacing.desc}")
	private String aboutText;
	
	@Value("${chunkPacing.url}")
	private String learnMoreUrl;
	
	@Value("${chunkPacing.pass}")
	private String textResultPass;
	
	@Value("${chunkPacing.results}")
	private String textResults;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		
		VideoUsage videoUsage = tracedata.getVideoUsage();
				
		double lastDl = 0;
		double averageDelay = 0;
		double count = 0;
		if (videoUsage != null && videoUsage.getAroManifestMap() != null) {

			for (AROManifest aroManifest : videoUsage.getAroManifestMap().values()) {
				if (!aroManifest.getVideoEventList().isEmpty()) { // don't count if no videos with manifest
					for (VideoEvent videoEvent : aroManifest.getVideoEventsBySegment()) {

						if (ManifestDash.class.isInstance(aroManifest) && videoEvent.getSegment() == 0) {
							continue;
						}
						count++;
						double dlTime = videoEvent.getDLTimeStamp();
						if (lastDl > 0) {
							averageDelay += dlTime - lastDl;
						}
						lastDl = dlTime;
					}
				}
			}
		}
		
		double chunkPacing = 0;
		if (count != 0) {
			chunkPacing = averageDelay / count;
		}

		VideoChunkPacingResult result = new VideoChunkPacingResult();
		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setResultType(BPResultType.SELF_TEST); // this VideoBestPractice is to be reported as a selftest until further notice
		result.setResultText(MessageFormat.format(textResults, 
				count == 1? "was" : "were", 
				count, 
				count == 1? "" : "different", 
				count == 1? "" : "s", 
				count == 1? "was" : "were", 
				chunkPacing, 
				chunkPacing == 1? "" : "s"
					));
		
		return result;
	}
	
}//end class
