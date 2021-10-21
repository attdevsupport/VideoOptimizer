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

import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.packetanalysis.impl.TimeRangeAnalysis;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Data returned from analyzing trace file or directory
 * Date: November 7, 2014
 */
public class PacketAnalyzerResult {
	private AbstractTraceResult traceresult;
	@JsonIgnore
	private List<Session> sessionlist;
	private Statistic statistic;

	private TimeRangeAnalysis timeRangeAnalysis;

	@JsonIgnore
	private AbstractRrcStateMachine statemachine;
	@JsonIgnore
	private EnergyModel energyModel;
	@JsonIgnore
	private BurstCollectionAnalysisData burstCollectionAnalysisData;
	
	//For changing the packets based on selection
	@JsonIgnore
	private boolean ipv4Packets = true;
	@JsonIgnore
	private boolean ipv6Packets = true;
	@JsonIgnore
	private boolean udpPackets = true;

	@JsonIgnore
	private List<VideoStall> videoStalls;
	@JsonIgnore
	private BufferOccupancyBPResult bufferOccupancyResult;
	@JsonIgnore
	private BufferTimeBPResult bufferTimeResult;
	@JsonIgnore
	private List<NearStall> nearStalls;
	
	/**
	 * The profile used in creating the EnergyModel
	 */
	@JsonIgnore
	private Profile profile;
	
	/**
	 * AnalysisFilter is used to limit the analysis results.<p>
	 * A null value is no filtering
	 */
	@JsonIgnore
	private AnalysisFilter filter;
	
	/**
	 * Results of a cache analysis of the HTTP requests and responses.
	 */
	@JsonIgnore
	private CacheAnalysis cacheAnalysis = null;
	
	/**
	 * sensitive data from device
	 */
	@JsonIgnore
	private Map<String, String> deviceKeywords;
	
	/**
	 * results Video analysis
	 */
	@JsonIgnore
	private StreamingVideoData streamingVideoData;
	
	/**
	 * Returns trace results
	 *  
	 * @return Returns trace results
	 */
	public AbstractTraceResult getTraceresult() {
		return traceresult;
	}

	/**
	 * Sets trace results
	 * 
	 * @param traceresult an AbstractTraceResult object
	 */
	public void setTraceresult(AbstractTraceResult traceresult) {
		this.traceresult = traceresult;
	}

	/**
	 * Returns sessionlist a List of all sessions contained in trace
	 * 
	 * @return sessionlist a List of sessions
	 */
	public List<Session> getSessionlist() {
		return sessionlist;
	}

	/**
	 * Stores the List of all sessions contained in trace
	 * 
	 * @param sessionlist a List of sessions
	 */
	public void setSessionlist(List<Session> sessionlist) {
		this.sessionlist = sessionlist;
	}

	/**
	 * Returns the Statistic object containing info about the trace.
	 * 
	 * @return Returns the Statistic object containing info about the trace
	 */
	public Statistic getStatistic() {
		return statistic;
	}

	/**
	 * Sets Statistic info about the trace
	 * 
	 * @param statistic info about the trace
	 */
	public void setStatistic(Statistic statistic) {
		this.statistic = statistic;
	}

	/**
	 * Returns resource state machine (AbstractRrcStateMachine) energy and duration
	 * 
	 * @return Returns resource state machine (AbstractRrcStateMachine) energy and duration
	 */
	public AbstractRrcStateMachine getStatemachine() {
		return statemachine;
	}

	/**
	 * Sets resource state machine (AbstractRrcStateMachine) energy and duration
	 * 
	 * @param statemachine a AbstractRrcStateMachine object
	 */
	public void setStatemachine(AbstractRrcStateMachine statemachine) {
		this.statemachine = statemachine;
	}

	/**
	 * Returns an energy model (EnergyModel) of peripherals
	 * 
	 * @return an energy model (EnergyModel) of peripherals
	 */
	public EnergyModel getEnergyModel() {
		return energyModel;
	}

	/**
	 * Sets an energy model (EnergyModel) of peripherals
	 * 
	 * @param energyModel an EnergyModel object
	 */
	public void setEnergyModel(EnergyModel energyModel) {
		this.energyModel = energyModel;
	}

	/**
	 * Returns burst collection analysis data
	 * 
	 * @return a BurstCollectionAnalysisData object
	 */
	public BurstCollectionAnalysisData getBurstCollectionAnalysisData() {
		return burstCollectionAnalysisData;
	}

	/**
	 * Sets burst collection analysis data
	 * 
	 * @param burstcollectionAnalysisData a BurstCollectionAnalysisData object
	 */
	public void setBurstCollectionAnalysisData(BurstCollectionAnalysisData burstcollectionAnalysisData) {
		this.burstCollectionAnalysisData = burstcollectionAnalysisData;
	}

	/**
	 * Returns profile data, profiling the device used in trace
	 * 
	 * @return profile data of device
	 */
	public Profile getProfile() {
		return profile;
	}

	/**
	 * Sets profile data, profiling the device used in trace
	 * 
	 * @param profile data of device
	 */
	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	/**
	 * Returns filter used when analyzing trace
	 * 
	 * @return filter used when analyzing trace
	 */
	public AnalysisFilter getFilter() {
		return filter;
	}

	/**
	 * Sets filter used when analyzing trace
	 * 
	 * @param filter used when analyzing trace
	 */
	public void setFilter(AnalysisFilter filter) {
		this.filter = filter;
	}

	/**
	 * Returns cache analysis of trace
	 * 
	 * @return cacheAnalysis a CacheAnalysis object
	 */
	public CacheAnalysis getCacheAnalysis() {
		return cacheAnalysis;
	}
	
	/**
	 * Sets cache analysis of trace
	 * 
	 * @param cacheAnalysis - results from a cache analysis of the HTTP requests and responses
	 */
	public void setCacheAnalysis(CacheAnalysis cacheAnalysis) {
		this.cacheAnalysis = cacheAnalysis;
	}

	public boolean isIpv4Packets() {
		return ipv4Packets;
	}

	public boolean isIpv6Packets() {
		return ipv6Packets;
	}

	public boolean isUdpPackets() {
		return udpPackets;
	}

	public Map<String, String> getDeviceKeywords() {
		return deviceKeywords;
	}

	public void setDeviceKeywords(Map<String, String> keywords) {
		this.deviceKeywords = keywords;
	}

	public StreamingVideoData getStreamingVideoData() {
		return streamingVideoData;
	}

	public void setStreamingVideoData(StreamingVideoData streamingVideoData) {
		this.streamingVideoData = streamingVideoData;
	}
	
	public List<VideoStall> getVideoStalls() {
		return videoStalls;
	}

	public void setVideoStalls(List<VideoStall> videoStalls) {
		this.videoStalls = videoStalls;
	}

	public List<NearStall> getNearStalls() {
		return nearStalls;
	}

	public void setNearStalls(List<NearStall> nearStalls) {
		this.nearStalls = nearStalls;
	}

	public BufferOccupancyBPResult getBufferOccupancyResult() {
		return bufferOccupancyResult;
	}

	public void setBufferOccupancyResult(BufferOccupancyBPResult bufferOccupancyResult) {
		this.bufferOccupancyResult = bufferOccupancyResult;
	}
	
	public void clearBPResults() {
		videoStalls = null;
		nearStalls = null;
		bufferOccupancyResult = null;
		cacheAnalysis = null;
		streamingVideoData = null;
	}

	public BufferTimeBPResult getBufferTimeResult() {
		return bufferTimeResult;
	}

	public void setBufferTimeResult(BufferTimeBPResult bufferTimeResult) {
		this.bufferTimeResult = bufferTimeResult;
	}

	public TimeRangeAnalysis getTimeRangeAnalysis() {
		return timeRangeAnalysis;
	}

	public void setTimeRangeAnalysis(TimeRangeAnalysis timeRangeAnalysis) {
		this.timeRangeAnalysis = timeRangeAnalysis;
	}
}
