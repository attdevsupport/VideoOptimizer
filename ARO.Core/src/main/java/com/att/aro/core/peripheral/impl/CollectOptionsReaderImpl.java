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

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.ICollectOptionsReader;
import com.att.aro.core.peripheral.pojo.CollectOptions;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.Orientation;

public class CollectOptionsReaderImpl extends PeripheralBase implements ICollectOptionsReader {
	@InjectLogger
	private static ILogger logger;
	
	//FIXME This shouldn't be a member variable.
	private String orientation = Orientation.PORTRAIT.toString();

	public void setLogger(ILogger logger) {
		this.logger = logger;
	}

	@Override
	public CollectOptions readData(String directory) {
		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.COLLECT_OPTIONS;
		CollectOptions collectOptions = new CollectOptions();
		
		if (!filereader.fileExist(filepath)) {
			return collectOptions;
		}
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filepath);
		} catch (IOException e) {
			logger.error("failed to read collection detail file: "+filepath);
		}
		if(lines == null || lines.length < 1){
			return collectOptions;
		}
		collectOptions.setTotalLines(lines.length);
		if (lines.length == 1) {
			String[] orientationType = lines[0].split(":");
			if (orientationType.length > 1) {
				orientation = orientationType[1];
			}
		} else {
			orientation = Orientation.PORTRAIT.name();
		}
		collectOptions.setOrientation(orientation);

	logger.info("result for collection: "+ 
				"orientation: "+ collectOptions.getOrientation() );
		return collectOptions;
	}

	public String getOrientation() {
		return orientation;
	}
}