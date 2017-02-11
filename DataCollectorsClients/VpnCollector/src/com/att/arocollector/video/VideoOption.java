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
package com.att.arocollector.video;

/*
 * Same enum as found in Core, but keeping
 * bit rate and screen size in Core's for 
 * the purpose of configurable numbers being 
 * kept in a single location.
 */
public enum VideoOption {
	HDEF, SDEF, LREZ, NONE;
	
	/**
	 * Gets the enum where its name equals to the String argument.
	 * 
	 * @param videoOptionStr
	 * @return
	 */
	public static VideoOption getVideoOption(String str) {
		
		for (VideoOption videoOption: VideoOption.values()) {
			if (str.equals(videoOption.name())) {
				return videoOption;
			}
		}
		return null;						
	}
}
