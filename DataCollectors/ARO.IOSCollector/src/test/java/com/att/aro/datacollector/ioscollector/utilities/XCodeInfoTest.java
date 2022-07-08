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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.datacollector.ioscollector.utilities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.att.aro.core.util.Util;

public class XCodeInfoTest {

	@Test
	public void testIsRVIAvailableString() throws Exception {
		XCodeInfo xcode = new XCodeInfo();
		if ("Mac OS X".equals(Util.OS_NAME)) {
			assertThat(xcode.isRVIAvailable()).isTrue();
		}
	}

	@Test
	public void testFindPath_when_installed() throws Exception {
		XCodeInfo xcode = new XCodeInfo();
		String path = xcode.getPath();
		System.out.println(Util.OS_NAME + Util.OS_VERSION);
		if ("Mac OS X".equals(Util.OS_NAME)) {
			if (Util.OS_VERSION == "10.14.6") {
				assertThat(path).isEqualTo("/usr/bin/rvictl");
			} else {
				assertThat(path).isEqualTo("/Library/Apple/usr/bin/rvictl");
			}
		}

	}

}
