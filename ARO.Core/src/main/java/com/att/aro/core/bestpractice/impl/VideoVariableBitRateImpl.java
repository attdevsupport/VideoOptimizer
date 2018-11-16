
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
import java.util.Collection;
import org.springframework.beans.factory.annotation.Value;
import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoVariableBitRateResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoVariableBitRateImpl implements IBestPractice{

	@Value("${videoVariableBitrate.title}")
	private String overviewTitle;
	
	@Value("${videoVariableBitrate.detailedTitle}")
	private String detailTitle;
	
	@Value("${videoVariableBitrate.desc}")
	private String aboutText;
	
	@Value("${videoVariableBitrate.url}")
	private String learnMoreUrl;
	
	@Value("${videoVariableBitrate.pass}")
	private String textResultPass;
	
	@Value("${videoVariableBitrate.results}")
	private String textResults;
	
	@Value("${videoVariableBitrate.init}")
	private String textResultInit;
	
	@Value("${videoVariableBitrate.empty}")
	private String textResultEmpty;
	
	private boolean vbrUsed = true;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		VideoVariableBitRateResult result = new VideoVariableBitRateResult();

		Collection<AROManifest> manifests = tracedata.getVideoUsage().getManifests();
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBaseNew()));
		result.setOverviewTitle(overviewTitle);
		result.setResultType(BPResultType.NONE);

		if (manifests == null || manifests.isEmpty()) {
			result.setResultType(BPResultType.PASS);
			result.setResultText(textResultEmpty);
		} else {
			int invalidManifestCount = 0;
			for (AROManifest manifest : manifests) {
				if (manifest.isValid()) {
					if (manifest.isVideoMetaDataExtracted()) {
						Collection<VideoEvent> videoEventList = manifest.getVideoEventsBySegment();
						double[] uniqueBitRates = videoEventList.stream().mapToDouble(ve -> ve.getBitrate()).distinct()
								.toArray();
						if (uniqueBitRates.length == 1 && videoEventList.size() != 1) {
							vbrUsed = false;
							break;
						}
					} else {
						result.setResultType(BPResultType.SELF_TEST);
						result.setResultText(textResultInit); // unable to tell
																// as video is
																// DRM protected
						return result;
					}
				} else {
					invalidManifestCount++;
				}
			}
			if (invalidManifestCount == manifests.size()) { // all manifests are
															// invalid ==> no
															// video data
				result.setResultType(BPResultType.PASS);
				result.setResultText(textResultEmpty);
			} else if (vbrUsed) {
				result.setResultText(textResultPass);
				result.setResultType(BPResultType.PASS);
			} else {
				result.setResultText(textResults);
				result.setResultType(BPResultType.WARNING);
			}
		
		}	
		return result;
	}

}

