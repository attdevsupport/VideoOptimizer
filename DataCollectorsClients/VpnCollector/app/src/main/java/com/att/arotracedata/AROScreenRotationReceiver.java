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

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import com.att.arocollector.utils.AROCollectorUtils;

public class AROScreenRotationReceiver extends AROBroadcastReceiver{
	
	private static final String TAG = "AROScreenRotationReceiver";

	/**
	 * LandScape Screen orientation
	 */
	private static final String LANDSCAPE_MODE = "landscape";
	
	/**
	 * Portrait Screen orientation
	 */
	private static final String PORTRAIT_MODE = "portrait";
	
	public AROScreenRotationReceiver(Context context, File traceDir, String outFileName, AROCollectorUtils mAroUtils) throws FileNotFoundException {
		super(context, traceDir, outFileName, mAroUtils);
		
		Log.i(TAG, "AROScreenRotationReceiver(...)");
		
		//recordScreenRotation();
	}
	
	/**
	 * method to record the screen rotation.
	 */
	private void recordScreenRotation() {
		final Configuration newConfig = context.getResources().getConfiguration();
		
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			writeTraceLineToAROTraceFile(LANDSCAPE_MODE, true);
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			writeTraceLineToAROTraceFile(PORTRAIT_MODE, true);
		}
	}
	

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "onReceive(...) action="+action);
		this.context = context;

		recordScreenRotation();
		
	}
	
}
