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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.videoanalysis.impl.SortSelection;
import com.att.aro.core.videoanalysis.impl.VideoEventComparator;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.ManifestHLS;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;

public abstract class PlotHelperAbstract {

	private List<VideoEvent> chunkDownload;
	protected Map<VideoEvent, Double> chunkPlayTimeList = new TreeMap<>();

	@Autowired
	private IVideoUsagePrefsManager videoPrefManager;
	
	@InjectLogger
	private static ILogger logger;

	protected VideoUsage videoUsage;

	public VideoUsage getVideoUsage(){
		return videoUsage;
	}
	
	public void setVideoUsage(VideoUsage videoUsage){
		this.videoUsage = videoUsage;
	}

	public List<VideoEvent> filterVideoSegment(VideoUsage videoUsage) {
		this.videoUsage = videoUsage;
		Map<VideoEvent, AROManifest> veManifestList = new HashMap<>();
		chunkDownload = new ArrayList<>();
		List<VideoEvent> duplicateChunks = new ArrayList<>();
		List<VideoEvent> allSegments = new ArrayList<>();

		for (AROManifest aroManifest : videoUsage.getManifests()) {
			if (aroManifest != null) {
				// don't count if no videos with manifest, or only one video
				if (aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty() && aroManifest.getVideoEventList().size() > 1) {
					TreeMap<String, VideoEvent> segmentEventList = aroManifest.getSegmentEventList();
					Entry<String, VideoEvent> segmentValue = segmentEventList.higherEntry("00000000:z");
					double firstSeg = segmentValue != null ? segmentValue.getValue().getSegment() : 0;
					VideoEvent first = null;
					for (VideoEvent videoEvent : aroManifest.getVideoEventList().values()) {
						if (videoEvent.getSegment() == firstSeg) {
							first = videoEvent;
							break;
						}
					}

					for (VideoEvent videoEvent : aroManifest.getVideoEventList().values()) {
						if (!(videoEvent.getSegment() == 0 && aroManifest instanceof ManifestDash) && (!chunkDownload.contains(videoEvent))) {

							for (VideoEvent video : chunkDownload) {
								if ((videoEvent.getSegment() != firstSeg) && video.getSegment() == videoEvent.getSegment()) {
									duplicateChunks.add(video);
								}

								if (videoEvent.getSegment() == firstSeg) {
									if (!videoEvent.equals(first)) {
										duplicateChunks.add(videoEvent);
									}
								}
							}
							veManifestList.put(videoEvent, aroManifest);
							chunkDownload.add(videoEvent);
							allSegments.add(videoEvent);
						}
					}
				}
			}
		}

		for (VideoEvent ve : duplicateChunks) {
			veManifestList.keySet().remove(ve);
			chunkDownload.remove(ve);
		}
		
		videoUsage.setVideoEventManifestMap(veManifestList);
		videoUsage.setAllSegments(allSegments);
		videoUsage.setDuplicateChunks(duplicateChunks);
		return chunkDownload;
	}

	public List<VideoEvent> filterSegmentByVideoPref(VideoUsage videoUsage) {
		this.videoUsage = videoUsage;
		Map<VideoEvent, AROManifest> veManifestList = new HashMap<>();
		chunkDownload = new ArrayList<>();
		List<VideoEvent> allSegments = new ArrayList<>();
		videoUsage.setDuplicateChunks(null);
		DUPLICATE_HANDLING segmentFilterChoice = videoPrefManager.getVideoUsagePreference().getDuplicateHandling();

		for (AROManifest aroManifest : videoUsage.getManifests()) {
			// don't count if no videos with manifest, or only one video
			if (aroManifest != null && aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty() && aroManifest.getVideoEventList().size() > 1) {

				for (VideoEvent videoEvent : aroManifest.getVideoEventList().values()) {
					if (videoEvent != null 
							&& !(videoEvent.getSegment() == 0 && aroManifest.getVideoFormat().equals(VideoFormat.MPEG4))
						&& (!chunkDownload.contains(videoEvent))) {

						switch (segmentFilterChoice) {
						case FIRST:
							filteByFirst(chunkDownload, videoEvent);
							break;
						case LAST:
							filterByLast(chunkDownload, videoEvent);
							break;
						case HIGHEST:
							filterByHighest(chunkDownload, videoEvent);
							break;
						default:
						}

						veManifestList.put(videoEvent, aroManifest);
						chunkDownload.add(videoEvent);
						allSegments.add(videoEvent);
					}
				}
			}
		}

		if (segmentFilterChoice == DUPLICATE_HANDLING.FIRST || segmentFilterChoice == DUPLICATE_HANDLING.LAST) {
			for (VideoEvent ve : videoUsage.getDuplicateChunks()){
				veManifestList.keySet().remove(ve);
				chunkDownload.remove(ve);
			}
		}
		videoUsage.setVideoEventManifestMap(veManifestList);
		videoUsage.setAllSegments(allSegments);
		return chunkDownload;
	}

	private void filteByFirst(List<VideoEvent> chunkDownloadList, VideoEvent videoEvent) {
		for (VideoEvent video : chunkDownloadList) {
			if (video.getSegment() == videoEvent.getSegment()) {
				videoUsage.getDuplicateChunks().add(videoEvent); // Adding the segments that came in LAST to the remove list
			}
		}
	}
	
	private void filterByLast(List<VideoEvent> chunkDownloadList, VideoEvent videoEvent) {
		for (VideoEvent video : chunkDownloadList) {
			if (video.getSegment() == videoEvent.getSegment()) {
				videoUsage.getDuplicateChunks().add(video); // Adding the segments that came in FIRST to the remove list
			}
		}
	}

	private void filterByHighest(List<VideoEvent> chunkDownloadList, VideoEvent videoEvent){
		for (VideoEvent video : chunkDownloadList) {
			if (video.getSegment() == videoEvent.getSegment()) {
				try {
					Integer videoQuality      = video.getQuality().isEmpty()         || video.getQuality() == null      || video.getQuality().matches(".*[A-Za-z].*")        ? 0 : Integer.parseInt(video.getQuality());     
					Integer videoEventQuality = videoEvent.getQuality().isEmpty()    || videoEvent.getQuality() == null || videoEvent.getQuality().matches(".*[A-Za-z].*")   ? 0 : Integer.parseInt(videoEvent.getQuality());
					if (videoQuality.compareTo(videoEventQuality) < 0){
						videoUsage.getDuplicateChunks().add(video);

					} else {
						videoUsage.getDuplicateChunks().add(videoEvent);
					}
				} catch (NumberFormatException e) {
					StackTraceElement[] stack = e.getStackTrace();
					logger.error("NumberFormatException : " + e + " @ " + ((stack != null && stack.length > 0) ? stack[0] : ""));
				} catch (Exception e) {
					StackTraceElement[] stack = e.getStackTrace();
					logger.error("Exception : " + e + " @ " + ((stack != null && stack.length > 0) ? stack[0] : ""));
				}
			}
		}
	}
	
	public List<VideoEvent> filterVideoSegmentUpdated(VideoUsage videoUsage) {
		this.videoUsage = videoUsage;
		List<VideoEvent> filteredSegments = new ArrayList<>();
		filterSegmentByVideoPref(videoUsage);// filterVideoSegment(videoUsage);
		for (VideoEvent ve : chunkDownload) {
			filteredSegments.add(ve);
		}
		videoUsage.setFilteredSegments(filteredSegments);
		return chunkDownload;
	}

	public List<VideoEvent> videoEventListBySegment(VideoUsage videoUsage) {
		this.videoUsage = videoUsage;
		List<VideoEvent> chunksBySegment = new ArrayList<>();
		for (AROManifest aroManifest : videoUsage.getManifests()){
			if (aroManifest.isSelected() && !aroManifest.getVideoEventList().isEmpty()) {
				for (VideoEvent videoEvent : aroManifest.getVideoEventsBySegment()) {
					if ((videoEvent.getSegment() != 0)
							|| (videoEvent.getSegment() == 0 && (aroManifest instanceof ManifestHLS))) { 
						chunksBySegment.add(videoEvent);
					}
				}
			}
		}
		for (VideoEvent ve : videoUsage.getDuplicateChunks()) {
			chunksBySegment.remove(ve);
		}

		Collections.sort(chunksBySegment, new VideoEventComparator(SortSelection.SEGMENT));
		videoUsage.setChunksBySegmentNumber(chunksBySegment);
		return chunksBySegment;
	}

	public double getChunkPlayTimeDuration(VideoEvent ve) {
		Map<VideoEvent, AROManifest> veManifestList = videoUsage.getVideoEventManifestMap();
		double duration = ve.getDuration();
		if (duration == 0 && veManifestList != null) {
			for (VideoEvent vEvent : veManifestList.keySet()) {
				if (ve.equals(vEvent)) {
					AROManifest aroManifest = veManifestList.get(vEvent);
					duration = aroManifest.getDuration();
					double timescale = aroManifest.getTimeScale();
					if(timescale != 0){
						duration = duration / timescale;
					}
					return duration;
				}
			}
		}
		return duration;
	}

	public void setChunkPlayBackTimeList(Map<VideoEvent, Double> chunkPlayTimeList) {
		this.chunkPlayTimeList = chunkPlayTimeList;
	}

	public double getChunkPlayStartTime(VideoEvent chunkPlaying) {
		for (VideoEvent veEvent : chunkPlayTimeList.keySet()) {
			if (veEvent.getSegment() == chunkPlaying.getSegment()) { // veEvent.equals(chunkPlaying)){
				return chunkPlayTimeList.get(veEvent); // return play start time
			}
		}
		return -1;
	}
}
