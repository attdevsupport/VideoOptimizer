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
@XmlRootElement(name = "Representation")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepresentationST {

	@XmlAttribute private String bandwidth = "";
	@XmlAttribute private String width = "";
	@XmlAttribute private String height = "";
	@XmlAttribute private String frameRate = "";
	@XmlAttribute private String codecs = "";
	@XmlAttribute private String scanType = "";
	@XmlAttribute(name = "id") private String contentID = "";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		if (!contentID.isEmpty()) {
			strblr.append(" id:");
			strblr.append(contentID);
		}
		if (!bandwidth.isEmpty()) {
			strblr.append(", bandwidth:");
			strblr.append(String.format("%8s", bandwidth));
		}
		if (!width.isEmpty()) {
			strblr.append(", width:");
			strblr.append(String.format("%4s", width));
		}
		if (!height.isEmpty()) {
			strblr.append(", height:");
			strblr.append(String.format("%4s", height));
		}
		if (!codecs.isEmpty()) {
			strblr.append(", codecs:");
			strblr.append(codecs);
		}
		
		return strblr.toString();
	}

}