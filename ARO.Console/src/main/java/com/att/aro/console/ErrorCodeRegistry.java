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
package com.att.aro.console;

import com.att.aro.core.pojo.ErrorCode;


public final class ErrorCodeRegistry {
	private ErrorCodeRegistry(){}
	public static ErrorCode getUnsupportedCollector(){
		ErrorCode error = new ErrorCode();
		error.setCode(300);
		error.setName("Unsupported collector");
		error.setDescription("Collector name passed in is not supported.");
		return error;
	}
	public static ErrorCode getOutputRequired(){
		ErrorCode err = new ErrorCode();
		err.setCode(301);
		err.setName("Output location missing");
		err.setDescription("location to save data to was not given.");
		return err;
	}
	public static ErrorCode getUnsupportedFormat(){
		ErrorCode err = new ErrorCode();
		err.setCode(302);
		err.setName("Unsupported report format");
		err.setDescription("Report format entered is not supported. Type --help to see supported commands and options.");
		return err;
	}
	public static ErrorCode getInvalidVideoOption(){
		ErrorCode err = new ErrorCode();
		err.setCode(303);
		err.setName("Invalid video option");
		err.setDescription("Valid video option is yes or no. Invalid value was entered.");
		return err;
	}
	public static ErrorCode getFileExist(){
		ErrorCode err = new ErrorCode();
		err.setCode(304);
		err.setName("File or directory exists");
		err.setDescription("ARO found existing file or directory");
		return err;
	}
	public static ErrorCode getCollectorNotfound(){
		ErrorCode err = new ErrorCode();
		err.setCode(305);
		err.setName("Collector not found");
		err.setDescription("Data collector specified cannot be found. Make sure data collector is installed.");
		return err;
	}
	public static ErrorCode getSecureEnableRequired() {
		ErrorCode err = new ErrorCode();
		err.setCode(306);
		err.setName("Secure not enabled");
		err.setDescription("Certificate installation requires enable secure collector using '--secure' option.");
		return err;
	}
	public static ErrorCode getSecureNotApplicable() {
		ErrorCode err = new ErrorCode();
		err.setCode(307);
		err.setName("Secure not applicable");
		err.setDescription("--secure option is not applicable for ios and rooted android device.");
		return err;
	}
	public static ErrorCode getAttenuatorNotApplicable() {
		ErrorCode err = new ErrorCode();
		err.setCode(308);
		err.setName("Attenuator not applicable");
		err.setDescription("--uplink and --downlink options are not applicable for ios and rooted android device.");
		return err;
	}
	public static ErrorCode getInvalidUplink() {
		ErrorCode err = new ErrorCode();
		err.setCode(309);
		err.setName("Invalid uplink throttle value");
		err.setDescription("Uplink value should be a number and range from 64 to 100m(102400k).");
		return err;
	}
	public static ErrorCode getInvalidDownlink() {
		ErrorCode err = new ErrorCode();
		err.setCode(310);
		err.setName("Invalid downlink throttle value");
		err.setDescription("Downlink value should be a number and range from 64 to 100m(102400k).");
		return err;
	}
}
