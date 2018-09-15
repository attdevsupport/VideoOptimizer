package com.att.aro.core.videoanalysis.pojo.mpdplayerady;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.att.aro.core.videoanalysis.pojo.amazonvideo.MpdBase;

//xsi:schemaLocation="urn:mpeg:dash:schema:mpd:2011 DASH-MPD.xsd"

@XmlRootElement(name = "MPD", namespace = "urn:mpeg:dash:schema:mpd:2011")
public class MPDPlayReady implements MpdBase {

	String majorVersion = "0";

	List<PeriodPR> period = new ArrayList<>();

	String type = "";
	String availabilityStartTime = "";
	String minimumUpdatePeriod = "";
	String minBufferTime = "";
	String publishTime = "";
	String timeShiftBufferDepth = "";
	String suggestedPresentationDelay = "";

	private String videoName;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("MPDPlayReady, Size :");
		strblr.append("\n\ttype=");
		strblr.append(getType());
		strblr.append("\n\tavailabilityStartTime=");
		strblr.append(getAvailabilityStartTime());

		if (period != null) {
			strblr.append(getSize());
			strblr.append(" List<PeriodPR>");
			strblr.append(period);
		} else {
			strblr.append("period == null");
		}
		strblr.append('\n');
		strblr.append(super.toString());

		return strblr.toString();
	}
	
	@Override
	public String getMajorVersion() {
		return majorVersion;
	}

	@XmlElement(name = "Period")
	public List<PeriodPR> getPeriod() {
		return period;
	}

	@XmlAttribute(name = "minBufferTime")
	public String getType() {
		return type;
	}

	@XmlAttribute
	public String getAvailabilityStartTime() {
		return availabilityStartTime;
	}

	@XmlAttribute
	public String getMinimumUpdatePeriod() {
		return minimumUpdatePeriod;
	}

	@XmlAttribute
	public String getMinBufferTime() {
		return minBufferTime;
	}

	@XmlAttribute
	public String getPublishTime() {
		return publishTime;
	}

	@XmlAttribute
	public String getTimeShiftBufferDepth() {
		return timeShiftBufferDepth;
	}

	@XmlAttribute
	public String getSuggestedPresentationDelay() {
		return suggestedPresentationDelay;
	}

	public void setMajorVersion(String majorVersion) {
		this.majorVersion = majorVersion;
	}

	public void setPeriod(List<PeriodPR> period) {
		this.period = period;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setAvailabilityStartTime(String availabilityStartTime) {
		this.availabilityStartTime = availabilityStartTime;
	}

	public void setMinimumUpdatePeriod(String minimumUpdatePeriod) {
		this.minimumUpdatePeriod = minimumUpdatePeriod;
	}

	public void setMinBufferTime(String minBufferTime) {
		this.minBufferTime = minBufferTime;
	}

	public void setPublishTime(String publishTime) {
		this.publishTime = publishTime;
	}

	public void setTimeShiftBufferDepth(String timeShiftBufferDepth) {
		this.timeShiftBufferDepth = timeShiftBufferDepth;
	}

	public void setSuggestedPresentationDelay(String suggestedPresentationDelay) {
		this.suggestedPresentationDelay = suggestedPresentationDelay;
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
	
	// all setters

}
