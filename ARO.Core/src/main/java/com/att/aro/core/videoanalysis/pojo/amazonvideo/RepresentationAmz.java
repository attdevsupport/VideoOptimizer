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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Representation")
public class RepresentationAmz {

	String bandwidth = "";
	String url = "";
	String width = "";
	String height = "";
	EncodedSegmentListAmz encodedSegment;
	SegmentListAmz segmentList;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append(" bandwidth:");
		strblr.append(bandwidth);
		strblr.append(", url:");
		strblr.append(url);
		if (encodedSegment != null) {
			strblr.append("\n\t\t\t\tEncodedSegmentListAmz :");
			strblr.append(encodedSegment);
		}
		if (segmentList != null) {
			strblr.append("\n\t\t\t\tSegmentListAmz :");
			strblr.append(segmentList);
		}
		return strblr.toString();
	}

	public String getUrl() {
		return url;
	}

	@XmlElement(name = "BaseURL")
	public void setUrl(String url) {
		this.url = url;
	}

	public String getBandwidth() {
		return bandwidth;
	}

	@XmlAttribute
	public void setWidth(String width) {
		this.width = width;
	}

	public String getWidth() {
		return width;
	}

	@XmlAttribute
	public void setHeight(String height) {
		this.height = height;
	}

	public String getHeight() {
		return height;
	}

	@XmlAttribute
	public void setBandwidth(String bandwidth) {
		this.bandwidth = bandwidth;
	}

	public EncodedSegmentListAmz getEncodedSegment() {
		return encodedSegment;
	}

	@XmlElement(name = "EncodedSegmentList")
	public void setEncodedSegment(EncodedSegmentListAmz encodedSegment) {
		this.encodedSegment = encodedSegment;
	}

	@XmlElement(name = "SegmentList")
	public SegmentListAmz getSegmentList() {
		return segmentList;
	}

	public void setSegmentList(SegmentListAmz segmentList) {
		this.segmentList = segmentList;
	}


}