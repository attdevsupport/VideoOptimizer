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
package com.att.aro.core.videoanalysis.pojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.impl.SortSelection;
import com.att.aro.core.videoanalysis.impl.VideoEventComparator;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * <pre>
 * 
 *  >>into some other object(s) held here
 *   ⁃ chunksBySegment
 *   ⁃ chunkPlayTimeList
 *   ⁃ veManifestList (what is this)
 *   ⁃ filteredSegments
 *   ⁃ allSegments (what is this)
 *   ⁃ duplicateChunks
 *
 */
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreType
public class StreamingVideoData extends AbstractBestPracticeResult {

	private static final Logger LOG = LogManager.getLogger(StreamingVideoData.class.getName());
	
	@NonNull@Setter(AccessLevel.PROTECTED)
	private BestPracticeType							bestPracticeType = BestPracticeType.VIDEOUSAGE;
	@NonNull@Setter(AccessLevel.PROTECTED)
	private NavigableMap<Double, VideoStream>			videoStreamMap = new TreeMap<>();
	@NonNull
	private SortedMap<Double, HttpRequestResponseInfo>	requestMap = new TreeMap<>();
	@NonNull@Setter(AccessLevel.PROTECTED)
	private SortedMap<Double, String>					failedRequestMap = new TreeMap<>();
	@NonNull@Setter(AccessLevel.PROTECTED)
	private Map<Manifest, Double>						durationManifestMap = new HashMap<>();
	@NonNull@Setter(AccessLevel.PROTECTED)
	private Map<Manifest, Double>						timescaleManifestMap = new HashMap<>();
	@NonNull@Setter(AccessLevel.PROTECTED)
	private StreamingVideoCompiled						streamingVideoCompiled = new StreamingVideoCompiled();
	@Setter(AccessLevel.PROTECTED) 
	private String tracePath;
	private final String VIDEO_SEGMENTS = "video_segments";
	@Setter(AccessLevel.PROTECTED) 
	private String videoPath;
	
	public void clear() {
		videoStreamMap.clear();
		requestMap.clear();
		failedRequestMap.clear();
		durationManifestMap.clear();
		timescaleManifestMap.clear();
		streamingVideoCompiled.clear();
	}

	@NonNull
	private Boolean validatedCount = false;
	@Setter(AccessLevel.PROTECTED)
	private int totalSegmentCount = 0;
	@Setter(AccessLevel.PROTECTED)
	private int selectedManifestCount = 0;
	@Setter(AccessLevel.PROTECTED)
	private int validSegmentCount = 0;
	@Setter(AccessLevel.PROTECTED)
	private int nonValidSegmentCount = 0;
	@Setter(AccessLevel.PROTECTED)
	private int invalidManifestCount = 0;

	@Getter
	@Setter
	private boolean finished = true;
	
	/**
	 * handy debugging info
	 */
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("StreamingVideoData :");
		strblr.append(tracePath);
		strblr.append("\nvideoStreamMap size: " + videoStreamMap.size());
		if (!videoStreamMap.isEmpty()) {
			strblr.append(":\n\t");
			for (VideoStream videoStream : videoStreamMap.values()) {
				strblr.append(videoStream.getSegmentCount());
				strblr.append("\n\t");
			}
		}
		return strblr.toString();
	}

	/**<pre>
	 * Count manifest valid, selected, invalid
	 * Count segments valid, selected, invalid, total
	 */
	public void scanVideoStreams() {
		synchronized (videoStreamMap) {
			validatedCount = false;
			totalSegmentCount = 0;
			selectedManifestCount = 0;
			invalidManifestCount = 0;
			validSegmentCount = 0;
			nonValidSegmentCount = 0;

			for (VideoStream videoStream : videoStreamMap.values()) {
				checkMissing(videoStream);
				scanFixMUX(videoStream);
				if (videoStream.isValid()) {
					validSegmentCount += videoStream.getSegmentCount();
				} else {
					nonValidSegmentCount += videoStream.getSegmentCount();
					invalidManifestCount++;
				}
				if (videoStream.isSelected() && !videoStream.getVideoEventMap().isEmpty()) {
					selectedManifestCount++;
					totalSegmentCount += videoStream.getSegmentCount();
				}
			}
		}	
		validatedCount = true;
	}

	// VID-TODO This needs rethinking, Video segments can be MUXED, and at the same time audio can be requested
	private void scanFixMUX(VideoStream videoStream) {
		if (!CollectionUtils.isEmpty(videoStream.getAudioSegmentEventList())) {
			for (VideoEvent videoEvent : videoStream.getVideoEventsBySegment()) {
				if (videoEvent.getContentType().equals(ContentType.MUXED)) {
					videoEvent.getSegmentInfo().setContentType(ContentType.VIDEO);
				}
			}
		}
	}

	private void checkMissing(VideoStream videoStream) {
		videoStream.getVideoEventsBySegment();
		videoStream.getAudioSegmentEventList();
		videoStream.setMissingSegmentCount(Math.max(scanSegmentGaps(videoStream.getVideoStartTimeMap()) 
												  , scanSegmentGaps(videoStream.getAudioStartTimeMap())));
	}

	private int scanSegmentGaps(TreeMap<String, VideoEvent> eventMap) {
		VideoEvent lastEvent = null;
		double threshold = .01;
		
		int count = 0;
		ArrayList<VideoEvent> sorted = new ArrayList<>(eventMap.values());
		Collections.sort(sorted, new VideoEventComparator(SortSelection.SEGMENT_ID));
		Collections.sort(sorted, new VideoEventComparator(SortSelection.PLAY_TIME));
		for (VideoEvent event : sorted) { //eventMap.values()
			if (!event.isNormalSegment() || event.getPlayTime() == 0) {
				continue;
			}
			if (lastEvent != null && event.getSegmentID() != lastEvent.getSegmentID()) {
				double dif = event.getPlayTime() - lastEvent.getPlayTimeEnd();
				if (dif > threshold) {
					// found gap, could be one or many segments missing
					LOG.debug(String.format("GAP after %s, segment:%.0f", lastEvent.getContentType().toString(), lastEvent.getSegmentID()));
					count++;
				}
			}
			lastEvent = event;
		}
		return count;
	}

	public StreamingVideoData(String tracePath) {
		setTracePath(tracePath + Util.FILE_SEPARATOR);
		setVideoPath(getTracePath() + VIDEO_SEGMENTS + Util.FILE_SEPARATOR);
	}
	
	public void addVideoStream(double timeStamp, VideoStream videoStream) {
		if (videoStream != null) {
			videoStreamMap.put(timeStamp, videoStream);
		}
	}
	
	public Collection<VideoStream> getVideoStreams(){
		 return videoStreamMap.values();
	}
	
	/**
	 * 
	 * @param durationAROManifestMap
	 * @param timescaleAROManifestMap
	 */
	public void addDurationTimescale(Map<Manifest, Double> durationAROManifestMap, Map<Manifest, Double> timescaleAROManifestMap) {
		this.durationManifestMap = durationAROManifestMap;
		this.timescaleManifestMap = timescaleAROManifestMap;
	}

	/**
	 * 
	 * @param flag
	 * @param request
	 */
	public void addRequest(HttpRequestResponseInfo request) {
		this.requestMap.put(request.getTimeStamp(), request);
	}

	public void addFailedRequestMap(HttpRequestResponseInfo request) {
		this.failedRequestMap.put(request.getTimeStamp(), request.getObjNameWithoutParams() != null ? request.getObjNameWithoutParams() : "");
	}

	public VideoStream getVideoStream (double videoManifestTime) {	
		return videoStreamMap.get(videoManifestTime);
	}
}