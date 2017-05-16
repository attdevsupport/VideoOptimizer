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

public class TransmissionPrivateDataEntry {

	private String destIP;
	private String domainName;
	private int destPort;
	private String privateDataType;
	private String privateDataTxt;
	private double sessionStartTime;
	
	public String getDestIP() {
		return destIP;
	}
	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}
	public String getDomainName() {
		return domainName;
	}
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
	public int getDestPort() {
		return destPort;
	}
	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}
	public String getPrivateDataType() {
		return privateDataType;
	}
	public void setPrivateDataType(String privateDataType) {
		this.privateDataType = privateDataType;
	}
	public String getPrivateDataTxt() {
		return privateDataTxt;
	}
	public void setPrivateDataTxt(String privateDataTxt) {
		this.privateDataTxt = privateDataTxt;
	}
	public double getSessionStartTime() {
		return sessionStartTime;
	}
	public void setSessionStartTime(double sessionStartTime) {
		this.sessionStartTime = sessionStartTime;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((destIP == null) ? 0 : destIP.hashCode());
		result = prime * result + destPort;
		result = prime * result + ((domainName == null) ? 0 : domainName.hashCode());
		result = prime * result + ((privateDataTxt == null) ? 0 : privateDataTxt.hashCode());
		result = prime * result + ((privateDataType == null) ? 0 : privateDataType.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TransmissionPrivateDataEntry other = (TransmissionPrivateDataEntry) obj;
		if (destIP == null) {
			if (other.destIP != null) {
				return false;
			}
		} else if (!destIP.equals(other.destIP)) {
			return false;
		}
		if (destPort != other.destPort) {
			return false;
		}
		if (domainName == null) {
			if (other.domainName != null) {
				return false;
			}
		} else if (!domainName.equals(other.domainName)) {
			return false;
		}
		if (privateDataTxt == null) {
			if (other.privateDataTxt != null) {
				return false;
			}
		} else if (!privateDataTxt.equals(other.privateDataTxt)) {
			return false;
		}
		if (privateDataType == null) {
			if (other.privateDataType != null) {
				return false;
			}
		} else if (!privateDataType.equals(other.privateDataType)) {
			return false;
		}
		return true;
	}
}
