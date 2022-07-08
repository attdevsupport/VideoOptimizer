/*
 *  Copyright 2013 AT&T
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
package com.att.aro.datacollector.ioscollector.reader;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;

public class PcapHelper {
	IExternalProcessRunner extRunner;
	private static final Logger LOG = LogManager.getLogger(PcapHelper.class);

	public PcapHelper() {
		extRunner = new ExternalProcessRunnerImpl();
	}

	public PcapHelper(IExternalProcessRunner extRunner) {
		this.extRunner = extRunner;
	}

	/**
	 * extract date of the first packet received in the pcap/cap file. For Mac
	 * OS only.
	 * 
	 * @param filepath
	 *            full path to the packet file pcap/cap
	 * @return instance of Date if available or null
	 */
	public Date getFirstPacketDate(String filepath) {
		// read first entry out of traffic file
		String data = extRunner.executeCmd(String.format("%s -r %s -c 1 -t e |awk -F ' '  '{print $2}'", Util.getTshark(), filepath));
		if (!StringUtils.isEmpty(data)) {
			LOG.info("found packet date string: " + data);
			return new Date((long) (StringParse.stringToDouble(data, 0) * 1000));
		}
		return null;
	}
}
