package com.att.aro.core.videoanalysis.pojo;

import lombok.Data;

@Data
public class UrlMatchDef {

	public static final String URLMATCHDEF = "UrlMatchDef";
	private boolean prefix = false;
	private int urlMatchLen = 0;
	private UrlMatchType urlMatchType = UrlMatchType.UNKNOWN;
	
	public enum UrlMatchType {
		FULL, COUNT, UNKNOWN
	}

	public boolean checkLength(int length) {
		return urlMatchLen == length;
	}

	@Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final UrlMatchDef other = (UrlMatchDef) obj;

        if (this.urlMatchType != other.urlMatchType) {
            return false;
        }

        if (this.urlMatchLen != other.urlMatchLen) {
            return false;
        }

        if (!this.prefix ^ other.prefix) {
            return false;
        }

        return true;
    }

	@Override
	public int hashCode() {
	    return Boolean.hashCode(prefix) + Integer.hashCode(urlMatchLen) + urlMatchType.hashCode();
	}

	@Override
	public String toString() {
	    return String.format("prefix: %s, length: %d, type: %s", prefix, urlMatchLen, urlMatchType.name());
	}
}
