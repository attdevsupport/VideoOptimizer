package com.att.aro.core.videoanalysis.parsers.encodedsegment;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "AudioChannelConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepresentationACC {
	
	@XmlAttribute private String schemeIdUri = "";
	@XmlAttribute private String value = "";
	
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder();
		strblr.append(" schemeIdUri:");
		strblr.append(schemeIdUri);
		strblr.append(", value:");
		strblr.append(value);

		return strblr.toString();
	}
}