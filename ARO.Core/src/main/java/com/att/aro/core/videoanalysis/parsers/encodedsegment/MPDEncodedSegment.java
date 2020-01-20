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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.att.aro.core.videoanalysis.parsers.MpdBase;

import lombok.Data;

//	xsi:schemaLocation="urn:mpeg:dash:schema:mpd:2011 http://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-DASH_schema_files/DASH-MPD.xsd"

@Data
@XmlRootElement(name = "MPD", namespace = "urn:mpeg:dash:schema:mpd:2011")
@XmlAccessorType(XmlAccessType.FIELD)
public class MPDEncodedSegment implements MpdBase{

	@XmlElement(name = "Period")
	List<PeriodESL> period = new ArrayList<>();
	@XmlAttribute String majorVersion;
	@XmlAttribute String mediaPresentationDuration;
	@XmlAttribute String minBufferTime;
	@XmlAttribute String minorVersion;
	@XmlAttribute String profiles;
	@XmlAttribute String revision;
	@XmlAttribute String type;
	
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("MPDEncodedSegment :")
		.append(" type :").append( type)
		.append(", majorVersion :").append( majorVersion)
		.append(", mediaPresentationDuration :").append( mediaPresentationDuration)
		.append(", minBufferTime :").append( minBufferTime)
		.append(", minorVersion :").append( minorVersion)
		.append(", profiles :").append( profiles)
		.append(", revision :").append( revision)
		.append("\n\tPeriod:").append(getPeriod());
		return strblr.toString();
	}
	
	@Override
	public int getSize() {
		return getPeriod().size();
	}

	@Override
	public String getVideoName() {
		return null;
	}

}