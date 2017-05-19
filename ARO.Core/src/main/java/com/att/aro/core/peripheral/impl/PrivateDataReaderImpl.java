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
package com.att.aro.core.peripheral.impl;

import java.util.LinkedList;
import java.util.List;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.IPrivateDataReader;
import com.att.aro.core.peripheral.pojo.PrivateDataInfo;
import com.att.aro.core.util.Util;

public class PrivateDataReaderImpl extends PeripheralBase implements IPrivateDataReader {

	@InjectLogger
	private static ILogger logger;
	
	private static final String DELIMITER = ",";
	
	@Override
	public List<PrivateDataInfo> readData(String directory) {
		List<PrivateDataInfo> privateDataInfos = new LinkedList<PrivateDataInfo>();
		String filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.PRIVATE_DATA_FILE;
		if (!filereader.fileExist(filePath)) {
			return privateDataInfos;
		}
		
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filePath);
		} catch (Exception e) {
			logger.error("failed to read private data info file: "+ filePath);
		}
		
		if (lines != null && lines.length != 0) {
			for(String line : lines) {
				PrivateDataInfo privateDataInfo = processLine(line);
				if (privateDataInfo != null) {
					privateDataInfos.add(privateDataInfo);
				}
			}
		}
		
		return privateDataInfos;
	}
	
	/**
	 * format of private data line in the file
	 * [category],[type],[value],[isSelected]
	 * 
	 * e.g.:
	 * 
	 * KEYWORD,PHONE NUMBER,(443)-234-5678,Y
	 * 
	 * @param line
	 * @return
	 */
	private PrivateDataInfo processLine(String line) {
		PrivateDataInfo privateDataInfo = null;
		try {
			String[] tokens = line.split(DELIMITER);
			privateDataInfo = new PrivateDataInfo();
			privateDataInfo.setCategory(tokens[0].trim());
			privateDataInfo.setType(tokens[1].trim());
			privateDataInfo.setValue(tokens[2].trim());
			boolean isSelected = TraceDataConst.PrivateData.YES_SELECTED.equals(tokens[3].trim())? true : false;
			privateDataInfo.setSelected(isSelected);
			return privateDataInfo;
		} catch (Exception e) {
			logger.error("Invalid format of pattern from user setting: "+ line);
			return null;
		}
	}
}
