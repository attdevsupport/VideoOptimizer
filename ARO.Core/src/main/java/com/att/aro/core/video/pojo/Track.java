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

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import com.att.aro.core.videoanalysis.pojo.MediaType;

@Data
public class Track {

	private int trackNumber;
	private String trackName;
	private String manifest;
	private MediaType mediaType;
	private float mediaBandwidth;
	private int maxSegmentIndex = 0;
	@Setter(AccessLevel.NONE)
	private List<Integer> segmentSizes;
	@Setter(AccessLevel.NONE)
	private List<Double> segmentDurations;
	
	public void setSegmentSizes(List<Integer> segmentSizes) {
		this.maxSegmentIndex = segmentSizes.get(0);
		segmentSizes.remove(0);
		this.segmentSizes = segmentSizes;
	}
	
	public void setSegmentDurations(List<Double> segmentDurations) {
		this.segmentDurations = new ArrayList<>();
		segmentDurations.remove(0);
		this.segmentDurations = segmentDurations;
	}
}