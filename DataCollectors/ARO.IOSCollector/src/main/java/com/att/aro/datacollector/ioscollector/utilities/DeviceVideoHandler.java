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

package com.att.aro.datacollector.ioscollector.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.util.Util;

public final class DeviceVideoHandler {
	private static final Logger LOGGER = LogManager.getLogger(DeviceVideoHandler.class.getName());
	private static ResourceBundle defaultBundle = ResourceBundle.getBundle("messages");

	private static final String IFUSE_VERIFY = "ifuse -V";

	private final IExternalProcessRunner extProcRunner = SpringContextUtil.getInstance().getContext().getBean(IExternalProcessRunner.class);

	private static DeviceVideoHandler INSTANCE = new DeviceVideoHandler();

	private DeviceVideoHandler() {
	}

	public static DeviceVideoHandler getInstance() {
		return INSTANCE;
	}
	
	public String executeCmd(String cmd) {
		return extProcRunner.executeCmd(cmd);
	}

	// as backup method for retrieved iOS version
	public int getIosVersion() {
		String cmd = "instruments -w device";
		String deviceList = extProcRunner.executeCmd(cmd);
		String[] devicesArray = deviceList.split("\n");
		int iosVersion = -1;
		for (String device : devicesArray) {
			if ((!device.contains("Simulator")) && device.contains("iPhone")) {
				try {
					String versionStr = device.substring(device.indexOf("(") + 1, device.indexOf(")"));
					if (versionStr.indexOf(".") != -1) {
						iosVersion = Integer.valueOf(versionStr.substring(0, versionStr.indexOf(".")));
					}
				} catch (NumberFormatException e) {
					LOGGER.error("Non numeric value cannot represent ios version: " + iosVersion);
				}
				break;
			}
		}
		return iosVersion;
	}

	public boolean verifyIFuse() {
		String result = extProcRunner.executeCmdRunner(IFUSE_VERIFY, true, "success", true);
		return result.contains(defaultBundle.getString("Message.ifuse.verify"));
	}

}