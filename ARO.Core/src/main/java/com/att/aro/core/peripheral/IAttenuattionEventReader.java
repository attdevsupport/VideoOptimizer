package com.att.aro.core.peripheral;

import java.util.List;

import com.att.aro.core.peripheral.pojo.AttenuatorEvent;

 
public interface IAttenuattionEventReader {

	List<AttenuatorEvent> readData(String directory);
	
}