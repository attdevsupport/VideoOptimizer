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
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.IGpsInfoReader;
import com.att.aro.core.peripheral.pojo.GpsInfo;
import com.att.aro.core.peripheral.pojo.GpsInfo.GpsState;
import com.att.aro.core.util.Util;

/**
 * Method to read the GPS data from the trace file and store it in the gpsInfos
 * list. It also updates the active duration for GPS.
 * 
 * Date: September 30, 2014
 */
public class GpsInfoReaderImpl extends PeripheralBase implements IGpsInfoReader {
	
	private static final Logger LOGGER = LogManager.getLogger(GpsInfoReaderImpl.class.getName());

	private double gpsActiveDuration = 0.0;

	@Override
	public double getGpsActiveDuration() {
		return this.gpsActiveDuration;
	}

	@Override
	public List<GpsInfo> readData(String directory, double startTime, double traceDuration) {
		this.gpsActiveDuration = 0;
		List<GpsInfo> gpsInfos = new ArrayList<GpsInfo>();
		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.GPS_FILE;
		if (!filereader.fileExist(filepath)) {
			return gpsInfos;
		}

		String[] lines = null;
		try {
			lines = filereader.readAllLine(filepath);
		} catch (IOException e1) {
			LOGGER.error("failed to read GPS info: " + filepath);
		}
		if (lines == null || lines.length < 1) {
			return gpsInfos;
		}

		double dLastActiveTimeStamp = 0.0;
		double dActiveDuration = 0.0;
		GpsState prevGpsState = null;
		GpsState gpsState = null;
		double beginTime = 0.0;
		double endTime = 0.0;
		String firstLine = lines[0];
		
		if (firstLine != null) {
			String strFieldsFirstLine[] = firstLine.split(" ");
			if (strFieldsFirstLine.length == 2) {
				try {
					beginTime = Util.normalizeTime(Double.parseDouble(strFieldsFirstLine[0]), startTime);
					if (TraceDataConst.GPS_STANDBY.equals(strFieldsFirstLine[1])) {
						prevGpsState = GpsState.GPS_STANDBY;
					} else if (TraceDataConst.GPS_DISABLED.equals(strFieldsFirstLine[1])) {
						prevGpsState = GpsState.GPS_DISABLED;
					} else if (TraceDataConst.GPS_ACTIVE.equals(strFieldsFirstLine[1])) {
						prevGpsState = GpsState.GPS_ACTIVE;
						if (0.0 == dLastActiveTimeStamp) {
							dLastActiveTimeStamp = beginTime;
						}
					} else {
						LOGGER.warn("Invalid GPS state: " + firstLine);
						prevGpsState = GpsState.GPS_UNKNOWN;
					}

					if ((!TraceDataConst.GPS_ACTIVE.equals(strFieldsFirstLine[1])) && dLastActiveTimeStamp > 0.0) {
						dActiveDuration += (beginTime - dLastActiveTimeStamp);
						dLastActiveTimeStamp = 0.0;
					}
				} catch (Exception e) {
					LOGGER.warn("Unexpected error parsing GPS event: " + firstLine, e);
				}
			}
			
			String strLineBuf;
			for (int i = 1; i < lines.length; i++) {
				strLineBuf = lines[i];
				String strFields[] = strLineBuf.split(" ");
				if (strFields.length == 2) {
					try {
						endTime = Util.normalizeTime(Double.parseDouble(strFields[0]), startTime);
						if (TraceDataConst.GPS_STANDBY.equals(strFields[1])) {
							gpsState = GpsState.GPS_STANDBY;
						} else if (TraceDataConst.GPS_DISABLED.equals(strFields[1])) {
							gpsState = GpsState.GPS_DISABLED;
						} else if (TraceDataConst.GPS_ACTIVE.equals(strFields[1])) {
							gpsState = GpsState.GPS_ACTIVE;
							if (0.0 == dLastActiveTimeStamp) {
								dLastActiveTimeStamp = endTime;
							}
						} else {
							LOGGER.warn("Invalid GPS state: " + strLineBuf);
							gpsState = GpsState.GPS_UNKNOWN;
						}
						gpsInfos.add(new GpsInfo(beginTime, endTime, prevGpsState));

						if ((!TraceDataConst.GPS_ACTIVE.equals(strFields[1])) && dLastActiveTimeStamp > 0.0) {
							dActiveDuration += (endTime - dLastActiveTimeStamp);
							dLastActiveTimeStamp = 0.0;
						}
						prevGpsState = gpsState;
						beginTime = endTime;

					} catch (Exception e) {
						LOGGER.warn("Unexpected error parsing GPS event: " + strLineBuf, e);
					}
				} else {
					LOGGER.warn("Invalid GPS trace entry: " + strLineBuf);
				}

			}

			gpsInfos.add(new GpsInfo(beginTime, traceDuration, prevGpsState));

			// Duration calculation should probably be done in analysis
			if (prevGpsState == GpsState.GPS_ACTIVE) {
				dActiveDuration += Math.max(0, traceDuration - dLastActiveTimeStamp);
			}

			this.gpsActiveDuration = dActiveDuration;
			Collections.sort(gpsInfos);
		}
		return gpsInfos;
	}
}
