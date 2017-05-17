/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Intent;
import android.util.Log;

import com.att.arocollector.utils.AROCollectorUtils;

public class ARONullMonitorService extends AROMonitorService{

	private static final String TAG = "ARONullMonitorService";
	public static final String ARO_NULL_MONITOR_SERVICE = "com.att.arotracedata.ARONullMonitorService";

	/**
	 * Setup and start monitoring
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand(...)");

		if (mAroUtils == null) {
			mAroUtils = new AROCollectorUtils();
			initFiles(intent);
			
			startARO_TraceMonitor();

		}
		return super.onStartCommand(intent, flags, startId);
		
	}

	/**
	 * Starts the GPS peripherals trace collection
	 */
	private void startARO_TraceMonitor() {
		Log.i(TAG, "startAROCameraTraceMonitor()");
		
	}
	
	/**
	 * Stops the trace collection
	 */
	@Override
	protected void stopMonitor(){
		
	}

}
