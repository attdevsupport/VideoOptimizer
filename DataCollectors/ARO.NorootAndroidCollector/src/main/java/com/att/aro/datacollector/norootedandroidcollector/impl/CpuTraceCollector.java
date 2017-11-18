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

import com.android.ddmlib.IDevice;
import com.att.aro.core.ILogger;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;

/** 
 * Initiates processcpumon.sh if the Android OS version is 7.0 or above
 */
public class CpuTraceCollector implements Runnable {
	
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

	private static ILogger LOGGER;
	
	private IAroDevice aroDevice;
	private IExternalProcessRunner extrunner;
	private IAndroid android;
	private IDevice device;
	private IFileManager filemanager;
	private IAdbService adbservice;
	String localTraceFolder;
	private boolean traceActive;
	private State currentState = State.Undefined;
	String killCpuMonPayload = "killcpumon.sh";
	String payloadFileName = "processcpumon.sh";
	String remoteCpuFilesPath = "/sdcard/ARO/cpufiles/";
	String remoteExecutable = remoteCpuFilesPath + payloadFileName;
	
	public void setAdbFileMgrLogExtProc(IAdbService adbservice, IFileManager fileManager, ILogger logger, IExternalProcessRunner runner){
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
		launchAROCpuTraceScript();
	}
	
	/*
	 * launch the processcpumon.sh to capture "top" every 2 seconds
	 */
	private void launchAROCpuTraceScript(){
		setCurrentState(State.Capturing);
		
		//Command to start script here
		String cmd = this.adbservice.getAdbPath()
				+ " -s "
				+ this.aroDevice.getId()
				+ " shell"
				+ " sh "
				+ remoteExecutable
				+ " "
				+ remoteCpuFilesPath;
		
		String line = extrunner.executeCmd(cmd);
		
		LOGGER.info("start process cpu mon script response:" + line);
	}
	
	/**
	 * Triggers the script to shutdown cpu trace collection script
	 * 
	 * @param device
	 * @return
	 */
	public boolean stopCpuTraceCapture(IDevice device) {

		setCurrentState(State.Stopping);
		String command = "sh " + remoteCpuFilesPath + killCpuMonPayload;
		String[] response = android.getShellReturn(device, command);
		for (String line : response) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
		response = null;
		response = android.getShellReturn(device, "rm -rf " + remoteCpuFilesPath);
		for (String line : response) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
		setCurrentState(State.Done);
		return true;
	}
	
	/*
	 * Initialize the cpu trace collector
	 * 
	 * @param android, arodevice
	 * @return state
	 */
	public State init(IAndroid android, IAroDevice aroDevice, String localTraceFolder){
		
		this.android = android;
		this.aroDevice = aroDevice;
		this.device = (IDevice) aroDevice.getDevice();
		this.localTraceFolder = localTraceFolder;
		
		String[] res = android.getShellReturn(device, "rm " + remoteCpuFilesPath + "*");
		for (String line : res) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
		this.filemanager.mkDir(localTraceFolder);
		String[] response;
		response = android.getShellReturn(device, "mkdir " + "/sdcard/ARO");
		for (String line : response) {
			LOGGER.info("stop cpu trace response:" + line);
		}
		response = null;
		response = android.getShellReturn(device, "mkdir " + "/sdcard/ARO/cpufiles");
		for (String line : response) {
			LOGGER.info("stop cpu trace response:" + line);
		}
		traceActive = this.adbservice.installPayloadFile(aroDevice, localTraceFolder, payloadFileName, remoteExecutable);
		if (traceActive) {
			setCurrentState(State.Initialized);
		}
		
		return getCurrentState();
	}
}