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

public class WeakCipherEntry {

	private String destIP;
	private String destPort;
	private String weakCipherName;
	private String weakCipherHex;
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
		return weakCipherName;
	}
	public void setCipherName(String cipherName) {
		this.weakCipherName = cipherName;
	}
	public String getCipherHex() {
		return weakCipherHex;
	}
	public void setCipherHex(String cipherHex) {
		this.weakCipherHex = cipherHex;
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
		result = prime * result + ((weakCipherHex == null) ? 0 : weakCipherHex.hashCode());
		result = prime * result + ((weakCipherName == null) ? 0 : weakCipherName.hashCode());
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
		WeakCipherEntry other = (WeakCipherEntry) obj;
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
		if (weakCipherHex == null) {
			if (other.weakCipherHex != null) {
				return false;
			}
		} else if (!weakCipherHex.equals(other.weakCipherHex)) {
			return false;
		}
		if (weakCipherName == null) {
			if (other.weakCipherName != null) {
				return false;
			}
		} else if (!weakCipherName.equals(other.weakCipherName)) {
			return false;
		}
		return true;
	}
}
