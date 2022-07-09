/*
 *  Copyright 2022 AT&T
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
package com.att.aro.datacollector.ioscollector.attenuator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Class for generating collect_options file for record attenuation and secure
 * information for iOS collection trace
 * 
 * @author ls661n
 *
 */
public class SaveCollectorOptions {
	private static final Logger LOG = LogManager.getLogger(MitmAttenuatorImpl.class.getName());
	public static final String COLLECT_OPTIONS = "collect_options";

	public void recordCollectOptions(String trafficFilePath, int delayTimeDL, int delayTimeUL, int throttleDL,
			int throttleUL, boolean atnrProfile, boolean secure, String atnrProfileName, String videoOrientation) {

		LOG.info("trace file path: " + trafficFilePath + " set secure: " + secure + " set videoOrientation: "
				+ videoOrientation + " set Down stream Delay Time: " + delayTimeDL + " set Up stream Delay Time: "
				+ delayTimeUL + " set Profile: " + atnrProfile + " set Profile name: " + atnrProfileName);

		File file = new File(trafficFilePath, COLLECT_OPTIONS);
		LOG.info("create file:" + file.getAbsolutePath());
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			file.createNewFile();
			bw.write("dsDelay=" + delayTimeDL + System.lineSeparator() + "usDelay=" + delayTimeUL
					+ System.lineSeparator() + "throttleDL=" + throttleDL + System.lineSeparator() + "throttleUL="
					+ throttleUL + System.lineSeparator() + "secure=" + secure + System.lineSeparator() + "orientation="
					+ videoOrientation + System.lineSeparator() + "attnrProfile=" + atnrProfile + System.lineSeparator()
					+ "attnrProfileName=" + atnrProfileName);
			bw.close();
		} catch (IOException e) {
			LOG.error("recordCollectOptions() Exception:", e);
		}
	}
}
