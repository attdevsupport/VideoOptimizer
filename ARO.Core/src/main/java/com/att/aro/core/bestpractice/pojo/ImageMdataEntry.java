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

public class ImageMdataEntry {
	
	@JsonIgnore
	private int imgSize; 
	@JsonIgnore
	private int formatdSize;
	private double imageSize;
	private double timeStamp;
	private String httpObjName = "";
	private String hostName = "";
	private int httpCode;
	@JsonIgnore
	private HttpRequestResponseInfo httpRequestResponse;
	private double formattedSize;
	private String percentSavings;

	public ImageMdataEntry(){}
	
	public ImageMdataEntry(HttpRequestResponseInfo reqRespInfo, String domainName, String imagefile, double iSize,
			double fSize, String savings) {

		this.httpRequestResponse = reqRespInfo;
		this.httpObjName = imagefile;

		this.imageSize = iSize;
		this.formattedSize = fSize;

		this.timeStamp = reqRespInfo.getTimeStamp();
		this.httpCode = reqRespInfo.getStatusCode();
		this.percentSavings = savings;

		assignHostAndHttpInfo(reqRespInfo,domainName);

	}

	public ImageMdataEntry(HttpRequestResponseInfo reqRespInfo, String imagefile, int iSize,
			int fSize, String savings) {

		this.httpRequestResponse = reqRespInfo;
		this.httpObjName = imagefile;

		this.imgSize = iSize;
		this.formatdSize = fSize;

		this.timeStamp = reqRespInfo.getTimeStamp();
		this.httpCode = reqRespInfo.getStatusCode();
		this.percentSavings = savings;

		assignHostAndHttpInfo(reqRespInfo,"");
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
	
	public int getImgSize() {
		return imgSize;
	}

	public void setImgSize(int imgSize) {
		this.imgSize = imgSize;
	}

	public int getFormatdSize() {
		return formatdSize;
	}

	public void setFormatdSize(int formatdSize) {
		this.formatdSize = formatdSize;
	}

	public double getImageSize() {
		return imageSize;
	}

	public void setImageSize(double imageSize) {
		this.imageSize = imageSize;
	}

	public double getFormattedSize() {
		return formattedSize;
	}

	public void setFormattedSize(double formattedSize) {
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

	@JsonIgnore
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
		ImageMdataEntry other = (ImageMdataEntry) obj;
		if (Double.doubleToLongBits(other.getFormattedSize()) != Double.doubleToLongBits(formattedSize)) {
			return false;
		}
		if (Double.doubleToLongBits(imageSize) != Double.doubleToLongBits(other.getImageSize())) {
			return false;
		}
		if (Double.doubleToLongBits(timeStamp) != Double.doubleToLongBits(other.getTimeStamp())) {
			return false;
		}
		if (!other.getHostName().equals(hostName) || (!other.getHttpObjectName().equals(httpObjName))) {
			return false;
		}
		if (other.getHttpCode() != httpCode || (!other.getPercentSavings().equals(percentSavings))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		result = prime * result + httpCode;
		result = prime * result + hostName.hashCode();
		result = prime * result + httpObjName.hashCode();
		result = prime * result + percentSavings.hashCode();
		temp = Double.doubleToLongBits(formattedSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(imageSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(timeStamp);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
