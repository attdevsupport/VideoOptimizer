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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.att.arocollector.AROCollectorActivity;
import com.att.arocollector.R;
import com.att.arocollector.utils.AROCollectorUtils;
import com.att.arocollector.utils.AROLogger;
import com.google.android.material.snackbar.Snackbar;

import static android.telephony.PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED;

public class ARONetworkdDetailsReceiver extends AROBroadcastReceiver {

	private static final String TAG = "AROBearerReceiver";

	private ConnectivityManager connectivityManager;

	/**indicates whether WIFI, MOBILE, or UNKNOWN **/
	private String prevNetwork = AroTraceFileConstants.NOT_ASSIGNED_NETWORK;

	private int prevNetworkType;

	private String applicationVersion = null;

	private boolean isProduction = false;

	private boolean isFirstBearerChange = true;

	private int currentNetworkType;

	private int displayInfo;

	public ARONetworkdDetailsReceiver(Context context, File traceDir, String outFileName, AROCollectorUtils mAroUtils) throws FileNotFoundException {
		super(context, traceDir, outFileName, mAroUtils);

		Log.i(TAG, "AROBearerReceiver(...)");

		connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);

		final ConnectivityManager mAROConnectivityMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo mAROActiveNetworkInfo = mAROConnectivityMgr.getActiveNetworkInfo();
		currentNetworkType = getDeviceNetworkType(mAROActiveNetworkInfo);
		recordBearerAndNetworkChange(mAROActiveNetworkInfo, true);
	}

	public int getDeviceNetworkType() {
		return currentNetworkType;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "onReceive(...) action=" + action);
		this.context = context;
		if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			telephonyManager.listen(new ARONetworkdDetailsReceiver.MyPhoneListenerState(context), LISTEN_DISPLAY_INFO_CHANGED);
		}
		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			final boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
			final boolean isNetworkConnected = !noConnectivity;

			final ConnectivityManager mAROConnectivityMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo mAROActiveNetworkInfo = mAROConnectivityMgr.getActiveNetworkInfo();
			if (!isFirstBearerChange) {
				recordBearerAndNetworkChange(mAROActiveNetworkInfo, isNetworkConnected);
			}
		}

	}

	/**
	 * called by the mAROBearerChangeReceiver and mPhoneStateListener to record:
	 * 		1. bearer change between Wifi-Mobile
	 * 		2. network change between 4G-3G-2G
	 * @param mAROActiveNetworkInfo
	 * @param isNetworkConnected
	 */
	private void recordBearerAndNetworkChange(final NetworkInfo mAROActiveNetworkInfo, final boolean isNetworkConnected) {

		AROLogger.d(TAG, "enter recordBearerAndNetworkChange()");
		if (mAROActiveNetworkInfo != null
				&& isNetworkConnected
				&& getDeviceNetworkType(mAROActiveNetworkInfo) != TelephonyManager.NETWORK_TYPE_UNKNOWN) {

			String currentBearer = getCurrentBearer();
			currentNetworkType = getDeviceNetworkType(mAROActiveNetworkInfo);
			if (AROLogger.logDebug) {
				AROLogger.d(TAG, "mAROActiveNetworkInfo.state=" + mAROActiveNetworkInfo.getState());
				AROLogger.d(TAG, "mAROPrevBearer=" + prevNetwork + "; currentBearer=" + currentBearer);
				AROLogger.d(TAG, "mAROPrevNetworkType=" + prevNetworkType + "; currentNetworkType=" + currentNetworkType);
			}
			if (!prevNetwork.equals(currentBearer)) {
				//bearer change, signaling a failover
				prevNetwork = currentBearer;
				writeTraceLineToAROTraceFile(currentNetworkType+ " " + getDisplayInfo(), true);

				if (AROLogger.logDebug) {
					AROLogger.d(TAG, "failover, wrote networkType=" + currentNetworkType + " to networkdetails completed at timestamp: " + mAroUtils.getDataCollectorEventTimeStamp());
				}
				prevNetworkType = currentNetworkType;
			}
			//We need to handle case when we switch between 4G-3G-2G ( This is not as handover)
			//-1 - Wifi (We don't want to check for wifi network for 4G-3G-2G transition)
			else if (currentNetworkType != -1 && prevNetworkType != currentNetworkType) {
				writeTraceLineToAROTraceFile(currentNetworkType+ " " + getDisplayInfo(), true);
				if (AROLogger.logDebug) {
					AROLogger.d(TAG, "4g-3g-2g switch, wrote networkType=" + currentNetworkType + " to networkdetails completed at timestamp: " + mAroUtils.getDataCollectorEventTimeStamp());
				}
				//log the 4G-3G-2G network switch
				prevNetworkType = currentNetworkType;
			}
			// device_details trace file
			if (isFirstBearerChange) {
				isFirstBearerChange = false;
			}

		} else {
			AROLogger.d(TAG, "mAROActiveNetworkInfo is null, network is not CONNECTED, or networkType is unknown...exiting recordBearerAndNetworkChange()");
		}
	}

	/**
	 * Gets the current connected bearer
	 *
	 * @return boolean value to validate if current bearer is wifi
	 */
	private Boolean getifCurrentBearerWifi() {
		int type = 0;
		if (connectivityManager == null)
			return false;
		if (connectivityManager.getActiveNetworkInfo() != null) {
			type = connectivityManager.getActiveNetworkInfo().getType();
		}
		if (type == ConnectivityManager.TYPE_MOBILE) {
			AROLogger.d(TAG, " Connection Type :  Mobile");
			return false;
		} else {
			AROLogger.d(TAG, " Connection Type :  Wifi");
			return true;
		}
	}

	/**
	 * returns the value of the current bearer, either WIFI or MOBILE
	 */
	private String getCurrentBearer() {

		return getifCurrentBearerWifi() ? "WIFI" : "MOBILE";
	}

	/**
	 * Gets the current connected data network type of device i.e 3G/LTE/Wifi
	 * @param mCurrentNetworkType network info class object to get current network type 
	 * @return mCellNetworkType Current network type
	 */
	private int getDeviceNetworkType(NetworkInfo mCurrentNetworkType) {
		if (AROLogger.logDebug) {
			AROLogger.d(TAG, "getting device network type" + mCurrentNetworkType);
		}
		int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
		final TelephonyManager mAROtelManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
		if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				networkType = mAROtelManager.getDataNetworkType();
			}
		}

		// Check if the current network is WiFi *//
		if (mCurrentNetworkType.getType() == 1) {
			networkType = -1;
		}
		return networkType;
			
	}

	public int getDisplayInfo() {
 		return displayInfo;
	}

	public void setDisplayInfo(int displayInfo) {
		this.displayInfo = displayInfo;
	}

	public class MyPhoneListenerState extends PhoneStateListener {
		Context myContext;

		public MyPhoneListenerState(Context context){
			super();
			myContext = context;
		}

		/**
		 * TelephonyDisplayInfo  contains telephony-related information used for display purposes only.
		 * This information is provided in accordance with carrier policy and branding preferences;
		 * it is not necessarily a precise or accurate representation of the current state and should be treated accordingly.
		 */
		@RequiresApi(api = Build.VERSION_CODES.R)
		@Override
		public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo) {
			if (ActivityCompat.checkSelfPermission( myContext , Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
			super.onDisplayInfoChanged(telephonyDisplayInfo);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				setDisplayInfo(telephonyDisplayInfo.getOverrideNetworkType());
				AROLogger.d(TAG,"display the result: "+ getDisplayInfo());
			}
		}

	}

}
