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
package com.att.aro.core.videoanalysis.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QualityTime{
	
	private String name = "";
	private int count;
	private int track;
	private double duration;
	private double percentage;
	private double resolution;
	private double segmentPosition; // FKA StartTime - this is the time position relative to the start of a video stream, not the trace
	private double bitrateDeclared;
	private double bitrateAverage;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("QualityTime:").append(name)
			.append("\n track: ").append(track)
			.append("\n count: ").append(count)
			.append("\n resolution:").append(resolution)
			.append("\n percentage:").append(String.format("%.3f", percentage))
			.append("\n duration:").append(String.format("%.3f", duration))
			.append("\n duration:").append(String.format("%.6f", segmentPosition))
			.append("\n bitrateDeclared:").append(String.format("%.3f", bitrateDeclared))
			.append("\n bitrateAverage:").append(String.format("%.3f", bitrateAverage));
		return strblr.toString();
	}

}
