/*
 *  Copyright 2014, 2021 AT&T
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
package com.att.aro.core.packetanalysis.pojo;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates a specific time range of trace data for analysis.
 * This can be stored in a time-range.json, when created by TimeRangeEditorDialog, in File menu
 * 
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeRange implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static enum TimeRangeType {
		DEFAULT
		,MANUAL
		,FULL
	}

	private String title;
	private TimeRangeType timeRangeType = TimeRangeType.DEFAULT;
	private Double beginTime;
	private Double endTime;
	
	@JsonIgnoreProperties(value = { "path" })
	private String path;
	 
	
	/**
	 * Initializes an instance of the TimeRange class, using the specified beginning and ending times.
	 * @param beginTime - The beginning of the time range.
	 * @param endTime - The ending of the time range.
	 */
	public TimeRange(double beginTime, double endTime) {
		this.beginTime = beginTime;
		this.endTime = endTime;
		timeRangeType = TimeRangeType.MANUAL;
	}

	public String getRange() {
		String result = null;
		if (beginTime != null && endTime != null) {
			if (timeRangeType.equals(TimeRangeType.FULL)) {
				result = String.format("%s [%.3f - %.3f]", "FULL", beginTime, endTime);
			} else if (title != null){
				result = String.format("%s [%.3f - %.3f]",  title, beginTime, endTime);
			} else {
				result = "";
			}
		}
		return result;
	}

	public TimeRange(String title, TimeRangeType timeRangeType, double beginTime, double endTime) {
		this.title = title;
		this.timeRangeType = timeRangeType;
		this.beginTime = beginTime;
		this.endTime = endTime;
	}
}
