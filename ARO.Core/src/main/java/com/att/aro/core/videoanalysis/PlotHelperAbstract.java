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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public abstract class  PlotHelperAbstract {

	private static Map<VideoEvent, AROManifest> veManifestList;
	private List<VideoEvent> chunkDownload;
	public static Map<VideoEvent, Double> chunkPlayTimeList = new TreeMap<>();
	protected static List<VideoEvent> chunksBySegment;
	protected static List<VideoEvent> removeChunks;
	public static List<VideoEvent> filteredSegments;
	
	public List<VideoEvent> filterVideoSegment(VideoUsage videoUsage) {

		veManifestList = new HashMap<>();
		chunkDownload = new ArrayList<>();
		
		removeChunks = new ArrayList<>();

		for (AROManifest aroManifest : videoUsage.getManifests()) {
			// don't count if no videos with manifest, or only one video
			if (!aroManifest.getVideoEventList().isEmpty() && aroManifest.getVideoEventList().size() > 1) { 

				TreeMap<String, VideoEvent> segmentEventList = aroManifest.getSegmentEventList();
				Entry<String, VideoEvent> segmentValue = segmentEventList.higherEntry("00000000:z");

				double firstSeg = 0;

				firstSeg = segmentValue.getValue().getSegment();
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
								removeChunks.add(video);
							}

							if (videoEvent.getSegment() == firstSeg) {
								if (!videoEvent.equals(first)) {
									removeChunks.add(videoEvent);
								}
							}

						}
						veManifestList.put(videoEvent, aroManifest);
						chunkDownload.add(videoEvent);

					}
				}
			}
		}
	
		for (VideoEvent ve : removeChunks) {
			veManifestList.keySet().remove(ve);
			chunkDownload.remove(ve);
		}
		return chunkDownload;
	}
	
	public List<VideoEvent> filterVideoSegmentUpdated(VideoUsage videoUsage) {
		filteredSegments = new ArrayList<>();
		filterVideoSegment(videoUsage);
		for (VideoEvent ve : chunkDownload) {			
			filteredSegments.add(ve);
		}
		
		return chunkDownload;
	}
	
	public List<VideoEvent> videoEventListBySegment(VideoUsage videoUsage){
		
		chunksBySegment = new ArrayList<>();
		//List<VideoEvent> removeChunks = new ArrayList<>();

		for(AROManifest aroManifest:videoUsage.getManifests()){
			if(!aroManifest.getVideoEventList().isEmpty()){
				for (VideoEvent videoEvent : aroManifest.getVideoEventsBySegment()) {				
					if(videoEvent.getSegment() !=0){ // && !chunksBySegment.contains(videoEvent)){
						/*for(VideoEvent ve:chunksBySegment){
								if(ve.getSegment()==videoEvent.getSegment()){
									removeChunks.add(ve);
								}
							}*/	
						chunksBySegment.add(videoEvent);
					}
				}			
			}
		}
		for (VideoEvent ve : removeChunks) {
			chunksBySegment.remove(ve);
		}
		
		return chunksBySegment;
	}
	
	protected List<VideoEvent> getChunksBySegmentNumber(){
		return chunksBySegment;
	}
	
	public List<VideoEvent> getFilteredSegments(){
		return filteredSegments;
	}
	
	public double getChunkPlayTimeDuration(VideoEvent ve) {
		double duration = ve.getDuration();
		if(duration == 0 && veManifestList != null){
			for(VideoEvent vEvent: veManifestList.keySet()){
				if(ve.equals(vEvent)){
					AROManifest aroManifest = veManifestList.get(vEvent);
					duration = aroManifest.getDuration();
					double timescale = aroManifest.getTimeScale();
					duration = duration/timescale;
					return duration;
				}
			}
		}
	
		return duration;
	}
	
	
	public void setChunkPlayBackTimeList(Map<VideoEvent, Double> chunkPlayTimeList) {
		PlotHelperAbstract.chunkPlayTimeList  = chunkPlayTimeList;
	}
	
	public double getChunkPlayStartTime(VideoEvent chunkPlaying){
		for(VideoEvent veEvent: chunkPlayTimeList.keySet()){
			if(veEvent.getSegment() == chunkPlaying.getSegment()){ //veEvent.equals(chunkPlaying)){
				return chunkPlayTimeList.get(veEvent); //return play start time
			}
		}
		return -1;
	}
	
	
	/*public List<VideoEvent> getFilteredChunks(){
		return filteredSegments;//chunkDownload;
	}*/
	
	public Map<VideoEvent, AROManifest> getVideoEventManifestMap(){
		return veManifestList;
	}

}
