/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.video.pojo;

import lombok.Data;

import java.util.List;

import com.att.aro.core.videoanalysis.pojo.MediaType;

@Data
public class VideoManifest {
	
	private List<Track> tracks;
	private Track audioTrack;
	private double medianAudioTrackSize;
	private int maxVideoSegmentIndexSize;
	
	public void setTracks(List<Track> tracks) {
		
		int trackNumber = 0;
		this.tracks = tracks;
		for (Track track : this.tracks) {
			
			maxVideoSegmentIndexSize = (track.getMaxSegmentIndex() > maxVideoSegmentIndexSize)? track.getMaxSegmentIndex() : maxVideoSegmentIndexSize;
			
			if (track.getMediaType() == MediaType.VIDEO) {
				track.setTrackNumber(trackNumber++);
			}
			
			if (track.getMediaType() == MediaType.AUDIO && medianAudioTrackSize == 0) {
				audioTrack = track;
				List<Integer> segmentSizes = track.getSegmentSizes();
				int listSize = segmentSizes.size();
				medianAudioTrackSize = segmentSizes.stream().mapToInt(s -> s).sorted().skip((listSize-1)/2).limit(2-listSize%2).average().orElse(0.0);
			}
		}
	}
}
