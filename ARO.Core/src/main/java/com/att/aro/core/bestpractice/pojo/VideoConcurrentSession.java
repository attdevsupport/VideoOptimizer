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

public class VideoConcurrentSession {
	int concurrentSessionCount;
	Double concurrencyDuration;
	String videoName = "";

	public VideoConcurrentSession() {
	}

	public VideoConcurrentSession(int concurrentSessionCount, Double concurrencyDuration) {
		this.concurrentSessionCount = concurrentSessionCount;
		this.concurrencyDuration = concurrencyDuration;
	}

	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}

	public String getVideoName() {
		return videoName;
	}

	public void setCountDuration(int concurrentSessionCount, Double concurrencyDuration) {
		this.concurrentSessionCount = concurrentSessionCount;
		this.concurrencyDuration = concurrencyDuration;
	}

	public Double getConcurrencyDuration() {
		return concurrencyDuration;
	}

	public int getConcurrentSessionCount() {
		return concurrentSessionCount;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		VideoConcurrentSession other = (VideoConcurrentSession) obj;
		if (Double.doubleToLongBits(other.getConcurrencyDuration()) != Double.doubleToLongBits(concurrencyDuration)
				|| other.getConcurrentSessionCount() != concurrentSessionCount) {
			return false;
		}
		if (!other.getVideoName().equals(videoName)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp = Double.doubleToLongBits(concurrencyDuration);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + concurrentSessionCount;
		result = prime * result + videoName.hashCode();
		return result;
	}
}
