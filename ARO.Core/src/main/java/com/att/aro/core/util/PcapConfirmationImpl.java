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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.util;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;

public class PcapConfirmationImpl {

	@Autowired
	private IExternalProcessRunner extProcessRunner;

	@Autowired
	private IStringParse stringParse;

	private String result = "";

	public boolean checkPcapVersion() {

		String cmd = "tcpdump --version";
		result = extProcessRunner.executeCmd(cmd);

		stringParse.parse(result, "version");
		String version = StringParse.findLabeledDataFromString("libpcap version", " ", result);
		if (version == null || version.isEmpty()) {
			return false;
		}
		String[] least = { "1", "8", "0" };
		if (Util.isMacOS()) {
			String[] points = version.split("\\.");
			for (int idx = 0; idx < least.length; idx++) {
				if (idx == points.length) {
					break;
				}
				if (points[idx].compareTo(least[idx]) < 0) {
					return false;
				}
			}
		}
		return true;
	}

	public String getResult() {
		return result;
	}

}
