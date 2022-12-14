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
package com.att.aro.datacollector.ioscollector.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.util.Util;
import com.att.aro.datacollector.ioscollector.IExternalProcessReaderSubscriber;
import com.att.aro.datacollector.ioscollector.IOSDeviceStatus;

public class ExternalDeviceMonitorIOS extends Thread implements IExternalProcessReaderSubscriber {
	private static final Logger LOG = LogManager.getLogger(ExternalDeviceMonitorIOS.class);
	Process proc = null;
	String exepath;
	ExternalProcessReader procreader;
	int pid = 0;
	IExternalProcessRunner extRunner;
	volatile boolean shutdownSignal = false;
	List<IOSDeviceStatus> subscribers;

	public ExternalDeviceMonitorIOS() {
		this.extRunner = new ExternalProcessRunnerImpl();
		init();
	}

	public ExternalDeviceMonitorIOS(IExternalProcessRunner extRunner) {
		this.extRunner = extRunner;
		init();
	}

	void init() {
		subscribers = new ArrayList<IOSDeviceStatus>();
		exepath = Util.getVideoOptimizerLibrary() + "/.drivers/libimobiledevice/idevicesyslog_aro";
		clearExe();
	}

	public void clearExe() {
		extRunner.executeCmd("fuser -f " + exepath + "|xargs kill");
	}
	
	public void subscribe(IOSDeviceStatus subscriber) {
		this.subscribers.add(subscriber);
	}

	@Override
	public void run() {
		String[] cmds = new String[] { "bash", "-c", this.exepath };

		ProcessBuilder builder = new ProcessBuilder(cmds);
		builder.redirectErrorStream(true);

		try {
			proc = builder.start();
		} catch (IOException e) {
			LOG.error("IOException :", e);
			return;
		}

		procreader = new ExternalProcessReader(proc.getInputStream());
		procreader.addSubscriber(ExternalDeviceMonitorIOS.this);
		procreader.start();

		setPid(exepath);

		try {
			proc.waitFor();

		} catch (InterruptedException e) {
			LOG.warn("Thread interrupted", e);
		}
	}

	/**
	 * locate first instance of theExec process and set the pid value in this.pid
	 * 
	 * @param theExec - idevicesyslog_aro
	 */
	private void setPid(String theExec) {
		String response = null;
		try {
			response = extRunner.executeCmdRunner(null, "fuser -f " + theExec, false, "", false, true);
			String[] pids = response.trim().split("\\s+");
			if (pids.length > 0 && !"".equals(pids[0])) {
				pid = Integer.parseInt(pids[0]);
			}

		} catch (Exception e) {
			LOG.error("IOException | NumberFormatException ", e);
		}
	}

	/**
	 * signals pid process to 
	 * that process was for idevicesyslog_aro which is no more
	 */
	public void stopMonitoring() {
		if (pid > 0) {
			this.shutdownSignal = false;
			try {
				Runtime.getRuntime().exec("kill -SIGINT " + pid);
				LOG.info("kill device monitor process pid: " + pid);
			} catch (IOException e) {
				LOG.error("IOException :", e);
			}
			int count = 0;
			while (!shutdownSignal && count < 10) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					break;
				}
				count++;
			}
		}
	}

	/**
	 * stop everything and exit
	 */
	public void shutDown() {

		if (procreader != null) {
			procreader.interrupt();
			procreader = null;
			proc.destroy();
		}
		this.shutdownSignal = true;
	}

	@Override
	public void newMessage(String message) {
		if (message.equals("[connected]")) {
			LOG.info("Device connected");
			notifyConnected();
		} else if (message.equals("[disconnected]")) {
			LOG.info("Device disconnected");
			notifyDisconnected();
		}
	}

	@Override
	public void willExit() {
		this.shutDown();
	}

	void notifyDisconnected() {
		for (IOSDeviceStatus sub : subscribers) {
			sub.onDisconnected();
		}
	}

	void notifyConnected() {
		for (IOSDeviceStatus sub : subscribers) {
			sub.onConnected();
		}
	}
}
