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

import lombok.Data;

/**
 * POJO for speed throttle record
 * format Duration, UL_speed, DL_speed, (future) , (future),......
 */

@Data
public class SpeedThrottleAdapter {

	private int throttleDL;
	private int throttleUL;
	private int delayDL;
	private int timeDuration;

	public SpeedThrottleAdapter() {}

	public SpeedThrottleAdapter(int timeDuration, int throttleDL, int throttleUL, int delayDL){
		this.timeDuration = timeDuration;
		this.throttleDL = throttleDL;
		this.throttleUL = throttleUL;
		this.delayDL = delayDL;
	}
}
