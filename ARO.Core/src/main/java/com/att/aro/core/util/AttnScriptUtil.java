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
package com.att.aro.core.util;

import static java.lang.Integer.parseInt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.att.aro.core.peripheral.pojo.SpeedThrottleAdapter;

/**
 * Utility class generated script for Attenuation
 */

public class AttnScriptUtil {

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final Logger LOGGER = Logger.getLogger(AttnScriptUtil.class.getSimpleName());
	private static final Charset ENCODING = StandardCharsets.US_ASCII;

	private List<SpeedThrottleAdapter> eventList;

	public boolean scriptGenerator(String directory) {
		try {
			
			List<SpeedThrottleAdapter> throttleProfile = readTextFileAlternate(directory);	 
			writeTextFile(Util.getVideoOptimizerLibrary() + FILE_SEPARATOR + "ScriptFile.sh", throttleProfile);
			return true;
		} catch (Exception e){
			LOGGER.error("Error generating script", e);
			return false;
		}
	}

	public List<SpeedThrottleAdapter> readTextFileAlternate(String aFileName) throws IOException ,NumberFormatException {
		List<SpeedThrottleAdapter> eventList = new ArrayList<SpeedThrottleAdapter>();
		Path path = Paths.get(aFileName);
		try (BufferedReader reader = Files.newBufferedReader(path, ENCODING)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				LOGGER.info(line);
				String[] valueTemp = line.split(",");
				if (valueTemp.length >= 3) {
					eventList.add(new SpeedThrottleAdapter(parseInt(valueTemp[0]), parseInt(valueTemp[1]),
							parseInt(valueTemp[2])));
				} else {
					throw new IOException("The attenuation profile doesn't match expected format!");
				}
			}
		}
		if(eventList.isEmpty()) {
			throw new IOException("The attenuation profile doesn't have any content");
		}
		return eventList;
	}

	public void writeTextFile(String aFileName, List<SpeedThrottleAdapter> throttleProfile) throws IOException {
		Path path = Paths.get(aFileName);
		try (BufferedWriter writer = Files.newBufferedWriter(path, ENCODING)) {
			writer.write(scriptHeader());
			for (SpeedThrottleAdapter throttle : throttleProfile) {
				writer.write(scriptVPN(throttle));
			}
			writer.write(scriptFooter());
		}
	}

	private String scriptVPN(SpeedThrottleAdapter throttleProfile) {
		StringBuffer bodyScript = new StringBuffer(100);
		bodyScript.append("check_aro\n");
		bodyScript.append(
				"am broadcast -a com.att.arocollector.throttle.dl --ei dlms " + throttleProfile.getThrottleDL() + "\n");
		bodyScript.append(
				"am broadcast -a com.att.arocollector.throttle.ul --ei ulms " + throttleProfile.getThrottleUL() + "\n");
		bodyScript.append("sleep " + throttleProfile.getTimeDuration() + "\n");
		return bodyScript.toString();
	}

	private String scriptHeader() {
		StringBuffer headBuffer = new StringBuffer(200);
		headBuffer.append(
				"#!/bin/sh\necho \"kill -s HUP\" $$ > $1\"killattnr.sh\"\nfunction check_aro(){\nps | grep com.att.arocollector > dev/null");
		headBuffer.append(
				"\nif [ !$? -eq 0 ]; then\necho \"Process is not running.\"\nkill $$\nfi\n}\nwhile true\ndo\nsleep 5\n");
		return headBuffer.toString();
	}

	private String scriptFooter() {
		StringBuffer footerBuffer = new StringBuffer(10);
		footerBuffer.append("done");
		return footerBuffer.toString();

	}

	public List<SpeedThrottleAdapter> getEventList() {
		return eventList;
	}

	public void setEventList(List<SpeedThrottleAdapter> eventList) {
		this.eventList = eventList;
	}
}
