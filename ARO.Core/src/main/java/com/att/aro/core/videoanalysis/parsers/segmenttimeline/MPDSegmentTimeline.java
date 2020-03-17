package com.att.aro.core.videoanalysis.parsers.segmenttimeline;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.att.aro.core.videoanalysis.parsers.MpdBase;

import lombok.Data;

//xsi:schemaLocation="urn:mpeg:dash:schema:mpd:2011 DASH-MPD.xsd"

@Data
@XmlRootElement(name = "MPD", namespace = "urn:mpeg:dash:schema:mpd:2011")
@XmlAccessorType(XmlAccessType.FIELD)
public class MPDSegmentTimeline implements MpdBase {

	String majorVersion = "0";
	
	@XmlElement(name = "BaseURL")			private String baseURL = "";
	@XmlElement(name = "Period")			private List<PeriodST> period = new ArrayList<>();
	@XmlAttribute(name = "minBufferTime")	private String minBufferTime = "";

	@XmlAttribute private String type = "";
	@XmlAttribute private String availabilityStartTime = "";
	@XmlAttribute private String minimumUpdatePeriod = "";
	@XmlAttribute private String publishTime = "";
	@XmlAttribute private String timeShiftBufferDepth = "";
	@XmlAttribute private String suggestedPresentationDelay = "";

	private String videoName;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("MPDSegmentTimeline, Size :");
		strblr.append("\n\ttype=").append(getType());
		strblr.append("\n\tavailabilityStartTime=").append(getAvailabilityStartTime());

		if (period != null) {
			strblr.append(getSize());
			strblr.append(" List<PeriodST>").append(period);
		} else {
			strblr.append("period == null");
		}
		strblr.append('\n');
		strblr.append(super.toString());

		return strblr.toString();
	}
	
	@Override
	public int getSize() {
		return period.size();
	}

	@Override
	public String getVideoName() {
		return this.videoName;
	}

	public void setName(String videoName) {
		this.videoName = videoName;
	}
}
