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

public class ImageMdataEntry {
	private long imageSize;
	private double timeStamp;
	private String httpObjName = "";
	private String hostName = "";
	private int httpCode;
	private HttpRequestResponseInfo httpReqResp;
	private long formattedSize;
	private String percentSavings;

	public ImageMdataEntry(HttpRequestResponseInfo reqRespInfo, String domainName, String imagefile, long iSize,
			long fSize, String savings) {

		this.httpReqResp = reqRespInfo;
		this.httpObjName = imagefile;

		this.imageSize = iSize;
		this.formattedSize = fSize;

		this.timeStamp = reqRespInfo.getTimeStamp();
		this.httpCode = reqRespInfo.getStatusCode();
		this.percentSavings = savings;

		assignHostAndHttpInfo(reqRespInfo,domainName);

	}

	private void assignHostAndHttpInfo(HttpRequestResponseInfo reqRespInfo, String domainName) {
		HttpRequestResponseInfo respons = reqRespInfo.getAssocReqResp();
		if (respons != null) {
			if (respons.getHostName() == null || respons.getHostName().isEmpty()) {
				if (reqRespInfo.getHostName() == null || reqRespInfo.getHostName().isEmpty()) {
					this.hostName = domainName;
				} else {
					this.hostName = reqRespInfo.getHostName();
				}
			} else {
				this.hostName = respons.getHostName();
			}
			this.httpObjName = respons.getObjName();
		} else {
			if (reqRespInfo.getHostName() == null || reqRespInfo.getHostName().isEmpty()) {
				this.hostName = domainName;
			} else {
				this.hostName = reqRespInfo.getHostName();
			}
		}
	}

	public long getImageSize() {
		return imageSize;
	}

	public void setImageSize(long imageSize) {
		this.imageSize = imageSize;
	}

	public long getFormattedSize() {
		return formattedSize;
	}

	public void setFormattedSize(long formattedSize) {
		this.formattedSize = formattedSize;
	}

	public String getPercentSavings() {
		return percentSavings;
	}

	public void setPercentSavings(String percentSavings) {
		this.percentSavings = percentSavings;
	}

	public double getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(double timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getHttpObjectName() {
		return httpObjName;
	}

	public void setHttpObjectName(String httpObjectName) {
		this.httpObjName = httpObjectName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getHttpCode() {
		return httpCode;
	}

	public void setHttpCode(int httpCode) {
		this.httpCode = httpCode;
	}

	public HttpRequestResponseInfo getHttpRequestResponse() {
		return httpReqResp;
	}

	public void setHttpRequestResponse(HttpRequestResponseInfo httpRequestResponse) {
		this.httpReqResp = httpRequestResponse;
	}

}
