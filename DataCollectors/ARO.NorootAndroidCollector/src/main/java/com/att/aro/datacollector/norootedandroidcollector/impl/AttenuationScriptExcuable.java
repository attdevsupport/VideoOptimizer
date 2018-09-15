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

public class AttenuationScriptExcuable implements Runnable {
	private static Logger LOGGER;	
	private IAroDevice aroDevice;
	private IExternalProcessRunner extrunner;
	private IAndroid android;
	private IDevice device;
	private IAdbService adbService;


	private static final String PAYLOAD_FILENAME = "broadcast.sh";
	private static final String REMOTE_FILEPATH = "/sdcard/ARO/Attnr/";
	private static final String REMOTE_EXECUTABLE = REMOTE_FILEPATH + PAYLOAD_FILENAME;

 
	public void setAdbFileMgrLogExtProc(IAroDevice aroDevice,IAdbService adbservice, IFileManager fileManager, Logger log, IExternalProcessRunner runner){
		this.adbService = adbservice;
		LOGGER = log;
		this.extrunner = runner;
		this.aroDevice = aroDevice;
	}
	
	/*
	 * Initiation method for preparing launch the attenuation script 
	 */
	public void init(IAndroid android, IAroDevice aroDevice, String localTraceFolder,String location){
		
		this.android = android;
		this.aroDevice = aroDevice;
		this.device = (IDevice) aroDevice.getDevice();
 		
		String[] res = android.getShellReturn(device, "rm " + REMOTE_FILEPATH + "*");
		for (String line : res) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
 		String[] response;
		response = android.getShellReturn(device, "mkdir " +"/sdcard/ARO");
		for (String line : response) {
			LOGGER.info("check the folder:" + line);
		}
		response = null;
		response = android.getShellReturn(device, "mkdir " + REMOTE_FILEPATH);
		for (String line : response) {
			LOGGER.info("create a folder for attenuation:" + line);
		}
		if(PAYLOAD_FILENAME.equals(location)){
			this.adbService.installPayloadFile(aroDevice, localTraceFolder, PAYLOAD_FILENAME, REMOTE_EXECUTABLE);
		}else{
			android.pushFile(device, location, REMOTE_EXECUTABLE);
		}

 	}
	
	@Override
	public void run() {
 		
		//Command to start script here
		String cmd =this.adbService.getAdbPath()
				+ " -s "
				+ this.aroDevice.getId()
				+ " shell"
				+ " sh "
				+ REMOTE_EXECUTABLE
				+ " "
				+ REMOTE_FILEPATH;				
		extrunner.executeCmd(cmd);
		LOGGER.info("start process ");
	    
	}
	
	public void stopAtenuationScript(IDevice device){
  
		String command = "sh " + REMOTE_FILEPATH + "killattnr.sh";
		String[] response = android.getShellReturn(device, command);
		for (String line : response) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
		response = null;
		response = android.getShellReturn(device, "rm -rf " + REMOTE_FILEPATH);
		for (String line : response) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}
	}
 
}
