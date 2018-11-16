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

public class VideoNetworkComparisonResult extends AbstractBestPracticeResult {

	private double avgKbps = 0.0;
	private double avgBitRate = 0.0;
 	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.NETWORK_COMPARISON;
	}
	
	public double getAvgKbps() {
		return avgKbps;
	}


	public void setAvgKbps(double avgKbps) {
		this.avgKbps = avgKbps;
	}


	public double getAvgBitRate() {
		return avgBitRate;
	}


	public void setAvgBitRate(double avgBitRate) {
		this.avgBitRate = avgBitRate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		VideoNetworkComparisonResult other = (VideoNetworkComparisonResult) obj;
		if (Double.doubleToLongBits(other.getAvgBitRate()) != Double.doubleToLongBits(avgBitRate)) {
			return false;
		}
		if (Double.doubleToLongBits(other.getAvgKbps()) != Double.doubleToLongBits(avgKbps)) {
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
		long temp;
		temp = Double.doubleToLongBits(avgBitRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(avgKbps);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
