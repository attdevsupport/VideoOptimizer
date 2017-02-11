/*
 *  Copyright 2015 AT&T
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.IProcessFactory;
import com.att.aro.core.commandline.pojo.ProcessWorker;
import com.att.aro.core.concurrent.IThreadExecutor;

public class ExternalProcessRunnerImplTest extends BaseTest {

	ExternalProcessRunnerImpl externalProcessRunner;
	ProcessWorker worker;
	IThreadExecutor threadExecuter;
	Process process;
	String[] cmds = new String[1];
	Runtime runtime;
	IProcessFactory factory;

	@Before
	public void setUp() {
		externalProcessRunner = (ExternalProcessRunnerImpl) context.getBean(IExternalProcessRunner.class);
		factory = Mockito.mock(IProcessFactory.class);
	}

	@Test
	public void executeCmdTest() throws InterruptedException {
		String results = externalProcessRunner.executeCmd("echo \"Hello\"");
		assertTrue(results.contains("Hello"));
	}

	@Test
	public void runCmdTest() throws IOException {
		process = Mockito.mock(Process.class);
		String aMessage = "hello";
		InputStream stream = new ByteArrayInputStream(aMessage.getBytes());
		Mockito.when(process.getInputStream()).thenReturn(stream);
		Mockito.doNothing().when(process).destroy();

		Mockito.when(factory.create(Mockito.any(String[].class))).thenReturn(process);
		externalProcessRunner.setProcessFactory(factory);

		cmds[0] = "test";
		String res = externalProcessRunner.runCmd(cmds);
		assertEquals(aMessage, res);
	}

	@Test
	public void runCmdGetStringTet() throws IOException {
		process = Mockito.mock(Process.class);
		String aMessage = "hello";
		InputStream stream = new ByteArrayInputStream(aMessage.getBytes());
		Mockito.when(process.getInputStream()).thenReturn(stream);
		Mockito.doNothing().when(process).destroy();

		Mockito.when(factory.create(Mockito.any(String[].class))).thenReturn(process);
		Mockito.when(factory.create(Mockito.anyString())).thenReturn(process);
		externalProcessRunner.setProcessFactory(factory);

		String res = externalProcessRunner.runGetString("test");

		assertEquals(aMessage, res);
	}

	@Test
	public void runCmdWithTimeout() throws Exception {
		threadExecuter = Mockito.mock(IThreadExecutor.class);
		Mockito.doNothing().when(threadExecuter).execute(Mockito.any(Runnable.class));

		process = Mockito.mock(Process.class);
		String aMessage = "hello";
		InputStream stream = new ByteArrayInputStream(aMessage.getBytes());
		Mockito.when(process.getInputStream()).thenReturn(stream);
		Mockito.doNothing().when(process).destroy();

		Mockito.when(factory.create(Mockito.any(String[].class))).thenReturn(process);
		Mockito.when(factory.create(Mockito.anyString())).thenReturn(process);

		worker = Mockito.mock(ProcessWorker.class);

		Mockito.doNothing().when(worker).setExit();

		externalProcessRunner.setProcessFactory(factory);
		externalProcessRunner.setProcessWorker(worker);
		externalProcessRunner.setThreadExecutor(threadExecuter);

		cmds[0] = "test";
		long timeout = 1;
		String res = externalProcessRunner.runCmdWithTimeout(cmds, timeout);
		assertEquals(aMessage, res);
	}

}
