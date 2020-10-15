/*
 *  Copyright 2017 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis.parsers.encodedsegment;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "Representation")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepresentationESL {

	@XmlAttribute private String bandwidth = "";
	@XmlAttribute private String width = "";
	@XmlAttribute private String height = "";
	@XmlAttribute private String codecs;
	@XmlElement(name = "BaseURL")				String baseURL = "";
	@XmlElement(name = "EncodedSegmentList")	EncodedSegmentList encodedSegment;
	@XmlElement(name = "SegmentList")			SegmentListESL segmentList;
	@XmlElement(name = "AudioChannelConfiguration")  		RepresentationACC representationACC;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append(" bandwidth:");
		strblr.append(bandwidth);
		strblr.append(", AudioChannelConfiguration:");
		strblr.append(representationACC);
		strblr.append(", baseURL:");
		strblr.append(baseURL);
		
		if (encodedSegment != null) {
			strblr.append("\n\t\t\t\tEncodedSegmentList :");
			strblr.append(encodedSegment);
		}
		if (segmentList != null) {
			strblr.append("\n\t\t\t\tSegmentList :");
			strblr.append(segmentList);
		}
		return strblr.toString();
	}
}