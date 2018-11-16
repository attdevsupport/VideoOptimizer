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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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

@EqualsAndHashCode
public class Statistic {
	/**
	 * Total bytes
	 */
	private int totalByte;
	/**
	 * Total TCP bytes
	 */
	private int totalTCPBytes;
	/**
	 * Total TCP bytes
	 */
	private int totalTCPPackets;
	/**
	 * total HTTPS bytes
	 */
	private int totalHTTPSByte;
	
	/**
	 * packet Duration
	 */
	private double packetDuration;
	/**
	 * packet Duration
	 */
	private double packetTCPDuration;
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
	
	@Getter @Setter
	private int totalPayloadBytes;
	
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

	/**
	 * Returns total bytes
	 * @return total bytes
	 */
	public int getTotalByte() {
		return totalByte;
	}

	/**
	 * Sets total bytes
	 * @param totalByte total bytes
	 */
	public void setTotalByte(int totalByte) {
		this.totalByte = totalByte;
	}

	/**
	 * Returns total TCP bytes
	 * 
	 * @return total TCP bytes
	 */
	public int getTotalTCPBytes() {
		return totalTCPBytes;
	}

	/**
	 * Sets total TCP bytes
	 * 
	 * @param totalByte
	 *            total TCP bytes
	 */
	public void setTotalTCPBytes(int totalTCPBytes) {
		this.totalTCPBytes = totalTCPBytes;
	}

	/**
	 * Returns total HTTPS bytes
	 * 
	 * @return total HTTPS bytes
	 */
	public int getTotalHTTPSByte() {
		return totalHTTPSByte;
	}

	/**
	 * Sets total HTTPS bytes
	 * @param totalHTTPSByte - total HTTPS bytes
	 */
	public void setTotalHTTPSByte(int totalHTTPSByte) {
		this.totalHTTPSByte = totalHTTPSByte;
	}

	/**
	 * Returns packet Duration
	 * @return - packet Duration
	 */
	public double getPacketDuration() {
		return packetDuration;
	}

	/**
	 * Sets packet Duration
	 * @param packetDuration - packet Duration
	 */
	public void setPacketDuration(double packetDuration) {
		this.packetDuration = packetDuration;
	}

	/**
	 * Returns average Kbps
	 * @return the average Kbps
	 */
	public double getAverageKbps() {
		return averageKbps;
	}

	/**
	 * Sets average Kbps
	 * @param averageKbps - average Kbps
	 */
	public void setAverageKbps(double averageKbps) {
		this.averageKbps = averageKbps;
	}

	/**
	 * Returns total Packets
	 * @return total Packets
	 */
	public int getTotalPackets() {
		return totalPackets;
	}

	/**
	 * Sets total Packets
	 * @param totalPackets - total Packets
	 */
	public void setTotalPackets(int totalPackets) {
		this.totalPackets = totalPackets;
	}

	/**
	 * Returns appName List
	 * @return appName List
	 */
	public Set<String> getAppName() {
		return appName;
	}

	/**
	 * Sets appName List
	 * @param appName - appName List
	 */
	public void setAppName(Set<String> appName) {
		this.appName = appName;
	}

	/**
	 * Returns ip Packet Summary List
	 * @return ip Packet Summary List
	 */
	public List<IPPacketSummary> getIpPacketSummary() {
		return ipPacketSummary;
	}

	/**
	 * Sets ip Packet Summary List
	 * @param ipPacketSummary - ip Packet Summary List
	 */
	public void setIpPacketSummary(List<IPPacketSummary> ipPacketSummary) {
		this.ipPacketSummary = ipPacketSummary;
	}

	/**
	 * Returns application Packet Summary List
	 * @return application Packet Summary List
	 */
	public List<ApplicationPacketSummary> getApplicationPacketSummary() {
		return applicationPacketSummary;
	}

	/**
	 * Sets application Packet Summary List
	 * 
	 * @param applicationPacketSummary - application Packet Summary List
	 */
	public void setApplicationPacketSummary(List<ApplicationPacketSummary> applicationPacketSummary) {
		this.applicationPacketSummary = applicationPacketSummary;
	}

	/**
	 * Returns a Map to contain a count of packets by size.
	 * 
	 * @return a Map to contain a count of packets by size.
	 */
	public Map<Integer, Integer> getPacketSizeToCountMap() {
		return packetSizeToCountMap;
	}

	/**
	 * Sets a Map to contain a count of packets by size.
	 * 
	 * @param packetSizeToCountMap - a Map to contain a count of packets by size.
	 */
	public void setPacketSizeToCountMap(Map<Integer, Integer> packetSizeToCountMap) {
		this.packetSizeToCountMap = packetSizeToCountMap;
	}

	/**
	 * Returns total TCP Packets
	 * @return total TCP Packets
	 */
	public int getTotalTCPPackets() {
		return totalTCPPackets;
	}

	/**
	 * Sets total TCP Packets
	 * @param total TCP Packets
	 */
	public void setTotalTCPPackets(int totalTCPPackets) {
		this.totalTCPPackets = totalTCPPackets;
	}

	public double getAverageTCPKbps() {
		return averageTCPKbps;
	}

	public void setAverageTCPKbps(double averageTCPKbps) {
		this.averageTCPKbps = averageTCPKbps;
	}

	public double getTCPPacketDuration() {
		return packetTCPDuration;
	}

	public void setTCPPacketDuration(double packetTCPDuration) {
		this.packetTCPDuration = packetTCPDuration;
	}

}
