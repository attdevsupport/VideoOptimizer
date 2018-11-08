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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FileOrderResult extends AbstractBestPracticeResult{
	@JsonIgnore
	private String textResult;
	@JsonIgnore
	private String exportAll;
	private List<FileOrderEntry> results = null;
	private int fileOrderCount = 0;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.FILE_ORDER;
	}

	public String getTextResult() {
		return textResult;
	}

	public void setTextResult(String textResult) {
		this.textResult = textResult;
	}

	public String getExportAll() {
		return exportAll;
	}

	public void setExportAll(String exportAll) {
		this.exportAll = exportAll;
	}
	/**
	 * Increments the file order count
	 * 
	 */
	public void incrementFileOrderCount() {
		this.fileOrderCount++;
	}

	/**
	 * Returns the file order count
	 * 
	 */
	public int getFileOrderCount() {
		return fileOrderCount;
	}

	/**
	 * Returns an indicator whether the file order test has failed or not.
	 * 
	 * @return failed/success test indicator
	 */
	@JsonIgnore
	public boolean isTestFailed() {
		return (getFileOrderCount() > 0);
	}

	/**
	 * Returns a list of file order BP files
	 * 
	 * @return the results
	 */
	public List<FileOrderEntry> getResults() {
		return results;
	}


	public void setResults(List<FileOrderEntry> results) {
		this.results = results;
	}

	public void setFileOrderCount(int fileOrderCount) {
		this.fileOrderCount = fileOrderCount;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		FileOrderResult fileOrderResult = (FileOrderResult) obj;
		if (fileOrderCount != fileOrderResult.getFileOrderCount()) {
			return false;
		}
		if (!fileOrderResult.getResults().containsAll(results)) {
			return false;
		}
		if ((!fileOrderResult.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != fileOrderResult.getResultType()) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fileOrderCount;
		for (FileOrderEntry entry : results) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
