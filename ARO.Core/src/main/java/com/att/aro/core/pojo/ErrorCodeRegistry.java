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
		return err;
	}

	/**
	 * Trace file not found
	 * 
	 * @return an ErrorCode
	 */
	public static ErrorCode getTraceFileNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(101);
		err.setName("Trace file not found.");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " cannot find or access trace file user specified. Please check if the file exist.");
		return err;
	}

	/**
	 * Trace folder not found
	 * 
	 * @return an ErrorCode
	 */
	public static ErrorCode getTraceFolderNotFound() {
		ErrorCode err = new ErrorCode();
		err.setCode(102);
		err.setName("Trace folder not found.");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " cannot find or access trace folder.");
		return err;
	}

	public static ErrorCode getTraceDirectoryNotAnalyzed(){
		ErrorCode err = new ErrorCode();
		err.setCode(103);
		err.setName("Analyzing trace directory Failure");
		err.setDescription("Failed to analyze trace directory.");
		return err;
	}
	
	public static ErrorCode getTraceFileNotAnalyzed(){
		ErrorCode err = new ErrorCode();
		err.setCode(104);
		err.setName("Analyzing trace file Failure");
		err.setDescription("Failed to analyze trace file.");
		return err;

	}	
	
	public static ErrorCode getUnRecognizedPackets(){
		ErrorCode err = new ErrorCode();
		err.setCode(105);
		err.setName("Unrecognized Packets");
		err.setDescription("This trace has no packets or has unrecognized packets and their data will not be displayed");
		return err;

	}
	
	public static ErrorCode getUnknownFileFormat(){
		ErrorCode err = new ErrorCode();
		err.setCode(106);
		err.setName("Unknown file format");
		err.setDescription("Result from executing all pcap packets: unknown file format");
		return err;
	}
	
	public static ErrorCode getTrafficFileNotFound(){
		ErrorCode err = new ErrorCode();
		err.setCode(107);
		err.setName("Traffic file not found");
		err.setDescription("This trace doesn't seem to have a traffic file.");
		return err;
	}
	
	public static ErrorCode getTraceDirExist() {
		ErrorCode err = new ErrorCode();
		err.setCode(202);
		err.setName("Found existing trace directory that is not empty");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " found an existing directory that contains files and did not want to override it. Some files may be hidden.");
		return err;
	}

	public static ErrorCode getProblemAccessingDevice(String message) {
		ErrorCode err = new ErrorCode();
		err.setCode(215);
		err.setName("Problem accessing device");
		err.setDescription(ApplicationConfig.getInstance().getAppName() + " failed to access device :"+message);
		return err;

	}

	public static ErrorCode getOutOfMemoryError(){
		ErrorCode err = new ErrorCode();
		err.setCode(130);
		err.setName("Out of memory");
		err.setDescription("Trace too big to load");
		return err;
	}

}
