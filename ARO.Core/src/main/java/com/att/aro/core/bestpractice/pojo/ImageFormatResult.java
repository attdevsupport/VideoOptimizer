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

public class ImageFormatResult extends AbstractBestPracticeResult {
	private List<ImageMdataEntry> imgMdataresults = null;

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.IMAGE_FORMAT;
	}

	public List<ImageMdataEntry> getResults() {
		return imgMdataresults;
	}

	public void setResults(List<ImageMdataEntry> results) {
		this.imgMdataresults = results;
	}

	public int getErrorCount() {
		return imgMdataresults != null ? imgMdataresults.size() : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		ImageFormatResult other = (ImageFormatResult) obj;
		if (!other.getResults().containsAll(imgMdataresults)) {
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
		for (ImageMdataEntry entry : imgMdataresults) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
