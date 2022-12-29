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
package com.att.aro.core.mobiledevice.pojo;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.android.ddmlib.IDevice;
import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.android.AndroidApiLevel;
import com.att.aro.core.mobiledevice.IAndroidDevice;

public class AroDevices implements IAroDevices {

	private static final Logger LOG = LogManager.getLogger(AroDevices.class.getName());

	ArrayList<IAroDevice> deviceList = null;

	@Autowired
	private IAndroidDevice androidDev;

	public AroDevices() {
		deviceList = new ArrayList<IAroDevice>();
		if (androidDev == null) {
			androidDev = SpringContextUtil.getInstance().getContext().getBean(IAndroidDevice.class);
		}
	}

	/**
	 * A convenience constructor
	 * 
	 * @param androidDevices
	 */
	public AroDevices(IDevice[] androidDevices) {
		this();
		addDeviceArray(androidDevices);
	}

	/**
	 * A convenience constructor
	 * 
	 * @param aroDeviceArray
	 */
	public AroDevices(IAroDevice[] aroDeviceArray) {
		this();
		addDeviceArray(aroDeviceArray);
	}

	@Override
	public void addDeviceArray(IDevice[] androidDevices) {
		if (androidDevices != null) {
			// filter out x86 emulators
			for (IDevice device : androidDevices) {
				if ((!device.isEmulator() || !device.getAbis().contains("x86"))) {
					IAroDevice aroDevice = new AROAndroidDevice(device, isRooted(device));
					int api = aroDevice.getApi() == null ? 0 : Integer.valueOf(aroDevice.getApi());
					if (api >= AndroidApiLevel.K14.levelNumber()) {
						deviceList.add(aroDevice);
					}
				}
			}
		}
	}
	
	@Override
	public void addDeviceArray(IAroDevice[] aroDeviceArray) {
		if (aroDeviceArray != null) {
			for (IAroDevice aroDevice : aroDeviceArray) {
				deviceList.add(aroDevice);
			}
		}
	}

	private boolean isRooted(IDevice device) {
		boolean rootFlag = false;
		try {
			rootFlag = androidDev.isAndroidRooted(device);
		} catch (Exception e) {
			LOG.error("Failed to determine rootflag:" + e.getMessage());
		}
		return rootFlag;
	}

	@Override
	public String getId(int selection) {
		return deviceList.get(selection).getId();
	}

	@Override
	public IAroDevice getDevice(int selection) {
		return deviceList.get(selection);
	}

	@Override
	public int size() {
		return deviceList.size();
	}

	@Override
	public ArrayList<IAroDevice> getDeviceList() {
		return deviceList;
	}

	@Override
	public String toString() {
		if (deviceList.isEmpty()) {
			return "No devices found";
		}
		StringBuilder strbldr = new StringBuilder();
		for (int i = 0; i < deviceList.size(); i++) {
			strbldr.append(i);
			strbldr.append(" - ");
			strbldr.append(deviceList.get(i));
			strbldr.append('\n');
		}
		return strbldr.toString();
	}

}
