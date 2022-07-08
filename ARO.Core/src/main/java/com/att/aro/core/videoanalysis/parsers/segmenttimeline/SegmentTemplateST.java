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
