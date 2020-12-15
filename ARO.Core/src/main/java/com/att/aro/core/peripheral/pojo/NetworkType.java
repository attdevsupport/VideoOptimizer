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

/**
 * The NetworkType Enumeration specifies constant values that identify recognized network types.
 */
public enum NetworkType {
	
	/**
	 * Identifies an unknown network type.
	 */
	UNKNOWN,
	
	/**
	 * Identifies the Wireless Fidelity (Wi-Fi) network.
	 */
	WIFI,

	/**
	 * Identifies the Enhanced Data Rates for GSM Evolution network.
	 */
	EDGE,
	
	/**
	 * Identifies the General Packet Radio Service (GPRS) network.
	 */
	GPRS,
	
	/**
	 * Identifies the Universal Mobile Telecommunications System (UMTS) network.
	 */
	UMTS,
	
	/**
	 * Identifies the Ethernet network.
	 */
	ETHERNET,
	
	/**
	 * Identifies the High-Speed Downlink Packet Access (HSDPA) network.
	 */
	HSDPA,
	
	/**
	 * Identifies the High-Speed Packet Access (HSPA) network.evdo
	 */
	HSPA,
	
	/**
	 * Identifies the High-Speed Packet Access Plus (HSPA+) network.
	 */
	HSPAP,
	
	/**
	 * Identifies the High-Speed Uplink Packet Access (HSUPA) network.
	 */
	HSUPA,
	
	/**
	 * Identifies the Long Term Evolution (LTE) network.
	 */
	LTE,
	
	/**
	 * Identifies the Code-division multiple access (CDMA).
	 */	
	CDMA,
	
	/**
	 * Identifies the Evolution-Data Optimized (EV-DO, EVDO) revision 0
	 */
	EVDO0,
	
	/**
	 * Identifies the Evolution-Data Optimized (EV-DO, EVDO) revision A.
	 */
	EVDOA,
	
	/**
	 * Identifies the Evolution-Data Optimized (EV-DO, EVDO) revision B.
	 */
	EVDOB,
	
	/**
	 * Identifies Global System for Mobile Communications.
	 */
	GSM,
	
	/**
	 * Identifies Industrial Wireless Local Area Network
	 */
	IWLAN,
	
	/**
	 * Identifies the  NR(New Radio) 5G.
	 */
	NR,

	/**
	 * Identifies Advanced pro LTE (5Ge)
	 */
	OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO,
		
	/**
	 * Identifies NR (5G) for 5G Sub-6 networks
	 */
	OVERRIDE_NETWORK_TYPE_NR_NSA,
	
	/**
	 * Identifies (5G+/5G UW) for 5G mmWave networks
	 */
	OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE,
	
	/**
	 * Identifies cellular network and is using carrier aggregation.
	 */
	OVERRIDE_NETWORK_TYPE_LTE_CA,
	
	/**
	 * No override. getNetworkType() should be used for display network type.
	 */
	OVERRIDE_NETWORK_TYPE_NONE,


}
