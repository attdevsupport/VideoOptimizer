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
package com.att.aro.mvc;

import static com.att.aro.core.settings.SettingsUtil.retrieveBestPractices;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.android.ddmlib.IDevice;
import com.att.aro.core.IAROService;
import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.IDataCollectorManager;
import com.att.aro.core.datacollector.pojo.CollectorStatus;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.AroDevices;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.mobiledevice.pojo.IAroDevices;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.ApplicationSelection;
import com.att.aro.core.packetanalysis.pojo.IPAddressSelection;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.pojo.ErrorCodeRegistry;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.VideoOption;

import lombok.Getter;

public class AROController implements PropertyChangeListener, ActionListener {

	private IAROView theView;
	private AROTraceData theModel;
	private ApplicationContext context = SpringContextUtil.getInstance().getContext();

	@Autowired
	private IAROService serv;

	private static final Logger LOG = LogManager.getLogger(AROController.class.getName());	
	private IDataCollector collector;
	private String traceFolderPath;
//	private VideoOption videoOption;
	private Date traceStartTime;
	private long traceDuration;
	private Hashtable<String, Object> extraParams;
	private PacketAnalyzerResult currentTraceInitialAnalyzerResult;
	
	@Getter
	private boolean isRooted = false;
	
	/**
	 * Constructor to instantiate an ARO API instance.
	 * 
	 * @param theView The view used by this controller
	 */
	public AROController(IAROView theView) {
		this.theView = theView;
		this.theModel = new AROTraceData();
		this.theView.addAROPropertyChangeListener(this);
		this.theView.addAROActionListener(this);
	}

	/**
	 * Returns the Model defined by this MVC pattern.
	 * 
	 * @return The model
	 */
	public AROTraceData getTheModel() {
		return theModel;
	}
	
	/**
	 * Returns the service used by the controller.
	 * 
	 * @return
	 */
	public IAROService getAROService() {
		return serv;
	}

	/**
	 * <p>Note:  Do not use this method - use <em>updateModel(...)</em> instead.</p><p>
	 * 
	 * Analyze a trace and produce a report either in json or html<br>
	 * 
	 * @param trace The FQPN of the directory or pcap file to analyze
	 * @param profile The Profile to use for this analysis - LTE if null
	 * @param filter The filters to use - can be empty for no filtering specified
	 * @see #updateModel(String, Profile, AnalysisFilter)
	 */
	public AROTraceData runAnalyzer(String trace, Profile profile, AnalysisFilter filter) {

		serv = context.getBean(IAROService.class);
		AROTraceData results = new AROTraceData();

		try {
			System.gc(); // Request garbage collection before loading a trace
			LOG.debug("Analyze trace :" + trace);
			long totalMem = Runtime.getRuntime().totalMemory();
			long freeMem = Runtime.getRuntime().freeMemory();
			LOG.debug("runAnalyzer total :"+totalMem+", free:"+freeMem);
			
			// analyze trace file or directory?
			try {
				if (serv.isFile(trace)) {
					results = serv.analyzeFile(retrieveBestPractices(), trace, profile, filter);
				} else {
					results = serv.analyzeDirectory(retrieveBestPractices(), trace, profile, filter);
				}
			} catch(OutOfMemoryError err) {
				LOG.error(err.getMessage(), err);
				results = new AROTraceData();
				results.setSuccess(false);
				results.setError(ErrorCodeRegistry.getOutOfMemoryError());
			}
		} catch (IOException exception) {
			LOG.error(exception.getMessage(), exception);
			results.setSuccess(false);
			results.setError(ErrorCodeRegistry.getUnknownFileFormat());
		}

		return results;
	}

	/**
	 * Not to be directly called.  Triggers a re-analysis if a property change is detected.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		Profile profile = null;
		AnalysisFilter filter = null;
		if (theModel.getAnalyzerResult() != null) {
			profile = theModel.getAnalyzerResult().getProfile();
			filter = theModel.getAnalyzerResult().getFilter();
		}
		try {
			theView.hideAllCharts();
			if (event.getPropertyName().equals("tracePath")) {
				updateModel((String) event.getNewValue(), profile, null);
			} else if (event.getPropertyName().equals("profile")) {
				if (theModel.isSuccess()) {
					updateModel(theModel.getAnalyzerResult().getTraceresult().getTraceDirectory(),
							(Profile) event.getNewValue(), filter);
				}
			} else if (event.getPropertyName().equals("filter")) {
				if(theModel.getAnalyzerResult().getTraceresult().getTraceFile() != null && !theModel.getAnalyzerResult().getTraceresult().getTraceFile().equals("")){
					updateModel(theModel.getAnalyzerResult().getTraceresult().getTraceFile(), profile, (AnalysisFilter) event.getNewValue());
				} else {
					updateModel(theModel.getAnalyzerResult().getTraceresult().getTraceDirectory(), profile, (AnalysisFilter) event.getNewValue());
				}
			}
		} finally {
			theView.showAllCharts();
		}
	}

	/**
	 * Not to be directly called.  Handles triggering the functionality if requested by AWT UI.
	 */
	@Override
	public void actionPerformed(ActionEvent event) {

		String actionCommand = event.getActionCommand();

		// match on Android and iOS collectors
		//		if (actionCommand.equals("startCollector") || actionCommand.equals("startCollectorIos")) {
		if ("startCollector".equals(actionCommand) || "startCollectorIos".equals(actionCommand)) {
			startCollector(event, actionCommand);
		} else if ("stopCollector".equals(actionCommand)) {
			stopCollector(CollectorStatus.STOPPED);
			this.theView.updateCollectorStatus(CollectorStatus.STOPPED, null);

			LOG.info("stopCollector() performed");
		} else if ("cancelCollector".equals(actionCommand)) {
			stopCollector(CollectorStatus.CANCELLED);
			LOG.info("stopCollector() cancel performed");
		} else if ("haltCollectorInDevice".equals(actionCommand)) {
			haltCollectorInDevice();
			this.theView.updateCollectorStatus(CollectorStatus.STOPPED, null);

			LOG.info("stopCollector() performed");
		} else if ("printJSONReport".equals(actionCommand)) {
			printReport(true, theView.getReportPath());
		} else if ("printCSVReport".equals(actionCommand)) {
			printReport(false, theView.getReportPath());
		}
	}

	/**
	 * Generates a JSON or HTML report of the last analysis to the specified file
	 * 
	 * @param json true = generate JSON report, false = generate HTML report
	 * @param reportPath The FQPN of the file where this report is generated
	 * @return true = report generation successful
	 */
	public boolean printReport(boolean json, String reportPath) {
		boolean res = false;
		if (json) {
			res = serv.getJSonReport(reportPath, theModel);
			if (res) {
				LOG.info("Successfully produce JSON report: " + reportPath);
			} else {
				LOG.info("Failed to produce JSON report: " + reportPath);
			}
		} else {
			res = serv.getHtmlReport(reportPath, theModel);
			if (res) {
				LOG.info("Successfully produce HTML report: " + reportPath);
			} else {
				LOG.info("Failed to produce HTML report: " + reportPath);
			}
		}
		return res;
	}

	/**
	 * <p>This is the main entry point for requesting an analysis of a trace.</p><p>
	 * 
	 * <em>path</em> is the file or folder containing the trace raw data if we load
	 * tracefile , the path include the file name ex, ......\traffic.cap if we
	 * load tracefolder, the path include the folder name ex, .....\tracefolder
	 * 
	 * @param path Where the trace directory or .cap file is located
	 * @param profile The Profile to use for this analysis - LTE if null
	 * @param filter The filters to use - can be empty for no filtering specified
	 */
	public void updateModel(String path, Profile profile, AnalysisFilter filter) {
		
		try{
			if (path != null) {
				AROTraceData model = runAnalyzer(path, profile, filter);
				if (!model.isSuccess()) {
					AROTraceData tempModel = theModel;
					theModel = model;
					theView.refresh();
					theModel = tempModel;
				} else {
					theModel = model;
					if (filter == null) { // when the first loading traces, set the filter
						initializeFilter();
					}
					theView.refresh();
				}
			}
		} catch(Exception ex){
			LOG.info("Error Log:" + ex.getMessage());
			LOG.error("Exception : ",ex);
		}
		(new Thread(() -> GoogleAnalyticsUtil.reportMimeDataType(theModel))).start();
	}
	
	private void initializeFilter() {
		Collection<String> appNames = theModel.getAnalyzerResult().getTraceresult().getAllAppNames();
		Map<String, Set<InetAddress>> map = theModel.getAnalyzerResult().getTraceresult().getAppIps();
		Map<InetAddress, String> domainNames = new HashMap<InetAddress, String>();
		for (Session tcpSession : theModel.getAnalyzerResult().getSessionlist()) {
			if (!domainNames.containsKey(tcpSession.getRemoteIP())) {
				domainNames.put(tcpSession.getRemoteIP(), tcpSession.getDomainName());
			}
		}
		HashMap<String, ApplicationSelection> applications = new HashMap<String, ApplicationSelection>(appNames.size());
		ApplicationSelection appSelection;
		for (String app : appNames) {
			appSelection = new ApplicationSelection(app, map.get(app));
			appSelection.setDomainNames(domainNames);
			for (IPAddressSelection ipAddressSelection : appSelection.getIPAddressSelections()) {
				ipAddressSelection.setDomainName(domainNames.get(ipAddressSelection.getIpAddress()));
			}
			applications.put(app, appSelection);
		}
		TimeRange timeRange = new TimeRange(0.0, theModel.getAnalyzerResult().getTraceresult().getTraceDuration());
		AnalysisFilter initFilter = new AnalysisFilter(applications, timeRange, domainNames);

		currentTraceInitialAnalyzerResult = theModel.getAnalyzerResult(); 
		currentTraceInitialAnalyzerResult.setFilter(initFilter);
	}

	/*
	 * Returns the initial packet analyzer result of the current trace.
	 * This method means to capture the very first analyzer result before 
	 * the model (and thus the analyzer result) starts getting updated. 
	 */
	public PacketAnalyzerResult getCurrentTraceInitialAnalyzerResult() {
		
		return currentTraceInitialAnalyzerResult;
	}
	
	/**
	 * Returns the currently available collectors (Android VPN, Android Rooted, IOS).
	 * 
	 * @return The available collectors
	 */
	public List<IDataCollector> getAvailableCollectors() {
		IDataCollectorManager colmg = context.getBean(IDataCollectorManager.class);
		return colmg.getAvailableCollectors(context);
	}

	/**
	 * Returns the available devices to start collections on (Android, IOS on Mac)
	 * 
	 * @return The available devices
	 */
	public IDevice[] getConnectedDevices() {
		IDevice[] devices = null;
		try {
			devices = context.getBean(IAdbService.class).getConnectedDevices();
		} catch (Exception exception) {
			LOG.error("failed to discover connected devices, Exception :" + exception.getMessage());
		}
		return devices;
	}

	/**
	 * Builds a list of IAroDevices
	 * 
	 * @return a list of IAroDevices
	 */
	public IAroDevices getAroDevices() {

//		IDevice[] androidDevices = null;

		IAroDevices aroDevices = new AroDevices();

		List<IDataCollector> collectors = getAvailableCollectors();
		
		if (Util.isMacOS()){
			getDevices(aroDevices, collectors, DataCollectorType.IOS);
		}
		if (getDevices(aroDevices, collectors, DataCollectorType.ROOTED_ANDROID)==0){
			getDevices(aroDevices, collectors, DataCollectorType.NON_ROOTED_ANDROID);
		}
		
		return aroDevices;
	}

	private int getDevices(IAroDevices aroDevices, List<IDataCollector> collectors, DataCollectorType collectorType) {
		int count = 0;
		for (IDataCollector iDataCollector : collectors) {
			if (iDataCollector.getType().equals(collectorType) && aroDevices != null) {
				if (Util.isMacOS() && iDataCollector.getType().equals(DataCollectorType.IOS)) {
					StatusResult status = new StatusResult();
					IAroDevice[] aroDeviceArray = iDataCollector.getDevices(status);
					aroDevices.addDeviceArray(aroDeviceArray);
					count = aroDeviceArray == null ? 0 : aroDeviceArray.length;
				} else {
					IDevice[] androidDevices = getConnectedDevices();
					if (androidDevices != null && androidDevices.length > 0) {
						aroDevices.addDeviceArray(androidDevices);
					}
					count = androidDevices == null ? 0 : androidDevices.length;
				}
			}
		}
		return count;
	}

	/**
	 * Extract parameters from an AROCollectorActionEvent event. Initiate a
	 * collection on Android and iOS devices
	 * 
	 * @param event
	 * @param actionCommand
	 *            - "startCollector" or "startCollectorIos"
	 */
	private void startCollector(ActionEvent event, String actionCommand) {
		StatusResult result;
		this.theView.updateCollectorStatus(CollectorStatus.STARTING, null);
		this.theView.setDeviceDataPulled(true); // reset so that a failure will be true
		
		if (event instanceof AROCollectorActionEvent) {
			IAroDevice device         = ((AROCollectorActionEvent) event).getDevice();
			String traceName 		  = ((AROCollectorActionEvent) event).getTrace();
			extraParams 			  = ((AROCollectorActionEvent) event).getExtraParams();
			
			result = startCollector(device, traceName, extraParams);
			LOG.info("---------- result: " + result.toString());

			if (!result.isSuccess()) { // report failure
				if (result.getError().getCode() == 206) {
					try {
						(new File(traceFolderPath)).delete();
					} catch (Exception e) {
						LOG.warn("failed to delete trace folder :" + traceFolderPath);
					}

					this.theView.updateCollectorStatus(CollectorStatus.CANCELLED, result);
				} else {
					this.theView.updateCollectorStatus(null, result);
				}
			} else { // apk has launched and been activated
				if (!getVideoOption().equals(VideoOption.NONE) && "startCollector".equals(actionCommand)) {
					this.theView.liveVideoDisplay(collector);
				}

				this.theView.updateCollectorStatus(CollectorStatus.STARTED, result);
			}

		}
	}

	/**
	 * Start the collector on device
	 * 
	 * @param deviceId
	 * @param traceFolderName
	 * @param videoOption
	 * @param secure 
	 */
	public StatusResult startCollector(IAroDevice device, String traceFolderName, Hashtable<String, Object> extraParams){

		StatusResult result = null;
		
		LOG.info("starting collector:" + traceFolderName +" " + extraParams);

		getAvailableCollectors();

		collector = device.getCollector();
		if (collector == null) {
			collector = loadCollector(device);
		}

		traceFolderPath = getTraceFolderPath(device, traceFolderName);

		IFileManager fileManager = context.getBean(IFileManager.class);

		if (!fileManager.directoryExistAndNotEmpty(traceFolderPath)) {
			result = collector.startCollector(false, traceFolderPath, null, true, device.getId(), extraParams, collector.getPassword());
			traceStartTime = new Date();

			if (result.isSuccess()) {
				LOG.info("Result : traffic capture launched successfully");
				traceDuration = 0;
			} else {
				LOG.error("Result trace success:" + result.isSuccess() + ", Name :" + result.getError().getName() + ", Description :" + result.getError().getDescription());
				LOG.error("device logcat:");
			}
		} else {
			LOG.info("Illegal path:" + traceFolderPath);
			result = new StatusResult();
			result.setError(ErrorCodeRegistry.getTraceFolderNotFound());
			return result;
		}
		return result;
	}

	/**
	 * 
	 * @param device
	 * @param traceFolderName
	 * @return complete path to trace folder
	 */
	private String getTraceFolderPath(IAroDevice device, String traceFolderName) {
		String path = null;

		if (device.isPlatform(IAroDevice.Platform.Android)) {
			path = Util.getAROTraceDirAndroid() + Util.FILE_SEPARATOR + traceFolderName;
		} else if (device.isPlatform(IAroDevice.Platform.iOS)) {
			path = Util.getAROTraceDirIOS() + Util.FILE_SEPARATOR + traceFolderName;
		}

		return path;
	}

	/**
	 * Loads the appropriate collector for an IAroDevice
	 * 
	 * @param device an IAroDevice
	 * @return collector associated with the device
	 */
	private  IDataCollector loadCollector(IAroDevice device) {
		
		IDataCollector collector = null;
		
		if (device.isPlatform(IAroDevice.Platform.Android)) {
			if (device.isRooted()) {
				LOG.debug("rooted device");
				isRooted  = true;
				collector = context.getBean(IDataCollectorManager.class).getRootedDataCollector();
			} else {
				LOG.debug("non-rooted device");
				collector = context.getBean(IDataCollectorManager.class).getNorootedDataCollector();
			}
		} else if (device.isPlatform(IAroDevice.Platform.iOS)) {
			LOG.debug("iOS device");
			collector = context.getBean(IDataCollectorManager.class).getIOSCollector();
		}
		
		return collector;
	}

	/**
	 * Stop the current collection process via a "force close" approach.  Used in case a
	 * collector had problems starting or stopping cleanly but still needs resources cleaned up.
	 */
	public void haltCollectorInDevice() {
		if (collector == null) {
			return;
		} else {
			collector.stopCollector();
			collector.haltCollectorInDevice();
		}
	}

	/**
	 * Stop the current collection process in a clean manner.
	 */
	public void stopCollector(CollectorStatus collectorstatus) {
		if (collector == null) {
			return;
		}
		LOG.debug("stopCollector() check if running");
		if (collector.isTrafficCaptureRunning(1) && !collectorstatus.equals(CollectorStatus.CANCELLED)) { //FIXME THINKS THE CAPTURE IS RUNNING AFTER STOP
			StatusResult result = collector.stopCollector();
			LOG.info("stopped collector, result:" + result);
			if (collector.getType().equals(DataCollectorType.IOS) && (!collector.isDeviceDataPulled())) {
				this.theView.setDeviceDataPulled(false);
			}
			if (result.isSuccess()) {
				Date traceStopTime = new Date();
				traceDuration = traceStopTime.getTime() - traceStartTime.getTime();
				this.theView.updateCollectorStatus(collectorstatus, result);
			} else {
				traceDuration = 0;
				this.theView.updateCollectorStatus(null, result);
			}
		} else {
			collector.haltCollectorInDevice();
		}

	}

	/**
	 * Returns the base path as persisted by ARO from which to select traces for analysis
	 * 
	 * @return Base path
	 */
	public String getTraceFolderPath() {
		return traceFolderPath;
	}

	/**
	 * Returns whether video capture is requested to be included for data collection
	 * 
	 * @return true = video is part of the collected data
	 */
	public VideoOption getVideoOption() {
		VideoOption val = (VideoOption)extraParams.get("video_option");
		if (val==null){
			val = VideoOption.NONE;
		}
		return val;
	}

	/**
	 * Returns the length of time captured in milliseconds.
	 * 
	 * @return length of the trace in milliseconds
	 */
	public long getTraceDuration() {
		return traceDuration;
	}

	/**
	 * Do not use - for internal use only.
	 * 
	 * @param traceDuration
	 */
	public void setTraceDuration(long traceDuration) {
		this.traceDuration = traceDuration;
	}
	
	
	/**
	 * Find the list of all applications in the selected android device.
	 * 
	 * @return List of all applications in selected android device.
	 */
	public String[] getApplicationsList(String id) {
		return context.getBean(IAdbService.class).getApplicationList(id);
	}

}


