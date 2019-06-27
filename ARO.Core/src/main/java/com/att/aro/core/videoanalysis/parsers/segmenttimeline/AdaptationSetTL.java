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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "AdaptationSet")
@XmlAccessorType(XmlAccessType.FIELD)
public class AdaptationSetTL {

	@XmlAttribute(name = "contentType")	private String contentType;
	@XmlAttribute(name = "mimeType")	private String mimeType;
	@XmlElement(name = "Representation")  private List<RepresentationST> representation;
	@XmlElement(name = "SegmentTemplate") private SegmentTemplateST segmentTemplate;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("\n\t\tAdaptationSet\n\t\t\tmimeType:").append(mimeType);
		strblr.append("\n\t\t\tcontentType:").append(contentType);
		int cntr = 0;
		strblr.append(segmentTemplate);
		for (RepresentationST repSet : representation) {
			strblr.append(cntr++ == 0 ? ": " : ", ");
			strblr.append("\n\t\t RepresentationTL :").append(repSet);
		}
		return strblr.toString();
	}
}