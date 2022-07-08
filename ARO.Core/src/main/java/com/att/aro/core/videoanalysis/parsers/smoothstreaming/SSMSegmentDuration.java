/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis.parsers.smoothstreaming;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * <c d="20020000" t="0"></c> <c d="20020000"></c> <c d="20020000"></c> ... <c d="20020000"></c> <c d="20020000"></c> <c d="19185833"></c>
 * 
 *
 */
public class SSMSegmentDuration {

	String d;
	String t;

	@Override
	public String toString() {
		return d;
	}

	@XmlAttribute(name = "d")
	public String getD() {
		return d;
	}

	@XmlAttribute(name = "t")
	public String getT() {
		return t;
	}

	public void setD(String d) {
		this.d = d;
	}

	public void setT(String t) {
		this.t = t;
	}

}
