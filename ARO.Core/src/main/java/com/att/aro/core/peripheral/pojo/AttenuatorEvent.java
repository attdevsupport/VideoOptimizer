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
package com.att.aro.core.peripheral.pojo;

/*
 * POJO Class for Attenuation Event record
 */

public class AttenuatorEvent {

	private int delayTime ;

	private long timeStamp ;

	private AttnrEventFlow atnrFL ;

	public enum AttnrEventFlow {
		DL, UL
	}

	public AttenuatorEvent(AttnrEventFlow atnrFL, int delayTime, long timeStamp) {
		this.atnrFL = atnrFL;
		this.delayTime = delayTime;
		this.timeStamp = timeStamp;
	}

	public int getDelayTime() {
		return delayTime;
	}

	public AttnrEventFlow getAtnrFL() {
		return atnrFL;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

}
