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

package com.att.aro.core.adb.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.FileListingService;
import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.AROAndroidDevice;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.resourceextractor.IReadWriteFileExtractor;
import com.att.aro.core.settings.Settings;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.Util;

/** 
 * Provides access to AndroidDebugBridge (ADB)
 * 
 * Designed to work with ddmlib r24.0.1 current release as of Jan 2015
 * 
 * Current dependencies are:
 *	   libs/ddmlib-26.1.2.jar
 * 	   libs/guava-22.0.jar
 *	   libs/kxml2-2.3.0.jar
 *	   libs/annotations-26.1.2.jar
 *	   libs/common-26.1.2.jar
 *	   libs/jsr305-2.0.1
 *	   libs/error_prone_annotations-2.0.18.jar
 *     libs/j2objc-annotations-1.1.jar
 *     libs/animal-sniffer-annotations-1.14
 *
 * <p>see: http://mvnrepository.com/artifact/com.android.tools.ddms/ddmlib</p>
 * 
 */
public class AdbServiceImpl implements IAdbService {

	private static final Logger LOGGER = LogManager.getLogger(AdbServiceImpl.class.getName());
	private IExternalProcessRunner extrunner;
	private IFileManager fileManager;
	private Settings configFile;
	private IAndroid android;
	
	@Autowired
	public void setAndroid(IAndroid android){
		this.android = android;
	}
		
	@Autowired
	public void setExternalProcessRunner(IExternalProcessRunner runner) {
		this.extrunner = runner;
	}
	
	private IReadWriteFileExtractor extractor;	
	@Autowired
	public void setFileExtactor(IReadWriteFileExtractor extractor) {
		this.extractor = extractor;
	}
	
	@Autowired
	public void setFileManager(IFileManager fileManager) {
		this.fileManager = fileManager;
	}

	@Autowired
	public void setAROConfigFile(Settings configFile) {
		this.configFile = configFile;
	}

	private String getADBAttributeName() {
		return SettingsImpl.ConfigFileAttributes.adb.name();
	}
	private String getAROConfigFileLocation() {
		return configFile.getAttribute(getADBAttributeName());
	}

	/**
	 * check if ADB location was set in settings.properties
	 * 
	 * @return
	 */
	@Override
	public boolean hasADBpath() {
		boolean result = false;
		String adbPath = getAROConfigFileLocation();
		if (adbPath != null && adbPath.length() > 3) {
			result = true;
		}
		return result;
	}

	/**
	 * Confirms and returns adb path
	 * @return the adb file path
	 */
	@Override
	public String getAdbPath() {
		return verifyAdbPath(getAROConfigFileLocation(), false);
	}

	@Override
	public String getAdbPath(boolean unfiltered) {
		return verifyAdbPath(getAROConfigFileLocation(), unfiltered);
	}
	/**
	 * <pre>Confirm adbPath, attempt repair from environmental variables. 
	 *  ANDROID_HOME - path to the android sdk
	 *  ANDROID_ADB - path directly to the executable adb or adb.exe
	 * @param adbPath path where adb should be found
	 * @return path if adb is found, false if adb cannot be located
	 */
	private String verifyAdbPath(String adbPath, boolean unfiltered) {

		if (adbPath == null || !fileManager.fileExist(adbPath)) {

			String[] paths = { System.getenv("ANDROID_ADB"), System.getProperty("ANDROID_ADB")
					, System.getenv("ANDROID_HOME") + Util.FILE_SEPARATOR + "platform-tools" + Util.FILE_SEPARATOR + "adb" 
					, System.getenv("ANDROID_HOME") + Util.FILE_SEPARATOR + "platform-tools" + Util.FILE_SEPARATOR + "adb.exe" 
					};

			for (String path : paths) {
				if (path != null && fileManager.fileExist(path)) {
					LOGGER.debug(path);
					configFile.setAndSaveAttribute(getADBAttributeName(), path);
					return Util.validateInputLink(path);
				}
			}

			LOGGER.error("failed to repair ADB path, no useful environmental variables (see: ANDROID_ADB, ANDROID_HOME)");
			return null;

		}
		return (Util.isMacOS() && !unfiltered) ? Util.escapeChars(adbPath) : Util.validateInputLink(adbPath);
	}
	
	/**
	 * adb path might have been set but the file does not exist or has been
	 * deleted
	 * 
	 * @return true if exists
	 */
	@Override
	public boolean isAdbFileExist() {
		return fileManager.fileExist(getAROConfigFileLocation());
	}

	/**
	 * will retrieve ADB service if active
	 * 
	 * @return true if launched successfully
	 */
	private boolean checkAdb(AndroidDebugBridge adb) {

		boolean result = false;

		// watch for device
		int attempt = 10;
		while (attempt-- > 0 && !adb.hasInitialDeviceList()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
		}
		if (adb.hasInitialDeviceList()) {
			result = true;
		}
		return result;
	}

	/**
	 * Perform an AndroidDebugBridge.init if needed.
	 *
	 * @param adbPath path to location of adb
	 * @return adb - AndroidDebugBridge
	 */
	@Override
	public AndroidDebugBridge initCreateBridge(String adbPath) {

		AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();

		if (bridge == null) {
			AndroidDebugBridge.init(false);
			bridge = AndroidDebugBridge.createBridge(adbPath, false);
		}
		return bridge;
	}

	/*
	 * Note: The following code is to be avoided, it only instantiates
	 * AndroidDebugBridge It does not make a connection to the process 'adb' nor
	 * does it launch it if not running.
	 * 
	 * adb = AndroidDebugBridge.createBridge();
	 */

	/**
	 * will start ADB service if not started, given that ADB path is set.
	 * 
	 * @return adb object if launched successfully
	 */
	@Override
	public AndroidDebugBridge ensureADBServiceStarted() {
		//try to connect to a running ADB service first.
		AndroidDebugBridge adb = null;

		adb = AndroidDebugBridge.getBridge();
		if (adb != null && adb.isConnected()) {
			return adb;
		}

		String adbPath = getAdbPath(true);
		if (adbPath != null && adbPath.length() > 3) {

			adb = initCreateBridge(adbPath);

			if (adb != null) {
				int attempt = 1;
				while (attempt-- > 0 && !checkAdb(adb)) {
					adb = AndroidDebugBridge.createBridge(adbPath, true);
				}
				if (adb == null || !adb.hasInitialDeviceList()) {
					LOGGER.error("ADB bridge not working or failed to connect.");
					adb = null;
				}
			} else {
				LOGGER.info("Failed to create ADB Bridge, unable to connect.");
			}

		} else {
			LOGGER.info("No ADB path found, failed to create ADB Bridge.");
		}
		return adb;
	}

	boolean runAdbCommand() {
		//assume that user has adb environment set, try running command line: adb devices
		String lines = "";
		try {
			lines = extrunner.runGetString("adb devices");
			//logger.debug("result of command 'adb devices':"+ lines);
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		if (lines.contains("command not found") 
				|| lines.contains("not recognized") 
				|| lines.contains("No such file") 
				|| lines.contains("Cannot run")) {
			return false;
		}
		if (lines.length() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Find and return an array of (IDevice) Android devices and emulators
	 * 
	 * @return array of IDevice
	 * @throws Exception if AndroidDebugBridge is invalid or fails to connect
	 */
	@Override
	public IDevice[] getConnectedDevices() throws IOException{
		AndroidDebugBridge adb = ensureADBServiceStarted();
		if (adb == null) {
			LOGGER.debug("failed to connect to existing bridge, now trying running adb from environment");
			throw new IOException("AndroidDebugBridge failed to start");
		}

		int waitcount = 1;
		while (waitcount <= 20 && !adb.hasInitialDeviceList()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
			waitcount++;
		}

		return adb.getDevices();
	}

	/**
	 * traverse Android filesystem to locate file/folder
	 * 
	 * @param root of current directory
	 * @param path to follow
	 * @return FileEntry of target
	 */
	@Override
	public FileEntry locate(IDevice device, FileEntry rootPath, String path) {
		FileListingService fileService = device.getFileListingService();
		FileEntry root = rootPath;
		if (root == null) {
			root = fileService.getRoot();
		}
		
		fileService.getChildren(root, false, null); // root gets populated with children
		if (path == null) {
			return root;
		}

		String[] pathNodes = path.split("/");
		FileEntry node = root;
		FileEntry nnode = root;
		for (String pathNode : pathNodes) {
			if (!"".equals(pathNode)) {
				nnode = node.findChild(pathNode);
				if (nnode == null) {
					return null;
				}
				fileService.getChildren(nnode, false, null); // nnode gets populated with children
				// traverse to next node
				node = nnode;
			}
		}
		return node;
	}

	/**
	 * Uses ddmlib to pull a file
	 * 
	 * @param service
	 * @param remotePath
	 * @param file
	 * @param localFolder
	 * @return true if success, fail if failure
	 */
	public boolean pullFile(SyncService service, String remotePath, String file, String localFolder) {
		
		try {
			service.pullFile(
					  remotePath + file
		            , localFolder + "/" + file
					, SyncService.getNullProgressMonitor());
		} catch (SyncException | TimeoutException | IOException e) {
			LOGGER.error("pull " + file + ":" + e.getMessage());
			return false;
		}
		return true;
	}


	@Override
	public boolean installPayloadFile(IAroDevice aroDevice, String tempFolder, String payloadFileName, String remotepath) {
		return installPayloadFile((IDevice)aroDevice.getDevice(), tempFolder, payloadFileName, remotepath);
	}

	@Override
	public boolean installPayloadFile(IDevice device, String tempFolder, String payloadFileName, String remotepath) {

		ClassLoader loader = AROAndroidDevice.class.getClassLoader();
		String payloadTempPath = tempFolder + Util.FILE_SEPARATOR + payloadFileName;
		boolean success = extractor.extractFiles(payloadTempPath, payloadFileName, loader);

		if (success) {
			success = android.pushFile(device, payloadTempPath, remotepath);
			fileManager.deleteFile(payloadTempPath);
		}

		return success;
	}

	@Override
	public String[] getApplicationList(String id) {
		if(StringUtils.isEmpty(id)) {
			return new String[] {"Select a device"};
		} else {
			String path = getAdbPath();
			String response = extrunner.executeCmd(path + " -s " + id + " shell pm list packages");
			String[] responseParts = response.split("\n");
			List<String> applications = new ArrayList<>();
			for(int i = 0; i < responseParts.length; i++) {
				if(responseParts[i] != null && responseParts[i].length() > 8) {
	                applications.add(responseParts[i].substring(8, responseParts[i].length()).trim());
	          }
			}
			return applications.toArray(new String[applications.size()]);
		}
	}

}
