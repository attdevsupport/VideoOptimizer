/*
 *  Copyright 2021 AT&T
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

import java.util.Arrays;
import java.util.HashMap;

public enum ThermalStatus {

	THROTTLING_NONE(0), THROTTLING_LIGHT(1), THROTTLING_MODERATE(2), THROTTLING_SEVERE(3), THROTTLING_CRITICAL(4),
	THROTTLING_EMERGENCY(5), THROTTLING_SHUTDOWN(6), UNKNOWN(-1);

	int code;

	private static HashMap<Integer, ThermalStatus> enumByCode = new HashMap<>();
	static {
		Arrays.stream(values()).forEach(e -> enumByCode.put(e.getCode(), e));
	}

	public static ThermalStatus getByCode(int code) {
		return enumByCode.getOrDefault(code, UNKNOWN);
	}

	ThermalStatus(int code) {
		this.code = code;
	}

	public Integer getCode() {
		return code;
	}

}
