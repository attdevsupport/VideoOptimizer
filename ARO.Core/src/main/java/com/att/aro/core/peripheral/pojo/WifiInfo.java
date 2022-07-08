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

import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulates information about the WiFi peripheral.
 */
@Getter
@Setter
public class WifiInfo {
	

	private String wifiRSSI;
	private String wifiSSID;
	private WifiState wifiState;
	private double beginTimeStamp;
	private double endTimeStamp;
	private String wifiMacAddress;

	
	/**
	 * The WifiInfo.WifiState Enumeration specifies constant values that
	 * describe the operational state of the WiFi peripheral on a device. This
	 * enumeration is part of the WifiInfo class.
	 */

	public enum WifiState {
		/**
		 * WiFi is in an unknown state.
		 */
		UNKNOWN,
		/**
		 * WIFI is in the disabled state.
		 */
		OFF,
		/**
		 * The device is connecting to a Wifi HotSpot.
		 */
		CONNECTING,
		/**
		 * The device is connected to a Wifi HotSpot.
		 */
		CONNECTED,
		/**
		 * The Device is disconnecting from a Wifi HotSpot.
		 */
		DISCONNECTING,
		/**
		 * The Device is disconnected from a Wifi HotSpot.
		 */
		DISCONNECTED,
		/**
		 * Wifi is suspended on the device.
		 */
		SUSPENDED
	}


	/**
	 * Initializes an instance of the WifiInfo class using the specified
	 * timestamps, WiFi state, Mac address, rssi, and ssid.
	 * 
	 * @param beginTimeStamp - The beginning timestamp for the WiFi event.
	 * @param endTimeStamp - The ending timestamp for the WiFi event.
	 * 
	 * @param wifiState - One of the values of the WiFiState enumeration that
	 *            		indicates the state of the WiFi connection.
	 * 
	 * @param macAddress - The WiFi Mac address.
	 * 
	 * @param rssi - The RSSI value.
	 * 
	 * @param ssid - The SSID value.
	 */
	public WifiInfo(double beginTimeStamp, double endTimeStamp, WifiState wifiState, String macAddress, String rssi, String ssid) {
		this.beginTimeStamp = beginTimeStamp;
		this.endTimeStamp = endTimeStamp;
		this.wifiState = wifiState;
		this.wifiMacAddress = macAddress;
		this.wifiRSSI = rssi;
		this.wifiSSID = ssid.equals("<unknown ssid>") ? "Unknown" : ssid;
	}

}
