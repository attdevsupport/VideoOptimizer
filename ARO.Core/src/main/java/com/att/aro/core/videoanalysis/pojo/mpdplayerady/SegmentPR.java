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
package com.att.aro.core.videoanalysis.pojo.mpdplayerady;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "S")
public class SegmentPR {

	String duration = "";
	String startTime = "";
	String repeat = "";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("\n\t\t\t\t\t\t\t\tSegment");
		if (!startTime.isEmpty()) {
			strblr.append(" startTime:"); strblr.append(startTime);
		}
		strblr.append(" duration:"); strblr.append(duration);
		if (!repeat.isEmpty()) {
			strblr.append(" repeat:"); strblr.append(repeat);
		}
		return strblr.toString();
	}

	public String getDuration() {
		return duration;
	}

	@XmlAttribute(name = "d")
	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getStartTime() {
		return startTime;
	}

	@XmlAttribute(name = "t")
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getRepeat() {
		return repeat;
	}
	
	@XmlAttribute(name = "r")
	public void setRepeat(String repeat) {
		this.repeat = repeat;
	}

	
}