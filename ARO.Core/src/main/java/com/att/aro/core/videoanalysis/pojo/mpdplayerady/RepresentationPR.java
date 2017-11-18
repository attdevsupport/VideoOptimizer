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

@XmlRootElement(name = "Representation")
public class RepresentationPR {

	String bandwidth = "";
	String width = "";
	String height = "";
	String frameRate = "";
	String codecs = "";
	String scanType = "";
	String contentID = "";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		if (! contentID.isEmpty())        {strblr.append(" id:");           strblr.append(contentID);                                  }      
		if (! bandwidth.isEmpty()) {strblr.append(", bandwidth:");   strblr.append(String.format("%8s", bandwidth));     }      
		if (! width.isEmpty())     {strblr.append(", width:");       strblr.append(String.format("%4s", width));         }      
		if (! height.isEmpty())    {strblr.append(", height:");      strblr.append(String.format("%4s", height));        }      
		if (! codecs.isEmpty())    {strblr.append(", codecs:");      strblr.append(codecs);                              }      
		
		return strblr.toString();
	}

	@XmlAttribute
	public void setWidth(String width) {
		this.width = width;
	}

	@XmlAttribute
	public void setBandwidth(String bandwidth) {
		this.bandwidth = bandwidth;
	}

	
	@XmlAttribute
	public void setHeight(String height) {
		this.height = height;
	}

	@XmlAttribute
	public void setFrameRate(String frameRate) {
		this.frameRate = frameRate;
	}

	@XmlAttribute
	public void setCodecs(String codecs) {
		this.codecs = codecs;
	}

	@XmlAttribute
	public void setScanType(String scanType) {
		this.scanType = scanType;
	}

	@XmlAttribute(name = "id")
	public void setId(String contentID) {
		this.contentID = contentID;
	}

	public String getBandwidth() {
		return bandwidth;
	}

	public String getWidth() {
		return width;
	}

	public String getHeight() {
		return height;
	}

	public String getFrameRate() {
		return frameRate;
	}

	public String getCodecs() {
		return codecs;
	}

	public String getScanType() {
		return scanType;
	}

	public String getId() {
		return contentID;
	}


}