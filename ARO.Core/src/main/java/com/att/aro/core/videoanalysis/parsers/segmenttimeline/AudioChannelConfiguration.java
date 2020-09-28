package com.att.aro.core.videoanalysis.parsers.segmenttimeline;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "AudioChannelConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class AudioChannelConfiguration {
	
	@XmlAttribute private String schemeIdUri = "";
	@XmlAttribute private String value = "";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(167)
		.append("\n\t\t\tAudioChannelConfiguration")
		.append("\n\t\t\t\tschemeIdUri:              ").append(schemeIdUri)
		.append("\n\t\t\t\tvalue: ").append(value)
		.append("\n\t\t\t");
		return strblr.toString();
	}	
}
