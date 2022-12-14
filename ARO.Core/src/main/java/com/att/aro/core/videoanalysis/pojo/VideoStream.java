/*  Copyright 2019 AT&T
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
package com.att.aro.core.videoanalysis.pojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup;
import com.att.aro.core.tracemetadata.pojo.MetaStream;
import com.att.aro.core.videoanalysis.XYPair;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**<pre>
 *
 * VideoStream - holds everything for a captured streaming video
 *  ⁃ Manifest
 *  ⁃ VideoSegments
 *
 *
 */
@Data
public class VideoStream {
	private static final Logger LOG = LogManager.getLogger(VideoStream.class.getName());

	public static enum StreamStatus{
		Load, Play, Stall, NA
	}
	
	private Manifest manifest;
	
	/**
	 * VideoSegments that are considered as playing
	 * 
	 * Normal segments only from videoStartTimeMap<br>
	 * key definition format sssssssssstttttttt, t = getSegmentStartTime() len = 11, s = segment len = 10
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private TreeMap<String, VideoEvent> videoActiveMap = new TreeMap<>();

	/**
	 * AudioSegments that are considered as playing
	 * 
	 * Normal segments only from audioStartTimeMap<br>
	 * key definition format sssssssssstttttttt, t = getSegmentStartTime() len = 11, s = segment len = 10
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private TreeMap<String, VideoEvent> audioActiveMap = new TreeMap<>();

	/**
	 * CC Segments that are considered as playing
	 * 
	 * Normal segments only from ccEventMap<br>
	 * key definition format sssssssssstttttttt, t = getEndTS() len = 11, s = segment len = 10
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private TreeMap<String, VideoEvent> ccActiveMap = new TreeMap<>();

	/** <pre>
	 * key definition DLtimestamp-segment
	 * value VideoEvent
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<String, VideoEvent> videoEventMap = new TreeMap<>();

	/** <pre>
	 * key definition format sssssssssstttttttt, t = timestamp len = 11, s = segment len = 10
	 *   
	 * value VideoEvent
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private TreeMap<String, VideoEvent> videoStartTimeMap = new TreeMap<>();

	public VideoEvent getStartingSegment() {
		return videoStartTimeMap.isEmpty() ? null : videoStartTimeMap.firstEntry().getValue();
	}
	
	VideoStreamStartup videoStreamStartup;
	
	/** <pre>
	 * key definition DLtimestamp-segment
	 * value VideoEvent
	 */	
	@NonNull@Setter(AccessLevel.NONE)
	private TreeMap<String, VideoEvent> audioEventMap = new TreeMap<>();
	
	/** <pre>
	 * key definition: segmentStartTime, endTS(in milliseconds)
	 * value VideoEvent
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private TreeMap<String, VideoEvent> audioStartTimeMap = new TreeMap<>();
	
	/** <pre>
	 * key definition DLtimestamp-segment
	 * value VideoEvent
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<String, VideoEvent> ccEventMap = new TreeMap<>();

	/** <pre>
	 * key definition segment-quality-timestamp
	 * value VideoEvent
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<String, VideoEvent> videoSegmentEventList = new TreeMap<>();
	
	/** <pre>
	 * key definition segment-quality-timestamp
	 * value VideoEvent
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private TreeMap<String, VideoEvent> audioSegmentEventList = new TreeMap<>();
	
	/** <pre>
	 * key definition segment-quality-timestamp
	 * value VideoEvent
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<String, VideoEvent> ccSegmentEventList = new TreeMap<>();
	
	@NonNull@Setter(AccessLevel.NONE)
	private List<VideoEvent> allSegments = new ArrayList<>();
	
	@NonNull@Setter(AccessLevel.NONE)
	private List<VideoStall> videoStallList = new ArrayList<>();
	
	private Boolean validatedCount = false;
	private int segmentCount = 0;
	private double duration = 0;
	private int selectedManifestCount = 0;
	private int validSegmentCount = 0;
	private int nonValidSegmentCount = 0;
	private int invalidManifestCount = 0;
	private int missingSegmentCount = 0;
	
	private boolean valid = true;
	private boolean selected = false;		// selected for graphic display in diagnostics tab
	private boolean activeState = false;
	private boolean currentStream = false;	// selected in  toggleStream(VideoStream stream, boolean isCurrentStream)

	public VideoEvent audioEvent;
	
	private int boIndex = 0;
	private int ptIndex = 0;
	
	/**
	 * Used in Byte Buffer BufferOccupancyPlot.java
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private ArrayList<XYPair> byteBufferList = new ArrayList<>();
	
	/**
	 * Used in Byte Buffer BufferOccupancyPlot.java
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<Integer, ToolTipDetail> byteToolTipDetailMap = new TreeMap<>();

	/**
	 *  playtime aka:time buffer, bufferInSeconds
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private ArrayList<XYPair> playTimeList = new ArrayList<>();
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<Integer, ToolTipDetail> playTimeToolTipDetailMap = new TreeMap<>();

	private Double playRequestedTime;
	private Double videoPlayBackTime;

	@Getter
	@Setter
	private MetaStream metaStream;
	
	public void clearBufferOccupancyData() {
		byteToolTipDetailMap.clear();
		byteBufferList.clear();
		boIndex = 0;
	}
	
	public void clearPlayTimeData() {
		playTimeToolTipDetailMap.clear();
		playTimeList.clear();
		ptIndex = 0;
	}
	
	public void addByteToolTipPoint(VideoEvent videoEvent, double totalBytes) {
		if (!byteToolTipDetailMap.containsKey(boIndex)) {
			byteToolTipDetailMap.put(boIndex++, new ToolTipDetail(boIndex, totalBytes, videoEvent));
		}
	}
	
	public void addPlayTimeToolTipPoint(VideoEvent videoEvent, double currentTotalSeconds, StreamStatus streamStatus) {
		if (!playTimeToolTipDetailMap.containsKey(ptIndex)) {
			playTimeToolTipDetailMap.put(
					ptIndex++
					, new ToolTipDetail(ptIndex, currentTotalSeconds, videoEvent, streamStatus));
		}
	}
	
	public void addStall(VideoEvent videoEvent) {
		videoStallList.add(new VideoStall(videoEvent));
	}
	
	@Setter
	@Getter
	@AllArgsConstructor
	public class ToolTipDetail{
		
		int index;
		/**
		 * This is current total buffer, as in totalBytes or totalSeconds
		 */
		private double currentTotal;
		private VideoEvent videoEvent;
		private StreamStatus streamStatus;
		
		public ToolTipDetail(int index, double currentTotal, VideoEvent videoEvent) {
			this(index, currentTotal, videoEvent,  StreamStatus.NA);
		}
		
		public double getTS() {
			return videoEvent.getEndTS();
		}
		public double getSize() {
			return videoEvent.getSize();
		}

		public double getSegmentID() {
			return videoEvent.getSegmentID();
		}

		public double getPlayTime() {
			return videoEvent.getPlayTime();
		}

		public double getPlayTimeEnd() {
			return videoEvent.getPlayTimeEnd();
		}
		
		@Override
		public String toString() {
				StringBuilder strblr = new StringBuilder(83);
				strblr.append("index:").append(index);
				strblr.append(String.format(", currentTotal: %.0f", getCurrentTotal()));
				strblr.append(String.format(", endTS: %.3f", getTS()));

				strblr.append(String.format(", SegID: %.0f", videoEvent.getSegmentID()));
				strblr.append(String.format(", pTime: %.6f", videoEvent.getPlayTime()));
				if (videoEvent.getStallTime() > 0) {
					strblr.append(String.format(", sTS: %.6f", videoEvent.getPlayTime() - videoEvent.getStallTime()));
				}
				strblr.append("\n");
			return strblr.toString();
		}
	}

	public VideoEvent getFirstActiveSegment() {
		return getFirstNormalSegment(videoActiveMap);
	}
	
	public VideoEvent getFirstSegment() {
		return getFirstNormalSegment((TreeMap<String, VideoEvent>)videoSegmentEventList);
	}

	private VideoEvent getFirstNormalSegment(TreeMap<String, VideoEvent> videoMap) {
		Optional<Entry<String, VideoEvent>> firstSegment = videoMap.entrySet().stream().filter(e -> e.getValue().isNormalSegment()).findFirst();
		return firstSegment.isPresent() ? firstSegment.get().getValue() : null;
	}

	/**
	 * Add VideoEvent to Video, Audio & CC. plus segmentEventList.
	 * 
	 * @param segment
	 * @param timestamp
	 * @param videoEvent
	 */
	public void addVideoEvent(VideoEvent videoEvent) {

		String keyDLtime = generateEventKey(videoEvent.getEndTS(), videoEvent.getSegmentID());
		String keyStartTime = generateEventKey(videoEvent.getSegmentStartTime(), videoEvent.getEndTS() * 1000);
		/* 
		 * Check for assignment problems.
		 * Duplicate segments do not get requested at the same time, and so the EndTS's for same segment #'s should not collide
		 * In other words plays request segments within a track. Track changes only occur when the player decides to change tracks.
		 * No need to "fix" things here, just log so it can get fixed at the source of problem.
		 */
		if (videoEventMap.containsKey(keyDLtime) || audioEventMap.containsKey(keyDLtime) || ccEventMap.containsKey(keyDLtime)) {
			LOG.debug("VideoEvent download TS collision" + videoEvent);
		}

		switch (videoEvent.getContentType()) {
		case VIDEO:
		case MUXED:
			videoEventMap.put(keyDLtime, videoEvent);
			videoStartTimeMap.put(keyStartTime, videoEvent);
			break;
		case AUDIO:
			audioEventMap.put(keyDLtime, videoEvent);
			audioStartTimeMap.put(keyStartTime, videoEvent);
			break;
		default:
			ccEventMap.put(keyDLtime, videoEvent);
			break;
		}
		this.selected = true;
		addSegmentEvent(videoEvent);
	}

	/**
	 * segment stored under a key{segment/quality/timestamp}
	 * @param segmentID
	 * @param timestamp
	 * @param videoEvent
	 */
	public void addSegmentEvent(VideoEvent videoEvent) {
		String key;
		if (videoEvent.getSegmentID() != -1) {
			key = generateVideoEventKey(videoEvent.getSegmentID(), videoEvent.getEndTS(), videoEvent.getQuality());
			switch (videoEvent.getContentType()) {
			case VIDEO:
			case MUXED:
				videoSegmentEventList.put(key, videoEvent);
				break;
			case AUDIO:
				audioSegmentEventList.put(key, videoEvent);
				break;
			default:
				ccSegmentEventList.put(key, videoEvent);
				break;
			}
			allSegments.add(videoEvent);
		}
	}

	/**
	 * <pre>Generates a key
	 *	 s = segment len = 8
	 *	 t = timestamp len = 11
	 *	 Q = quality variable length
	 *   format ssssssssQualitytttttt.tttt
	 *   
	 * @param segment
	 * @param timestamp
	 * @param quality
	 * @return
	 */
	public String generateVideoEventKey(double segment, double timestamp, String quality) {
		String key;
		key = String.format("%08.0f:%s:%010.4f", segment, quality, timestamp);
		return key;
	}

	public String generateVideoEventKey(VideoEvent event) {
		return generateVideoEventKey(event.getSegmentID(), event.getSegmentStartTime(), event.getQuality());
	}

	/**
	 * <pre>Generates a key
	 *	 t = timestamp len = 11
	 *	 s = segment len = 10
	 *   format sssssssssstttttttt
	 *   
	 * @param timestamp
	 * @param segment
	 * @return
	 */
	public static String generateEventKey(double timestamp, double segment) {
		return String.format("%010.4f:%08.0f", timestamp, segment );
	}
	
	public static String generateSegmentKey(double segment) {
		return String.format("%08.0f", segment );
	}
	
	/**
	 * <pre>Generates a key
	 *	 t = timestamp len = 11
	 *   format sssssssssstttttttt
	 *   
	 * @param timestamp
	 * @param quality
	 * @return
	 */
	public static String generateTimestampKey(double timestamp) {
		return String.format("%010.4f:", timestamp);
	}
	
	public static String generateEventKey(VideoEvent event) {
		return generateEventKey(event.getSegmentStartTime(), event.getSegmentID());
	}

	public double getSegmentCount() {
		return segmentCount > 0 ? segmentCount : videoSegmentEventList.size();
	}
	
	/**
	 * Locates the first match with the segmentID by locating the next entry, which will contain quality
	 * 
	 * @param segID
	 * @return 
	 * @return a VideoEvent
	 */
	public VideoEvent getVideoEventBySegment(double segID) {
		String key = generateSegmentKey(segID);
		Entry<String, VideoEvent> nextEntry = ((TreeMap<String, VideoEvent>) videoSegmentEventList).higherEntry(key);
		return nextEntry != null ? nextEntry.getValue() : null;
	}

	public VideoEvent getVideoEventBySegment(String key) {
		return videoSegmentEventList.get(key);
	}  

	/**
	 * 
	 * Retrieve VideoEvents by segment
	 * 
	 * @return
	 * 
	 */
	public Collection<VideoEvent> getVideoEventsBySegment() {	
		return videoSegmentEventList.values();
	}
	
	public void applyStartupOffset(double startupOffset) {
		videoEventMap.entrySet().stream().forEach(x -> x.getValue().setStartupOffset(startupOffset));
		audioEventMap.entrySet().stream().forEach(x -> x.getValue().setStartupOffset(startupOffset));
		ccEventMap.   entrySet().stream().forEach(x -> x.getValue().setStartupOffset(startupOffset));
	}
	
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("VideoStream :");

		strblr.append("\n\t\t\tManifest              :").append(manifest);
		strblr.append("\n\t\t\tSegmentCount          :").append(segmentCount);
		strblr.append("\n\t\t\tSelectedManifestCount :").append(selectedManifestCount);
		strblr.append("\n\t\t\tValidSegmentCount     :").append(validSegmentCount);
		strblr.append("\n\t\t\tNonValidSegmentCount  :").append(nonValidSegmentCount);
		strblr.append("\n\t\t\tInvalidManifestCount  :").append(invalidManifestCount);
		strblr.append("\n\t\t\tValid                 :").append(valid);
		strblr.append("\n\t\t\tSelected              :").append(selected);
		strblr.append("\n\t\t\tActiveState           :").append(activeState);
		
		strblr.append("\n\t\t\tVideoEventList:").append(videoEventMap.size());
		if (videoEventMap.size() > 0) {
			Iterator<String> keys = videoEventMap.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				strblr.append("\n\t\t\t\t<").append(key + ">: " + videoEventMap.get(key));
			}
		}
		strblr.append("\n");
		return strblr.toString();
	}
}