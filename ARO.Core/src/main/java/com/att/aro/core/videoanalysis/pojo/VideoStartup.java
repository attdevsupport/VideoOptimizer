package com.att.aro.core.videoanalysis.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * All numbers are to refer to trace normalized timestamp
 * 
 * @author barrynelson
 *
 */
@Data
@AllArgsConstructor
public class VideoStartup {
	String appName = "";
	double manifestArrived = 0;
	double prepareToPlay = 0;
	/**
	 * total number of member variables
	 */
	public static final int COLUMN_COUNT = 3;
}
