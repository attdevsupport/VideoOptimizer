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

public class TextFileCompressionEntry extends HttpEntry {
	private int contentLength;
	private int savingsTextPercentage; //US432352 changes
	
	public TextFileCompressionEntry(){}
	
	public TextFileCompressionEntry(HttpRequestResponseInfo hrri,
			HttpRequestResponseInfo lastRequestObj, String domainName) {
		super(hrri, lastRequestObj, domainName);
		this.contentLength = hrri.getContentLength();
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	/**
	 * @return the savingsPercentage
	 */
	public int getSavingsTextPercentage() {
		return savingsTextPercentage;
	}

	/**
	 * @param savingsPercentage the savingsPercentage to set
	 */
	public void setSavingsTextPercentage(int savingsPercentage) {
		this.savingsTextPercentage = savingsPercentage;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		TextFileCompressionEntry other = (TextFileCompressionEntry) obj;
		if (!other.getHostName().equals(getHostName())) {
			return false;
		}
		if (!other.getHttpObjectName().equals(getHttpObjectName())) {
			return false;
		}
		if (!other.getHttpCode().equals(getHttpCode())) {
			return false;
		}
		if (other.getContentLength() != contentLength
				|| other.getSavingsTextPercentage() != savingsTextPercentage) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + contentLength;
		result = prime * result + savingsTextPercentage;
		result = prime * result + getHostName().hashCode();
		result = prime * result + getHttpObjectName().hashCode();
		result = prime * result + (int) getHttpCode();
		return result;
	}
}
