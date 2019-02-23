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

import javax.annotation.Nonnull;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.math3.util.MathUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoChunkPacingResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
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
public class VideoSegmentPacingImpl implements IBestPractice{
	@Value("${segmentPacing.title}")
	private String overviewTitle;
	
	@Value("${segmentPacing.detailedTitle}")
	private String detailTitle;
	
	@Value("${segmentPacing.desc}")
	private String aboutText;
	
	@Value("${segmentPacing.url}")
	private String learnMoreUrl;
	
	@Value("${segmentPacing.pass}")
	private String textResultPass;
	
	@Value("${segmentPacing.results}")
	private String textResults;
	
	@Value("${video.noData}")
	private String noData;
	
	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;

	@Value("${videoManifest.multipleManifestsSelected}")
	private String multipleManifestsSelected;

	@Value("${videoManifest.noManifestsSelected}")
	private String noManifestsSelected;
	
	@Value("${videoManifest.noManifestsSelectedMixed}")
	private String noManifestsSelectedMixed;
	
	@Value("${videoManifest.invalid}")
	private String invalidManifestsFound;
	
	@Nonnull
	private SortedMap<Double, AROManifest> manifestCollection = new TreeMap<>();
	
	@Nonnull
	VideoUsage videoUsage;

	private int selectedCount;
	private int invalidCount;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		
		BPResultType bpResultType = BPResultType.SELF_TEST;
		VideoChunkPacingResult result = new VideoChunkPacingResult();
		double lastDl = 0;
		double averageDelay = 0;
		int count = 0;
		
		init(result);
		
		videoUsage = tracedata.getVideoUsage();

		if (videoUsage != null) {
			manifestCollection = videoUsage.getAroManifestMap();
		}

		if (MapUtils.isNotEmpty(manifestCollection)) {
			selectedCount = videoUsage.getSelectedManifestCount();
			invalidCount = videoUsage.getInvalidManifestCount();
			
			if (selectedCount == 0) {
				if (invalidCount == manifestCollection.size()) {
					result.setResultText(invalidManifestsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noManifestsSelectedMixed);
				} else {
					result.setResultText(noManifestsSelected);
				}
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setSelfTest(false);
			} else if (selectedCount > 1) {
				result.setResultText(multipleManifestsSelected);
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setSelfTest(false);
			} else {
				for (AROManifest aroManifest : manifestCollection.values()) {
					if (aroManifest != null && aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty()) { // don't count if no videos with manifest
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
						break;
					}
				}
				double segmentPacing = 0;
				if (count != 0) {
					segmentPacing = averageDelay / count;
				}
				bpResultType = BPResultType.SELF_TEST; // this VideoBestPractice is to be reported as a selftest until further notice
				result.setResultText(MessageFormat.format(
						textResults, count == 1 ? "was" : "were"
						,count
						,count == 1 ? "" : "different"
						,count == 1 ? "" : "s"
						,count == 1 ? "was" : "were"
						,segmentPacing
						,MathUtils.equals(segmentPacing, 1.0) ? "" : "s"));
				result.setChunkPacing(segmentPacing);
				result.setSelfTest(true);
			}
		  } else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
		}
		
		result.setResultType(bpResultType);
		return result;
	}
	
	public void init(VideoChunkPacingResult result) {
		selectedCount = 0;
		invalidCount = 0;
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
	}
}
