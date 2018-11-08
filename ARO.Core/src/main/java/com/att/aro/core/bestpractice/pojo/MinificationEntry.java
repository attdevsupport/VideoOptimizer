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

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;

public class MinificationEntry extends HttpEntry {
	private int savingPercentage;
	private int savingsSizeInByte;
	private int originalSizeInByte;
	
	public MinificationEntry(){}
	
	public MinificationEntry(HttpRequestResponseInfo hrri,
			HttpRequestResponseInfo lastRequestObj, String domainName, int savingPercentage,int savingsSizeInByte, int originalSizeInByte) {
		super(hrri, lastRequestObj, domainName);
		this.savingPercentage = savingPercentage;
		this.savingsSizeInByte = savingsSizeInByte;
		this.originalSizeInByte = originalSizeInByte;
	}

	public int getSavingPercentage() {
		return savingPercentage;
	}

	public void setSavingPercentage(int savingPercentage) {
		this.savingPercentage = savingPercentage;
	}

	public int getSavingsSizeInByte() {
		return savingsSizeInByte;
	}

	public void setSavingsSizeInByte(int savingsSizeInByte) {
		this.savingsSizeInByte = savingsSizeInByte;
	}

	public int getOriginalSizeInByte() {
		return originalSizeInByte;
	}

	public void setOriginalSizeInByte(int originalSizeInByte) {
		this.originalSizeInByte = originalSizeInByte;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		MinificationEntry other = (MinificationEntry) obj;
		if (other.getOriginalSizeInByte() != originalSizeInByte || other.getSavingPercentage() != savingPercentage) {
			return false;
		}
		if (other.getSavingsSizeInByte() != savingsSizeInByte) {
			return false;
		}
		if (!other.getHostName().equals(getHostName())) {
			return false;
		}
		if (!other.getHttpObjectName().equals(getHttpObjectName())) {
			return false;
		}
		if (!other.getHttpCode().equals(getHttpCode())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + originalSizeInByte;
		result = prime * result + savingPercentage;
		result = prime * result + savingsSizeInByte;
		result = prime * result + getHostName().hashCode();
		result = prime * result + getHttpObjectName().hashCode();
		result = prime * result + (int) getHttpCode();
		return result;
	}
}
