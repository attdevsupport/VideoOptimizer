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

public enum ErrorCodeEnum {

	TRACE_FILE_NOT_FOUND(101), 
	TRACE_FOLDER_NOT_FOUND(102), 
	TRACE_FOLDER_NOT_ANALYSED(103), 
	TRACE_FILE_NOT_ANALYSED(104), 
	UNRECOGNIZED_PACKETS(105), 
	UNKNOWN_FORMAT( 106), 
	PACKETS_NOT_FOUND(107), 
	TRAFFIC_FILE_NOT_FOUND(108),
	WIRESHARK_NOT_FOUND(250),
	DIR_EXIST(202), 
	DEVICE_ACCESS(215), 
	OUT_OF_MEMEORY(130), 
	POST_ERROR(140);

	private final Integer errorCode;

	private ErrorCodeEnum(Integer errorCode) {
		this.errorCode = errorCode;
	}

	public Integer getCode() {
		return errorCode;
	}
	
}
