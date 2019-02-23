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
	private double nearStall = 0.01D;
	@Value("${preferences.video.defaultSegmentRedundancyWarnVal : 15 }")
	private int segmentRedundancyWarnVal;
	@Value("${preferences.video.defaultStartUpDelayWarnVal : 2.0000 }")
	private String startUpDelayWarnVal;
	@Value("${preferences.video.defaultStallDurationWarnVal : 0.5000 }")
	private String stallDurationWarnVal;
	@Value("${preferences.video.defaultStartUpDelayFailVal : 3.0000}")
	private String startUpDelayFailVal;
	@Value("${preferences.video.defaultStallDurationFailVal : 1.0000 }")
	private String stallDurationFailVal;
	@Value("${preferences.video.defaultSegmentRedundancyFailVal : 25 }")
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
		strblr.append(": duplicateHandling = ")		.append(getDuplicateHandling());
		strblr.append(", startupDelay = ")			.append(getStartupDelay());
		strblr.append(", maxBuffer = ")				.append(getMaxBuffer());
		strblr.append(", stallTriggerTime = ")		.append(getStallTriggerTime());
		strblr.append(", startUpDelayWarnVal = ")	.append(getStartUpDelayWarnVal());
		strblr.append(", startUpDelayFail = ")		.append(getStartUpDelayFailVal());
		strblr.append(", stallDurationWarnVal = ")	.append(getStallDurationWarnVal());
		strblr.append(", stallDurationFailVal = ")	.append(getStallDurationFailVal());
		strblr.append(", segmentRedundancyWarnVal = ").append(getSegmentRedundancyWarnVal());
		strblr.append(", segmentRedundancyFailVal = ").append(getSegmentRedundancyFailVal());
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

	public double getNearStall() {
		return nearStall;
	}

	public void setNearStall(double nearStall) {
		this.nearStall = nearStall;
	}
}
