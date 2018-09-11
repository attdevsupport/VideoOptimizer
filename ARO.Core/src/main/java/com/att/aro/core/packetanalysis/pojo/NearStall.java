
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

public class NearStall{

	private double nearStallTimeStamp;
	private VideoEvent nearlyStalledSegment;
	
	public NearStall(double nearStallTimeStamp, VideoEvent nearlyStalledSegment) {
		this.setNearStallTimeStamp(nearStallTimeStamp);
		this.setNearlyStalledSegment(nearlyStalledSegment);
	}

	public VideoEvent getNearlyStalledSegment() {
		return nearlyStalledSegment;
	}

	public void setNearlyStalledSegment(VideoEvent nearlyStalledSegment) {
		this.nearlyStalledSegment = nearlyStalledSegment;
	}

	public double getNearStallTimeStamp() {
		return nearStallTimeStamp;
	}

	public void setNearStallTimeStamp(double nearStallTimeStamp) {
		this.nearStallTimeStamp = nearStallTimeStamp;
	}

}

