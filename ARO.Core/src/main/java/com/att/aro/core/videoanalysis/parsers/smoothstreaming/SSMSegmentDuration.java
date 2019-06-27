package com.att.aro.core.videoanalysis.parsers.smoothstreaming;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * <c d="20020000" t="0"></c> <c d="20020000"></c> <c d="20020000"></c> ... <c d="20020000"></c> <c d="20020000"></c> <c d="19185833"></c>
 * 
 *
 */
public class SSMSegmentDuration {

	String d;
	String t;

	@Override
	public String toString() {
		return d;
	}

	@XmlAttribute(name = "d")
	public String getD() {
		return d;
	}

	@XmlAttribute(name = "t")
	public String getT() {
		return t;
	}

	public void setD(String d) {
		this.d = d;
	}

	public void setT(String t) {
		this.t = t;
	}

}
