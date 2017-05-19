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

/**
 * <pre>
 * Stores:
 *   startupDelay      The delay from first arriving segment to the start of play
 *   maxBuffer         The targeted max value for the buffer, used to compare with actual buffer used
 *   stallTriggerTime  Amount of time to allow for recovery before calling a hard stall
 *   duplicateHandling Determines how to handle duplicate(redundant) segments
 * 
 * Note: arrivalToPlay is deprecated, need a clean way to clean out of stored preferences
 *
 */
public class VideoUsagePrefs {

	public static final String VIDEO_PREFERENCE = "VIDEO_PREFERENCE";

	private double startupDelay = 10.000D;		// default startup delay
	private double maxBuffer = 100.0D;		// MB
	private double stallTriggerTime = .05D; // in seconds
	private DUPLICATE_HANDLING duplicateHandling = DUPLICATE_HANDLING.HIGHEST;
	
	private boolean ffmpegConfirmationShowAgain=false;
	
	@Deprecated	
	private double arrivalToPlay = 0.0D;	// reject if segment arrives within VIDEO_ARRIVAL_TO_PLAY of playing the segment

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
		strblr.append(": duplicateHandling = ");	strblr.append(getDuplicateHandling());
		strblr.append(", startupDelay = ");		strblr.append(getStartupDelay());
		strblr.append(", maxBuffer = ");		strblr.append(getMaxBuffer());
		strblr.append(", stallTriggerTime = ");	strblr.append(getStallTriggerTime());
//		strblr.append(", arrivalToPlay = ");	strblr.append(getArrivalToPlay());
		return strblr.toString();
	}

	public double getStartupDelay() {
		return startupDelay;
	}

	public void setStartupDelay(double startupDelay) {
		this.startupDelay = startupDelay;
	}

	@Deprecated
	public double getArrivalToPlay() {
		return arrivalToPlay;
	}

	@Deprecated
	public void setArrivalToPlay(double arrivalToPlay) {
		this.arrivalToPlay = arrivalToPlay * 0; // mult by zero to nullify this feature
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
}
