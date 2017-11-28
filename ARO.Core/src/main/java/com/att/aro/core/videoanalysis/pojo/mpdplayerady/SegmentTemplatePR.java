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
		StringBuilder strblr = new StringBuilder(167);
		strblr.append("\n\t\t\tSegmentTemplate\n\t\t\t\ttimescale:              "); strblr.append(timescale);
		strblr.append("\n\t\t\t\tpresentationTimeOffset: "); strblr.append(presentationTimeOffset);
		strblr.append("\n\t\t\t\tmedia:                  "); strblr.append(media);
		strblr.append("\n\t\t\t\tinitialization:         "); strblr.append(initialization);
		strblr.append(segmentTimeline);
		strblr.append("\n\t\t\t");
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
