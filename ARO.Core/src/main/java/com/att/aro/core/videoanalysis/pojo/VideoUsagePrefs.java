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
package com.att.aro.core.videoanalysis.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;

/**
 * <pre>
 * Stores: startupDelay The delay from first arriving segment to the start of
 * play maxBuffer The targeted max value for the buffer, used to compare with
 * actual buffer used stallTriggerTime Amount of time to allow for recovery
 * before calling a hard stall duplicateHandling Determines how to handle
 * duplicate(redundant) segments
 *
 * Note: arrivalToPlay is deprecated, need a clean way to clean out of stored
 * preferences
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true) // allows for changes dropping items or using older versions, but not before this ignore

@Data
public class VideoUsagePrefs {
	public static final String VIDEO_PREFERENCE = "VIDEO_PREFERENCE";
	private double startupDelay = 10.000D; // default startup delay
	private double maxBuffer = 100.0D; // MB
	private double stallTriggerTime = .05D; // in seconds
	private DUPLICATE_HANDLING duplicateHandling = DUPLICATE_HANDLING.HIGHEST;
	private boolean ffmpegConfirmationShowAgain = false;
	private double stallPausePoint = 0.0D;
	private double stallRecovery = 0.0D;
	private double nearStall = 0.01D;
	@Value("${preferences.video.defaultSegmentRedundancyWarnVal : 15 }")
	private double segmentRedundancyWarnVal;
	@Value("${preferences.video.defaultStartUpDelayWarnVal : 8.0000 }")
	private double startUpDelayWarnVal;
	@Value("${preferences.video.defaultStallDurationWarnVal : 0.5000 }")
	private double stallDurationWarnVal;
	@Value("${preferences.video.defaultStallDurationFailVal : 1.0000 }")
	private double stallDurationFailVal;
	@Value("${preferences.video.defaultSegmentRedundancyFailVal : 25 }")
	private double segmentRedundancyFailVal;

	/**
	 * Determine which segment should be used when there are duplicate segments
	 *
	 */
	public static enum DUPLICATE_HANDLING {
		FIRST // to arrive
		, LAST // to arrive
		, HIGHEST // quality
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(VIDEO_PREFERENCE);
		strblr.append("\n\t duplicateHandling = ")		.append(getDuplicateHandling());
		strblr.append("\n\t startupDelay = ")			.append(getStartupDelay());
		strblr.append("\n\t maxBuffer = ")				.append(getMaxBuffer());
		strblr.append("\n\t stallTriggerTime = ")		.append(getStallTriggerTime());
		strblr.append("\n\t startUpDelayWarnVal = ")	.append(getStartUpDelayWarnVal());
		strblr.append("\n\t stallDurationWarnVal = ")	.append(getStallDurationWarnVal());
		strblr.append("\n\t stallDurationFailVal = ")	.append(getStallDurationFailVal());
		strblr.append("\n\t segmentRedundancyWarnVal = ").append(getSegmentRedundancyWarnVal());
		strblr.append("\n\t segmentRedundancyFailVal = ").append(getSegmentRedundancyFailVal());
		return strblr.toString();
	}

	public static String getVideoPreference() {
		return VIDEO_PREFERENCE;
	}

	
}
