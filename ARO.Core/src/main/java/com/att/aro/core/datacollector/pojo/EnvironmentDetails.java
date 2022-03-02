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
