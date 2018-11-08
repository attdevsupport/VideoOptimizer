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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UnnecessaryConnectionResult extends AbstractBestPracticeResult{
	private int tightlyCoupledBurstCount = 0;
	private double tightlyCoupledBurstTime = 0;
	@JsonIgnore
	private String exportAllMultiConnDesc;
	@JsonProperty("connectionEntries")
	private List<UnnecessaryConnectionEntry> tightlyCoupledBurstsDetails;
	
	public int getTightlyCoupledBurstCount() {
		return tightlyCoupledBurstCount;
	}

	public void setTightlyCoupledBurstCount(int tightlyCoupledBurstCount) {
		this.tightlyCoupledBurstCount = tightlyCoupledBurstCount;
	}

	public double getTightlyCoupledBurstTime() {
		return tightlyCoupledBurstTime;
	}

	public void setTightlyCoupledBurstTime(double tightlyCoupledBurstTime) {
		this.tightlyCoupledBurstTime = tightlyCoupledBurstTime;
	}

	public String getExportAllMultiConnDesc() {
		return exportAllMultiConnDesc;
	}

	public void setExportAllMultiConnDesc(String exportAllMultiConnDesc) {
		this.exportAllMultiConnDesc = exportAllMultiConnDesc;
	}

	/**
	 * @return the tightlyCoupledBurstsDetails
	 */
	public List<UnnecessaryConnectionEntry> getTightlyCoupledBurstsDetails() {
		return tightlyCoupledBurstsDetails;
	}

	/**
	 * @param tightlyCoupledBurstsDetails the tightlyCoupledBurstsDetails to set
	 */
	public void setTightlyCoupledBurstsDetails(
			List<UnnecessaryConnectionEntry> tightlyCoupledBurstsDetails) {
		this.tightlyCoupledBurstsDetails = tightlyCoupledBurstsDetails;
	}

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.UNNECESSARY_CONNECTIONS;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		UnnecessaryConnectionResult other = (UnnecessaryConnectionResult) obj;
		if (Double.doubleToLongBits(other.getTightlyCoupledBurstTime()) != Double
				.doubleToLongBits(tightlyCoupledBurstTime)) {
			return false;
		}
		if (other.getTightlyCoupledBurstCount() != tightlyCoupledBurstCount) {
			return false;
		}
		if (!other.getTightlyCoupledBurstsDetails().containsAll(tightlyCoupledBurstsDetails)) {
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
		temp = Double.doubleToLongBits(tightlyCoupledBurstTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + tightlyCoupledBurstCount;
		for (UnnecessaryConnectionEntry entry : tightlyCoupledBurstsDetails) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
