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

import org.springframework.beans.factory.annotation.Autowired;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.videoanalysis.AbstractBufferOccupancyCalculator;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class BufferInSecondsCalculatorImpl extends AbstractBufferOccupancyCalculator {

	private List<VideoEvent> filteredChunk;
	private List<VideoEvent> chunkDownload;
	private Map<VideoEvent, AROManifest> veManifestList;

	private List<VideoEvent> veDone;

	double beginBuffer, endBuffer;
	
	Map<Integer, String> seriesDataSets = new TreeMap<Integer, String>();
	int key;

	private List<VideoEvent> veWithIn;
	private List<VideoEvent> completedDownloads = new ArrayList<>();
	//private int stallCount;
	private List<VideoStall> videoStallResult;
	private boolean stallStarted;

	@Autowired
	private IVideoUsagePrefsManager videoPrefManager;

    private double stallTriggerTime;
	
	public List<VideoStall> getVideoStallResult(){
		return videoStallResult;
	}
	
	public double getStallTriggerTime(){
		return stallTriggerTime;
	}
	
	private void setStallTriggerTime(double stallTriggerTime){
		this.stallTriggerTime = stallTriggerTime;
	}
	
	public Map<Integer, String> populate(VideoUsage videoUsage,Map<VideoEvent, Double> chunkPlayTimeList) {
		if(videoPrefManager.getVideoUsagePreference() != null){
			setStallTriggerTime(videoPrefManager.getVideoUsagePreference().getStallTriggerTime());
		}		
		//this.chunkPlayTimeList = chunkPlayTimeList;
		seriesDataSets.clear();
		key=0;
		//stallCount=0;
		videoStallResult = new ArrayList<>();
		if(videoUsage != null){
			filteredChunk = new ArrayList<>();
			chunkDownload = new ArrayList<>();
			veDone = new ArrayList<>();
			veWithIn = new ArrayList<>();
			completedDownloads.clear();
			beginBuffer=0;
			endBuffer=0;
			stallStarted=false;
			
			initialize(videoUsage);
			
			double bufferInSeconds = 0;
			for (int index = 0, limit = 0; index < getChunksBySegmentNumber().size(); index++, limit++) {//filteredChunk
				bufferInSeconds=0;
				
				updateUnfinishedDoneVideoEvent();
			
				bufferInSeconds = drawVeDone(veDone, beginBuffer);
				veDone.clear();
				
				bufferInSeconds = drawVeWithIn(veWithIn,bufferInSeconds);
				veWithIn.clear();
				
				endBuffer = bufferInSeconds;

				if (bufferInSeconds < 0) { // using -ve as stall indicator
					// if indicated push the chunk play start time
					double possibleStartPlayTime = updatePlayStartTime(chunkPlaying);
					if (possibleStartPlayTime != -1 && limit < getChunksBySegmentNumber().size()) {//filteredChunk.size()
						// redo updateVeDone & updateWithIn
						index--;
						continue;
					} else{
						if(!videoStallResult.isEmpty()){
							videoStallResult.get(videoStallResult.size()-1).setSegmentTryingToPlay(chunkPlaying);
						}
						break;
					}
				}
				
				beginBuffer = endBuffer;

				if (index + 1 <= getChunksBySegmentNumber().size() - 1) {
					setNextPlayingChunk(index + 1,getChunksBySegmentNumber()); //, videoUsage);
				}

			}
		}
		
		return seriesDataSets;
		
	}
	
    @Override
	public double drawVeDone(List<VideoEvent> veDone, double beginBuffer){
		double buffer = beginBuffer;
		Collections.sort(veDone, new VideoEventComparator(SortSelection.END_TS));
	
		for (VideoEvent chunk : veDone) {			
			
			seriesDataSets.put(key, chunk.getEndTS() + "," + buffer);
			key++;
			
			buffer = buffer + getChunkPlayTimeDuration(chunk); 

			seriesDataSets.put(key, chunk.getEndTS() + "," + buffer);
			key++;
			
			completedDownloads.add(chunk);
			chunkDownload.remove(chunk);

		}

		return buffer;
	}
	
	public double drawVeWithIn(List<VideoEvent> veWithIn, double beginBuffer) {
		double buffer = beginBuffer;

		if (veWithIn.size() == 0) {
			buffer = bufferDrain(buffer);
		} else if (completedDownloads.contains(chunkPlaying)) {
			Collections.sort(veWithIn, new VideoEventComparator(SortSelection.END_TS));

			boolean drained = false;
			VideoEvent chunk;
			double timeRange;
			double durationLeft = chunkPlayTimeDuration; 
			
			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;
			
			if(stallStarted){
				updateStallInformation(chunkPlayStartTime);
				stallStarted = false;
			}		
			
			for (int index=0;index<veWithIn.size();index++) {
				chunk = veWithIn.get(index);
				if(chunk.getEndTS() == chunkPlayEndTime){
					//finish draining
					buffer = buffer - durationLeft;
					if (buffer <= 0){
						buffer = 0;
						updateStallInformation(chunk.getEndTS());
					}
					seriesDataSets.put(key, chunkPlayEndTime + "," + buffer);
					key++;
					drained = true;
				
					buffer = buffer + getChunkPlayTimeDuration(chunk);

					seriesDataSets.put(key, chunk.getEndTS() + "," + buffer);
					key++;				
					
					completedDownloads.add(chunk);
					chunkDownload.remove(chunk);
				}else{
					if(index==0){
						timeRange = chunk.getEndTS()-chunkPlayStartTime;
					}else{
						timeRange = chunk.getEndTS() -veWithIn.get(index-1).getEndTS();
					}
			
						buffer = buffer - timeRange;
						if (buffer <= 0) {
							buffer = 0;
							stallStarted=true;
							updateStallInformation(chunk.getEndTS());
						}
						durationLeft = durationLeft - timeRange;

						seriesDataSets.put(key, chunk.getEndTS() + "," + buffer);
						key++;
						
						buffer = buffer + getChunkPlayTimeDuration(chunk);

						seriesDataSets.put(key, chunk.getEndTS() + "," + buffer);
						key++;

						completedDownloads.add(chunk);
						chunkDownload.remove(chunk);		
				}
			}
			
			if(drained==false){
				buffer = buffer - durationLeft;
				if (buffer <= 0){
					buffer = 0;
					stallStarted=true;
					updateStallInformation(chunkPlayEndTime);
				}
				seriesDataSets.put(key, chunkPlayEndTime + "," + buffer);
				key++;
	
			}
			
		}else{
			stallStarted=true;
			VideoStall stall = new VideoStall(chunkPlayStartTime);
			videoStallResult.add(stall);
			
			return -1;
		}
			
		return buffer;
		
	}

	private void initialize(VideoUsage videoUsage) {
		filteredChunk = getFilteredSegments(); //filterVideoSegment(videoUsage);
		chunkDownload = new ArrayList<>();
    	for(VideoEvent vEvent:filteredChunk){
    		chunkDownload.add(vEvent);
    	}
    	veManifestList = getVideoEventManifestMap();
    	
    	runInit(veManifestList,getChunksBySegmentNumber());		
	}
	
	public double getChunkPlayStartTime(VideoEvent chunkPlaying) {
		for (VideoEvent veEvent : chunkPlayTimeList.keySet()) {
			if (veEvent.equals(chunkPlaying)) {
				return chunkPlayTimeList.get(veEvent); // return play start time
			}
		}
		return -1;
	}
	
	public void updateUnfinishedDoneVideoEvent() {
		for (VideoEvent ve : chunkDownload) {
			if (ve.getDLTimeStamp() < chunkPlayStartTime && ve.getEndTS() <= chunkPlayStartTime) {
					veDone.add(ve);			
			}
			else if(ve.getEndTS() > chunkPlayStartTime && ve.getEndTS()<= chunkPlayEndTime){
				veWithIn.add(ve);
			}
		}
	}

	public void updateStallInformation(double stallTime){
		if(stallStarted){
			videoStallResult.get(videoStallResult.size()-1).setStallEndTimeStamp(stallTime);
		}else{
			stallStarted=true;
			VideoStall stall = new VideoStall(stallTime);
			videoStallResult.add(stall);
		}
	}
	
	@Override
	public double bufferDrain(double buffer) {
		if (buffer > 0 && completedDownloads.contains(chunkPlaying)) { 
			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;
	
			buffer = buffer - chunkPlayTimeDuration;
			if (buffer <= 0){ 
				buffer = 0;
				stallStarted=true;
				VideoStall stall = new VideoStall(chunkPlayEndTime);
				videoStallResult.add(stall);						
			}
			else{
				if(stallStarted){
				updateStallInformation(chunkPlayStartTime);
				stallStarted = false;
				}
			}
			
			seriesDataSets.put(key, chunkPlayEndTime + "," + buffer);
			key++;
	
		} else {
			// stall
			if (buffer <= 0) {  //want to see if buffer >0 & stall happening
				buffer = 0;
				stallStarted=true;
				updateStallInformation(chunkPlayStartTime);
			}
			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;

			return -1;
		}

		return buffer;
	}


}
