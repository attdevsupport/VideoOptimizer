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
import com.att.aro.core.bestpractice.pojo.VideoChunkSizeResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

/**
 * <pre>
 * VBP #6 Video Chunk Size
 * 
 * Criteria: ARO will measure the size of chunks in video stream, in order to help developers determine the most efficient size
 * 
 * About: Streaming chunks vary in size. Understanding the size of chunks in a video stream can help to determine the most efficient chunk size.
 * 
 * Result: There were X different chunks with an average of Y bytes. Consider reducing/increasing the size of chunks you are sending.
 * 
 * Link: goes to a view of chunks measured in bytes.
 */
public class VideoChunkSizeImpl implements IBestPractice{

	@InjectLogger
	private static ILogger log;

	@Value("${chunkSize.title}")
	private String overviewTitle;

	@Value("${chunkSize.detailedTitle}")
	private String detailTitle;

	@Value("${chunkSize.desc}")
	private String aboutText;

	@Value("${chunkSize.url}")
	private String learnMoreUrl;

	@Value("${chunkSize.pass}")
	private String textResultPass;

	@Value("${chunkSize.results}")
	private String textResults;

	private double averageSize;

	private double count;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		
		VideoUsage videoUsage = tracedata.getVideoUsage();

		count = 0;
		averageSize = 0;
		double totalSize = 0;
		if (videoUsage != null && videoUsage.getAroManifestMap() != null) {

			for (AROManifest aroManifest : videoUsage.getAroManifestMap().values()) {
				if (!aroManifest.getVideoEventList().isEmpty()) {

					for (VideoEvent videoEvent : aroManifest.getVideoEventsBySegment()) {

						if (ManifestDash.class.isInstance(aroManifest) && videoEvent.getSegment() == 0) {
							continue;
						}
						count++;
						totalSize += videoEvent.getSize();
					}
				}
			}
		}

		if (count != 0) {
			averageSize = totalSize / count;
		}

		VideoChunkSizeResult result = new VideoChunkSizeResult();
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
				(int) averageSize / 1024
				));

		return result;
	}

}// end class
