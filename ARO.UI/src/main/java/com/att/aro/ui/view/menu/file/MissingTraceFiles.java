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
package com.att.aro.ui.view.menu.file;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.pojo.AROTraceData;

/**
 * <p>
 * This encapsulates the check for missing files in a trace.
 * </p><p>
 * 	<strong>NOTE:</strong>  This does not belong here!!  It most likely belongs in core.
 * Furthermore, this really does the wrong thing by directly altering the data model's
 * <em>missingFiles</em> attribute.  <b><u>THIS SHOULD NOT BE DONE FROM THE UI!!!</u></b>.
 * It's currently done here for time and lack of model alteration forethought reasons (found
 * out the hard way this functionality was not previously implemented).
 * </p><p>
 * TODO:  FIX this direct modification to the model from the UI - and the actual process
 * of determining missing files while we're at it!
 * </p>
 * 
 *
 *
 */
public class MissingTraceFiles {
	private final File tracePath;
	private final AROTraceData model;

	public MissingTraceFiles(File tracePath, AROTraceData model) {
		this.tracePath = tracePath;
		this.model = model;
	}
	public MissingTraceFiles(File tracePath) {
		this(tracePath, null);
	}


	public String formatMissingFiles(Set<File> missingFiles) {
		StringBuilder missingFilesString = new StringBuilder();
		boolean firstTime = true;
		for (File missingFile : missingFiles) {
			if (!firstTime) {
				missingFilesString.append("\n");
			}
			missingFilesString.append(missingFile.getName());
			firstTime = false;
		}
		return missingFilesString.toString();
	}

	public Set<String> getModelMissingFiles(Set<File> missingFiles) {
		Set<String> modelMissingFiles = new LinkedHashSet<String>();
		for (File missingFile : missingFiles) {
			modelMissingFiles.add(missingFile.getName());
		}
		return modelMissingFiles;
	}

	private boolean isFilePresent(String traceDataFileName) {
		return new File(tracePath, traceDataFileName).exists();
	}

	private void addMissingFileMaybe(String traceDataFileName,
			Set<File> missingFiles) {
		File fileExistCheck = new File(tracePath, traceDataFileName);
		if (!isFilePresent(traceDataFileName)) {
			missingFiles.add(fileExistCheck);
		}
	}


	public Set<File> retrieveMissingFiles() {
		Set<File> missingFiles = new LinkedHashSet<File>();
		addMissingFileMaybe(TraceDataConst.FileName.APPNAME_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.DEVICEINFO_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.DEVICEDETAILS_FILE, missingFiles);
		// addMissingFileMaybe(TraceDataConst.FileName.NETWORKINFO_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.CPU_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.GPS_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.BLUETOOTH_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.WIFI_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.CAMERA_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.SCREEN_STATE_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.USER_EVENTS_FILE, missingFiles);
		// addMissingFileMaybe(TraceDataConst.FileName.SCREEN_ROTATIONS_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.ALARM_END_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.ALARM_START_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.KERNEL_LOG_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.BATTERY_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.BATTERYINFO_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.TEMPERATURE_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.LOCATION_FILE, missingFiles);
		addMissingFileMaybe(TraceDataConst.FileName.RADIO_EVENTS_FILE, missingFiles);
		// addMissingFileMaybe(TraceDataConst.FileName.SSLKEY_FILE, missingFiles);

		// TODO:  Move this to Core (preferably immutable through a builder and no setters)!
		// UI shoule not be doing this!!!
		if (model != null) {
			AbstractTraceResult traceResult = model.getAnalyzerResult().getTraceresult();
			if (traceResult instanceof TraceDirectoryResult) {
				((TraceDirectoryResult) traceResult).setMissingFiles(
						getModelMissingFiles(missingFiles));
			}
		}
		return missingFiles;
	}


	@Override
	public String toString() {
		return "MissingTraceFiles [tracePath=" + tracePath + "]";
	}
}
