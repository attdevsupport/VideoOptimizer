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
package com.att.aro.core.commandline;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public interface IExternalProcessRunner {

	/**
	 * Execute command passed in as a string and return string back.
	 */
	String runGetString(String command) throws IOException;

	/**
	 * Execute command passed in as a array of strings and return string back.
	 */
	String runCmd(String[] command) throws IOException;

	String runCmdWithTimeout(String[] command, long timeout) throws IOException;


	/**
	 * Execute command passed in as a string and return ByteArrayOutputStream
	 * object back.
	 */
	ByteArrayOutputStream run(String command) throws IOException;

	/**
	 * execute command in bash/CMD shell
	 * 
	 * @param cmd
	 * @return stdout and stderr
	 */
	String executeCmd(String cmd);
	/**
	 * execute command in bash/CMD shell
	 * 
	 * @param cmd
	 * @param redirectErrorStream
	 * @param readCommandResponse
	 * @return stdout and stderr
	 */
	String executeCmd(String cmd, boolean redirectErrorStream, boolean readCommandResponse);
	/**
	 * @param cmd
	 * @param earlyExit
	 * @param msg
	 * @param readCommandResponse
	 * @return stdout and stderr
	 */
	String executeCmdRunner(String cmd, boolean earlyExit, String msg,  boolean readCommandResponse);
	
	/**
	 * @param cmd
	 * @param earlyExit
	 * @param msg
	 * @param redirectErrorStream
	 * @param readCommandResponse
	 * @return stdout and stderr
	 */
	String executeCmdRunner(String cmd, boolean earlyExit, String msg, boolean redirectErrorStream,  boolean readCommandResponse);
	/**
	 * @param workingDir
	 * @param cmd
	 * @param redirectErrorStream
	 * @param readCommandResponse
	 * @return stdout and stderr
	 */
	String executeCmd(File workingDir, String cmd, boolean redirectErrorStream, boolean readCommandResponse);
	
	/**
	 * @param workingPath
	 * @param cmd
	 * @param earlyExit
	 * @param msg
	 * @param redirectErrorStream
	 * @param readCommandResponse
	 * @return stdout and stderr
	 */
	String executeCmdRunner(File workingPath, String cmd, boolean earlyExit, String msg, boolean redirectErrorStream,  boolean readCommandResponse);
	
 

}
