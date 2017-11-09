package com.att.aro.datacollector.norootedandroidcollector.impl;

import com.android.ddmlib.IDevice;
import com.att.aro.core.ILogger;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;

/** 
 * Initiates cputemperature.sh & parse the output file
 */
@Deprecated
public class CpuTemperatureCollector implements Runnable {
	
	public enum State{
		Initialized
		, Capturing
		, Stopping
		, Done
		, Error
		, Undefined
	}

	private static ILogger log;
	private IAroDevice aroDevice;
	private IExternalProcessRunner extrunner;
	private IAndroid android;
	private IDevice device;
	private IFileManager filemanager;
	private IAdbService adbservice;
	String localTraceFolder;
	private boolean traceActive;
	private State currentState = State.Undefined;
	String killTemperaturePayload = "killcputemperature.sh";
	String payloadFileName = "cputemperature.sh";
	String temperatureLogFile = "temperature_data";
	String remoteTemperatureFilesPath = "/sdcard/ARO/";
	String remoteExecutable = remoteTemperatureFilesPath + payloadFileName;
	
	public void setAdbFileMgrLogExtProc(IAdbService adbservice, IFileManager fileManager, ILogger logger, IExternalProcessRunner runner){
		this.adbservice = adbservice;
		this.filemanager = fileManager;
		this.log = logger;
		this.extrunner = runner;
	}
	
	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}
	
	public void run() {
		launchAROCpuTemperatureScript();
	}
	
	/*
	 * launch the cputemperature.sh to capture device events
	 */
	private boolean launchAROCpuTemperatureScript(){
		setCurrentState(State.Capturing);
		
		//Command to start script here
		String cmd = this.adbservice.getAdbPath()
				+ " -s "
				+ this.aroDevice.getId()
				+ " shell"
				+ " sh "
				+ remoteExecutable
				+ " "
				+ remoteTemperatureFilesPath
				+ " "
				+ temperatureLogFile
				+ " "
				+ killTemperaturePayload;
		
		boolean commandFailed = false;
		try {

			String commandOutput = extrunner.executeCmd(cmd);
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
	
	/**
	 * Triggers the script to shutdown cpu temperature collection script
	 * 
	 * @param device
	 * @return
	 */
	public boolean stopCpuTemperatureCapture(IDevice device) {

		setCurrentState(State.Stopping);
		String command = "sh " + remoteTemperatureFilesPath + killTemperaturePayload;
		String[] response = android.getShellReturn(device, command);
		for (String line : response) {
			if (line.length() > 0) {
				log.debug(">>" + line + "<<");
			}
		}
		response = null;
		response = android.getShellReturn(device, "rm " + remoteExecutable);
		for (String line : response) {
			if (line.length() > 0) {
				log.debug(">>" + line + "<<");
			}
		}
		response = null;
		setCurrentState(State.Done);
		return true;
	}
	
	/*
	 * Initialize the cpu temperature collector
	 * 
	 * @param android, arodevice
	 * @return state
	 */
	public State init(IAndroid android, IAroDevice aroDevice, String localTraceFolder){
		
		this.android = android;
		this.aroDevice = aroDevice;
		this.device = (IDevice) aroDevice.getDevice();
		this.localTraceFolder = localTraceFolder;
		
		String[] res = android.getShellReturn(device, "rm " + remoteTemperatureFilesPath + temperatureLogFile);
		for (String line : res) {
			if (line.length() > 0) {
				log.debug(">>" + line + "<<");
			}
		}
		this.filemanager.mkDir(localTraceFolder);
		String[] response;
		response = android.getShellReturn(device, "mkdir " + "/sdcard/ARO");
		for (String line : response) {
			log.info("temperature response:" + line);
		}
		traceActive = this.adbservice.installPayloadFile(aroDevice, localTraceFolder, payloadFileName, remoteExecutable);
		if (traceActive) {
			setCurrentState(State.Initialized);
		}
		
		return getCurrentState();
	}
}