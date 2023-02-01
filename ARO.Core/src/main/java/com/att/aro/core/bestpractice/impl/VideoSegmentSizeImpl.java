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

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoChunkSizeResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

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
public class VideoSegmentSizeImpl implements IBestPractice{
	@Value("${segmentSize.title}")
	private String overviewTitle;

	@Value("${segmentSize.detailedTitle}")
	private String detailTitle;

	@Value("${segmentSize.desc}")
	private String aboutText;

	@Value("${segmentSize.url}")
	private String learnMoreUrl;

	@Value("${segmentSize.pass}")
	private String textResultPass;

	@Value("${segmentSize.results}")
	private String textResults;

	@Value("${segmentSize.excel.results}")
    private String textExcelResults;

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
	
	private SortedMap<Double, VideoStream> videoStreamCollection = new TreeMap<>();
	
	private StreamingVideoData streamingVideoData;

	private double averageSize;
	private double count;
	private int selectedCount;
	private int invalidCount;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		
		BPResultType bpResultType = BPResultType.SELF_TEST;
		VideoChunkSizeResult result = new VideoChunkSizeResult();
		double totalSize = 0;

		init(result);

		if ((streamingVideoData = tracedata.getStreamingVideoData()) != null 
				&& (videoStreamCollection = streamingVideoData.getVideoStreamMap()) != null 
				&& MapUtils.isNotEmpty(videoStreamCollection)) {
			selectedCount = streamingVideoData.getSelectedManifestCount();
			invalidCount = streamingVideoData.getInvalidManifestCount();
			
			if (selectedCount == 0) {
				if (invalidCount == videoStreamCollection.size()) {
					result.setResultText(invalidManifestsFound);
				} else if (invalidCount > 0) {
					result.setResultText(noManifestsSelectedMixed);
				} else {
					result.setResultText(noManifestsSelected);
				}
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultExcelText(bpResultType.getDescription());
			} else if (selectedCount > 1) {
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultText(multipleManifestsSelected);
				result.setResultExcelText(bpResultType.getDescription());
				result.setSelfTest(false);
			} else {
				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream != null && videoStream.isSelected() && !videoStream.getVideoEventsBySegment().isEmpty()) {
						for (VideoEvent videoEvent : videoStream.getVideoEventsBySegment()) {
							if (!videoEvent.isNormalSegment()) {
								continue;
							}
							count++;
							totalSize += videoEvent.getSize();
						}
						break;
					}
				}
				if (count != 0) {
					averageSize = totalSize / count;
				}
				bpResultType = BPResultType.SELF_TEST;
				result.setSelfTest(true);
				result.setResultText(MessageFormat.format(textResults
						, count == 1 ? "was" : "were", count
						, count == 1 ? "" : "different"
						, count == 1 ? "" : "s", (int) averageSize / 1024));				
				result.setResultExcelText(
			        MessageFormat.format(textExcelResults, bpResultType.getDescription(), count, (int) averageSize / 1024)
				);

				result.setSegmentSize((int) averageSize / 1024); // Size in KB
				result.setSegmentCount((int) count);
			}
		} else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
			result.setResultExcelText(bpResultType.getDescription());
			result.setSelfTest(false);
		}
		
		result.setResultType(bpResultType);
		return result;
	}

	public void init(VideoChunkSizeResult result) {
		count = 0;
		averageSize = 0;
		selectedCount = 0;
		invalidCount = 0;
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
	}

}// end class
