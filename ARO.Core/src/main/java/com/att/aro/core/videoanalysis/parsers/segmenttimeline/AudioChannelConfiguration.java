/*

 *  Copyright 2020 AT&T
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
@XmlRootElement(name = "AudioChannelConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class AudioChannelConfiguration {
	
	@XmlAttribute private String schemeIdUri = "";
	@XmlAttribute private String value = "";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(167)
		.append("\n\t\t\tAudioChannelConfiguration")
		.append("\n\t\t\t\tschemeIdUri:              ").append(schemeIdUri)
		.append("\n\t\t\t\tvalue: ").append(value)
		.append("\n\t\t\t");
		return strblr.toString();
	}	
}
