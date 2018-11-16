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

import com.fasterxml.jackson.annotation.JsonProperty;

public class VideoConcurrentSessionResult extends AbstractBestPracticeResult {
	private int maxConcurrentSessionsCount = 0;
	@Nonnull private List<VideoConcurrentSession> manifestConcurrency = new ArrayList<VideoConcurrentSession>();

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.VIDEO_CONCURRENT_SESSION;
	}

	public int getMaxConcurrentSessionCount() {
		return maxConcurrentSessionsCount;
	}
	@JsonProperty(value = "maxConcurrentSessionCount")
	public void setMaxConcurrentSessionsCount(int maxConcurrentSessionsCount) {
		this.maxConcurrentSessionsCount = maxConcurrentSessionsCount;
	}

	public List<VideoConcurrentSession> getResults() {
		return manifestConcurrency;
	}

	public void setResults(List<VideoConcurrentSession> manifestConcurrency) {
		if (manifestConcurrency != null) {
			this.manifestConcurrency = manifestConcurrency;
		}else{
			this.manifestConcurrency.clear();
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
		VideoConcurrentSessionResult other = (VideoConcurrentSessionResult) obj;
		if (other.getMaxConcurrentSessionCount() != this.maxConcurrentSessionsCount) {
			return false;
		}
		if (!other.getResults().containsAll(manifestConcurrency)) {
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
		result = prime * result + maxConcurrentSessionsCount;
		for (VideoConcurrentSession session : manifestConcurrency) {
			result = prime * result + session.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
