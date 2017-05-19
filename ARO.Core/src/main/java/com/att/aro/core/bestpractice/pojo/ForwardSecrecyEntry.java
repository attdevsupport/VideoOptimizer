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

public class ForwardSecrecyEntry {

	private String destIP;
	private String destPort;
	private String cipherName;
	private String cipherHex;
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
	public String getCipherName() {
		return cipherName;
	}
	public void setCipherName(String cipherName) {
		this.cipherName = cipherName;
	}
	public String getCipherHex() {
		return cipherHex;
	}
	public void setCipherHex(String cipherHex) {
		this.cipherHex = cipherHex;
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
		result = prime * result + ((cipherHex == null) ? 0 : cipherHex.hashCode());
		result = prime * result + ((cipherName == null) ? 0 : cipherName.hashCode());
		result = prime * result + ((destIP == null) ? 0 : destIP.hashCode());
		result = prime * result + ((destPort == null) ? 0 : destPort.hashCode());
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
		ForwardSecrecyEntry other = (ForwardSecrecyEntry) obj;
		if (cipherHex == null) {
			if (other.cipherHex != null) {
				return false;
			}
		} else if (!cipherHex.equals(other.cipherHex)) {
			return false;
		}
		if (cipherName == null) {
			if (other.cipherName != null) {
				return false;
			}
		} else if (!cipherName.equals(other.cipherName)) {
			return false;
		}
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
		return true;
	}
}
