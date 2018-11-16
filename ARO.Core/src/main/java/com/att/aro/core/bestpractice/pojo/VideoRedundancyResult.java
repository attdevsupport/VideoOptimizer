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

public class VideoRedundancyResult extends AbstractBestPracticeResult {

	private double redundantPercentage;
	private int countSegment;
	private int countRedundant;
	private int countDuplicate;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.VIDEO_REDUNDANCY;
	}

	public double getRedundantPercentage() {
		return redundantPercentage;
	}

	public int getCountSegment() {
		return countSegment;
	}

	public int getCountRedundant() {
		return countRedundant;
	}

	public int getCountDuplicate() {
		return countDuplicate;
	}

	public void setRedundantPercentage(double redundantPercentage) {
		this.redundantPercentage = redundantPercentage;
	}

	public void setSegmentCount(int countSegment) {
		this.countSegment = countSegment;
	}

	public void setRedundantCount(int countRedundant) {
		this.countRedundant = countRedundant;
	}

	public void setDuplicateCount(int countDuplicate) {
		this.countDuplicate = countDuplicate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		VideoRedundancyResult other = (VideoRedundancyResult) obj;
		if (Double.doubleToLongBits(other.getRedundantPercentage()) != Double.doubleToLongBits(redundantPercentage)) {
			return false;
		}
		if (other.getCountDuplicate() != countDuplicate || other.getCountRedundant() != countRedundant) {
			return false;
		}
		if (other.getCountSegment() != countSegment) {
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
		long temp = Double.doubleToLongBits(redundantPercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + countDuplicate;
		result = prime * result + countRedundant;
		result = prime * result + countSegment;
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
