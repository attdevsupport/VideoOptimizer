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
package com.att.aro.datacollector.norootedandroidcollector.pojo;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.pojo.ErrorCode;

/**
 * error code for none-rooted Android Collector start from 400
 * 
 * Date: March 17, 2015
 */
public final class ErrorCodeRegistry {
	private ErrorCodeRegistry() {
	}

	public static ErrorCode getAndroidBridgeFailedToStart() {
		ErrorCode error = new ErrorCode();
		error.setCode(400);
		error.setName("Android Debug Bridge failed to start");
		error.setDescription(ApplicationConfig.getInstance().getVPNCollectorName() + " tried to start Android Debug Bridge service. The service was not started successfully.");
		return error;
	}

	public static ErrorCode getFailToInstallAPK() {
		ErrorCode err = new ErrorCode();
		err.setCode(401);
		err.setName("Failed to install Android App on device");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " tried to install " + ApplicationConfig.getInstance().getVPNCollectorName() + " on device and failed.");
		return err;
	}

	public static ErrorCode getTraceDirExist() {
		ErrorCode err = new ErrorCode();
		err.setCode(402);
		err.setName("Found existing trace directory that is not empty");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " found an existing directory that contains files and did not want to override it. Some files may be hidden.");
		return err;
	}

	public static ErrorCode getNoDeviceConnected() {
		ErrorCode err = new ErrorCode();
		err.setCode(403);
		err.setName("No Android device found.");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " cannot find any Android device plugged into the machine.");
		return err;
	}
	

	/**
	 * no device id matched the device list
	 * 
	 * @return
	 */
	public static ErrorCode getDeviceIdNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(404);
		err.setName("Android device Id or serial number not found.");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " cannot find any Android device plugged into the machine that matched the device ID or serial number you specified.");
		return err;
	}

	public static ErrorCode getFaildedToRunVpnApk() {
		ErrorCode err = new ErrorCode();
		err.setCode(405);
		err.setName("Failed to run VPN APK");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " failed to run Data Collector in device");
		return err;
	}

	/**
	 * failed to create local directory in user's machine to save trace data to.
	 * 
	 * @return
	 */
	public static ErrorCode getFailedToCreateLocalTraceDirectory() {
		ErrorCode err = new ErrorCode();
		err.setCode(406);
		err.setName("Failed to create local trace directory");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " tried to create local directory for saving trace data, but failed.");
		return err;
	}

	public static ErrorCode getTimeoutVpnActivation() {
		ErrorCode err = new ErrorCode();
		err.setCode(407);
		err.setName("VPN activation timeout");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " failed to get the VPN service to activate.");
		return err;
	}

	public static ErrorCode getFailSyncService() {
		ErrorCode err = new ErrorCode();
		err.setCode(411);
		err.setName("Failed to connect to device SyncService");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " failed to get the trace, since it's not able to connect to Device");
		return err;
	}

	public static ErrorCode getDeviceNotOnline() {
		ErrorCode err = new ErrorCode();
		err.setCode(412);
		err.setName("Android device not online.");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " cannot command an Android device that is not online.");
		return err;
	}

	public static ErrorCode getCollectorAlreadyRunning() {
		ErrorCode err = new ErrorCode();
		err.setCode(206);
		err.setName( ApplicationConfig.getInstance().getAppShortName() + ApplicationConfig.getInstance().getVPNCollectorName() + " is already running");
		err.setDescription("There is already a " + ApplicationConfig.getInstance().getVPNCollectorName() +" running on this device. Stop it first, manually, before starting a new trace.");
		return err;
	}
	
	public static ErrorCode getLostConnection(String message) {
		ErrorCode err = new ErrorCode();
		err.setCode(414);
		err.setName("Lost connection");
		err.setDescription("lost AndroidDebugBridge connection" + message);
		return err;
	}

	public static ErrorCode getNotSupported(String message) {
		ErrorCode err = new ErrorCode();
		err.setCode(415);
		err.setName("ABI X86 Not Supported");
		err.setDescription(message);
		return err;
	}

	public static ErrorCode getAdbPullFailure() {
		ErrorCode err = new ErrorCode();
		err.setCode(416);
		err.setName("ADB command failed to pull data.");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() + " was unable to pull trace from target device.");
		return err;
	}
	
	public static ErrorCode getScriptAdapterError(String path){
		ErrorCode err = new ErrorCode();
		err.setCode(417);
		err.setName("Script convert Error");
		err.setDescription(ApplicationConfig.getInstance().getAppShortName() +" was unable to read the profile "+ path);
		return err;
	}
	
}
