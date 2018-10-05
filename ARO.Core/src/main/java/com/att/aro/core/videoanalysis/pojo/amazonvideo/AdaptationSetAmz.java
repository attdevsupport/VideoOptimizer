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

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "AdaptationSet")
@XmlType(propOrder = { "representation" })
public class AdaptationSetAmz {

	String contentType;
	List<RepresentationAmz> representation;
//	List<SegmentDurationListAmz> segmentDurationList; // audio only


	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("\n\tAdaptationSet\n\t\tcontentType:");
		strblr.append(contentType);
		int cntr = 0;
		for (RepresentationAmz set : representation) {
			strblr.append(cntr++ == 0 ? ": " : ", ");
			strblr.append(" RepresentationAmz :");
			strblr.append(set);
		}
		return strblr.toString();
	}

	public String getContentType() {
		return contentType;
	}

	@XmlAttribute(name = "contentType")
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@XmlElement(name = "Representation")
	public List<RepresentationAmz> getRepresentation() {
		return representation;
	}

	public void setRepresentation(List<RepresentationAmz> representation) {
		this.representation = representation;
	}

}