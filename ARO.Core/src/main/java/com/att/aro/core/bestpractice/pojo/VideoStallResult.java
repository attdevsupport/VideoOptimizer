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

import javax.annotation.Nonnull;

import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class VideoStallResult extends AbstractBestPracticeResult {
	private int videoStalls;
	@Nonnull private List<VideoStall> videoStallList = new ArrayList<>();
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.VIDEO_STALL;
	}
	
	public void setVideoStallResult(int videoStalls){
		this.videoStalls  = videoStalls;
	}

	public int getStallResult() {
		return videoStalls;
	}

	@JsonIgnore
	public List<VideoStall> getResults() {
		return videoStallList;
	}

	public void setResults(List<VideoStall> videoStallList) {
		if(videoStallList != null){
			this.videoStallList = videoStallList;
		}else{
			this.videoStallList.clear();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		VideoStallResult other = (VideoStallResult) obj;
		if (other.getStallResult() != videoStalls) {
			return false;
		}
		if (!other.getResults().containsAll(videoStallList)) {
			return false;
		}
		if ((!other.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != other.getResultType()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + videoStalls;
		for (VideoStall stall : videoStallList) {
			result = prime * result + stall.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
