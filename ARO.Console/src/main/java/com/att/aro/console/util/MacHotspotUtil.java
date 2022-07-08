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
package com.att.aro.console.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import com.att.aro.core.util.NetworkUtil;

/**
 * This is the utility class for verifying  network status pre-requisite.
 * When the user use iOS attenuation and secure, the network environment need to set properly before proceeding the collection.
 * @author ls661n
 */
public class MacHotspotUtil {
	private static final String SHARED_NET_IF = "bridge100";
	private static final String PORT_NUMBER = "8080";

	public static String getStatusMessage() {
		
		String WIFI_SHARED_STATUS = NetworkUtil.isNetworkUp(SHARED_NET_IF) ? "Active":"InActive";	
		
		return "Attenuation Status\n"+ 	
				"Please make sure Mac WiFi sharing is turned on, and "
				+ "the iOS device is connected to the WiFi before proceeding.\n" 
				+ "Internet Sharing Status: " + WIFI_SHARED_STATUS + "\n"
				+ "Server IP address: " + getSharedIP() + "\n"
				+ "Server Port:"+ PORT_NUMBER;
	}
	
	private static String getSharedIP() {
		if (NetworkUtil.isNetworkUp(SHARED_NET_IF)) {
			List<InetAddress> listIp = NetworkUtil.listNetIFIPAddress(SHARED_NET_IF);
			for (InetAddress ip : listIp) {
				if (ip instanceof Inet4Address) {
					String ipString = ip.toString().replaceAll("/", "");
					return ipString;
				}
			}
		}
		return "N/A ";
	}
}
