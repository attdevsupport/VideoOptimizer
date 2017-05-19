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

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.att.aro.core.videoanalysis.pojo.URLBase;


@XmlRootElement(name="Representation")
public class Representation {

   
    String bandwidth;
    List<URLBase> url;

	@XmlElement(name = "URL")
	public List<URLBase> getUrl() {
		return url;
	}

	public void setUrl(List<URLBase> url) {
		this.url = url;
	}
    


	public String getBandwidth() {
		return bandwidth;
	}

	@XmlAttribute
	public void setBandwidth(String bandwidth) {
		this.bandwidth = bandwidth;
	}


	

	
    
}