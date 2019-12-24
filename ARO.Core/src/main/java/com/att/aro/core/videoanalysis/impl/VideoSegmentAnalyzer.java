/*
 *  Copyright 2019 AT&T
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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;

public class VideoSegmentAnalyzer {

	private static final Logger LOG = LogManager.getLogger(VideoSegmentAnalyzer.class.getName());

	@Autowired private IVideoUsagePrefsManager videoUsagePrefsManager;

	private VideoStreamStartup videoStreamStartup;
	private VideoUsagePrefs videoPrefs;
	private VideoStall videoStall;

	private double stallOffset;
	private double totalStallOffset;

	private List<VideoStall> stalls = new ArrayList<>();

	public void process(AbstractTraceResult result, StreamingVideoData streamingVideoData) {
		if (result instanceof TraceDirectoryResult) {
			videoStreamStartup = ((TraceDirectoryResult) result).getVideoStartup();
			this.videoPrefs = videoUsagePrefsManager.getVideoUsagePreference();

			if (!CollectionUtils.isEmpty(streamingVideoData.getVideoStreamMap())) {
				for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()) {
					if (videoStream.isSelected() && !videoStream.getVideoEventList().isEmpty()) {
						double startupDelay;
						VideoEvent chosenEvent;
						if (videoStreamStartup != null && videoStream.getManifest().getVideoName().equals(videoStreamStartup.getManifestName())) {
							startupDelay = videoStreamStartup.getStartupDelay();
							double segID = videoStreamStartup.getFirstSegID();
							chosenEvent = videoStream.getVideoEventBySegment(segID);
						} else {
							chosenEvent = videoStream.getVideoEventList().get(videoStream.getVideoEventList().firstKey());
							startupDelay = chosenEvent.getDLLastTimestamp() + videoPrefs.getStallRecovery();
						}
						propagatePlaytime(startupDelay, chosenEvent, videoStream);
					}
				}
			}
		}
	}

	/**
	 * not quite there yet
	 * 
	 * for each segment:
	 *  playtime = SegmentStartTime + startupOffset + stallOffset
	 * 
	 * if playtime is before segment has arrived
	 *  need to increment stallOffset to bring playtime up to arrival time plus all overhead of recovery from stall
	 * 
	 * TODO account for ALL recovery time for stall, not just finished the download
	 * 
	 * @param startupTime
	 * @param chosenEvent
	 * @param videoStream
	 */
	public void propagatePlaytime(double startupTime, VideoEvent chosenEvent, VideoStream videoStream) {
		int baseEvent = videoStream.getManifest().isVideoTypeFamily(VideoType.DASH) ? 0 : -1;
		double startupOffset = startupTime - chosenEvent.getSegmentStartTime();
		stallOffset = 0;
		totalStallOffset = 0;
		VideoEvent priorEvent = null;
		double priorDuration = 0;

		clearStalls(videoStream);
		videoStream.applyStartupOffset(startupOffset);

		TreeMap<String, VideoEvent> audioStreamMap = videoStream.getAudioStartTimeMap();
		boolean isAudio = !CollectionUtils.isEmpty(audioStreamMap);

		DUPLICATE_HANDLING duplicateHandling = videoPrefs.getDuplicateHandling();
		ArrayList<VideoEvent> videoStreamFiltered = new ArrayList<VideoEvent>(videoStream.getVideoEventList().values());

		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_START_TS));

		for (VideoEvent videoEvent : videoStreamFiltered) {
			if (videoEvent.getSegmentID() > baseEvent) {

				if (priorEvent != null && priorEvent.getSegmentID() == videoEvent.getSegmentID()) {
					if (duplicateHandling.equals(DUPLICATE_HANDLING.FIRST)) {
						if (videoEvent.getEndTS() < priorEvent.getEndTS()) {
							priorEvent.setSelected(false);
							videoEvent.setSelected(true);
						} else {
							priorEvent.setSelected(true);
							videoEvent.setSelected(false);
							continue;
						}
					} else if (duplicateHandling.equals(DUPLICATE_HANDLING.LAST)) {
						if (videoEvent.getEndTS() > priorEvent.getEndTS()) {
							priorEvent.setSelected(false);
							videoEvent.setSelected(true);
						} else {
							priorEvent.setSelected(true);
							videoEvent.setSelected(false);
							continue;
						}
					} else if (videoEvent.getQuality().compareTo(priorEvent.getQuality()) > 0) {
						priorEvent.setSelected(false);
						videoEvent.setSelected(true);
					} else {
						priorEvent.setSelected(true);
						videoEvent.setSelected(false);
						continue;
					}

				}

				double playtime = videoEvent.getSegmentStartTime() + startupOffset + totalStallOffset;

				priorDuration = (priorEvent != null && priorEvent.getSegmentID() != videoEvent.getSegmentID()) ? priorEvent.getDuration() : priorDuration;
				if (videoEvent.getSegmentID() == 39) {
					LOG.debug(String.format("%.0f dl:%.3f ? pt:%.3f"
							, videoEvent.getSegmentID()
							, videoEvent.getDLLastTimestamp()
							, playtime
							));
				}
				if (videoEvent.getDLLastTimestamp() > playtime) {
					// generate a video segment caused stall
					double newStallOffset = 0;
					newStallOffset = calcSegmentStallOffset(startupOffset, videoEvent, totalStallOffset);
					totalStallOffset += newStallOffset;
					videoEvent.setStallTime(newStallOffset);
					stallOffset = newStallOffset;
					playtime += videoEvent.getStallTime();
					LOG.debug(String.format("VideoStall %.0f: %8.6f", videoEvent.getSegmentID(), videoEvent.getStallTime()));
				}
				VideoEvent audioStall;
				if (isAudio && (audioStall = syncWithAudio(startupOffset, videoStream, audioStreamMap, videoEvent)) != null && audioStall.getStallTime() > 0) {
					LOG.debug(String.format("audioStall %.0f: %8.6f", audioStall.getSegmentID(), audioStall.getStallTime()));
				}
				videoEvent.setPlayTime(playtime);
				priorEvent = videoEvent;
			}
		}
	}

	/** <pre>
	 * Calculate a segmentStallOffset in seconds.
	 * 
	 * @param current startupOffset in seconds
	 * @param videoEvent either Audio or Video
	 * @param stallOffset 
	 * @return double value in seconds
	 */
	public double calcSegmentStallOffset(double startupOffset, VideoEvent videoEvent, double totalStallOffset) {
		double seqTime = videoEvent.getDLLastTimestamp() - (startupOffset + videoEvent.getSegmentStartTime());
		if (seqTime > 0) {
			LOG.debug(String.format("(%.3f) Stall found: videoEvent :%.0f", seqTime, videoEvent.getSegmentID()));
		}
		double offset = seqTime + getStallRecovery() - totalStallOffset;
		if (offset < 0) {
			LOG.error("Illegal stall offset: " + offset);
		}
		return offset;
	}

	/**<pre>
	 * Scan through all audio event related to videoEvent
	 *   Starting with audio event from before the videoEvent
	 *   Record all audio segments associated with Video segment.
	 *   Including partial overlaps, often audio and video segments do not start at the same time.
	 * 
	 * @param startupOffset 
	 *   
	 * @param videoStream contains collections of Video, Audio and Captioning
	 * @param audioStreamMap contains all audio in videoStream (when non-muxed)
	 * @param videoEvent The video segment to receive audio linkage
	 * @param appliedStallTime 
	 * @return audioEvent associated with a stall
	 */
	public VideoEvent syncWithAudio(double startupOffset, VideoStream videoStream, TreeMap<String, VideoEvent> audioStreamMap, VideoEvent videoEvent) {
		VideoEvent audioEvent = null;
		if (videoEvent.getSegmentID() == 39) {
			LOG.debug(String.format("%.0f dl:%.3f ? pt:%.3f"
					, videoEvent.getSegmentID()
					, videoEvent.getDLLastTimestamp()
					, videoEvent.getPlayTime()
					));
		}
		String segmentStartTime = VideoStream.generateTimestampKey(videoEvent.getSegmentStartTime());
		String segmentEndTime   = VideoStream.generateTimestampKey(videoEvent.getSegmentStartTime() + videoEvent.getDuration());

		String audioKeyStart = null;
		String audioKeyEnd = null;
		try {
			audioKeyStart = audioStreamMap.lowerKey(segmentStartTime);
			audioKeyEnd = audioStreamMap.higherKey(segmentEndTime);

			String key = audioKeyStart;

			while (!key.equals(audioKeyEnd)) {
				VideoEvent lastAudioEvent = audioEvent;
				audioEvent = audioStreamMap.get(key);
				calcAudioTime(videoEvent, audioEvent);

				double audioPlaytime = audioEvent.getSegmentStartTime() + startupOffset + totalStallOffset;

				if (audioEvent.getDLLastTimestamp() > audioPlaytime) {

					double stallPoint = lastAudioEvent.getSegmentStartTime() + audioEvent.getDuration() - videoPrefs.getStallPausePoint();
					stallOffset = audioEvent.getDLLastTimestamp() - audioEvent.getPlayTime() + getStallRecovery();
					stallOffset = calcSegmentStallOffset(startupOffset, audioEvent, totalStallOffset);
					stallOffset = audioEvent.getDLLastTimestamp() - audioPlaytime + getStallRecovery();

					audioEvent.setStallTime(stallOffset);
					videoEvent.setStallTime(stallOffset);
					totalStallOffset += stallOffset;
					videoStall = new VideoStall(stallPoint);
					videoStall.setSegmentTryingToPlay(audioEvent);
					videoStall.setStallEndTimestamp(audioEvent.getPlayTime());	

					double resumePoint = audioEvent.getDLLastTimestamp() + getStallRecovery();
					videoStall.setStallEndTimestamp(resumePoint );
					stalls.add(videoStall);
				}
				key = audioStreamMap.higherKey(key);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return audioEvent;
	}

	public double calcAudioTime(VideoEvent videoEvent, VideoEvent audioEvent) {
		double aStart = audioEvent.getSegmentStartTime();
		double aEnd = audioEvent.getSegmentStartTime() + audioEvent.getDuration();
		double vStart = videoEvent.getSegmentStartTime();
		double vEnd = videoEvent.getSegmentStartTime() + videoEvent.getDuration();
		double duration;

		if (aStart >= vStart) {
			if (aEnd <= vEnd) {
				duration = audioEvent.getDuration();
				videoEvent.addAudioPart(aStart, duration, audioEvent);
			} else {
				duration = vEnd - aStart;
				videoEvent.addAudioPart(aStart, duration, audioEvent);
			}

		} else if (aEnd < vEnd){
			duration = aEnd - vStart;
			videoEvent.addAudioPart(vStart, duration, audioEvent);
		} else {
			duration = 0;
		}

		return duration;
	}

	/**
	 * incorporates stallRecovery + stallPausePoint
	 * @return
	 */
	private double getStallRecovery() {
		return videoPrefs.getStallRecovery() + videoPrefs.getStallPausePoint();
	}

	public double getStartupDelay() {
		return videoStreamStartup.getStartupDelay();
	}

	private void clearStalls(VideoStream videoStream) {
		clearStalls(videoStream.getVideoEventList());
		clearStalls(videoStream.getAudioEventList());
		clearStalls(videoStream.getCcEventList());
	}

	private void clearStalls(SortedMap<String, VideoEvent> eventList) {
		eventList.entrySet().parallelStream().forEach(x -> {
			x.getValue().setStallTime(0);
		});
	}

}
