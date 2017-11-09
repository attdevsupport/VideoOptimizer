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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.ISpeedThrottleEventReader;
import com.att.aro.core.peripheral.pojo.SpeedThrottleEvent;
import com.att.aro.core.peripheral.pojo.SpeedThrottleEvent.SpeedThrottleFlow;
import com.att.aro.core.util.Util;

/**
 * Read speed throttle log for the file and save in the speed throttle event pojo
 */

public class SpeedThrottleEventReaderImpl extends PeripheralBase implements ISpeedThrottleEventReader {
	
	@InjectLogger
	private static ILogger logger;
	private static final String DELIMITER = ",";

	@Override
	public List<SpeedThrottleEvent> readData(String directory) {
		List<SpeedThrottleEvent> throttleInfo = new ArrayList<SpeedThrottleEvent>();
		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.SPEED_THROTTLE_EVENT;

		if (!filereader.fileExist(filepath)) {
			return throttleInfo;
		}
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filepath);
		} catch (IOException e1) {
			logger.error("Failed to open file for Speed Throttle Event: " + filepath, e1);
		}
		if(lines != null && lines.length > 0){
			for (String strLineBuf : lines) {
				throttleInfo.add(processThrottleData(strLineBuf));				
			}
		}		
		return throttleInfo;
	}
	
	private SpeedThrottleEvent processThrottleData(String line){
		SpeedThrottleEvent throttleEvent = null;
		try {
			String[] tokens = line.split(DELIMITER);
			SpeedThrottleFlow throttleFlow =  SpeedThrottleFlow.valueOf(tokens[0].trim());
			int delayTime = Integer.valueOf(tokens[1].trim());
			long timeStamp = Long.valueOf(tokens[2].trim());
			throttleEvent = new SpeedThrottleEvent(throttleFlow,delayTime,timeStamp);
			
		}catch(NumberFormatException numberException){
			logger.error("Invalid number format : "+ line , numberException);
			return new SpeedThrottleEvent(SpeedThrottleFlow.DLT,0,0L);

		}catch (Exception exception) {
			logger.error("Invalid input: "+ line , exception);
			return new SpeedThrottleEvent(SpeedThrottleFlow.DLT,0,0L);
		}

		return throttleEvent;
	}

}
