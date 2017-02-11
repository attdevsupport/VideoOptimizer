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

package com.att.aro.datacollector.norootedandroidcollector.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.AndroidApiLevel;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.concurrent.IThreadExecutor;
import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.IDeviceStatus;
import com.att.aro.core.datacollector.IVideoImageSubscriber;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.AROAndroidDevice;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.resourceextractor.IReadWriteFileExtractor;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.IScreenRecorder;
import com.att.aro.core.video.IVideoCapture;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.datacollector.norootedandroidcollector.pojo.ErrorCodeRegistry;

public class NorootedAndroidCollectorImpl implements IDataCollector, IVideoImageSubscriber {

	private ILogger log;
	private volatile boolean running = false;
	private IReadWriteFileExtractor extractor;
	private IThreadExecutor threadexecutor;
	private List<IVideoImageSubscriber> videoImageSubscribers = new ArrayList<IVideoImageSubscriber>();
	private IVideoCapture videoCapture;
	private VideoOption videoOption = VideoOption.NONE;
	private IDevice device;
	private IAroDevice aroDevice;

	private IAdbService adbservice;
	private IFileManager filemanager;
	// local directory in user machine to pull trace from device to
	private String localTraceFolder;
	private static final int MILLISECONDSFORTIMEOUT = 300;
	private static final String APK_FILE_NAME = "VPNCollector-1.0.0.apk";
	private static final String ARO_PACKAGE_NAME = "com.att.arocollector";

	private IAndroid android;
	private boolean usingScreenRecorder = false;

	private static final String[] WIN_RUNTIME = { "cmd.exe", "/C" };
	private static final String[] OS_LINUX_RUNTIME = { "/bin/bash", "-l", "-c" };

	// files to pull from device
	private String[] mDataDeviceCollectortraceFileNames = { "cpu", "appid", "appname", "time", "processed_events",
			"active_process", "battery_events", "bluetooth_events", "camera_events", "device_details", "device_info",
			"gps_events", "network_details", "prop", "radio_events", "screen_events", "screen_rotations",
			"user_input_log_events", "alarm_info_start", "alarm_info_end", "batteryinfo_dump", "dmesg", "video_time",
			"wifi_events", "traffic.cap", "collect_options" };

	@Autowired
	public void setLog(ILogger log) {
		this.log = log;
	}

	@Autowired
	public void setAndroid(IAndroid android) {
		this.android = android;
	}

	private IExternalProcessRunner extrunner;

	@Autowired
	public void setExternalProcessRunner(IExternalProcessRunner runner) {
		this.extrunner = runner;
	}

	@Autowired
	public void setFileManager(IFileManager filemanager) {
		this.filemanager = filemanager;
	}

	public void setDevice(IDevice aDevice) {
		this.device = aDevice;
	}

	@Autowired
	public void setAdbService(IAdbService adbservice) {
		this.adbservice = adbservice;
	}

	@Autowired
	public void setVideoCapture(IVideoCapture videocapture) {
		this.videoCapture = videocapture;
	}

	@Autowired
	private IScreenRecorder screenRecorder;

	private CpuTraceCollector cpuTraceCollector;

	@Autowired
	public void setThreadExecutor(IThreadExecutor thread) {
		this.threadexecutor = thread;
	}

	@Autowired
	public void setFileExtactor(IReadWriteFileExtractor extractor) {
		this.extractor = extractor;
	}

	@Override
	public String getName() {
		return "VPN Android Collector";
	}

	@Override
	public void addDeviceStatusSubscriber(IDeviceStatus subscriber) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addVideoImageSubscriber(IVideoImageSubscriber subscriber) {
		videoCapture.addSubscriber(subscriber);
	}

	/**
	 * receive video frame from background capture thread, then forward it to subscribers
	 */
	@Override
	public void receiveImage(BufferedImage videoimage) {
		log.debug("receiveImage");
		for (IVideoImageSubscriber subscriber : videoImageSubscribers) {
			subscriber.receiveImage(videoimage);
		}
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public String getMinorVersion() {
		return "0.0.1";
	}

	@Override
	public DataCollectorType getType() {
		return DataCollectorType.NON_ROOTED_ANDROID;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	public void stopRunning() {
		this.running = false;
	}

	/**
	 * 
	 * @return logcat dump from device
	 */
	@Override
	public String[] getLog() {
		return android.getShellReturn(device, "echo ''Android version :'';getprop ro.build.version.release;logcat -d");
	}

	private Object getOrDefault(Hashtable<String, Object> extraParams, String key, Object defval) {
		Object val = extraParams.get(key);
		if (val == null) {
			val = defval;
		}
		return val;
	}

	@Override
	public StatusResult startCollector(boolean isCommandLine, String tracepath, VideoOption videoOption_deprecated,
			String password) {
		return this.startCollector(isCommandLine, tracepath, videoOption_deprecated, false, null, null, password);
	}

	/**
	 * Start collector in background and returns result which indicates success or error and detail data.
	 * 
	 * @param folderToSaveTrace
	 *            directory to save trace to
	 * @param videoOption
	 *            optional flag to capture video of device. default is false
	 * @param isLiveViewVideo
	 *            ignored here
	 * @param androidId
	 *            optional id of device to capture. default is the connected device.
	 * @param extraParams
	 *            optional data to pass to collectors. required by some collectors.
	 * @return a StatusResult to hold result and success or failure
	 */
	@Override
	public StatusResult startCollector(boolean isCommandLine, String folderToSaveTrace,
			VideoOption videoOption_deprecated, boolean isLiveViewVideo, String deviceId,
			Hashtable<String, Object> extraParams, String password) {
		log.info("startCollector() for non-rooted-android-collector");

		int delayTimeDL = 0;
		int delayTimeUL = 0;
		boolean secure = false;
		boolean installCert = false;
		if (extraParams != null) {
			delayTimeDL = (int) getOrDefault(extraParams, "delayTimeDL", 0);
			delayTimeUL = (int) getOrDefault(extraParams, "delayTimeUL", 0);
			secure = (boolean) getOrDefault(extraParams, "secure", false);
			if (secure) {
				installCert = (boolean) getOrDefault(extraParams, "installCert", false);
			}
			videoOption = (VideoOption) getOrDefault(extraParams, "video_option", VideoOption.NONE);
		}

		int bitRate = videoOption.getBitRate();
		String screenSize = videoOption.getScreenSize();

		log.info("get the delayTime: " + delayTimeDL);

		StatusResult result = new StatusResult();

		// find the device by the id
		result = findDevice(deviceId, result);
		if (!result.isSuccess()) {
			return result;
		}

		this.running = isCollectorRunning();
		// avoid running it twice
		if (this.running) {
			log.error("unknown collection still running on device");
			result.setError(ErrorCodeRegistry.getCollectorAlreadyRunning());
			result.setSuccess(false);
			return result;
		}

		if (filemanager.directoryExistAndNotEmpty(folderToSaveTrace)) {
			result.setError(ErrorCodeRegistry.getTraceDirExist());
			return result;
		}
		// there might be permission issue to creating dir to save trace
		filemanager.mkDir(folderToSaveTrace);
		if (!filemanager.directoryExist(folderToSaveTrace)) {
			result.setError(ErrorCodeRegistry.getFailedToCreateLocalTraceDirectory());
			return result;
		}

		aroDevice = new AROAndroidDevice(device, false);

		if (device.isEmulator()) {
			if (!aroDevice.getAbi().equals("x86_64")) {
				String message = "Emulator ABI:" + aroDevice.getAbi()
						+ " does not support VPN collection! use an x86_64 instead.";
				log.error(message);
				result.setError(ErrorCodeRegistry.getNotSupported(message));
				return result;
			}
		}

		// Is this required?????
		log.debug("check VPN");
		if (isVpnActivated()) {
			log.error("unknown collection still running on device");
			result.setError(ErrorCodeRegistry.getCollectorAlreadyRunning());
			return result;
		}

		// String tracename =
		// folderToSaveTrace.substring(folderToSaveTrace.lastIndexOf(Util.FILE_SEPARATOR)
		// + 1);

		this.localTraceFolder = folderToSaveTrace;
		this.videoOption = videoOption;

		// remove existing trace if presence
		android.removeEmulatorData(this.device, "/sdcard/ARO");
		android.makeDirectory(this.device, "/sdcard/ARO");

		// there might be an instance of vpn_collector running
		// to be sure it is not in memory
		this.haltCollectorInDevice();

		if (pushApk(this.device)) {
			// if (!android.runVpnApkInDevice(this.device)) {
			String cmd;

			/*if (installCert) {
				cmd = "am start -n com.att.arocollector/com.att.arocollector.AROCertInstallerActivity"
						+ " --ei delayDL " + delayTimeDL + " --ei delayUL " + delayTimeUL + " --ez secure " + secure
						+ " --es video " + videoOption.toString() + " --ei bitRate " + bitRate + " --es screenSize "
						+ screenSize;
			} else {*/
			cmd = "am start -n com.att.arocollector/com.att.arocollector.AROCollectorActivity" + " --ei delayDL "
					+ delayTimeDL + " --ei delayUL " + delayTimeUL + " --ez secure " + secure + " --es video "
					+ videoOption.toString() + " --ei bitRate " + bitRate + " --es screenSize " + screenSize 
					+ " --ez certInstall " + installCert;
			//}
			log.info(cmd);
			if (!android.runApkInDevice(this.device, cmd)) {
				result.setError(ErrorCodeRegistry.getFaildedToRunVpnApk());
				result.setSuccess(false);
				return result;
			}
		} else {
			result.setError(ErrorCodeRegistry.getFailToInstallAPK());
			return result;
		}
		if (!isTrafficCaptureRunning(MILLISECONDSFORTIMEOUT)) {
			// timeout while waiting for VPN to activate within 15 seconds
			timeOutShutdown();
			result.setError(ErrorCodeRegistry.getTimeoutVpnActivation(MILLISECONDSFORTIMEOUT));
			return result;
		}
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getNonRootedCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getStartTrace()); // GA
																			// Request

		if (isAndroidVersionNougatOrHigher(this.device) == true) {
			startCpuTraceCapture();
		}
		if (isVideo()) {
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getNonRootedCollector(),
					GoogleAnalyticsUtil.getAnalyticsEvents().getVideoCheck()); // GA
																				// Request
			startVideoCapture();
		}
		result.setSuccess(true);
		this.running = true;
		return result;
	}

	private boolean isAndroidVersionNougatOrHigher(IDevice device) {
		int androidVersion = 0;
		String version = aroDevice.getApi();

		if (version != null) {
			androidVersion = Integer.parseInt(version);
		} else {
			androidVersion = AndroidApiLevel.N24.levelNumber();
		}

		return (androidVersion >= AndroidApiLevel.N24.levelNumber()) ? true : false;
	}

	void startCpuTraceCapture() {
		cpuTraceCollector = new CpuTraceCollector();
		cpuTraceCollector.setAdbFileMgrLogExtProc(this.adbservice, this.filemanager, this.log, this.extrunner);
		try {
			if (State.Initialized == cpuTraceCollector.init(android, aroDevice, this.localTraceFolder)) {
				log.debug("execute cpucapture Thread");
				threadexecutor.execute(cpuTraceCollector);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private StatusResult findDevice(String deviceId, StatusResult result) {
		IDevice[] devlist = null;
		try {
			devlist = adbservice.getConnectedDevices();
		} catch (Exception e1) {
			if (e1.getMessage().contains("AndroidDebugBridge failed to start")) {
				result.setError(ErrorCodeRegistry.getAndroidBridgeFailedToStart());
				return result;
			}
		}
		if (devlist.length < 1) {
			result.setError(ErrorCodeRegistry.getNoDeviceConnected());
			return result;
		}
		this.device = null;
		if (deviceId != null) {
			for (IDevice aDevice : devlist) {
				if (deviceId.equals(aDevice.getSerialNumber())) {
					this.device = aDevice;
					break;
				}
			}
			if (this.device == null) {
				result.setError(ErrorCodeRegistry.getDeviceIdNotFound());
				return result;
			}
		} else {
			this.device = devlist[0];
		}
		if (!device.getState().equals(DeviceState.ONLINE)) {
			log.error("Android device is not online.");
			result.setError(ErrorCodeRegistry.getDeviceNotOnline());
			return result;
		}
		result.setSuccess(true);
		return result;
	}

	private Integer getDeviceApi() {

		Integer defaultApi = AndroidApiLevel.M23.levelNumber();
		Integer api = defaultApi;
		String api_str = aroDevice.getApi();
		
		if (api_str == null) {
			return api;
		}
		
		try {
			api = Integer.valueOf(api_str);
		} catch (NumberFormatException e1) {
			log.error("unable to obtain api:" + e1.getMessage() + " defaulting to N-Preview api-23");
			// TODO failure in N-Preview
		}
		return api;
	}

	private boolean captureVideoUsingScreenRecorder() {

		// TODO VideoOption control here
		Integer api = getDeviceApi();
		
		return isVideo() && !videoOption.equals(VideoOption.LREZ)
				&& (api >= 19 && api < 21);
	}
	
	void startVideoCapture() {
		String videopath = this.localTraceFolder + Util.FILE_SEPARATOR + "video.mov";

		/* 
		 * Media Projection vs Screen Recorder will be used to capture 
		 * SD and HD videos for devices with API 21 or above, and that 
		 * piece of code is in the APK.
		 */
		if (captureVideoUsingScreenRecorder()) {
			try {
				screenRecorder.init(aroDevice, this.localTraceFolder, videoOption);
				threadexecutor.execute(screenRecorder);
				usingScreenRecorder = true;
				
			} catch (NumberFormatException e) {
				log.error(e.getMessage());
			}
		}

		try {
			videoCapture.init(this.device, videopath);
			log.debug("execute videocapture Thread");
			threadexecutor.execute(videoCapture);
		} catch (IOException e) {
			log.error(e.getMessage());
		}

	}

	private boolean isVideo() {
		return !videoOption.equals(VideoOption.NONE);
	}

	void stopCaptureVideo() {
		// create video time file
		if (videoCapture.isVideoCaptureActive()) {
			log.debug("stopCaptureVideo(");
			String videotimepath = this.localTraceFolder + Util.FILE_SEPARATOR + "video_time";
			String data = "0.00";
			Date videoTimeStart = videoCapture.getVideoStartTime();
			if (videoTimeStart != null) {
				data = Double.toString(videoTimeStart.getTime() / 1000.0);
			}

			InputStream stream = new ByteArrayInputStream(data.getBytes());
			try {
				filemanager.saveFile(stream, videotimepath);
			} catch (IOException e) {
				log.error(e.getMessage());
			}
			videoCapture.stopRecording();
			if (usingScreenRecorder && screenRecorder != null 
					&& screenRecorder.isVideoCaptureActive()) {
				screenRecorder.stopRecording();
			}
		}
	}

	private boolean waitForVpn() {
		boolean success = true;
		int counter = 0;
		do {
			counter++;
			log.debug("waitForVpn() iteration:" + counter);
			success = isVpnActivated();
			if (!success) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		} while (counter < 150 && !success);
		return success;
	}

	/**
	 * check if the internal traffic capture is running, in this case VPN
	 */
	@Override
	public boolean isTrafficCaptureRunning(int milliSeconds) {
		boolean trafficCaptureActive = false;
		int timer = 100;
		do {
			log.debug("isTrafficCaptureRunning() seconds left :" + milliSeconds);
			trafficCaptureActive = isVpnActivated();
			if (!trafficCaptureActive) {
				try {
					Thread.sleep(timer);
				} catch (InterruptedException e) {
				}
			}
		} while (trafficCaptureActive == false && milliSeconds-- > 0);
		return trafficCaptureActive;
	}

	/**
	 * check device to see if VPN was activated
	 * 
	 * @return
	 */
	public boolean isVpnActivated() {
		String cmd = "ifconfig tun0";
		String[] lines = android.getShellReturn(this.device, cmd);
		boolean success = false;
		// log.debug("responses :" + lines.length);
		for (String line : lines) {
			// log.debug("<" + line + ">");
			if (line.contains("tun0: ip 10.") || line.contains("UP POINTOPOINT RUNNING")) {
				log.info("tun is active :" + line);
				success = true;
				break;
			}
		}
		return success;
	}

	/**
	 * install ARO Data Collector on Device
	 * 
	 * @param device
	 * @return
	 */
	boolean pushApk(IDevice device) {

		//Still need to figure out the root cause of chrome hang up when VPN collector runs, hence disabling this performance fix for 6.0.
//		if (isAPKInstallationRequired() == false)			
//			return true;
		
		try {
			device.uninstallPackage("com.att.arocollector");
		} catch (InstallException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String filepath = localTraceFolder + Util.FILE_SEPARATOR + APK_FILE_NAME; // getAROCollectorLocation();
		boolean gotlocalapk = true;
		if (!filemanager.fileExist(filepath)) {
			ClassLoader loader = NorootedAndroidCollectorImpl.class.getClassLoader();
			if (!extractor.extractFiles(filepath, APK_FILE_NAME, loader)) {
				gotlocalapk = false;
			}
		}
		if (gotlocalapk) {
			try {
				device.installPackage(filepath, true);
				log.debug("installed apk in device");
				filemanager.deleteFile(filepath);
			} catch (InstallException e) {
				log.error(e.getMessage());
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	boolean isAPKInstallationRequired() {

		boolean installationRequired = false;
		String deviceAPKVersion = null;
		String newAPKVersion = StringParse.findLabeledDataFromString("VPNCollector-", ".apk", APK_FILE_NAME);
		String cmdAROAPKVersion = "dumpsys package " + ARO_PACKAGE_NAME + " | grep versionName";
		String[] result = android.getShellReturn(this.device, cmdAROAPKVersion);
		if (result != null && (result.length > 0) && result[0] != null) {
			deviceAPKVersion = StringParse.findLabeledDataFromString("versionName=", "=", result[0]);
		}
		installationRequired = !(Objects.equals(deviceAPKVersion, newAPKVersion));
		// result.length is "0", means ARO not installed in the device.
		// Uninstall only if the APK is already installed on device and the new APK to be installed.
		if (installationRequired == true && result.length > 0) {
			try {
				device.uninstallPackage(ARO_PACKAGE_NAME);
			} catch (InstallException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return installationRequired;
	}

	/**
	 * issue commands to stop the collector on vpn This cannot halt the vpn connection programmatically. VPN must be
	 * revoked through gestures. Best done by human interaction. With sufficient knowledge of screen size, VPN
	 * implementation, Android Version gestures can be programmatically performed to close the connection.
	 */
	@Override
	public StatusResult stopCollector() {
		StatusResult result = new StatusResult();
		int count = 0;
		boolean stillRunning = true;
		log.debug("send stop command to app");
		this.sendStopCommand();
		// wait at most 2 seconds
		while (count < 20 && stillRunning) {
			count++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			stillRunning = isCollectorRunning();
		}
		if (stillRunning) {
			log.debug("send stop command again");
			this.sendStopCommand();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
			// last option is to force stop it
			if (isCollectorRunning()) {
				log.debug("Force stop app");
				this.haltCollectorInDevice();
			}
		}
		if (isAndroidVersionNougatOrHigher(device) == true)
			cpuTraceCollector.stopCpuTraceCapture(this.device);
		if (isVideo()) {
			log.debug("stopping video capture");
			this.stopCaptureVideo();
		}
		log.debug("pulling trace to local dir");
		result = pullTrace(this.mDataDeviceCollectortraceFileNames);
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getNonRootedCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getEndTrace()); // GA
																			// Request
		// Uninstall the ARO package
		try {
			if(null != device)
				device.uninstallPackage(ARO_PACKAGE_NAME);
		} catch (InstallException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Remove the data from the device after the trace pull
		// android.removeEmulatorData(this.device, "/sdcard/ARO");
		
		running = false;
		return result;
	}

	@Override
	public void timeOutShutdown() {
		haltCollectorInDevice();
	}

	@Override
	public void haltCollectorInDevice() {
		android.getShellReturn(this.device, "am force-stop com.att.arocollector");
	}

	private boolean isCollectorRunning() {
		String cmd = "ps | grep com.att";
		String[] lines = android.getShellReturn(this.device, cmd);
		boolean found = false;
		for (String line : lines) {
			if (line.contains("com.att.arocollector")) {
				found = true;
				break;
			}
		}
		return found;
	}

	/**
	 * <pre>
	 * send stop capture command send
	 */
	private void sendStopCommand() {
		String cmdclosevpn = "am broadcast -a arovpndatacollector.service.close"; // stop capture
		String cmdcloseapp = "am broadcast -a arodatacollector.home.activity.close"; // hides VPN app

		android.getShellReturn(this.device, cmdclosevpn);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		android.getShellReturn(this.device, cmdcloseapp);
	}

	private StatusResult pullTrace(String[] files) {
		StatusResult result = new StatusResult();
		SyncService service = getSyncService();
		if (service == null) {
			result.setError(ErrorCodeRegistry.getFailSyncService());
			return result;
		}

		String deviceTracePath = "";
		String setCommand = "";
		boolean commandFailure = false;

		if (Util.isWindowsOS()) {
			deviceTracePath = "/sdcard/ARO/";
			setCommand = adbservice.getAdbPath() + " -s " + device.getSerialNumber() + " pull " + deviceTracePath + ". "
					+ localTraceFolder + "/ARO ";
			commandFailure = runCommand(setCommand);

			if (!commandFailure) {
				setCommand = "move " + localTraceFolder + "\\ARO\\* " + localTraceFolder;
				commandFailure = runCommand(setCommand);
			}
			if (!commandFailure) {
				setCommand = "rd " + localTraceFolder + "\\ARO";
				runCommand(setCommand);
			}
		} else {

			deviceTracePath = "/sdcard/ARO/";
			setCommand = adbservice.getAdbPath() + " -s " + device.getSerialNumber() + " pull " + deviceTracePath + ". "
					+ localTraceFolder;
			commandFailure = runCommand(setCommand);
		}
		if (commandFailure) {
			result.setError(ErrorCodeRegistry.getAdbPullFailure());
		} else {
			result.setSuccess(true);
		}
		return result;
	}

	private boolean runCommand(String command) {
		boolean commandFailed = false;
		try {

			String commandOutput = extrunner.executeCmd(command);
			if (!commandOutput.isEmpty() && commandOutput.contains("adb: error")) {
				commandFailed = true;
				log.info("ADB command execute: " + commandOutput);
			}

		} catch (Exception e1) {
			commandFailed = true;
			e1.printStackTrace();
		}
		return commandFailed;
	}

	private <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	private SyncService getSyncService() {
		SyncService service = null;
		try {
			service = this.device.getSyncService();
		} catch (TimeoutException e) {
			log.error("Timeout error when getting SyncService from device");
		} catch (AdbCommandRejectedException e) {
			log.error("AdbCommandRejectionException: " + e.getMessage());
		} catch (IOException e) {
			log.error("IOException: " + e.getMessage());
		}
		return service;
	}

	/**
	 * Android does not require a password
	 */
	@Override
	public String getPassword() {
		return null;
	}

	/**
	 * Android does not require a password
	 * 
	 * @return false, always false, password not used in Android
	 */
	@Override
	public boolean setPassword(String requestPassword) {
		return false;
	}

	@Override
	public String[] getDeviceSerialNumber(StatusResult status) {
		return new String[0];
	}

	@Override
	public IAroDevice[] getDevices(StatusResult status) {
		return new IAroDevice[0];
	}

}
