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
