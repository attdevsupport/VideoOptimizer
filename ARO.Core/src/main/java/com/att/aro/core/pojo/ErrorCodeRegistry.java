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
package com.att.aro.core.pojo;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.util.GoogleAnalyticsUtil;

/**
 * Standardized ErrorCodes for ARO.Core<br>
 * 
 * <p>
 * <pre>
 *  Rules for definition of codes
 *  * codes start at 100
 * </pre>
 * </p>
 * 
 * Date: Feb 24, 2015
 */
public final class ErrorCodeRegistry {
	private ErrorCodeRegistry() {
	}

	/**
	 * Trace directory not found
	 * 
	 * @return an ErrorCode
	 */
	public static ErrorCode getTraceDirNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(100);
		err.setName("Trace Directory not found.");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " cannot find or access the trace directory user specified. Please check if the directory exists.");
		sendGAErrorCode(err);
		return err;
	}

	/**
	 * Trace file not found
	 * 
	 * @return an ErrorCode
	 */
	public static ErrorCode getTraceFileNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.TRACE_FILE_NOT_FOUND.getCode());
		err.setName("Trace file not found.");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " cannot find or access trace file user specified. Please check if the file exist.");
		sendGAErrorCode(err);
		return err;
	}

	/**
	 * Trace folder not found
	 * 
	 * @return an ErrorCode
	 */
	public static ErrorCode getTraceFolderNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.TRACE_FOLDER_NOT_FOUND.getCode());
		err.setName("Trace folder not found.");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " cannot find or access trace folder.");
		sendGAErrorCode(err);
		return err;
	}

	public static ErrorCode getTraceDirectoryNotAnalyzed(){
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.TRACE_FOLDER_NOT_ANALYSED.getCode());
		err.setName("Analyzing trace directory Failure");
		err.setDescription("Failed to analyze trace directory.");
		sendGAErrorCode(err);
		return err;
	}
	
	public static ErrorCode getTraceFileNotAnalyzed(){
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.TRACE_FILE_NOT_ANALYSED.getCode());
		err.setName("Analyzing trace file Failure");
		err.setDescription("Failed to analyze trace file.");
		sendGAErrorCode(err);
		return err;

	}
	
	public static ErrorCode getUnRecognizedPackets(){
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.UNRECOGNIZED_PACKETS.getCode());
		err.setName("Unrecognized Packets");
		err.setDescription("This trace has no packets or has unrecognized packets and their data will not be displayed");
		sendGAErrorCode(err);
		return err;

	}
	
	public static ErrorCode getUnknownFileFormat(){
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.UNKNOWN_FORMAT.getCode());
		err.setName("Unknown file format");
		err.setDescription("Result from executing all pcap packets: unknown file format");
		sendGAErrorCode(err);
		return err;
	}
	
	public static ErrorCode getPacketsNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.PACKETS_NOT_FOUND.getCode());
		err.setName("Traffic file has no packet information");
		err.setDescription("There was no activity in the traffic file.");
		sendGAErrorCode(err);
		return err;
	}

	public static ErrorCode getTrafficFileNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.TRAFFIC_FILE_NOT_FOUND.getCode());
		err.setName("Traffic file not found");
		err.setDescription("This trace doesn't seem to have a traffic file.");
		sendGAErrorCode(err);
		return err;
	}
	
	public static ErrorCode getTraceDirExist() {
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.DIR_EXIST.getCode());
		err.setName("Found existing trace directory that is not empty");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " found an existing directory that contains files and did not want to override it. Some files may be hidden.");
		sendGAErrorCode(err);
		return err;
	}

	public static ErrorCode getProblemAccessingDevice(String message) {
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.DEVICE_ACCESS.getCode());
		err.setName("Problem accessing device");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " failed to access device :"+message);
		sendGAErrorCode(err);
		return err;

	}

	public static ErrorCode getOutOfMemoryError(){
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.OUT_OF_MEMEORY.getCode());
		err.setName("Out of memory");
		err.setDescription("Trace is too large to load and has caused the system to reach it's memory limit. You may try reopening the trace after allocating more memory by "
				+ "going into File->Preferences...->General->Max heap in GB.");
		sendGAErrorCode(err);
		return err;
	}

	public static ErrorCode getPostError(String message){
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.POST_ERROR.getCode());
		err.setName("REST:POST failure");
		err.setDescription(message);
		sendGAErrorCode(err);
		return err;
	}
	
	public static ErrorCode getWiresharkError(){
		ErrorCode err = new ErrorCode();
		err.setCode(ErrorCodeEnum.WIRESHARK_NOT_FOUND.getCode());
		err.setName("Missing Wireshark");
		err.setDescription("Wireshark is either not installed on your machine or its not in your path. \nGo to https://www.wireshark.org to download.");
		sendGAErrorCode(err);
		return err;
	}
	
	private static void sendGAErrorCode(ErrorCode err){
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendErrorEvents(err.getName(),err.getDescription(), false);
	}

}
