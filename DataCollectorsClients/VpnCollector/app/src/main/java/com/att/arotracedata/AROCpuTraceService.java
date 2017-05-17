/*
 * Copyright 2017 AT&T
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
package com.att.arotracedata;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.att.arocollector.utils.AROCollectorUtils;
import com.att.arocollector.utils.AROLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AROCpuTraceService extends Service {

	/** A string for logging an ARO Data Collector service. */
	public static final String TAG = "AROCpuTraceService";

	/** ARO Data Collector utilities class object */
	private AROCollectorUtils mAroUtils;

	private File traceDir;

	private BufferedWriter mCpuTraceWriter;

	private BufferedWriter mCpuDebugWriter;

	/** cpu event file name */
	private static final String outCpuFileName = "cpu";

	private static final String outCpuDebugFileName = "cpu_log";

	/** The root directory of the ARO Data Collector Trace. */
	public static final String ARO_TRACE_ROOTDIR = "/sdcard/ARO/";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (mAroUtils == null) {
			mAroUtils = new AROCollectorUtils();
			initTraceFile(intent);

			startAROCpuTrace();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * Gets the ARO Data Collector trace folder name.
	 * 
	 * @return A string that is the ARO Data Collector trace folder name.
	 */
	public String getDumpTraceFolderName() {
		final SharedPreferences prefs = getSharedPreferences("AROPrefs", 0);
		return (prefs.getString("TraceFolderName", ""));
	}

	/**
	 * Create file and open outputStream
	 */
	private void initTraceFile(Intent intent) {

		if (intent != null) {
			Log.i(TAG, "initFiles(Intent " + intent.toString() + ") hasExtras = " + intent.getExtras());
			String traceDirStr = intent.getStringExtra("TRACE_DIR");
			traceDir = new File(traceDirStr);
			traceDir.mkdir();
		} else {
			Log.i(TAG, "intent is null");
		}

		File file = new File(traceDir, outCpuFileName);
		try {
			file.createNewFile();
			mCpuTraceWriter = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			Log.i(TAG, "initTraceFile(Intent intent) Exception:" + e.getMessage());
			e.printStackTrace();
		}

		File mCpuTraceDebugFile = new File(traceDir, outCpuDebugFileName);
		try {
			mCpuTraceDebugFile.createNewFile();
			mCpuDebugWriter = new BufferedWriter(new FileWriter(mCpuTraceDebugFile));
		} catch (IOException e) {
			Log.i(TAG, "initTraceFile(Intent intent) Exception:" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * method to stop the cpu trace 1. stop the cpu script 2. process the
	 * remaining cpu files
	 */
	private void stopAROCpuTrace() {
		try {
			Process sh = null;
			DataOutputStream os = null;
			int pid = getCpuScriptPid();

			if (pid != -1) {
				String command = "kill -15 " + pid + "\n";
				sh = Runtime.getRuntime().exec("sh");
				AROLogger.e(TAG, "stopAROCpuTrace - su pid = " + sh);
				os = new DataOutputStream(sh.getOutputStream());
				os.writeBytes(command);

				command = "exit\n";
				os.writeBytes(command);
				os.flush();

				// clear out the sharedpref
				setCpuScriptPid("-1");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (cpuProcessingTimer != null) {
			cpuProcessingTimer.cancel();
			cpuProcessingTimer = null;
		}

		// process the directory for any files that hasnt been processed
		processCpuDirectory();

		if(isAndroidVersionLessThanNougat() == true){
			// delete the directory
			File cpuDir = new File(getCpuDirFullPath());
			if (cpuDir.listFiles() != null) {
				for (File cpuFile : cpuDir.listFiles()) {
					cpuFile.delete();
				}
			}
			cpuDir.delete();
		}
	}
	
	/*
	 * Finds out if the device connected or used is less than Android 7.0
	 */
	public boolean isAndroidVersionLessThanNougat(){
		boolean result = false;
		int sdkAPILevel = Integer.valueOf(android.os.Build.VERSION.SDK_INT);
		if(sdkAPILevel < 24)// TODO - Update the Nougat build version code
			result = true;
		return result;
	}
	
	/**
	 * stop monitoring and close trace file
	 */
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
		stopAROCpuTrace();
		closeTraceFile();
		super.onDestroy();
	}

	/**
	 * Finalize and close the trace file
	 */
	private void closeTraceFile() {
		try {
			mCpuTraceWriter.flush();
			mCpuTraceWriter.close();
		} catch (IOException e) {
			Log.i(TAG, "closeTraceFile() Exception:" + e.getMessage());
			e.printStackTrace();
		}

		try {
			mCpuDebugWriter.flush();
			mCpuDebugWriter.close();
		} catch (IOException e) {
			Log.i(TAG, "closeTraceFile() Exception:" + e.getMessage());
			e.printStackTrace();
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*
	 * below imported from AROCollectorService
	 */

	// private static final Pattern totalCpuLinePattern = Pattern.compile("^User
	// (\\d+)%, System (\\d+)%, IOW (\\d+)%, IRQ (\\d+)%");
	// total cpu line has pattern: "User 261 + Nice 1 + Sys 105 + Idle 388 + IOW
	// 7 + IRQ 0 + SIRQ 0 = 762"
	private static final Pattern totalCpuLinePattern = Pattern.compile(
			"^User (\\d+) \\+ Nice (\\d+) \\+ Sys (\\d+) \\+ Idle (\\d+) \\+ IOW (\\d+) \\+ IRQ (\\d+) \\+ SIRQ (\\d+) = (\\d+)");
	private static final int CPU_PATTERN_GROUP_COUNT = 8;
	private static final int TOTAL_CPU_GROUP_NUMBER = 8;
	private static final int IDLE_CPU_GROUP_NUMBER = 4;

	private static final String WHITE_SPACE_REG_EXP = "\\s+";
	private static final int CPU_TRACE_INTERVAL_MILLIS = 5000;
	private static final int CPU_TRACE_INITIAL_DELAY_MILLIS = 4000;

	Timer cpuProcessingTimer;
	private final String CPU_DIR = "cpufiles/";
	private static final String CPU_SCRIPT_PID = "CPU_SCRIPT_PID";

	// for cpu tracing
	private boolean columnHeaderLineProcessed = false, columnIndicesSet = false, totalCpuLineProcessed = false;
	private boolean doneParsingProcessCpu = false;
	private static int processCpuColumnIndex = -1, processNameColumnIndex = -1, curProcessCount = 0,
			nonEmptyLineNum = 0;
	private static final int PROCESS_LIMIT = 8;

	private static final String CPU_HEADER = "CPU%";
	private static final String PROCESS_NAME_HEADER = "Name";

	/**
	 * method to start the cpu trace
	 */
	private void startAROCpuTrace() {
		
		// start the script if android os version is less than nougat
		if(isAndroidVersionLessThanNougat() == true)
			startAROCpuTraceScript();

		// start timer to process the script output files
		cpuProcessingTimer = new Timer();

		TimerTask checkCpuUsageTask = new TimerTask() {
			// Runnable cpuTrace = new Runnable(){
			public void run() {
				processCpuDirectory();
			}

		};

		cpuProcessingTimer.scheduleAtFixedRate(checkCpuUsageTask, CPU_TRACE_INITIAL_DELAY_MILLIS,
				CPU_TRACE_INTERVAL_MILLIS);
	}

	/**
	 * get the pid of the cpu script
	 * 
	 * @return pid of the cpu script
	 */
	public int getCpuScriptPid() {
		final SharedPreferences prefs = getSharedPreferences("AROPrefs", 0);
		return prefs.getInt(CPU_SCRIPT_PID, -1);
	}

	/**
	 * save the pid of the cpu script
	 * 
	 * @param pid
	 */
	public void setCpuScriptPid(String pid) {
		int pidInt = -1;
		try {
			pidInt = Integer.parseInt(pid);
		} catch (Exception e) {
			e.printStackTrace();
		}

		final SharedPreferences prefs = getSharedPreferences("AROPrefs", 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(CPU_SCRIPT_PID, pidInt);
		editor.commit();
	}

	/**
	 * Start the script that tracks the cpu usage stats 1. give script
	 * executable permission 2. make output dir 3. execute script
	 */
	private void startAROCpuTraceScript() {
		final int cpuScriptPid = getCpuScriptPid();

		if (cpuScriptPid == -1){
			// script NOT already running, need to start it
			String cpuDirFullPath = getCpuDirFullPath();

			try {
				File cpuDir = new File(cpuDirFullPath);
				if (!cpuDir.exists()) {
					cpuDir.mkdir();
				}

				File cpumonFile = new File(cpuDirFullPath + "/processcpumon.sh");
				// if file doesn't exists, then create it
				if (!cpumonFile.exists()) {
					cpumonFile.createNewFile();
				}

				final String[] processCpuMonFile = { "#!/bin/sh", "echo $$", "while :", "do", "	curTime=$(date +\"%s\")",
						"	outFile=$1\"/\"$curTime", "	top -n 1 > $outFile", "	sleep 2", "done" };
				FileWriter fw = new FileWriter(cpumonFile.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (String line : processCpuMonFile) {
					bw.write(line);
					bw.newLine();
				}
				bw.close();

				String cmd = "sh " + cpuDirFullPath + "processcpumon.sh" + " " + cpuDirFullPath + "\n";
				Process proc = Runtime.getRuntime().exec(cmd);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String pId = bufferedReader.readLine();
				Log.d(TAG, "pId : " + pId );
				Log.d(TAG, "cmd : " + cmd );
				setCpuScriptPid(pId);

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			AROLogger.d(TAG, "script already running under pid=" + cpuScriptPid);
			writeTraceLineToAROTraceFile(mCpuDebugWriter, "script already running under pid=" + cpuScriptPid, true);
		}
	}

	/**
	 * Method to process all the files in the cpu directory. needs
	 * synchronization here so that in case the timertask is already in
	 * progress, the stop code calling this method will need to wait
	 */
	private synchronized void processCpuDirectory() {
		String cpuFilesPath = getCpuDirFullPath();
		long start = System.currentTimeMillis();
		AROLogger.v(TAG, "processCpuDirectory() started at " + start);
		writeTraceLineToAROTraceFile(mCpuDebugWriter, "cpu file processing started", true);
		try {
			File cpuDir = new File(getCpuDirFullPath());
			if (cpuDir.exists() && cpuDir.listFiles() != null) {
				for (File cpuFile : cpuDir.listFiles()) {
					// No need to parse script file, just parse the output of the scripts
					if(false == cpuFile.getAbsolutePath().equals(cpuFilesPath + "processcpumon.sh") &&
							false == cpuFile.getAbsolutePath().equals(cpuFilesPath + "cpumonpid.sh"))
						processCpuFile(cpuFile);
				}
			}

		} catch (Exception e) {
			writeTraceLineToAROTraceFile(mCpuDebugWriter, "Exception occurred in checkCpuUsageTask: " + e.getMessage(),
					true);
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();

		if (AROLogger.logVerbose) {
			AROLogger.v(TAG, "processCpuDirectory() ended at " + end + ". Duration: " + (end - start));
		}
	}

	/**
	 * Method to process the cpu statistic file, which is the output of the
	 * 'top' command
	 * 
	 * @param cpuFile
	 */
	private void processCpuFile(File cpuFile) {
		writeTraceLineToAROTraceFile(mCpuDebugWriter, "start processing " + cpuFile.getName(), true);
		long start = System.currentTimeMillis();
		columnHeaderLineProcessed = false;
		totalCpuLineProcessed = false;
		curProcessCount = 0;
		nonEmptyLineNum = 0;

		doneParsingProcessCpu = false;

		String line = "";
		boolean isDelete = true;
		StringBuffer content = new StringBuffer();
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(cpuFile));
			while ((!doneParsingProcessCpu && curProcessCount < PROCESS_LIMIT) && (line = input.readLine()) != null) {
				parseCpuInfo(content, line);
			}
			String contentStr = content.toString().trim();
			if (contentStr.length() > 0) {
				double traceFileTimestamp = Double.parseDouble(cpuFile.getName());
				writeTraceLineToAROTraceFile(mCpuTraceWriter, traceFileTimestamp + " " + contentStr, false);
			} else {
				isDelete = false;
			}

			long end = System.currentTimeMillis();
			writeTraceLineToAROTraceFile(mCpuDebugWriter, (end - start) + "ms| " + contentStr, true);

		} catch (Exception e) {
			// this file has error, allow processing to continue to the next
			// file
			writeTraceLineToAROTraceFile(mCpuDebugWriter,
					cpuFile.getName() + ": Exception: " + e.getMessage() + ". Line: " + line, true);
			e.printStackTrace();
		} finally {
			try {
				input.close();

				if (isDelete) {
					cpuFile.delete();
				}
			} catch (IOException e) {
				e.printStackTrace();
				writeTraceLineToAROTraceFile(mCpuDebugWriter, cpuFile.getName() + ": Exception: " + e.getMessage(),
						true);
			}
		}
	}

	/**
	 * get the process name and cpu percentage from the line outputted by the
	 * 'top' command
	 * 
	 * @param content
	 * @param line
	 */
	private void getProcessCpuInfo(StringBuffer content, String line) {
		String[] cpuInfoComp = line.split(WHITE_SPACE_REG_EXP);

		String processName = "";
		String processCpu = cpuInfoComp[processCpuColumnIndex].replace("%", "");

		// workaround for blank column values
		if (cpuInfoComp.length > processNameColumnIndex) {
			processName = cpuInfoComp[processNameColumnIndex];
		} else {
			processName = cpuInfoComp[cpuInfoComp.length - 1];
		}

		if (Integer.parseInt(processCpu) == 0) {
			// the top command returns process ordered by cpu usage,
			// we only want non-zero processes, so we can stop when the
			// processCpu is 0.
			doneParsingProcessCpu = true;
		} else {
			content.append(processName + "=" + processCpu + " ");
			++curProcessCount;
		}
	}

	/**
	 * Method write given String message to trace file passed as an argument
	 * outputfilewriter : Name of Trace File writer to which trace has to be
	 * written content : Trace message to be written
	 */
	private void writeTraceLineToAROTraceFile(BufferedWriter outputfilewriter, String content, boolean timestamp) {
		try {
			if (outputfilewriter != null) {

				final String eol = System.getProperty("line.separator");
				if (timestamp) {
					outputfilewriter.write(mAroUtils.getDataCollectorEventTimeStamp() + " " + content + eol);
					outputfilewriter.flush();
				} else {
					outputfilewriter.write(content + eol);
					outputfilewriter.flush();
				}
			}
		} catch (IOException e) {
			// TODO: Need to display the exception error instead of Mid Trace
			// mounted error
			// mApp.setMediaMountedMidAROTrace(mAroUtils.checkSDCardMounted());
			Log.e(TAG, "exception in writeTraceLineToAROTraceFile" + e);
		}
	}

	/**
	 * Method to parse the cpu info from a line of text outputted by the 'top'
	 * command
	 * 
	 * @param content
	 * @param line
	 */
	private void parseCpuInfo(StringBuffer content, String line) {
		// writeTraceLineToAROTraceFile(mCpuDebugWriter, "parseCpuInfo for
		// line=: " + line, false);

		line = line.replaceAll("\r", "").replaceAll("\n", "").trim();
		if (line.length() > 0) {
			++nonEmptyLineNum;

			if (!columnHeaderLineProcessed) {
				if (!totalCpuLineProcessed && parseTotalCpuLine(content, line)) {
					totalCpuLineProcessed = true;
				}

				else if (totalCpuLineProcessed && isColumnHeaderLine(line)) {
					if (!columnIndicesSet) {
						// columnIndices are set just once per app run
						setColumnHeaderIndices(line);
					}

					columnHeaderLineProcessed = true;
				}
			}

			else if (!doneParsingProcessCpu && curProcessCount < PROCESS_LIMIT) {
				// get process cpu info
				getProcessCpuInfo(content, line);
			}
		}
	}

	/**
	 * check if the line matches the total cpu line. if yes, parse the cpu line
	 * and return true. Otherwise, return false.
	 * 
	 * @param content
	 * @param line
	 */
	private boolean parseTotalCpuLine(StringBuffer content, String line) {

		Matcher totalCpuLineMatcher = totalCpuLinePattern.matcher(line);

		boolean isTotalCpuLine = totalCpuLineMatcher.find();
		if (isTotalCpuLine) {
			// capturing group is numbered starting at 1 from left to right
			// based on placement of opening parenthesis
			// total cpu group = 8, idle cpu group = 4

			if (totalCpuLineMatcher.groupCount() == CPU_PATTERN_GROUP_COUNT) {

				int total = Integer.parseInt(totalCpuLineMatcher.group(TOTAL_CPU_GROUP_NUMBER));
				int idle = Integer.parseInt(totalCpuLineMatcher.group(IDLE_CPU_GROUP_NUMBER));
				int usage = total - idle;
				int usagePercent = usage * 100 / total;
				content.append(usagePercent + " ");

			} else {
				AROLogger.w(TAG, "totalCpuLineMatcher doesn't return 8 capturing groups for line=" + line);
				writeTraceLineToAROTraceFile(mCpuDebugWriter,
						"totalCpuLineMatcher doesn't return 8 capturing groups for line= " + line, true);
				isTotalCpuLine = false;
			}
		} else if (nonEmptyLineNum == 2) {
			// 2nd line isnt a parsable total cpu line
			writeTraceLineToAROTraceFile(mCpuDebugWriter, "2nd line is not totalCpuLine; line= " + line, true);
		}

		return isTotalCpuLine;
	}

	/**
	 * check if the line contains the column header
	 * 
	 * @param line
	 * @return
	 */
	private boolean isColumnHeaderLine(String line) {
		return line.contains("PID") && line.contains("CPU%") && line.contains("Name");
	}

	/**
	 * need to set the col header indices because different android
	 * devices/platform returns the header in different order
	 * 
	 * @param line
	 */
	private void setColumnHeaderIndices(String line) {
		String[] colHeaders = line.split(WHITE_SPACE_REG_EXP);
		int colHeaderIndex = 0;

		for (int i = 0; i < colHeaders.length; ++i) {
			String curHeader = colHeaders[i].trim();

			if (curHeader.length() > 0) {
				if (curHeader.equalsIgnoreCase(CPU_HEADER)) {
					processCpuColumnIndex = colHeaderIndex;
				} else if (curHeader.equalsIgnoreCase(PROCESS_NAME_HEADER)) {
					processNameColumnIndex = colHeaderIndex;
				}

				++colHeaderIndex;
			}
		}

		if (processCpuColumnIndex != -1 && processNameColumnIndex != -1) {
			columnIndicesSet = true;
		} else {
			AROLogger.w(TAG, "could not set processCpuColumnIndex/processNameColumnIndex for line=" + line);
			writeTraceLineToAROTraceFile(mCpuDebugWriter,
					"could not set processCpuColumnIndex/processNameColumnIndex for line=" + line, false);
		}
	}

	/**
	 * get the full path of the directory of the cpu files
	 * 
	 * @return full path of the cpu dir
	 */
	private String getCpuDirFullPath() {
		return (ARO_TRACE_ROOTDIR + CPU_DIR);
	}
}