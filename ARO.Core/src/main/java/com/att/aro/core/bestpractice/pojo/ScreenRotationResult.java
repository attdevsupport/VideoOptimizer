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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ScreenRotationResult extends AbstractBestPracticeResult {
	private double screenRotationBurstTime = 0.0;
	@JsonIgnore
	private String exportAllScreenRotationDescPass;
	@JsonIgnore
	private String exportAllScreenRotationFailed;
	
	public double getScreenRotationBurstTime() {
		return screenRotationBurstTime;
	}

	public void setScreenRotationBurstTime(double screenRotationBurstTime) {
		this.screenRotationBurstTime = screenRotationBurstTime;
	}

	
	public String getExportAllScreenRotationDescPass() {
		return exportAllScreenRotationDescPass;
	}

	public void setExportAllScreenRotationDescPass(
			String exportAllScreenRotationDescPass) {
		this.exportAllScreenRotationDescPass = exportAllScreenRotationDescPass;
	}

	public String getExportAllScreenRotationFailed() {
		return exportAllScreenRotationFailed;
	}

	public void setExportAllScreenRotationFailed(
			String exportAllScreenRotationFailed) {
		this.exportAllScreenRotationFailed = exportAllScreenRotationFailed;
	}

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.SCREEN_ROTATION;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		ScreenRotationResult other = (ScreenRotationResult) obj;
		if (Double.doubleToLongBits(other.getScreenRotationBurstTime()) != Double
				.doubleToLongBits(screenRotationBurstTime)) {
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
		long temp;
		temp = Double.doubleToLongBits(screenRotationBurstTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
