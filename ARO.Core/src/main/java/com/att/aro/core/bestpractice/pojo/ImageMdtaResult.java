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

public class ImageMdtaResult extends AbstractBestPracticeResult {
	private List<ImageMdataEntry> results = null;
	@JsonIgnore
	private String exportNumberOfMdataImages;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.IMAGE_MDATA;
	}

	public List<ImageMdataEntry> getResults() {
		return results;
	}

	public void setResults(List<ImageMdataEntry> results) {
		this.results = results;
	}

	public String getExportNumberOfMdataImages() {
		return exportNumberOfMdataImages;
	}

	public void setExportNumberOfMdataImages(String exportNumberOfMdataImages) {
		this.exportNumberOfMdataImages = exportNumberOfMdataImages;
	}
	
	public int getErrorCount() {
		return results != null ? results.size() : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		ImageMdtaResult other = (ImageMdtaResult) obj;
		if (!other.getResults().containsAll(results)) {
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
		for (ImageMdataEntry entry : results) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
