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

import com.android.ddmlib.IDevice;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;

/** 
 * Initiates userinput.sh & parse the output file
 */
public class UserInputTraceCollector implements Runnable {
	
	public enum State{
		Initialized
		, Capturing
		, Stopping
		, Pulling
		, PullComplete
		, Compile
		, Done
		, Error
		, Undefined
	}

	private static Logger LOGGER;	
	private IAroDevice aroDevice;
	private IExternalProcessRunner extrunner;
	private IAndroid android;
	private IDevice device;
	private IFileManager filemanager;
	private IAdbService adbservice;
	String localTraceFolder;
	private boolean traceActive;
	private State currentState = State.Undefined;
	String killUserInputPayload = "killgeteventscript.sh";
	String payloadFileName = "userinput.sh";
	String userEventLogFile = "geteventslog";
	String remoteUserInputFilesPath = "/sdcard/ARO/";
	String remoteExecutable = remoteUserInputFilesPath + payloadFileName;
	
	public void setAdbFileMgrLogExtProc(IAdbService adbservice, IFileManager fileManager, Logger logger, IExternalProcessRunner runner){
		this.adbservice = adbservice;
		this.filemanager = fileManager;
		LOGGER = logger;
		this.extrunner = runner;
	}
	
	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}
	
	public void run() {
		launchAROUserInputTraceScript();
	}
	
	/*
	 * launch the userinput.sh to capture device events
	 */
	private void launchAROUserInputTraceScript(){
		setCurrentState(State.Capturing);
		//Command to start script here
		String path = adbservice.getAdbPath();
		String cmd = path
				+ " -s "
				+ this.aroDevice.getId()
				+ " shell"
				+ " sh "
				+ remoteExecutable
				+ " "
				+ remoteUserInputFilesPath;
		
		String line = extrunner.executeCmd(cmd);
		
		LOGGER.trace("start process userinput mon script response:" + line);
	}
	
	/**
	 * Triggers the script to shutdown userinput trace collection script
	 * 
	 * @param device
	 * @return
	 */
	public boolean stopUserInputTraceCapture(IDevice device) {
	    LOGGER.info("Stopping user input trace capture...");

		setCurrentState(State.Stopping);
		String command = "sh " + remoteUserInputFilesPath + killUserInputPayload;
		String[] response = android.getShellReturn(device, command);
		for (String line : response) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
		response = null;
		response = android.getShellReturn(device, "rm " + remoteExecutable);
		for (String line : response) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
		response = null;
		setCurrentState(State.Done);

		LOGGER.info("User input trace collection state: " + getCurrentState().name());
		return true;
	}
	
	/*
	 * Initialize the user input trace collector
	 * 
	 * @param android, arodevice
	 * @return state
	 */
	public State init(IAndroid android, IAroDevice aroDevice, String localTraceFolder){
	    LOGGER.info("Initializing User Input trace collection...");
		this.android = android;
		this.aroDevice = aroDevice;
		this.device = (IDevice) aroDevice.getDevice();
		this.localTraceFolder = localTraceFolder;

		LOGGER.debug("Remove existing user input directories:");
		String[] res = android.getShellReturn(device, "rm " + remoteUserInputFilesPath + userEventLogFile);
		for (String line : res) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}

		LOGGER.debug("Create user input directories:");
		this.filemanager.mkDir(localTraceFolder);
		String[] response;
		response = android.getShellReturn(device, "mkdir " + "/sdcard/ARO");
		for (String line : response) {
		    LOGGER.debug(">>" + line + "<<");
		}

		traceActive = this.adbservice.installPayloadFile(aroDevice, localTraceFolder, payloadFileName, remoteExecutable);
		if (traceActive) {
			setCurrentState(State.Initialized);
		}

		LOGGER.info("User input trace collection state: " + getCurrentState().name());
		return getCurrentState();
	}
}