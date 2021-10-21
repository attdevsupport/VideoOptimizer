/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.packetanalysis.impl;

import java.io.Serializable;
import java.util.List;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.configuration.impl.ProfileFactoryImpl;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.configuration.pojo.Profile3G;
import com.att.aro.core.configuration.pojo.ProfileLTE;
import com.att.aro.core.configuration.pojo.ProfileType;
import com.att.aro.core.configuration.pojo.ProfileWiFi;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.RRCState;
import com.att.aro.core.packetanalysis.pojo.RrcStateRange;
import com.att.aro.core.packetreader.pojo.PacketDirection;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;


/**
 * Encapsulates Time Range Analysis information.
 */
public class TimeRangeAnalysis implements Serializable {
	private static final long serialVersionUID = 1L;

	@JsonIgnore
	private static final ProfileFactoryImpl profileFactory = SpringContextUtil.getInstance().getContext().getBean(ProfileFactoryImpl.class);

	@Getter
	private double startTime;
	@Getter
	private double endTime;

	private long totalBytes;
	private long uplinkBytes;
	private long downlinkBytes;
	private long payloadLen; // bytes
	private double activeTime;
	private double rrcEnergy;


	/**
	 * Constructor taking start time and end time of the time range, and performs analysis to populate remaining fields
	 * @param startTime
	 * @param endTime
	 * @param analysisData
	 */
	public TimeRangeAnalysis(double startTime, double endTime, PacketAnalyzerResult analysisData) {
		this.startTime = startTime;
		this.endTime = endTime;
		performTimeRangeAnalysis(analysisData);
	}

	/**
	 * Initializes an instance of the TimeRangeAnalysis class, using the specified start and 
	 * end times, total number of bytes transferred, payload length, active state time, and RRC energy value. 
	 * 
	 * @param startTime The start of the time range (in seconds). 
	 * @param endTime The end of the time range (in seconds). 
	 * @param totalBytes The total bytes transferred, including all packet headers. 
	 * @param payloadLen The length of the payload in bytes.
	 * @param activeTime The total amount of high energy radio time. 
	 * @param energy The amount of RRC energy used to deliver the payload.
	 */
	public TimeRangeAnalysis(double startTime, double endTime, long totalBytes, long uplinkBytes, long downlinkBytes,
			long payloadLen, double activeTime, double rrcEnergy) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.totalBytes = totalBytes;
		this.uplinkBytes = uplinkBytes;
		this.downlinkBytes = downlinkBytes;
		this.payloadLen = payloadLen;
		this.activeTime = activeTime;
		this.rrcEnergy = rrcEnergy;
	}

	/**
	 * Returns the total number of bytes transferred, including packet headers.
	 * @return The total bytes transferred.
	 */
	public long getTotalBytes() {
		return totalBytes;
	}

	/**
	 *  Returns the length of the payload. 
	 * 
	 * @return The payload length, in bytes.
	 */
	public long getPayloadLen() {
		return payloadLen;
	}
	
	/**
	 * Returns the total uplink number of bytes transferred, including packet headers.
	 * @return The uplink bytes transferred.
	 */
	public long getUplinkBytes() {
		return uplinkBytes;
	}
	
	/**
	 * Returns the total downlink number of bytes transferred, including packet headers.
	 * @return The downlink bytes transferred.
	 */
	public long getDownlinkBytes() {
		return downlinkBytes;
	}

	/**
	 * Returns the total amount of time that the radio was in a high energy active state.
	 * 
	 * @return The active time value, in seconds.
	 */
	public double getActiveTime() {
		return activeTime;
	}

	/**
	 * Returns the amount of RRC energy used to deliver the payload. 
	 * 
	 * @return The RRC energy value, in joules.
	 */
	public double getRrcEnergy() {
		return rrcEnergy;
	}

	/**
	 * Returns the average throughput for the time range
	 * @return The throughput value, in kilobits per second.
	 */
	public double getAverageThroughput() {
		return (totalBytes * 8 / 1000) / (endTime - startTime);
	}
	
	/**
	 * Returns the average uplink throughput for the time range
	 * @return The uplink throughput value, in kilobits per second.
	 */
	public double getAverageUplinkThroughput() {
		return (uplinkBytes * 8 / 1000) / (endTime - startTime);
	}
	
	/**
	 * Returns the average downlink throughput for the time range
	 * @return The downlink throughput value, in kilobits per second.
	 */
	public double getAverageDownlinkThroughput() {
		return (downlinkBytes * 8 / 1000) / (endTime - startTime);
	}
	

	/**
	 * Performs a TimeRangeAnalysis on the trace data.
	 * @param analysisData Packet analyzer result object
	 */
	private void performTimeRangeAnalysis(PacketAnalyzerResult analysisData) {
		if (analysisData != null) {
			List<RrcStateRange> rrcCollection = analysisData.getStatemachine().getStaterangelist();
			List<PacketInfo> packets = analysisData.getTraceresult().getAllpackets();
			Profile profile = analysisData.getProfile();
			int packetNum = packets.size();

			for (int i = 0; i < packetNum; i++) {
				PacketInfo packetInfo = packets.get(i);
				if (packetInfo.getTimeStamp() >= startTime && packetInfo.getTimeStamp() <= endTime) {
					payloadLen += packetInfo.getPayloadLen();
					totalBytes += packetInfo.getLen();
					if (packetInfo.getDir().equals(PacketDirection.UPLINK)) {
						uplinkBytes += packetInfo.getLen();
					} else if (packetInfo.getDir().equals(PacketDirection.DOWNLINK)) {
						downlinkBytes += packetInfo.getLen();
					}
				}
			}
	
			int collectionSize = rrcCollection.size();
			for (int i = 0; i < collectionSize; i++) {
				double beginTime;
				double endTime;
	
				RrcStateRange rrc = rrcCollection.get(i);
				if (rrc.getEndTime() < this.startTime) {
					continue;
				}
				if (rrc.getBeginTime() > this.endTime) {
					continue;
				}
	
				if (rrc.getBeginTime() >= this.startTime) {
					beginTime = rrc.getBeginTime();
				} else {
					beginTime = this.startTime;
				}
	
				if (rrc.getEndTime() <= this.endTime) {
					endTime = rrc.getEndTime();
				} else {
					endTime = this.endTime;
				}
	
				RRCState rrcState = rrc.getState();
				rrcEnergy += updateEnergy(analysisData, profile, beginTime, endTime, rrcState);
				activeTime += updateActiveTime(profile, beginTime, endTime, rrcState);
			}
		}
	}

	private double updateActiveTime(Profile profile, double beginTime, double endTime, RRCState rrcState) {
		double activeTime = 0.0f;
		if (profile.getProfileType() == ProfileType.T3G && (rrcState == RRCState.STATE_DCH || rrcState == RRCState.TAIL_DCH)
				|| profile.getProfileType() == ProfileType.LTE && (rrcState == RRCState.LTE_CONTINUOUS || rrcState == RRCState.LTE_CR_TAIL)
				|| profile.getProfileType() == ProfileType.WIFI && (rrcState == RRCState.WIFI_ACTIVE || rrcState == RRCState.WIFI_TAIL)) {
			activeTime = endTime - beginTime;
		}
		return activeTime;
	}

	private double updateEnergy(PacketAnalyzerResult analysisData,
			Profile profile, double beginTime, double endTime, RRCState rrcState) {
		double energy = 0.0f;

		if (profile.getProfileType().equals(ProfileType.T3G)) {
			energy += profileFactory.energy3G(beginTime, endTime, rrcState, (Profile3G)profile);
		} else if (profile.getProfileType().equals(ProfileType.LTE)) {
			energy += profileFactory.energyLTE(beginTime, endTime, rrcState, (ProfileLTE)profile, analysisData.getTraceresult().getAllpackets());
		} else if (profile.getProfileType().equals(ProfileType.WIFI)) {
			energy += profileFactory.energyWiFi(beginTime, endTime, rrcState, (ProfileWiFi)profile);
		}
		return energy;
	}
}
