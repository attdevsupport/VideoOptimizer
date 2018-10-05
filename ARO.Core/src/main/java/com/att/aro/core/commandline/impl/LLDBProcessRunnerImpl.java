
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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.ILLDBProcessRunner;
import com.att.aro.core.commandline.IProcessFactory;

public class LLDBProcessRunnerImpl implements ILLDBProcessRunner{

	IProcessFactory procfactory;
	private Process lldbProcess;

	private static final Logger LOG = LogManager.getLogger(LLDBProcessRunnerImpl.class.getName());

	@Autowired
	public void setProcessFactory(IProcessFactory factory){
		this.procfactory = factory;
	}
	
	@Override
	public boolean executeCmds(String cmd1, String cmd2){
		OutputStream out;
		boolean done = false;
		try {
			if(lldbProcess != null){
				out = lldbProcess.getOutputStream();
				out.write(cmd1.getBytes());
				out.write(cmd2.getBytes());
				out.flush();
				out.close();
			}else{
				lldbProcess = procfactory.create(cmd1);
				out = lldbProcess.getOutputStream();
				out.write(cmd2.getBytes());  
				out.flush();
			}		
			done = true;
		} catch (IOException e1) {
			LOG.error("Executing cmds on attached lldb process has failed");
		}
		
		return done;
		
	}
}

