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
package com.att.aro.core.bestpractice.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UnnecessaryConnectionEntry {
	@JsonIgnore
	private double lowTime;
	@JsonIgnore
	private double highTime;
	private int burstCount;
	private double totalKB;
	
	public UnnecessaryConnectionEntry(){}
	
	public UnnecessaryConnectionEntry(double lTime, double hTime, int bCount, double tKB){
		this.lowTime = lTime;
		this.highTime = hTime;
		this.burstCount = bCount;
		this.totalKB = tKB;
		
	}
	
	/**
	 * @return the lowTime
	 */
	public double getLowTime() {
		return lowTime;
	}
	/**
	 * @return the highTime
	 */
	public double getHighTime() {
		return highTime;
	}
	/**
	 * @return the burstCount
	 */
	public int getBurstCount() {
		return burstCount;
	}
	/**
	 * @return the totalKB
	 */
	public double getTotalKB() {
		return totalKB;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		UnnecessaryConnectionEntry other = (UnnecessaryConnectionEntry) obj;
		if (Double.doubleToLongBits(other.getTotalKB()) != Double.doubleToLongBits(totalKB)
				|| other.getBurstCount() != burstCount) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp = Double.doubleToLongBits(totalKB);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + burstCount;
		return result;
	}
}
