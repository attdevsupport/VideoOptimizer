/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.peripheral.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Store Startup Delay
 * Date: October 10, 2018, Feb 19, 2021 (expanded to support multiple manifests)
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) 
public class VideoStreamStartup {
	private String manifestName;
	private double manifestReqTime;
	private double firstSegID;
	private double playRequestedTime;
	private double startupDelay;
	private UserEvent userEvent;

	public VideoStreamStartup(String videoName) {
		setManifestName(videoName);
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\n\tVideoStartup :");
		strblr.append("\t" + (manifestName != null ? manifestName : ""))
		.append("\n\t\tRequest Time:    \t").append(String.format("%.3f", manifestReqTime))
		.append("\n\t\tSegment:         \t").append(firstSegID)
		.append("\n\t\tStartupDelay:    \t").append(String.format("%.3f", startupDelay))
		.append("\n\t\tplayRequestedTime:\t").append(String.format("%.3f", playRequestedTime))
		.append("\n\t\tuserEvent         \t")
		.append(userEvent == null ? "" : String.format("%s: press: %.3f,  release %.3f", userEvent.getEventType(), userEvent.getPressTime(), userEvent.getReleaseTime()))
		;
		return strblr.toString();
	}
}
