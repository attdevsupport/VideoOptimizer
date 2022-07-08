/*
 *  Copyright 2012, 2022 AT&T
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
package com.att.aro.datacollector.ioscollector.utilities;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.datacollector.ioscollector.reader.ExternalDumpcapExecutor;
import com.att.aro.datacollector.ioscollector.reader.PcapHelper;

/**
 * For Mac OS machine. This class start/stop Remote Virtual Interface. It also
 * start dumpcap which is capturing packet and save it as .pcap file. This class
 * require Sudo password to perform dumpcap command.
 */
public class RemoteVirtualInterface {
	private static final Logger LOG = LogManager.getLogger(RemoteVirtualInterface.class);
	String serialNumber = "";
	String sudoPassword = "";//we need to ask user for password to run some command on Mac
	private IExternalProcessRunner extRunner;
	Date initDate;
	Date startCaptureDate;
	Date startDate;
	Date stopDate;
	String pcapfilepath;
	PcapHelper pcaphelp = null;
	ExternalDumpcapExecutor dumpcapExecutor;
	int totalPacketCaptured = 0;
	String errorMsg;
	volatile boolean hasSetup = false;
	private boolean launchDaemonsExecuted;
	private String rviName;
	private XCodeInfo xcode;
	private String rviPath;

	public RemoteVirtualInterface(String sudoPassword) {
		this.sudoPassword = sudoPassword;
		startDate = new Date();
		initDate = startDate;
		extRunner = new ExternalProcessRunnerImpl();
		pcaphelp = new PcapHelper(extRunner);
		xcode = new XCodeInfo();
		rviPath = xcode.getPath();
	}

	public String getErrorMessage() {
		return this.errorMsg;
	}

	/**
	 * <pre>
	 * Start Remote Virtual Interface (RVI), which is required before dumpcap
	 * can capture packet in device. 
	 * 
	 * sequence:
	 * 	1 very LaunchDaemons are launched
	 *  2 make 10 attempts at connecting
	 *  
	 *  Failed attempts cause a disconnect and a pause before attempting another connect
	 * 
	 * @throws Exception
	 */
	public boolean setup(String serialNumber, String pcapFilePath) throws Exception {
		launchDaemons();
		
		boolean success = false;
		this.pcapfilepath = pcapFilePath;
		// avoid setup RVI again for the same device => reuse previous session
		// multiple start/stop of RVI causes device to start multiple pcap services and then deny access
		if (this.serialNumber.equals(serialNumber) && this.hasSetup) {
			return true;
		}
		
		this.serialNumber = serialNumber;
		
		String connect = "-s";
		String disconnect = "-x";

		for (int attempt = 0; attempt < 10; attempt++) {
			if (rviConnection(connect, serialNumber)) {
				success = true;
				LOG.info("RVI is started ok.");
				break;
			} else {
				LOG.info("RVI failed :" + attempt + " time(s)");
				rviConnection(disconnect, serialNumber);
				Thread.sleep(500);
			}
		}		
		
		if (!success){
			this.errorMsg = "Failed to connect to device. \r\nTry disconnecting your device and reconnect it back. \r\n If the problem still exists, try again or restarting ARO or Machine.";
			LOG.error(this.errorMsg);
		}
		
		this.hasSetup = success;
		return success;
	}

	private void launchDaemons() {
		if (!launchDaemonsExecuted) {
			extRunner.executeCmd("echo " + this.sudoPassword + " | sudo -S launchctl load -w /System/Library/LaunchDaemons/com.apple.rpmuxd.plist");
			launchDaemonsExecuted = true;
		}
	}

	/**
	 * 
	 * Start or Stop rvictl connection to device
	 * 
	 * @param mode
	 *            -s start, -x close
	 * @param serialNumber
	 *            of iOS device
	 * @return true if succeeded, false if failed
	 */
	private boolean rviConnection(String mode, String serialNumber) {
		String cmdResponse;
		String cmdString = rviPath + " " + mode + " " + serialNumber;
		cmdResponse = extRunner.executeCmd(cmdString);
		LOG.debug("cmd : " + cmdString);
		LOG.debug("cmdResponse : " + cmdResponse);
		if (cmdResponse != null && cmdResponse.contains("[SUCCEEDED]") && !cmdResponse.contains("Stopping")) {
			String[] splitResponse = cmdResponse.split("interface");
			if (splitResponse.length > 1) {
				setRviName(splitResponse[1].trim());
			} else {
				setRviName(findDeviceInRvictl(serialNumber));
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Disconnect iOSdevice serialnumber from all rvi network interfaces
	 * 
	 * @param serialNumber
	 * @return 
	 */
	public void disconnectFromRvi(String serialNumber) {
		String response = null;
		do {
			rviConnection("-x", serialNumber);
			response = extRunner.executeCmd(rviPath + " -l");
		} while (!response.trim().equals("Could not get list of devices"));
	}

	/**
	 * Execute list of rvi devices and filter for serialnumber
	 * 
	 * @param serialNumber
	 * @return rvi device associated with serialNumber
	 */
	private String findDeviceInRvictl(String serialNumber) {
		return extRunner.executeCmd(rviPath + " -l | grep " + serialNumber + " |awk -F \"interface \" '{print $2}'");
	}

	/**
	 * start new dumpcap command in background thread
	 * 
	 * @throws Exception
	 */
	public void startCapture() throws Exception {
		LOG.info("********** Starting dumpcap... **********");
		
		startDate = new Date();
		initDate = startDate;
		startCaptureDate = startDate;
		
		dumpcapExecutor = new ExternalDumpcapExecutor(this.pcapfilepath, sudoPassword, "rvi0", extRunner);
		dumpcapExecutor.start();
		
		LOG.info("************  Tcpdump started in background. ****************");
	}

	/**
	 * stop background thread that run dumpcap and destroy thread
	 */
	public void stopCapture() {
		LOG.info("********** Stop dumpcap... **********");

		if (dumpcapExecutor != null) {
			try {
				dumpcapExecutor.stopTshark();
				stopDate = new Date();
				this.totalPacketCaptured = dumpcapExecutor.getTotalPacketCaptured();
				dumpcapExecutor.interrupt();
			} catch (Exception ex) {
			    LOG.warn("Exception while trying to stop dumpcap process", ex);
			}

			dumpcapExecutor = null;

			LOG.info("destroyed dumpcap executor thread");

			if (this.totalPacketCaptured > 0) {
				Date dt = pcaphelp.getFirstPacketDate(this.pcapfilepath);
				if (dt != null) {
					this.startDate = dt;
					if(startCaptureDate != null) {
						this.startCaptureDate = startDate;
					}
					LOG.info("RVI Set packet date to: " + dt.getTime());
				}
			}

		}
	}

	/**
	 * Stop RVI and stop packet capture.
	 */
	public void stop() throws IOException {
		this.stopCapture();
		disconnectFromRvi(serialNumber);
		this.hasSetup = false;
	}

	public Date getTcpdumpInitDate() {
		return this.initDate;
	}

	public Date getTcpdumpStartDate() {
		return this.startDate;
	}

	public Date getTcpdumpStopDate() {
		return this.stopDate;
	}

	public int getTotalPacketCaptured() {
		return this.totalPacketCaptured;
	}
	
	public Date getStartCaptureDate() {
		return startCaptureDate;
	}

	public String getRviName() {
		return rviName;
	}

	public void setRviName(String rviName) {
		this.rviName = rviName;
	}
	
	public String testRVIConnection(String serialNumber) {
		return findDeviceInRvictl(serialNumber);
	}
}