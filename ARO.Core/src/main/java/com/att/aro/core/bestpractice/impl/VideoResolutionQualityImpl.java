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
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoResolutionQualityResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.pojo.DeviceDetail;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

/**
 * 
 * VBP #9 Redundant Versions of Same Video Content
 * 
 * Criteria: ARO will discover if and segments have dimensions taller than 720 pixels
 * 
 * Metric: Pass- When device is determined to be a tablet, ie: greater than or equal to 4 inches high in landscape OR When cannot determine screen height (no
 * DPI measurement) OR When device is determined to be a phone and not a tablet, ie: less than 4 inches high in landscape And no segment is greater than 740
 * pixels high (landscape)
 * 
 * Warning- When device is determined to be a phone and not a tablet, ie: less than 4 inches high in landscape And any one segment is greater than 720 pixels
 * high (landscape)
 * 
 * NoData- When no segments or no information found on video height
 * 
 * About:
 * 
 * Result: There were no segments beyond 720p in this video. There were 2 segments beyond 720p in this video. There was 1 segment beyond 720p in this video.
 * 
 * videoResolutionQuality.results=There {0} {1} segments beyond 720p in this video.
 *
 */
public class VideoResolutionQualityImpl implements IBestPractice {

	private static final double TABLET_HEIGHT_LANDSCAPE_MINIMUM = 4; // inch

	private static final double MAX_HEIGHT = 720;

	@Value("${videoResolutionQuality.title}")
	private String overviewTitle;

	@Value("${videoResolutionQuality.detailedTitle}")
	private String detailTitle;

	@Value("${videoResolutionQuality.desc}")
	private String aboutText;

	@Value("${videoResolutionQuality.url}")
	private String learnMoreUrl;

	@Value("${videoResolutionQuality.pass}")
	private String textResultPass;

	@Value("${videoResolutionQuality.results.none}")
	private String textResultsNone;

	@Value("${videoResolutionQuality.results.single}")
	private String textResultsSingle;

	@Value("${videoResolutionQuality.results.multiple}")
	private String textResultsMultiple;

	@Value("${videoResolutionQuality.results.noResolution}")
	private String textResultsResolutionData;

	@Value("${videoResolutionQuality.results.noData}")
	private String textResultsNoData;

	@Value("${videoResolutionQuality.noDPI}")
	private String noDPI;

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

	private BPResultType resultType;
	String conditionalMessage;

	@Nonnull
	private SortedMap<Double, AROManifest> manifestCollection = new TreeMap<>();
	
	@Nonnull
	VideoUsage videoUsage;

	private double maxHeightUsed;
	private int overSizeCount;
	
	private int selectedManifestCount;
	private int validSegmentCount;

	private boolean noDPIflag;
	
	private VideoResolutionQualityResult result;

	private boolean hasSelectedManifest;

	private BPResultType bpResultType;

	private int invalidCount;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		resultType = null;
		conditionalMessage = "";
		
		result = new VideoResolutionQualityResult();
		init(result);

		resultType = presetResultsByDevice(tracedata.getTraceresult());

		videoUsage = tracedata.getVideoUsage();

		if (videoUsage != null) {
			manifestCollection = videoUsage.getAroManifestMap();
		}

		if (MapUtils.isNotEmpty(manifestCollection)) {

			bpResultType = BPResultType.CONFIG_REQUIRED;
			selectedManifestCount = videoUsage.getSelectedManifestCount();
			validSegmentCount = videoUsage.getValidSegmentCount();
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
				scanManifestsForHeight(manifestCollection);

				bpResultType = BPResultType.SELF_TEST;

				if (overSizeCount == 0) {
					if (maxHeightUsed == 0) {
						if (validSegmentCount == 0 || selectedManifestCount == 0) {
							result.setResultText(textResultsNoData);
							bpResultType = BPResultType.CONFIG_REQUIRED;
						} else {
							result.setResultText(textResultsResolutionData);
							bpResultType = BPResultType.SELF_TEST;
						}
					} else {
						result.setResultText(textResultsNone);
						bpResultType = BPResultType.PASS;
					}
				} else {
					// at least one oversize
					if (noDPIflag) {
						resultType = decideResultType(BPResultType.SELF_TEST);
					}
					if (overSizeCount == 1) {
						result.setResultText(textResultsSingle + conditionalMessage);
						bpResultType = decideResultType(BPResultType.SELF_TEST);
					} else if (overSizeCount > 1) {
						result.setResultText(MessageFormat.format(textResultsMultiple + conditionalMessage, overSizeCount));
						bpResultType = decideResultType(BPResultType.FAIL);
					}
				}
				result.setOverSizeCount(overSizeCount);
				result.setMaxHeight(maxHeightUsed);
			}
		} else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
		}
		result.setResultType(bpResultType);
		return result;
	}

	private BPResultType decideResultType(BPResultType provisional) {
		if (BPResultType.NONE.equals(resultType)) {
			return provisional;
		} else {
			return resultType;
		}
	}
	
	private BPResultType presetResultsByDevice(AbstractTraceResult traceResults) {
		noDPIflag = false;
		
		BPResultType resultType = BPResultType.NONE;
		if (traceResults != null && traceResults instanceof TraceDirectoryResult) {

			DeviceDetail deviceDetail = ((TraceDirectoryResult) traceResults).getDeviceDetail();
			String deviceModel = deviceDetail.getDeviceModel();
			String[] screenSize = deviceDetail.getScreenSize().split("\\*");
			if (screenSize != null && screenSize.length > 0) {
				double screenHeight = Double.valueOf(screenSize[0]);
				if (deviceDetail.getOsType().equals("android") && deviceDetail.getScreenDensity() == 0) {
					noDPIflag = true;
					conditionalMessage = " " + noDPI;
					resultType = BPResultType.SELF_TEST;
				}
				if (deviceModel.startsWith("iPad") || isTablet(screenHeight, deviceDetail.getScreenDensity())) {
					resultType = BPResultType.PASS;
				}
			}
		}
		return resultType;
	}

	/**
	 * Sets values on maxHeightUsed and overSizeCount
	 * @param manifestCollection
	 */
	private void scanManifestsForHeight(SortedMap<Double, AROManifest> manifestCollection) {
		
		maxHeightUsed = 0;
		overSizeCount = 0;
		
		for (AROManifest aroManifest : manifestCollection.values()) {
			if (aroManifest.isSelected()) {
				for (VideoEvent videoEvent : aroManifest.getVideoEventList().values()) {
					double height = videoEvent.getResolutionHeight();
					if (height > maxHeightUsed) {
						maxHeightUsed = height;
					}
					if (height > MAX_HEIGHT) {
						overSizeCount++;
					}
				}
				break;
			}
		}
	}

	public void init(VideoResolutionQualityResult result) {
		selectedManifestCount = 0;
		validSegmentCount = 0;
		hasSelectedManifest = false;
		bpResultType = BPResultType.SELF_TEST;

		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
		result.setSelfTest(true);
	}

	/**
	 * Determine tablet vs phone by comparison with an arbitrary value.
	 * 
	 * @param screenHeight
	 *            when measured as in landscape
	 * @param screenDensity
	 * @return
	 */
	private boolean isTablet(double screenHeight, double screenDensity) {
		return screenDensity > 0 && TABLET_HEIGHT_LANDSCAPE_MINIMUM < (screenHeight / screenDensity);
	}

}
