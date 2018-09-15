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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.adb.IAdbService;

/**
 * Collects logcat messages from android devices, and clear the previous
 * messages on the phone.
 * 
 * @author bharath
 *
 */
public class LogcatCollector {
	private static final Logger LOGGER = LogManager.getLogger(LogcatCollector.class);
	private final String serialNumber;
	private final IAdbService adbService;

	public LogcatCollector(IAdbService adbService, String serialNumber) {
		this.adbService = adbService;
		this.serialNumber = serialNumber;
	}

	public String getSerialNumer() {
		return serialNumber;
	}

	public void clearLogcat() {
		try {
			String adbPath = adbService.getAdbPath();
			String[] command = new String[] { adbPath, "-s", serialNumber, "logcat", "-c" };
			new ProcessBuilder(command).start();
			LOGGER.info("Logcat clear Successful");
		} catch (IOException e) {
			LOGGER.error("Logcat clear encoutered an error", e);
		}
	}

	public void collectLogcat(String folder, String fileName) {
		try {
			File outFile = new File(folder, fileName);
			String adbPath = adbService.getAdbPath();
			String[] command = new String[] { adbPath, "logcat", "-d", "2", "com.att.arocollector:I" };
			ProcessBuilder procBuilder = new ProcessBuilder(command);
			Process process = procBuilder.redirectOutput(outFile).start();
			process.waitFor(6, TimeUnit.SECONDS);
			LOGGER.info("Collection Successful for " + serialNumber);
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Logcat collection failed for " + serialNumber, e);
		}
	}
}
