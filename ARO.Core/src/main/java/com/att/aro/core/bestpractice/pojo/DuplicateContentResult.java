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

import com.att.aro.core.packetanalysis.pojo.CacheEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DuplicateContentResult extends AbstractBestPracticeResult{
	private double duplicateContentBytesRatio = 0.0;
	private int duplicateContentSizeOfUniqueItems = 0;
	private long duplicateContentBytes = 0;
	private long totalContentBytes = 0;
	@JsonProperty("duplicateContentCount")
	private int duplicateContentsize = 0;
	@JsonIgnore
	private String exportAllPct;
	@JsonIgnore
	private String exportAllFiles;
	@JsonIgnore
	private String staticsUnitsMbytes;
	private List<CacheEntry> duplicateContentList;
	
	
	public List<CacheEntry> getDuplicateContentList() {
		return duplicateContentList;
	}
	public void setDuplicateContentList(List<CacheEntry> duplicateContentList) {
		this.duplicateContentList = duplicateContentList;
	}
	public String getExportAllPct() {
		return exportAllPct;
	}
	public void setExportAllPct(String exportAllPct) {
		this.exportAllPct = exportAllPct;
	}
	public String getExportAllFiles() {
		return exportAllFiles;
	}
	public void setExportAllFiles(String exportAllFiles) {
		this.exportAllFiles = exportAllFiles;
	}
	public String getStaticsUnitsMbytes() {
		return staticsUnitsMbytes;
	}
	public void setStaticsUnitsMbytes(String staticsUnitsMbytes) {
		this.staticsUnitsMbytes = staticsUnitsMbytes;
	}
	public int getDuplicateContentsize() {
		return duplicateContentsize;
	}
	public void setDuplicateContentsize(int duplicateContentsize) {
		this.duplicateContentsize = duplicateContentsize;
	}
	public double getDuplicateContentBytesRatio() {
		return duplicateContentBytesRatio;
	}
	public void setDuplicateContentBytesRatio(double duplicateContentBytesRatio) {
		this.duplicateContentBytesRatio = duplicateContentBytesRatio;
	}
	public int getDuplicateContentSizeOfUniqueItems() {
		return duplicateContentSizeOfUniqueItems;
	}
	public void setDuplicateContentSizeOfUniqueItems(
			int duplicateContentSizeOfUniqueItems) {
		this.duplicateContentSizeOfUniqueItems = duplicateContentSizeOfUniqueItems;
	}
	public long getDuplicateContentBytes() {
		return duplicateContentBytes;
	}
	public void setDuplicateContentBytes(long duplicateContentBytes) {
		this.duplicateContentBytes = duplicateContentBytes;
	}
	public long getTotalContentBytes() {
		return totalContentBytes;
	}
	public void setTotalContentBytes(long totalContentBytes) {
		this.totalContentBytes = totalContentBytes;
	}

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.DUPLICATE_CONTENT;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		DuplicateContentResult dupContentResult = (DuplicateContentResult) obj;
		if (Double.doubleToLongBits(duplicateContentBytesRatio) != Double
				.doubleToLongBits(dupContentResult.getDuplicateContentBytesRatio())) {
			return false;
		}
		if (dupContentResult.getDuplicateContentBytes() != duplicateContentBytes
				|| dupContentResult.getTotalContentBytes() != totalContentBytes) {
			return false;
		}
		if (dupContentResult.getDuplicateContentSizeOfUniqueItems() != duplicateContentSizeOfUniqueItems
				|| dupContentResult.getDuplicateContentsize() != duplicateContentsize) {
			return false;
		}
		if (!dupContentResult.getDuplicateContentList().containsAll(duplicateContentList)) {
			return false;
		}
		if ((!dupContentResult.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != dupContentResult.getResultType()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(duplicateContentBytesRatio);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) duplicateContentBytes;
		result = prime * result + (int) totalContentBytes;
		result = prime * result + duplicateContentSizeOfUniqueItems;
		result = prime * result + duplicateContentsize;
		for (CacheEntry cacheEntry : duplicateContentList) {
			result = prime * result + cacheEntry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
