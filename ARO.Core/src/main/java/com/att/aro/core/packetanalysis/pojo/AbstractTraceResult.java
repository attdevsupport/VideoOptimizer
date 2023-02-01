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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.att.aro.core.datacollector.pojo.EnvironmentDetails;
import com.att.aro.core.peripheral.pojo.BluetoothInfo;
import com.att.aro.core.peripheral.pojo.CameraInfo;
import com.att.aro.core.peripheral.pojo.CpuActivityList;
import com.att.aro.core.peripheral.pojo.GpsInfo;
import com.att.aro.core.peripheral.pojo.LocationEvent;
import com.att.aro.core.peripheral.pojo.ScreenStateInfo;
import com.att.aro.core.peripheral.pojo.TemperatureEvent;
import com.att.aro.core.peripheral.pojo.ThermalStatusInfo;
import com.att.aro.core.peripheral.pojo.UserEvent;
import com.att.aro.core.securedpacketreader.ICrypto;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Common trace data returned from reading trace file
 * Date: November 7, 2014
 */
public abstract class AbstractTraceResult {
	
	/**
	 *   a list of all packets
	 */
	@JsonIgnore
	protected List<PacketInfo> allpackets;
	
	/**
	 * a Date timestamp of start of trace
	 */
	protected Date traceDateTime;
	
	/** 
	 * duration of trace
	 */
	protected double traceDuration;
	
	/**
	 * path of the trace directory
	 */
	protected String traceDirectory;
	
	/**
	 * path of the trace file
	 */
	protected String traceFile;
	
	/**
	 * TimeRange used in analysis
	 */
	protected TimeRange timeRange;

	/**
	 * the pcap startTime timestamp 
	 */
	@JsonIgnore
	protected double pcapTime0 = 0;
	
	/**
	 * A Set of all app names active during trace
	 */
	@JsonIgnore
	protected Set<String> allAppNames = null;
	
	/**
	 * the time zone offset
	 */
	@JsonIgnore
	protected int captureOffset = -1;
	
	/**
	 *  a Map of IpAddresses
	 */
	@JsonIgnore
	private Map<String, Set<InetAddress>> appIps = null;
	
	/**
	 * List of active app ids
	 */
	@JsonIgnore
	private List<Integer> appIds = null;
	
	/**
	 * a list of app info
	 */
	@JsonIgnore
	private List<String> appInfos = null;
	
	/**
	 * Map of ip address count
	 */
	@JsonIgnore
	private Map<InetAddress, Integer> ipCountMap;
	
	/**
	 * List of CPU activity info
	 */
	@JsonIgnore
	private CpuActivityList cpuActivityList = null;

	/**
	 * List of Gps Info
	 */
	@JsonIgnore
	private List<GpsInfo> gpsInfos = null;

	/**
	 * List of Bluetooth Info
	 */
	@JsonIgnore
	private List<BluetoothInfo> bluetoothInfos = null;

	/**
	 * List of Camera Info
	 */
	@JsonIgnore
	private List<CameraInfo> cameraInfos = null;

	/**
	 * List of Screen State Info
	 */
	@JsonIgnore
	private List<ScreenStateInfo> screenStateInfos = null;

	/**
	 * List of Thermal Status Info
	 */
	@JsonIgnore
	private List<ThermalStatusInfo> thermalstatusInfos = null;

	/**
	 * List of User Event Info
	 */
	@JsonIgnore
	private List<UserEvent> userEvents = null;
	
	/**
	 * List of User Event Info
	 */
	@JsonIgnore
	private List<TemperatureEvent> temperatureEvents = null;
	
	/**
	 * List of Location Event Info
	 */
	@JsonIgnore
	private List<LocationEvent> locationEvents = null;
	
	/**
	 * keyword private data info from device
	 * <br>from trace directory - private_data
	 */
	@JsonIgnore
	private Map<String, String> deviceKeywordInfos = null;

	/**
	 * from trace directory - video_time
	 */
	@JsonIgnore
	protected double videoStartTime;

	/**
	 * indicates absence of exVideo_time
	 */
	@JsonIgnore
	protected boolean exVideoTimeFileNotFound;

	/**
	 * indicates absence of exVideo_time
	 */
	@JsonIgnore
	protected boolean exVideoFound;
	
	/**
	 * from trace directory - 
	 * <ul>
	 * true when video_time exists<br>
	 * false when exVideo_time exists
	 * </ul>
	 */
	@JsonIgnore
	protected boolean deviceScreenVideo;

	@JsonIgnore
	private ICrypto crypto;

	@JsonIgnore
	private List<VideoEvent> videoEvents;
	protected TraceResultType traceResultType;
	
	@Setter
	@Getter
	@JsonIgnore
	private MetaDataModel metaData;
	
	@Setter
	@Getter
	@JsonIgnore
	private EnvironmentDetails environmentDetails;

	@Getter
	@Setter
	private List<String> localIpAddressList;

	/**
	 * time offset of first pcap packet from timeFile start of trace
	 */
	private double pcapTimeOffset;

	/**
	 * Constructor, Initializes all base tracedata objects.
	 */
	public AbstractTraceResult() {
		pcapTime0 = 0;
		traceDateTime = null;
		traceDuration = 0;
		captureOffset = -1;
		allAppNames = new HashSet<String>();
		appIps = new HashMap<String, Set<InetAddress>>();
		traceDirectory = "";
		appInfos = new ArrayList<String>();
		ipCountMap = new HashMap<InetAddress, Integer>();

		cpuActivityList = new CpuActivityList();
		gpsInfos = new ArrayList<GpsInfo>();
		bluetoothInfos = new ArrayList<BluetoothInfo>();
		cameraInfos = new ArrayList<CameraInfo>();
		screenStateInfos = new ArrayList<ScreenStateInfo>();
		userEvents = new ArrayList<UserEvent>();
		temperatureEvents = new ArrayList<TemperatureEvent>();
		locationEvents = new ArrayList<LocationEvent>();

		exVideoTimeFileNotFound = false;
		exVideoFound = false;
		deviceScreenVideo = false;
		metaData = null;
		environmentDetails = null;
	}

	/**
	 * Return TraceResultType type of trace. ie. TRACE_DIRECTORY or TRACE_FILE
	 * @return TraceResultType type of trace
	 */
	public abstract TraceResultType getTraceResultType();

	

	/**
	 * @return a list of all packets
	 */
	public List<PacketInfo> getAllpackets() {
		return allpackets;
	}

	/**
	 * Set list of all packets
	 * @param allpackets a list of all packets
	 */
	public void setAllpackets(List<PacketInfo> allpackets) {
		this.allpackets = allpackets;
	}

	/**
	 * @return timestamp of start of trace
	 */
	public Date getTraceDateTime() {
		return traceDateTime;
	}

	/**
	 * Set timestamp of start of trace
	 * @param traceDateTime - timestamp of start of trace
	 */
	public void setTraceDateTime(Date traceDateTime) {
		this.traceDateTime = traceDateTime;
	}

	public void setCrypto(ICrypto crypto) {
		this.crypto = crypto;
	}

	public ICrypto getCrypto() {
		return crypto;
	}


	/**
	 * @return duration of trace
	 */
	public double getTraceDuration() {
		return traceDuration;
	}

	/**
	 * @return timestamp of video start
	 */
	public double getVideoStartTime() {
		return videoStartTime;
	}

	/**
	 * @return boolean indicating absence of exVideo_time
	 */
	public boolean isExVideoTimeFileNotFound() {
		return exVideoTimeFileNotFound;
	}

	/**
	 * Set true for absence of exVideo_time
	 * @param exVideoTimeFileNotFound - absence of exVideo_time
	 */
	public void setExVideoTimeFileNotFound(boolean exVideoTimeFileNotFound) {
		this.exVideoTimeFileNotFound = exVideoTimeFileNotFound;
	}

	/**
	 * @return boolean indicating absence of exvideo.mov 
	 */
	public boolean isExVideoFound() {
		return exVideoFound;
	}

	/**
	 * @return boolean true if native video (video_time)
	 */
	public boolean isDeviceScreenVideo() {
		return deviceScreenVideo;
	}

	/**
	 * Set true for absence of exvideo.mov 
	 * @param exVideoFound - absence of exvideo.mov
	 */
	public void setExVideoFound(boolean exVideoFound) {
		this.exVideoFound = exVideoFound;
	}

	/**
	 * Sets true if native video (video_time)
	 * @param deviceScreenVideo - true if native video (video_time)
	 */
	public void setDeviceScreenVideo(boolean deviceScreenVideo) {
		this.deviceScreenVideo = deviceScreenVideo;
	}

	/**
	 * Set timestamp of video start
	 * @param videoStartTime - timestamp of video start
	 */
	public void setVideoStartTime(double videoStartTime) {
		this.videoStartTime = videoStartTime;
	}

	/**
	 * Set duration of trace
	 * @param traceDuration - duration of trace
	 */
	public void setTraceDuration(double traceDuration) {
		this.traceDuration = traceDuration;
	}

	/**
	 * @return the trace directory
	 */
	public String getTraceDirectory() {
		return traceDirectory;
	}

	/**
	 * Set the trace directory
	 * @param traceDirectory - path of the trace directory
	 */
	public void setTraceDirectory(String traceDirectory) {
		this.traceDirectory = traceDirectory;
	}

	/**
	 * @return the trace file
	 */
	public String getTraceFile() {
		return traceFile;
	}

	/**
	 * Set the trace directory
	 * @param traceFile - path of the trace file
	 */
	public void setTraceFile(String traceFile) {
		this.traceFile = traceFile;
	}

	/**
	 * Set the trace result type
	 * @param traceResultType
	 */
	public void setTraceResultType(TraceResultType traceResultType) {
		this.traceResultType = traceResultType;
	}
	
	/**
	 * @return the pcap startTime timestamp
	 */
	public double getPcapTime0() {
		return pcapTime0;
	}

	/**
	 * Set the pcap startTime timestamp
	 * @param pcapTime0 - the pcap startTime timestamp
	 */
	public void setPcapTime0(double pcapTime0) {
		this.pcapTime0 = pcapTime0;
	}

	/**
	 * @return a Set of all app names active during trace
	 */
	public Set<String> getAllAppNames() {
		return allAppNames;
	}

	/**
	 * Sets a Set of all app names active during trace
	 * @param allAppNames - A Set of all app names active during trace
	 */
	public void setAllAppNames(Set<String> allAppNames) {
		this.allAppNames = allAppNames;
	}

	/**
	 * @return the time zone offset
	 */
	public int getCaptureOffset() {
		return captureOffset;
	}

	/**
	 * Set time zone offset
	 * @param captureOffset - the time zone offset
	 */
	public void setCaptureOffset(int captureOffset) {
		this.captureOffset = captureOffset;
	}

	/**
	 * @return Map of IpAddresses
	 */
	public Map<String, Set<InetAddress>> getAppIps() {
		return appIps;
	}

	/**
	 * Set a Map of IpAddresses
	 * @param appIps a Map of IpAddresses
	 */
	public void setAppIps(Map<String, Set<InetAddress>> appIps) {
		this.appIps = appIps;
	}

	/**
	 * @return List of active app ids
	 */
	public List<Integer> getAppIds() {
		return appIds;
	}

	/**
	 * Sets List of active app ids
	 * @param appIds - List of active app ids
	 */
	public void setAppIds(List<Integer> appIds) {
		this.appIds = appIds;
	}

	/**
	 * @return List of app info
	 */
	public List<String> getAppInfos() {
		return appInfos;
	}

	/**
	 * Set List of app info
	 * @param appInfos of app info
	 */
	public void setAppInfos(List<String> appInfos) {
		this.appInfos = appInfos;
	}

	/**
	 * @return Map of ip address count
	 */
	public Map<InetAddress, Integer> getIpCountMap() {
		return ipCountMap;
	}

	/**
	 * Set Map of ip address count
	 * @param ipCountMap - ip address count
	 */
	public void setIpCountMap(Map<InetAddress, Integer> ipCountMap) {
		this.ipCountMap = ipCountMap;
	}

	/**
	 * @return a cpu activity list 
	 */
	public CpuActivityList getCpuActivityList() {
		return cpuActivityList;
	}

	/**
	 * Set a cpu activity list
	 * @param cpuActivityList a cpu activity list
	 */
	public void setCpuActivityList(CpuActivityList cpuActivityList) {
		this.cpuActivityList = cpuActivityList;
	}

	/**
	 * @return List of gps info
	 */
	public List<GpsInfo> getGpsInfos() {
		return gpsInfos;
	}

	/**
	 * Set List of gps info
	 * @param gpsInfos - List of gps info
	 */
	public void setGpsInfos(List<GpsInfo> gpsInfos) {
		this.gpsInfos = gpsInfos;
	}

	/**
	 * @return list of bluetooth info
	 */
	public List<BluetoothInfo> getBluetoothInfos() {
		return bluetoothInfos;
	}

	/**
	 * Set list of bluetooth info
	 * @param bluetoothInfos - list of bluetooth info
	 */
	public void setBluetoothInfos(List<BluetoothInfo> bluetoothInfos) {
		this.bluetoothInfos = bluetoothInfos;
	}

	/**
	 * @return a List of camera info
	 */
	public List<CameraInfo> getCameraInfos() {
		return cameraInfos;
	}

	/**
	 * Set List of camera info
	 * @param cameraInfos - List of camera info
	 */
	public void setCameraInfos(List<CameraInfo> cameraInfos) {
		this.cameraInfos = cameraInfos;
	}

	/**
	 * @return List of screen states
	 */
	public List<ScreenStateInfo> getScreenStateInfos() {
		return screenStateInfos;
	}

	/**
	 * Set List of screen states
	 * @param screenStateInfos - A List of screen states
	 */
	public void setScreenStateInfos(List<ScreenStateInfo> screenStateInfos) {
		this.screenStateInfos = screenStateInfos;
	}

	/**
	 * @return List of user events
	 */
	public List<UserEvent> getUserEvents() {
		return userEvents;
	}
	
	/**
	 * @return List of temperature events
	 */
	public List<TemperatureEvent> getTemperatureEvents() {
		return temperatureEvents;
	}
	
	/**
	 * @return List of location events
	 */
	public List<LocationEvent> getLocationEvents() {
		return locationEvents;
	}

	public void setVideoEvents(List<VideoEvent> videoEvents) {
		this.videoEvents = videoEvents;
	}

	public List<VideoEvent> getVideoEvents() {
		return videoEvents;
	}


	public List<ThermalStatusInfo> getThermalstatusInfos() {
		return thermalstatusInfos;
	}

	public void setThermalstatusInfos(List<ThermalStatusInfo> thermalstatusInfos) {
		this.thermalstatusInfos = thermalstatusInfos;
	}


	/**
	 * Set List of user events
	 * @param userEvents - List of user events
	 */
	public void setUserEvents(List<UserEvent> userEvents) {
		this.userEvents = userEvents;
	}

	public Map<String, String> getDeviceKeywordInfos() {
		return deviceKeywordInfos;
	}

	public void setDeviceKeywordInfos(Map<String, String> keywordInfos) {
		this.deviceKeywordInfos = keywordInfos;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((traceDateTime == null) ? 0 : traceDateTime.hashCode());
		long temp;
		temp = Double.doubleToLongBits(traceDuration);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((traceFile == null) ? 0 : traceFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		AbstractTraceResult other = (AbstractTraceResult) obj;
		if (traceDateTime == null) {
			if (other.traceDateTime != null) {
				return false;
			}
		} else if (!traceDateTime.equals(other.traceDateTime)) {
			return false;
		}
		if (Double.doubleToLongBits(traceDuration) != Double.doubleToLongBits(other.traceDuration)) {
			return false;
		}
		if (traceFile == null) {
			if (other.traceFile != null) {
				return false;
			}
		} else if (!traceFile.equals(other.traceFile)) {
			return false;
		}
		return true;
	}

	/**
	 * set time offset of first pcap packet from timeFile start of trace
	 * @param offset
	 */
	public void setPcapTimeOffset(double offset) {
		this.pcapTimeOffset = offset;
	}

	/**
	 * @return time offset of first pcap packet from timeFile start of trace
	 */
	public double getPcapTimeOffset() {
		return pcapTimeOffset;
	}

	public TimeRange getTimeRange() {
		if (timeRange == null) {
			timeRange = new TimeRange();
		}
		return timeRange;
	}

	public void setTimeRange(TimeRange timeRange) {
		this.timeRange = timeRange;
	}
	
}
