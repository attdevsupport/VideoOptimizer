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

public class ImageCompressionEntry {
	private double orgImageSize;
	private double imgTimeStamp;
	private String httpObjtName = "";
	private String serverHostName = "";
	private int imgHttpCode;
	private HttpRequestResponseInfo httpReqRes;
	private double sizeE;
	private double sizeS;

	public ImageCompressionEntry(HttpRequestResponseInfo reqResInf, String domainName, String imagefile, double iSize,
			double sizeE, double sizeS) {

		this.httpReqRes = reqResInf;
		this.httpObjtName = imagefile;

		this.orgImageSize = iSize;
		this.sizeE = sizeE;

		this.imgTimeStamp = reqResInf.getTimeStamp();
		this.imgHttpCode = reqResInf.getStatusCode();
		this.sizeS = sizeS;

		assignHostAndHttpObjInfo(reqResInf,domainName);
	}


	private void assignHostAndHttpObjInfo(HttpRequestResponseInfo reqResInf, String domainName) {
		HttpRequestResponseInfo reqResponseInfo = reqResInf.getAssocReqResp();
		if (reqResponseInfo != null) {
			if (reqResponseInfo.getHostName() == null || reqResponseInfo.getHostName().isEmpty()) {
				if (reqResInf.getHostName() == null || reqResInf.getHostName().isEmpty()) {
					this.serverHostName = domainName;
				} else {
					this.serverHostName = reqResInf.getHostName();
				}
			} else {
				this.serverHostName = reqResponseInfo.getHostName();
			}
			this.httpObjtName = reqResponseInfo.getObjName();
		} else {
			if (reqResInf.getHostName() == null || reqResInf.getHostName().isEmpty()) {
				this.serverHostName = domainName;
			} else {
				this.serverHostName = reqResInf.getHostName();
			}

		}
	}
	
	
	public double getOrgImageSize() {
		return orgImageSize;
	}


	public void setOrgImageSize(long orgImageSize) {
		this.orgImageSize = orgImageSize;
	
	}


	public double getSizeE() {
		return sizeE;
	}

	public double getSizeS() {
		return sizeS;
	}


	public void setSizeS(long sizeS) {
		this.sizeS = sizeS;
	}

	public double getTimeStamp() {
		return imgTimeStamp;
	}

	public void setTimeStamp(double timeStamp) {
		this.imgTimeStamp = timeStamp;
	}

	public String getHttpObjectName() {
		return httpObjtName;
	}

	public void setHttpObjectName(String httpObjectName) {
		this.httpObjtName = httpObjectName;
	}

	public String getHostName() {
		return serverHostName;
	}

	public void setHostName(String hostName) {
		this.serverHostName = hostName;
	}

	public int getHttpCode() {
		return imgHttpCode;
	}

	public void setHttpCode(int httpCode) {
		this.imgHttpCode = httpCode;
	}

	public HttpRequestResponseInfo getHttpRequestResponse() {
		return httpReqRes;
	}

	public void setHttpRequestResponse(HttpRequestResponseInfo httpRequestResponse) {
		this.httpReqRes = httpRequestResponse;
	}

}
