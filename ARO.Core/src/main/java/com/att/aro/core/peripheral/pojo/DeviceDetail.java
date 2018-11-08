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
package com.att.aro.core.peripheral.pojo;

/**
 * 
 * Date: October 7, 2014
 */
public class DeviceDetail {
	
	private String collectorName = "";
	private String deviceModel = "";
	private String deviceMake = "";
	private String osType = "";
	private String osVersion = "";
	private String collectorVersion = "";
	private int totalLines = 0;
	private String screenSize = "0x0";
	public String getCollectorName() {
		return collectorName;
	}
	public void setCollectorName(String collectorName) {
		this.collectorName = collectorName;
	}
	public String getDeviceModel() {
		return deviceModel;
	}
	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}
	public String getDeviceMake() {
		return deviceMake;
	}
	public void setDeviceMake(String deviceMake) {
		this.deviceMake = deviceMake;
	}
	public String getOsType() {
		return osType;
	}
	public void setOsType(String osType) {
		this.osType = osType;
	}
	public String getOsVersion() {
		return osVersion;
	}
	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}
	public String getCollectorVersion() {
		return collectorVersion;
	}
	public void setCollectorVersion(String collectorVersion) {
		this.collectorVersion = collectorVersion;
	}
	public int getTotalLines() {
		return totalLines;
	}
	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}
	public String getScreenSize() {
		return screenSize;
	}
	public void setScreenSize(String screenSize) {
		this.screenSize = screenSize;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collectorName == null) ? 0 : collectorName.hashCode());
		result = prime * result + ((collectorVersion == null) ? 0 : collectorVersion.hashCode());
		result = prime * result + ((deviceMake == null) ? 0 : deviceMake.hashCode());
		result = prime * result + ((deviceModel == null) ? 0 : deviceModel.hashCode());
		result = prime * result + ((osType == null) ? 0 : osType.hashCode());
		result = prime * result + ((osVersion == null) ? 0 : osVersion.hashCode());
		result = prime * result + ((screenSize == null) ? 0 : screenSize.hashCode());
		result = prime * result + totalLines;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DeviceDetail other = (DeviceDetail) obj;
		if (collectorName == null) {
			if (other.collectorName != null) {
				return false;
			}
		} else if (!collectorName.equals(other.collectorName)) {
			return false;
		}
		if (collectorVersion == null) {
			if (other.collectorVersion != null) {
				return false;
			}
		} else if (!collectorVersion.equals(other.collectorVersion)) {
			return false;
		}
		if (deviceMake == null) {
			if (other.deviceMake != null) {
				return false;
			}
		} else if (!deviceMake.equals(other.deviceMake)) {
			return false;
		}
		if (deviceModel == null) {
			if (other.deviceModel != null) {
				return false;
			}
		} else if (!deviceModel.equals(other.deviceModel)) {
			return false;
		}
		if (osType == null) {
			if (other.osType != null) {
				return false;
			}
		} else if (!osType.equals(other.osType)) {
			return false;
		}
		if (osVersion == null) {
			if (other.osVersion != null) {
				return false;
			}
		} else if (!osVersion.equals(other.osVersion)){
			return false;
		}
		if (screenSize == null) {
			if (other.screenSize != null) {
				return false;
			}
		} else if (!screenSize.equals(other.screenSize)) {
			return false;
		}
		if (totalLines != other.totalLines) {
			return false;
		}
		return true;
	}
	
}
