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
