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


import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import com.att.aro.core.commandline.IExternalProcessRunner;


public class TSharkConfirmationImpl {
	private static final Logger LOG = LogManager.getLogger(TSharkConfirmationImpl.class);

	private IExternalProcessRunner extProcessRunner;
	private String result = "";


	public TSharkConfirmationImpl(IExternalProcessRunner extProcessRunner) {
		this.extProcessRunner = extProcessRunner;
	}


	public boolean checkTsharkVersion() {
		String tsharkPath = Util.getTshark();
		String cmd = tsharkPath + " --version";
		LOG.info("Checking tshark with command: " + cmd);
		result = extProcessRunner.executeCmd(cmd);
		LOG.debug("tshark command result: " + result);

		if (StringUtils.isBlank(result) || !result.startsWith("TShark")) {
			return false;
		}

		String version = StringParse.findLabeledDataFromString("TShark (Wireshark)", " ", result);
		LOG.info("tshark version: " + version);
		if (version == null || version.isEmpty()) {
			return false;
		}

		return true;
	}

	public String getResult() {
		return result;
	}
}
