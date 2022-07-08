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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.peripheral.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.pojo.ThermalStatus;
import com.att.aro.core.peripheral.pojo.ThermalStatusInfo;
import com.att.aro.core.util.Util;

public class ThermalStatusReaderImpl extends PeripheralBase {

	private static final Logger LOGGER = LogManager.getLogger(ThermalStatusReaderImpl.class.getSimpleName());

	public ThermalStatusReaderImpl(IFileManager fileReader) {
		setFileReader(fileReader);
	}

	public List<ThermalStatusInfo> readData(String directory, double startTime, double traceDuration) {

		List<ThermalStatusInfo> thermalStatusInfos = new ArrayList<ThermalStatusInfo>();
		String filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.THERMAL_STATUS;
		if (!filereader.fileExist(filePath)) {
			return thermalStatusInfos;
		}

		String[] lines = null;
		try {
			lines = filereader.readAllLine(filePath);
		} catch (IOException e1) {
			LOGGER.error("failed to read Thermal status file: " + filePath);
		}

		String line;
		if (lines != null && lines.length > 0) {
			line = lines[0];
			double beginTime = 0.0;
			double endTime = 0.0;
			ThermalStatus preThermalStatus = ThermalStatus.UNKNOWN;
			ThermalStatus currThermalStatus = ThermalStatus.UNKNOWN;

			String[] fields = line.split(" ");
			if (fields.length == 2) {
				beginTime = Util.normalizeTime(Double.parseDouble(fields[0]), startTime);
				try {
					preThermalStatus = ThermalStatus.getByCode(Integer.parseInt(fields[1]));
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
					preThermalStatus = ThermalStatus.UNKNOWN;
					LOGGER.error("failed to read Thermal status code: " + fields[1], ex);
				}
			}

			for (int i = 1; i < lines.length; i++) {
				line = lines[i];
				fields = line.split(" ");
				if (fields.length == 2) {
					endTime = Util.normalizeTime(Double.parseDouble(fields[0]), startTime);
					try {
						currThermalStatus = ThermalStatus.getByCode(Integer.parseInt(fields[1]));
					} catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
						currThermalStatus = ThermalStatus.UNKNOWN;
						LOGGER.error("failed to read Thermal status code: " + fields[1], ex);
					}
					thermalStatusInfos.add(new ThermalStatusInfo(beginTime, endTime, preThermalStatus));
					beginTime = endTime;
					preThermalStatus = currThermalStatus;
				}
			}
			thermalStatusInfos.add(new ThermalStatusInfo(beginTime, traceDuration, preThermalStatus));
		}

		return thermalStatusInfos;
	}

}
