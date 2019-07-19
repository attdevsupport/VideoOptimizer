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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.att.aro.core.packetanalysis.pojo.BufferTimeBPResult;
import com.att.aro.core.packetanalysis.pojo.NearStall;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.videoanalysis.AbstractBufferOccupancyCalculator;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

public class BufferInSecondsCalculatorImpl extends AbstractBufferOccupancyCalculator {

	private List<VideoEvent> filteredChunk;
	private List<VideoEvent> chunkDownload;
	private List<VideoEvent> chunkDownloadCopy;
	private Map<VideoEvent, VideoStream> veManifestList;

	private List<VideoEvent> veDone;

	double beginBuffer, endBuffer;

	Map<Integer, String> seriesDataSets = new TreeMap<Integer, String>();
	int key;
	
	private BufferTimeBPResult bufferTimeResult;

	@Autowired
	private VideoChunkPlotterImpl videoChunkPlotterImpl;
	
	private List<VideoEvent> veWithIn;
	private List<VideoEvent> completedDownloads = new ArrayList<>();
	private List<VideoStall> videoStallResult;
	private List<NearStall> videoNearStallResult;
	private boolean stallStarted;
	double possibleStartPlayTime;
	private List<VideoEvent> completedDownloadsWithOutNearStalls = new ArrayList<>();
	private List<VideoEvent> veWithInPlayDownloadedSegments;
	
	@Autowired
	private IVideoUsagePrefsManager videoPrefManager;

	private double stallTriggerTime;
	private double stallPausePoint;
	private double stallRecovery;
	private double nearStall;
	
	public List<VideoStall> getVideoStallResult() {
		return videoStallResult;
	}

	public List<NearStall> getVideoNearStallResult(){
		return videoNearStallResult;
	}
	
	public double getStallTriggerTime() {
		return stallTriggerTime;
	}

	private void setStallTriggerTime(double stallTriggerTime) {
		this.stallTriggerTime = stallTriggerTime;
	}

	public double getStallPausePoint() {
		return stallPausePoint;
	}

	public void setStallPausePoint(double stallPausePoint) {
		this.stallPausePoint = stallPausePoint;
	}

	public double getStallRecovery() {
		return stallRecovery;
	}

	public void setStallRecovery(double stallRecovery) {
		this.stallRecovery = stallRecovery;
	}

	public double getNearStall() {
		return nearStall;
	}

	public void setNearStall(double nearStall) {
		this.nearStall = nearStall;
	}

	public void detectionForNearStall(VideoEvent chunkPlaying) {
		if (completedDownloads.contains(chunkPlaying)
				&& (!completedDownloadsWithOutNearStalls.contains(chunkPlaying))) {
			// mark chunkPlaying as nearly stalled segment
			NearStall nearStalledSegment = new NearStall(chunkPlaying.getEndTS() - (nearStall + stallPausePoint), chunkPlaying);
			videoNearStallResult.add(nearStalledSegment);
		} else if (completedDownloadsWithOutNearStalls.contains(chunkPlaying)
				&& (!veWithInPlayDownloadedSegments.isEmpty())) {
			// add veWithInPlayDownloadedSegments to completedDownloadsWithOutNearStalls collection
			for (VideoEvent ve : veWithInPlayDownloadedSegments) {
				completedDownloadsWithOutNearStalls.add(ve);
				chunkDownloadCopy.remove(ve);
			}
		}
	}

	public Map<Integer, String> populate(StreamingVideoData streamingVideoData, Map<VideoEvent, Double> chunkPlayTimeList) {
		if (videoPrefManager.getVideoUsagePreference() != null) {
			setStallTriggerTime(videoPrefManager.getVideoUsagePreference().getStallTriggerTime());
			setStallPausePoint(videoPrefManager.getVideoUsagePreference().getStallPausePoint());
			setStallRecovery(videoPrefManager.getVideoUsagePreference().getStallRecovery());
			setNearStall(videoPrefManager.getVideoUsagePreference().getNearStall());
		}
		seriesDataSets.clear();
		key = 0;
		videoStallResult = new ArrayList<>();
		videoNearStallResult = new ArrayList<>();
		if (streamingVideoData != null && streamingVideoData.getStreamingVideoCompiled() != null) {
			filteredChunk = new ArrayList<>();
			chunkDownload = new ArrayList<>();
			chunkDownloadCopy = new ArrayList<>();
			veWithInPlayDownloadedSegments = new ArrayList<>();
			veDone = new ArrayList<>();
			veWithIn = new ArrayList<>();
			completedDownloads.clear();
			beginBuffer = 0;
			endBuffer = 0;
			stallStarted = false;

			initialize(streamingVideoData);

			double bufferInSeconds = 0;
			possibleStartPlayTime = 0;
			for (int index = 0; index < streamingVideoData.getStreamingVideoCompiled().getChunksBySegment().size(); index++) {
				bufferInSeconds = 0;

				updateUnfinishedDoneVideoEvent();
				// update downloaded segments list with consideration of near stall
				updateSegementsDownloadedList();

				bufferInSeconds = drawVeDone(veDone, beginBuffer);
				veDone.clear();

				detectionForNearStall(chunkPlaying);
				bufferInSeconds = drawVeWithIn(veWithIn, bufferInSeconds);
				veWithIn.clear();

				endBuffer = bufferInSeconds;

				if (bufferInSeconds < 0) { // using -ve as stall indicator
					// if indicated push the chunk play start time
					double tempPrevTime = 0;
					if (possibleStartPlayTime != 0) {
						tempPrevTime = possibleStartPlayTime;
					}
					possibleStartPlayTime = updatePlayStartTime(chunkPlaying);

					if (possibleStartPlayTime == -1 || possibleStartPlayTime == tempPrevTime) {
						possibleStartPlayTime = updatePlayStartTimeAfterStall(chunkPlaying, stallPausePoint, stallRecovery);
						if (possibleStartPlayTime <= tempPrevTime) {
							seriesDataSets.clear();
							break;
						}
						if (!videoStallResult.isEmpty()) {
							videoStallResult.get(videoStallResult.size() - 1).setSegmentTryingToPlay(chunkPlaying);
							videoStallResult.get(videoStallResult.size() - 1).setStallEndTimeStamp(possibleStartPlayTime);
						}
						addToChunkPlayTimeList(chunkPlaying, possibleStartPlayTime);
					}
					index--;
					continue;
				}

				beginBuffer = endBuffer;
				if (index + 1 <= streamingVideoData.getStreamingVideoCompiled().getChunksBySegment().size() - 1) {
					setNextPlayingChunk(index + 1, streamingVideoData.getStreamingVideoCompiled().getChunksBySegment());
				}
			}
		}

		return seriesDataSets;

	}

	@Override
	public double drawVeDone(List<VideoEvent> veDone, double beginBuffer) {
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

		if (veWithIn.size() == 0  && completedDownloads.contains(chunkPlaying)) {
			buffer = bufferDrain(buffer);
		} else if (completedDownloads.contains(chunkPlaying)) {
			Collections.sort(veWithIn, new VideoEventComparator(SortSelection.END_TS));

			boolean drained = false;
			VideoEvent chunk;
			double timeRange;
			double durationLeft = chunkPlayTimeDuration;

			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;

			if (stallStarted) {
				updateStallInformation(chunkPlayStartTime);
				stallStarted = false;
			}

			for (int index = 0; index < veWithIn.size(); index++) {
				chunk = veWithIn.get(index);
				if (MathUtils.equals(chunk.getEndTS(), chunkPlayEndTime)) {
					// finish draining
					buffer = buffer - durationLeft;
					if (buffer <= 0) {
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
				} else {
					if (index == 0) {
						timeRange = chunk.getEndTS() - chunkPlayStartTime;
					} else {
						timeRange = chunk.getEndTS() - veWithIn.get(index - 1).getEndTS();
					}

					buffer = buffer - timeRange;
					if (buffer <= 0) {
						buffer = 0;
						stallStarted = true;
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

			if (drained == false) {
				buffer = buffer - durationLeft;
				if (buffer <= 0) {
					buffer = 0;
					stallStarted = true;
					updateStallInformation(chunkPlayEndTime);
				}
				seriesDataSets.put(key, chunkPlayEndTime + "," + buffer);
				key++;

			}

		} else {
			boolean skipStallStart = false;
			for (VideoStall stall : videoStallResult) {
				if (BigDecimal.valueOf(stall.getStallStartTimeStamp()) == BigDecimal.valueOf(chunkPlayStartTime)) {
					skipStallStart = true;
					break;
				}
			}
			if (skipStallStart == false) {
				stallStarted = true;
				VideoStall stall = new VideoStall(chunkPlayStartTime);
				videoStallResult.add(stall);
			}
			return -1;
		}

		return buffer;

	}

	private void initialize(StreamingVideoData streamingVideoData) {
		this.streamingVideoData = streamingVideoData;
		filteredChunk = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments(); // filterVideoSegment(videoUsage);
		chunkDownload = new ArrayList<>();
		chunkDownloadCopy = new ArrayList<>();
		for (VideoEvent vEvent : filteredChunk) {
			chunkDownload.add(vEvent);
			chunkDownloadCopy.add(vEvent);
		}
		veManifestList = streamingVideoData.getStreamingVideoCompiled().getVeStreamList();

		runInit(streamingVideoData, veManifestList, streamingVideoData.getStreamingVideoCompiled().getChunksBySegment());
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
			} else if (ve.getEndTS() > chunkPlayStartTime && ve.getEndTS() <= chunkPlayEndTime) {
				veWithIn.add(ve);
			}
		}
	}

	public void updateSegementsDownloadedList() {
		List<VideoEvent> toBeRemovedSegments = new ArrayList<>();
		for (VideoEvent ve : chunkDownloadCopy) {
			if (ve.getDLTimeStamp() < chunkPlayStartTime && ve.getEndTS() - (nearStall + stallPausePoint) <= chunkPlayStartTime) {
				completedDownloadsWithOutNearStalls.add(ve);
				toBeRemovedSegments.add(ve);
			} else if (ve.getEndTS()- (nearStall + stallPausePoint) > chunkPlayStartTime && ve.getEndTS() - (nearStall + stallPausePoint) <= chunkPlayEndTime) {
				veWithInPlayDownloadedSegments.add(ve);
			}
		}
		for (VideoEvent ve : toBeRemovedSegments) {
			chunkDownloadCopy.remove(ve);
		}
	}

	public void updateStallInformation(double stallTime) {
		if (stallStarted && (videoStallResult != null && videoStallResult.size()!=0)) {
			VideoStall veStall = videoStallResult.get(videoStallResult.size() - 1);
			if(veStall.getStallStartTimeStamp() != stallTime){
				veStall.setStallEndTimeStamp(stallTime);
			}
		} else {
			stallStarted = true;
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
			if (buffer <= 0) {
				buffer = 0;
				stallStarted = true;
				VideoStall stall = new VideoStall(chunkPlayEndTime);
				videoStallResult.add(stall);
			} else {
				if (stallStarted) {
					updateStallInformation(chunkPlayStartTime);
					stallStarted = false;
				}
			}

			seriesDataSets.put(key, chunkPlayEndTime + "," + buffer);
			key++;

		} else {
			// stall
			if (buffer <= 0) { // want to see if buffer >0 & stall happening
				buffer = 0;
				stallStarted = true;
				updateStallInformation(chunkPlayStartTime);
			}
			seriesDataSets.put(key, chunkPlayStartTime + "," + buffer);
			key++;
			return -1;
		}

		return buffer;
	}
	
	public Map<Long, Double> getSegmentStartTimeMap() {
		return videoChunkPlotterImpl.getSegmentStartTimeList();
	}



	public Map<Double, Long> getSegmentEndTimeMap() {
		Map<Long, Double> segmentStartTimeMap = getSegmentStartTimeMap();
		Map<Double, Long> segmentEndTimeMap = new HashMap<Double, Long>();
		if(segmentStartTimeMap!=null) {
			for (VideoEvent ve : streamingVideoData.getStreamingVideoCompiled().getFilteredSegments()) {
				if(ve == null) {
					continue;
				}
				Double startTime = segmentStartTimeMap.get(new Double(ve.getSegmentID()).longValue());
				double segmentPlayEndTime = (startTime != null ? startTime : 0.0) + getChunkPlayTimeDuration(ve);
				segmentEndTimeMap.put(segmentPlayEndTime, new Double(ve.getSegmentID()).longValue());
			}
		}
		return segmentEndTimeMap;
	}
	
	public BufferTimeBPResult updateBufferTimeResult(List<Double> bufferTimeBPResult){
		bufferTimeResult= new BufferTimeBPResult(bufferTimeBPResult);
		return bufferTimeResult;
	}
}
