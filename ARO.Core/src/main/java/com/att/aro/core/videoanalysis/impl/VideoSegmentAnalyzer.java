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
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.peripheral.pojo.UserEvent;
import com.att.aro.core.peripheral.pojo.UserEvent.UserEventType;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup.ValidationStartup;
import com.att.aro.core.peripheral.pojo.VideoStreamStartupData;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.XYPair;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;

import lombok.NonNull;

public class VideoSegmentAnalyzer {

	private static final Logger LOG = LogManager.getLogger(VideoSegmentAnalyzer.class.getName());

	@Autowired
	private IVideoUsagePrefsManager videoUsagePrefsManager;

	private VideoStreamStartupData videoStreamStartupData;
	private VideoStreamStartup videoStreamStartup;
	private VideoUsagePrefs videoPrefs;
	private VideoStall videoStall;

	private double stallOffset;
	private double totalStallOffset;

	private List<VideoStall> stalls = new ArrayList<>();

	@Nonnull
	private DUPLICATE_HANDLING duplicateHandling = DUPLICATE_HANDLING.LAST;

	@NonNull
	private ArrayList<XYPair> byteBufferList; // from videoStream.getByteBufferList();
	@NonNull
	private VideoStream videoStream;

	@NonNull
	private ArrayList<XYPair> playTimeList;

	public VideoStreamStartup findStartupFromName(VideoStreamStartupData videoStreamStartupData, VideoStream videoStream) {
		if (videoStreamStartupData != null && videoStream != null) {
			for (VideoStreamStartup videoStreamStartup : videoStreamStartupData.getStreams()) {
				if (videoStream.getManifest().getVideoName().equals(videoStreamStartup.getManifestName())) {
					videoStream.getManifest().setDelay(videoStreamStartup.getStartupTime() - videoStreamStartup.getManifestReqTime());
					return this.videoStreamStartup = videoStreamStartup;
				}
			}
		}
		return null;
	}

	/**
	 * <pre>
	 * Loads, and or creates estimated, startup data for a stream Populates
	 * VideoStreamStartup from first segment and manifest data. Populates
	 * VideoStream so that graphs can be displayed. Attaches to VideoStream to aid
	 * SegmentTablePanel
	 *
	 * @param result
	 * @param videoStream
	 * @return existing or estimated VideoStreamStartup
	 */
	public VideoStreamStartup locateStartupDelay(AbstractTraceResult result, VideoStream videoStream) {
		if (result instanceof TraceDirectoryResult) {
			if ((videoStreamStartupData = ((TraceDirectoryResult) result).getVideoStartupData()) != null) {
				if ((videoStreamStartup = findStartupFromName(videoStreamStartupData, videoStream)) != null) {
					if (videoStreamStartup.getValidationStartup().equals(ValidationStartup.NA)) {
						videoStreamStartup.setValidationStartup(ValidationStartup.USER);
					}
				}
			} else {
				videoStreamStartupData = new VideoStreamStartupData();
			}
			if (videoStreamStartup == null) {
				VideoEvent firstEvent = null;
				videoStreamStartup = new VideoStreamStartup(videoStream.getManifest().getVideoName());
				videoStreamStartup.setValidationStartup(ValidationStartup.ESTIMATED);
				videoStreamStartupData.getStreams().add(videoStreamStartup);
				if (!CollectionUtils.isEmpty(videoStream.getVideoActiveMap())) {
					firstEvent = videoStream.getFirstActiveSegment();
				} else {
					firstEvent = videoStream.getFirstSegment();
					if (firstEvent == null) {
						return null; // invalid stream, no first segment that is a normal segment
					}
					if (videoStream.getManifest().getRequestTime() == 0.0) {
						// CSI there is no requestTime so make an estimate
						videoStream.getManifest().setRequestTime(firstEvent.getRequest().getTimeStamp() - videoPrefs.getStallRecovery());
					}
				}				
				
				if (firstEvent.getPlayRequestedTime() == 0) {
					firstEvent.setPlayRequestedTime(videoStream.getManifest().getRequestTime());
				}
				firstEvent.setStartupOffset(firstEvent.getDLLastTimestamp() + videoPrefs.getStallRecovery());
				
				videoStreamStartup.setFirstSegID(firstEvent.getSegmentID());
				videoStreamStartup.setManifestReqTime(firstEvent.getManifest().getRequestTime());
				videoStreamStartup.setStartupTime(firstEvent.getStartupOffset());
				
				if (videoStreamStartup.getUserEvent() == null) {
					UserEvent userEvent = new UserEvent();
					double pressTime = videoStream.getManifest().getRequestTime();
					userEvent.setPressTime(pressTime);
					userEvent.setReleaseTime(pressTime);
					userEvent.setEventType(UserEventType.EVENT_UNKNOWN);
					videoStreamStartup.setUserEvent(userEvent);
				}
			}

			videoStream.getManifest().setDelay(videoStreamStartup.getStartupTime() - videoStreamStartup.getManifestReqTime());
			videoStream.setVideoPlayBackTime(videoStreamStartup.getStartupTime());
			videoStream.setVideoStreamStartup(videoStreamStartup);
			((TraceDirectoryResult) result).setVideoStartupData(videoStreamStartupData);

		}
		return videoStreamStartup;
	}

	public void process(AbstractTraceResult result, StreamingVideoData streamingVideoData) {
		if (result instanceof TraceDirectoryResult) {
			videoStreamStartupData = ((TraceDirectoryResult) result).getVideoStartupData();
			this.videoPrefs = videoUsagePrefsManager.getVideoUsagePreference();
			if (!CollectionUtils.isEmpty(streamingVideoData.getVideoStreamMap())) {
				NavigableMap<Double, VideoStream> reverseVideoStreamMap = streamingVideoData.getVideoStreamMap().descendingMap();
				for (VideoStream videoStream : reverseVideoStreamMap.values()) {
					if (!CollectionUtils.isEmpty(videoStream.getVideoEventMap())) {
						if ((videoStreamStartup = locateStartupDelay(result, videoStream)) == null) {
							continue; // StartupDelay could not be set, usually an invalid Stream
						}

						double startupDelay;
						VideoEvent chosenEvent;
						if (videoStreamStartup != null && videoStream.getManifest().getVideoName().equals(videoStreamStartup.getManifestName())) {
							startupDelay = videoStreamStartup.getStartupTime();
							chosenEvent = videoStream.getVideoEventBySegment(videoStreamStartup.getFirstSegID());
							if (videoStreamStartup.getUserEvent() != null) {
								videoStream.setPlayRequestedTime(videoStreamStartup.getUserEvent().getPressTime());
							}
						} else {
							continue;
						}

						duplicateHandling = videoPrefs.getDuplicateHandling();
						LOG.debug(String.format("Stream RQ:%10.3f", videoStream.getManifest().getRequestTime()));
						applyStartupDelayToStream(startupDelay, chosenEvent, videoStream, streamingVideoData);
						videoStream.setDuration(videoStream.getVideoEventMap().entrySet().stream()
								.filter(f -> f.getValue().isSelected() && f.getValue().isNormalSegment())
								.mapToDouble(x -> x.getValue().getDuration()).sum());
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
		eventMap.entrySet().stream().filter(f -> f.getValue().isSelected() && f.getValue().isNormalSegment())
				.forEach(v -> activeMap.put(v.getKey(), v.getValue()));
	}
	
	public void applyStartupDelayToStream(double startupTime, VideoEvent chosenVideoEvent, VideoStream videoStream, StreamingVideoData streamingVideoData) {

		if (chosenVideoEvent == null && (chosenVideoEvent = videoStream.getFirstSegment()) != null) {
			chosenVideoEvent.setSelected(true);
			videoStream.getManifest().setStartupVideoEvent(chosenVideoEvent);
		}
		if (chosenVideoEvent != null) {
			propagatePlaytime(startupTime, chosenVideoEvent, videoStream);

			chosenVideoEvent.setPlayTime(startupTime);
			videoStream.setVideoPlayBackTime(startupTime);

			videoStream.getManifest().setDelay(startupTime - chosenVideoEvent.getEndTS());
			videoStream.getManifest().setStartupVideoEvent(chosenVideoEvent);
			videoStream.getManifest().setStartupDelay(chosenVideoEvent.getSegmentStartTime() - videoStream.getManifest().getRequestTime());
		}
		streamingVideoData.scanVideoStreams();
		
		for (VideoStream stream : streamingVideoData.getVideoStreamMap().values()) {
			if (stream.equals(videoStream)) {
				stream.setSelected(true);
				stream.setCurrentStream(true);
			} else {
				stream.setSelected(false);
				stream.setCurrentStream(false);
			}
		}
	}

	/**
	 * not quite there yet
	 *
	 * for each segment: playtime = SegmentStartTime + startupOffset + stallOffset
	 *
	 * if playtime is before segment has arrived need to increment stallOffset to
	 * bring playtime up to arrival time plus all overhead of recovery from stall
	 *
	 * @param startupTime
	 * @param chosenVideoEvent
	 * @param videoStream
	 */
	public void propagatePlaytime(double startupTime, VideoEvent chosenVideoEvent, VideoStream videoStream) {

		double startupOffset;
		startupOffset = chosenVideoEvent != null ? startupTime - chosenVideoEvent.getSegmentStartTime() : 0;
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
			if (videoEvent.isNormalSegment()) {
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
					if (isAudio && (audioStall = syncWithAudio(startupOffset, videoStream, audioStreamMap, videoEvent)) != null
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
		generatePlaytimeData(videoStream);

		LOG.debug(videoStream.getByteToolTipDetailMap().keySet());
	}

	private ArrayList<VideoEvent> sortBySegThenStartTS(VideoStream videoStream) {
		ArrayList<VideoEvent> videoStreamFiltered = new ArrayList<VideoEvent>(videoStream.getVideoEventMap().values());

		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_ID));
		Collections.sort(videoStreamFiltered, new VideoEventComparator(SortSelection.SEGMENT_START_TS));
		return videoStreamFiltered;
	}

	/**
	 * Creates and stores Playtime chart points and tooltip data directly into
	 * videoStream not meant to create any object data to be used locally in this
	 * class
	 *
	 * @param videoStream
	 */
	private void generatePlaytimeData(VideoStream videoStream) {
		new PlayTimeData(videoStream);
	}

	/**
	 * Scans VideoStream to produce/populate - VideoStream.byteBufferList -
	 * VideoStream.toolTipDetailMap
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

		videoStream.getVideoEventMap().entrySet().stream().filter((f) -> !f.getValue().isFailedRequest()).forEach(e -> {
			mergedMap.put(e.getKey(), e.getValue());
		});
		videoStream.getAudioEventMap().entrySet().stream().filter((f) -> !f.getValue().isFailedRequest()).forEach(e -> {
			mergedMap.put(e.getKey(), e.getValue());
		});

		byteBufferList = videoStream.getByteBufferList();
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
		this.videoStream.addByteToolTipPoint(event, buffer);
		this.videoStream.addByteToolTipPoint(event, buffer2);
		return buffer2;
	}

	private void applyDuplicateHandlingRules(SortedMap<String, VideoEvent> eventStreamMap) {
		VideoEvent priorEvent = null;
		ArrayList<VideoEvent> videoStreamSorted = new ArrayList<VideoEvent>(eventStreamMap.values());
		Collections.sort(videoStreamSorted, new VideoEventComparator(SortSelection.END_TS));
		Collections.sort(videoStreamSorted, new VideoEventComparator(SortSelection.SEGMENT_ID));

		for (VideoEvent event : videoStreamSorted) {
			if (event.isNormalSegment()) {
				event.setSelected(true); // set to 'selected' so applyRules can judge
				applyRules(event, priorEvent);
				if (event.isSelected()) {
					priorEvent = event;
				}
			} else {
				LOG.debug("reject:" + event);
			}
		}
	}

	/**
	 * Compare event and priorEvent against duplicateHandling rules. When a
	 * VideoEvent has a competing segmentID, the two VideoEvents will be compared
	 * with the "loser" being deselected
	 *
	 * @param event
	 * @param priorEvent
	 */
	private void applyRules(VideoEvent event, VideoEvent priorEvent) {
		boolean tempFlag = false;
		if (event.isNormalSegment() && priorEvent != null) {
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

	/**
	 * <pre>
	 * Scan through all audio event related to videoEvent Starting with audio event
	 * from before the videoEvent Record all audio segments associated with Video
	 * segment. Including partial overlaps, often audio and video segments do not
	 * start at the same time.
	 *
	 * @param startupOffset
	 *
	 * @param videoStream      contains collections of Video, Audio and Captioning
	 * @param audioStreamMap   contains all audio in videoStream (when non-muxed)
	 *                         <key definition: segmentStartTime, endTS(in
	 *                         milliseconds)>
	 * @param videoEvent       The video segment to receive audio linkage
	 * @param appliedStallTime
	 * @return audioEvent associated with a stall
	 */
	private VideoEvent syncWithAudio(double startupOffset, VideoStream videoStream, TreeMap<String, VideoEvent> audioStreamMap, VideoEvent videoEvent) {
		VideoEvent audioEvent = null;
		String segmentStartTime = VideoStream.generateTimestampKey(videoEvent.getSegmentStartTime());
		String segmentEndTime = VideoStream.generateTimestampKey(videoEvent.getSegmentStartTime() + videoEvent.getDuration());

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
	 *
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
