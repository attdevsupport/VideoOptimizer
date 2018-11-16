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

public class SpriteImageEntry {
	@JsonIgnore
	private double timeStamp;
	private int fileSize;
	private String hostName;
	private String httpObjectName;
	@JsonIgnore
	private HttpRequestResponseInfo httpRequestResponse;
	
	public SpriteImageEntry(HttpRequestResponseInfo hrri) {
		this.timeStamp = hrri.getTimeStamp();
		this.fileSize = hrri.getContentLength();
		this.httpRequestResponse = hrri;

		HttpRequestResponseInfo rsp = hrri.getAssocReqResp();
		if (rsp != null) {
			this.httpObjectName = rsp.getObjName();
			this.hostName = rsp.getHostName();
		} else {
			this.httpObjectName = "";
			this.hostName = "";
		}
	}
	public int getFileSize() {
		return fileSize;
	}
	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}
	public double getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(double timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getHttpObjectName() {
		return httpObjectName;
	}
	public void setHttpObjectName(String httpObjectName) {
		this.httpObjectName = httpObjectName;
	}
	public HttpRequestResponseInfo getHttpRequestResponse() {
		return httpRequestResponse;
	}
	public void setHttpRequestResponse(HttpRequestResponseInfo httpRequestResponse) {
		this.httpRequestResponse = httpRequestResponse;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		SpriteImageEntry other = (SpriteImageEntry) obj;
		if (other.getFileSize() != fileSize) {
			return false;
		}
		if (!other.getHostName().equals(hostName) || (!other.getHttpObjectName().equals(httpObjectName))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fileSize;
		result = prime * result + hostName.hashCode();
		result = prime * result + httpObjectName.hashCode();
		return result;
	}
}
