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
package com.att.aro.core.packetanalysis.pojo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Statistic contains an accumulation of certain statistics pertaining to some
 * collection of packets. This is usually used for holding statistical data for
 * an entire trace.
 * <pre>
 * 	 totalByte                  // total bytes
*	 totalHTTPSByte             // total HTTPS bytes
*	 packetDuration             // packet Duration
*	 averageKbps                // average Kbps
*	 totalPackets               // total Packets
*	 appName                    // appName List
*	 ipPacketSummary            // ip Packet Summary List
*	 applicationPacketSummary   // application Packet Summary List
*	 packetSizeToCountMap       // a Map to contain a count of packets by size.
 *
 * </pre>
 * 
 * Date: October 24, 2014
 */

@Data
public class Statistic {
	/**
	 * Total bytes
	 */
	private long totalByte;
	/**
	 * Total TCP bytes
	 */
	private long totalTCPBytes;
	/**
	 * Total TCP bytes
	 */
	private int totalTCPPackets;
	/**
	 * total HTTPS bytes
	 */
	private long totalHTTPSByte;

	/**
     * Total HTTPS bytes not analyzed
     */
    private long totalHTTPSBytesNotAnalyzed;
	
	/**
	 * packet Duration
	 */
	private double packetDuration;
	/**
	 * packet Duration
	 */
	private double tcpPacketDuration;
	/**
	 * average Kbps
	 */
	private double averageKbps;
	
	/**
	 * average TCP Kbps
	 */
	private double averageTCPKbps;
	
	/**
	 * total Packets
	 */
	private int totalPackets;
	
	/**
	 * appName List
	 */
	private Set<String> appName;

	private long totalPayloadBytes;

	private long totalTCPPayloadBytes;
	
	private double minLatency;
	private double maxLatency;
	private double averageLatency;
		
	/**
	 * ip Packet Summary List
	 */
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private List<IPPacketSummary> ipPacketSummary = null;
	
	/**
	 * application Packet Summary List
	 */
	private List<ApplicationPacketSummary> applicationPacketSummary = null;
	
	/**
	 * a Map to contain a count of packets by size.<br>
	 * <pre>
	 * Usage:
	 *   Map<Integer, Integer> packetSizeToCountMap = new HashMap<Integer, Integer>();
	 *   Integer iValue = packetSizeToCountMap.get(packetSize);
	 *   iValue = iValue != null ? iValue++: 1;
	 *   packetSizeToCountMap.put(packetSize, iValue);
	 * </pre>
	 */
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private Map<Integer, Integer> packetSizeToCountMap = null;
}