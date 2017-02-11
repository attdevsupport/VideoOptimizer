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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

/**
 * 
 * VBP #- ,
 *
 */
public class VideoUsage extends AbstractBestPracticeResult {

	private TreeMap<Double, AROManifest> aroManifestMap = new TreeMap<>();
	
	private Map<AROManifest,Double> durationAROManifestMap = new HashMap<>();
	private Map<AROManifest,Double> timescaleAROManifestMap = new HashMap<>();
	
	private String tracePath;

	private VideoUsagePrefs videoUsagePrefs;

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
		aroManifestMap.put(timeStamp,aroManifest);
	}

	public AROManifest findVideoInManifest(String videoName){
		
		for(AROManifest aroManifest:getAroManifestMap().values()){
			if (videoName.contains(aroManifest.getVideoName())) {
				return aroManifest;
			}
		}
		
		return null;
	}
	
	/**
	 * <pre>
	 * Returns a Map of AroManifest objects keyed by timestamps
	 * 
	 * Use getAroManifestMap() instead
	 * Better yet, use getManifests() for a List<AROManifest>
	 * 
	 * @return Map of AroManifest
	 */
	@Deprecated
	public TreeMap<Double, AROManifest> getVideoEventList() {
		return aroManifestMap;
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
		// TODO Auto-generated method stub
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
	
	
}
