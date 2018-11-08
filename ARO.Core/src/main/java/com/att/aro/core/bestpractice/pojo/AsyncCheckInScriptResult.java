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

public class AsyncCheckInScriptResult extends AbstractBestPracticeResult {
	private int syncPacketCount = 0;
	private int asyncPacketCount = 0;
	private int syncLoadedScripts = 0;
	private int asyncLoadedScripts = 0;
	private List<AsyncCheckEntry> results;
	@JsonIgnore
	private String exportAllSyncPacketCount;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.ASYNC_CHECK;
	}
	public int getSyncPacketCount() {
		return syncPacketCount;
	}
	public void setSyncPacketCount(int syncPacketCount) {
		this.syncPacketCount = syncPacketCount;
	}
	public int getAsyncPacketCount() {
		return asyncPacketCount;
	}
	public void setAsyncPacketCount(int asyncPacketCount) {
		this.asyncPacketCount = asyncPacketCount;
	}
	public int getSyncLoadedScripts() {
		return syncLoadedScripts;
	}
	public void setSyncLoadedScripts(int syncLoadedScripts) {
		this.syncLoadedScripts = syncLoadedScripts;
	}
	/**
	 * Returns a list of async loaded files.
	 * 
	 * @return the results
	 */
	public List<AsyncCheckEntry> getResults() {
		return results;
	}
	public void setResults(List<AsyncCheckEntry> results) {
		this.results = results;
	}
	public int getTotalLoadedScripts() {
		return syncLoadedScripts + asyncLoadedScripts;
	}
	/**
	 * Increments the Async loaded scripts
	 * 
	 */
	public void incrementAsyncLoadedScripts() {
		this.asyncLoadedScripts++;
	}

	/**
	 * Increments the sync loaded scripts
	 * 
	 */
	public void incrementSyncLoadedScripts() {
		this.syncLoadedScripts++;
	}
	public int getAsyncLoadedScripts() {
		return asyncLoadedScripts;
	}
	public void setAsyncLoadedScripts(int asyncLoadedScripts) {
		this.asyncLoadedScripts = asyncLoadedScripts;
	}
	/**
	 * Increments the Sync packet count
	 * 
	 */
	public void incrementSyncPacketCount() {
		this.syncPacketCount++;
	}
	public void incrementAsyncPacketCount() {
		this.asyncPacketCount++;
	}
	public String getExportAllSyncPacketCount() {
		return exportAllSyncPacketCount;
	}
	public void setExportAllSyncPacketCount(String exportAllSyncPacketCount) {
		this.exportAllSyncPacketCount = exportAllSyncPacketCount;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		AsyncCheckInScriptResult scriptResult = (AsyncCheckInScriptResult) obj;
		if (scriptResult.getSyncLoadedScripts() != syncLoadedScripts
				|| scriptResult.getSyncPacketCount() != syncPacketCount) {
			return false;
		}
		if (scriptResult.getAsyncLoadedScripts() != asyncLoadedScripts
				|| scriptResult.getAsyncPacketCount() != asyncPacketCount) {
			return false;
		}
		if ((!scriptResult.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != scriptResult.getResultType()) {
			return false;
		}
		if (!scriptResult.getResults().containsAll(results)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + syncLoadedScripts;
		result = prime * result + syncPacketCount;
		result = prime * result + asyncLoadedScripts;
		result = prime * result + asyncPacketCount;
		for (AsyncCheckEntry entry : results) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();

		return result;
	}
}
