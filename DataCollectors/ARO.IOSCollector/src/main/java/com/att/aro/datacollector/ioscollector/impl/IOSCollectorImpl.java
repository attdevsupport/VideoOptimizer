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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.IDeviceStatus;
import com.att.aro.core.datacollector.IVideoImageSubscriber;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.datacollector.ioscollector.IOSDevice;
import com.att.aro.datacollector.ioscollector.IOSDeviceStatus;
import com.att.aro.datacollector.ioscollector.ImageSubscriber;
import com.att.aro.datacollector.ioscollector.app.IOSAppException;
import com.att.aro.datacollector.ioscollector.attenuator.MitmAttenuatorImpl;
import com.att.aro.datacollector.ioscollector.reader.ExternalDeviceMonitorIOS;
import com.att.aro.datacollector.ioscollector.reader.ExternalProcessRunner;
import com.att.aro.datacollector.ioscollector.reader.UDIDReader;
import com.att.aro.datacollector.ioscollector.utilities.AppSigningHelper;
import com.att.aro.datacollector.ioscollector.utilities.ErrorCodeRegistry;
import com.att.aro.datacollector.ioscollector.utilities.IOSDeviceInfo;
import com.att.aro.datacollector.ioscollector.utilities.RemoteVirtualInterface;
import com.att.aro.datacollector.ioscollector.utilities.XCodeInfo;
import com.att.aro.datacollector.ioscollector.video.VideoCaptureMacOS;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class IOSCollectorImpl implements IDataCollector, IOSDeviceStatus, ImageSubscriber {
	private static final String IOSAPP_MOUNT = "iosapp_mount";
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
	private boolean hasxCodeV = false;
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
	private boolean deviceDataPulled = true;
	private boolean validPW;
	private String udId = "";
	
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
	 * Close down collection processes, Video, RemoteVirtualInterface(tcpdump).
	 * Record start times for Video and tcpdump into video_time file Report stop
	 * times to time file
	 */
	StatusResult stopWorkers() {
		StatusResult status = new StatusResult();
		if (videoCapture != null) {
			videoCapture.signalStop();
		}

		monitor.stopMonitoring();

		if (rvi != null) {
			try {
				rvi.stop();
			} catch (IOException e) {
				LOG.error("IOException", e);
			}
			recordPcapStartStop();
		}

		if (packetworker != null) {
			packetworker.cancel(true);
			packetworker = null;
			LOG.info("disposed packetworker");
		}

		if (videoCapture != null) {
			videoCapture.stopCapture();// blocking till video capture engine
										// fully stop //FIXME REMOVE THIS
			try(BufferedWriter videoTimeStampWriter = new BufferedWriter(
					new FileWriter(new File(localTraceFolder, TraceDataConst.FileName.VIDEO_TIME_FILE)));) {
				LOG.info("Writing video time to file");
				String timestr = Double.toString(videoCapture.getVideoStartTime().getTime() / 1000.0);
				timestr += " " + Double.toString(rvi.getTcpdumpInitDate().getTime() / 1000.0);
				videoTimeStampWriter.write(timestr);
			} catch (IOException e) {
				LOG.info("Error writing video time to file: " + e.getMessage());
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
	 * receive video frame from background capture thread, then forward it to
	 * subscribers
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
	public StatusResult startCollector(boolean isCommandLine, String tracepath, VideoOption videoOption,
			String passwd) {
		return this.startCollector(isCommandLine, tracepath, videoOption, false, null, null, passwd);
	}

	@Override
	public StatusResult startCollector(boolean commandLine, String folderToSaveTrace, VideoOption videoOption,
			boolean liveViewVideo, String udId, Hashtable<String, Object> extraParams, String password) {
		if(extraParams != null) {
			this.videoOption = (VideoOption) extraParams.get("video_option");
			this.attenuatorModel = (AttenuatorModel)extraParams.get("AttenuatorModel");			
		}
 
		Callable<StatusResult> launchAppCallable = () -> {
			return launchApp();
		};
		FutureTask<StatusResult> futureTask = new FutureTask<>(launchAppCallable);
		Thread appThread = new Thread(futureTask);
		appThread.start();
		
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
			return status;
		}

		if (udId == null || udId.length() < 2) {
			// Failed to get Serial Number of Device, connect an IOS device to
			// start.
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
		status = checkDeviceInfo(status, udId, deviceDetails);
		if (!status.isSuccess()) {
			return status; // device info error
		}

		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getIosCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getStartTrace(),
				deviceinfo != null && deviceinfo.getDeviceVersion() != null ? deviceinfo.getDeviceVersion()
						: "Unknown"); // GA Request

		if ("".equals(this.sudoPassword)|| !validPW ) {
			if (isCommandLine) {
				status.setError(ErrorCodeRegistry.getSudoPasswordIssue());
				LOG.info(defaultBundle.getString("Error.sudopasswordissue"));
				return status;
			} else {
				status.setData("requestPassword");
				return status; // bad or missing sudo password
			}
		}
		
		launchCollection(trafficFilePath, udId, status);		
		// Start Attenuation
		if((attenuatorModel.isConstantThrottle()
				&&(attenuatorModel.isThrottleDLEnabled()||attenuatorModel.isThrottleULEnabled()))) {
			startAttenuatorCollection(datadir, attenuatorModel);
		}
		
		
		if (status.isSuccess()) {
			try {
				status = futureTask.get();
			} catch (InterruptedException inEx) {
				LOG.error("Error getting app launching result", inEx);
			} catch (ExecutionException exEx) {
				LOG.error("Error getting app launching result", exEx);
			}
		}	
		
		return status;		
	}

	private StatusResult launchApp() {
		String provProfile = SettingsImpl.getInstance().getAttribute("iosProv");
		String certName = SettingsImpl.getInstance().getAttribute("iosCert");
		StatusResult status = new StatusResult();
		status.setSuccess(true);
		if (AppSigningHelper.isCertInfoPresent() && (this.videoOption.equals(VideoOption.HDEF))) {
			try {
				AppSigningHelper.getInstance().extractAndSign(provProfile, certName);
				AppSigningHelper.getInstance().deployAndLaunchApp();
			} catch (IOSAppException appEx) {
				status.setSuccess(false);
				status.setError(appEx.getErrorCode());
				LOG.error(appEx.getMessage(), appEx);
				stopProccesses();
			}
		}
		return status;
	}

	private void stopProccesses() {
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getIosCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getEndTrace());
		this.stopWorkers();
	}

	/**
	 * @param isCapturingVideo
	 * @param isLiveViewVideo
	 * @param status
	 * @param trafficFilePath
	 * @param serialNumber
	 * @return
	 */
	private StatusResult launchCollection(final String trafficFilePath, final String serialNumber,
			StatusResult status) {
		// check RVI status, and reinitialize it if not initialized already
		status = initRVI(status, trafficFilePath, serialNumber);
		if (!status.isSuccess()) {
			return status;
		}

		// packet capture start
		startPacketCollection();
		
		if (isCapturingVideo) {
			status = startVideoCapture(status);
			if (!status.isSuccess()) {
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
		return status;
	}
	
	private void startAttenuatorCollection(String trafficFilePath,AttenuatorModel attenuatorModel) {

		    int throttleDL = 0;
			int throttleUL = 0;

			if(attenuatorModel.isThrottleDLEnabled()){
				throttleDL = attenuatorModel.getThrottleDL();
			}
			if(attenuatorModel.isThrottleULEnabled()){
				throttleUL = attenuatorModel.getThrottleUL();
			}
 			// mitm start
			if(mitmAttenuator == null) {
				mitmAttenuator = new MitmAttenuatorImpl();
			}
			LOG.info("ios attenuation setting: "+" trafficFilePath: "+ trafficFilePath +" throttleDL: "+throttleDL
					+"throttleUL: "+throttleUL);
			mitmAttenuator.startCollect(trafficFilePath,throttleDL,throttleUL);	
			
		
	}

	private StatusResult startVideoCapture(StatusResult status) {

		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getIosCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getVideoCheck());

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
					LOG.info("Error thrown by videoworker: " + ex.getMessage());
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
					LOG.info("Error thrown by packetworker: " + ex.getMessage());
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
			}
		} catch (Exception e1) {
			LOG.error(e1.getMessage());
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getrviError());
		}
		return status;
	}

	private StatusResult checkDeviceInfo(StatusResult status, String udid, String deviceDetails) {
		if (!deviceinfo.getDeviceInfo(udid, deviceDetails)) {
			LOG.error(defaultBundle.getString("Error.deviceinfoissue"));
			status.setSuccess(false);
			status.setError(ErrorCodeRegistry.getDeviceInfoIssue());
		} else {
			String version = deviceinfo.getDeviceVersion(); // get device
															// version number
			LOG.info("Device Version :" + version);
			if (version != null && version.length() > 0) {
				int versionNumber = 0;
				int dotIndex = 0;
				try {
					dotIndex = version.indexOf(".");
					versionNumber = Integer.parseInt(version.substring(0, dotIndex));
					LOG.info("Parsed Version Number : " + versionNumber);
				} catch (NumberFormatException nfe) {
					LOG.error(defaultBundle.getString("Error.deviceversionissue"));
					status.setSuccess(false);
					status.setError(ErrorCodeRegistry.getDeviceVersionIssue());
				}
				if (versionNumber < 5) {
					String msg = MessageFormat.format(defaultBundle.getString("Error.iosunsupportedversion"),
							ApplicationConfig.getInstance().getAppShortName());
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
			LOG.info(defaultBundle.getString("Status.start"));// "Started device
																// monitoring");
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
			if (!hasxCodeV) {
				if (xcode.isXcodeAvailable()) {
					hasxCodeV = xcode.isXcodeSupportedVersionInstalled();
					boolean xcodeCLTError = xcode.isXcodeCLTError();
					if (!hasxCodeV && xcodeCLTError) {
						status.setSuccess(false);
						status.setError(ErrorCodeRegistry.getXCodeCLTError());
						return status;
					} else if (!hasxCodeV) {
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
	 * get the name of the directory. The last part of full directory after
	 * slash. e.g: full path /User/Documents will return Documents as the name.
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
	 * @param password sudoer password
	 * @return true if password is correct otherwise false
	 */
	private boolean isValidSudoPassword(String password) {

		ExternalProcessRunner runner = new ExternalProcessRunner();
		String cmd = "echo " + password + " | sudo -k -S file /etc/sudoers 2>&1";
		String data = null;
		try {
			data = runner.runCmd(new String[] { "bash", "-c", cmd });
		} catch (IOException e) {
			LOG.debug("IOException:", e);
			// There was an error validating password.
			LOG.error(defaultBundle.getString("Error.validatepassword"));
			validPW = false;
			return validPW;
		}
		if (data != null) {
			data = data.trim();
		}
		if (data != null && data.length() > 1 && !data.contains("incorrect password attempt")) {
			validPW = true;
		}

		return validPW;
	}

	/**
	 * Create and populate the "time" file time file format line 1: header line
	 * 2: pcap start time line 3: eventtime or uptime (doesn't appear to be
	 * used) line 4: pcap stop time line 5: time zone offset
	 */
	private void recordPcapStartStop() {
		try {

			String sFileName = "time";
			timeFile = new File(datadir + Util.FILE_SEPARATOR + sFileName);
			timeStream = new FileOutputStream(timeFile);
		} catch (IOException e) {
			LOG.error("file creation error: " + e.getMessage());
		}

		String str = String.format("%s\n%.3f\n%d\n%.3f\n", "Synchronized timestamps" // line
																						// 1
																						// (header),,
				, rvi.getStartCaptureDate().getTime() / 1000.0 // line 2 (pcap
																// start time)
				, 0 // line 3 (userTime) should refer to device. [ not used ]
				, rvi.getTcpdumpStopDate().getTime() / 1000.0 // line 4 (pcap
																// stop time)
		);

		try {
			timeStream.write(str.getBytes());
			timeStream.flush();
			timeStream.close();
		} catch (IOException e) {
			LOG.error("closeTimeFile() IOException:" + e.getMessage());
		}

	}

	@Override
	public StatusResult stopCollector() {
		StatusResult status = new StatusResult();
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getIosCollector(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getEndTrace()); // GA Request
		if((attenuatorModel.isThrottleDLEnabled()||
				attenuatorModel.isThrottleULEnabled())
				&& mitmAttenuator!=null) {
			mitmAttenuator.stopCollect();
		}
		this.deviceDataPulled = true;
		this.stopWorkers();
		if (datadir.length() > 1) {
			File folder = new File(datadir);
			if (folder.exists()) {
				// if video data is zero, java media player will throw exception
				if (videofile != null && videofile.exists() && videofile.length() < 2) {
					videofile.delete();
					LOG.info("deleted empty video file");
				}
				// now check for pcap existence otherwise there will be popup
				// error.
				File pcapfile = new File(
						datadir + Util.FILE_SEPARATOR + defaultBundle.getString("datadump.trafficFile"));
				status.setSuccess(pcapfile.exists());
				if (AppSigningHelper.isCertInfoPresent() && (this.videoOption.equals(VideoOption.HDEF))) {
					pullFromDevice(); // hd video trace only
				}
			}
		}
		return status;
	}
	
	public boolean isDeviceDataPulledStatus() {
		return this.deviceDataPulled;
	}

	@SuppressFBWarnings(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification="Findbugs false alarm")
	private void pullFromDevice() {
		AppSigningHelper.getInstance().relaunchApp();
		try {
			Thread.sleep(5*1000);
			mountDevice();
			File mountFolder = new File(datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT);
			if (mountFolder != null && mountFolder.listFiles() != null) {
				for (File file : mountFolder.listFiles()) {
					if (file == null || file.getName().contains("DS_Store")) {
						continue;
					}
					if (file.getName().toLowerCase().contains(".mp4")) {
						Files.move(file.toPath(), Paths.get(datadir, "video.mp4"));
					} else {
						Files.move(file.toPath(), Paths.get(datadir, file.getName()));
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			LOG.error("Error Copying files from ios device", e);
			this.deviceDataPulled = false;
		}
		unmountDevice();
	}
	
	private void mountDevice() {
		File folder = new File(datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT);
		folder.mkdir();
		String cmd = Util.getIfuse() + " --documents " + AppSigningHelper.getInstance().getPackageName() + " " + datadir
				+ Util.FILE_SEPARATOR + IOSAPP_MOUNT;
		AppSigningHelper.getInstance().executeCmd(cmd);
	}

	private void unmountDevice() {
		String mountFolder = datadir + Util.FILE_SEPARATOR + IOSAPP_MOUNT;
		String cmd = "umount " + mountFolder;
		AppSigningHelper.getInstance().executeCmd(cmd);
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
		if (packetworker == null || !isRunning()) {
			return false;
		}
		do {
			LOG.debug("isTrafficCaptureRunning :" + packetworker.isCancelled() + " - " + packetworker.isDone());
			tcpdumpActive = (packetworker.isCancelled() || packetworker.isDone());// checkTcpDumpRunning(device);
			if (!tcpdumpActive) {
				try {
					// log.info("waiting " + timer + ", for tcpdump to launch:"
					// + count);
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
