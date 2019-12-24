
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

package com.att.aro.core.videoanalysis.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class StreamingVideoCompiled {

	private SortedMap<VideoEvent, Double> chunkPlayTimeList = new TreeMap<>();

	/**
	 * All video segments including the duplicate chunks
	 */
	@Nonnull
	private List<VideoEvent> allSegments = new ArrayList<>();
	@Nonnull
	private List<VideoEvent> deleteChunkList = new ArrayList<>();

	/**
	 * All Video segments from selected video streams ordered by segment number
	 */
	@Nonnull@Setter(AccessLevel.NONE)
	private List<VideoEvent> chunksBySegmentID = new ArrayList<>();

	/**
	 * 
	 */
	@Nonnull@Setter(AccessLevel.NONE)
	private List<VideoEvent> filteredSegments = new ArrayList<>();

	public void setFilteredSegments(List<VideoEvent> replacementSegmentsList) {
		filteredSegments.clear();
		for (VideoEvent videoEvent : replacementSegmentsList) {
			filteredSegments.add(videoEvent);
		}
	}

	public void setAllSegments(List<VideoEvent> allSegmentsList) {
		if (null == allSegmentsList) {
			this.allSegments.clear();
		} else {
			this.allSegments = allSegmentsList;
		}
	}
	
	public void clear() {
		getAllSegments().clear();
		getChunksBySegmentID().clear();
		getDeleteChunkList().clear();
		getFilteredSegments().clear();
		getChunkPlayTimeList().clear();
	}
}
