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

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class VideoConcurrentSessionResult extends AbstractBestPracticeResult {
	private int maxConcurrentSessionCount = 0;
	@Nonnull
	private List<VideoConcurrentSession> manifestConcurrency = new ArrayList<>();

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.VIDEO_CONCURRENT_SESSION;
	}

	public List<VideoConcurrentSession> getResults() {
		return manifestConcurrency;
	}

	public void setResults(List<VideoConcurrentSession> manifestConcurrency) {
		if (manifestConcurrency != null) {
			this.manifestConcurrency = manifestConcurrency;
		} else {
			this.manifestConcurrency.clear();
		}
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
