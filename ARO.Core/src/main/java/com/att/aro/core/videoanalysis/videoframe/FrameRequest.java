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
package com.att.aro.core.videoanalysis.videoframe;

import lombok.Data;

@Data
public class FrameRequest {
	
	public FrameRequest(JobType jobType, double startTimeStamp, int count, Double targetFrame, FrameReceiver frameReceiver) {
		this.jobType = jobType;
		this.startTimeStamp = startTimeStamp;
		this.count = count;
		this.targetFrame = targetFrame;
		this.frameReceiver = frameReceiver;
		this.tryCount = 0;
	}

	public enum JobType {
		PRELOAD,
		COLLECT_FRAMES,
		DROP
	}
	
	private int tryCount;
	private double startTimeStamp;
	private Double targetFrame;
	private JobType jobType;
	private Integer count;
	private FrameReceiver frameReceiver;
}
