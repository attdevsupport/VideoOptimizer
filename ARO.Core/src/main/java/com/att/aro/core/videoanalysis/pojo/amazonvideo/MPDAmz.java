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
package com.att.aro.core.videoanalysis.pojo.amazonvideo;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "MPD", namespace = "urn:mpeg:dash:schema:mpd:2011")
public class MPDAmz {

	List<PeriodAmz> period = new ArrayList<>();
	String majorVersion;
	String mediaPresentationDuration;
	String minBufferTime;
	String minorVersion;
	String profiles;
	String revision;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("MPDAmz :"); 
		strblr.append(" majorVersion :"); strblr.append( majorVersion);
		strblr.append(",  mediaPresentationDuration :"); strblr.append( mediaPresentationDuration);
		strblr.append(",  minBufferTime :"); strblr.append( minBufferTime);
		strblr.append(",  minorVersion :"); strblr.append( minorVersion);
		strblr.append(",  profiles :"); strblr.append( profiles);
		strblr.append(",  revision :"); strblr.append( revision);
		strblr.append("\n\tPeriod:"); strblr.append(getPeriod());
		return strblr.toString();
	}
	
	@XmlElement(name = "Period")
	public List<PeriodAmz> getPeriod() {
		return period;
	}

	public void setPeriod(List<PeriodAmz> url) {
		this.period = url;
	}

	@XmlAttribute
	public String getMajorVersion() {
		return majorVersion;
	}

	public void setMajorVersion(String majorVersion) {
		this.majorVersion = majorVersion;
	}

	@XmlAttribute
	public String getMediaPresentationDuration() {
		return mediaPresentationDuration;
	}

	public void setMediaPresentationDuration(String mediaPresentationDuration) {
		this.mediaPresentationDuration = mediaPresentationDuration;
	}

	@XmlAttribute
	public String getMinBufferTime() {
		return minBufferTime;
	}

	public void setMinBufferTime(String minBufferTime) {
		this.minBufferTime = minBufferTime;
	}

	@XmlAttribute
	public String getMinorVersion() {
		return minorVersion;
	}

	public void setMinorVersion(String minorVersion) {
		this.minorVersion = minorVersion;
	}

	@XmlAttribute
	public String getProfiles() {
		return profiles;
	}

	public void setProfiles(String profiles) {
		this.profiles = profiles;
	}

	@XmlAttribute
	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

}