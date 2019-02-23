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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoRedundancyResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

/**
 * 
 * VBP #8 Redundant Versions of Same Video Content
 * 
 * Criteria: ARO will identify and compare redundant content sent in additional versions with alternative quality.
 * 
 * Metric: Count how many additional versions for each chunk/segment. Example Segment 5 has 3 different quality levels, this would be counted as 2 redundant
 * versions.
 * 
 * About: HTTP Streaming generates multiple versions of the same content in different quality , which allows the client to display the most appropriate version.
 * Understanding the number and quality of these versions can help a developer avoid overkill.
 * 
 * Result: (orig) There were X different versions of the same video segment. The optimal number of number of alternative versions could improve efficiency,
 * while reducing congestion and preparation effort.
 * 
 * videoRedundancy.results=There {0} {1} {2} version{3} of the same video. The optimal number of alternative versions could improve efficiency, while reducing
 * congestion and preparation effort. videoRedundancy.pass=There were {0} different versions and passes the test.
 *
 * Link: goes to a view of all versions of identical content.
 */
public class VideoRedundancyImpl implements IBestPractice {

	private static final Logger LOG = LogManager.getLogger(VideoRedundancyImpl.class.getName());

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

	@Value("${video.noData}")
	private String noData;

	@Autowired
	private IVideoUsagePrefsManager videoPref;

	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;
	@Value("${videoManifest.multipleManifestsSelected}")
	private String multipleManifestsSelected;	
	@Value("${videoManifest.noManifestsSelected}")
	private String noManifestsSelected;
	@Value("${videoManifest.invalid}")
	private String invalidManifestsFound;
	@Value("${videoManifest.noManifestsSelectedMixed}")
	private String noManifestsSelectedMixed;

	private VideoRedundancyResult result;

	private int selectedManifestCount;
	private boolean hasSelectedManifest;
	@Nonnull
	private BPResultType bpResultType;

	int countRedundant = 0;
	int countSegment = 0;
	int redundantPercentage = 0;
	int countDuplicate = 0;

	@Nonnull
	private SortedMap<Double, AROManifest> manifestCollection;
	
	@Nonnull
	VideoUsage videoUsage;

	private int invalidCount;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		result = new VideoRedundancyResult();

		init(result);

		videoUsage = tracedata.getVideoUsage();
		manifestCollection = new TreeMap<>();

		if (videoUsage != null) {
			manifestCollection = videoUsage.getAroManifestMap();
		}

		if (MapUtils.isNotEmpty(manifestCollection)) {
			bpResultType = BPResultType.CONFIG_REQUIRED;

			selectedManifestCount = videoUsage.getSelectedManifestCount();
			hasSelectedManifest = (selectedManifestCount > 0);
			invalidCount = videoUsage.getInvalidManifestCount();

			if (selectedManifestCount == 0) {
				if (invalidCount == manifestCollection.size()) {
					result.setResultText(invalidManifestsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noManifestsSelectedMixed);
				} else {
					result.setResultText(noManifestsSelected);
				}
			} else if (selectedManifestCount > 1) {
				result.setResultText(multipleManifestsSelected);
			} else if (hasSelectedManifest) {
				for (AROManifest aroManifest : manifestCollection.values()) {
					if (aroManifest.isSelected()) {
						countDuplicateChunks(aroManifest);
						break;
					}
				}

				redundantPercentage = calculateRedundantPercentage(countRedundant, countSegment);
				bpResultType = Util.checkPassFailorWarning(redundantPercentage
						, videoPref.getVideoUsagePreference().getSegmentRedundancyWarnVal()
						, videoPref.getVideoUsagePreference().getSegmentRedundancyFailVal());
				result.setResultType(bpResultType);

				if (redundantPercentage != 0.0) {
					result.setResultText(MessageFormat.format(textResults, (String.format("%d", redundantPercentage))));
				} else {
					result.setResultText(MessageFormat.format(textResultPass, redundantPercentage));
				}
				result.setRedundantPercentage(redundantPercentage);
				result.setSegmentCount(countSegment);
				result.setRedundantCount(countRedundant);
				result.setDuplicateCount(countDuplicate);
				result.setSelfTest(true);
			}
		} else {
			result.setSelfTest(false);
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
		}
		result.setResultType(bpResultType);
		return result;
	}

	public void countDuplicateChunks(AROManifest aroManifest) {
		VideoEvent preStuff = null;
		Double prevSegment = null;
		String prevQuality = "";
		for (VideoEvent videoEvent : aroManifest.getVideoEventsBySegment()) {
			double segment = videoEvent.getSegment();
			String quality = videoEvent.getQuality();
			if (aroManifest instanceof ManifestDash && segment == 0) {
				continue;
			}
			countSegment++;
			if (prevSegment != null && prevSegment == segment) {
				countSegment--;
				if (prevQuality.equals(quality)) {
					LOG.debug("Duplicate :\t" + preStuff + "\n\t\t" + videoEvent);
					countDuplicate++;
				} else {
					LOG.debug("Redundant :\t" + preStuff + "\n\t\t" + videoEvent);
					countRedundant++;
				}
			}
			preStuff = videoEvent;
			prevSegment = segment;
			prevQuality = quality;
		}
	}

	private void init(VideoRedundancyResult result) {
		bpResultType = BPResultType.SELF_TEST;
		selectedManifestCount = 0;
		hasSelectedManifest = false;
		
		countRedundant = 0;
		countSegment = 0;
		redundantPercentage = 0;
		countDuplicate = 0;

		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setOverviewTitle(overviewTitle);
		result.setLearnMoreUrl(learnMoreUrl);
	}

	/**
	 * 
	 * @param countRedundant
	 *            - number of Redundant Segments
	 * @param countSegment
	 *            - number of non duplicate Segments
	 * @return percentage of Redundant Segments over the non duplicate Segments
	 */
	private int calculateRedundantPercentage(int countRedundant, int countSegment) {
		double redundantPercantage = 0.0d;
		if (countRedundant != 0 && countSegment != 0) {
			redundantPercantage = (double) countRedundant * 100 / (double) countSegment;
		}
		return (int) Math.round(redundantPercantage);
	}
}
