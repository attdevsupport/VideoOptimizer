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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.BufferOccupancyBPResult;
import com.att.aro.core.videoanalysis.AbstractBufferOccupancyCalculator;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;


public class BufferOccupancyCalculatorImpl extends AbstractBufferOccupancyCalculator { 
	
	double beginByte, endByte;
	double startupDelay;

	boolean setNextChunk;
	
	private List<VideoEvent> chunkDownload;
	private List<VideoEvent> chunkPlay;
	private List<VideoEvent> veDone;
	private List<VideoEvent> filteredSegmentByLastArrival;
	private List<VideoEvent> completedDownloads = new ArrayList<>();
	List<VideoEvent> veCollection = new ArrayList<>();

	Map<Integer, String> seriesDataSets = new TreeMap<Integer, String>();
	int key = 1;

	private Map<VideoEvent, AROManifest> veManifestList;

	private BufferOccupancyBPResult bufferOccupancyResult;

	
	private VideoChunkPlotterImpl videoChunkPlotterRef;
	  
	@Autowired
	@Qualifier("videoChunkPlotterImpl")
	public void setVideoChunkPlotterRef(PlotHelperAbstract videoChunkPlotter) {
		videoChunkPlotterRef = (VideoChunkPlotterImpl)videoChunkPlotter;
	}

	private void initialize(VideoUsage videoUsage){
		filteredSegmentByLastArrival = videoUsage.getFilteredSegments();
	
		for (AROManifest aroManifest : videoUsage.getManifests()) {
			if (aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty()) { // don't count if no videos with manifest
				for (VideoEvent ve : aroManifest.getVideoEventList().values()) {
					if (ve.getSegment() != 0) {
						chunkDownload.add(ve);
					}
				}
			}
		}
    	    	
    	chunkPlay.clear();
    	for(VideoEvent vEvent:chunkDownload){
    		chunkPlay.add(vEvent);
    	}
    	Collections.sort(chunkPlay, new VideoEventComparator(SortSelection.SEGMENT));
        Collections.sort(filteredSegmentByLastArrival, new VideoEventComparator(SortSelection.SEGMENT));
    	veManifestList = videoUsage.getVideoEventManifestMap();
    	runInit(videoUsage, veManifestList,filteredSegmentByLastArrival);
    }
    
	public double getChunkPlayStartTime(VideoEvent chunkPlaying) {
		for (VideoEvent veEvent : chunkPlayTimeList.keySet()) {
			if (veEvent.equals(chunkPlaying)) {
				return chunkPlayTimeList.get(veEvent); // return play start time
			}
		}
		return -1;
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
				if(chunkPlay.get(index).getSegment() > chunkPlaying.getSegment()){
					break;
				} else if(chunkPlay.get(index).getSegment() == chunkPlaying.getSegment() && completedDownloads.contains(chunkPlay.get(index)) && (!chunkPlay.get(index).equals(chunkPlaying))){
					buffer = buffer - chunkPlay.get(index).getTotalBytes();
					chunkPlay.remove(chunkPlay.get(index));
				} else if(chunkPlay.get(index).getSegment() == chunkPlaying.getSegment()&& (!completedDownloads.contains(chunkPlay.get(index))) && (!chunkPlay.get(index).equals(chunkPlaying))){
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

	public Map<Integer, String> populateBufferOccupancyDataSet(VideoUsage videoUsage, Map<VideoEvent, Double> chunkPlayTimeList) {
		key = 0;
		seriesDataSets.clear();
		//this.chunkPlayTimeList = chunkPlayTimeList;
		if (videoUsage != null) {
			chunkDownload = new ArrayList<VideoEvent>();
			chunkPlay = new ArrayList<VideoEvent>();
			veDone = new ArrayList<>();
			completedDownloads.clear();
			filteredSegmentByLastArrival = new ArrayList<VideoEvent>();
			beginByte=0; 
			endByte =0;
			initialize(videoUsage);

			double bufferFill = 0;
			for (int index = 0; index < filteredSegmentByLastArrival.size(); index++) { // chunkPlay.size()
				bufferFill = 0;

				updateUnfinishedDoneVideoEvent();

				bufferFill = drawVeDone(veDone, beginByte);
				veDone.clear();

				endByte = bufferFill;
				
				beginByte = endByte;

				if (index + 1 <= filteredSegmentByLastArrival.size() - 1) { //chunkPlay.size()
					setNextPlayingChunk(index + 1,filteredSegmentByLastArrival); //, videoUsage);
				}

			}
		}

		return seriesDataSets;

	}

	@Override
	protected void setNextPlayingChunk(int currentVideoSegmentIndex, List<VideoEvent> filteredChunk) { // , VideoUsage videoUsage

		chunkPlaying = filteredChunk.get(currentVideoSegmentIndex);
		int chunkIndex = -1;
		boolean nonStalled =false;
		/*for (int index = 0; index < filteredSegmentByStartTS.size(); index++) {
			if (filteredSegmentByStartTS.get(index).getSegment() == chunkPlaying.getSegment()) {
				chunkIndex = index;
				break;
			}
		}*/
		
		Map<VideoEvent, Double> chunkPlayTimeList = videoChunkPlotterRef.getVideoUsage().getChunkPlayTimeList();
		if (chunkPlayTimeList != null && !chunkPlayTimeList.isEmpty()) {
			if (chunkPlayTimeList.keySet().contains(chunkPlaying)) {
				chunkPlayStartTime = chunkPlayTimeList.get(chunkPlaying);
			}else{
				nonStalled = true;
			}
		}else{
			nonStalled=true;
		}
        if(nonStalled){
			if (chunkIndex != -1 && videoChunkPlotterRef.getChunkPlayStartTimeList() != null && !videoChunkPlotterRef.getChunkPlayStartTimeList().isEmpty()) {
				// FIXME - this fix prevents out of bounds - this methods logic needs more clarity
				if (chunkIndex < videoChunkPlotterRef.getChunkPlayStartTimeList().size()) {
					chunkPlayStartTime = videoChunkPlotterRef.getChunkPlayStartTimeList().get(chunkIndex); // currentVideoSegmentIndex);
				}

			} else {
				chunkPlayStartTime = chunkPlayEndTime;
			}
		}
		
		chunkPlayTimeDuration = getChunkPlayTimeDuration(chunkPlaying);// , videoUsage);
		chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		chunkByteRange = (chunkPlaying.getTotalBytes());
	}
	
	public void updateUnfinishedDoneVideoEvent() {
		for (VideoEvent ve : chunkDownload) {
			if (ve.getDLTimeStamp() < chunkPlayStartTime && ve.getEndTS() <= chunkPlayStartTime) {
				//if (ve.getDLTimeStamp() < chunkPlayEndTime && ve.getEndTS() <= chunkPlayEndTime) {
					veDone.add(ve);			
			}
		}
	}

	public BufferOccupancyBPResult setMaxBuffer(Double maxbuffer) {
		bufferOccupancyResult = new BufferOccupancyBPResult(maxbuffer);
		return bufferOccupancyResult;
	}

}
