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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.util.Util;
import com.att.aro.datacollector.ioscollector.IExternalProcessReaderSubscriber;

public class ExternalDumpcapExecutor extends Thread implements IExternalProcessReaderSubscriber {
	
	private static final Logger LOG = LogManager.getLogger(ExternalDumpcapExecutor.class);
	Process proc = null;
	String pcappath;
	ExternalProcessReader processReader;
	String sudoPassword = "";
	String captureInterface;
	IExternalProcessRunner extRunner;
	volatile boolean shutdownSignal = false;
	int totalpacketCaptured = 0;
	List<Integer> pidlist;

	public ExternalDumpcapExecutor(String pcappath, String sudopass, String captureInterface, IExternalProcessRunner extRunner) throws Exception {
		this.pcappath = pcappath;
		this.sudoPassword = sudopass;
		this.extRunner = extRunner;
		this.captureInterface = captureInterface;
		pidlist = new ArrayList<Integer>();
	}

	@Override
	public void run() {
		LOG.debug("run");
		String[] cmds = new String[] { "bash", "-c", "echo " + sudoPassword + " | sudo -S " + Util.getDumpCap() + " -P -i " + captureInterface +" -s 0 -Z none -w \"" + pcappath + "\"" };

		ProcessBuilder builder = new ProcessBuilder(cmds);
		builder.redirectErrorStream(true);

		try {
			proc = builder.start();
		} catch (IOException e) {
			LOG.error("IOException:", e);
			return;
		}

		processReader = new ExternalProcessReader(proc.getInputStream());
		processReader.addSubscriber(ExternalDumpcapExecutor.this);
		processReader.start();

		// find the processID for tshark, used to kill the process when done with trace
		if (!findPcapPathProcess(40)) {
			LOG.warn("failed to locate process number for " + this.pcappath);
		}
	}

	/**
	 * find running process for this.pcappath
	 * 
	 * @param attemptCounter
	 *            - number of times to attempt finding the process
	 * @return process string from ps
	 */
	private boolean findPcapPathProcess(int attemptCounter) {
		boolean result = false;

		while ((!result) || (attemptCounter-- > 0)) {
			String str = extRunner.executeCmd("ps ax | grep \"" + this.pcappath.replaceAll("p$", "[pP]") + "\"");
			String[] strarr = str.split("\n");
			String token;
			// find child process first
			for (int i = 0; i < strarr.length; i++) {
				token = strarr[i];
				if (token.contains(this.pcappath) && !token.contains("grep ") && !token.contains("sudo ")) {
					// record the ProcessID
					this.extractPid(token);
					result = true;
					return true;
				}
			}
			try {
				sleep(50);
			} catch (InterruptedException e) {
				LOG.debug("InterruptedException:", e);
			}
		}
		return result;
	}

	void extractPid(String line) {
		line = line.trim();
		int end = line.indexOf(' ');
		int pid;
		if (end > 0) {
			String sub = (String) line.subSequence(0, end);
			pid = Integer.parseInt(sub);
			if (!pidlist.contains(pid)) {
				pidlist.add(pid);
			}
		}
	}

	/**
	 * kill tshark which will cause shutdown() to be called after tshark is
	 * destroyed
	 */
	public void stopTshark() {

		LOG.info("shutting down tshark");

		if (pidlist.size() > 0) {
			this.shutdownSignal = false;
			for (Integer pid : pidlist) {
				extRunner.executeCmd("echo " + this.sudoPassword + " | sudo -S kill -SIGINT " + pid);
			}
			if (processReader != null) {
				processReader.setStop();//signal loop to quit
			}

			int count = 0;
			while (!shutdownSignal && count < 40) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					LOG.debug("InterruptedException:", e);
					break;
				}
				count++;
			}
		}
		shutDown();
		enableTraceAccess();
	}

	private void enableTraceAccess() {
		String result = null;
		result = extRunner.executeCmd("echo " + this.sudoPassword + " | sudo -S chmod 666 \"" + this.pcappath + "\"");
		LOG.info(result);
	}

	/**
	 * stop everything and exit
	 */
	public void shutDown() {

		if (processReader != null) {
			processReader.interrupt();
			processReader = null;
			proc.destroy();
		}
		this.shutdownSignal = true;
	}

	@Override
	public synchronized void newMessage(String message) {
		if (message.contains("packets captured")) {
			int end = message.indexOf(' ');
			if (end > 0) {
				String nstr = message.substring(0, end);
				try {
					this.totalpacketCaptured = Integer.parseInt(nstr);
				} catch (Exception ex) {
				}
			}
		}
	}

	@Override
	public void willExit() {
		this.shutDown();
	}

	public int getTotalPacketCaptured() {
		return this.totalpacketCaptured;
	}
}
