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
package com.att.aro.core.video.pojo;

public enum VideoOption {
	
	LREZ(0, "0x0"), // temp bit rate and screen size values for now until upcoming change on how we capture LREZ videos on devices (SN 2016/10/24)
	HDEF(8000000, "1280x720"), 
	SDEF(3000000, "960x540"), 
	KITCAT_HDEF(8000000, "720x1280"),
	KITCAT_SDEF(3000000, "540x960"),
	NONE(0, "0x0"); 
	
	private int bitRate;
	private String screenSize;
	
	private VideoOption(int bitRate, String screenSize) {		
		this.bitRate = bitRate;
		this.screenSize = screenSize;
	}
	
	public int getBitRate() {
		return bitRate;
	}
	
	public String getScreenSize() {
		return screenSize;
	}
}
