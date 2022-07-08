/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.peripheral.pojo;

import static java.lang.Integer.parseInt;

import java.util.Properties;

import com.att.aro.core.video.pojo.Orientation;

public class CollectOptions {
	
	private int dsDelay = 0;
	private int usDelay = 0;
	// -1 is no speed throttle (and default) 
	private int throttleDL = -1;  
	private int throttleUL = -1; 
 	private boolean attnrProfile = false;
	private String attnrProfileName = "";
	private SecureStatus secureStatus = SecureStatus.UNKNOWN;
	private int totalLines = 0;
	private Orientation orientation = Orientation.PORTRAIT;

	public CollectOptions() {
	}


	public CollectOptions(Properties properties) {
		dsDelay = parseInt(properties.getProperty("dsDelay", "0"));
		usDelay = parseInt(properties.getProperty("usDelay", "0"));
		throttleDL = parseInt(properties.getProperty("throttleDL", "0"));
		throttleUL = parseInt(properties.getProperty("throttleUL", "0"));
 		secureStatus = SecureStatus.valueOf(properties.getProperty("secure", "UNKNOWN").toUpperCase());
		orientation = Orientation.valueOf(properties.getProperty("orientation", "PORTRAIT").toUpperCase());
		attnrProfile = Boolean.valueOf(properties.getProperty("attnrProfile", "false"));
		attnrProfileName = properties.getProperty("attnrProfileName", "");
	}

	public String getOrientation() {
		return orientation.name();
	}

	public void setOrientation(String orientation) {
		this.orientation = Orientation.valueOf(orientation);
	}

	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}

	public enum SecureStatus {
		UNKNOWN, TRUE, FALSE;
	}

	public int getUsDelay() {
		return usDelay;
	}

	public void setUsDelay(int usDelay) {
		this.usDelay = usDelay;
	}

	public SecureStatus getSecureStatus() {
		return secureStatus;
	}

	public void setSecureStatus(SecureStatus secureStatus) {
		this.secureStatus = secureStatus;
	}

	public int getDsDelay() {
		return dsDelay;
	}

	public void setDsDelay(int dsDelay) {
		this.dsDelay = dsDelay;
	}

	public int getThrottleDL() {
		return throttleDL;
	}

	public void setThrottleDL(int throttleDL) {
		this.throttleDL = throttleDL;
	}

	public int getThrottleUL() {
		return throttleUL;
	}

	public void setThrottleUL(int throttleUL) {
		this.throttleUL = throttleUL;
	}

	public int getTotalLines() {
		return totalLines;
	}

	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}

	public boolean isAttnrProfile() {
		return attnrProfile;
	}

	public void setAttnrProfile(boolean attnrProfile) {
		this.attnrProfile = attnrProfile;
	}

	public String getAttnrProfileName() {
		return attnrProfileName;
	}

	public void setAttnrProfileName(String attnrProfileName) {
		this.attnrProfileName = attnrProfileName;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (attnrProfile ? 1231 : 1237);
		result = prime * result + ((attnrProfileName == null) ? 0 : attnrProfileName.hashCode());
		result = prime * result + dsDelay;
		result = prime * result + ((orientation == null) ? 0 : orientation.hashCode());
		result = prime * result + ((secureStatus == null) ? 0 : secureStatus.hashCode());
		result = prime * result + throttleDL;
		result = prime * result + throttleUL;
		result = prime * result + totalLines;
		result = prime * result + usDelay;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()){
			return false;
		}
		CollectOptions other = (CollectOptions) obj;
		if (attnrProfile != other.attnrProfile) {
			return false;
		}
		if (attnrProfileName == null) {
			if (other.attnrProfileName != null) {
				return false;
			}
		} else if (!attnrProfileName.equals(other.attnrProfileName)) {
			return false;
		}
		if (dsDelay != other.dsDelay) {
			return false;
		}
		if (orientation != other.orientation) {
			return false;
		}
		if (secureStatus != other.secureStatus) {
			return false;
		}
		if (throttleDL != other.throttleDL) {
			return false;
		}
		if (throttleUL != other.throttleUL) {
			return false;
		}
		if (totalLines != other.totalLines) {
			return false;
		}
		if (usDelay != other.usDelay) {
			return false;
		}
		return true;
	}
 
}
