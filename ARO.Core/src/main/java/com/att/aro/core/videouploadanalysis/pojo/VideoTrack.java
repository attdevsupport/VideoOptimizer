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
package com.att.aro.core.videouploadanalysis.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VideoTrack {
	private String type;
	private String trk;
	private String utc;

	@JsonProperty("Type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@JsonProperty("Trk")
	public String getTrk() {
		return trk;
	}

	public void setTrk(String trk) {
		this.trk = trk;
	}

	@JsonProperty("UTC")
	public String getUtc() {
		return utc;
	}

	public void setUtc(String utc) {
		this.utc = utc;
	}

	public String toString() {
		return "Type: " + getType() + ", Trk: " + getTrk() + ", UTC: " + getUtc();
	}
}
