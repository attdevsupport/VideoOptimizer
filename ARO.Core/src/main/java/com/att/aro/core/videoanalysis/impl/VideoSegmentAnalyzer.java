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
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.XYPair;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;

import lombok.NonNull;

public class VideoSegmentAnalyzer {

	private static final Logger LOG = LogManager.getLogger(VideoSegmentAnalyzer.class.getName());

	@Autowired private IVideoUsagePrefsManager videoUsagePrefsManager;

	private VideoStreamStartup videoStreamStartup;
	private VideoUsagePrefs videoPrefs;
	private VideoStall videoStall;

	private double stallOffset;
	private double totalStallOffset;

	private List<VideoStall> stalls = new ArrayList<>();

	@Nonnull private DUPLICATE_HANDLING duplicateHandling = DUPLICATE_HANDLING.LAST;
	
	@NonNull private ArrayList<XYPair> byteBufferList;
	@NonNull private VideoStream videoStream;
	
	private double videoDuration = 0;

	public void process(AbstractTraceResult result, StreamingVideoData streamingVideoData) {
		if (result instanceof TraceDirectoryResult) {
			videoStreamStartup = ((TraceDirectoryResult) result).getVideoStartup();
			this.videoPrefs = videoUsagePrefsManager.getVideoUsagePreference();

			if (!CollectionUtils.isEmpty(streamingVideoData.getVideoStreamMap())) {
				for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()) {
					if (videoStream.isSelected() && !CollectionUtils.isEmpty(videoStream.getVideoEventMap())) {
						
						double startupDelay;
						VideoEvent chosenEvent;
						if (videoStreamStartup != null && videoStream.getManifest().getVideoName().equals(videoStreamStartup.getManifestName())) {
							startupDelay = videoStreamStartup.getStartupDelay();
							double segID = videoStreamStartup.getFirstSegID();
							chosenEvent = videoStream.getVideoEventBySegment(segID);
						} else {
							ArrayList<VideoEvent> videoStreamFiltered = sortBySegThenStartTS(videoStream);
							if (videoStreamFiltered.stream().findFirst().isPresent()) {
								Optional<VideoEvent> event = videoStreamFiltered.stream().filter(v -> v.isNormalSegment()).findFirst();
								chosenEvent = event.isPresent() ? event.get() : videoStreamFiltered.get(0); //
								startupDelay = chosenEvent.getDLLastTimestamp() + videoPrefs.getStallRecovery();
							} else {
								continue;
							}
						}

						duplicateHandling = videoPrefs.getDuplicateHandling();
						LOG.debug(String.format("Stream RQ:%10.3f", videoStream.getManifest().getRequestTime()));
						propagatePlaytime(startupDelay, chosenEvent, videoStream);
						videoStream.setDuration(totalDurations(videoStream));
					} else {
						videoStream.setDuration(0);
						videoStream.setSelected(false);
						videoStream.setValid(false);
					}
				}
			}
		}
	}

	/**
	 * Sum all regular selected segments in videoStream to calculate total duration
	 * 
	 * @param videoStream
	 * @return duration in seconds
	 */
	private double totalDurations(VideoStream videoStream) {
		videoDuration = 0;
		videoStream.getVideoEventMap().entrySet().stream().filter(f -> f.getValue().isSelected()).forEach(value -> {
			if (value.getValue().isNormalSegment()) {
				videoDuration += value.getValue().getDuration();
			}
		});
		return videoDuration;
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
		double startupOffset;
		startupOffset = startupTime - chosenEvent.getSegmentStartTime();
		stallOffset = 0;
		totalStallOffset = 0;
		VideoEvent priorEvent = null;
		double priorDuration = 0;

		clearStalls(videoStream);
		videoStream.clearBufferOccupancyData();
		videoStream.applyStartupOffset(startupOffset);

		TreeMap<String, VideoEvent> audioStreamMap = videoStream.getAudioEventMap(); // key definition: segmentStartTime, endTS(in milliseconds)

		applyDuplicateHandlingRules(audioStreamMap);
		applyDuplicateHandlingRules(videoStream.getVideoEventMap());

		boolean isAudio = !CollectionUtils.isEmpty(audioStreamMap);

		ArrayList<VideoEvent> videoStreamFiltered = sortBySegThenStartTS(videoStream);

		for (VideoEvent videoEvent : videoStreamFiltered) {
			if (videoEvent.getSegmentID() > baseEvent) {
				if (priorEvent != null && videoEvent.isSelected()) {
					double playtime = videoEvent.getSegmentStartTime() + startupOffset + totalStallOffset;

					priorDuration = (priorEvent.getSegmentID() != videoEvent.getSegmentID()) ? priorEvent.getDuration() : priorDuration;
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
					if (isAudio 
							&& (audioStall = syncWithAudio(startupOffset, videoStream, audioStreamMap, videoEvent)) != null 
							&& audioStall.getStallTime() > 0) {
						LOG.debug(String.format("audioStall %.0f: %8.6f", audioStall.getSegmentID(), audioStall.getStallTime()));
					}
					videoEvent.setPlayTime(playtime);
				}
				priorEvent = videoEvent;
			}
		}

		generateByteBufferData(videoStream);
		LOG.debug(videoStream.getToolTipDetailMap());
	}

	public ArrayList<VideoEvent> sortBySegThenStartTS(VideoStream videoStream) {
		ArrayList<VideoEvent> videoStreamFiltered = new ArrayList<VideoEvent>(videoStream.getVideoEventMap().values());

		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_ID));
		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_START_TS));
		return videoStreamFiltered;
	}

	private void generateByteBufferData(VideoStream videoStream) {
		@NonNull VideoEvent eventPlay = null;
		Double buffer = 0D;
		VideoEvent eventDL;
		double timeKey = 0D;
		
		this.videoStream = videoStream;
		videoStream.clearBufferOccupancyData();

		TreeMap<Double, VideoEvent> mergedPlayMap = new TreeMap<>();
		TreeMap<String, VideoEvent> mergedMap = new TreeMap<>();
		mergedMap.putAll(videoStream.getVideoEventMap());
		mergedMap.putAll(videoStream.getAudioEventMap());

		byteBufferList = videoStream.getByteBufferList();
		LOG.debug("\ntime     buffer");
		for (String key : mergedMap.keySet()) {
			eventDL = mergedMap.get(key);
			if (eventDL.isNormalSegment()) {
				if (timeKey > 0 && (timeKey < eventDL.getEndTS())) {
					eventPlay = mergedPlayMap.get(timeKey);
					while (eventPlay != null && eventPlay.getPlayTime() <= eventDL.getEndTS()) {
						mergedPlayMap.remove(eventPlay.getPlayTime());
						buffer = addBufferPoints(buffer, eventPlay, eventPlay.getPlayTime(), -eventPlay.getSize());
						timeKey = mergedPlayMap.isEmpty() ? 0 : mergedPlayMap.firstKey();
						eventPlay = mergedPlayMap.isEmpty() ? null : mergedPlayMap.get(mergedPlayMap.firstKey());
					}
				}
				mergedPlayMap.put(eventDL.getPlayTime(), eventDL);
				timeKey = mergedPlayMap.firstKey();
				buffer = addBufferPoints(buffer, eventDL, eventDL.getEndTS(), eventDL.getSize());

			}
		}
		timeKey = mergedPlayMap.isEmpty() ? 0 : mergedPlayMap.firstKey();
		while (!mergedPlayMap.isEmpty()) {
			eventPlay = mergedPlayMap.remove(timeKey);
			buffer = addBufferPoints(buffer, eventPlay, eventPlay.getPlayTime(), -eventPlay.getSize());
			timeKey = mergedPlayMap.isEmpty() ? 0 : mergedPlayMap.firstKey();
			eventPlay = mergedPlayMap.isEmpty() ? null : mergedPlayMap.get(mergedPlayMap.firstKey());
		}
		LOG.debug("done");

	}

	public Double addBufferPoints(Double buffer, VideoEvent event, double startTS, double delta) {
		double buffer2 = buffer + delta;
		byteBufferList.add(new XYPair(startTS, buffer));
		byteBufferList.add(new XYPair(startTS, buffer2));
		this.videoStream.addToolTipPoint(event, buffer);
		this.videoStream.addToolTipPoint(event, buffer2);
		LOG.debug(String.format("%.3f\t%.0f", startTS, buffer));
		LOG.debug(String.format("%.3f\t%.0f", startTS, buffer2));
		return buffer2;
	}

	private void applyDuplicateHandlingRules(SortedMap<String, VideoEvent> eventStreamMap) {
		VideoEvent priorEvent = null;
		ArrayList<VideoEvent> videoStreamFiltered = new ArrayList<VideoEvent>(eventStreamMap.values());
		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.END_TS));
		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_ID));
		
		for (VideoEvent event : videoStreamFiltered) {
			event.setSelected(true); // set to 'selected' so applyRules can judge
			applyRules(event, priorEvent);
			if (event.isSelected()) {
				priorEvent = event;
			}
		}
	}

	/**
	 * Compare event and priorEvent against duplicateHandling rules.
	 * When a VideoEvent has a competing segmentID, the two VideoEvents will be compared with the "loser" being deselected
	 * 
	 * @param event
	 * @param priorEvent
	 */
	private void applyRules(VideoEvent event, VideoEvent priorEvent) {
		boolean tempFlag = false;
		if (priorEvent != null) {
			if (priorEvent.getSegmentID() == event.getSegmentID()) {
				if (duplicateHandling.equals(DUPLICATE_HANDLING.FIRST)) {
					tempFlag = event.getEndTS() < priorEvent.getEndTS();
				} else if (duplicateHandling.equals(DUPLICATE_HANDLING.LAST)) {
					tempFlag = event.getEndTS() > priorEvent.getEndTS();
				} else if (duplicateHandling.equals(DUPLICATE_HANDLING.HIGHEST)) {
				tempFlag = Integer.valueOf(event.getQuality()).compareTo(Integer.valueOf(priorEvent.getQuality())) > 0;
				}
				event.setSelected(tempFlag);
				priorEvent.setSelected(!tempFlag);
			}
			
		}
	}

	/**
	 * <pre>
	 * Calculate a segmentStallOffset in seconds.
	 * 
	 * @param current     startupOffset in seconds
	 * @param videoEvent  either Audio or Video
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
	 * @param videoStream      contains collections of Video, Audio and Captioning
	 * @param audioStreamMap contains all audio in videoStream (when non-muxed) <key definition: segmentStartTime, endTS(in milliseconds)>
	 * @param videoEvent       The video segment to receive audio linkage
	 * @param appliedStallTime
	 * @return audioEvent associated with a stall
	 */
	public VideoEvent syncWithAudio(double startupOffset, VideoStream videoStream, TreeMap<String, VideoEvent> audioStreamMap, VideoEvent videoEvent) {
		VideoEvent audioEvent = null;
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
				VideoEvent tempEvent = audioStreamMap.get(key);
				if (tempEvent.isSelected()) {
					audioEvent = tempEvent;
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
						videoStall.setStallEndTimestamp(resumePoint);
						stalls.add(videoStall);
					}
				}
				// advance to next segmentStartTime
				key = audioStreamMap.higherKey(StringUtils.substringBefore(key, ":") + "z");
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

		} else if (aEnd < vEnd) {
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
		clearStalls(videoStream.getVideoEventMap());
		clearStalls(videoStream.getAudioEventMap());
		clearStalls(videoStream.getCcEventList());
	}

	private void clearStalls(SortedMap<String, VideoEvent> eventList) {
		eventList.entrySet().parallelStream().forEach(x -> {
			x.getValue().setStallTime(0);
		});
	}

}
