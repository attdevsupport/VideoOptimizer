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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.att.aro.core.peripheral.pojo.VideoStreamStartupData;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.XYPair;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;

import lombok.NonNull;

public class VideoSegmentAnalyzer {

	private static final Logger LOG = LogManager.getLogger(VideoSegmentAnalyzer.class.getName());

	@Autowired private IVideoUsagePrefsManager videoUsagePrefsManager;

	private VideoStreamStartupData videoStreamStartupData;
	private VideoStreamStartup videoStreamStartup;
	private VideoUsagePrefs videoPrefs;
	private VideoStall videoStall;

	private double stallOffset;
	private double totalStallOffset;

	private List<VideoStall> stalls = new ArrayList<>();

	@Nonnull private DUPLICATE_HANDLING duplicateHandling = DUPLICATE_HANDLING.LAST;
	
	@NonNull private ArrayList<XYPair> byteBufferList; // from videoStream.getByteBufferList();
	@NonNull private VideoStream videoStream;

	@NonNull private ArrayList<XYPair> playTimeList;

	public VideoStreamStartup findStartupFromName(VideoStreamStartupData videoStreamStartupData, VideoStream videoStream) {
		if (videoStreamStartupData != null && videoStream != null) {
			for (VideoStreamStartup videoStreamStartup : videoStreamStartupData.getStreams()) {
				if (videoStream.getManifest().getVideoName().equals(videoStreamStartup.getManifestName())) {
					return this.videoStreamStartup = videoStreamStartup;
				}
			}
		}
		return null;
	}

	public void process(AbstractTraceResult result, StreamingVideoData streamingVideoData) {
		if (result instanceof TraceDirectoryResult) {
			videoStreamStartupData = ((TraceDirectoryResult) result).getVideoStartupData();
			this.videoPrefs = videoUsagePrefsManager.getVideoUsagePreference();
			if (!CollectionUtils.isEmpty(streamingVideoData.getVideoStreamMap())) {
				for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()) {
					if (videoStream.isSelected() && !CollectionUtils.isEmpty(videoStream.getVideoEventMap())) {
						videoStreamStartup = findStartupFromName(videoStreamStartupData, videoStream);
						double startupDelay;
						VideoEvent chosenEvent;
						if (videoStreamStartup != null && videoStream.getManifest().getVideoName().equals(videoStreamStartup.getManifestName())) {
							startupDelay = videoStreamStartup.getStartupTime();
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
						videoStream.setDuration(videoStream.getVideoEventMap().entrySet().stream().filter(f -> f.getValue().isSelected() && f.getValue().isNormalSegment()).mapToDouble(x -> x.getValue().getDuration()).sum());
					} else {
						videoStream.setDuration(0);
						videoStream.setSelected(false);
						videoStream.setValid(false);
					}
				}
			}
		}
	}

	private void populateActive(VideoStream videoStream) {
		populateActiveMap(videoStream.getVideoActiveMap(), videoStream.getVideoStartTimeMap());
		populateActiveMap(videoStream.getAudioActiveMap(), videoStream.getAudioStartTimeMap());
		populateActiveMap(videoStream.getCcActiveMap(), videoStream.getCcEventMap());
	}

	private void populateActiveMap(SortedMap<String, VideoEvent> activeMap, SortedMap<String, VideoEvent> eventMap) {
		activeMap.clear();
		eventMap.entrySet().stream().filter(f -> f.getValue().isSelected() && f.getValue().isNormalSegment()).forEach(v -> activeMap.put(v.getKey(), v.getValue()));
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
	 * @param startupTime
	 * @param chosenEvent
	 * @param videoStream
	 */
	public void propagatePlaytime(double startupTime, VideoEvent chosenEvent, VideoStream videoStream) {

		int baseEvent = videoStream.getManifest().isVideoFormat(VideoFormat.MPEG4) ? 0 : -1;
		double startupOffset;
		startupOffset = chosenEvent != null ? startupTime - chosenEvent.getSegmentStartTime() : 0;
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
					if (videoEvent.getStallTime() != 0) {
						videoStream.addStall(videoEvent);
					}
				}
				priorEvent = videoEvent;
			}
		}

		populateActive(videoStream);
		generateByteBufferData(videoStream);
		generatePlayTimeData(videoStream);

		LOG.debug(videoStream.getToolTipDetailMap().keySet());
	}

	private ArrayList<VideoEvent> sortBySegThenStartTS(VideoStream videoStream) {
		ArrayList<VideoEvent> videoStreamFiltered = new ArrayList<VideoEvent>(videoStream.getVideoEventMap().values());

		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_ID));
		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_START_TS));
		return videoStreamFiltered;
	}

	/**
	 * Scans VideoStream to produce/populate
	 * 	- VideoStream.
	 *  - VideoStream.
	 *  
	 * @param timelineMap
	 * @param eventTimeLine
	 */
	private void generatePlayTimeData(VideoStream videoStream) {
		Double playTime = 0D;
		this.videoStream = videoStream;
		videoStream.clearPlayTimeData();

		playTimeList = videoStream.getPlayTimeList();
		TreeMap<String, VideoEvent> videoActiveMap = videoStream.getVideoActiveMap(); // VideoSegments that are considered as playing
		TreeMap<Double, Double> timelineMap = new TreeMap<>();
		TreeMap<Double, VideoEvent> stallMap = new TreeMap<>();

		Map<Double, VideoEvent> eventTimeLine = populateTimelineMaps(playTime, videoActiveMap, timelineMap, stallMap);

		generatePlayPoints(timelineMap, eventTimeLine);
	}

	/**
	 * Populates timelineMap, and stallMap
	 * 
	 * @param playTime
	 * @param videoActiveMap
	 * @param timelineMap
	 * @param stallMap
	 * 
	 * @return total playtime
	 */
	private Map<Double, VideoEvent> populateTimelineMaps(Double playTime, TreeMap<String, VideoEvent> videoActiveMap, TreeMap<Double, Double> timelineMap, TreeMap<Double, VideoEvent> stallMap) {
		double stallCheck = 0;
		Map<Double, VideoEvent> eventTimeLine = new TreeMap<>();
		for (VideoEvent event : videoActiveMap.values()) {

			timelineMap.put(event.getEndTS(), event.getDuration());
			timelineMap.put(event.getPlayTime(), -event.getDuration());
			
			eventTimeLine.put(event.getEndTS(), event);
			eventTimeLine.put(event.getPlayTime(), event);

			if (event.getStallTime() > 0) {
				timelineMap.put(stallCheck, 0D);
				stallMap.put(event.getPlayTime() - event.getStallTime(), event);
			}
			stallCheck = event.getPlayTime() + event.getDuration();
		}
		return eventTimeLine;
	}
	
	private Double addPlayTimePoints(Double buffer, VideoEvent event, double timestamp, double duration) {
		
		double buffer2 = buffer + duration;
		playTimeList.add(new XYPair(timestamp, buffer2));
		
		this.videoStream.addPlayTimeToolTipPoint(event, buffer);
		this.videoStream.addPlayTimeToolTipPoint(event, buffer2);

		LOG.debug(String.format("t%.3f\t%.3f", timestamp, buffer2));
		
		return buffer2;
	}

	/**
	 * Generates points for Playtimeline (timestamp, play_time_buffer)
	 * Generates tooltip data
	 * 
	 * @param timelineMap
	 * @param eventTimeLine
	 */
	private void generatePlayPoints(TreeMap<Double, Double> timelineMap, Map<Double, VideoEvent> eventTimeLine) {
		boolean isPlaying = false;
		Double playTime;
		VideoEvent videoEvent;

		playTime = 0D;
		Double dur2 = 0D, dur1 = 0D;
		Double ts1 = 0D, ts2 = 0D;
		Double stepDur1 = 0D;

		Iterator<Double> timeLineIterator = timelineMap.keySet().iterator();
		while ((ts1 = getNextKey(timeLineIterator)) != null) {
			videoEvent = eventTimeLine.get(ts1);
			dur1 = timelineMap.get(ts1);
			if ((ts2 = timelineMap.higherKey(ts1)) != null) {
				dur2 = timelineMap.get(ts2);
			}

			if (dur2 == null) {
				// reached the end
				playTime = addPlayTimePoints(playTime, videoEvent, ts1, 0);
				playTime = addPlayTimePoints(playTime, videoEvent, ts1, dur1);
			} else {
				if (dur1 == 0) {
					isPlaying = false; // stalled
				}

				if (!isPlaying) {
					// filling buffer
					playTime = addPlayTimePoints(playTime, videoEvent, ts1, 0);
					if (dur1 > 0) {
						playTime = addPlayTimePoints(playTime, videoEvent, ts1, dur1);
					} else if (dur1 < 0) {
						isPlaying = true; // not stalled anymore
					}
				}

				if (isPlaying) {
					if (ts2 != null && (dur1 < 0 && (stepDur1 = ts2 - ts1) < Math.abs(dur1) - 1e-6)) {
						do {
							playTime = addPlayTimePoints(playTime, videoEvent, ts2, -stepDur1);
							if (nearEquals(stepDur1, Math.abs(dur1), -7)) {
								break;
							}
							if (dur1 < 0 && Math.abs(dur1 += stepDur1) > 1e-6) {
								playTime = addPlayTimePoints(playTime, videoEvent, ts2, dur2);
							} else {
								ts1 = getNextKey(timeLineIterator);
								ts2 = getNextKey(timeLineIterator);
								if (ts1 != null && ts2 != null) {
									stepDur1 = ts2 - ts1;
									dur2 = timelineMap.get(ts2);
									playTime = addPlayTimePoints(playTime, videoEvent, ts2, -(ts2 - ts1));
									if (dur2 == 0) {
										// skip
										ts1 = getNextKey(timeLineIterator);
										isPlaying = false;
									} else {
										ts2 = getNextKey(timeLineIterator);
										playTime = addPlayTimePoints(playTime, videoEvent, ts2, dur2);
									}
								}
								break;
							}
							if ((ts1 = getNextKey(timeLineIterator)) != null && (ts2 = timelineMap.higherKey(ts1)) != null) {
								dur2 = timelineMap.get(ts2);
							}
							if (ts1 != null && ts2 != null) {
								stepDur1 = ts2 - ts1;
							}

						} while (dur1 < 1e-6 && ts2 != null);
					} else {
						if (dur2 == 0) {
							isPlaying = false;
						}
						if (ts2 != null) {
							playTime = addPlayTimePoints(playTime, videoEvent, ts2, dur1);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Compare two Doubles while allowing for some degree of drift. I difference between the doubles is less than 10^exp
	 * 
	 * @param stepDur1
	 * @param dur1
	 * @param exp
	 * @return
	 */
	private boolean nearEquals(Double stepDur1, Double dur1, double exp) {
		return (Math.abs(stepDur1 - dur1) < Math.pow(10, exp));
	}

	private Double getNextKey(Iterator<Double> timeLineIterator) {
		Double timestamp = null;
		if (timeLineIterator.hasNext()) {
			timestamp = timeLineIterator.next();
		}
		return timestamp;
	}

	/**
	 * Scans VideoStream to produce/populate
	 * 	- VideoStream.byteBufferList
	 *  - VideoStream.toolTipDetailMap
	 * 
	 * @param videoStream
	 */
	private void generateByteBufferData(VideoStream videoStream) {
		VideoEvent eventPlay = null;
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
						buffer = addByteBufferPoints(buffer, eventPlay, eventPlay.getPlayTime(), -eventPlay.getSize());
						timeKey = mergedPlayMap.isEmpty() ? 0 : mergedPlayMap.firstKey();
						eventPlay = mergedPlayMap.isEmpty() ? null : mergedPlayMap.get(mergedPlayMap.firstKey());
					}
				}
				mergedPlayMap.put(eventDL.getPlayTime(), eventDL);
				timeKey = mergedPlayMap.firstKey();
				buffer = addByteBufferPoints(buffer, eventDL, eventDL.getEndTS(), eventDL.getSize());

			}
		}
		timeKey = mergedPlayMap.isEmpty() ? 0 : mergedPlayMap.firstKey();
		while (!mergedPlayMap.isEmpty()) {
			eventPlay = mergedPlayMap.remove(timeKey);
			buffer = addByteBufferPoints(buffer, eventPlay, eventPlay.getPlayTime(), -eventPlay.getSize());
			timeKey = mergedPlayMap.isEmpty() ? 0 : mergedPlayMap.firstKey();
			eventPlay = mergedPlayMap.isEmpty() ? null : mergedPlayMap.get(mergedPlayMap.firstKey());
		}
	}

	private Double addByteBufferPoints(Double buffer, VideoEvent event, double startTS, double delta) {
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
	private double calcSegmentStallOffset(double startupOffset, VideoEvent videoEvent, double totalStallOffset) {
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
	private VideoEvent syncWithAudio(double startupOffset, VideoStream videoStream, TreeMap<String, VideoEvent> audioStreamMap, VideoEvent videoEvent) {
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

	private double calcAudioTime(VideoEvent videoEvent, VideoEvent audioEvent) {
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

	private void clearStalls(VideoStream videoStream) {
		clearStalls(videoStream.getVideoEventMap());
		clearStalls(videoStream.getAudioEventMap());
		clearStalls(videoStream.getCcEventMap());
	}

	private void clearStalls(SortedMap<String, VideoEvent> eventList) {
		eventList.entrySet().parallelStream().forEach(x -> {
			x.getValue().setStallTime(0);
		});
	}

}
