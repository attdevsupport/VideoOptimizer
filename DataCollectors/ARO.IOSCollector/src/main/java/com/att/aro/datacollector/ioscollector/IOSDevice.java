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
package com.att.aro.datacollector.ioscollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.util.StringParse;
import com.att.aro.datacollector.ioscollector.utilities.DeviceVideoHandler;
import com.att.aro.datacollector.ioscollector.utilities.IOSDeviceInfo;

import lombok.Getter;
import lombok.Setter;

/**
 * Utility class for mapping device information retrieved from the libimobiledevice library
 * Commend: ideviceinfo -u [device's UDID]
 */
@Setter
@Getter
public class IOSDevice implements IAroDevice {

	private static final Logger LOG = LogManager.getLogger(IOSDevice.class.getName());

	private String udid;		// UDID is a 40-digit hexadecimal number of the device
 	private String deviceClass; // : iPhone
	private String deviceName; // : aro team’s iPhone
	private String productVersion; // : 12.1.4
	private String productName; // : iPhone OS 
	private String productType; // : iPhone 10,6 -> iPhone X
	private String abi;			// : arm64
	
	private Double deviceTimestamp = 0D;	// 1653522755.586348
	private String deviceTimeZoneID;	// America/Los_Angeles

	private boolean timingOffset;
	
	private String voTimeZoneID;
	private double voTimestamp;
	
	private AroDeviceState state = AroDeviceState.Unknown ;	   // Available, in-use, Unknown         

	private IDataCollector collector;
	private IOSDeviceInfo deviceinfo;

	@Setter
	@Getter
	private List<String[]> ipAddressList;
	
	@Setter
	@Getter
	private List<String[]> voIpAddressList;
	
	public IOSDevice(String udid) throws IOException {
		this.udid = udid;

		deviceinfo = new IOSDeviceInfo();
		String data = deviceinfo.getDeviceData(udid);
		Map<String, String> profile = stringToMap(data);
		deviceClass = profile.get("DeviceClass");
		deviceName = profile.get("DeviceName");
		productVersion = profile.get("ProductVersion");
		productName = profile.get("ProductName");
		productType = profile.get("ProductType");
		
		deviceTimeZoneID = profile.get("TimeZone");
		
		abi = profile.get("CPUArchitecture");
		state = "Activated".equals(profile.get("ActivationState")) ? AroDeviceState.Available : AroDeviceState.Unknown;
		
		if (productName == null) {
			productName = "iosDevice";
			if (productType != null) {
				String[] splt = productType.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
				if (splt != null) {
					productName = splt[0];
				}
			}
		}
	}

	@Override
	public Double obtainDeviceTimestamp() {
		try {
			String data = deviceinfo.getDeviceData(udid);
			Map<String, String> profile = stringToMap(data);
			String currentUTC = profile.get("TimeIntervalSince1970");
			deviceTimestamp = StringParse.stringToDouble(currentUTC, 0D);
		} catch (IOException e) {
			LOG.error("Failed to obtain current UTC time:", e);
		}
		return deviceTimestamp;
	}
	
	@Override
	public double getTimeDiff() {
		return voTimestamp - deviceTimestamp;
	}
	
	private String getProductVersion() {
		int iosVersion = DeviceVideoHandler.getInstance().getIosVersion();
		if (iosVersion != -1) {
			return String.valueOf(iosVersion);
		} else {
			return null;
		}
	}
	
	private Map<String, String> stringToMap(String data) {
		Map<String, String> mapped = new HashMap<String, String>();
		String[] arr = data.split("\\r?\\n");
		String[] tokens;
		String key, value;
		for (String line : arr) {
			tokens = line.split(":");
			if (tokens.length > 1) {
				key = tokens[0].trim();
				value = tokens[1].trim();
				mapped.put(key, value);
			}
		}
		return mapped;
	}
	
	@Override
	public String getId() {
		return udid;
	}

	@Override
	public String getOS() {
		return productName.equals("iPhone OS") ? "iOS" : productName;
	}

	@Override
	public String getApi() {
		if (productVersion == null) {
			productVersion = getProductVersion();
		}
		return productVersion;
	}

	@Override
	public String getModel() {
		return productType;
	}

	public String getDeviceClass() {
		return deviceClass;
	}

	@Override
	public Platform getPlatform() {
		return Platform.iOS;
	}

	@Override
	public boolean isRooted() {
		return false;
	}

	@Override
	public boolean isEmulator() {
		return false;
	}
	
	/**
	 * Ignore in iOS
	 */
	@Override
	public String getAbi() {
		return abi;
	}
	
	@Override
	public boolean isPlatform(Platform platform) {
		return (Platform.iOS.equals(platform));
	}	

	@Override
	public void setCollector(IDataCollector collectorOption) {
		this.collector = collectorOption;
		
	}

	@Override
	public IDataCollector getCollector() {
		return this.collector;
	}

	@Override
	public Object getDevice() {
		return null;
	}

	@Override
	public void setStatus(AroDeviceState state) {
		this.state = state;
		
	}
	
	@Override
	public String toString() {
		return new String(getDeviceClass()
				+ " iOS " + getApi()
				+ ", deviceTimeZoneID:" + getDeviceTimeZoneID()
				+ ", deviceTimestamp:" + getDeviceTimestamp()
				+ ", udid:" + getId() 
				+ ", api:" + getApi() 
				+ ", abi:" + getAbi() 
				+ ", model:" + getModel() 
				+ ", DevName:" + getDeviceName());
	}

	@Override
	public List<String[]> obtainDeviceIpAddress() {
		// return empty list, as not yet collected
		return new ArrayList<>();
	}

}
