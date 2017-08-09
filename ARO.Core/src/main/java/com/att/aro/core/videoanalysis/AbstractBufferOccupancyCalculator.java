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
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public abstract class AbstractBufferOccupancyCalculator extends PlotHelperAbstract {
	// Map<Integer, String> populateBufferOccupancyDataSet(VideoUsage videoUsage, Map<VideoEvent,Double> chunkPlayTimeList);
	
	protected double chunkPlayTimeDuration = -1;
	protected double chunkPlayStartTime = -1;
	protected double chunkPlayEndTime = -1;
	protected VideoEvent chunkPlaying;
	protected double chunkByteRange = 0;
	
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
		PlotHelperAbstract.chunkPlayTimeList.put(chunkPlaying, possibleStartPlayTimeAfterStall);
	}
	
	protected void setNextPlayingChunk(int currentVideoSegmentIndex,List<VideoEvent> filteredChunk) {
		chunkPlaying = filteredChunk.get(currentVideoSegmentIndex);
		// chunkPlayStartTime = chunkPlayEndTime;
		chunkPlayTimeDuration = getChunkPlayTimeDuration(chunkPlaying);
		VideoEvent previousChunk = filteredChunk.get(currentVideoSegmentIndex - 1);
		int diff = (int) (chunkPlaying.getSegment() - previousChunk.getSegment()) > 1
				? (int) (chunkPlaying.getSegment() - previousChunk.getSegment()) : 1;
		chunkPlayStartTime = chunkPlayEndTime + (diff - 1) * chunkPlayTimeDuration;
		chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		chunkByteRange = (chunkPlaying.getTotalBytes());
	}

	protected void runInit(Map<VideoEvent, AROManifest> veManifestList, List<VideoEvent> chunkDownload){
		int firstChunk = 0;

		for (AROManifest aroManifest : veManifestList.values()) {

			if (firstChunk == 0) {
				firstChunkArrivalTime = chunkDownload.get(0).getEndTS(); // chunkDownload.get(0).getEndTS();
				double possiblePlayStartTime = getChunkPlayStartTime(chunkDownload.get(0)); //chunkDownload.get(0));
				if (possiblePlayStartTime != -1){
					chunkPlayStartTime = possiblePlayStartTime;// + firstChunkArrivalTime;
				}
				else{
					chunkPlayStartTime = aroManifest.getDelay() + firstChunkArrivalTime;
				}
				chunkPlaying = chunkDownload.get(0); // chunkDownload.get(0);getChunksBySegmentNumber()
				chunkPlayTimeDuration = getChunkPlayTimeDuration(chunkPlaying);//, videoUsage);
				chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
				firstChunk++;

				startPoint = chunkDownload.get(0).getDLTimeStamp(); // chunkDownload.get(0).getDLTimeStamp();
				break;
			}	
    	}	
	}
	
	
	
}
