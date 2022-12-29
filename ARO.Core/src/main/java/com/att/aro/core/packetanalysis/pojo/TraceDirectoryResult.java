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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.att.aro.core.peripheral.pojo.AlarmAnalysisInfo;
import com.att.aro.core.peripheral.pojo.AlarmInfo;
import com.att.aro.core.peripheral.pojo.AttenuatorEvent;
import com.att.aro.core.peripheral.pojo.BatteryInfo;
import com.att.aro.core.peripheral.pojo.CellInfo;
import com.att.aro.core.peripheral.pojo.CollectOptions;
import com.att.aro.core.peripheral.pojo.DeviceDetail;
import com.att.aro.core.peripheral.pojo.LocationEvent;
import com.att.aro.core.peripheral.pojo.NetworkType;
import com.att.aro.core.peripheral.pojo.RadioInfo;
import com.att.aro.core.peripheral.pojo.SpeedThrottleEvent;
import com.att.aro.core.peripheral.pojo.TemperatureEvent;
import com.att.aro.core.peripheral.pojo.VideoStreamStartupData;
import com.att.aro.core.peripheral.pojo.WakelockInfo;
import com.att.aro.core.peripheral.pojo.WifiInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *  Trace data from reading trace directory, which contains a pcap file.<br>
 *  Depending on the type of trace various other trace information may be recorded.
 *  
 *  <br>Potential trace data
 *  <pre>
 *    active_process      
 *    alarm_info_end      
 *    alarm_info_start    
 *    appid               
 *    appname             
 *    battery_events      
 *    batteryinfo_dump    
 *    bluetooth_events    
 *    camera_events       
 *    cpu                 
 *    datadump.csv        
 *    device_details      
 *    device_info         
 *    dmesg               
 *    gps_events          
 *    network_details     
 *    processed_events    
 *    prop                
 *    radio_events        
 *    screen_events       
 *    screen_rotations    
 *    time                
 *    traffic.cap         
 *    video.mov           
 *    video_time          
 *    wifi_events     
 *    collect_options
 *    attenuattionEvent
 *    speedThrottleEvent   
 *  </pre>
 * 
 * Date: October 23, 2014
 */
public class TraceDirectoryResult extends AbstractTraceResult {
	
	/**
	 * from trace directory - screen_rotations
	 */
	@JsonIgnore
	private int screenRotationCounter = 0;
	
	// Alarm Info
	/**
	 * Epoch time in milliseconds from (trace directory) alarm_info_end/alarm_info_start files
	 */
	@JsonIgnore
	private double dumpsysEpochTimestamp;
	
	/**
	 * Elapsed time in milliseconds from (trace directory) alarm_info_end/alarm_info_start files
	 */
	@JsonIgnore
	private double dumpsysElapsedTimestamp; 
	
	/**
	 * from trace directory - dmesg
	 */
	@JsonIgnore
	private List<AlarmInfo> alarmInfos = null;
	
	/**
	 * from trace directorys - alarm_info_end or alarm_info_start if first file doesn't exist
	 */
	@JsonIgnore
	private List<AlarmAnalysisInfo> alarmStatisticsInfos = null;
	
	/**
	 * a Map of scheduled alarms parsed from alarmStatisticsInfos
	 */
	@JsonIgnore
	private Map<String, List<ScheduledAlarmInfo>> scheduledAlarms = null;

	/**
	 * App Version Info
	 * <br>from trace directory - appname
	 */
	@JsonIgnore
	private Map<String, String> appVersionMap = null;

	/**
	 * Wifi Info
	 * <br>from trace directory - wifi_events
	 */
	@JsonIgnore
	private List<WifiInfo> wifiInfos = null;

	/**
	 * Wakelock Info
	 * <br>from trace directory - batteryinfo_dump
	 */
	@JsonIgnore
	private List<WakelockInfo> wakelockInfos = null;

	/**
	 * Battery Info
	 * <br>from trace directory - battery_events
	 */
	@JsonIgnore
	private List<BatteryInfo> batteryInfos = null;
	
	/**
	 * Temperature Data
	 * <br>from trace directory - temperature
	 */
	@JsonIgnore
	private List<TemperatureEvent> temperatureInfos = null;
	
	/**
	 * Location Data
	 * <br>from trace directory - location_events
	 */
	@JsonIgnore
	private List<LocationEvent> locationInfos = null;
	
	/**
	 * Radio Info
	 * <br>from trace directory - radio_events
	 */
	@JsonIgnore
	private List<RadioInfo> radioInfos = null;

	/**
	 * from trace directory - network_details
	 */
	@JsonIgnore
	private List<NetworkBearerTypeInfo> networkTypeInfos = null;
	
	/**
	 * from trace directory - device_details
	 */
	private DeviceDetail deviceDetail;
	
	/**
	 * from collect options directory - collect_options
	 */
	private CollectOptions collectOptions;
	
	private List<CellInfo> cellInfoList;

	private boolean secureTrace;

	/**
	 * default screen size width<br>
	 * initially set to a default - updated with real size from trace directory
	 * - device_details
	 */
	private int deviceScreenSizeX = 480; //DEFAULT_SCREENSIZE_X;

	/**
	 * screen size height<br>
	 * initially set to a default - updated with real size from trace directory
	 * - device_details
	 */
	private int deviceScreenSizeY = 800; //DEFAULT_SCREENSIZE_Y;

	/**
	 * Event time in nanoseconds
	 * <br>from trace directory - time file line 3
	 */
	@JsonIgnore
	private double eventTime0;
	
	/**
	 * from trace directory - gps_events
	 */
	@JsonIgnore
	private double gpsActiveDuration;
	
	/**
	 * from trace directory - wifi_events
	 */	
	@JsonIgnore
	private double wifiActiveDuration;
	
	/**
	 * from trace directory - bluetooth_events
	 */
	@JsonIgnore
	private double bluetoothActiveDuration;
	
	/**
	 * from trace directory - camera_events
	 */
	@JsonIgnore
	private double cameraActiveDuration;

	/**
	 * List of trace files NOT found in trace directory.<br>
	 * Note: different collectors create different sets of trace files.
	 */
	@JsonIgnore
	private Set<String> missingFiles = null;
	
	/**
	 * from trace directory - network_details
	 */
	@JsonIgnore
	private List<NetworkType> networkTypesList = null;
	
	/**
	 * list of attenuation event 
	 */
	@JsonIgnore
	private  List<AttenuatorEvent> attenautionEvent = null;
	
	/**
	 * list of throttle speed evnt
	 */
	@JsonIgnore
	private List<SpeedThrottleEvent> speedThrottleEvent = null;
	
	/**
	 * Total packets extracted from pcap file.
	 * <br>from trace directory - traffic.cap
	 */
	@JsonIgnore
	private int totalNoPackets = 0;
	
	/**
	 * from trace directory - device_details (7th line)
	 * <br> Note: No visible usage found
	 */
	@JsonIgnore
	private NetworkType networkType;

	@JsonIgnore
	private VideoStreamStartupData videoStreamStartupData;
	
	public CollectOptions getCollectOptions() {
		return collectOptions;
	}

	public void setCollectOptions(CollectOptions collectOptions) {
		this.collectOptions = collectOptions;
	}

	/**
	 * Constructor, Initializes all TraceDirectoryResult objects.
	 */
	public TraceDirectoryResult() {
		super();
		screenRotationCounter = 0;
		alarmInfos = new ArrayList<AlarmInfo>();
		alarmStatisticsInfos = new ArrayList<AlarmAnalysisInfo>();
		scheduledAlarms = new HashMap<String, List<ScheduledAlarmInfo>>();

		appVersionMap = new HashMap<String, String>();

		wifiInfos = new ArrayList<WifiInfo>();
		wakelockInfos = new ArrayList<WakelockInfo>();
		batteryInfos = new ArrayList<BatteryInfo>();
		radioInfos = new ArrayList<RadioInfo>();
		temperatureInfos = new ArrayList<TemperatureEvent>();
		locationInfos = new ArrayList<LocationEvent>();

		networkTypeInfos = new ArrayList<NetworkBearerTypeInfo>();
		networkType = null;

		this.deviceDetail = new DeviceDetail();
		this.collectOptions = new CollectOptions();
		this.attenautionEvent = new ArrayList<AttenuatorEvent>();
		
		deviceScreenSizeX = 480;
		deviceScreenSizeY = 800;
		eventTime0 = 0;

		gpsActiveDuration = 0;
		wifiActiveDuration = 0;
		bluetoothActiveDuration = 0;
		cameraActiveDuration = 0;
		missingFiles = new HashSet<String>();
		networkTypesList = new ArrayList<NetworkType>();
		cellInfoList = new ArrayList<>();
		totalNoPackets = 0;
	}

	/**
	 * @return screen rotations count
	 */
	public int getScreenRotationCounter() {
		return screenRotationCounter;
	}

	/**
	 * Set screen rotations count
	 * @param screenRotationCounter - screen rotations count
	 */
	public void setScreenRotationCounter(int screenRotationCounter) {
		this.screenRotationCounter = screenRotationCounter;
	}
	
	/**
	 * @return Epoch time in milliseconds
	 */
	public double getDumpsysEpochTimestamp() {
		return dumpsysEpochTimestamp;
	}

	/**
	 * Set Epoch time in milliseconds
	 * @param dumpsysEpochTimestamp - Epoch time in milliseconds
	 */
	public void setDumpsysEpochTimestamp(double dumpsysEpochTimestamp) {
		this.dumpsysEpochTimestamp = dumpsysEpochTimestamp;
	}

	/**
	 * @return Elapsed time in milliseconds
	 */
	public double getDumpsysElapsedTimestamp() {
		return dumpsysElapsedTimestamp;
	}

	/**
	 * Set Elapsed time in milliseconds
	 * @param dumpsysElapsedTimestamp - Elapsed time in milliseconds
	 */
	public void setDumpsysElapsedTimestamp(double dumpsysElapsedTimestamp) {
		this.dumpsysElapsedTimestamp = dumpsysElapsedTimestamp;
	}

	/**
	 * @return alarm info from dmesg
	 */
	public List<AlarmInfo> getAlarmInfos() {
		return alarmInfos;
	}

	/**
	 * Set alarm info from dmesg
	 * @param alarmInfos - alarm info from dmesg
	 */
	public void setAlarmInfos(List<AlarmInfo> alarmInfos) {
		this.alarmInfos = alarmInfos;
	}

	/**
	 * @return alarm statistics from alarm_info_end/alarm_info_start
	 */
	public List<AlarmAnalysisInfo> getAlarmStatisticsInfos() {
		return alarmStatisticsInfos;
	}

	/**
	 * Set alarm statistics from alarm_info_end/alarm_info_start
	 * @param alarmStatisticsInfos - alarm statistics from alarm_info_end/alarm_info_start
	 */
	public void setAlarmStatisticsInfos(List<AlarmAnalysisInfo> alarmStatisticsInfos) {
		this.alarmStatisticsInfos = alarmStatisticsInfos;
	}

	/**
	 * @return scheduled alarms - from alarm stats in alarm_info_end/alarm_info_start
	 */
	public Map<String, List<ScheduledAlarmInfo>> getScheduledAlarms() {
		return scheduledAlarms;
	}

	/**
	 * Set scheduled alarms - from alarm stats in alarm_info_end/alarm_info_start
	 * @param scheduledAlarms - scheduled alarms - from alarm stats in alarm_info_end/alarm_info_start
	 */
	public void setScheduledAlarms(Map<String, List<ScheduledAlarmInfo>> scheduledAlarms) {
		this.scheduledAlarms = scheduledAlarms;
	}

	/**
	 * @return Map of app versions
	 */
	public Map<String, String> getAppVersionMap() {
		return appVersionMap;
	}

	/**
	 * Set a Map of app version
	 * @param appVersionMap - a Map of app version
	 */
	public void setAppVersionMap(Map<String, String> appVersionMap) {
		this.appVersionMap = appVersionMap;
	}

	/**
	 * @return a List of WifiInfo
	 */
	public List<WifiInfo> getWifiInfos() {
		return wifiInfos;
	}

	/**
	 * Set a List of WifiInfo
	 * @param wifiInfos - a List of WifiInfo
	 */
	public void setWifiInfos(List<WifiInfo> wifiInfos) {
		this.wifiInfos = wifiInfos;
	}

	/**
	 * @return a List of WakelockInfo
	 */
	public List<WakelockInfo> getWakelockInfos() {
		return wakelockInfos;
	}

	/**
	 * Set a List of WakelockInfo
	 * @param wakelockInfos - a List of WakelockInfo
	 */
	public void setWakelockInfos(List<WakelockInfo> wakelockInfos) {
		this.wakelockInfos = wakelockInfos;
	}

	/**
	 * @return a List of BatteryInfo
	 */
	public List<BatteryInfo> getBatteryInfos() {
		return batteryInfos;
	}

	/**
	 * Set a List of BatteryInfo
	 * @param batteryInfos - a List of BatteryInfo
	 */
	public void setBatteryInfos(List<BatteryInfo> batteryInfos) {
		this.batteryInfos = batteryInfos;
	}
	
	/**
	 * @return a List of TemperatureEvent
	 */
	public List<TemperatureEvent> getTemperatureInfos() {
		return temperatureInfos;
	}

	/**
	 * Set a List of TemperatureEvent Info
	 * @param batteryInfos - a List of TemperatureEvent
	 */
	public void setTemperatureInfos(List<TemperatureEvent> temperatureInfos) {
		this.temperatureInfos = temperatureInfos;
	}
	
	/**
	 * @return a List of LocationEvents
	 */
	public List<LocationEvent> getLocationEventInfos() {
		return locationInfos;
	}
	
	/**
	 * Set a List of LocationEvent Info
	 * @param locationInfos - a List of LocationEvent
	 */
	public void setLocationEventInfos(List<LocationEvent> locationInfos) {
		this.locationInfos = locationInfos;
	}

	/**
	 * @return a List of RadioInfo
	 */
	public List<RadioInfo> getRadioInfos() {
		return radioInfos;
	}

	/**
	 * Set a List of RadioInfo
	 * @param radioInfos - a List of RadioInfo
	 */
	public void setRadioInfos(List<RadioInfo> radioInfos) {
		this.radioInfos = radioInfos;
	}

	/**
	 * @return a List of NetworkBearerTypeInfo
	 */
	public List<NetworkBearerTypeInfo> getNetworkTypeInfos() {
		return networkTypeInfos;
	}

	/**
	 * Set a List of NetworkBearerTypeInfo
	 * @param networkTypeInfos - a List of NetworkBearerTypeInfo
	 */
	public void setNetworkTypeInfos(List<NetworkBearerTypeInfo> networkTypeInfos) {
		this.networkTypeInfos = networkTypeInfos;
	}

	/**
	 * @return network type
	 */
	public NetworkType getNetworkType() {
		return networkType;
	}

	/**
	 * Set network type
	 * @param networkType - network type
	 */
	public void setNetworkType(NetworkType networkType) {
		this.networkType = networkType;
	}

	/**
	 * @return device details - DeviceDetail
	 */
	public DeviceDetail getDeviceDetail() {
		return this.deviceDetail;
	}

	/**
	 * Set device details - DeviceDetail
	 * @param deviceDetail - device details
	 */
	public void setDeviceDetail(DeviceDetail deviceDetail) {
		this.deviceDetail = deviceDetail;
	}

	/**
	 * @return collector name from deviceDetail
	 */
	@JsonIgnore
	public String getCollectorName() {
		return this.deviceDetail.getCollectorName();
	}

	/**
	 * @return device model from deviceDetail
	 */
	@JsonIgnore
	public String getDeviceModel() {
		return this.deviceDetail.getDeviceModel();
	}

	/**
	 * @return device make from deviceDetail
	 */
	@JsonIgnore
	public String getDeviceMake() {
		return this.deviceDetail.getDeviceMake();
	}

	/**
	 * @return os type from deviceDetail ie. LGE
	 */
	@JsonIgnore
	public String getOsType() {
		return this.deviceDetail.getOsType();
	}

	/**
	 * @return os type from deviceDetail ie. android
	 */
	@JsonIgnore
	public String getOsVersion() {
		return this.deviceDetail.getOsVersion();
	}

	/**
	 * @return Version of collector 
	 */
	@JsonIgnore
	public String getCollectorVersion() {
		return this.deviceDetail.getCollectorVersion();
	}

	/**
	 * @return device screen width
	 */
	public int getDeviceScreenSizeX() {
		return deviceScreenSizeX;
	}

	/**
	 * Set device screen width
	 * @param deviceScreenSizeX - device screen width
	 */
	public void setDeviceScreenSizeX(int deviceScreenSizeX) {
		this.deviceScreenSizeX = deviceScreenSizeX;
	}

	/**
	 * @return device screen height
	 */
	public int getDeviceScreenSizeY() {
		return deviceScreenSizeY;
	}

	/**
	 * Set device screen height
	 * @param deviceScreenSizeY - device screen height
	 */
	public void setDeviceScreenSizeY(int deviceScreenSizeY) {
		this.deviceScreenSizeY = deviceScreenSizeY;
	}

	/**
	 * @return event time in nanoseconds
	 */
	public double getEventTime0() {
		return eventTime0;
	}

	/**
	 * Set event time in nanoseconds
	 * @param eventTime0 - event time in nanoseconds
	 */
	public void setEventTime0(double eventTime0) {
		this.eventTime0 = eventTime0;
	}

	/**
	 * @return gps activity duration
	 */
	public double getGpsActiveDuration() {
		return gpsActiveDuration;
	}

	/**
	 * Set gps activity duration
	 * @param gpsActiveDuration - gps activity duration
	 */
	public void setGpsActiveDuration(double gpsActiveDuration) {
		this.gpsActiveDuration = gpsActiveDuration;
	}

	/**
	 * @return wifi activity duration
	 */
	public double getWifiActiveDuration() {
		return wifiActiveDuration;
	}

	/**
	 * Set wifi activity duration
	 * @param wifiActiveDuration - wifi activity duration
	 */
	public void setWifiActiveDuration(double wifiActiveDuration) {
		this.wifiActiveDuration = wifiActiveDuration;
	}

	/**
	 * @return bluetooth  activity duration
	 */
	public double getBluetoothActiveDuration() {
		return bluetoothActiveDuration;
	}

	/**
	 * Set bluetooth  activity duration
	 * @param bluetoothActiveDuration - bluetooth  activity duration
	 */
	public void setBluetoothActiveDuration(double bluetoothActiveDuration) {
		this.bluetoothActiveDuration = bluetoothActiveDuration;
	}

	/**
	 * @return camera activity duration
	 */
	public double getCameraActiveDuration() {
		return cameraActiveDuration;
	}

	/**
	 * Set camera activity duration
	 * @param cameraActiveDuration - camera activity duration
	 */
	public void setCameraActiveDuration(double cameraActiveDuration) {
		this.cameraActiveDuration = cameraActiveDuration;
	}

	/**
	 * @return the Set of missing trace files
	 */
	public Set<String> getMissingFiles() {
		return missingFiles;
	}

	/**
	 * Set the Set of missing trace files
	 * @param missingFiles - Set of missing trace files
	 */
	public void setMissingFiles(Set<String> missingFiles) {
		this.missingFiles = missingFiles;
	}

	/**
	 * @return String of comma separated network types
	 */
	public List<NetworkType> getNetworkTypesList() {		
		return (networkTypesList != null && !networkTypesList.isEmpty()) ? networkTypesList : null;
	}

	/**
	 * Set a List of NetworkType
	 * @param networkTypesList - a List of NetworkType
	 */
	public void setNetworkTypesList(List<NetworkType> networkTypesList) {
		this.networkTypesList = networkTypesList;
	}

	/**
	 * @return total packets extracted from pcap file
	 */
	public int getTotalNoPackets() {
		return totalNoPackets;
	}

	/**
	 * Set total packets extracted from pcap file
	 * @param totalNoPackets - total packets extracted from pcap file
	 */
	public void setTotalNoPackets(int totalNoPackets) {
		this.totalNoPackets = totalNoPackets;
	}
	
	public List<AttenuatorEvent> getAttenautionEvent() {
		return attenautionEvent;
	}
	
	/**
	 * set Attenuation event information
	 */
	public void setAttenautionEvent(List<AttenuatorEvent> attenautionEvent) {
		this.attenautionEvent = attenautionEvent;
	}
	
	public List<SpeedThrottleEvent> getSpeedThrottleEvent() {
		return speedThrottleEvent;
	}
	
	/**
	 * set speed throttle event information
	 * @param speedThrottleEvent
	 */
	public void setSpeedThrottleEvent(List<SpeedThrottleEvent> speedThrottleEvent) {
		this.speedThrottleEvent = speedThrottleEvent;
	}


	/**
	 * Return TraceResultType.TRACE_DIRECTORY to identify that this trace is
	 * collected from a directory
	 * 
	 * @return TraceResultType.TRACE_DIRECTORY
	 */
	@Override
	public TraceResultType getTraceResultType() {
		this.traceResultType = TraceResultType.TRACE_DIRECTORY;
		return traceResultType;
	}

	public VideoStreamStartupData getVideoStartupData() {
		return videoStreamStartupData;
	}

	public void setVideoStartupData(VideoStreamStartupData videoStreamStartupData) {
		this.videoStreamStartupData = videoStreamStartupData;
	}

	public boolean isSecureTrace() {
	    return secureTrace;
	}

	public void setSecureTrace(boolean secureTrace) {
	    this.secureTrace = secureTrace;
	}

	public List<CellInfo> getCellInfoList() {
		return cellInfoList;
	}

	public void setCellInfoList(List<CellInfo> cellInfoList) {
		this.cellInfoList = cellInfoList;
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((collectOptions == null) ? 0 : collectOptions.hashCode());
		result = prime * result + ((deviceDetail == null) ? 0 : deviceDetail.hashCode());
		result = prime * result + deviceScreenSizeX;
		result = prime * result + deviceScreenSizeY;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TraceDirectoryResult other = (TraceDirectoryResult) obj;
		if (collectOptions == null) {
			if (other.collectOptions != null) {
				return false;
			}
		} else if (!collectOptions.equals(other.collectOptions)) {
			return false;
		}
		if (deviceDetail == null) {
			if (other.deviceDetail != null) {
				return false;
			}
		} else if (!deviceDetail.equals(other.deviceDetail)) {
			return false;
		}
		if (deviceScreenSizeX != other.deviceScreenSizeX) {
			return false;
		}
		if (deviceScreenSizeY != other.deviceScreenSizeY) {
			return false;
		}
		return true;
	}
	
}
