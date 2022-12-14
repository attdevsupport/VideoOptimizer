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
package com.att.aro.datacollector.ioscollector.impl;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.IDeviceStatus;
import com.att.aro.core.datacollector.IVideoImageSubscriber;
import com.att.aro.core.datacollector.pojo.EnvironmentDetails;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.datacollector.ioscollector.IOSDevice;
import com.att.aro.datacollector.ioscollector.IOSDeviceStatus;
import com.att.aro.datacollector.ioscollector.ImageSubscriber;
import com.att.aro.datacollector.ioscollector.attenuator.MitmAttenuatorImpl;
import com.att.aro.datacollector.ioscollector.attenuator.SaveCollectorOptions;
import com.att.aro.datacollector.ioscollector.reader.ExternalDeviceMonitorIOS;
import com.att.aro.datacollector.ioscollector.reader.UDIDReader;
import com.att.aro.datacollector.ioscollector.utilities.DeviceVideoHandler;
import com.att.aro.datacollector.ioscollector.utilities.ErrorCodeRegistry;
import com.att.aro.datacollector.ioscollector.utilities.IOSDeviceInfo;
import com.att.aro.datacollector.ioscollector.utilities.RemoteVirtualInterface;
import com.att.aro.datacollector.ioscollector.utilities.XCodeInfo;
import com.att.aro.datacollector.ioscollector.video.VideoCaptureMacOS;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class IOSCollectorImpl implements IDataCollector, IOSDeviceStatus, ImageSubscriber {
	public static final String IOSAPP_MOUNT = "iosapp_mount";
	private IFileManager filemanager;
	private static final Logger LOG = LogManager.getLogger(IOSCollectorImpl.class);
	private static ResourceBundle defaultBundle = ResourceBundle.getBundle("messages");
	private volatile boolean running = false;
	private File timeFile;
	private FileOutputStream timeStream;

	private ExternalDeviceMonitorIOS monitor = null;
	private boolean isDeviceConnected = false;
	private XCodeInfo xcode = null;
	private RemoteVirtualInterface rvi = null;
	private SwingWorker<String, Object> packetworker;
	private VideoCaptureMacOS videoCapture;
	private boolean hasRVI = false;
	private List<IVideoImageSubscriber> videoImageSubscribers = new ArrayList<IVideoImageSubscriber>();
	private IOSDeviceInfo deviceinfo;
	private SwingWorker<String, Object> videoworker;
	private SwingWorker<String, Object> rviTestWorker;
	private boolean hasXcodeV = false;
	private String sudoPassword = "";
	private String datadir;
	private File videofile;
	private File localTraceFolder;
	private boolean isLiveViewVideo;
	private boolean isCommandLine;
	private boolean isCapturingVideo;
	private VideoOption videoOption;
	private AttenuatorModel attenuatorModel;
	private MitmAttenuatorImpl mitmAttenuator;
	private SaveCollectorOptions saveCollectorOptions;
	private boolean hdVideoPulled = true;
	private boolean validPW;
	private boolean secure = false;
	private String udId = "";
	private String dumpcapVersion;
	
	IExternalProcessRunner extRunner = new ExternalProcessRunnerImpl();

	public IOSCollectorImpl() {
		super();
		deviceinfo = new IOSDeviceInfo();
	}

	@Autowired
	public void setFileManager(IFileManager filemanager) {
		this.filemanager = filemanager;
	}

	@Override
	public void onConnected() {
		this.isDeviceConnected = true;
	}

	@Override
	public void onDisconnected() {
		this.isDeviceConnected = false;
		if (packetworker != null) {
			this.stopWorkers();
		}
		if (rvi != null) {
			try {
				rvi.stop();
			} catch (IOException e) {
				LOG.debug("IOException:", e);
			}
		}
	}

	/**
	 * Close down collection processes, Video, RemoteVirtualInterface(tcpdump). Record start times for Video and tcpdump into video_time file Report stop times to
	 * time file
	 */
	StatusResult stopWorkers() {
		StatusResult status = new StatusResult();
		if (videoCapture != null) {
			videoCapture.signalStop();
		}
		if (monitor != null) {
			monitor.stopMonitoring();
		}

		if (rvi != null) {
			try {
				rvi.stop();
			} catch (IOException e) {
				LOG.error("IOException", e);
			}
			recordPcapStartStop();
		}
		
		if (mitmAttenuator != null) {
			mitmAttenuator.stopCollect();
			recordPcapStartStop();
		}
		
		if (packetworker != null) {
			packetworker.cancel(true);
			packetworker = null;
			LOG.info("disposed packetworker");
		}
		
		if (rviTestWorker != null) {
			rviTestWorker.cancel(true);
			rviTestWorker = null;
			LOG.info("disposed rviTestWorker");
		}

		if (videoCapture != null) {
			videoCapture.stopCapture();// blocking till video capture engine fully stops
			try (BufferedWriter videoTimeStampWriter = new BufferedWriter(new FileWriter(new File(localTraceFolder, TraceDataConst.FileName.VIDEO_TIME_FILE)));) {
				LOG.info("Writing video time to file");
				String timestr = Double.toString(videoCapture.getVideoStartTime().getTime() / 1000.0);
				if (rvi != null) {
					timestr += " " + Double.toString(rvi.getTcpdumpInitDate().getTime() / 1000.0);
				} else if (mitmAttenuator != null) {
					timestr += " " + Double.toString(mitmAttenuator.getStartDate().getTime() / 1000.0);
				}
				videoTimeStampWriter.write(timestr);
			} catch (IOException e) {
				LOG.info("Error writing video time to file: ", e);
			}
			if (videoCapture.isAlive()) {
				videoCapture.interrupt();
			}
			videoCapture = null;
			LOG.info("disposed videoCapture");
		}

		if (videoworker != null) {
			videoworker.cancel(true);
			videoworker = null;
			LOG.info("disposed videoworker");
		}
		running = false;
		status.setSuccess(true);
		return status;
	}

	private boolean isVideo() {
		return !videoOption.equals(VideoOption.NONE);
	}

	@Override
	public String getName() {
		return "IOS Data Collector";
	}

	@Override
	public void addDeviceStatusSubscriber(IDeviceStatus arg0) {

	}

	@Override
	public void addVideoImageSubscriber(IVideoImageSubscriber subscriber) {
		LOG.debug("subscribe :" + subscriber.getClass().getName());
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
		return DataCollectorType.IOS;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	public void stopRunning() {
		this.running = false;
	}

	@Override
	public StatusResult startCollector(boolean isCommandLine, String tracepath, VideoOption videoOption, String passwd) {
		return this.startCollector(isCommandLine, tracepath, videoOption, false, null, null, passwd);
	}

	@Override
	public StatusResult startCollector(boolean commandLine, String folderToSaveTrace, VideoOption videoOption, boolean liveViewVideo, String udId,
			Hashtable<String, Object> extraParams, String password) {
		if (extraParams != null) {
			this.videoOption = (VideoOption) extraParams.get("video_option");
			this.attenuatorModel = (AttenuatorModel) extraParams.get("AttenuatorModel");
			this.secure = extraParams.get("secure") == null ? false : (boolean) extraParams.get("secure");
		}

		hdVideoPulled = true;
	
		if (password != null && !validPW) {
			setPassword(password);
		}

		isCapturingVideo = isVideo();
		this.udId = udId;

		this.isCommandLine = commandLine;
		if (isCommandLine) {
			isLiveViewVideo = false;
		}

		this.isLiveViewVideo = liveViewVideo;

		StatusResult status = new StatusResult();
		status.setSuccess(true);
		// avoid running it twice
		if (this.running) {
			return status;
		}
		if (filemanager.directoryExistAndNotEmpty(folderToSaveTrace)) {
			status.setError(ErrorCodeRegistry.getTraceDirExist());
			return status;
		}

		// there might be permission issue to creating dir to save trace
		filemanager.mkDir(folderToSaveTrace);
		if (!filemanager.directoryExist(folderToSaveTrace)) {
			status.setError(ErrorCodeRegistry.getFailedToCreateLocalTraceDirectory());
			return status;
		}

		// initialize monitor, xcode and rvi
		status = init(status);
		if (!status.isSuccess()) {// an error has occurred in initialization
		    LOG.error("Something went wrong while initializing monitor, xcode or rvi");
			return status;
		}

		if (udId == null || udId.length() < 2) {
			// Failed to get Serial Number of Device, connect an IOS device to start.
			LOG.error(defaultBundle.getString("Error.serialnumberconnection"));
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getIncorrectSerialNumber());
		}

		if (!status.isSuccess()) {
			return status; // failed to get device s/n
		}

		datadir = folderToSaveTrace;
		localTraceFolder = new File(folderToSaveTrace);
		if (!localTraceFolder.exists()) {
			if (!localTraceFolder.mkdirs()) {
				datadir = "";
				// There was an error creating directory:
				LOG.error(defaultBundle.getString("Error.foldernamerequired"));
				status.setSuccess(false);
				status.setError(ErrorCodeRegistry.getMissingFolderName());
				return status;
			}
		}

		final String trafficFilePath = datadir + Util.FILE_SEPARATOR + defaultBundle.getString("datadump.trafficFile"); // "traffic.pcap";

		// device info
		String deviceDetails = datadir + Util.FILE_SEPARATOR + "device_details";
		status = collectDeviceDetails(status, udId, deviceDetails);
		if (!status.isSuccess()) {
		    LOG.error("Something went wrong while fetching the device information");
			return status; // device info error
		}

		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(GoogleAnalyticsUtil.getAnalyticsEvents().getIosCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getStartTrace(), deviceinfo != null && deviceinfo.getDeviceVersion() != null ? deviceinfo.getDeviceVersion() : "Unknown"); // GA
																																													// Request

		if ("".equals(this.sudoPassword) || !validPW) {
		    LOG.info(defaultBundle.getString("Error.sudopasswordissue"));

			if (isCommandLine) {
				status.setError(ErrorCodeRegistry.getSudoPasswordIssue());
				return status;
			} else {
				status.setData("requestPassword");
				return status; // bad or missing sudo password
			}
		}
		
		if (saveCollectorOptions == null) {
	        saveCollectorOptions = new SaveCollectorOptions();
	    }
		
		status = checkDumpcap(status);
		if (!status.isSuccess()) {
		    LOG.error("Something went wrong while setting up dumpcap");
			return status;
		}
		
		if (isCapturingVideo) {
			status = startVideoCapture(status);
			if (!status.isSuccess()) {
			    LOG.error("Something went wrong while starting the video capture");
				return status;
			}
		}

		if (isCapturingVideo && isLiveViewVideo) {

			if (isDeviceConnected) {
				LOG.info("device is connected");
			} else {
				LOG.info("Device not connected");
			}
		}
		
		try {
			EnvironmentDetails environmentDetails = new EnvironmentDetails(folderToSaveTrace);
			environmentDetails.populateDeviceInfo(deviceinfo.getDeviceVersion(), null, IAroDevice.Platform.iOS.name());
			environmentDetails.populateMacOSDetails(xcode.getXcodeVersion(), dumpcapVersion, getLibimobileDeviceVersion());

			FileWriter writer = new FileWriter(folderToSaveTrace + "/environment_details.json");
			writer.append(new ObjectMapper().writeValueAsString(environmentDetails));
			writer.close();
		} catch (IOException e) {
			LOG.error("Error while writing environment details", e);
		}

	    if ((attenuatorModel.isConstantThrottle() && (attenuatorModel.isThrottleDLEnabled() || attenuatorModel.isThrottleULEnabled())) || secure) {
	    	// Attenuator or Secure Collection performed here
	    	rvi = null;
	        startAttenuatorCollection(datadir, attenuatorModel, secure, saveCollectorOptions, status, trafficFilePath);
	    } else {
	    	mitmAttenuator = null;
	    	launchCollection(trafficFilePath, udId, status);
	        saveCollectorOptions.recordCollectOptions(datadir, 0, 0, -1, -1, false, false, "", "PORTRAIT");
	    }
		
		return status;
	}

	/**
	 * @param isCapturingVideo
	 * @param isLiveViewVideo
	 * @param status
	 * @param trafficFilePath
	 * @param serialNumber
	 * @return
	 */
	private StatusResult launchCollection(final String trafficFilePath, final String serialNumber, StatusResult status) {
		// check RVI status, and reinitialize it if not initialized already
		status = initRVI(status, trafficFilePath, serialNumber);
		if (!status.isSuccess()) {
		    LOG.error("Failed to setup RVI");
			return status;
		}
			
		// packet capture start
		startPacketCollection();
		
		return status;
	}

	private StatusResult startAttenuatorCollection(String traceFolder, AttenuatorModel attenuatorModel, boolean secure, SaveCollectorOptions saveCollectorOptions, StatusResult status, String trafficFilePath) {

		int throttleDL = 0;
		int throttleUL = 0;

		if (attenuatorModel.isThrottleDLEnabled()) {
			throttleDL = attenuatorModel.getThrottleDL();
		}
		if (attenuatorModel.isThrottleULEnabled()) {
			throttleUL = attenuatorModel.getThrottleUL();
		}
		// mitm start
		if (mitmAttenuator == null) {
			mitmAttenuator = new MitmAttenuatorImpl();
		}
		LOG.info("ios attenuation setting: " + " traceFolder: " + traceFolder + " throttleDL: " + throttleDL
				+ "throttleUL: " + throttleUL + "secure :" + secure);
		mitmAttenuator.startCollect(traceFolder, throttleDL, throttleUL, secure, saveCollectorOptions, status, this.sudoPassword, trafficFilePath);
		
		return status;
	}

	private StatusResult startVideoCapture(StatusResult status) {
		LOG.info("startVideoCapture");

		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(GoogleAnalyticsUtil.getAnalyticsEvents().getIosCollector(), GoogleAnalyticsUtil.getAnalyticsEvents().getVideoCheck());

		if (videoCapture == null) {
			final String videofilepath = datadir + Util.FILE_SEPARATOR + TraceDataConst.FileName.VIDEO_MOV_FILE;
			videofile = new File(videofilepath);
			try {
				videoCapture = new VideoCaptureMacOS(videofile, udId);
			} catch (IOException e) {
				LOG.error(rvi.getErrorMessage());
				status.setSuccess(false);
				status.setError(ErrorCodeRegistry.getrviError());
				return status;
			}
			videoCapture.setWorkingFolder(datadir);
			videoCapture.addSubscriber(this);
		}

		videoworker = new SwingWorker<String, Object>() {
			@Override
			protected String doInBackground() throws Exception {
				if (videoCapture != null) {
					videoCapture.start();
				}
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
					if (videoCapture.getStatusResult() != null) {
						StatusResult temp = videoCapture.getStatusResult();
						status.setError(temp.getError());
						status.setSuccess(temp.isSuccess());
					}
				} catch (Exception ex) {
					LOG.error("Error thrown by videoworker: ", ex);
				}
			}
		};

		videoworker.execute();
		return status;
	}

	private void startPacketCollection() {
		packetworker = new SwingWorker<String, Object>() {

			@Override
			protected String doInBackground() throws Exception {
				rvi.startCapture();
				running = true;
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
				} catch (Exception ex) {
					LOG.error("Error thrown by packetworker: ", ex);
				}
			}
		};
		packetworker.execute();
	}

	private StatusResult initRVI(StatusResult status, final String trafficFilePath, final String serialNumber) {	
		
		LOG.debug("initRVI");
		if (rvi == null) {
			rvi = new RemoteVirtualInterface(this.sudoPassword);
			rvi.disconnectFromRvi(serialNumber);
		}

		try {
			if (!rvi.setup(serialNumber, trafficFilePath)) {
				LOG.error(rvi.getErrorMessage());
				if (isCommandLine) {
					LOG.info(rvi.getErrorMessage());
				}
				status.setSuccess(false);
				status.setError(ErrorCodeRegistry.getrviError());
			} else {
				rviTestWorker = new SwingWorker<String, Object>() {
					@Override
					protected String doInBackground() throws Exception {
						if (rvi != null) {
							for (int i = 0; i < 30; i++) {
								String response = rvi.testRVIConnection(serialNumber);
								if (StringUtils.isNotEmpty(response) && !response.contains("Could not get list of devices")) {
									break;
								} else {
									status.setSuccess(false);
									status.setError(ErrorCodeRegistry.getRVIDropIssue());
									break;
								}
							}
						}
						return null;
					}
				};

				rviTestWorker.execute();
			}
		} catch (Exception e) {
			LOG.error("Exception while trying to setup RVI", e);
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getrviError());
		}

		return status;
	}

	private StatusResult checkDumpcap(StatusResult status) {
		String cmd = Util.getDumpCap() + " -v";
		String data = null;
		data = extRunner.executeCmd(cmd);
		
		if (data != null && data.length() > 1 && !data.contains("command not found:")) {
			dumpcapVersion = StringParse.findLabeledDataFromString("Dumpcap (Wireshark)", " ", data);
			status.setSuccess(true);
		} else {
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getDumpcapError());
		}

		return status;
	}

	private String getLibimobileDeviceVersion() {
		String data = null;
		try {
			data = extRunner.executeCmd("ideviceinf -v");
			if (data != null && data.length() > 1 && !data.contains("command not found:")) {
				data = StringParse.findLabeledDataFromString("ideviceinfo", " ", data.split(Util.LINE_SEPARATOR)[0]);
			} else {
				data = "";
			}
		} catch (Exception e) {
			data = "";
			LOG.debug("Error while getting ilibmobiledevice library version", e);
		}

		return data;
	}

	/**
	 * Create device_details file and populates with collected data from targeted iOS device
	 */
	private StatusResult collectDeviceDetails(StatusResult status, String udid, String deviceDetails) {
		if (!deviceinfo.recordDeviceInfo(udid, deviceDetails)) {
			LOG.error(defaultBundle.getString("Error.deviceinfoissue"));
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getDeviceInfoIssue());
		} else {
			// get device version number
			String version = deviceinfo.getDeviceVersion();
			LOG.info("Device Version :" + version);
			if (version != null && version.length() > 0) {
				int versionNumber = 0;
				int dotIndex = 0;

				try {
					dotIndex = version.indexOf(".");
					versionNumber = Integer.parseInt(version.substring(0, dotIndex));
					LOG.info("Parsed Version Number : " + versionNumber);
				} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
					LOG.error(defaultBundle.getString("Error.deviceversionissue"));
					status.setSuccess(false);
					status.setError(ErrorCodeRegistry.getDeviceVersionIssue());
				}

				if (versionNumber < 5) {
					String msg = MessageFormat.format(defaultBundle.getString("Error.iosunsupportedversion"), ApplicationConfig.getInstance().getAppShortName());
					LOG.error(msg);
					status.setSuccess(false);
					status.setError(ErrorCodeRegistry.getiOSUnsupportedVersion());
				}
			}
		}

		return status;
	}

	public String[] getDeviceSerialNumber(StatusResult status) {
		String udid = null;
		String[] deviceIds = null;

		UDIDReader reader = new UDIDReader();

		try {
			udid = reader.getSerialNumber();
		} catch (IOException e) {
			LOG.error(defaultBundle.getString("Error.incorrectserialnumber") + e.getMessage());
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getIncorrectSerialNumber());
		}
		if (udid != null && !udid.isEmpty()) {
			deviceIds = udid.split("\\s");
		}
		return deviceIds;
	}

	@Override
	public IAroDevice[] getDevices(StatusResult status) {
		IAroDevice[] aroDevices = null;
		String[] iosDevices = getDeviceSerialNumber(status);
		if (iosDevices != null && iosDevices.length > 0) {
			aroDevices = new IAroDevice[iosDevices.length];
			int pos = 0;

			for (String udid : iosDevices) {
				if (!udid.isEmpty()) {
					try {
						aroDevices[pos++] = new IOSDevice(udid);
					} catch (IOException e) {
						String errorMessage = "Failed to retrieve iOS device data:" + e.getMessage();
						LOG.error(errorMessage);
						status.setError(ErrorCodeRegistry.getFailedRetrieveDeviceData(errorMessage));
					}
				}
			}
		}
		return aroDevices;
	}

	private StatusResult init(StatusResult status) {
		if (monitor == null) {
			monitor = new ExternalDeviceMonitorIOS();
			monitor.subscribe(this);
			monitor.start();
			LOG.info(defaultBundle.getString("Status.start"));
		}
		if (xcode == null) {
			xcode = new XCodeInfo();
		}
		if (!hasRVI) {
			hasRVI = xcode.isRVIAvailable();
		}
		if (!hasRVI) {
			// please install the latest version of XCode to continue.
			LOG.error(defaultBundle.getString("Error.xcoderequired"));
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getFailedToLoadXCode());
			return status;
		} else {
			if (!hasXcodeV) {
				if (xcode.isXcodeAvailable()) {
					hasXcodeV = xcode.isXcodeSupportedVersionInstalled();
					boolean xcodeCLTError = xcode.isXcodeCLTError();
					if (!hasXcodeV && xcodeCLTError) {
						status.setSuccess(false);
						status.setError(ErrorCodeRegistry.getXCodeCLTError());
						return status;
					} else if (!hasXcodeV) {
						status.setSuccess(false);
						status.setError(ErrorCodeRegistry.getUnsupportedXCodeVersion());
						return status;
					}
				} else {
					// please install the latest version of XCode to continue.
					LOG.error(defaultBundle.getString("Error.xcoderequired"));
					status.setSuccess(false);
					status.setError(ErrorCodeRegistry.getFailedToLoadXCode());
					return status;
				}
				LOG.info("Found rvictl command");
			}
		}
		return status;
	}

	/**
	 * get the name of the directory. The last part of full directory after slash. e.g: full path /User/Documents will return Documents as the name.
	 * 
	 * @return
	 */
	public String getDirectoryName(String path) {
		String name = "";
		if (path.length() > 1) {
			path = path.replace('\\', '/');
			int index = path.lastIndexOf('/');
			if (index != -1) {
				name = path.substring(index + 1);
			} else {
				name = path;
			}
		}
		return name;
	}

	/**
	 * Check if the password provided is correct
	 * 
	 * @param password
	 *            sudoer password
	 * @return true if password is correct otherwise false
	 */
	private boolean isValidSudoPassword(String password) {
		String data = null;
		data = extRunner.executeCmd("echo " + password + " | sudo -k -S file /etc/sudoers 2>&1");
		
		if (data != null) {
			data = data.trim();
		}
		if (data != null && data.length() > 1 && !data.contains("incorrect password attempt")) {
			validPW = true;
		} else {
			// There was an error validating password.
			LOG.error(defaultBundle.getString("Error.validatepassword"));
			validPW = false;
			return validPW;
		}

		return validPW;
	}

	/**
	 * Create and populate the "time" file time file format line 1: header line 2: pcap start time line 3: eventtime or uptime (doesn't appear to be used) line 4:
	 * pcap stop time line 5: time zone offset
	 */
	private void recordPcapStartStop() {
		
		try {

			String sFileName = "time";
			timeFile = new File(datadir + Util.FILE_SEPARATOR + sFileName);
			timeStream = new FileOutputStream(timeFile);
		} catch (IOException e) {
			LOG.error("file creation error: ", e);
		}
		
		
		long startTime = mitmAttenuator != null ? mitmAttenuator.getStartDate().getTime() : rvi.getStartCaptureDate().getTime();
		long endTime = mitmAttenuator != null ? mitmAttenuator.getStopDate().getTime() : rvi.getTcpdumpStopDate().getTime();

		String str = String.format("%s\n%.3f\n%d\n%.3f\n", "Synchronized timestamps" // line 1 (header)
				, startTime / 1000.0 // line 2 (PCAP start time)
				, 0 // line 3 (userTime) should refer to device. [ not used ]
				, endTime / 1000.0 // line 4 (PCAP stop time)
		);
		
		try {
			timeStream.write(str.getBytes());
			timeStream.flush();
			timeStream.close();
		} catch (IOException e) {
			LOG.error("closeTimeFile() IOException:", e);
		}

	}

	@Override
	public StatusResult stopCollector() {
		StatusResult status = new StatusResult();
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(GoogleAnalyticsUtil.getAnalyticsEvents().getIosCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getEndTrace()); // GA Request
		this.hdVideoPulled = true;
		this.stopWorkers();
		if (datadir.length() > 1) {
			File folder = new File(datadir);
			if (folder.exists()) {
				// if video data is zero, java media player will throw exception
				if (videofile != null && videofile.exists() && videofile.length() < 2) {
					videofile.delete();
					LOG.info("deleted empty video file");
				}
				// now check for pcap existence otherwise there will be popup error.
				File pcapfile = new File(datadir + Util.FILE_SEPARATOR + defaultBundle.getString("datadump.trafficFile"));
				status.setSuccess(pcapfile.exists());
				if (this.videoOption.equals(VideoOption.HDEF)) {
					pullFromDevice(); // hd video trace only
				}
			}
		}
		return status;
	}

	
	/**
	 * Reports on whether the video.mp4 file was obtained from an iOS device, which is all that we care about
	 */
	@Override
	public boolean isDeviceDataPulled() {
		return this.hdVideoPulled;
	}

	@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Findbugs false alarm")
	private void pullFromDevice() {
		try {
			mountDevice();
			String devicePath = datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT;
			String bashCommand = String.format("find %s/DCIM -name *.MP4 -type f -newer %s/device_details", devicePath, datadir);
			String foundVideo = "";
			int tryCount = 0;
			while ((foundVideo = extRunner.executeCmd(bashCommand)).isEmpty() && ++tryCount < 60) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					LOG.debug("Thread.sleep interrupted:" + e.getMessage());
					break;
				}
			}
			if (!foundVideo.isEmpty()) {
				String[] foundVideos = foundVideo.split("[\n\r]");
				foundVideo = foundVideos[0];
				Files.move(filemanager.createFile(foundVideo).toPath(), Paths.get(datadir, "video.mp4"));
			}
			this.hdVideoPulled = true;
		} catch (IOException e) {
			LOG.error("Error Copying files from ios device", e);
			this.hdVideoPulled = false;
		} finally {
			unmountDevice();
		}
	}

	private void mountDevice() {
		File folder = new File(datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT);
		folder.mkdir();
		String cmd = String.format("%s -u %s %s", Util.getIfuse(), udId, datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT);
		DeviceVideoHandler.getInstance().executeCmd(cmd);
	}

	private void unmountDevice() {
		String mountFolder = datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT;
		String cmd = "umount " + mountFolder;
		DeviceVideoHandler.getInstance().executeCmd(cmd);
		(new File(datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT)).delete();
	}

	/**
	 * check if the internal collector method is running, in this case tcpdump
	 */
	@Override
	public boolean isTrafficCaptureRunning(int seconds) {
		LOG.info("isiOSCollectorRunning()");
		boolean tcpdumpActive = false;
		int count = 30;
		int timer = seconds / count;

		if (mitmAttenuator != null) {
			return true;
		}

		if (packetworker == null || !isRunning()) {
			return false;
		}
		do {
			LOG.debug("isTrafficCaptureRunning :" + packetworker.isCancelled() + " - " + packetworker.isDone());
			tcpdumpActive = (packetworker.isCancelled() || packetworker.isDone());
			if (!tcpdumpActive) {
				try {
					Thread.sleep(timer);
				} catch (InterruptedException e) {
				}
			}
		} while (tcpdumpActive == false && count-- > 0);
		return tcpdumpActive;
	}

	@Override
	public void haltCollectorInDevice() {
		stopCollector();
	}

	@Override
	public String[] getLog() {
		return null;
	}

	@Override
	public void timeOutShutdown() {
		stopCollector();
	}

	/**
	 * Stores the wrapped password
	 */
	@Override
	public boolean setPassword(String password) {
		sudoPassword = Util.wrapPasswordForEcho(password);
		validPW = isValidSudoPassword(sudoPassword);
		return validPW;
	}

	/**
	 * Retrieve the sudo password
	 * 
	 * @return password
	 */
	@Override
	public String getPassword() {
		return sudoPassword;
	}

}
