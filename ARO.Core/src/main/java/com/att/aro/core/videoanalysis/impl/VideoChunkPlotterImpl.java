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
package com.att.aro.core.videoanalysis.impl;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoCompiled;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VideoChunkPlotterImpl extends PlotHelperAbstract {

	private int key = 0;
	Map<Integer, Double> seriesDataSets = new TreeMap<Integer, Double>();
	private List<BufferedImage> imgSeries;
	private double firstChunkTimestamp;

	private List<Double> chunkPlayStartTimes = new ArrayList<>();
	private IBestPractice startUpDelayBPReference;
	private IBestPractice stallBPReference;
	private IBestPractice bufferOccupancyBPReference;
	private Map<Long,Double> segmentStartTimeList = new TreeMap<>();
	
	@Autowired
	@Qualifier("startupDelay")
	public void setVideoStartupDelayImpl(IBestPractice startupdelay) {
		startUpDelayBPReference = startupdelay;
	}

	@Autowired
	@Qualifier("videoStall")
	public void setVideoStallImpl(IBestPractice videoStall) {
		stallBPReference = videoStall;
	}

	@Autowired
	@Qualifier("bufferOccupancy")
	public void setVideoBufferOccupancyImpl(IBestPractice bufferOccupancy) {
		bufferOccupancyBPReference = bufferOccupancy;
	}


	public Map<Integer, Double> populateDataSet(StreamingVideoData streamingVideoData) {
		if (streamingVideoData != null) {
			this.streamingVideoData = streamingVideoData;
			key = 0;
			imgSeries = new ArrayList<BufferedImage>();
			seriesDataSets.clear();
			filterVideoSegment(streamingVideoData);
			filterVideoSegmentUpdated(streamingVideoData);

			if (!streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList().isEmpty()){
				videoEventListBySegment(streamingVideoData);
				updateChunkPlayStartTimes();
			}

			getChunkCollectionDataSet();

		}
		return seriesDataSets;
	}

	public AbstractBestPracticeResult refreshStartUpDelayBP(AROTraceData analysis) {
		return startUpDelayBPReference.runTest(analysis.getAnalyzerResult());
	}

	public AbstractBestPracticeResult refreshVideoStallBP(AROTraceData analysis) {
		return stallBPReference.runTest(analysis.getAnalyzerResult());
	}

	public AbstractBestPracticeResult refreshVideoBufferOccupancyBP(AROTraceData analysis) {
		return bufferOccupancyBPReference.runTest(analysis.getAnalyzerResult());
	}

	private void getChunkCollectionDataSet() {
		int count = 0;
		List<VideoEvent> allSegments2 = streamingVideoData.getStreamingVideoCompiled().getAllSegments();
		if(allSegments2 == null ) {
			return;
		}
		for (VideoEvent ve : allSegments2) {

			BufferedImage img = ve.getThumbnail();

			if (count == 0) { // first chunk
				firstChunkTimestamp = ve.getDLTimeStamp();
				count++;
			}

			imgSeries.add(img);

			seriesDataSets.put(key, ve.getDLTimeStamp());
			key++;
		}
	}

	public void updateChunkPlayStartTimes() {
		this.chunkPlayStartTimes.clear();
		this.segmentStartTimeList.clear();
		boolean filterAgain = false;
		StreamingVideoCompiled streamingVideoCompiled = streamingVideoData.getStreamingVideoCompiled();
		double playtime = streamingVideoCompiled.getChunkPlayTimeList().get(streamingVideoCompiled.getChunkPlayTimeList().keySet().toArray()[0]); 
		double possibleStartTime;
		double duration = 0;
		List<VideoEvent> chunksBySegment = streamingVideoData.getStreamingVideoCompiled().getChunksBySegment();
		if (!chunksBySegment.isEmpty()) {
			duration = getChunkPlayTimeDuration(chunksBySegment.get(0)); // filteredChunks
		}
		Map<Double, VideoEvent> playStartTimeBySegment = new TreeMap<>();

		for (int index = 0; index < chunksBySegment.size(); index++) {
			possibleStartTime = getChunkPlayStartTime(chunksBySegment.get(index));
			if (possibleStartTime != -1) {
				playtime = possibleStartTime;
			} else {
				int diff = (index > 1) ? (int) (chunksBySegment.get(index).getSegmentID()
						- chunksBySegment.get(index - 1).getSegmentID()) : 1;
				playtime = diff * duration + playtime;
			}

			if (chunksBySegment.get(index).getEndTS() > playtime) { // Meaning this is not the right quality level chunk picked by the player
				// alter filteredChunks & chunksBySegment List
				boolean shuffled = alterFilteredSegmentList(chunksBySegment.get(index),playtime);
				if(shuffled){
					filterAgain = true;
				}
			}
			playStartTimeBySegment.put(playtime, chunksBySegment.get(index));
			duration = getChunkPlayTimeDuration(chunksBySegment.get(index));
		}

		for (VideoEvent ve : streamingVideoData.getStreamingVideoCompiled().getFilteredSegments()) {
			for (int index = 0; index < playStartTimeBySegment.keySet().size(); index++) {
				VideoEvent veSegment = playStartTimeBySegment.get(playStartTimeBySegment.keySet().toArray()[index]);
				if (veSegment.getSegmentID() == ve.getSegmentID()) {
					double playStartTime = (double) playStartTimeBySegment.keySet().toArray()[index];
					chunkPlayStartTimes.add(playStartTime);
					segmentStartTimeList.put((new Double(veSegment.getSegmentID())).longValue(), playStartTime);
					break;
				}
			}
		}
		
		if(filterAgain){
			List<VideoEvent> filteredSegments = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments();
			Collections.sort(filteredSegments, new VideoEventComparator(SortSelection.START_TS));
			for (VideoEvent ve : filteredSegments) { //Originally Content of filteredSegments List and chunksBySegment List should be the same, the difference is chunksBySegment is ordered by segment number while the other is ordered by dlTime
				if(!streamingVideoData.getStreamingVideoCompiled().getChunksBySegment().contains(ve)){
					for(VideoEvent segment:streamingVideoData.getStreamingVideoCompiled().getChunksBySegment()){
						if(segment.getSegmentID() == ve.getSegmentID()){
							//segment should be replaced by ve
							streamingVideoData.getStreamingVideoCompiled().getChunksBySegment().add(streamingVideoData.getStreamingVideoCompiled().getChunksBySegment().indexOf(segment), ve);
							streamingVideoData.getStreamingVideoCompiled().getChunksBySegment().remove(segment);
							break;
						}
					}	
				}
			}
		}

	}

	public boolean alterFilteredSegmentList(VideoEvent ve,double segmentPlayTime) {
		List<VideoEvent> duplicateChunks = streamingVideoData.getStreamingVideoCompiled().getDuplicateChunks();
		if (!duplicateChunks.isEmpty()) {
			//descending order sorting by download end time.
			Collections.sort(duplicateChunks, new VideoEventComparator(SortSelection.END_TS_DESCENDING));
			boolean swapedTheMinimum=false;
			int minIndex=-1;
			for (VideoEvent removedChunk : duplicateChunks) {
				if (removedChunk.getSegmentID() == ve.getSegmentID() && removedChunk.getEndTS() <= segmentPlayTime) {
					//This is the correct quality level of this segment played
					//swap
					int index = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().indexOf(ve);
					streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().remove(ve);
					streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().add(index, removedChunk);
					duplicateChunks.add(ve);
					duplicateChunks.remove(removedChunk);
					return true;
				} else if(ve.getEndTS() > removedChunk.getEndTS() && removedChunk.getSegmentID() == ve.getSegmentID()){
					//swap the closest		
					minIndex = duplicateChunks.indexOf(removedChunk);
					swapedTheMinimum=true;
				}
			}
			if(swapedTheMinimum && minIndex !=-1)
			{
				int index = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().indexOf(ve);
				streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().remove(ve);	
				streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().add(index, duplicateChunks.get(minIndex));
				duplicateChunks.add(ve);
				duplicateChunks.remove(duplicateChunks.get(minIndex));
				return true;
			}
		}
		return false;
	}

	public double getSegmentPlayStartTime(VideoEvent currentChunk) {
		Double temp = segmentStartTimeList.get((new Double(currentChunk.getSegmentID())).longValue());
		return temp != null ? temp : -1;
	}
	
	public Map<VideoEvent, Double> getChunkPlayTimeList() {
		return streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList();
	}
}
