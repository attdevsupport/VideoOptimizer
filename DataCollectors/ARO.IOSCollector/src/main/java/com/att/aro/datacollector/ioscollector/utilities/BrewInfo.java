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
package com.att.aro.datacollector.ioscollector.utilities;

import java.util.ResourceBundle;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;

public class BrewInfo {
	private static final Logger LOG = LogManager.getLogger(BrewInfo.class);
	private IExternalProcessRunner runner = null;

	public void setRunner(IExternalProcessRunner runner) {
		this.runner = runner;
	}

	public BrewInfo() {
		runner = new ExternalProcessRunnerImpl();
	}

	public boolean isBrewUpToDate(String localBrewVersion, String targetBrewVersion) {
		boolean flag = false;

		VersionComparator local = new VersionComparator();

		if (local.compare(localBrewVersion, targetBrewVersion) < 0) {
			LOG.error("Please update the version because the latest is " + localBrewVersion + "target: "
					+ targetBrewVersion);
		} else {
			LOG.debug("your is up to date:  " + localBrewVersion + "target: " + targetBrewVersion);
			flag = true;
		}
		return flag;
	}

	public String getLocalBrewVersion() {
		String brewReturn = runner.executeCmdRunner("brew --version | sed 1q | tr -d 'Homebrew '", true, "success",
				true, true);
		if (!brewReturn.isEmpty()) {
			try {
				if (brewReturn.contains("-")) {
					brewReturn = brewReturn.substring(0, brewReturn.indexOf("-"));
				}
			} catch (NumberFormatException e) {
				LOG.error("Non numeric value cannot represent ios version: " + brewReturn);
			}
		}
		return brewReturn;

	}

	public String getSuggestBrewVersion() {
		return ResourceBundle.getBundle("build").getString("homebrew.version");
	}

}
