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
package com.att.aro.core.video;

import java.util.Date;

import com.att.aro.core.datacollector.IVideoImageSubscriber;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.video.pojo.VideoOption;

public interface IScreenRecorder extends Runnable {

	public enum State{
		Initialized
		, Recording
		, Stopping
		, Pulling
		, PullComplete
		, Compile
		, Done
		, Error
		, Undefined
	}

	State init(IAroDevice aroDevice, String localTraceFolder, VideoOption videoOption);

	/**
	 * add client who wants to get video frame
	 * @param vImageSubscriber
	 */
	void addSubscriber(IVideoImageSubscriber vImageSubscriber);

	/**
	 * Stops the process of capturing images.
	 */
	boolean stopRecording();
	
	/**
	 * Gets the start time of the video capture.
	 * @return
	 */
	Date getVideoStartTime();
	
	/**
	 * Returns the status of Video Capture
	 * @return true if capture is active, false if not active
	 */
	boolean isVideoCaptureActive();

	State getStatus();

	void setLocalTraceFolder(String localTraceFolder);
	void compileVideo() throws Exception;

	boolean pullVideos() throws Exception;

	/**
	 * Delete the entire vids folder that contains all of the video segments
	 * @return true if success
	 */
	boolean deleteSrcVideos();

}
