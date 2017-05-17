/*
 * Copyright 2014 AT&T
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

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.att.arocollector.utils.AROCollectorUtils;
import com.att.arocollector.utils.AROLogger;

import java.io.File;
import java.io.FileNotFoundException;

public class AROWifiEventsReceiver extends AROBroadcastReceiver{
	
	private static final String TAG = "AROWifiEventsReceiver";

	private WifiManager mWifiManager;
	private String mWifiMacAddress;
	private String mWifiNetworkSSID;
	private int mWifiRssi;

	private ConnectivityManager mConnectivityManager;
	
	public AROWifiEventsReceiver(Context context, File traceDir, String outFileName, AROCollectorUtils mAroUtils) throws FileNotFoundException {
		super(context, traceDir, outFileName, mAroUtils);
		
		Log.i(TAG, "AROWifiEventsReceiver(...)");

		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "onReceive(...) action=" + action);
		this.context = context;

		if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
			AROLogger.d(TAG, "entered WIFI_STATE_CHANGED_ACTION");

			if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
				AROLogger.d(TAG, "entered WIFI_STATE_CHANGED_ACTION--DISCONNECTED");
				writeTraceLineToAROTraceFile(AroTraceFileConstants.DISCONNECTED_NETWORK, true);

				// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.DISCONNECTED_NETWORK, true);

			} else if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
				AROLogger.d(TAG, "entered WIFI_STATE_CHANGED_ACTION--OFF");
				writeTraceLineToAROTraceFile(AroTraceFileConstants.OFF, true);

				// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.OFF, true);
			}
		} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

			final NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			final NetworkInfo.State state = info.getState();

			switch (state) {

			case CONNECTING:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.CONNECTING_NETWORK, true);
				// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.CONNECTING_NETWORK, true);
				break;
			case CONNECTED:
				recordAndLogConnectedWifiDetails();
				break;
			case DISCONNECTING:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.DISCONNECTING_NETWORK, true);
				// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.DISCONNECTING_NETWORK, true);
				break;
			case DISCONNECTED:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.DISCONNECTED_NETWORK, true);
				// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.DISCONNECTED_NETWORK, true);

				break;
			case SUSPENDED:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.SUSPENDED_NETWORK, true);
				// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.SUSPENDED_NETWORK, true);
				break;
			case UNKNOWN:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.UNKNOWN_NETWORK, true);
				// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.UNKNOWN_NETWORK, true);
				break;
			}
		}

	}
	
	/**
	 * record the connected wifi information
	 */
	private void recordAndLogConnectedWifiDetails() {
		collectWifiNetworkData();
		writeTraceLineToAROTraceFile(AroTraceFileConstants.CONNECTED_NETWORK + " " + mWifiMacAddress + " " + mWifiRssi + " " + mWifiNetworkSSID, true);

		if (AROLogger.logDebug) {
			AROLogger.d(TAG, "connected to " + mWifiNetworkSSID + " write to mWifiTracewriter completed at timestamp: " + mAroUtils.getDataCollectorEventTimeStamp());
		}

		// writeToFlurryAndMaintainStateAndLogEvent(wifiFlurryEvent, getString(R.string.flurry_param_status), AroTraceFileConstants.CONNECTED_NETWORK, true);
	}

	/**
	 * Collects the wifi network trace data
	 */
	private void collectWifiNetworkData() {
		
		if (mWifiManager != null) {
			mWifiMacAddress = mWifiManager.getConnectionInfo().getBSSID();
			mWifiNetworkSSID = mWifiManager.getConnectionInfo().getSSID();
			mWifiRssi = mWifiManager.getConnectionInfo().getRssi();
			
			if (AROLogger.logDebug){
				AROLogger.d(TAG, "mWifiMac=" + mWifiMacAddress + ", ssid=" + mWifiNetworkSSID + ", rssi:" + mWifiRssi);
			}
		}
	}

}
