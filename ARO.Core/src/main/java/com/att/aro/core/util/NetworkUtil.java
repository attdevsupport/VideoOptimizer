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
package com.att.aro.core.util;

 
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

/**
 * Utility class for network interfaces on a machine 
 * as well as their IP addresses and status.
 *
 */
public class NetworkUtil {
	
	private static final Logger LOGGER = LogManager.getLogger(NetworkUtil.class.getSimpleName());

 	public static boolean isNetworkUp(String ifName) {
 		
		try {
 			NetworkInterface nif = NetworkInterface.getByName(ifName);
 			if(nif!=null && nif.isUp()) {
 				return true;
 			}
 			 
		} catch (SocketException soe) {
 			LOGGER.error("SocketException :" + soe );
		} catch( NullPointerException npe) {
			LOGGER.error("NullPointerException :"  + npe );
		}

 		return false;
 		
	}
	
	 
	public static List<InetAddress> listNetIFIPAddress(String ifName){
		ArrayList<InetAddress> listAddresses = new ArrayList<InetAddress>();
		NetworkInterface nif;
		try {
			nif = NetworkInterface.getByName(ifName);
			if (nif != null) {
				Enumeration<InetAddress> inetAddresses = nif.getInetAddresses();
				listAddresses = Collections.list(inetAddresses);
			}
		} catch (SocketException soe) {
			LOGGER.error("SocketException :"  + soe );
		}

		return listAddresses;
	}

	
	public static List<NetworkInterface> listInterfaceInformation() {
 		ArrayList<NetworkInterface> networklist = new ArrayList<NetworkInterface>();
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			networklist = Collections.list(nets);
			 
		} catch (SocketException soe) {
			LOGGER.error("SocketException :"  + soe );
		} catch( NullPointerException npe) {
			LOGGER.error("NullPointerException :"  + npe );
		}

		return networklist;
	}		
	
}
