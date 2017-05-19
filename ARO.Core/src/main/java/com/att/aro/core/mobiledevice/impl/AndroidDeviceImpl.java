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
package com.att.aro.core.mobiledevice.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;
import com.att.aro.core.ILogger;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.android.pojo.ShellOutputReceiver;
import com.att.aro.core.mobiledevice.IAndroidDevice;
import com.att.aro.core.mobiledevice.pojo.RootCheckOutputReceiver;
import com.att.aro.core.model.InjectLogger;

public class AndroidDeviceImpl implements IAndroidDevice {

	@InjectLogger
	private ILogger logger;

	private IAndroid android;
	@Autowired
	public void setAndroid(IAndroid android) {
		this.android = android;
	}
	
	@Autowired
	public void setLogger(ILogger logger) {
		this.logger = logger;
	}
	
	public RootCheckOutputReceiver makeRootCheckOutputReceiver() {
		return new RootCheckOutputReceiver();
	}

	/**
	 * Check Android device for SELinux enforcement
	 * 
	 * @param device - a real device or emulator
	 * @return true if SELinux-Enforced, false if permissive
	 * @throws Exception
	 */
	@Override
	public boolean isSeLinuxEnforced(IDevice device) throws Exception {

		if (device == null) {
			throw new Exception("device is null");
		}

		ShellOutputReceiver shSELinux = new ShellOutputReceiver();
		device.executeShellCommand("getenforce", shSELinux);

		boolean seLinuxEnforced = shSELinux.isSELinuxEnforce();
		logger.info("--->seLinuxEnforced:" + seLinuxEnforced);

		return seLinuxEnforced;
	}
	
	/**
	 * Check if a connected Android device is rooted or not.
	 * <p>performs 'su -c id' on Android<br>
	 * a response containing "uid=0(root) gid=0(root)" is considered rooted</p>
	 * 
	 * @throws IOException
	 */
	@Override
	public boolean isAndroidRooted(IDevice device) throws Exception {

		if (device == null) {
			throw new Exception("device is null");
		}

		DeviceState state = device.getState();
		if (state.equals(DeviceState.UNAUTHORIZED)) {
			return false;
		}

		for (String cmd : new String[] { "su -c id", "id" }) {
			String[] res = android.getShellReturn(device, cmd);
			for (String string : res) {
				if (string.contains("uid=0(root) gid=0(root)")) {
					return true;
				}
			}
		}

		return false;
	}

}
