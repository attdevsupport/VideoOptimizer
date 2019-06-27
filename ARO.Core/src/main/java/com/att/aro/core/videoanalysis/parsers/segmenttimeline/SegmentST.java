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
package com.att.aro.core.videoanalysis.parsers.segmenttimeline;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "S")
@XmlAccessorType(XmlAccessType.FIELD)
public class SegmentST {

	@XmlAttribute(name = "t") private String startTime = "";
	@XmlAttribute(name = "d") private String duration = "";
	@XmlAttribute(name = "r") private String repeat = "";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("\n\t\t\t\t\t\t\t\tSegment");
		if (!startTime.isEmpty()) {
			strblr.append(" startTime:").append(startTime);
		}
		strblr.append(" duration:").append(duration);
		if (!repeat.isEmpty()) {
			strblr.append(" repeat:").append(repeat);
		}
		return strblr.toString();
	}

}