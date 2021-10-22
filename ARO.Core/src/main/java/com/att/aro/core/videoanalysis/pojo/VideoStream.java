package com.att.aro.core.videoanalysis.pojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.videoanalysis.XYPair;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
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
	private SortedMap<String, VideoEvent> audioActiveMap = new TreeMap<>();

	/**
	 * CC Segments that are considered as playing
	 * 
	 * Normal segments only from ccEventMap<br>
	 * key definition format sssssssssstttttttt, t = getEndTS() len = 11, s = segment len = 10
	 */
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<String, VideoEvent> ccActiveMap = new TreeMap<>();

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
	
	public void addStall(VideoEvent videoEvent) {
		videoStallList.add(new VideoStall(videoEvent));
	}
	
	private Boolean validatedCount = false;
	private int segmentCount = 0;
	private double duration = 0;
	private int selectedManifestCount = 0;
	private int validSegmentCount = 0;
	private int nonValidSegmentCount = 0;
	private int invalidManifestCount = 0;
	private int missingSegmentCount = 0;
	
	private boolean valid = true;
	private boolean selected = false;
	private boolean activeState = false;
	private boolean currentStream = false;

	public VideoEvent audioEvent;
	
	private int boIndex = 0;
	private int ptIndex = 0;
	
	// byte buffer
	@NonNull@Setter(AccessLevel.NONE)
	private ArrayList<XYPair> byteBufferList = new ArrayList<>();
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<Integer, ToolTipDetail> toolTipDetailMap = new TreeMap<>();

	// playtime aka:time buffer, bufferInSeconds
	@NonNull@Setter(AccessLevel.NONE)
	private ArrayList<XYPair> playTimeList = new ArrayList<>();
	@NonNull@Setter(AccessLevel.NONE)
	private SortedMap<Integer, ToolTipDetail> playTimeToolTipDetailMap = new TreeMap<>();

	private Double playRequestedTime;
	private Double videoPlayBackTime;
	
	public void clearBufferOccupancyData() {
		toolTipDetailMap.clear();
		byteBufferList.clear();
		boIndex = 0;
	}
	
	public void clearPlayTimeData() {
		playTimeToolTipDetailMap.clear();
		playTimeList.clear();
		ptIndex = 0;
	}
	
	public void addToolTipPoint(VideoEvent videoEvent, double totalBytes) {
		if (!toolTipDetailMap.containsKey(boIndex)) {
			toolTipDetailMap.put(boIndex++, new ToolTipDetail(boIndex, totalBytes, videoEvent));
		}
	}
	
	public void addPlayTimeToolTipPoint(VideoEvent videoEvent, double totalBytes) {
		if (!playTimeToolTipDetailMap.containsKey(boIndex)) {
			playTimeToolTipDetailMap.put(boIndex++, new ToolTipDetail(boIndex, totalBytes, videoEvent));
		}
	}
	
	@Data
	@AllArgsConstructor
	public class ToolTipDetail{
		int index;
		double totalBytes;
		VideoEvent videoEvent;
		
		public double getTS() {
			return videoEvent.getEndTS();
		}
		public double getSize() {
			return videoEvent.getSize();
		}
		
		@Override
		public String toString() {
				StringBuilder strblr = new StringBuilder(83);
				strblr.append("index:").append(index);
				strblr.append(String.format(", totalBytes: %.0f", getTotalBytes()));
				strblr.append(String.format(", endTS: %.3f", getTS()));
				strblr.append(String.format(", bytes: %.0f", getSize()));
				strblr.append("\n");
			return strblr.toString();
		}
	}

	public VideoEvent getFirstSegment() {
		return ((TreeMap<String, VideoEvent>)videoSegmentEventList).firstEntry().getValue();
	}

	/**
	 * Add VideoEvent to both  and segmentEventList.
	 * 
	 * @param segment
	 * @param timestamp
	 * @param videoEvent
	 */
	public void addVideoEvent(VideoEvent videoEvent) {
		String keyDLtime = generateEventKey(videoEvent.getEndTS(), videoEvent.getSegmentID());
		String keyStartTime = generateEventKey(videoEvent.getSegmentStartTime(), videoEvent.getEndTS() * 1000);
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

	public void setPlayRequestedTime(Double playRequestedTime) {
		this.playRequestedTime=playRequestedTime;
	}


}