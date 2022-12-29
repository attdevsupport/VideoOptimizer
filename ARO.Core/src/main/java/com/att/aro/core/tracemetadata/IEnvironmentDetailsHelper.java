package com.att.aro.core.tracemetadata;

import java.util.List;

import com.att.aro.core.datacollector.pojo.EnvironmentDetails;

public interface IEnvironmentDetailsHelper {

	EnvironmentDetails getEnvironmentDetails();
	
	boolean save();

	void save(String path) throws Exception;

	void save(String path, EnvironmentDetails environmentDetails) throws Exception;

	EnvironmentDetails loadEnvironmentDetails(String tracePath) throws Exception;
}
