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
package com.att.aro.core.packetanalysis.pojo;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoStall {


	private double duration;
	private double stallStartTimeStamp;
	private double stallEndTimeStamp;
	private VideoEvent segmentTryingToPlay;
	
	//private double stallTriggerTime;
	
	public VideoStall(double stallStartTimeStamp){
		this.stallStartTimeStamp = stallStartTimeStamp;
	//	this.stallEndTimeStamp = duration + stallStartTimeStamp;
	}

	public double getDuration() {
		return duration;
	}

	public double getStallStartTimeStamp() {
		return stallStartTimeStamp;
	}

	public double getStallEndTimeStamp() {
		return stallEndTimeStamp;
	}
	
	public void setStallEndTimeStamp(double stallEndTimeStamp){
		this.stallEndTimeStamp = stallEndTimeStamp;
		duration = this.stallEndTimeStamp - this.stallStartTimeStamp;
	}

	public void setSegmentTryingToPlay(VideoEvent chunkPlaying) {
		// TODO Auto-generated method stub
		this.segmentTryingToPlay = chunkPlaying;
	}

	public VideoEvent getSegmentTryingToPlay() {
		return segmentTryingToPlay;
	}
	
	/*public double getStallTriggerTime(){
		return this.stallTriggerTime;
	}*/
}
