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
package com.att.aro.core.android;

/*
 * References: 
 * https://source.android.com/source/build-numbers.html
 * https://en.wikipedia.org/wiki/Android_version_history
 */

public enum AndroidApiLevel {

	K14 ("ICS", 14, "4.0"),
	K19 ("Kitkat", 19, "4.4","4.4.1","4.4.2","4.4.3","4.4.4"),
	L21 ("Lollipop", 21, "5.0","5.0.1","5.0.2"),
	L22 ("Lollipop", 22, "5.1","5.1.1"),
	M23 ("Marshmallow", 23, "6.0","6.0.1"),
	N24 ("Marshmallow", 24, "7.0"),
	N25 ("Nougat", 25, "7.1");
	
	private String codeName;
	private int levelNumber;
	private String[] versions;
	
	private AndroidApiLevel(String codeName, int levelNumber, String... versions) {
		this.codeName = codeName;
		this.levelNumber = levelNumber;
		this.versions = versions;
	}
	
	public String codeName() {
		return codeName;
	}
	
	public int levelNumber() {
		return levelNumber;
	}
	
	public String[] versions() {
		return versions;
	}
}
