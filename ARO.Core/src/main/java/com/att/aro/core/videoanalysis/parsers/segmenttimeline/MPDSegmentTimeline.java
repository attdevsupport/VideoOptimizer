/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
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
	
	@XmlAttribute(name = "minBufferTime")				private String minBufferTime = "";
	@XmlAttribute(name = "mediaPresentationDuration")	private String mediaPresentationDuration;
	@XmlAttribute(name = "minimumUpdatePeriod")			private String minimumUpdatePeriod;
	@XmlAttribute(name = "suggestedPresentationDelay")	private String suggestedPresentationDelay;
	
	@XmlAttribute private String type = "";
	@XmlAttribute private String availabilityStartTime = "";
	@XmlAttribute private String publishTime = "";
	@XmlAttribute private String timeShiftBufferDepth = "";

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
