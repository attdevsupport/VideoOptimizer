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

public class AdAnalyticsResult extends AbstractBestPracticeResult {
	private List<MultipleConnectionsEntry> results = null;
	double bytes = 0.0f;

	@Override
	public BestPracticeType getBestPracticeType() {
		return null;
	}

	public List<MultipleConnectionsEntry> getResults() {
		return results;
	}

	public void setResults(List<MultipleConnectionsEntry> results) {
		this.results = results;
	}

	public double getBytes() {
		return bytes;
	}

	public void setBytes(double bytes) {
		this.bytes = bytes;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		AdAnalyticsResult analyticResult = (AdAnalyticsResult) obj;
		if(Double.doubleToLongBits(bytes) != Double.doubleToLongBits(analyticResult.getBytes())){
			return false;
		}
		if(!results.containsAll(analyticResult.getResults())){
			return false;
		}	
		if ((!analyticResult.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != analyticResult.getResultType()) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(bytes);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		for(MultipleConnectionsEntry entry : getResults()){
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		
		return result;
	}
}
