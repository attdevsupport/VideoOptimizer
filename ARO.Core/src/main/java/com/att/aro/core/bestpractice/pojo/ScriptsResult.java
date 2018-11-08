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

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;


public class ScriptsResult extends AbstractBestPracticeResult {
	private int numberOfFailedFiles = 0;
	@JsonIgnore
	private HttpRequestResponseInfo firstFailedHtml;
	@JsonIgnore
	private String exportAllNumberOfScriptsFiles;
	
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.SCRIPTS_URL;
	}
	public int getNumberOfFailedFiles() {
		return numberOfFailedFiles;
	}
	public void setNumberOfFailedFiles(int numberOfFailedFiles) {
		this.numberOfFailedFiles = numberOfFailedFiles;
	}
	public void incrementNumberOfFailedFiles(){
		this.numberOfFailedFiles++;
	}
	public HttpRequestResponseInfo getFirstFailedHtml() {
		return firstFailedHtml;
	}
	public void setFirstFailedHtml(HttpRequestResponseInfo firstFailedHtml) {
		this.firstFailedHtml = firstFailedHtml;
	}
	public String getExportAllNumberOfScriptsFiles() {
		return exportAllNumberOfScriptsFiles;
	}
	public void setExportAllNumberOfScriptsFiles(
			String exportAllNumberOfScriptsFiles) {
		this.exportAllNumberOfScriptsFiles = exportAllNumberOfScriptsFiles;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		ScriptsResult other = (ScriptsResult) obj;
		if (other.getNumberOfFailedFiles() != numberOfFailedFiles) {
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
		result = prime * result + numberOfFailedFiles;
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
