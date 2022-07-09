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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.video.pojo;

import com.att.aro.core.videoanalysis.pojo.MediaType;

import lombok.Data;

@Data
public class Segment {

	private Track track;
	private VideoManifest manifest;
	private MediaType mediaType = MediaType.UNKNOWN;
	
	private int size;
	private double requestKey;
	private int trackNumber;
	private int segmentIndex;
	private int requestNumber;
	private int candidateNumber;
	private double startPlayTime;
	private double endPlayTime;
	
	public Segment(VideoManifest manifest, Track track, int segmentIndex, int size, double requestKey, int requestNumber, int candidateNumber) {
		
		super();
		this.size = size;
		this.track = track;
		this.manifest = manifest;
		this.requestKey = requestKey;
		this.segmentIndex = segmentIndex;
		this.requestNumber = requestNumber;
		this.candidateNumber = candidateNumber;
		if (track != null) {
			this.mediaType = track.getMediaType();
			this.trackNumber = track.getTrackNumber();
			this.startPlayTime = track.getSegmentDurations().stream().limit(segmentIndex - 1).reduce(0.0, Double::sum);
			this.endPlayTime = startPlayTime + ((segmentIndex <= track.getSegmentDurations().size()) ? track.getSegmentDurations().get(segmentIndex - 1):0);
		} else {
			this.trackNumber = -1;
		}
	}
	
	public String toString() {
		return candidateNumber + "_" + requestNumber + "_" + mediaType.toString() + "_" + trackNumber + "_" + segmentIndex;
	}

}
