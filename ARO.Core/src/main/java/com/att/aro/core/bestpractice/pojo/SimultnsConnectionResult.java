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
import java.util.OptionalInt;

public class SimultnsConnectionResult extends AbstractBestPracticeResult {
	private List<MultipleConnectionsEntry> results = null;

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.SIMUL_CONN;
	}

	public List<MultipleConnectionsEntry> getResults() {
		return results;
	}

	public void setResults(List<MultipleConnectionsEntry> results) {
		this.results = results;
	}
	
	public int getMaxSimultaneousConn() {
		OptionalInt max = results.stream().mapToInt((m) -> m.getConcurrentSessions()).max();
		return max.isPresent() ? max.getAsInt() : 0;
	}
}
