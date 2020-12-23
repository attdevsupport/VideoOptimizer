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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.IProcessFactory;
import com.att.aro.core.commandline.pojo.ProcessWorker;
import com.att.aro.core.concurrent.IThreadExecutor;
import com.att.aro.core.util.Util;

public class ExternalProcessRunnerImpl implements IExternalProcessRunner {

	IThreadExecutor threadExecuter;
	ProcessWorker worker = null;
	IProcessFactory procfactory;
    
	private static final Logger LOG = LogManager.getLogger(ExternalProcessRunnerImpl.class.getName());

	@Autowired
	public void setProcessFactory(IProcessFactory factory){
		this.procfactory = factory;
	}

	@Autowired
	public void setThreadExecutor(IThreadExecutor threadExecuter) {
		this.threadExecuter = threadExecuter;
	}

	public ExternalProcessRunnerImpl() {
	}

	public void setProcessWorker(ProcessWorker worker) {
		this.worker = worker;
	}

	/**
	 * execute command in bash/CMD shell
	 * 
	 * @param cmd
	 * @return stdout and stderr
	 */
	@Override
	public String executeCmd(String cmd) {
		return executeCmd(cmd, true);
	}

	@Override
	public String executeCmdRunner(String cmd, boolean earlyExit, String msg) {
		return executeCmdRunner(cmd, earlyExit, msg, false);
	}
	/**
	 * execute command in bash/CMD shell
	 * 
	 * @param cmd
	 * @return stdout and stderr
	 */
	@Override
	public String executeCmd(String cmd, boolean redirectErrorStream) {
		String result = executeCmdRunner(cmd, false, "", redirectErrorStream);
		return result;
	}

	@Override
	public String executeCmdRunner(String cmd, boolean earlyExit, String msg, boolean redirectErrorStream) {
		ProcessBuilder pbldr = new ProcessBuilder();
		if (redirectErrorStream) {
			pbldr.redirectErrorStream(true);
		}
		String binPath = Util.getBinPath();
		if (!StringUtils.isEmpty(binPath)) {
			Map<String, String> envs = pbldr.environment();
			envs.put("PATH", System.getenv("PATH") + ":" + binPath);
		}

		if (!Util.isWindowsOS()) {
			pbldr.command(new String[] { "bash", "-c", cmd });
		} else {
			pbldr.command(new String[] { "CMD", "/C", cmd });
		}

		StringBuilder builder = new StringBuilder();
		try {
			Process proc = pbldr.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String line = null;
			while (true) {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				if (earlyExit && line.trim().equals(msg)) {
					LOG.debug("read a line:" + line);
					builder.append(line);
					break;
				}
				LOG.debug("read a line:" + line);
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			LOG.error("Error executing <" + cmd + "> IOException:", e);
		}
		return builder.toString();
	}

	@Override
	public String runCmd(String[] command) throws IOException {
		Process process = procfactory.create(command);

		InputStream input = process.getInputStream();
		try (ByteArrayOutputStream out = readInputStream(input)) {

			if (input != null) {
				input.close();
			}
			if (process != null) {
				process.destroy();
			}
			String datastr = null;
			if (out != null) {
				datastr = out.toString();
			}
			return datastr;
		}
	}

	ByteArrayOutputStream readInputStream(InputStream input) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		int totalread = -1;

		while ((totalread = input.read(data, 0, data.length)) != -1) {
			out.write(data, 0, totalread);
		}
		return out;
	}

	@Override
	public String runCmdWithTimeout(String[] command, long timeout) throws IOException {
		Process process = procfactory.create(command);
		if (worker == null) {
			worker = new ProcessWorker(process, timeout);
		}
		threadExecuter.execute(worker);

		InputStream input = process.getInputStream();
		try (ByteArrayOutputStream out = readInputStream(input)) {
			worker.setExit();
			worker = null;
			if (input != null) {
				input.close();
			}
			if (process != null) {
				process.destroy();
			}
			String datastr = null;
			if (out != null) {
				datastr = out.toString();
			}
			return datastr;
		}
	}


	@Override
	public String runGetString(String command) throws IOException {
		ByteArrayOutputStream data = this.run(command);
		String out = "";
		if (data != null) {
			out = data.toString();
			data.close();
			return out;
		}
		return null;
	}

	@Override
	public ByteArrayOutputStream run(String command) throws IOException {
		Process process = procfactory.create(command);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream input = process.getInputStream();
		byte[] data = new byte[1024];
		int totalread = -1;

		while ((totalread = input.read(data, 0, data.length)) != -1) {
			out.write(data, 0, totalread);
		}

		if (input != null) {
			input.close();
		}
		if (process != null) {
			process.destroy();
		}
		return out;
	}

}
