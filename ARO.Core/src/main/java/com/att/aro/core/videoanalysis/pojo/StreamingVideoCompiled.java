
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;

import lombok.Data;

@Data
public class StreamingVideoCompiled {

	private SortedMap<VideoEvent, Double> chunkPlayTimeList = new TreeMap<>();
	
	/**
	 * All video segements with out the duplicate chunks
	 */
	@Nonnull
	private Map<VideoEvent, VideoStream> veStreamList = new HashMap<>();
	
	/**
	 * All video segments including the duplicate chunks
	 */
	@Nonnull
	private List<VideoEvent> allSegments = new ArrayList<>();
	@Nonnull
	private List<VideoEvent> duplicateChunks = new ArrayList<>();
	
	/**
	 * All Video segments from selected video streams ordered by segment number
	 */
	@Nonnull
	private List<VideoEvent> chunksBySegment = new ArrayList<>();
	
	/**
	 * 
	 */
	@Nonnull
	private List<VideoEvent> filteredSegments = new ArrayList<>();
	
	
	public void setVideoEventManifestMap(Map<VideoEvent, VideoStream> veStreamList) {
		if(null == veStreamList){
			this.veStreamList.clear();
		}else{
			this.veStreamList = veStreamList;
		}
	}

	public void setAllSegments(List<VideoEvent> allSegments) {
		if(null == allSegments){
			this.allSegments.clear();
		}else{
			this.allSegments = allSegments;
		}
	}
	
	public void setChunksBySegmentNumber(List<VideoEvent> chunksBySegment) {
		if(chunksBySegment == null){
			this.chunksBySegment.clear();
		}else{
			this.chunksBySegment = chunksBySegment;
		}
	}
}

