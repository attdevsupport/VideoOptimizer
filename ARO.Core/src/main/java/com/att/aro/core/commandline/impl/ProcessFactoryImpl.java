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
package com.att.aro.core.commandline.impl;

import java.io.File;
import java.io.IOException;

import com.att.aro.core.commandline.IProcessFactory;

public class ProcessFactoryImpl implements IProcessFactory {

	@Override
	public Process create(String cmd) throws IOException {
		return Runtime.getRuntime().exec(cmd);
	}

	@Override
	@Deprecated
	public Process create(String command, String directory) throws IOException {
		return Runtime.getRuntime().exec(command, null, new File(directory));
	}

	@Override
	@Deprecated
	public Process create(String[] commands) throws IOException {
		return Runtime.getRuntime().exec(commands);
	}
}
