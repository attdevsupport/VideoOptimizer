package com.att.aro.core.videoanalysis.pojo;

import lombok.Data;

@Data
public class UrlMatchDef {

	private boolean prefix = false;
	private int urlMatchLen = 0;
	private UrlMatchType urlMatchType = UrlMatchType.UNKNOWN;
	
	public enum UrlMatchType {
		FULL, HEAD, TAIL, OBJNAME, NAME, COUNT, UNKNOWN
	}

	public boolean checkLength(int length) {
		return urlMatchLen == length;
	}
}