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

public class ForwardSecrecyResult extends AbstractBestPracticeResult {
	private List<ForwardSecrecyEntry> results;

	public List<ForwardSecrecyEntry> getResults() {
		return results;
	}

	public void setResults(List<ForwardSecrecyEntry> results) {
		this.results = results;
	}
	
	public int getFailureCount() {
		return  results != null ? results.size() : 0;
	}

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.FORWARD_SECRECY;
	}
}
