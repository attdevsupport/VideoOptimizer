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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.util.MathUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.att.aro.core.packetanalysis.pojo.BufferOccupancyBPResult;
import com.att.aro.core.videoanalysis.AbstractBufferOccupancyCalculator;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;


public class BufferOccupancyCalculatorImpl extends AbstractBufferOccupancyCalculator { 
	
	protected static final Logger LOG = LogManager.getLogger(BufferOccupancyCalculatorImpl.class.getName());
	
	double beginByte, endByte;
	double startupDelay;

	boolean setNextChunk;
	
	private List<VideoEvent> chunkDownload;
	private List<VideoEvent> chunkPlay;
	private List<VideoEvent> veDone;
	private List<VideoEvent> filteredSegments;
	private List<VideoEvent> completedDownloads = new ArrayList<>();
	List<VideoEvent> veCollection = new ArrayList<>();

	Map<Integer, String> seriesDataSets = new TreeMap<Integer, String>();
	int key = 1;

	private BufferOccupancyBPResult bufferOccupancyResult;

	private VideoChunkPlotterImpl videoChunkPlotterRef;
	  
	@Autowired
	@Qualifier("videoChunkPlotterImpl")
	public void setVideoChunkPlotterRef(PlotHelperAbstract videoChunkPlotter) {
		videoChunkPlotterRef = (VideoChunkPlotterImpl)videoChunkPlotter;
	}

	private void initialize(StreamingVideoData streamingVideoData){
		filteredSegments = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments();
	
		for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()) {
			if (videoStream.isSelected()) {
				for (VideoEvent ve : videoStream.getVideoEventList().values()) {
					if (ve.getSegmentID() != 0) {
						chunkDownload.add(ve);
					}
				}
			}
		}
    	    	
    	chunkPlay.clear();
    	for(VideoEvent vEvent:chunkDownload){
    		chunkPlay.add(vEvent);
    	}
    	
    	Collections.sort(chunkPlay, new VideoEventComparator(SortSelection.SEGMENT_ID));
        Collections.sort(filteredSegments, new VideoEventComparator(SortSelection.SEGMENT_ID));
    	runInit(streamingVideoData, filteredSegments);
    }

    @Override
	public double drawVeDone(List<VideoEvent> veDone, double beginByte) {
		double buffer = beginByte;

		if (veDone.size() == 0) {
			// drain buffer
			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;

			buffer = bufferDrain(buffer);
		} else {
			Collections.sort(veDone, new VideoEventComparator(SortSelection.END_TS)); 
		
			boolean drained = false;

			for (VideoEvent chunk : veDone) {
				seriesDataSets.put(key, chunk.getEndTS() + "," + buffer);
				key++;

				if (MathUtils.equals(chunk.getEndTS(), chunkPlayStartTime)) {
					// drain
					buffer = buffer + (chunk.getTotalBytes());

					completedDownloads.add(chunk);
					chunkDownload.remove(chunk);

					buffer = bufferDrain(buffer);
					drained = true;
					/*if (buffer < 0) {
						// stall
						return -1;
					}*/

				} else {

					buffer = buffer + (chunk.getTotalBytes());
					seriesDataSets.put(key, chunk.getEndTS() + "," + buffer);
					key++; 
				    
					completedDownloads.add(chunk);
					chunkDownload.remove(chunk);
				}

			}
			if (drained == false) {
				buffer = bufferDrain(buffer);
			}

		}

		return buffer;
	}
    
	@Override
	public double bufferDrain(double buffer) { //here start to end
		if (buffer >= 0){// && completedDownloads.contains(chunkPlaying)) {
			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;
			buffer = buffer - (chunkPlaying.getTotalBytes());
			for(int index=0;index<chunkPlay.size();index++){		
				if(chunkPlay.get(index).getSegmentID() > chunkPlaying.getSegmentID()){
					break;
				} else if(chunkPlay.get(index).getSegmentID() == chunkPlaying.getSegmentID() && completedDownloads.contains(chunkPlay.get(index)) && (!chunkPlay.get(index).equals(chunkPlaying))){
					buffer = buffer - chunkPlay.get(index).getTotalBytes();
					chunkPlay.remove(chunkPlay.get(index));
				} else if(chunkPlay.get(index).getSegmentID() == chunkPlaying.getSegmentID()&& (!completedDownloads.contains(chunkPlay.get(index))) && (!chunkPlay.get(index).equals(chunkPlaying))){
					chunkPlay.remove(chunkPlay.get(index));
				}
			}
			if (buffer < 0) {
				buffer = 0;
			}
			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;
			
		} else {
			buffer=0;
		}
		return buffer;
	}

	public Map<Integer, String> populateBufferOccupancyDataSet(StreamingVideoData streamingVideoData, Map<VideoEvent, Double> chunkPlayTimeList) {
		key = 0;
		seriesDataSets.clear();
		if (streamingVideoData != null) {
			chunkDownload = new ArrayList<VideoEvent>();
			chunkPlay = new ArrayList<VideoEvent>();
			veDone = new ArrayList<>();
			completedDownloads.clear();
			filteredSegments = new ArrayList<VideoEvent>();
			beginByte=0; 
			endByte =0;
			initialize(streamingVideoData);

			double bufferFill = 0;
			for (int index = 0; index < filteredSegments.size(); index++) { // chunkPlay.size()
				bufferFill = 0;

				updateUnfinishedDoneVideoEvent();

				bufferFill = drawVeDone(veDone, beginByte);
				veDone.clear();

				endByte = bufferFill;
				
				beginByte = endByte;

				if (index + 1 <= filteredSegments.size() - 1) { //chunkPlay.size()
					setNextPlayingChunk(index + 1,filteredSegments); //, videoUsage);
				}

			}
		}

		return seriesDataSets;

	}

	@Override
	protected void setNextPlayingChunk(int currentVideoSegmentIndex, List<VideoEvent> filteredChunk) {

		if (videoChunkPlotterRef.getStreamingVideoData() == null) {
			LOG.error("videoChunkPlotterRef.getStreamingVideoData() returns NULL");
			return;
		}
		chunkPlaying = filteredChunk.get(currentVideoSegmentIndex);
		int chunkIndex = -1;
		boolean nonStalled = false;
		Map<VideoEvent, Double> chunkPlayTimeList = videoChunkPlotterRef.getStreamingVideoData().getStreamingVideoCompiled().getChunkPlayTimeList();
		if (chunkPlayTimeList != null && !chunkPlayTimeList.isEmpty()) {
			if (chunkPlayTimeList.keySet().contains(chunkPlaying)) {
				chunkPlayStartTime = chunkPlayTimeList.get(chunkPlaying);
			} else {
				nonStalled = true;
			}
		} else {
			nonStalled = true;
		}
		if (nonStalled) {
			if (chunkIndex != -1 && videoChunkPlotterRef.getChunkPlayStartTimesList() != null && !videoChunkPlotterRef.getChunkPlayStartTimesList().isEmpty()) {
				if (chunkIndex < videoChunkPlotterRef.getChunkPlayStartTimesList().size()) {
					chunkPlayStartTime = videoChunkPlotterRef.getChunkPlayStartTimesList().get(chunkIndex);
				}

			} else {
				chunkPlayStartTime = chunkPlayEndTime;
			}
		}

		chunkPlayTimeDuration = chunkPlaying.getDuration();// , videoUsage);
		chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		chunkByteRange = (chunkPlaying.getTotalBytes());
	}
	
	public void updateUnfinishedDoneVideoEvent() {
		for (VideoEvent ve : chunkDownload) {
			if (ve.getDLTimeStamp() < chunkPlayStartTime && ve.getEndTS() <= chunkPlayStartTime) {
				veDone.add(ve);
			}
		}
	}

	public BufferOccupancyBPResult setMaxBuffer(Double maxbuffer) {
		bufferOccupancyResult = new BufferOccupancyBPResult(maxbuffer);
		return bufferOccupancyResult;
	}

}
