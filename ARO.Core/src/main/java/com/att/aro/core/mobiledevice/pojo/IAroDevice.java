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
package com.att.aro.core.mobiledevice.pojo;

import java.util.List;

import com.att.aro.core.datacollector.IDataCollector;

public interface IAroDevice {
	
	public enum AroDeviceState {
		Available, in_use, Offline, Unauthorized, Unknown, Invalid
	}

	public enum Platform {
		Android, iOS, Unknown
	}

	/**
	 * Returns os of device Android or iOS
	 * 
	 * @return os of device Android or iOS
	 */
	Platform getPlatform();

	boolean isPlatform(Platform platform);

	boolean isEmulator();

	String getAbi();

	AroDeviceState getState();

	String getId();

	String getDeviceName();

	String getOS();

	String getApi();

	String getProductName();

	String getModel();

	boolean isRooted();

	void setCollector(IDataCollector collectorOption);

	IDataCollector getCollector();

	Object getDevice();

	void setStatus(AroDeviceState status);
	
	String getDeviceTimeZoneID();
	Double obtainDeviceTimestamp();
	Double getDeviceTimestamp();

	void setTimingOffset(boolean timingOffset);
	double getTimeDiff();
	boolean isTimingOffset();
	
	void setVoTimeZoneID(String voTimeZoneID);
	String getVoTimeZoneID();
	
	void setVoTimestamp(double voCurrentUTC);
	double getVoTimestamp();

	List<String[]> obtainDeviceIpAddress();
	List<String[]> getIpAddressList();
	List<String[]> getVoIpAddressList();
	void setIpAddressList(List<String[]> ipList);
	void setVoIpAddressList(List<String[]> voIpAddressList);
	
}
