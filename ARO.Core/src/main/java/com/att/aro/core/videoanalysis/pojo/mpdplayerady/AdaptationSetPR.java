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

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "AdaptationSet")
@XmlType(propOrder = { "segmentTemplate", "representation" })
public class AdaptationSetPR {

	String contentType;
	String mimeType;
	List<RepresentationPR> representation;
	SegmentTemplatePR segmenttemplate;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("\n\t\tAdaptationSet\n\t\t\tmimeType:");
		strblr.append(mimeType);
		strblr.append("\n\t\t\tcontentType:");
		strblr.append(contentType);
		int cntr = 0;
		strblr.append(segmenttemplate);
		for (RepresentationPR repSet : representation) {
			strblr.append(cntr++ == 0 ? ": " : ", ");
			strblr.append("\n\t\t RepresentationPR :");
			strblr.append(repSet);
		}
		return strblr.toString();
	}

	public String getMimeType() {
		return mimeType;
	}

	@XmlAttribute(name = "mimeType")
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getContentType() {
		return contentType;
	}

	@XmlAttribute(name = "contentType")
	public void setContentTypeType(String contentType) {
		this.contentType = contentType;
	}

	public List<RepresentationPR> getRepresentation() {
		return representation;
	}

	@XmlElement(name = "Representation")
	public void setRepresentation(List<RepresentationPR> representation) {
		this.representation = representation;
	}

	public SegmentTemplatePR getSegmentTemplate() {
		return segmenttemplate;
	}

	@XmlElement(name = "SegmentTemplate")
	public void setSegmentTemplate(SegmentTemplatePR segmentTemplate) {
		this.segmenttemplate = segmentTemplate;
	}

	
}