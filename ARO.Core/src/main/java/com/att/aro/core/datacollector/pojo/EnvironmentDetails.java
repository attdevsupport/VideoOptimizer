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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;

import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.util.Util;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private final String voTimeZoneID;
	private final Double voTimestamp;
	private String xcodeVersion;
	private String dumpcapVersion;
	private String libimobiledeviceVersion;
	private DeviceInfo deviceInfo;
	private ArrayList<EnvIPAddress> voIpAddressList = new ArrayList<>();
	
	@Getter
	@Setter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public class DeviceInfo {
			private String version;
			private String platform;
			private Boolean rooted;
			private boolean timingOffset;
			private String deviceTimeZoneID;
			private double deviceTimestamp;
			private double timeDiff;
			private ArrayList<String> localIpAddressList = new ArrayList<>();		// phone local addresses, VPN addresses will go here
			private ArrayList<EnvIPAddress> networkInterfaceList = new ArrayList<>();	// phone network interface list, as in ifconfig

		public DeviceInfo() {
		}
	}

	public EnvironmentDetails(String tracePath, IAroDevice aroDevice) {
		this.tracePath = tracePath;
		date = new Date().toString();
		voVersion = ResourceBundle.getBundle("build").getString("build.majorversion") + "." + ResourceBundle.getBundle("build").getString("build.timestamp");
		vpnVersion = Util.APK_FILE_NAME;
		osName = Util.OS_NAME + " " + Util.OS_ARCHITECTURE;
		osVersion = Util.OS_VERSION;
		jdkVersion = Util.JDK_VERSION;
		voTimestamp = aroDevice.getVoTimestamp();
		voTimeZoneID = aroDevice.getVoTimeZoneID();
		populateDeviceInfo(aroDevice);
	}

	public EnvironmentDetails() {
		tracePath = "";
		date = "";
		voVersion = "";
		vpnVersion = "";
		osName = "";
		osVersion = "";
		jdkVersion = "";
		voTimeZoneID = "";
		voTimestamp = 0D;
		deviceInfo = new DeviceInfo();
	}

	public void populateDeviceInfo(IAroDevice aroDevice) {
		if (deviceInfo == null) {
			deviceInfo = new DeviceInfo();
		}
		deviceInfo.setVersion(aroDevice.getOS());
		deviceInfo.setRooted(aroDevice.isRooted());
		deviceInfo.setPlatform(aroDevice.getPlatform().toString());
		deviceInfo.setDeviceTimeZoneID(aroDevice.getDeviceTimeZoneID());
		deviceInfo.setDeviceTimestamp(aroDevice.getDeviceTimestamp());
		deviceInfo.setTimeDiff(aroDevice.getTimeDiff());
		List<EnvIPAddress> deviceNetworkInterfaceList = deviceInfo.getNetworkInterfaceList();
		if (aroDevice.getIpAddressList() != null) {
			aroDevice.getIpAddressList().forEach(e -> {
				deviceNetworkInterfaceList.add(new EnvIPAddress(e[0], e[1], e[2]));
				deviceInfo.getLocalIpAddressList().add(Util.expandAddress(e[2]));
			});
		}		
		List<EnvIPAddress> voIpAddressList = getVoIpAddressList();
		if (aroDevice.getVoIpAddressList() != null) {
			aroDevice.getVoIpAddressList().forEach(e -> {
				voIpAddressList.add(new EnvIPAddress(e[0], e[1], e[2]));
			});
		}
	}

	public void populateMacOSDetails(String xcodeVersion, String dumpcapVersion, String libimobiledeviceVersion) {
		this.xcodeVersion = xcodeVersion;
		this.dumpcapVersion = dumpcapVersion;
		this.libimobiledeviceVersion = libimobiledeviceVersion;
	}
	
	@Override
	public String toString() {
		String prettyPrint;
		try {
			prettyPrint = (new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			prettyPrint = "failed to serialize:" + e.getMessage();
		}
		return prettyPrint;
	}
}
