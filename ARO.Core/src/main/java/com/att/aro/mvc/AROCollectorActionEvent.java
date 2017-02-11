/*
 *  Copyright 2017 AT&T
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
package com.att.aro.mvc;

import java.awt.event.ActionEvent;
import java.util.Hashtable;

import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.video.pojo.VideoOption;

public class AROCollectorActionEvent extends ActionEvent {

	private static final long serialVersionUID = 1L;
	
	IDataCollector collector;

	private IAroDevice device;
	private String deviceSerialNumber;

	private String trace;

	private Hashtable<String, Object> extraParams;
	private VideoOption videoOption;


	//for Android ,vpn
	public AROCollectorActionEvent(Object source, int eventId, String command, IAroDevice device, String trace, Hashtable<String, Object> extraParams){//VideoOption videoOption, int delayTime, boolean secure) {
		super(source, eventId, command);
		this.collector = null;
		this.device = device;
		this.trace = trace;
		this.extraParams = extraParams;
	}

	/**
	 * for iOS ?
	 * @param source
	 * @param eventId
	 * @param command
	 * @param collector collector to be used
	 * @param deviceSerialNumber serial number of device
	 * @param trace directory
	 * @param videoOption true for video capture, false no video
	 */
	public AROCollectorActionEvent(Object source, int eventId, String command, IDataCollector collector, String deviceSerialNumber, String trace, VideoOption videoOption) {
		super(source, eventId, command);
		this.collector = collector;
		this.deviceSerialNumber = deviceSerialNumber;
		this.trace = trace;
		this.videoOption = videoOption;
	}

	public String getTrace() {
		return trace;
	}


	public IAroDevice getDevice() {
		return device;
	}

	public String getDeviceSerialNumber() {
		return deviceSerialNumber;
	}
	
	public IDataCollector getCollector() {
		return collector;
	}

	public VideoOption getVideoOption() {
		return videoOption;
	}
	
	public Hashtable<String, Object> getExtraParams() {
		return extraParams;
	}

}
