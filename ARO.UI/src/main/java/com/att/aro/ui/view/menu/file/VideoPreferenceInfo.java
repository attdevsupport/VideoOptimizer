/*
 *  Copyright 2018 AT&T
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
package com.att.aro.ui.view.menu.file;

public class VideoPreferenceInfo {

	String bestPractice;
	String warningCriteria;
	String failCriteria;
	int warningCriteriaInt;
	int failCriteriaInt;
	
	public VideoPreferenceInfo(String bestPractice, String warningCriteria,
			String failCriteria) {
		this.bestPractice = bestPractice;
		this.warningCriteria = warningCriteria;
		this.failCriteria = failCriteria;
	}
	
	public VideoPreferenceInfo(String bestPractice, int warningCriteriaInt,
			int failCriteriaInt) {
		this.bestPractice = bestPractice;
		this.warningCriteriaInt = warningCriteriaInt;
		this.failCriteriaInt = failCriteriaInt;
	}
	
	public String getBestPractice() {
		return bestPractice;
	}

	public void setBestPractice(String bestPractice) {
		this.bestPractice = bestPractice;
	}

	public String getWarningCriteria() {
		return warningCriteria;
	}

	public void setWarningCriteria(String warningCriteria) {
		this.warningCriteria = warningCriteria;
	}

	public String getFailCriteria() {
		return failCriteria;
	}

	public void setFailCriteria(String failCriteria) {
		this.failCriteria = failCriteria;
	}

	public int getWarningCriteriaInt() {
		return warningCriteriaInt;
	}

	public void setWarningCriteriaInt(int warningCriteriaInt) {
		this.warningCriteriaInt = warningCriteriaInt;
	}

	public int getFailCriteriaInt() {
		return failCriteriaInt;
	}

	public void setFailCriteriaInt(int failCriteriaInt) {
		this.failCriteriaInt = failCriteriaInt;
	}
	
}
