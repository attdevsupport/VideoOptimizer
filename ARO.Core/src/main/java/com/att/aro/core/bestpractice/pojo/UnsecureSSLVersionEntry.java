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

public class UnsecureSSLVersionEntry {

	private String destIP;
	private String destPort;
	private String unsecureSSLVersions;
	private double sessionStartTime;
	
	public String getDestIP() {
		return destIP;
	}
	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}
	public String getDestPort() {
		return destPort;
	}
	public void setDestPort(String destPort) {
		this.destPort = destPort;
	}
	public String getUnsecureSSLVersions() {
		return unsecureSSLVersions;
	}
	public void setUnsecureSSLVersions(String unsecureSSLVersions) {
		this.unsecureSSLVersions = unsecureSSLVersions;
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
		result = prime * result + ((destPort == null) ? 0 : destPort.hashCode());
		result = prime * result + ((unsecureSSLVersions == null) ? 0 : unsecureSSLVersions.hashCode());
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
		UnsecureSSLVersionEntry other = (UnsecureSSLVersionEntry) obj;
		if (destIP == null) {
			if (other.destIP != null) {
				return false;
			}
		} else if (!destIP.equals(other.destIP)) {
			return false;
		}
		if (destPort == null) {
			if (other.destPort != null) {
				return false;
			}
		} else if (!destPort.equals(other.destPort)) {
			return false;
		}
		if (unsecureSSLVersions == null) {
			if (other.unsecureSSLVersions != null) {
				return false;
			}
		} else if (!unsecureSSLVersions.equals(other.unsecureSSLVersions)) {
			return false;
		}
		return true;
	}
}
