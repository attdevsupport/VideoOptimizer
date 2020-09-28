/*
 *  Copyright 2018 AT&T
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


import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.android.ddmlib.IDevice;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.util.Util;

/**
 * Initiates uidump.sh & parse the output file
 */
public class UIXmlCollector implements Runnable {
	public enum State {
		INITIALISED, CAPTURING, STOPPING, DONE, ERROR, UNDEFINED
	}

	private static final Logger logger = LogManager.getLogger(UIXmlCollector.class.getName());
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

	public UIXmlCollector(IAdbService adbservice, IFileManager fileManager, IExternalProcessRunner runner) {
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

	@Override
	public void run() {
		launchAROUIXmlScript();
	}

	/*
	 * launch the uidump.sh to capture device events
	 */
	private boolean launchAROUIXmlScript() {
		setCurrentState(State.CAPTURING);
		// Command to start script here
		String path = adbservice.getAdbPath();
		String cmd = path+ " -s " + this.aroDevice.getId() + " shell" + " sh "
				+ remoteExecutable + " " + remoteFilesPath + " " + killUiXmlPayload;
		boolean iscommandSuccessful = false;
		try {
			if(Util.isWindowsOS()) {
				cmd = Util.wrapText(cmd);
			}
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
	 * 
	 * @return state
	 */
	public State init(IAndroid android, IAroDevice aroDevice, String localTraceFolder) {
		this.android = android;
		this.aroDevice = aroDevice;
		this.device = (IDevice) aroDevice.getDevice();
		this.localTraceFolder = localTraceFolder;
		android.getShellReturn(device, "rm -rf" + remoteFilesPath + "/UIComparator");
		this.filemanager.mkDir(localTraceFolder);
		android.getShellReturn(device, "mkdir " + "/sdcard/ARO");
		traceActive = this.adbservice.installPayloadFile(aroDevice, localTraceFolder, payloadFileName,
				remoteExecutable);
		if (traceActive) {
			setCurrentState(State.INITIALISED);
		}
		return getCurrentState();
	}
}