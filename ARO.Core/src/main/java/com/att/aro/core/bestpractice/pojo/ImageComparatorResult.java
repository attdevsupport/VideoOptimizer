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

public class ImageComparatorResult extends AbstractBestPracticeResult {
	private List<ImageMdataEntry> results = null;
	private String numberOfImages;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.IMAGE_COMPARE;
	}

	public List<ImageMdataEntry> getResults() {
		return results;
	}

	public void setResults(List<ImageMdataEntry> results) {
		this.results = results;
	}

	public String getNumberOfImages() {
		return numberOfImages;
	}

	public void setNumberOfImages(String numberOfImages) {
		this.numberOfImages = numberOfImages;
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
		ImageComparatorResult other = (ImageComparatorResult) obj;
		if (numberOfImages == null) {
			if (other.getNumberOfImages() != null) {
				return false;
			}
		} else if (!other.getNumberOfImages().equals(numberOfImages)) {
			return false;
		}
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
		result = prime * result + numberOfImages.hashCode();
		for (ImageMdataEntry entry : results) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}