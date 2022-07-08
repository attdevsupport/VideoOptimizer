/*
 *  Copyright 2014 AT&T
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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.att.arocollector.utils.AROCollectorUtils;
import com.att.arocollector.utils.AROLogger;

public class AROWifiEventsReceiver extends AROBroadcastReceiver{
	
	private static final String TAG = "AROWifiEventsReceiver";
	
	public AROWifiEventsReceiver(Context context, File traceDir, String outFileName, AROCollectorUtils mAroUtils) throws FileNotFoundException {
		super(context, traceDir, outFileName, mAroUtils);
		Log.i(TAG, "AROWifiEventsReceiver(...)");
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "onReceive(...) action=" + action);
		Log.d("VPNData", "OnReceive");
		this.context = context;

		if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
			Log.d("VPNData", "WiFi Changed Action");
			AROLogger.d(TAG, "entered WIFI_STATE_CHANGED_ACTION");
			WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
				Log.d("VPNData", "WiFi State Enabled");
				AROLogger.d(TAG, "entered WIFI_STATE_CHANGED_ACTION--DISCONNECTED");
				writeTraceLineToAROTraceFile(AroTraceFileConstants.DISCONNECTED_NETWORK, true);

			} else if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
				Log.d("VPNData", "WiFi State Disabled");
				AROLogger.d(TAG, "entered WIFI_STATE_CHANGED_ACTION--OFF");
				writeTraceLineToAROTraceFile(AroTraceFileConstants.OFF, true);
			}
		} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			final NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			final NetworkInfo.State state = info.getState();
			Log.d("VPNData", "Network State Changed. " + state);
			switch (state) {

			case CONNECTING:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.CONNECTING_NETWORK, true);
				break;
			case CONNECTED:
				if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
					if (mWifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED) {
						Log.d("VPNData", AroTraceFileConstants.CONNECTED_NETWORK + " " + mWifiManager.getConnectionInfo().getBSSID() + " " + mWifiManager.getConnectionInfo().getRssi() + " " + mWifiManager.getConnectionInfo().getSSID() + " " + mWifiManager.getConnectionInfo().getHiddenSSID());
						writeTraceLineToAROTraceFile(AroTraceFileConstants.CONNECTED_NETWORK + " " + mWifiManager.getConnectionInfo().getBSSID() + " " + mWifiManager.getConnectionInfo().getRssi() + " " + mWifiManager.getConnectionInfo().getSSID(), true);
					}
				}
				break;
			case DISCONNECTING:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.DISCONNECTING_NETWORK, true);
				break;
			case DISCONNECTED:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.DISCONNECTED_NETWORK, true);

				break;
			case SUSPENDED:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.SUSPENDED_NETWORK, true);
				break;
			case UNKNOWN:
				writeTraceLineToAROTraceFile(AroTraceFileConstants.UNKNOWN_NETWORK, true);
				break;
			}
		}
	}
}
