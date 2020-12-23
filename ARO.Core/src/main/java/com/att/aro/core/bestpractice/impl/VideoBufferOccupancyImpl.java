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
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.BufferOccupancyResult;
import com.att.aro.core.packetanalysis.pojo.BufferOccupancyBPResult;
import com.att.aro.core.packetanalysis.pojo.BufferTimeBPResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * <pre>
 * VBP #3 Video Buffer Occupancy
 * 
 * Criteria: ARO will measure frequency and duration of video stream buffers
 * 
 * About: Buffer occupancy is the amount of video stored in RAM to help prevent interruption due to transmission delays, known as "buffering".
 * 
 * Result: Your video had X size buffer occupancy By managing buffer occupancy you can make sure it is not too long or too short.
 * 
 * Link: goes to a view of buffers.
 * 
 */

public class VideoBufferOccupancyImpl implements IBestPractice {
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

	@Value("${bufferOccupancy.excel.results}")
    private String textExcelResults;

	@Value("${startUpDelay.init}")
	private String startUpDelayNotSet;

	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;

	@Value("${video.noData}")
	private String noData;

	@Autowired
	private IVideoUsagePrefsManager videoUsagePrefs;
	
	@Value("${videoManifest.multipleManifestsSelected}")
	private String multipleManifestsSelected;
	
	@Value("${videoManifest.noManifestsSelected}")
	private String noManifestsSelected;
	
	@Value("${videoManifest.noManifestsSelectedMixed}")
	private String noManifestsSelectedMixed;
	
	@Value("${videoManifest.invalid}")
	private String invalidManifestsFound;

	@Nonnull
	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();
	
	@NonNull
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private StreamingVideoData streamingVideoData;

	private BufferOccupancyResult result;

	private int selectedManifestCount;
	private boolean hasSelectedManifest;

	@Nonnull
	private BPResultType bpResultType = BPResultType.CONFIG_REQUIRED;;

	private int invalidCount;

	public double getVideoPrefMaxBuffer() {
		if (videoUsagePrefs.getVideoUsagePreference() != null) {
			return videoUsagePrefs.getVideoUsagePreference().getMaxBuffer();
		}

		return 0.0d;
	}

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {

		result = new BufferOccupancyResult();
		init(result);

		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null
				&& MapUtils.isNotEmpty(videoStreamCollection)) {

			bpResultType = BPResultType.CONFIG_REQUIRED;
			result.setResultExcelText(bpResultType.getDescription());

			selectedManifestCount = streamingVideoData.getSelectedManifestCount();
			hasSelectedManifest = (selectedManifestCount > 0);
			invalidCount = streamingVideoData.getInvalidManifestCount();

			if (selectedManifestCount == 0) {
				if (invalidCount == videoStreamCollection.size()) {
					result.setResultText(invalidManifestsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noManifestsSelectedMixed);
				} else {
					result.setResultText(noManifestsSelected);
				}
			} else if (selectedManifestCount > 1) {
				result.setResultText(multipleManifestsSelected);
			} else if (hasSelectedManifest) {
				BufferOccupancyBPResult bufferBPResult = tracedata.getBufferOccupancyResult();
				BufferTimeBPResult bufferTimeBPResult = tracedata.getBufferTimeResult();
				double maxBufferInMB = 0;

				if (bufferBPResult != null && bufferBPResult.getBufferByteDataSet().size() > 0) {
				    double megabyteDivisor = 1000 * 1000;
				    maxBufferInMB = bufferBPResult.getMaxBuffer() / megabyteDivisor; // getMaxBuffer() returns in bytes
					List<Double> bufferDataSet = bufferBPResult.getBufferByteDataSet();
					result.setMinBufferByte(bufferDataSet.get(0) / megabyteDivisor); // In MB
					double bufferSum = bufferDataSet.stream().reduce((a, b) -> a + b).get();
					result.setAvgBufferByte((bufferSum / bufferDataSet.size()) / megabyteDivisor);
				} else {
				    maxBufferInMB = 0;
				}

				if (bufferTimeBPResult != null && bufferTimeBPResult.getBufferTimeDataSet().size() > 0) {
					List<Double> bufferTimeDataSet = bufferTimeBPResult.getBufferTimeDataSet();
					result.setMinBufferTime(bufferTimeDataSet.get(0));
					result.setMaxBufferTime(bufferTimeDataSet.get(bufferTimeDataSet.size() - 1));
					double sum = bufferTimeDataSet.stream().reduce((a, b) -> a + b).get();
					result.setAvgBufferTime(sum / bufferTimeDataSet.size());
				}

				result.setSelfTest(true);
				result.setMaxBuffer(maxBufferInMB);

				double percentage = 0;
				double maxBufferSet = getVideoPrefMaxBuffer();
				if (maxBufferSet != 0) {
					percentage = (maxBufferInMB / maxBufferSet) * 100;
				}

				if (MapUtils.isEmpty(streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList())) {
					result.setResultText(startUpDelayNotSet);
					bpResultType = BPResultType.CONFIG_REQUIRED;
				} else {
					if (percentage > 100) {
						bpResultType = BPResultType.WARNING;
					}
					bpResultType = BPResultType.PASS;
					result.setResultText(MessageFormat.format(this.textResults, String.format("%.2f", percentage), String.format("%.2f", maxBufferInMB),
							String.format("%.2f", maxBufferSet)));
					result.setResultExcelText(
				        MessageFormat.format(textExcelResults, bpResultType.getDescription(), String.format("%.2f", percentage), String.format("%.2f", maxBufferInMB))
			        );
				}
			}
		} else {
			result.setSelfTest(false);
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
			result.setResultExcelText(bpResultType.getDescription());
		}

		result.setResultType(bpResultType);
		return result;
	}

	private void init(BufferOccupancyResult result) {
		bpResultType = BPResultType.SELF_TEST;
		selectedManifestCount = 0;
		hasSelectedManifest = false;

		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setOverviewTitle(overviewTitle);
		result.setLearnMoreUrl(learnMoreUrl);
	}

}
