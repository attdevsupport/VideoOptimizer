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
package com.att.aro.core.peripheral.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.IWifiInfoReader;
import com.att.aro.core.peripheral.pojo.WifiInfo;
import com.att.aro.core.peripheral.pojo.WifiInfo.WifiState;
import com.att.aro.core.util.Util;

/**
 * Method to read the WIFI data from the trace file and store it in the
 * wifiInfos list. It also updates the active duration for Wifi.
 * Date: September 30, 2014
 */
public class WifiInfoReaderImpl extends PeripheralBase implements IWifiInfoReader {

	private static final Logger LOGGER = LogManager.getLogger(WifiInfoReaderImpl.class.getName());

	private double wifiActiveDuration = 0;
	private Pattern wifiPattern = Pattern.compile("\\S*\\s*\\S*\\s*(\\S*)\\s*(\\S*)\\s*(.*)");

	@Override
	public List<WifiInfo> readData(String directory, double pcapStartTime, double traceDuration) {	
		List<WifiInfo> wifiInfos = new ArrayList<WifiInfo>();
		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.WIFI_FILE;
		try {
			if (filereader.fileExist(filepath)) {
				String[] wifiEvents = null;
				wifiEvents = filereader.readAllLine(filepath);
				if (wifiEvents != null && wifiEvents.length > 0) {
					for (String wifiEvent : wifiEvents) {
						parseWifiEvent(wifiInfos, pcapStartTime, traceDuration, wifiEvent);
					}
					WifiInfo previousWifiInfo = wifiInfos.get(wifiInfos.size() - 1);
					WifiState previousWifiState = previousWifiInfo.getWifiState();
					if (previousWifiState == WifiState.CONNECTED || previousWifiState == WifiState.CONNECTING || previousWifiState == WifiState.DISCONNECTING) {
						this.wifiActiveDuration += (previousWifiInfo.getEndTimeStamp() - previousWifiInfo.getBeginTimeStamp());
					}
				} else {
					LOGGER.info("Wifi events file is empty, no action taken");
				}
			}			
		} catch (IOException e1) {
			LOGGER.error("failed to read Wifi info file: " + filepath);
		} catch (Exception e) {
			LOGGER.warn("Unexpected error parsing Wifi event: ", e);
		}
		return wifiInfos;
	}

	private void parseWifiEvent(List<WifiInfo> wifiInfos, double pcapStartTime,  double traceDuration, String wifiEvent) {
		String rssi = "";
		String ssid = "";
		String macAddress = "";
		String wiFiEventFields[] = wifiEvent.split(" ");
		if (wiFiEventFields.length >= 2) {
			double beginTime = Util.normalizeTime(Double.parseDouble(wiFiEventFields[0]), pcapStartTime);
			WifiState wifiState = WifiState.valueOf(wiFiEventFields[1]);
			if (wifiState == WifiState.CONNECTED) {
				Matcher matcher = wifiPattern.matcher(wifiEvent);
				if (matcher.lookingAt()) {
					macAddress = matcher.group(1);
					rssi = matcher.group(2);
					ssid = matcher.group(3);
				}
			}
			if (wifiInfos.isEmpty()) {
				wifiInfos.add(new WifiInfo(beginTime, traceDuration, wifiState, macAddress, rssi, ssid));
			} else {
				WifiInfo previousWifiInfo = wifiInfos.get(wifiInfos.size() - 1);
				WifiState previousWifiState = previousWifiInfo.getWifiState();
				if (previousWifiState != wifiState) {
					previousWifiInfo.setEndTimeStamp(beginTime);
					wifiInfos.add(new WifiInfo(beginTime, traceDuration, wifiState, macAddress, rssi, ssid));
					if (previousWifiState == WifiState.CONNECTED || previousWifiState == WifiState.CONNECTING || previousWifiState == WifiState.DISCONNECTING) {
						this.wifiActiveDuration += (previousWifiInfo.getEndTimeStamp() - previousWifiInfo.getBeginTimeStamp());
					}
				}
			}
		} else {
			LOGGER.debug("Invalid WiFi trace entry: " + wifiEvent);
		}
	}

	@Override
	public double getWifiActiveDuration() {
		return this.wifiActiveDuration;
	}

}
