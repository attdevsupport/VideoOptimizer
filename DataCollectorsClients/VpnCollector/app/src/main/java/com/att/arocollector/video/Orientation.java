/*
 *  Copyright 2014 AT&T
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

public enum Orientation {
	PORTRAIT, LANDSCAPE;
	
	/**
	 * Gets the enum where its name equals to the String argument.
	 * 
	 * @param orientation expecting "landscape" or "portrait"
	 * @return
	 */
	public static Orientation getOrientation(String orientation) {
		
		for (Orientation orient: Orientation.values()) {
			if (orientation.equals(orient.name())) {
				return orient;
			}
		}
		return null;						
	}
}
