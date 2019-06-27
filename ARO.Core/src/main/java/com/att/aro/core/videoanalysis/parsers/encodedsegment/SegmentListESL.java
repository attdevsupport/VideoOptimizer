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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "SegmentList")
@XmlAccessorType(XmlAccessType.FIELD)
public class SegmentListESL {

	@XmlAttribute String duration;
	@XmlAttribute String timescale;
	
	@XmlElement(name = "Initialization")	Initialization initialization;
	@XmlElement(name = "SegmentURL")		List<SegmentUrlESL> segmentUrlList;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append(" duration:");
		strblr.append(duration);
		strblr.append(", timescale:");
		strblr.append(timescale);
		if (initialization != null) {
			strblr.append("\n\t\t\t\t\t");
			strblr.append(initialization);
		}
		if (segmentUrlList != null) {
			for (SegmentUrlESL segmentURL : segmentUrlList) {
				strblr.append("\n\t\t\t\t\t");
				strblr.append(segmentURL);
			}
		}
		return strblr.toString();
	}
}