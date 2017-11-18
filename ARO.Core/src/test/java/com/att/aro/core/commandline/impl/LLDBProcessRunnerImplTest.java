
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.commandline.ILLDBProcessRunner;
import com.att.aro.core.commandline.IProcessFactory;

public class LLDBProcessRunnerImplTest extends BaseTest{

	LLDBProcessRunnerImpl lldbProcessRunner;
	IProcessFactory factory;
	Process process;

	@Before
	public void setUp() {
		lldbProcessRunner = (LLDBProcessRunnerImpl) context.getBean(ILLDBProcessRunner.class);
		factory = Mockito.mock(IProcessFactory.class);
	}
	

	@SuppressWarnings("unused")
	@Test
	public void executeCmdsTest() throws IOException{
		Runtime runtime = Mockito.mock(Runtime.class);
		process = Mockito.mock(Process.class);
		OutputStream out = Mockito.mock(ByteArrayOutputStream.class);
		boolean done = true;
		String cmd2 = "echo Hello";
		String cmd1 = "ls -la";
		Mockito.when(runtime.exec(cmd1)).thenReturn(process);
		        Mockito.when(process.getOutputStream()).thenReturn(out);
		       
		        Mockito.doNothing().when(out).write(cmd2.getBytes());
		        Mockito.doNothing().when(out).flush();

		boolean result = lldbProcessRunner.executeCmds(cmd1, cmd2);
		
		boolean result2 = lldbProcessRunner.executeCmds(cmd1, cmd2);
//		assertEquals(done,result);
//		assertEquals(done,result2);
	}
}

