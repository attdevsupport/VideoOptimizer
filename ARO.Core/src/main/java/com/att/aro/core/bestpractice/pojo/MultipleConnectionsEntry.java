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

import lombok.Data;

@Data
public class MultipleConnectionsEntry {
	@JsonIgnore
	private double startTimeStamp;
	@JsonIgnore
	private double endTimeStamp;
	@JsonIgnore
	private int httpStatusCode;
	private int concurrentSessions;
	private double byteCount;
	@JsonIgnore
	private String httpReqObjName = "";
	private String hostName = "";
	private String ipValue = "";
	@JsonIgnore
	private HttpRequestResponseInfo httpReqRespInfo;
	private boolean isMultiple;

	public MultipleConnectionsEntry(){}
	
	public MultipleConnectionsEntry(HttpRequestResponseInfo httpReqRespInfo, String domainName, int concurrentSessions,
			double start, double end, String ipInside, boolean isMultiple) {
		this.httpReqRespInfo = httpReqRespInfo;
		this.httpReqObjName = domainName;
		this.startTimeStamp = Math.round(start * 1000.0) / 1000.0;
		this.endTimeStamp = Math.round(end * 1000.0) / 1000.0;
		this.httpStatusCode = httpReqRespInfo.getStatusCode();
		this.concurrentSessions = concurrentSessions;
		this.ipValue = ipInside;
		this.isMultiple = isMultiple;
		assignHostAndHttpInfo(httpReqRespInfo, domainName);
	}

	private void assignHostAndHttpInfo(HttpRequestResponseInfo httpReqResp, String hostName) {
		HttpRequestResponseInfo respons = httpReqResp.getAssocReqResp();
		if (respons != null) {
			if (respons.getHostName() == null || respons.getHostName().isEmpty()) {
				if (httpReqResp.getHostName() == null || httpReqResp.getHostName().isEmpty()) {
					this.hostName = hostName;
				} else {
					this.hostName = httpReqResp.getHostName();
				}
			} else {
				this.hostName = respons.getHostName();
			}
			this.httpReqObjName = respons.getObjName();
		} else {
			if (httpReqResp.getHostName() == null || httpReqResp.getHostName().isEmpty()) {
				this.hostName = hostName;
			} else {
				this.hostName = httpReqResp.getHostName();
			}
		}
		String hostNameObj = this.hostName;
		if (hostNameObj.contains("/")) {
			this.hostName = hostNameObj.substring(hostNameObj.lastIndexOf('/') + 1, hostNameObj.length());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		MultipleConnectionsEntry entry = (MultipleConnectionsEntry) obj;
		if((!ipValue.equals(entry.getIpValue())) || (!hostName.equals(entry.getHostName()))){
			return false;
		}
		if(concurrentSessions != entry.getConcurrentSessions() || Double.doubleToLongBits(byteCount) != Double.doubleToLongBits(entry.getByteCount())){
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(byteCount);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		result = prime * result + concurrentSessions;
		result = prime * result + ipValue.hashCode();
		result = prime * result + hostName.hashCode();
		return result;
	}
}
