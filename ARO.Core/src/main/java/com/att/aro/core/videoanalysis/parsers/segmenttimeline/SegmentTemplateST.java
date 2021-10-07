package com.att.aro.core.videoanalysis.parsers.segmenttimeline;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;

@Setter@Getter
@XmlRootElement(name = "SegmentTemplate")
@XmlAccessorType(XmlAccessType.FIELD)
public class SegmentTemplateST {
	
	@XmlAttribute private String duration;
	@XmlAttribute private String timescale;
	@XmlAttribute private String media = "";
	@XmlAttribute private String initialization = "";
	@XmlAttribute private int startNumber;
	@XmlAttribute private String presentationTimeOffset = "";
	
	@XmlElement(name = "SegmentTimeline") private SegmentTimeLineST segmentTimeline;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(167)
		.append("\n\t\t\tSegmentTemplate")
		.append("\n\t\t\t\ttimescale:              ").append(timescale)
		.append("\n\t\t\t\tpresentationTimeOffset: ").append(presentationTimeOffset)
		.append("\n\t\t\t\tmedia:                  ").append(media)
		.append("\n\t\t\t\tinitialization:         ").append(initialization)
		.append(segmentTimeline)
		.append("\n\t\t\t");
		return strblr.toString();
	}	
}
