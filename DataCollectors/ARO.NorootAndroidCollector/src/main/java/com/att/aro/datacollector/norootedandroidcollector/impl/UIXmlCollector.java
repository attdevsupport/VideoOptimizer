package com.att.aro.datacollector.norootedandroidcollector.impl;

import com.android.ddmlib.IDevice;
import com.att.aro.core.ILogger;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.model.InjectLogger;

/** 
 * Initiates uidump.sh & parse the output file
 */

public class UIXmlCollector implements Runnable {
	
	public enum State {
		INITIALISED, CAPTURING, STOPPING, DONE, ERROR, UNDEFINED
	}

	@InjectLogger
	private static ILogger logger;
	
	private IAroDevice aroDevice;
	private IExternalProcessRunner extrunner;
	private IAndroid android;
	private IDevice device;
	private IAdbService adbservice;
	String localTraceFolder;
	private boolean traceActive;
	private State currentState = State.UNDEFINED;
	String killUiXmlPayload = "killuidump.sh";
	String payloadFileName = "uidump.sh";
	String remoteFilesPath = "/sdcard/ARO/";
	String remoteExecutable = remoteFilesPath + payloadFileName;
	private IFileManager filemanager;
	
	public UIXmlCollector(IAdbService adbservice,IFileManager fileManager, IExternalProcessRunner runner) {
		this.adbservice = adbservice;
		this.filemanager = fileManager;
		this.extrunner = runner;
	}
	
	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}
	
	public void run() {
		launchAROUIXmlScript();
	}
	
	/*
	 * launch the uidump.sh to capture device events
	 */
	private boolean launchAROUIXmlScript() {
		setCurrentState(State.CAPTURING);

		// Command to start script here
		String cmd = this.adbservice.getAdbPath() + " -s " + this.aroDevice.getId() + " shell" + " sh "
				+ remoteExecutable + " " + remoteFilesPath + " " + killUiXmlPayload;

		boolean iscommandSuccessful = false;
		try {

			String commandOutput = extrunner.executeCmd(cmd);
			if (!commandOutput.isEmpty() && commandOutput.contains("adb: error")) {
				iscommandSuccessful = true;
				logger.info("ADB command execute: " + commandOutput);
			}

		} catch (Exception e1) {
			iscommandSuccessful = true;
			e1.printStackTrace();
		}
		return iscommandSuccessful;
	}
	
	/**
	 * Triggers the script to shutdown UI automator collection script
	 * 
	 * @param device
	 * @return
	 */
	public boolean stopUiXmlCapture(IDevice device) {

		setCurrentState(State.STOPPING);
		String command = "sh " + remoteFilesPath + killUiXmlPayload;
		String[] response = android.getShellReturn(device, command);
		for (String line : response) {
			if (line.length() > 0) {
				logger.debug(">>" + line + "<<");
			}
		}
		response = null;
		response = android.getShellReturn(device, "rm " + remoteExecutable);
		for (String line : response) {
			if (line.length() > 0) {
				logger.debug(">>" + line + "<<");
			}
		}
		response = null;
		setCurrentState(State.DONE);
		return true;
	}
	
	/*
	 * Initialize the UI automator xml collector
	 * 
	 * @param android, arodevice
	 * @return state
	 */
	public State init(IAndroid android, IAroDevice aroDevice, String localTraceFolder) {
		
		this.android = android;
		this.aroDevice = aroDevice;
		this.device = (IDevice) aroDevice.getDevice();
		this.localTraceFolder = localTraceFolder;
		
		android.getShellReturn(device, "rm -rf" + remoteFilesPath+"/UIComparator");
		
		this.filemanager.mkDir(localTraceFolder);
	
		android.getShellReturn(device, "mkdir " + "/sdcard/ARO");

		traceActive = this.adbservice.installPayloadFile(aroDevice, localTraceFolder, payloadFileName, remoteExecutable);
		if (traceActive) {
			setCurrentState(State.INITIALISED);
		}
		
		return getCurrentState();
	}
}