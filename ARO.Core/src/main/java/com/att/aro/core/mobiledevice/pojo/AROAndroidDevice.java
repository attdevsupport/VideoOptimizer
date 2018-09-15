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

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.android.impl.AndroidImpl;
import com.att.aro.core.datacollector.IDataCollector;

public class AROAndroidDevice implements IAroDevice {

	private final Platform platform = IAroDevice.Platform.Android;

	AroDeviceState state = AroDeviceState.Unknown; // Available, in-use, Unknown
	
	private IDevice device;

	boolean rootflag;

	private IDataCollector dataCollector = null;

	private AroDeviceState devState = null;

	private String abi;
	
	private IAndroid android;

	public AROAndroidDevice(IDevice device, boolean rootflag) {
		this.device = device;
		this.rootflag = rootflag;
		android = new AndroidImpl();
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
		+ ", state:" + getState()
		+ ", api:" + getApi()
		+ ", abi:" + getAbi()
		+ ", id:" + getId() 
		+ ", model:" + getModel() 
		+ ", DevName:" + getDeviceName());
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
}
