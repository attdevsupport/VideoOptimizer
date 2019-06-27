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
package com.att.aro.core.videoanalysis;

import java.util.List;
import java.util.Map;

import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

public abstract class AbstractBufferOccupancyCalculator extends PlotHelperAbstract {
	
	protected double chunkPlayTimeDuration = -1;
	protected double chunkPlayStartTime = -1;
	protected double chunkPlayEndTime = -1;
	protected VideoEvent chunkPlaying;
	protected double chunkByteRange = 0;
	private VideoEvent previousChunk=null;

	protected double firstChunkArrivalTime;
	protected double startPoint;
	
	public abstract double drawVeDone(List<VideoEvent> veDone, double beginByte);
	public abstract double bufferDrain(double buffer);
	
	protected double updatePlayStartTime(VideoEvent chunkPlaying) {
		double possibleStartPlayTime = getChunkPlayStartTime(chunkPlaying);
		if (possibleStartPlayTime != -1) {
			chunkPlayStartTime = possibleStartPlayTime;
			chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		}
		return possibleStartPlayTime;
	}
	
	protected double updatePlayStartTimeAfterStall(VideoEvent chunkPlaying,double stallPausePoint, double stallRecovery) {
		double possibleStartPlayTimeAfterStall = chunkPlaying.getEndTS()+stallPausePoint+stallRecovery;//Math.ceil(chunkPlaying.getEndTS());
		if (possibleStartPlayTimeAfterStall != -1) {
			chunkPlayStartTime = possibleStartPlayTimeAfterStall;
			chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		}
		return possibleStartPlayTimeAfterStall;
	}
	
	protected void addToChunkPlayTimeList(VideoEvent chunkPlaying, double possibleStartPlayTimeAfterStall){
		streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList().put(chunkPlaying, possibleStartPlayTimeAfterStall);
	}
	
	protected void setNextPlayingChunk(int currentVideoSegmentIndex,List<VideoEvent> filteredChunk) {	
		int index = currentVideoSegmentIndex;
		chunkPlaying = filteredChunk.get(currentVideoSegmentIndex);
		while(previousChunk != null && previousChunk.getSegmentID() == chunkPlaying.getSegmentID() && currentVideoSegmentIndex < filteredChunk.size()-1){
			index= index+1;
			chunkPlaying = filteredChunk.get(index);
		}
		previousChunk = chunkPlaying;
		chunkPlayTimeDuration = getChunkPlayTimeDuration(chunkPlaying);
		int diff = (int) (chunkPlaying.getSegmentID() - previousChunk.getSegmentID()) > 1
				? (int) (chunkPlaying.getSegmentID() - previousChunk.getSegmentID()) : 1;
		chunkPlayStartTime = chunkPlayEndTime + (diff - 1) * chunkPlayTimeDuration;
		chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		chunkByteRange = (chunkPlaying.getTotalBytes());
	}

	protected void runInit(StreamingVideoData streamingVideoData, Map<VideoEvent, VideoStream> veManifestList,
			List<VideoEvent> chunkDownload) {
		int firstChunk = 0;

		for (VideoStream videoStream : veManifestList.values()) {

			if (firstChunk == 0) {
				previousChunk = chunkDownload.get(0);
				firstChunkArrivalTime = chunkDownload.get(0).getEndTS();
				double possiblePlayStartTime = getChunkPlayStartTime(chunkDownload.get(0));
				if (possiblePlayStartTime != -1){
					chunkPlayStartTime = possiblePlayStartTime;
				} else{
					chunkPlayStartTime = videoStream.getManifest().getDelay() + firstChunkArrivalTime;
				}
				chunkPlaying = chunkDownload.get(0);
				setStreamingVideoData(streamingVideoData);
				chunkPlayTimeDuration = getChunkPlayTimeDuration(chunkPlaying);
				chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
				firstChunk++;

				startPoint = chunkDownload.get(0).getDLTimeStamp();
				break;
			}	
    	}	
	}	
}
