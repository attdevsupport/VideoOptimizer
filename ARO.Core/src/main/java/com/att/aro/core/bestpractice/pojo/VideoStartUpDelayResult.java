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

public class VideoStartUpDelayResult extends AbstractBestPracticeResult {

	private double startUpDelay;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.STARTUP_DELAY;
	}

	public double getStartUpDelay() {
		return startUpDelay;
	}

	public void setStartUpDelay(double startUpDelay) {
		this.startUpDelay = startUpDelay;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		VideoStartUpDelayResult startupDelay = (VideoStartUpDelayResult) obj;
		if (Double.doubleToLongBits(this.startUpDelay) != Double.doubleToLongBits(startupDelay.getStartUpDelay())) {
			return false;
		}
		if ((!startupDelay.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != startupDelay.getResultType()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(startUpDelay);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
