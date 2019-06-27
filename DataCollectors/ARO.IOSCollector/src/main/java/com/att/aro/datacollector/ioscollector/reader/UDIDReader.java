/*
 *  Copyright 2012 AT&T
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

import java.io.IOException;

import org.springframework.context.ApplicationContext;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.commandline.IExternalProcessRunner;

/**
 * Execute Mac command to get Serial Number of connected iPhone/iPad/iPod.
 * Obviously, this class pertains only to Mac OSX.
 *
 */
public class UDIDReader {

	private IExternalProcessRunner runner;

	public UDIDReader() {
		ApplicationContext context = SpringContextUtil.getInstance().getContext();
		runner = context.getBean(IExternalProcessRunner.class);
	}

	/**<pre>
	 * Get Serial Number or UDID or ECID of IOS device connected to Mac OS machine. 
	 * UDID codes are 40 characters long, ECID codes are shorter and contain a hyphen.
	 */
	public String getSerialNumber() throws IOException {
		return runner.executeCmd("idevice_id -l");
	}
}
