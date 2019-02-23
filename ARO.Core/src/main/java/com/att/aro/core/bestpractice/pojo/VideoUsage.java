/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.bestpractice.pojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
public class VideoUsage extends AbstractBestPracticeResult {
	private TreeMap<Double, AROManifest> aroManifestMap = new TreeMap<>();
	private TreeMap<Double, HttpRequestResponseInfo> requestMap = new TreeMap<>();
	private TreeMap<Double, String> failedRequestMap = new TreeMap<>();
	private Map<AROManifest,Double> durationAROManifestMap = new HashMap<>();
	private Map<AROManifest,Double> timescaleAROManifestMap = new HashMap<>();
	private String tracePath;
	private VideoUsagePrefs videoUsagePrefs;
	private Map<VideoEvent, Double> chunkPlayTimeList = new TreeMap<>();
	private  Map<AROManifest,VideoEvent> firstSelectedSegment = new HashMap<>();
	@Nonnull
	private List<VideoEvent> chunksBySegment = new ArrayList<>();
	@Nonnull
	private Map<VideoEvent, AROManifest> veManifestList = new HashMap<>();
	@Nonnull
	private List<VideoEvent> filteredSegments = new ArrayList<>();
	@Nonnull
	private List<VideoEvent> allSegments = new ArrayList<>();
	@Nonnull
	private List<VideoEvent> duplicateChunks = new ArrayList<>();
	
	private Boolean validatedCount = false;
	private int segmentCount = 0;
	private int selectedManifestCount = 0;
	private int validSegmentCount = 0;
	private int nonValidSegmentCount = 0;
	private int invalidManifestCount = 0;

	public void setValidatedCount(Boolean validatedCount) {
		this.validatedCount = validatedCount;
	}

	public boolean isValidatedCount(){
		return validatedCount;
	}
	
	public int getInvalidManifestCount() {
		return invalidManifestCount;
	}

	public int getSegmentCount() {
		return segmentCount;
	}

	public int getSelectedManifestCount() {
		return selectedManifestCount;
	}

	public int getValidSegmentCount() {
		return validSegmentCount;
	}

	public int getNonValidSegmentCount() {
		return nonValidSegmentCount;
	}

	public void scanManifests() {
		synchronized (aroManifestMap) {
			validatedCount = false;
			segmentCount = 0;
			selectedManifestCount = 0;
			invalidManifestCount = 0;
			validSegmentCount = 0;
			nonValidSegmentCount = 0;

			for (AROManifest aroManifest : aroManifestMap.values()) {
				if (aroManifest.isValid()) {
					validSegmentCount += aroManifest.getSegmentCount();
				} else {
					nonValidSegmentCount += aroManifest.getSegmentCount();
					invalidManifestCount++;
				}
				if (aroManifest.isSelected()) {
					selectedManifestCount++;
					segmentCount += aroManifest.getSegmentCount();
				}
			}
		}
		validatedCount = true;
	}

	public List<VideoEvent> getChunksBySegmentNumber() {
		return chunksBySegment;
	}
	
	public List<VideoEvent> getFilteredSegments() {
		return filteredSegments;
	}

	public void setFilteredSegments(List<VideoEvent> filteredSegments) {
		if(filteredSegments == null){
			this.filteredSegments.clear();
		}else{
			this.filteredSegments = filteredSegments;
		}
	}

	public void setChunksBySegmentNumber(List<VideoEvent> chunksBySegment) {
		if(chunksBySegment == null){
			this.chunksBySegment.clear();
		}else{
			this.chunksBySegment = chunksBySegment;
		}
	}
	
	public Map<VideoEvent, Double> getChunkPlayTimeList() {
		return chunkPlayTimeList;
	}

	public void setChunkPlayTimeList(Map<VideoEvent, Double> chunkPlayTimeList) {
		this.chunkPlayTimeList = chunkPlayTimeList;
	}

	public Map<AROManifest,VideoEvent> getFirstSelectedSegment() {
		return firstSelectedSegment;
	}

	public void setFirstSelectedSegment(Map<AROManifest,VideoEvent> firstSelectedSegment) {
		this.firstSelectedSegment = firstSelectedSegment;
	}
	
	public VideoUsage(String tracePath) {
		this.tracePath = tracePath;
	}
	
	/**
	 * handy debugging info
	 */
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("VideoUsage :");
		strblr.append(tracePath);

		if (!aroManifestMap.isEmpty()) {
			strblr.append(":\n\t");
			for (AROManifest aroManifest : aroManifestMap.values()) {
				strblr.append(aroManifest);
				strblr.append("\n\t");
			}
		}
		return strblr.toString();
	}

	/**
	 * Returns a Map of AroManifest objects keyed by timestamps
	 * @return Map of AroManifest
	 */
	public TreeMap<Double, AROManifest> getAroManifestMap() {
		return aroManifestMap;
	}

	public void add(double timeStamp, AROManifest aroManifest) {
		if (aroManifest != null) {
			aroManifestMap.put(timeStamp, aroManifest);
		}
	}

	/**
	 * Find Manifest with same name and is active
	 * @param videoName
	 * @return
	 */
	public AROManifest findVideoInManifest(String videoName) {
		for (AROManifest manifest : getAroManifestMap().values()) {
			// problem here when seg from an old video comes in, hmmm??
			if (manifest.isActiveState()) {
				if (videoName.replaceAll("\\.", "_").equals(manifest.getVideoName())) {
					return manifest;
				}
			}
		}

		return null;
	}
	
	public Collection<AROManifest> getManifests(){
		 return aroManifestMap.values();
	}
	
	public String getTracePath() {
		return tracePath;
	}

	public void setTracePath(String tracePath) {
		this.tracePath = tracePath;
	}

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.VIDEOUSAGE;
	}

	/**
	 * 
	 * @param durationAROManifestMap
	 * @param timescaleAROManifestMap
	 */
	public void addDurationTimescale(Map<AROManifest, Double> durationAROManifestMap, Map<AROManifest, Double> timescaleAROManifestMap) {
		this.durationAROManifestMap = durationAROManifestMap;
		this.timescaleAROManifestMap = timescaleAROManifestMap;
	}

	public Map<AROManifest, Double> getDurationAROManifestMap(){
		return this.durationAROManifestMap;
	}

	public Map<AROManifest, Double> getTimeScaleAROManifestMap(){
		return this.timescaleAROManifestMap;
	}

	public VideoUsagePrefs getVideoUsagePrefs() {
		return videoUsagePrefs;
	}

	public void setVideoUsagePrefs(VideoUsagePrefs videoUsagePrefs) {
		this.videoUsagePrefs = videoUsagePrefs;
	}

	public TreeMap<Double, HttpRequestResponseInfo> getRequestMap() {
		return requestMap;
	}

	public void setRequestMap(TreeMap<Double, HttpRequestResponseInfo> requestMap) {
		this.requestMap = requestMap;
	}
	
	/**
	 * 
	 * @param flag
	 * @param request
	 */
	public void addRequest(HttpRequestResponseInfo request) {
		this.requestMap.put(request.getTimeStamp(), request);
	}

	public TreeMap<Double, String> getFailedRequestMap() {
		return failedRequestMap;
	}

	public void addFailedRequestMap(HttpRequestResponseInfo request) {
		this.failedRequestMap.put(request.getTimeStamp(), request.getObjUri().toString());
	}

	public Map<VideoEvent, AROManifest> getVideoEventManifestMap() {
		return veManifestList;
	}

	public void setVideoEventManifestMap(Map<VideoEvent, AROManifest> veManifestList) {
		if(null == veManifestList){
			this.veManifestList.clear();
		}else{
			this.veManifestList = veManifestList;
		}
	}

	public List<VideoEvent> getAllSegments() {
		return allSegments;
	}

	public void setAllSegments(List<VideoEvent> allSegments) {
		if(null == allSegments){
			this.allSegments.clear();
		}else{
			this.allSegments = allSegments;
		}
	}

	public List<VideoEvent> getDuplicateChunks() {
		return duplicateChunks;
	}

	public void setDuplicateChunks(List<VideoEvent> duplicateChunks) {
		if(null == duplicateChunks){
			this.duplicateChunks.clear();
		}else{
			this.duplicateChunks = duplicateChunks;
		}
	}
	
}
