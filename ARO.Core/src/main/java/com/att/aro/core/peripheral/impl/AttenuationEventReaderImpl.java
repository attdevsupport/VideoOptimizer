package com.att.aro.core.peripheral.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.IAttenuattionEventReader;
import com.att.aro.core.peripheral.pojo.AttenuatorEvent;
import com.att.aro.core.peripheral.pojo.AttenuatorEvent.AttnrEventFlow;
import com.att.aro.core.util.Util;

public class AttenuationEventReaderImpl extends PeripheralBase implements IAttenuattionEventReader{
	
	private static final Logger LOGGER = LogManager.getLogger(AttenuationEventReaderImpl.class.getName());
	private static final String DELIMITER = ",";

	@Override
	public List<AttenuatorEvent> readData(String directory){
		List<AttenuatorEvent> attnrInfos = new ArrayList<AttenuatorEvent>();
		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.ATTENUATION_EVENT;

		if (!filereader.fileExist(filepath)) {
			return attnrInfos;
		}
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filepath);
		} catch (IOException e1) {
			LOGGER.error("Failed to open file for Attenuation Event: " + filepath, e1);
		}
		if(lines != null && lines.length > 0){
			for (String strLineBuf : lines) {
				attnrInfos.add(processAttnrData(strLineBuf));				
			}
		}
 		
		return attnrInfos;
	}
	
	private AttenuatorEvent processAttnrData(String line){
		AttenuatorEvent attnrEvent = null;
		try {
			String[] tokens = line.split(DELIMITER);
			AttnrEventFlow atnrFL =  AttnrEventFlow.valueOf(tokens[0].trim());
			int delayTime = Integer.valueOf(tokens[1].trim());
			long timeStamp = Long.valueOf(tokens[2].trim());
			attnrEvent = new AttenuatorEvent(atnrFL,delayTime,timeStamp);
			
		}catch(NumberFormatException numberException){
			LOGGER.error("Invalid number format : "+ line , numberException);
			return new AttenuatorEvent(AttnrEventFlow.DL,0,0L);

		}catch (Exception exception) {
			LOGGER.error("Invalid input: "+ line , exception);
			return new AttenuatorEvent(AttnrEventFlow.DL,0,0L);
		}

		return attnrEvent;
	}
	
}
