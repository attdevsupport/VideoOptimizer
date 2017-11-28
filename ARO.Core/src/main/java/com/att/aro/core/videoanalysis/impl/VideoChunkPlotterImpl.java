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
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoChunkPlotterImpl extends PlotHelperAbstract {

	private int key = 0;
	// private List<VideoEvent> filteredChunks;

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


	public Map<Integer, Double> populateDataSet(VideoUsage videoUsage) {
		if (videoUsage != null) {
			this.videoUsage = videoUsage;
			key = 0;
			imgSeries = new ArrayList<BufferedImage>();
			seriesDataSets.clear();
			filterVideoSegment(videoUsage);
			filterVideoSegmentUpdated(videoUsage);

			if (!videoUsage.getChunkPlayTimeList().isEmpty()){
				videoEventListBySegment(videoUsage);
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
		List<VideoEvent> allSegments2 = videoUsage.getAllSegments();
		if(allSegments2 == null ) {
			return;
		}
		for (VideoEvent ve : allSegments2){//getFilteredSegments()) {//filteredChunks

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

	public List<BufferedImage> getImageSeries() {
		return imgSeries;
	}

	public double getFirstChunkTimestamp() {
		return firstChunkTimestamp;
	}

	public void updateChunkPlayStartTimes() {
		this.chunkPlayStartTimes.clear();
		this.segmentStartTimeList.clear();
		boolean filterAgain = false;
		double playtime = videoUsage.getChunkPlayTimeList().get(videoUsage.getChunkPlayTimeList().keySet().toArray()[0]); 
		double possibleStartTime;
		double duration = 0;
		if (!videoUsage.getChunksBySegmentNumber().isEmpty()) {
			duration = getChunkPlayTimeDuration(videoUsage.getChunksBySegmentNumber().get(0)); // filteredChunks
		}
		Map<Double, VideoEvent> playStartTimeBySegment = new TreeMap<>();

		for (int index = 0; index < videoUsage.getChunksBySegmentNumber().size(); index++) {
			possibleStartTime = getChunkPlayStartTime(videoUsage.getChunksBySegmentNumber().get(index));
			if (possibleStartTime != -1) {
				playtime = possibleStartTime;
			} else {
				int diff = (index > 1) ? (int) (videoUsage.getChunksBySegmentNumber().get(index).getSegment()
						- videoUsage.getChunksBySegmentNumber().get(index - 1).getSegment()) : 1;
				playtime = diff * duration + playtime;
			}

			if (videoUsage.getChunksBySegmentNumber().get(index).getEndTS() > playtime) { // Meaning this is not the right quality level chunk picked by the player
				// alter filteredChunks & chunksBySegment List
				boolean shuffled = alterFilteredSegmentList(videoUsage.getChunksBySegmentNumber().get(index),playtime);
				if(shuffled){
					filterAgain = true;
				}
			}
			playStartTimeBySegment.put(playtime, videoUsage.getChunksBySegmentNumber().get(index));
			duration = getChunkPlayTimeDuration(videoUsage.getChunksBySegmentNumber().get(index));
		}

		for (VideoEvent ve : videoUsage.getFilteredSegments()) {
			for (int index = 0; index < playStartTimeBySegment.keySet().size(); index++) {
				VideoEvent veSegment = playStartTimeBySegment.get(playStartTimeBySegment.keySet().toArray()[index]);
				if (veSegment.getSegment() == ve.getSegment()) {
					double playStartTime = (double) playStartTimeBySegment.keySet().toArray()[index];
					chunkPlayStartTimes.add(playStartTime);
					segmentStartTimeList.put((new Double(veSegment.getSegment())).longValue(), playStartTime);
					break;
				}
			}
		}
		
		if(filterAgain){
			Collections.sort(videoUsage.getFilteredSegments(), new VideoEventComparator(SortSelection.START_TS));
			for (VideoEvent ve : videoUsage.getFilteredSegments()) { //Originally Content of filteredSegments List and chunksBySegment List should be the same, the difference is chunksBySegment is ordered by segment number while the other is ordered by dlTime
				if(!videoUsage.getChunksBySegmentNumber().contains(ve)){
					for(VideoEvent segment:videoUsage.getChunksBySegmentNumber()){
						if(segment.getSegment() == ve.getSegment()){
							//segment should be replaced by ve
							videoUsage.getChunksBySegmentNumber().add(videoUsage.getChunksBySegmentNumber().indexOf(segment), ve);
							videoUsage.getChunksBySegmentNumber().remove(segment);
							break;
						}
					}	
				}
			}
		}

	}

	public boolean alterFilteredSegmentList(VideoEvent ve,double segmentPlayTime) {
		List<VideoEvent> removeChunks = videoUsage.getRemoveChunks();
		if (!removeChunks.isEmpty()) {
			//descending order sorting by download end time.
			Collections.sort(removeChunks, new VideoEventComparator(SortSelection.END_TS_DESCENDING));
			boolean swapedTheMinimum=false;
			int minIndex=-1;
			for (VideoEvent removedChunk : removeChunks) {
				if (removedChunk.getSegment() == ve.getSegment() && removedChunk.getEndTS() <= segmentPlayTime) {
					//This is the correct quality level of this segment played
					//swap
					int index = videoUsage.getFilteredSegments().indexOf(ve);
					videoUsage.getFilteredSegments().remove(ve);
					videoUsage.getFilteredSegments().add(index, removedChunk);
					removeChunks.add(ve);
					removeChunks.remove(removedChunk);
					return true;
				}
				else if(ve.getEndTS() > removedChunk.getEndTS() && removedChunk.getSegment() == ve.getSegment()){
					//swap the closest		
					minIndex = removeChunks.indexOf(removedChunk);
					swapedTheMinimum=true;
				}
			}
			if(swapedTheMinimum && minIndex !=-1)
			{
				int index = videoUsage.getFilteredSegments().indexOf(ve);
				videoUsage.getFilteredSegments().remove(ve);	
				videoUsage.getFilteredSegments().add(index, removeChunks.get(minIndex));
				removeChunks.add(ve);
				removeChunks.remove(removeChunks.get(minIndex));
				return true;
			}
		}
		return false;
	}

	public List<Double> getChunkPlayStartTimeList() {
		return chunkPlayStartTimes;
	}

	public double getSegmentPlayStartTime(VideoEvent currentChunk) {

		Double temp = segmentStartTimeList.get((new Double(currentChunk.getSegment())).longValue());
		return temp != null ? temp : -1;

	}

	public Map<Long, Double> getSegmentStartTimeList() {
		return segmentStartTimeList;
	}
	
	public Map<VideoEvent, Double> getChunkPlayTimeList() {
		return videoUsage.getChunkPlayTimeList();
	}

	
	

	
	
	
}
