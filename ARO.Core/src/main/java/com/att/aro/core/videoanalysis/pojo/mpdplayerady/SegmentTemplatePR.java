package com.att.aro.core.videoanalysis.pojo.mpdplayerady;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "SegmentTemplate")
@XmlType(propOrder = { "segmentTimeline" })
public class SegmentTemplatePR {
	
	String timescale = "";
	String presentationTimeOffset = "";
	String media = "";
	String initialization = "";
	
	SegmentTimeLinePR segmentTimeline;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(167)
		.append("\n\t\t\tSegmentTemplate\n\t\t\t\ttimescale:              ").append(timescale)
		.append("\n\t\t\t\tpresentationTimeOffset: ").append(presentationTimeOffset)
		.append("\n\t\t\t\tmedia:                  ").append(media)
		.append("\n\t\t\t\tinitialization:         ").append(initialization)
		.append(segmentTimeline)
		.append("\n\t\t\t");
		return strblr.toString();
	}

	public SegmentTimeLinePR getSegmentTimeline() {
		return segmentTimeline;
	}

	@XmlElement(name = "SegmentTimeline")	
	public void setSegmentTimeline(SegmentTimeLinePR segmentTimeline) {
		this.segmentTimeline = segmentTimeline;
	}

	@XmlAttribute
	public String getTimescale() {
		return timescale;
	}

	@XmlAttribute
	public String getPresentationTimeOffset() {
		return presentationTimeOffset;
	}

	@XmlAttribute
	public String getMedia() {
		return media;
	}

	@XmlAttribute
	public String getInitialization() {
		return initialization;
	}

	public void setTimescale(String timescale) {
		this.timescale = timescale;
	}

	public void setPresentationTimeOffset(String presentationTimeOffset) {
		this.presentationTimeOffset = presentationTimeOffset;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public void setInitialization(String initialization) {
		this.initialization = initialization;
	}
	
	
	
}
