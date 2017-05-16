/*
 *  Copyright 2015 AT&T
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
package com.att.aro.core.adb;

import java.io.IOException;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncService;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.settings.Settings;

/** 
 * Provides access to AndroidDebugBridge (ADB)
 * 
 * Designed to work with ddmlib r24.0.1 current release as of Jan 2015
 * 
 */
public interface IAdbService {

	void setFileManager(IFileManager fileManager);

	void setAROConfigFile(Settings configFile);

	/**
	 * check if ADB location was set.
	 * 
	 * @return
	 */
	boolean hasADBpath();

	/**
	 * @return the adb file path
	 */
	String getAdbPath();

	/**
	 * adb path might have been set but the file does not exist or has been
	 * deleted
	 * 
	 * @return true if exists
	 */
	boolean isAdbFileExist();

	/**
	 * Perform an AndroidDebugBridge.init if needed.
	 *
	 * @param adbPath path to location of adb
	 * @return adb - AndroidDebugBridge
	 */
	AndroidDebugBridge initCreateBridge(String adbPath);

	/**
	 * will start ADB service if not started, given that ADB path is set.
	 * 
	 * @return adb object if launched successfully
	 */
	AndroidDebugBridge ensureADBServiceStarted();
	
	/**
	 * Find and return an array of (IDevice) Android devices and emulators
	 * 
	 * @return array of IDevice
	 * @throws Exception if AndroidDebugBridge is invalid or fails to connect
	 */
	IDevice[] getConnectedDevices() throws IOException;

	/**
	 * traverse Android filesystem to locate file/folder
	 * 
	 * @param root of current directory
	 * @param path to follow
	 * @return FileEntry of target
	 */
	FileEntry locate(IDevice device, FileEntry root, String path);

	/**
	 * 
	 * @param aroDevice an AroDevice
	 * @param tempFolder
	 * @param payloadFileName
	 * @param remotepath
	 * @return
	 */
	boolean installPayloadFile(IAroDevice aroDevice, String tempFolder, String payloadFileName, String remotepath);
	
	/**
	 * 
	 * @param device an IDevice
	 * @param tempFolder
	 * @param payloadFileName
	 * @param remotepath
	 * @return
	 */
	boolean installPayloadFile(IDevice device, String tempFolder, String payloadFileName, String remotepath);

	/**
	 * Uses ddmlib to pull a file
	 * 
	 * @param service
	 * @param remotePath
	 * @param file
	 * @param localFolder
	 * @return
	 */
	boolean pullFile(SyncService service, String remotePath, String file, String localFolder);
}
