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
package com.att.aro.core.peripheral.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.NetworkBearerTypeInfo;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.INetworkTypeReader;
import com.att.aro.core.peripheral.pojo.NetworkType;
import com.att.aro.core.peripheral.pojo.NetworkTypeObject;
import com.att.aro.core.util.Util;

/**
 * Date: October 7, 2014
 */
public class NetworkTypeReaderImpl extends PeripheralBase implements INetworkTypeReader {

	private static final Logger LOGGER = LogManager.getLogger(NetworkTypeReaderImpl.class.getName());

	@Override
	public NetworkTypeObject readData(String directory, double startTime, double traceDuration) {
		NetworkTypeObject obj = null;
		List<NetworkBearerTypeInfo> networkTypeInfos = new ArrayList<NetworkBearerTypeInfo>();
		List<NetworkType> networkTypesList = new ArrayList<NetworkType>();

		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.NETWORKINFO_FILE;
		if (!filereader.fileExist(filepath)) {
			return obj;
		}
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filepath);
		} catch (IOException e1) {
			LOGGER.error("failed to read network info file: " + filepath);
		}

		String line;
		if (lines != null && lines.length > 0) {
			line = lines[0];
			// Clear any data that may have been added by device_details
			networkTypeInfos.clear();

			NetworkType networkType;
			NetworkType overrideNetworkType = NetworkType.OVERRIDE_NETWORK_TYPE_NONE;
			double beginTime;
			double endTime;
			String[] fields = line.split(" ");
			if (fields.length >= 2) {
				beginTime = Util.normalizeTime(Double.parseDouble(fields[0]), startTime);
				try {
					networkType = getNetworkTypeFromCode(Integer.parseInt(fields[1]));
					if (fields.length > 2) {
						overrideNetworkType = getOverriderNetworkTypeFromCode(Integer.parseInt(fields[2]));
					}
				} catch (NumberFormatException e) {
					networkType = NetworkType.UNKNOWN;
					LOGGER.warn("Invalid network type [" + fields[1] + "]");
				}
				networkTypesList.add(networkType);
				for (int i = 1; i < lines.length; i++) {
					line = lines[i];
					fields = line.split(" ");
					if (fields.length >= 2) {
						endTime = Util.normalizeTime(Double.parseDouble(fields[0]), startTime);
						networkTypeInfos
								.add(new NetworkBearerTypeInfo(beginTime, endTime, networkType, overrideNetworkType));
						try {
							networkType = getNetworkTypeFromCode(Integer.parseInt(fields[1]));
							if (fields.length > 2) {
								overrideNetworkType = getOverriderNetworkTypeFromCode(Integer.parseInt(fields[2]));
							}
						} catch (NumberFormatException e) {
							networkType = NetworkType.UNKNOWN;
							LOGGER.warn("Invalid network type [" + fields[1] + "]");
						}
						beginTime = endTime;
						if (!networkTypesList.contains(networkType)) {
							networkTypesList.add(networkType);
						}
					}
				}
				networkTypeInfos.add(new NetworkBearerTypeInfo(beginTime, traceDuration, networkType, overrideNetworkType));
			}
		}
		obj = new NetworkTypeObject();
		obj.setNetworkTypeInfos(networkTypeInfos);
		obj.setNetworkTypesList(networkTypesList);
		return obj;
	}

	private NetworkType getNetworkTypeFromCode(int networkTypeCode) {
		switch (networkTypeCode) {
		case TraceDataConst.TraceNetworkType.WIFI:
			return NetworkType.WIFI;
		case TraceDataConst.TraceNetworkType.EDGE:
			return NetworkType.EDGE;
		case TraceDataConst.TraceNetworkType.GPRS:
			return NetworkType.GPRS;
		case TraceDataConst.TraceNetworkType.UMTS:
			return NetworkType.UMTS;
		case TraceDataConst.TraceNetworkType.EVDO0:
			return NetworkType.ETHERNET;
		case TraceDataConst.TraceNetworkType.HSDPA:
			return NetworkType.HSDPA;
		case TraceDataConst.TraceNetworkType.HSUPA:
			return NetworkType.HSUPA;
		case TraceDataConst.TraceNetworkType.HSPA:
			return NetworkType.HSPA;
		case TraceDataConst.TraceNetworkType.HSPAP:
			return NetworkType.HSPAP;
		case TraceDataConst.TraceNetworkType.LTE:
			return NetworkType.LTE;
		case TraceDataConst.TraceNetworkType.UNKNOWN:
			return NetworkType.UNKNOWN;
		default:
			return NetworkType.UNKNOWN;
		}

	}
	
	/**
	 * This information is provided in accordance with carrier policy and branding preferences.
	 * The number is based on Android document from TelephonyDisplayInfo.java
	 * Only device with Android 11 (API 30) will return the number
	 * @param overrideNetworkTypeCode
	 * @return
	 */	
	private NetworkType getOverriderNetworkTypeFromCode(int overrideNetworkTypeCode) {
		switch (overrideNetworkTypeCode) {
		case 0:
			return NetworkType.OVERRIDE_NETWORK_TYPE_NONE;
		case 1:
			return NetworkType.OVERRIDE_NETWORK_TYPE_LTE_CA;
		case 2:
			return NetworkType.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO;
		case 3:
			return NetworkType.OVERRIDE_NETWORK_TYPE_NR_NSA;
		case 4:
			return NetworkType.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE;
		default:
			return NetworkType.OVERRIDE_NETWORK_TYPE_NONE;
		}		
	}

	
}//end class
