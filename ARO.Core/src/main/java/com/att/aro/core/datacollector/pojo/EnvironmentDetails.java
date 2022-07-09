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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.datacollector.pojo;


import java.util.Date;
import java.util.ResourceBundle;

import com.att.aro.core.util.Util;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentDetails {

	private final String tracePath;
	private final String date;
	private final String voVersion;
	private final String vpnVersion;
	private final String osName;
	private final String osVersion;
	private final String jdkVersion;
	private DeviceInfo deviceInfo;

	// MacOS specific
	private String xcodeVersion;
	private String dumpcapVersion;
	private String libimobiledeviceVersion;


	@Getter @Setter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private class DeviceInfo {
		private String version;
		private String platform;
		private Boolean rooted;

		public DeviceInfo(String version, Boolean rooted, String platform) {
			this.version = version;
			this.rooted = rooted;
			this.platform = platform;
		}
	}

	public EnvironmentDetails(String tracePath) {
		this.tracePath = tracePath;
		date = new Date().toString();
		voVersion = ResourceBundle.getBundle("build").getString("build.majorversion") + "." + ResourceBundle.getBundle("build").getString("build.timestamp");
		vpnVersion = Util.APK_FILE_NAME;
		osName = Util.OS_NAME + " " + Util.OS_ARCHITECTURE;
		osVersion = Util.OS_VERSION;
		jdkVersion = Util.JDK_VERSION;
	}

	public void populateDeviceInfo(String version, Boolean rooted, String platform) {
		if (deviceInfo == null) {
			deviceInfo = new DeviceInfo(version, rooted, platform);
		} else {
			deviceInfo.version = version;
			deviceInfo.rooted = rooted;
			deviceInfo.platform = platform;
		}
	}

	public void populateMacOSDetails(String xcodeVersion, String dumpcapVersion, String libimobiledeviceVersion) {
		this.xcodeVersion = xcodeVersion;
		this.dumpcapVersion = dumpcapVersion;
		this.libimobiledeviceVersion = libimobiledeviceVersion;
	}
}
