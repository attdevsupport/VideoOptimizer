/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.peripheral.impl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.ICpuTemperatureReader;
import com.att.aro.core.peripheral.pojo.TemperatureEvent;
import com.att.aro.core.util.Util;

/**
 * read cpu temperature captured in the file
 * Date: January 3, 2017
 *
 */
public class CpuTemperatureReaderImpl extends PeripheralBase implements ICpuTemperatureReader {

	@InjectLogger
	private static ILogger logger;
	
	@Override
	public List<TemperatureEvent> readData(String directory, double startTime) {
		List<TemperatureEvent> temperatureEvents = new ArrayList<TemperatureEvent>();
		String filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.TEMPERATURE_FILE;
		
		if (!filereader.fileExist(filePath)) {
			return temperatureEvents;
		}
		String[] contents = null;
		try {
			contents = filereader.readAllLine(filePath);
		} catch (IOException e) {
			logger.error("failed to read user event file: " + filePath);
		}
		if(contents != null && contents.length > 0){
			for (String contentBuf : contents) {
	
				// Ignore empty line
				if (contentBuf.trim().isEmpty()) {
					continue;
				}
	
				// Parse entry
				String splitContents[] = contentBuf.split(" ");	
				if (splitContents.length <= 1) {
					logger.warn("Found invalid user event entry: " + contentBuf);
					continue;
				}
	
				// Get timestamp
				double timeStamp = Double.parseDouble(splitContents[0]);
				if (timeStamp > 1.0e9) {
					timeStamp = Util.normalizeTime(timeStamp, startTime);
				}
	
				// Get event type
				int temperatureC = 0;
				int temperatureF = 0;
				
				if (splitContents.length > 1 && splitContents[0].length() > 0) {
					temperatureC = Integer.parseInt(splitContents[1]);
					if (temperatureC != 0) {
						if (splitContents[1].length() > 2) {
							temperatureC = temperatureC / (int) Math.pow(10, splitContents[1].length() - 2);
						}
						temperatureF = (((temperatureC * 9) / 5) + 32);

						temperatureEvents.add(new TemperatureEvent(timeStamp, temperatureC, temperatureF));
					}
				}
			}
		}
		return temperatureEvents;
	}

}
