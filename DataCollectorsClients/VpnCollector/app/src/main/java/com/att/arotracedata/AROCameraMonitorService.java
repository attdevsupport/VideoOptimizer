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

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.att.arocollector.utils.AROCollectorUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AROCameraMonitorService extends AROMonitorService{

	private static final String TAG = "AROCameraMonitorService";
	public static final String ARO_CAMERA_MONITOR_SERVICE = "com.att.arotracedata.AROCameraMonitorService";

	/** Timer to run every 500 milliseconds to check Camera states */
	private Timer checkCameraLaunch = new Timer();

	/** Current Camera state on/off boolean flag */
	private Boolean mCameraOn = false;

	/** Previous Camera state on/off boolean flag */
	private Boolean mPrevCameraOn = true;


	/**
	 * Camera/GPS/Screen trace timer repeat time value to capture camera events
	 * ( 1/2 seconds)
	 */
	private static int HALF_SECOND_TARCE_TIMER_REPATE_TIME = 1000;

	/** ARO Data Collector utilities class object */
	private AROCollectorUtils mAroUtils;
	
	/**
	 * Setup and start monitoring
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand(...)");

		if (mAroUtils == null) {
			mAroUtils = new AROCollectorUtils();
			initFiles(intent);
			
			startAROCameraTraceMonitor();

		}
		return super.onStartCommand(intent, flags, startId);
		
	}

	/**
	 * Starts the GPS peripherals trace collection
	 */
	private void startAROCameraTraceMonitor() {
		Log.i(TAG, "startAROCameraTraceMonitor()");
		
		checkCameraLaunch.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				final String recentTaskName = getRecentTaskInfo().toLowerCase();
				if (recentTaskName.contains("camera")
						|| checkCurrentProcessStateForGround("camera")) {
					mCameraOn = true;
				} else
					mCameraOn = false;
				if (checkCurrentProcessState("camera"))
					mCameraOn = false;
				if (mCameraOn && !mPrevCameraOn) {
					Log.d(TAG, "Camera Turned on");
					writeTraceLineToAROTraceFile("ON", true);
				//	writeToFlurryAndMaintainStateAndLogEvent(cameraFlurryEvent, getString(R.string.flurry_param_status), "ON", true);
					mCameraOn = true;
					mPrevCameraOn = true;
				} else if (!mCameraOn && mPrevCameraOn) {
					Log.d(TAG, "Camera Turned Off");
					writeTraceLineToAROTraceFile(AroTraceFileConstants.OFF, true);
				//	writeToFlurryAndMaintainStateAndLogEvent(cameraFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.OFF, true);
					mCameraOn = false;
					mPrevCameraOn = false;
				}
			}
		}, HALF_SECOND_TARCE_TIMER_REPATE_TIME, HALF_SECOND_TARCE_TIMER_REPATE_TIME);
	}
	
	/**
	 * Gets the recent opened package name
	 * 
	 * @return recent launched package name
	 */
	private String getRecentTaskInfo() {
		/** Package name of recent launched application */
		String mLastLaucnhedProcess = " ";
		final ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final List<?> l = mActivityManager.getRecentTasks(5, ActivityManager.RECENT_WITH_EXCLUDED);
		final RecentTaskInfo rti = (RecentTaskInfo) l.get(0);
		if (!mLastLaucnhedProcess.equalsIgnoreCase(rti.baseIntent.getComponent().getPackageName())
				&& !rti.baseIntent.getComponent().getPackageName().equalsIgnoreCase("com.att.android.arodatacollector.main")) {

			Log.v(TAG, "New Task=" + rti.baseIntent.getComponent().getPackageName());

			mLastLaucnhedProcess = rti.baseIntent.getComponent().getPackageName();
			return mLastLaucnhedProcess;
		}
		mLastLaucnhedProcess = rti.baseIntent.getComponent().getPackageName();
		return mLastLaucnhedProcess;

	}

	/**
	 * Stops the camera trace collection
	 */
	@Override
	protected void stopMonitor(){
		if (checkCameraLaunch != null) {
			checkCameraLaunch.cancel();
			checkCameraLaunch = null;
		}
	}



}
