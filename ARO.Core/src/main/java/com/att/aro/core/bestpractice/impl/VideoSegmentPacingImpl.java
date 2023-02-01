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
import org.apache.commons.math3.util.MathUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoChunkPacingResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;


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

	@Value("${segmentPacing.excel.results}")
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

	private int selectedCount;
	private int invalidCount;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		
		BPResultType bpResultType = BPResultType.SELF_TEST;
		VideoChunkPacingResult result = new VideoChunkPacingResult();
		Double dlFirst = Double.MAX_VALUE;
		Double dlLast = 0D;
		int count = 0;
		
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
				result.setSelfTest(false);
			} else if (selectedCount > 1) {
				result.setResultText(multipleManifestsSelected);
				bpResultType = BPResultType.CONFIG_REQUIRED;
				result.setResultExcelText(bpResultType.getDescription());
				result.setSelfTest(false);
			} else {
				for (VideoStream videoStream : videoStreamCollection.values()) {
					if (videoStream != null && videoStream.isSelected() && !videoStream.getVideoEventsBySegment().isEmpty()) {
						for (VideoEvent videoEvent : videoStream.getVideoEventsBySegment()) {
							if (videoEvent.isNormalSegment()) {
								count++;
								double dlTime = videoEvent.getDLLastTimestamp();
								if (dlTime < dlFirst) {  // look for earliest download of valid segment in a stream
									dlFirst = dlTime;
								}
								if (dlTime > dlLast) {  // look for last download of valid segment in a stream
									dlLast = dlTime;
								}
							}
						}
						break;
					}
				}
				double segmentPacing = 0;
				if (count > 1) {
					segmentPacing = (dlLast - dlFirst) / (count - 1);
				}
				bpResultType = BPResultType.SELF_TEST;
				result.setResultText(MessageFormat.format(
						textResults, count == 1 ? "was" : "were"
						,count
						,count == 1 ? "" : "different"
						,count == 1 ? "" : "s"
						,count == 1 ? "was" : "were"
						,segmentPacing
						,MathUtils.equals(segmentPacing, 1.0) ? "" : "s"));
				
				result.setResultExcelText(
			        MessageFormat.format(textExcelResults,
			                bpResultType.getDescription(),
			                count,
			                count <= 1 ? "" : "different",
	                        count <= 1 ? "" : "s",
			                count <= 1 ? "was" : "were",
	                        segmentPacing,
	                        segmentPacing <= 1.0 ? "" : "s")
		        );
				result.setChunkPacing(segmentPacing);
				result.setSelfTest(true);
			}
		  } else {
			result.setResultText(noData);
			bpResultType = BPResultType.NO_DATA;
			result.setResultExcelText(bpResultType.getDescription());
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
