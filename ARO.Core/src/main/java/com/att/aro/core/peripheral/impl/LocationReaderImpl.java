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
import com.att.aro.core.peripheral.LocationReader;
import com.att.aro.core.peripheral.pojo.LocationEvent;
import com.att.aro.core.util.Util;

/**
 * read location events captured in the file
 * Date: January 3, 2017
 *
 */
public class LocationReaderImpl extends PeripheralBase implements LocationReader {

	@InjectLogger
	private static ILogger logger;
	
	@Override
	public List<LocationEvent> readData(String directory, double startTime) {
		List<LocationEvent> locationEvents = new ArrayList<LocationEvent>();
		String filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.LOCATION_FILE;
		
		if (!filereader.fileExist(filePath)) {
			return locationEvents;
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
					logger.warn("Found invalid event entry: " + contentBuf);
					continue;
				}
	
				// Get timestamp
				double timeStamp = Double.parseDouble(splitContents[0]);
				if (timeStamp > 1.0e9) {
					timeStamp = Util.normalizeTime(timeStamp, startTime);
				}
	
				// Get event
				double latitude = 0;
				double longitude = 0;
				String provider = "";
				StringBuffer locality = new StringBuffer();
				
				// Format the Location data is stored in log file
				// EVENT_TIME LAT_VALUE LONG_VALUE LOCALITY PROVIDER
				if (splitContents.length >= 4) {
					try {
						latitude = Double.parseDouble(splitContents[1]);
						longitude = Double.parseDouble(splitContents[2]);
						provider = splitContents[3];

						for (int idx = 4; idx <= (splitContents.length - 1); idx++) {
							if (locality.length() > 0) {
								locality.append(' ');
							}
							locality.append(splitContents[idx]);
						}

						locationEvents
								.add(new LocationEvent(timeStamp, latitude, longitude, provider, locality.toString()));
					} catch (Exception e) {
						logger.warn("Found invalid event entry: " + contentBuf);
						continue;
					}
				}
			}
		}
		return locationEvents;
	}

}
