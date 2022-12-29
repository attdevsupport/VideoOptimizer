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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;
import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.android.impl.AndroidImpl;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.parse.DevInterface;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;

import lombok.Getter;
import lombok.Setter;

public class AROAndroidDevice implements IAroDevice {

	private final Platform platform = IAroDevice.Platform.Android;	
	
	private static final IExternalProcessRunner EXTERNAL_PROCESS_RUNNER = SpringContextUtil.getInstance().getContext()
			.getBean(IExternalProcessRunner.class);
	private static final Logger LOG = LogManager.getLogger(AROAndroidDevice.class.getName());

	private DevInterface devInterface = new DevInterface();
	
	AroDeviceState state = AroDeviceState.Unknown; // Available, in-use, Unknown
	
	private IDevice device;
	boolean rootflag;
	private IDataCollector dataCollector = null;
	private AroDeviceState devState = null;
	private String abi;
	private IAndroid android;

	@Setter @Getter private boolean timingOffset;
	
	@Setter @Getter private String voTimeZoneID;
	@Setter @Getter private double voTimestamp;
	
	@Getter	private Double deviceTimestamp = 0D;
	@Getter private String deviceTimeZoneID;	// America/Los_Angeles

	@Setter
	@Getter
	private List<String[]> ipAddressList;
	@Setter
	@Getter
	private List<String[]> voIpAddressList;

	public AROAndroidDevice(IDevice device, boolean rootflag) {
		this.device = device;
		this.rootflag = rootflag;
		android = new AndroidImpl();
		try {
			deviceTimeZoneID = device.getSystemProperty("persist.sys.timezone").get(1, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOG.error("Failed to get TimeZoneID from phone :", e);
		}
	}
	
	@Override
	public Double obtainDeviceTimestamp() {
		String[] commands = Util.getCommand("adb");
		File commandFileParent = commands[0] != null ? new File(commands[0]) : null;
		String cmd = String.format("%s -s %s shell 'echo $EPOCHREALTIME'", commands[1], device.getSerialNumber());
		if (Util.isWindowsOS()) {
			cmd = cmd.replaceAll("'", "\"");
		}
		String epochRealTime = EXTERNAL_PROCESS_RUNNER.executeCmd(commandFileParent, cmd, true, true);
		if (epochRealTime.trim().matches("^[0-9\\.]*")) {
			deviceTimestamp = StringParse.stringToDouble(epochRealTime, 0);
		} else {
			LOG.error("Problem with Android:" + epochRealTime);
		}
		return deviceTimestamp;
	}

	@Override
	public List<String[]> obtainDeviceIpAddress() {
		List<String[]> ipList = new ArrayList<>();
		
		String[] commands = Util.getCommand("adb");
		File commandFileParent = commands[0] != null ? new File(commands[0]) : null;
		String cmd = String.format("%s -s %s shell ip addr", commands[1], device.getSerialNumber());
		if (Util.isWindowsOS()) {
			cmd = cmd.replaceAll("'", "\"");
		}
		String ipAddr = EXTERNAL_PROCESS_RUNNER.executeCmd(commandFileParent, cmd, true, true);

		ipAddr = ipAddr.replaceAll("addr: ", "").replaceAll("addr:", "");

		return devInterface.parseLinuxIP(ipList, ipAddr);
	}

	@Override
	public double getTimeDiff() {
		return voTimestamp - deviceTimestamp;
	}
	
	/**
	 * Returns an IDevice
	 */
	public Object getDevice() {
		return device;
	}
	
	/**
	 * IDevice delivers these states
	 * 
	 * BOOTLOADER("bootloader") OFFLINE("offline") ONLINE("device")
	 * RECOVERY("recovery") UNAUTHORIZED("unauthorized")
	 */
	@Override
	public AroDeviceState getState() {
		if (devState == null) {
			switch (device.getState()) {
			case ONLINE:
				devState = AroDeviceState.Available;
				break;
			case BOOTLOADER:
				devState = AroDeviceState.Unknown;
				break;
			case RECOVERY:
				devState = AroDeviceState.Unknown;
				break;
			case OFFLINE:
				devState = AroDeviceState.Offline;
				break;
			case UNAUTHORIZED:
				devState = AroDeviceState.Unauthorized;
				break;

			default:
				break;
			}
		}

		return devState;
	}

	@Override
	public String getId() {
		return device.getSerialNumber();
	}

	@Override
	public String getDeviceName() {
		String name = device.getAvdName();
		if (name == null) {
			name = device.getName();
		}
		return name;
	}

	@Override
	public String getOS() {
		return getPropertyValue(IDevice.PROP_BUILD_VERSION);
	}

	@Override
	public String getApi() {
		return getPropertyValue(IDevice.PROP_BUILD_API_LEVEL);
	}

	@Override
	public String getModel() {	
		return getPropertyValue(IDevice.PROP_DEVICE_MODEL);
	}

	@Override
	public String getProductName() {
		String productName = null;
		if (getId().startsWith("emulator")) {
			productName = "emulator";
		} else {
			if (isAuthorized()) {
				productName = getPropertyValue(IDevice.PROP_DEVICE_MANUFACTURER);
			} else {
				productName = "Unauthorized";
			}
		}
		return productName;
	}

	private boolean isAuthorized() {
		return !device.getState().equals(DeviceState.UNAUTHORIZED);
	}

	@Override
	public boolean isRooted() {
		return rootflag;
	}

	@Override
	public boolean isEmulator() {
		return device.isEmulator();
	}

	/**
	 * Returns values such as armeabi, x86 or x86_64
	 */
	@Override
	public String getAbi() {
		
		if (abi != null) {
			return abi;
		}
		
		String abiList = getPropertyValue(IDevice.PROP_DEVICE_CPU_ABI_LIST);
		
		if (abiList != null) {	    
		
			String[] abis = abiList.split(",");
		 
			if (abis.length != 0) {
				
			    abi = removeLineFeed(abis[0]);
			    return abi;
	 	    }
	    } 	        

		String abi1 = getPropertyValue(IDevice.PROP_DEVICE_CPU_ABI);
	       
        if (abi1 != null) {  
        	abi = removeLineFeed(abi1);
        	return abi;        
        } 
            
	    String abi2 = getPropertyValue(IDevice.PROP_DEVICE_CPU_ABI2);
            
        if (abi2 != null) {
            abi = removeLineFeed(abi2);
            return abi;
       }      
	
	   return "unknown";
	}

	@Override
	public Platform getPlatform() {
		return IAroDevice.Platform.Android;
	}

	@Override
	public boolean isPlatform(Platform platform) {
		return (this.platform.equals(platform));
	}

	@Override
	public String toString() {
		return new String("Android:"+getProductName() 
		+ (rootflag?" (rooted)":"")
		+ ", deviceTimeZoneID:" + getDeviceTimeZoneID()
		+ ", deviceTimestamp:" + getDeviceTimestamp()
		+ ", state:" + getState()
		+ ", api:" + getApi()
		+ ", abi:" + getAbi()
		+ ", id:" + getId() 
		+ ", model:" + getModel() 
		+ ", DevName:" + getDeviceName()
		);
}

	@Override
	public void setCollector(IDataCollector collectorOption) {
		this.dataCollector = collectorOption;
	}

	@Override
	public IDataCollector getCollector() {
		return this.dataCollector;
	}

	private String getPropertyValue(String property) {
		
		String cmd = "getprop " + property;
		String[] cmdOutput = android.getShellReturn(device, cmd);

		if (cmdOutput != null && cmdOutput.length > 0) {
			return removeLineFeed(cmdOutput[0]);
		}
		
		return null;		
	}
	
	private String removeLineFeed(String propValue) {
		return propValue.replaceAll("\\n", "");
	}

	@Override
	public void setStatus(AroDeviceState state) {
		this.devState = state;

	}
	
	public boolean isAPKInstalled() {
		if (Util.APK_FILE_NAME.contains("%s")) {
			ClassLoader loader = AROAndroidDevice.class.getClassLoader();
			for (int i = 0; i < 500; i++) {
				String tempFile = String.format(Util.APK_FILE_NAME, i);
				if (loader.getResource(tempFile) != null) {
					Util.APK_FILE_NAME = tempFile;
					break;
				}
			}
		}
		String deviceAPKVersion = null;
		String newAPKVersion = StringParse.findLabeledDataFromString("VPNCollector-", ".apk", Util.APK_FILE_NAME);
		String cmdAROAPKVersion = "dumpsys package " + Util.ARO_PACKAGE_NAME + " | grep versionName";
		String[] result = android.getShellReturn(this.device, cmdAROAPKVersion);
		if (result != null && (result.length > 0) && result[0] != null) {
			deviceAPKVersion = StringParse.findLabeledDataFromString("versionName=", "=", result[0].trim());
		}
		return (Objects.equals(deviceAPKVersion, newAPKVersion));
	}

}
