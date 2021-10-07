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
import java.util.List;
import java.util.SortedMap;

import javax.annotation.Nonnull;

import org.springframework.util.CollectionUtils;

import com.att.aro.core.videoanalysis.pojo.QualityTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class VideoAdaptiveBitrateLadderResult extends AbstractBestPracticeResult {
	
	@Nonnull
	private List<QualityTime> results  = new ArrayList<>();
	
	@JsonIgnore
	public List<QualityTime> getResults() {
		return results;
	}

	public void setResults(SortedMap<Integer, QualityTime> qualityMap) {
		if (!CollectionUtils.isEmpty(qualityMap)) {
			this.results = new ArrayList<QualityTime>(qualityMap.values());
		} else {
			this.results.clear();
		}
	}
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.VIDEO_ABR_LADDER;
	}

}
