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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
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
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.core.resourceextractor.IReadWriteFileExtractor;
import com.att.aro.core.util.AttnScriptUtil;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.IScreenRecorder;
import com.att.aro.core.video.IVideoCapture;
import com.att.aro.core.video.impl.VideoCaptureImpl;
import com.att.aro.core.video.pojo.Orientation;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.datacollector.norootedandroidcollector.impl.CpuTraceCollector.State;
import com.att.aro.datacollector.norootedandroidcollector.pojo.ErrorCodeRegistry;

public class NorootedAndroidCollectorImpl implements IDataCollector, IVideoImageSubscriber {

	private static final Logger LOG = LogManager.getLogger(NorootedAndroidCollectorImpl.class.getName());	
	private volatile boolean running = false;
	private IReadWriteFileExtractor extractor;
	private IThreadExecutor threadexecutor;
	private List<IVideoImageSubscriber> videoImageSubscribers = new ArrayList<IVideoImageSubscriber>();
	private IVideoCapture videoCapture;
	private VideoOption videoOption = VideoOption.NONE;
	private IDevice device;
	private IAroDevice aroDevice;
	private IDeviceChangeListener deviceChangeListener;

	private IAdbService adbService;
	private IFileManager filemanager;
	// local directory in user machine to pull trace from device to
	private String localTraceFolder;
	private static final int MILLISECONDSFORTIMEOUT = 300;
	private static String APK_FILE_NAME = "VPNCollector-2.4.%s.apk";
	private static final String ARO_PACKAGE_NAME = "com.att.arocollector";
	private static final String ARO_PACKAGE_NAME_GREP = "[c]om.att.arocollector";

	private IAndroid android;
	private boolean usingScreenRecorder = false;

	//private static final String[] WIN_RUNTIME = { "cmd.exe", "/C" };
	//private static final String[] OS_LINUX_RUNTIME = { "/bin/bash", "-l", "-c" };
	
	private static final int RETRY_TIME = 3;
	private boolean attnrScriptRun = false;
	

	// files to pull from device
	private String[] mDataDeviceCollectortraceFileNames = { "cpu", "appid", "appname", "time", "processed_events",
			"active_process", "battery_events", "bluetooth_events", "camera_events", "device_details", "device_info",
			"gps_events", "network_details", "prop", "radio_events", "screen_events", "screen_rotations",
			"user_input_log_events", "alarm_info_start", "alarm_info_end", "batteryinfo_dump", "dmesg", "video_time",
			"wifi_events", "traffic.cap", "collect_options", "location_events","attenuation_logs" };
	
	private String traceDesc = "";
	private String traceType = "";
	private String targetedApp = "";
	private String appProducer = "";

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
		this.adbService = adbservice;
	}

	@Autowired
	public void setVideoCapture(IVideoCapture videocapture) {
		this.videoCapture = videocapture;
	}

	@Autowired
	private IScreenRecorder screenRecorder;

	private CpuTraceCollector cpuTraceCollector;
		
	private UIXmlCollector uiXmlCollector;

	private UserInputTraceCollector userInputTraceCollector;
	private AttenuationScriptExcuable attnr;

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
		LOG.debug("receiveImage");
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
		return "4.0";
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
	
	/**
	 * initialize device change listener
	 */
	private void initDeviceChangeListener() {
		final NorootedAndroidCollectorImpl collectorHelper = this;
		try {
			deviceChangeListener = new AndroidDebugBridge.IDeviceChangeListener() {
				
				private static final int MAX_RETRY_TIMES = 5;
				
				@Override
				public void deviceDisconnected(IDevice device) {
					if (isRunning() && isCurrentRunningDevice(device)) {
						LOG.info("Device " + device.getSerialNumber() + " disconnected...");
						if (videoCapture instanceof VideoCaptureImpl) {
							((VideoCaptureImpl) videoCapture).setUsbConnected(false);
						}
					}
				}
				
				@Override
				public void deviceConnected(IDevice device) {
					if (isRunning()) {
						if (!isCurrentRunningDevice(device)) {
							return;
						}
						
						if (connect(device)) {
							LOG.info("Device " + device.getSerialNumber() + " reconnected...");
							if (videoCapture instanceof VideoCaptureImpl) {
								((VideoCaptureImpl) videoCapture).setUsbConnected(true);
							}
						} else {
							LOG.error("Cannot reconnect device " + device.getSerialNumber());
						}
					}
				}
				
				/**
				 * check if the connected device is the current one which is collecting data
				 * @param device
				 * @return
				 */
				private boolean isCurrentRunningDevice(IDevice device) {
					return device.getSerialNumber().equals(collectorHelper.device.getSerialNumber());
				}
				
				/**
				 * try to connect the same device with limited retry times
				 * @param device
				 * @return
				 */
				private boolean connect(IDevice device) {
					int retry = 0;
					int interval = is32BitMachine()? 15000 : 3000;
					while(!isDeviceOnline(device) && retry++ < MAX_RETRY_TIMES) {
						LOG.info("Trying to reconnect device " + device.getSerialNumber() + "...");
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {}
					}
					return retry < MAX_RETRY_TIMES;
				}
				
				private boolean is32BitMachine() {
					return "32".equals(System.getProperty("sun.arch.data.model"));
				}
				
				/**
				 * check if device is online
				 * @return
				 */
				private boolean isDeviceOnline(IDevice device) {
					InputStream in = exec();
					if (in == null) {
						return false;
					}
					try (InputStreamReader iReader = new InputStreamReader(in);
							BufferedReader bReader = new BufferedReader(iReader);) {
						String line = null;
						while((line = bReader.readLine()) != null) {
							if (line.contains(device.getSerialNumber())) {
								return line.contains("device");
							}
						}
					} catch(IOException e) {
						LOG.error("Exception occurs when checking device online status :: " + e.getMessage());
					}
					return false;
				}
				
				/**
				 * execute adb devices command to check device status
				 * @return
				 */
				private InputStream exec() {
					String[] cmd = {adbService.getAdbPath(), "devices"};
					ProcessBuilder pBuilder = new ProcessBuilder(cmd);
					try {
						Process ps = pBuilder.start();
						return ps.getInputStream();
					} catch (IOException e) {
						LOG.error("Exception occurs when execute adb devices command for checking device online status :: " + e.getMessage());
					}
					return null;
				}
				
				@Override
				public void deviceChanged(IDevice device, int changeMask) {
					// Do nothing
				}
			};
			AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener);
		} catch(Exception e) {
			LOG.error("Could not initialize device change listener to android debug bridge :: " + e.getMessage());
		}
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
		LOG.info("startCollector() for non-rooted-android-collector");
		
		if (deviceChangeListener == null) {
			initDeviceChangeListener();
		}

		AttenuatorModel atnr = new AttenuatorModel();
		int throttleDL = -1;
		int throttleUL = -1;
		boolean atnrProfile = false;
		String location = "";
		String selectedAppName = "";
		Orientation videoOrientation = Orientation.PORTRAIT;
		
		if (extraParams != null) {
			atnr = (AttenuatorModel)getOrDefault(extraParams, "AttenuatorModel", atnr);

			videoOption = (VideoOption) getOrDefault(extraParams, "video_option", VideoOption.NONE);
			videoOrientation = (Orientation) getOrDefault(extraParams, "videoOrientation", Orientation.PORTRAIT);
			selectedAppName = (String) getOrDefault(extraParams, "selectedAppName", StringUtils.EMPTY);
			traceDesc = (String) getOrDefault(extraParams, "traceDesc", StringUtils.EMPTY);
			traceType = (String) getOrDefault(extraParams, "traceType", StringUtils.EMPTY);
			targetedApp = (String) getOrDefault(extraParams, "targetedApp", StringUtils.EMPTY);
			appProducer = (String) getOrDefault(extraParams, "appProducer", StringUtils.EMPTY);
		}
		
		int bitRate = videoOption.getBitRate();
		String screenSize = videoOption.getScreenSize();

		StatusResult result = new StatusResult();

		// find the device by the id
		result = findDevice(deviceId, result);
		if (!result.isSuccess()) {
			return result;
		}

		this.running = isCollectorRunning();
		// avoid running it twice
		if (this.running) {
			LOG.error("unknown collection still running on device");
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
			result.setSuccess(false);
			return result;
		}

		aroDevice = new AROAndroidDevice(device, false);

		if (device.isEmulator()) {
			if (!aroDevice.getAbi().equals("x86_64")) {
				String message = "Emulator ABI:" + aroDevice.getAbi()
						+ " does not support VPN collection! use an x86_64 instead.";
				LOG.error(message);
				result.setError(ErrorCodeRegistry.getNotSupported(message));
				return result;
			}
		}

		// Is this required?????
		LOG.debug("check VPN");
		if (isVpnActivated()) {
			LOG.error("unknown collection still running on device");
			result.setError(ErrorCodeRegistry.getCollectorAlreadyRunning());
			result.setSuccess(false);
			return result;
		}

		this.localTraceFolder = folderToSaveTrace;

		// remove existing trace if presence
		android.removeEmulatorData(this.device, "/sdcard/ARO");
		android.makeDirectory(this.device, "/sdcard/ARO");

		// there might be an instance of vpn_collector running
		// to be sure it is not in memory
		this.haltCollectorInDevice();
		new LogcatCollector(adbService, device.getSerialNumber()).clearLogcat();
		atnrProfile = atnr.isLoadProfile();
		if(atnrProfile){
			int apiNumber = AndroidApiLevel.K19.levelNumber();// default 
			try {
				apiNumber = Integer.parseInt(aroDevice.getApi());
			} catch (Exception e) {
				LOG.error("unknown device api number");
			}
 			location = (String) atnr.getLocalPath();
			AttnScriptUtil util = new AttnScriptUtil(apiNumber);
			boolean scriptResult =  util.scriptGenerator(location);
			if(!scriptResult){
				result.setError(ErrorCodeRegistry.getScriptAdapterError(location));
				result.setSuccess(false);
				return result;
			}
			this.attnrScriptRun = true;

		}else if (atnr.isConstantThrottle()) {
			if(atnr.isThrottleDLEnabled()){
				throttleDL = atnr.getThrottleDL();
			}
			if(atnr.isThrottleULEnabled()){
				throttleUL = atnr.getThrottleUL();
			}
		}
	
		if (pushApk(this.device)) {
			String cmd;
			cmd = "am start -n com.att.arocollector/com.att.arocollector.AROCollectorActivity" 
//					+ " --ei delayDL " + delayTimeDL + " --ei delayUL " + delayTimeUL
					+ " --ei throttleDL " + throttleDL + " --ei throttleUL " + throttleUL
					+ (atnrProfile ? (" --ez profile " + atnrProfile + " --es profilename '" + location + "'") : "")
					+ " --es video "+ videoOption.toString() 
					+ " --ei bitRate " + bitRate + " --es screenSize " + screenSize 
					+ " --es videoOrientation " + videoOrientation.toString() 
					+ " --es selectedAppName " + (StringUtils.isEmpty(selectedAppName)?"EMPTY":selectedAppName) ;
			LOG.info(cmd);
			if (!android.runApkInDevice(this.device, cmd)) {
				result.setError(ErrorCodeRegistry.getFaildedToRunVpnApk());
				result.setSuccess(false);
				return result;
			}
		} else {
			result.setError(ErrorCodeRegistry.getFailToInstallAPK());
			result.setSuccess(false);
			return result;
		}
		if (!isTrafficCaptureRunning(MILLISECONDSFORTIMEOUT)) {
			// timeout while waiting for VPN to activate within 15 seconds
			timeOutShutdown();
			result.setError(ErrorCodeRegistry.getTimeoutVpnActivation());
			result.setSuccess(false);
			return result;
		}
		new Thread(() -> {
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getNonRootedCollector(),
					GoogleAnalyticsUtil.getAnalyticsEvents().getStartTrace(),
					aroDevice != null && aroDevice.getApi() != null ? aroDevice.getApi() : "Unknown");
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getNonRootedCollector(),
					GoogleAnalyticsUtil.getAnalyticsEvents().getVideoCheck(),
					videoOption != null ? videoOption.name() : "Unknown");
		}).start();
		if (isAndroidVersionNougatOrHigher(this.device) == true) {
			startCpuTraceCapture();
		}
		if (isVideo()) {
			startVideoCapture();
		}
		startUserInputCapture();
		startUiXmlCapture();
		if(atnrProfile){
			startAttnrProfile(location);// wait for vpn collection start		
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
		cpuTraceCollector.setAdbFileMgrLogExtProc(this.adbService, this.filemanager, LOG, this.extrunner);
		try {
			if (State.Initialized == cpuTraceCollector.init(android, aroDevice, this.localTraceFolder)) {
				LOG.debug("execute cpucapture Thread");
				threadexecutor.execute(cpuTraceCollector);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}
	
	void startAttnrProfile(String location){
  			attnr = new AttenuationScriptExcuable();
			attnr.setAdbFileMgrLogExtProc(this.aroDevice, this.adbService, this.filemanager, LOG, this.extrunner);
			
			location = (String)Util.getVideoOptimizerLibrary()+System.getProperty("file.separator")+"ScriptFile.sh";
			attnr.init(android, aroDevice, this.localTraceFolder,location);
 			threadexecutor.execute(attnr);
		
	}
	
	void startUiXmlCapture() {
		uiXmlCollector = new UIXmlCollector(this.adbService, this.filemanager, this.extrunner);
		try {
			if (UIXmlCollector.State.INITIALISED == uiXmlCollector.init(android, aroDevice, this.localTraceFolder)) {
				LOG.debug("execute cpucapture Thread");
				threadexecutor.execute(uiXmlCollector);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}
	
	void startUserInputCapture(){
		userInputTraceCollector = new UserInputTraceCollector();
		userInputTraceCollector.setAdbFileMgrLogExtProc(this.adbService, this.filemanager, LOG, this.extrunner);
		try {
			if (UserInputTraceCollector.State.Initialized == userInputTraceCollector.init(android, aroDevice, this.localTraceFolder)) {
				LOG.debug("execute userinputcapture Thread");
				threadexecutor.execute(userInputTraceCollector);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}

	private StatusResult findDevice(String deviceId, StatusResult result) {
		IDevice[] devlist = null;
		try {
			devlist = adbService.getConnectedDevices();
		} catch (Exception e1) {
			if (e1.getMessage().contains("AndroidDebugBridge failed to start")) {
				result.setError(ErrorCodeRegistry.getAndroidBridgeFailedToStart());
				return result;
			}
		}
		if (devlist==null || devlist.length < 1) {
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
			LOG.error("Android device is not online.");
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
			LOG.error("unable to obtain api:" + e1.getMessage() + " defaulting to N-Preview api-23");
		}
		return api;
	}

	private boolean captureVideoUsingScreenRecorder() {
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
				LOG.error(e.getMessage());
			}
		}

		try {
			videoCapture.init(this.device, videopath);
			LOG.debug("execute videocapture Thread");
			threadexecutor.execute(videoCapture);
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}

	}

	private boolean isVideo() {
		return !videoOption.equals(VideoOption.NONE);
	}

	void stopCaptureVideo() {
		// create video time file
		if (videoCapture.isVideoCaptureActive()) {
			LOG.debug("stopCaptureVideo(");
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
				LOG.error(e.getMessage());
			}
			videoCapture.stopRecording();
			if (usingScreenRecorder && screenRecorder != null 
					&& screenRecorder.isVideoCaptureActive()) {
				screenRecorder.stopRecording();
			}
		}
	}

	/**
	 * check if the internal traffic capture is running, in this case VPN
	 */
	@Override
	public boolean isTrafficCaptureRunning(int milliSeconds) {
		boolean trafficCaptureActive = false;
		int timer = 100;
		do {
			LOG.debug("isTrafficCaptureRunning() seconds left :" + milliSeconds);
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
				LOG.info("tun is active :" + line);
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
		ClassLoader loader = NorootedAndroidCollectorImpl.class.getClassLoader();
		for(int i = 0; i < 500; i++) {
			String tempFile = String.format(APK_FILE_NAME, i);
			if(loader.getResource(tempFile) != null) {
				APK_FILE_NAME = tempFile;
				break;
			}
		}
		//Still need to figure out the root cause of chrome hang up when VPN collector runs, hence disabling this performance fix for 6.0.
		if (!isAPKInstallationRequired()) {
			return true;
		}
		String filepath = localTraceFolder + Util.FILE_SEPARATOR + APK_FILE_NAME;
		boolean gotlocalapk = true;
		if (!filemanager.fileExist(filepath)) {
			if (!extractor.extractFiles(filepath, APK_FILE_NAME, loader)) {
				gotlocalapk = false;
			}
		}
		if (gotlocalapk) {
			try {
				device.installPackage(filepath, true);
				LOG.debug("installed apk in device");
				filemanager.deleteFile(filepath);
			} catch (InstallException e) {
				LOG.error(e.getMessage());
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	/**
	 * check if APK installation is required, if so, remove the previous one first
	 * @return
	 */
	boolean isAPKInstallationRequired() {

		boolean installationRequired = false;
		String deviceAPKVersion = null;
		String newAPKVersion = StringParse.findLabeledDataFromString("VPNCollector-", ".apk", APK_FILE_NAME);
		String cmdAROAPKVersion = "dumpsys package " + ARO_PACKAGE_NAME + " | grep versionName";
		String[] result = android.getShellReturn(this.device, cmdAROAPKVersion);
		if (result != null && (result.length > 0) && result[0] != null) {
			deviceAPKVersion = StringParse.findLabeledDataFromString("versionName=", "=", result[0].trim());
		}
		installationRequired = !(Objects.equals(deviceAPKVersion, newAPKVersion));
		// result.length is "0", means ARO not installed in the device.
		// Uninstall only if the APK is already installed on device and the new APK to be installed.
		if (installationRequired == true && result != null && result.length > 0) {
			try {
				device.uninstallPackage(ARO_PACKAGE_NAME);
			} catch (InstallException e1) {
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
		if (!stopAllServices()) {
			LOG.error("Cannot stop all services, please check services in device Settings->Apps->Running");
		}
		if (!forceStopProcess()) {
			LOG.error("Cannot force stop VPN Collector");
		}
		
		userInputTraceCollector.stopUserInputTraceCapture(this.device);
		if (isAndroidVersionNougatOrHigher(device) == true)
			cpuTraceCollector.stopCpuTraceCapture(this.device);
		if (isVideo()) {
			LOG.debug("stopping video capture");
			this.stopCaptureVideo();
		}
		
		if(this.attnrScriptRun){
			attnr.stopAtenuationScript(this.device);
		}
		uiXmlCollector.stopUiXmlCapture(this.device);
		LOG.debug("pulling trace to local dir");
		new LogcatCollector(adbService, device.getSerialNumber()).collectLogcat(localTraceFolder, "Logcat.log");
		result = pullTrace(this.mDataDeviceCollectortraceFileNames);
		
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getNonRootedCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getEndTrace()); // GA Request
		running = false;
		return result;
	}
	
	/**
	 * stop VPN service and all other services associated with it
	 */
	private boolean stopAllServices() {
		try {
			int count = 0;
			while(count < RETRY_TIME) {
				this.sendStopCommand();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				if (isServiceClosed()) {
					return true;
				}
				count++;
			}
			return false;
		} catch (Exception e) {
			LOG.error("Exception occured when stopping all services in VPN Collector :: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * check if all associated services are closed
	 * @return
	 */
	private boolean isServiceClosed() {
		String cmd = "dumpsys activity services " + ARO_PACKAGE_NAME;
		String[] lines = android.getShellReturn(this.device, cmd);
		for(String line : lines) {
			if (line != null && line.contains("(nothing)")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * force stop ARO process in device
	 */
	private boolean forceStopProcess() {
		try {
			if (isCollectorRunning()) {
				this.haltCollectorInDevice();
			}
			return !isCollectorRunning();
		} catch (Exception e) {
			LOG.error("Exception occuered when force stopping VPN Collector :: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void timeOutShutdown() {
		haltCollectorInDevice();
	}

	@Override
	public void haltCollectorInDevice() {
		android.getShellReturn(this.device, "am force-stop " + ARO_PACKAGE_NAME);
	}

	/**
	 * check if the ARO process is still running
	 * @return
	 */
	private boolean isCollectorRunning() {
		String cmd = "ps -ef | grep " + ARO_PACKAGE_NAME_GREP;
		String[] lines = android.getShellReturn(this.device, cmd);
		boolean found = false;
		for (String line : lines) {
			if (line.contains(ARO_PACKAGE_NAME)) {
				found = true;
				break;
			}
		}
		return found;
	}

	/**
	 * <pre>
	 * send stop capture command send, stop captureVPNService and home activity
	 */
	private void sendStopCommand() {
		sendShellCommand("am broadcast -a arovpndatacollector.service.close"   ,0);	// CaptureVpnService#serviceCloseCmdReceiver
		sendShellCommand("am broadcast -a arodatacollector.home.activity.close",0);	// AROCollectorActivity#broadcastReceiver
		sendShellCommand("am broadcast -a videocaptureservice.service.close"   ,0);	// VideoCapture#broadcastReceiver
	}

	private String sendShellCommand(String shellCommand, int sleepForMilliseconds) {
		String[] response = android.getShellReturn(this.device, shellCommand);
		if (sleepForMilliseconds > 0) {
			try {
				Thread.sleep(sleepForMilliseconds);
			} catch (InterruptedException e) {
				// Do not care if interrupted
			}
		}
		String result = Util.condenseStringArray(response);
		LOG.info(String.format("%s :%s", shellCommand, result));
		return result;
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
			setCommand = Util.wrapText(adbService.getAdbPath() + " -s " + device.getSerialNumber() + " pull " + deviceTracePath + ". "
					+ Util.wrapText(localTraceFolder + "/ARO"));
			commandFailure = runCommand(setCommand);

			if (!commandFailure) {
				setCommand = Util.wrapText("move " + Util.wrapText(localTraceFolder + "\\ARO\\*") + " " + Util.wrapText(localTraceFolder));
				commandFailure = runCommand(setCommand);
			}
			if (!commandFailure) {
				setCommand = Util.wrapText("rd " + Util.wrapText(localTraceFolder + "\\ARO"));
				runCommand(setCommand);
			}
			if(!commandFailure) {
				setCommand = Util.wrapText("rmdir /S /Q " + Util.wrapText(localTraceFolder + "\\ARO"));
			}
		} else {

			deviceTracePath = "/sdcard/ARO/";
			setCommand = adbService.getAdbPath() + " -s " + device.getSerialNumber() + " pull " + deviceTracePath + ". "
					+ Util.wrapText(localTraceFolder);
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
				LOG.info("ADB command execute: " + commandOutput);
			}

		} catch (Exception e1) {
			commandFailed = true;
			e1.printStackTrace();
		}
		return commandFailed;
	}

	private SyncService getSyncService() {
		SyncService service = null;
		try {
			service = this.device.getSyncService();
		} catch (TimeoutException e) {
			LOG.error("Timeout error when getting SyncService from device");
		} catch (AdbCommandRejectedException e) {
			LOG.error("AdbCommandRejectionException: " + e.getMessage());
		} catch (IOException e) {
			LOG.error("IOException: " + e.getMessage());
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

	@Override
	public boolean isDeviceDataPulledStatus() {
		return true;
	}

}
