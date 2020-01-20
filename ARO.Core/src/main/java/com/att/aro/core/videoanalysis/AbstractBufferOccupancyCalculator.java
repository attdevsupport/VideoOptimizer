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

import org.apache.commons.collections.CollectionUtils;

import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

public abstract class AbstractBufferOccupancyCalculator extends PlotHelperAbstract {

	protected double chunkPlayTimeDuration = -1;
	protected double chunkPlayStartTime = -1;
	protected double chunkPlayEndTime = -1;
	protected VideoEvent chunkPlaying;
	protected double chunkByteRange = 0;
	private VideoEvent firstChunk = null;

	protected double firstChunkArrivalTime;
	protected double startPoint;

	public abstract double drawVeDone(List<VideoEvent> veDone, double beginByte);

	public abstract double bufferDrain(double buffer);

	protected double updatePlayStartTime(VideoEvent chunkPlaying) {
		double possibleStartPlayTime = getChunkPlayStartTime(chunkPlaying);
		 
		chunkPlayEndTime = chunkPlaying.getPlayTimeEnd();
		if (possibleStartPlayTime != -1) {
			chunkPlayStartTime = possibleStartPlayTime;
			chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		}
		return possibleStartPlayTime;
	}

	protected double updatePlayStartTimeAfterStall(VideoEvent chunkPlaying, double stallPausePoint, double stallRecovery) {
		double possibleStartPlayTimeAfterStall = chunkPlaying.getPlayTime();
		if (possibleStartPlayTimeAfterStall != -1) {
			chunkPlayStartTime = possibleStartPlayTimeAfterStall;
			chunkPlayEndTime = chunkPlaying.getPlayTimeEnd();
		}
		return possibleStartPlayTimeAfterStall;
	}

	protected void addToChunkPlayTimeList(VideoEvent chunkPlaying, double possibleStartPlayTimeAfterStall) {
		streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList().put(chunkPlaying, possibleStartPlayTimeAfterStall);
	}

	protected void setNextPlayingChunk(int currentVideoSegmentIndex, List<VideoEvent> filteredChunk) {
		int index = currentVideoSegmentIndex;
		chunkPlaying = filteredChunk.get(currentVideoSegmentIndex);
		while (firstChunk != null && firstChunk.getSegmentID() == chunkPlaying.getSegmentID() && currentVideoSegmentIndex < filteredChunk.size() - 1) {
			index = index + 1;
			chunkPlaying = filteredChunk.get(index);
		}
		firstChunk = chunkPlaying;
		chunkPlayTimeDuration = chunkPlaying.getDuration();
		int diff = (int) (chunkPlaying.getSegmentID() - firstChunk.getSegmentID()) > 1 
				 ? (int) (chunkPlaying.getSegmentID() - firstChunk.getSegmentID())
				 : 1;
		chunkPlayStartTime = chunkPlayEndTime + (diff - 1) * chunkPlayTimeDuration;
		chunkPlayEndTime = chunkPlayStartTime + chunkPlayTimeDuration;
		chunkByteRange = (chunkPlaying.getTotalBytes());
	}

	/**
	 * 
	 * @param streamingVideoData seems to be 
	 * 	streamingVideoData.getStreamingVideoCompiled().getChunksBySegmentID()
	 * or
	 *  Collections.sort(filteredSegments, new VideoEventComparator(SortSelection.SEGMENT_ID));
	 * @param segmentCollection
	 */
	protected void runInit(StreamingVideoData streamingVideoData, List<VideoEvent> segmentCollection) {

		if (!CollectionUtils.isEmpty(segmentCollection)) {
			for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()){
				if (videoStream.isSelected()) {

					chunkPlaying = firstChunk = segmentCollection.get(0);
				
					chunkPlayStartTime = firstChunk.getPlayTime();
					chunkPlayEndTime = firstChunk.getPlayTimeEnd();

					startPoint = firstChunk.getDLTimeStamp();
					
					setStreamingVideoData(streamingVideoData);
					
					break;
				}
			}
		}
	}
	
	/**
	 * Return play start time
	 * 
	 * @param chunkPlaying
	 * @return
	 */
	public double getChunkPlayStartTime(VideoEvent chunkPlaying) {
		for (VideoEvent veEvent : chunkPlayTimeList.keySet()) {
			if (veEvent.equals(chunkPlaying)) {
				GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents("dev_getChunkPlayStartTime", veEvent.getManifest().getVideoName());
				return chunkPlayTimeList.get(veEvent);
			}
		}
		return -1;
	}
	
}









