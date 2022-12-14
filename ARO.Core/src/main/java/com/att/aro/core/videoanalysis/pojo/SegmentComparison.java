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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Table item information for comparing network throughput and segment information 
 * during downloading the video
 */
@Data
@AllArgsConstructor
public class SegmentComparison {

	private String name = "";
	private int count;
	private int track;
	private double declaredBandwidth;
	private List<Double> calculatedThroughputList;

	public double getCalculatedThroughput() {
		Double sum = 0.0;
		if (!calculatedThroughputList.isEmpty()) {
			for (Double throughtput : calculatedThroughputList) {
				sum += throughtput;
			}
			double size = calculatedThroughputList.size();
			return sum.doubleValue() / size / 1000.0;
		}
		return sum;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder();
		strblr.append("QualityTime:").append(name).append("\n track: ").append(track).append("\n bitrateDeclared:")
				.append(String.format("%.3f", declaredBandwidth)).append("\n calculate network bitrate:")
				.append(String.format("%.3f", getCalculatedThroughput()));
		return strblr.toString();

	}
}
