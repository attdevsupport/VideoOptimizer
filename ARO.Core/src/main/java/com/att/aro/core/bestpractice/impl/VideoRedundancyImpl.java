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
import com.att.aro.core.bestpractice.pojo.VideoRedundancyResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

/**
 * 
 * VBP #8
 * Redundant Versions of Same Video Content
 * 
 * Criteria:
 *  ARO will identify and compare redundant content sent in additional versions with alternative quality.
 *  
 * Metric:
 *  Count how many additional versions for each chunk/segment. Example Segment 5 has 3 different quality levels, this would be counted as 2 redundant versions.
 *  
 * About:
 *  HTTP Streaming generates multiple versions of the same content in different quality
 *  , which allows the client to display the most appropriate version. 
 *  Understanding the number and quality of these versions can help a developer avoid overkill.
 *  
 * Result:
 *  There were X different versions of the same video segment. The optimal number of number of alternative versions could improve efficiency, while reducing congestion and preparation effort.
 *  
 * Link:
 *  goes to a view of all versions of identical content.
 */
public class VideoRedundancyImpl implements IBestPractice{

	@InjectLogger
	private static ILogger log;

	@Value("${videoRedundancy.title}")
	private String overviewTitle;

	@Value("${videoRedundancy.detailedTitle}")
	private String detailTitle;

	@Value("${videoRedundancy.desc}")
	private String aboutText;

	@Value("${videoRedundancy.url}")
	private String learnMoreUrl;

	@Value("${videoRedundancy.pass}")
	private String textResultPass;

	@Value("${videoRedundancy.results}")
	private String textResults;

//	private String manifestLabel = null;
//	private String videoLabel = null;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		VideoRedundancyResult result = new VideoRedundancyResult();

		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);

		VideoUsage videoUsage = tracedata.getVideoUsage();
		if (videoUsage != null) {

			int count = 0;

			// count duplicate chunks (same segment number)
			for (AROManifest aroManifest : videoUsage.getManifests()) {
				VideoEvent preStuff = null;
				for (VideoEvent stuff : aroManifest.getVideoEventsBySegment()) {
					if (aroManifest instanceof ManifestDash && stuff.getSegment() == 0 ){
						continue;
					}
					if (preStuff != null 
							&& preStuff.getSegment() == stuff.getSegment()
							&& !preStuff.getQuality().equals(stuff.getQuality())
							) {
						System.out.println("Redundant :\n" + preStuff +"\n"+ stuff);
						count++;
					}
					preStuff = stuff;
				}
			}

			result.setResultType(BPResultType.SELF_TEST); // this VideoBestPractice is to be reported as a selftest until further notice
			result.setResultText(
					MessageFormat.format(textResults, 
							count == 1? "was" : "were", 
							count,
							count == 1? "" : "different",
							count == 1? "" : "s"
								));
		}
		return result;
	}

}
