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

import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UsingCacheResult extends AbstractBestPracticeResult {
	@JsonProperty("missingCacheHeaderPct")
	private double cacheHeaderRatio = 0.0;
	@JsonIgnore
	private PacketInfo noCacheHeaderFirstPacket;
	@JsonIgnore
	private String exportAllCacheConPct;
	
	public double getCacheHeaderRatio() {
		return cacheHeaderRatio;
	}


	public void setCacheHeaderRatio(double cacheHeaderRatio) {
		this.cacheHeaderRatio = cacheHeaderRatio;
	}


	public PacketInfo getNoCacheHeaderFirstPacket() {
		return noCacheHeaderFirstPacket;
	}


	public void setNoCacheHeaderFirstPacket(PacketInfo noCacheHeaderFirstPacket) {
		this.noCacheHeaderFirstPacket = noCacheHeaderFirstPacket;
	}
	
	public String getExportAllCacheConPct() {
		return exportAllCacheConPct;
	}


	public void setExportAllCacheConPct(String exportAllCacheConPct) {
		this.exportAllCacheConPct = exportAllCacheConPct;
	}


	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.USING_CACHE;//name is confusing, using cache-> cache control, ref. bestpractice.properties
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		UsingCacheResult other = (UsingCacheResult) obj;
		if (Double.doubleToLongBits(other.getCacheHeaderRatio()) != Double.doubleToLongBits(cacheHeaderRatio)) {
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
		long temp = Double.doubleToLongBits(cacheHeaderRatio);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
