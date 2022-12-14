/*
 *  Copyright 2012, 2022 AT&T
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;

public class IOSDeviceInfo {
	private static final Logger LOG = LogManager.getLogger(IOSDeviceInfo.class);
	private static final String IDEVICE_INFO = "ideviceinfo";
	private IExternalProcessRunner extRunner = new ExternalProcessRunnerImpl();
	
	Map<String, String> list;
	String buildversion;
	boolean foundrealscreensize = false;
	String currentFilepath = "";
	ResourceBundle buildBundle = ResourceBundle.getBundle("build");

	public IOSDeviceInfo() {
		list = new HashMap<String, String>();
		buildversion = buildBundle.getString("build.majorversion");
	}

	/**
	 * Retrieved the device information and write to the file
	 * 
	 * @param deviceId is UDID for IOS device
	 * @return
	 */
	public boolean recordDeviceInfo(String deviceId, String filepath) {
		boolean retrievedSuccess = false;
		File exefile = new File(IDEVICE_INFO);
		String result = extRunner.executeCmd("which " + IDEVICE_INFO);
		if (result.contains("/" + IDEVICE_INFO)) {
			exefile = new File(result.trim());
		}
		LOG.debug("exepath :" + exefile.toString());
		this.currentFilepath = filepath;

		if (!exefile.exists()) {
			LOG.error(" " + IDEVICE_INFO + " is not found.");
		} else {
			try {
				String data = getDeviceData(deviceId);
				readData(data);
				writeData(filepath);
				retrievedSuccess = true;
			} catch (IOException e) {
				LOG.error("recordDeviceInfo IOException:", e);
			}
		}
		return retrievedSuccess;
	}

	public String getDeviceData(String deviceId) throws IOException {
		String data = extRunner.executeCmd(IDEVICE_INFO + " -u " + deviceId);
		return data;
	}

	/**
	 * For getting the Device Version. Used for prompt a message to the user.
	 * 
	 * @return
	 */
	public String getDeviceVersion() {
		String deviceVersion = "0";
		if (list != null) {
			deviceVersion = list.get("ProductVersion"); //Since list get when device info called
		}
		return deviceVersion;
	}
	
	public void updateScreensize(int width, int height) {
		if (width > height) {
			list.put("ScreenResolution", height + "*" + width);
		} else {
			list.put("ScreenResolution", width + "*" + height);
		}
		writeData(this.currentFilepath);
		this.foundrealscreensize = true;
	}

	public boolean foundScreensize() {
		return this.foundrealscreensize;
	}

	private void readData(String data) {
		String[] arr = data.split("\\r?\\n");
		String[] tokens;
		String key, value;
		for (String line : arr) {
			tokens = line.split(":");
			if (tokens.length > 1) {
				key = tokens[0].trim();
				value = tokens[1].trim();
				list.put(key, value);
			}
		}
	}

	private void writeData(String filepath) {
		try {
			File file = new File(filepath);
			if (file.exists()) {
				file.delete();
			}
			FileWriter writer = new FileWriter(file, false);
			BufferedWriter bw = new BufferedWriter(writer);
			bw.write("ARO Analyzer/IOS");
			bw.newLine();

			String deviceType;
			String val;
			deviceType = list.get("ProductType");
			if (deviceType != null) {
				bw.write(deviceType);
			} else {
				bw.write("Unknown");
			}
			bw.newLine();

			bw.write("Apple");
			bw.newLine();

			bw.write("IOS");
			bw.newLine();

			val = list.get("ProductVersion");
			if (val != null) {
				bw.write(val);
			} else {
				bw.write("Unknown");
			}
			bw.newLine();

			bw.write(buildversion);
			bw.newLine();

			bw.write("0");
			bw.newLine();

			val = list.get("ScreenResolution");
			if (val == null) {
				bw.write(getScreensize(deviceType));
			} else {
				bw.write(val);
			}
			bw.newLine();

			bw.close();
		} catch (Exception e) {
		    LOG.error("Exception while writing data", e);
		}

	}

	private String getScreensize(String deviceType) {
		this.foundrealscreensize = true;
		if (deviceType != null) {
			if (deviceType.contains("iPhone5") || deviceType.contains("iPhone6")) {return "640*1136";
			} else if (deviceType.contains("iPhone7,2"))   {return "750*1334";	// iPhone 6
			} else if (deviceType.contains("iPhone7,2"))   {return "750*1334";	// iPhone 6
			} else if (deviceType.contains("iPhone8,1"))   {return "1080*1920";	// iPhone 6+
			} else if (deviceType.contains("iPhone8,1"))   {return "750*1334";	// iPhone 6s
			} else if (deviceType.contains("iPhone8,4"))   {return "640*1136";	// iPhone SE
			} else if (deviceType.contains("iPhone8,2"))   {return "1080*1920";	// iPhone 6s+
			} else if (deviceType.contains("iPhone9,1")  || deviceType.contains("iPhone9,3") )  {return "750*1334";	 // iPhone 7
			} else if (deviceType.contains("iPhone9,2")  || deviceType.contains("iPhone9,4") )  {return "1080*1920"; // iPhone 7 Plus
			} else if (deviceType.contains("iPhone10,1") || deviceType.contains("iPhone10,4") ) {return "750*1334";	 // iPhone 8
			} else if (deviceType.contains("iPhone10,2") || deviceType.contains("iPhone10,5") ) {return "1080*1920"; // iPhone 8 Plus
			} else if (deviceType.contains("iPhone10,3") || deviceType.contains("iPhone10,6")	// iPhone X
					|| deviceType.contains("iPhone11,2") ) {return "1125*2436";					// iPhone XS
			} else if (deviceType.contains("iPhone11,6") ) {return "1242*2688";					// iPhone XS Max
			} else if (deviceType.contains("iPhone11,8"))  {return "828*1792";					// iPhone XR
			
			} else if (deviceType.contains("iPad2"))   {return "768*1024";
			} else if (deviceType.contains("iPad3"))   {return "1536*2048"; // iPad 3rd/4th Gen
			} else if (deviceType.contains("iPad4"))   {return "1536*2048";	// iPad Air, mini 2, mini 3
			} else if (deviceType.contains("iPad5"))   {return "1536*2048";	// iPad Air 2, mini 4
			} else if (deviceType.contains("iPad6,7") || deviceType.contains("iPad6,8"))   {return "2048*2732"; // iPad Pro 12.9 inch
			} else if (deviceType.contains("iPad6"))   {return "1536*2048"; // iPad Pro 9.7 inch, iPad 5th Gen
			} else if (deviceType.contains("iPad7,1") || deviceType.contains("iPad7,2"))   {return "2048*2732"; // iPad Pro 12.9 inch
			} else if (deviceType.contains("iPad7,3") || deviceType.contains("iPad7,4"))   {return "1668*2224"; // iPad Pro 10.5 inch
			} else if (deviceType.contains("iPad7,5") || deviceType.contains("iPad7,6"))   {return "1536*2048"; // iPad 6th Gen
			} else if (deviceType.contains("iPad8,5") || deviceType.contains("iPad8,7"))   {return "2048*2732"; // iPad Pro 12.9 inch
			} else if (deviceType.contains("iPad8"))   {return "1668*2388"; // iPad 11 inch
			} else if (deviceType.contains("iPad11,1") || deviceType.contains("iPad11,2"))   {return "1536*2048"; // iPad mini 5th gen
			} else if (deviceType.contains("iPad11"))   {return "1668*2224"; // iPad Air 3rd gen
			}
		}
		this.foundrealscreensize = false;
		return "640*960";
	}
}
