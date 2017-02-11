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
package com.att.aro.core.videoanalysis.pojo.huluvideo;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Representation")
@XmlType(propOrder = { "baseURL" })
public class Representation2 {

	String bandwidth;
	String codecs;
	String width;
	String height;

	String baseURL;

	public String getBandwidth() {
		return bandwidth;
	}

	@XmlAttribute(name = "bandwidth")
	public void setBandwidth(String bandwidth) {
		this.bandwidth = bandwidth;
	}

	public String getCodecs() {
		return codecs;
	}

	@XmlAttribute(name = "codecs")
	public void setCodecs(String codecs) {
		this.codecs = codecs;
	}

	public String getWidth() {
		return width;
	}

	@XmlAttribute(name = "width")
	public void setWidth(String width) {
		this.width = width;
	}

	public String getHeight() {
		return height;
	}

	@XmlAttribute(name = "height")
	public void setHeight(String height) {
		this.height = height;
	}

	@XmlElement(name = "BaseURL")
	public String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

}