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
package com.att.aro.core.packetanalysis.pojo;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * A bean class that contains one cache entry, and provides methods for
 * returning the information from that entry.
 */
public class DuplicateEntry {
	private HttpRequestResponseInfo assocRequest;
	private HttpRequestResponseInfo assocResponse;
	private Diagnosis diagnosis;
	private PacketInfo sessionFirstPacket;
	private String hostName;
	private long contentLength;
	private Double timeStamp;
	private HttpRequestResponseInfo httpRequestResponse;
	private String httpObjectName;
	private byte[] content;
	private int count;
	@JsonIgnore
	private Session session;

	@JsonIgnore
	public Session getSession() {
		return session;
	}

	public DuplicateEntry(HttpRequestResponseInfo assocRequest, HttpRequestResponseInfo assocResponse,
			Diagnosis diagnosis, PacketInfo sessionFirstPacket, Session session, byte[] content) {
		if (assocRequest != null) {
			this.assocRequest = assocRequest;
		}
		if (assocRequest != null) {
			this.timeStamp = 0.0;// request.getTimeStamp();
			this.httpObjectName = assocRequest.getObjName();
			this.hostName = assocRequest.getHostName();
			httpRequestResponse = assocRequest.getAssocReqResp();
		} else {
			this.httpObjectName = "";
			this.hostName = "";
		}
		// Response cannot be null
		this.session = session;
		this.assocResponse = assocResponse;
		this.contentLength = assocResponse.getContentLength();
		this.diagnosis = diagnosis;
		this.sessionFirstPacket = sessionFirstPacket;
		this.content = content;
	}

	public byte[] getContent() {
		return content;
	}

	public HttpRequestResponseInfo getRequest() {
		return assocRequest;
	}

	public HttpRequestResponseInfo getResponse() {
		return assocResponse;
	}

	public Diagnosis getDiagnosis() {
		return diagnosis;
	}

	public PacketInfo getSessionFirstPacket() {
		return sessionFirstPacket;
	}

	public String getHostName() {
		return hostName;
	}

	public long getContentLength() {
		return contentLength;
	}

	public Double getTimeStamp() {
		return timeStamp;
	}

	public HttpRequestResponseInfo getHttpRequestResponse() {
		return httpRequestResponse;
	}

	public String getHttpObjectName() {
		return httpObjectName;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}