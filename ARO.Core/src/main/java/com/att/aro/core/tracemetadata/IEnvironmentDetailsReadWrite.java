package com.att.aro.core.tracemetadata;

import java.io.File;

import com.att.aro.core.datacollector.pojo.EnvironmentDetails;

public interface IEnvironmentDetailsReadWrite {
	
	EnvironmentDetails getEnvironmentDetails();
	
	EnvironmentDetails readData(String tracePath);

	boolean save();
	public boolean save(File traceFolder, EnvironmentDetails environmetalDetails) throws Exception;

}
