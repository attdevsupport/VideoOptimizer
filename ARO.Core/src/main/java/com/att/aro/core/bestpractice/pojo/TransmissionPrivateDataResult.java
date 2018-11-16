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

import java.util.List;

public class TransmissionPrivateDataResult extends AbstractBestPracticeResult {
	private List<TransmissionPrivateDataEntry> results;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.TRANSMISSION_PRIVATE_DATA;
	}

	public List<TransmissionPrivateDataEntry> getResults() {
		return results;
	}

	public void setResults(List<TransmissionPrivateDataEntry> results) {
		this.results = results;
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
		TransmissionPrivateDataResult other = (TransmissionPrivateDataResult) obj;
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
		for (TransmissionPrivateDataEntry entry : results) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
