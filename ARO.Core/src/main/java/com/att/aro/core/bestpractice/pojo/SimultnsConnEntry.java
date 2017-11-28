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

public class SimultnsConnEntry {
	private double startTimeStamp;
	private double endTimeStamp;
	private int httpStatusCode;
	private int concurrentSessions;
	private String httpReqObjName = "";
	private String hostName = "";
	private String ipValue= "";
	private HttpRequestResponseInfo httpReqRespInfo;
	
	public SimultnsConnEntry(HttpRequestResponseInfo httpReqRespInfo, String domainName,int concurrentSessions, double start, double end) {

		this.httpReqRespInfo = httpReqRespInfo;
		this.httpReqObjName = domainName;
		this.startTimeStamp = Math.round (start * 1000.0) / 1000.0;
		this.endTimeStamp = Math.round (end * 1000.0) / 1000.0;
		this.httpStatusCode = httpReqRespInfo.getStatusCode();
		this.concurrentSessions=concurrentSessions;
		this.ipValue=domainName;
		
		assignHostAndHttpInfo(httpReqRespInfo,domainName);
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
		if(hostNameObj.contains("/")) {
			this.hostName= hostNameObj.substring(hostNameObj.lastIndexOf('/') + 1, hostNameObj.length());		
		}
	}
	
	

	public double getStartTimeStamp() {
		return startTimeStamp;
	}

	public void setStartTimeStamp(double startTimeStamp) {
		this.startTimeStamp = startTimeStamp;
	}

	public double getEndTimeStamp() {
		return endTimeStamp;
	}

	public void setEndTimeStamp(double endTimeStamp) {
		this.endTimeStamp = endTimeStamp;
	}

	public double getTimeStamp() {
		return startTimeStamp;
	}

	public void setTimeStamp(double timeStamp) {
		this.startTimeStamp = timeStamp;
	}

	public String getHttpObjName() {
		return httpReqObjName;
	}

	public void setHttpObjName(String httpObjName) {
		this.httpReqObjName = httpObjName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}


	public int getConcurrentSessions() {
		return concurrentSessions;
	}

	public void setConcurrentSessions(int concurrentSessions) {
		this.concurrentSessions = concurrentSessions;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}


	public HttpRequestResponseInfo getHttpReqRespInfo() {
		return httpReqRespInfo;
	}

	public void setHttpReqRespInfo(HttpRequestResponseInfo httpReqRespInfo) {
		this.httpReqRespInfo = httpReqRespInfo;
	}
	
	public String getHttpReqObjName() {
		return httpReqObjName;
	}

	public void setHttpReqObjName(String httpReqObjName) {
		this.httpReqObjName = httpReqObjName;
	}



	public String getIpValue() {
		return ipValue;
	}
	
	
}
