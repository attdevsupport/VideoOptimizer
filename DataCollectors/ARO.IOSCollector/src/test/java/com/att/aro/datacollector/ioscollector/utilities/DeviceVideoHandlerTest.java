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

import org.junit.Test;

import com.att.aro.core.util.Util;

public class DeviceVideoHandlerTest {

	@Test
	public void testExecuteCmd_mount_iphonXR() throws Exception {
		String udId = "00008020-001418213C91002E";
		String datadir = "/Users/barrynelson/VideoOptimizerTraceIOS/test2";
		String mountPoint = datadir + Util.FILE_SEPARATOR + "mountPoint";
		String cmd = String.format("%s -d -u %s", Util.getIfuse(), udId , mountPoint);
		String result = DeviceVideoHandler.getInstance().executeCmd(cmd);
		System.out.println(">>" + result);
	}

	@Test
	public void testExecuteCmd_mount_iphon5s() throws Exception {
		String udId = "6ea7ad28b9bf9662f01914aa99122ba5d113205d";
		String datadir = "/Users/barrynelson/VideoOptimizerTraceIOS/test2";
		String mountPoint = datadir + Util.FILE_SEPARATOR + "mountPoint";
		
		String cmd = String.format("%s -d -u %s", Util.getIfuse(), udId , mountPoint);
		String result = DeviceVideoHandler.getInstance().executeCmd(cmd);
		System.out.println(">>" + result);
	}
	
}
