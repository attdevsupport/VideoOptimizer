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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
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

public class VideoUsagePrefs {
	public static final String VIDEO_PREFERENCE = "VIDEO_PREFERENCE";
	private double startupDelay = 10.000D; // default startup delay
	private double maxBuffer = 100.0D; // MB
	private double stallTriggerTime = .05D; // in seconds
	private DUPLICATE_HANDLING duplicateHandling = DUPLICATE_HANDLING.HIGHEST;
	private boolean ffmpegConfirmationShowAgain = false;
	private double stallPausePoint = 0.0D;
	private double stallRecovery = 0.0D;
	private boolean startupDelayReminder = true;
	@Value("${preferences.video.defaultSegmentRedundancyWarnVal }")
	private int segmentRedundancyWarnVal;
	@Value("${preferences.video.defaultStartUpDelayWarnVal }")
	private String startUpDelayWarnVal;
	@Value("${preferences.video.defaultStallDurationWarnVal }")
	private String stallDurationWarnVal;
	@Value("${preferences.video.defaultStartUpDelayFailVal}")
	private String startUpDelayFailVal;
	@Value("${preferences.video.defaultStallDurationFailVal }")
	private String stallDurationFailVal;
	@Value("${preferences.video.defaultSegmentRedundancyFailVal }")
	private int segmentRedundancyFailVal;

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
		strblr.append(": duplicateHandling = ");
		strblr.append(getDuplicateHandling());
		strblr.append(", startupDelay = ");
		strblr.append(getStartupDelay());
		strblr.append(", maxBuffer = ");
		strblr.append(getMaxBuffer());
		strblr.append(", stallTriggerTime = ");
		strblr.append(getStallTriggerTime());
		strblr.append(", startUpDelayWarnVal = ");
		strblr.append(getStartUpDelayWarnVal());
		strblr.append(", startUpDelayFail = ");
		strblr.append(getStartUpDelayFailVal());
		strblr.append(", stallDurationWarnVal = ");
		strblr.append(getStallDurationWarnVal());
		strblr.append(", stallDurationFailVal = ");
		strblr.append(getStallDurationFailVal());
		strblr.append(", segmentRedundancyWarnVal = ");
		strblr.append(getSegmentRedundancyWarnVal());
		strblr.append(", segmentRedundancyFailVal = ");
		strblr.append(getSegmentRedundancyFailVal());
		return strblr.toString();
	}

	public double getStartupDelay() {
		return startupDelay;
	}

	public void setStartupDelay(double startupDelay) {
		this.startupDelay = startupDelay;
	}

	public DUPLICATE_HANDLING getDuplicateHandling() {
		return duplicateHandling;
	}

	public void setDuplicateHandling(DUPLICATE_HANDLING duplicateHandling) {
		this.duplicateHandling = duplicateHandling;
	}

	public static String getVideoPreference() {
		return VIDEO_PREFERENCE;
	}

	public double getMaxBuffer() {
		return maxBuffer;
	}

	public void setMaxBuffer(double maxBuffer) {
		this.maxBuffer = maxBuffer;
	}

	public double getStallTriggerTime() {
		return stallTriggerTime;
	}

	public void setStallTriggerTime(double stallTriggerTime) {
		this.stallTriggerTime = stallTriggerTime;
	}

	public boolean isFfmpegConfirmationShowAgain() {
		return ffmpegConfirmationShowAgain;
	}

	public void setFfmpegConfirmationShowAgain(boolean ffmpegConfirmationShowAgain) {
		this.ffmpegConfirmationShowAgain = ffmpegConfirmationShowAgain;
	}

	public double getStallPausePoint() {
		return stallPausePoint;
	}

	public void setStallPausePoint(double stallPausePoint) {
		this.stallPausePoint = stallPausePoint;
	}

	public double getStallRecovery() {
		return stallRecovery;
	}

	public void setStallRecovery(double stallRecovery) {
		this.stallRecovery = stallRecovery;
	}

	public boolean isStartupDelayReminder() {
		return startupDelayReminder;
	}

	public void setStartupDelayReminder(boolean startupDelayReminder) {
		this.startupDelayReminder = startupDelayReminder;
	}

	public String getStartUpDelayWarnVal() {
		return startUpDelayWarnVal;
	}

	public void setStartUpDelayWarnVal(String startUpDelayWarnVal) {
		this.startUpDelayWarnVal = startUpDelayWarnVal;
	}

	public String getStartUpDelayFailVal() {
		return startUpDelayFailVal;
	}

	public void setStartUpDelayFailVal(String startUpDelayFailVal) {
		this.startUpDelayFailVal = startUpDelayFailVal;
	}

	public int getSegmentRedundancyWarnVal() {
		return segmentRedundancyWarnVal;
	}

	public void setSegmentRedundancyWarnVal(int segmentRedundancyWarnVal) {
		this.segmentRedundancyWarnVal = segmentRedundancyWarnVal;
	}

	public int getSegmentRedundancyFailVal() {
		return segmentRedundancyFailVal;
	}

	public void setSegmentRedundancyFailVal(int segmentRedundancyFailVal) {
		this.segmentRedundancyFailVal = segmentRedundancyFailVal;
	}

	public String getStallDurationWarnVal() {
		return stallDurationWarnVal;
	}

	public void setStallDurationWarnVal(String stallDurationWarnVal) {
		this.stallDurationWarnVal = stallDurationWarnVal;
	}

	public String getStallDurationFailVal() {
		return stallDurationFailVal;
	}

	public void setStallDurationFailVal(String stallDurationFailVal) {
		this.stallDurationFailVal = stallDurationFailVal;
	}
}
