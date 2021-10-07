/*
 *
 *   Copyright 2017 AT&T
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package com.att.arotracedata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.att.arocollector.R;
import com.att.arocollector.utils.AROCollectorUtils;

public class AROGpsMonitorService extends Service {

	private static final String TAG = "AROGpsMonitorService";
	public static final String ARO_GPS_MONITOR_SERVICE = "com.att.arocollector.ARO_GPS_MONITOR_SERVICE";

	private static final String OUTGPSFILENAME = "gps_events";

	private static final String OUTLOCATIONFILENAME = "location_events";

	private static final String PRIVATEDATEFILENAME = "private_data";

	private LocationManager mGPSStatesManager;

	private GpsStatus.Listener mGPSStatesListner;

	private boolean prevGpsEnabledState = false;
	
	private Timer checkLocationService = new Timer();

	private Boolean mGPSActive = false;

	private static int HALF_SECOND_TARCE_TIMER_REPATE_TIME = 1000;

	private AROCollectorUtils mAroUtils;

	private File traceDir;
	private String outputTraceFile;
	private FileOutputStream outputTraceFileStream;
	private BufferedWriter bufferedWriter;
	private boolean writeEvents = false;

	private String outputLocationTraceFile;
	private FileOutputStream outputLocationTraceFileStream;
	private BufferedWriter bufferedLocationWriter;

	Map<LatLong, String> map = new HashMap<>();

	/**
	 * Setup and start monitoring
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand(...)");

		if (mAroUtils == null) {
			mAroUtils = new AROCollectorUtils();
			initFiles(intent);
			createTraceFiles(OUTGPSFILENAME, OUTLOCATIONFILENAME);

			startAROGpsTraceMonitor();
			startAROLocationPassiveTraceMonitor();
		}
		return super.onStartCommand(intent, flags, startId);

	}

	/**
	 * read Intent for trace directory
	 *
	 * @param intent
	 */
	private void initFiles(Intent intent) {
		if (intent != null) {
			Log.i(TAG, "initFiles(Intent " + intent.toString() + ") hasExtras = " + intent.getExtras());
			String traceDirStr = intent.getStringExtra("TRACE_DIR");
			traceDir = new File(traceDirStr);
			traceDir.mkdir();
		} else {
			Log.i(TAG, "intent is null");
		}
	}

	/**
	 * Create deviceinfo file store info
	 */
	private void createTraceFiles(String traceGPSFileName, String traceLocationFileName) {

		try {
			outputTraceFile = traceDir + "/" + traceGPSFileName;
			outputTraceFileStream = new FileOutputStream(outputTraceFile, true);
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputTraceFileStream));
		} catch (FileNotFoundException e) {
			outputTraceFileStream = null;
		}

		try {
			outputLocationTraceFile = traceDir + "/" + traceLocationFileName;
			outputLocationTraceFileStream = new FileOutputStream(outputLocationTraceFile, true);
			bufferedLocationWriter = new BufferedWriter(new OutputStreamWriter(outputLocationTraceFileStream));
		} catch (FileNotFoundException e) {
			outputLocationTraceFileStream = null;
		}
	}

	/**
	 * Close the trace file
	 */
	public void closeTraceFiles() {

		Log.i(TAG, "Close TraceFile :" + outputTraceFile + " & " + outputLocationTraceFile);
		try {
			bufferedWriter.close();
			outputTraceFileStream.close();
		} catch (IOException e) {
			Log.e(TAG, "closeTraceFile() <" + outputTraceFile + "> IOException :" + e.getMessage());
		}

		try {
			bufferedLocationWriter.close();
			outputLocationTraceFileStream.close();
		} catch (IOException e) {
			Log.e(TAG, "closeTraceFile() <" + outputLocationTraceFile + "> IOException :" + e.getMessage());
		}
	}

	@Override
	public boolean stopService(Intent name) {
		Log.i(TAG, "stopService()");
		return super.stopService(name);
	}

	/**
	 * stop monitoring and close trace file
	 */
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
		stopAROGpsTraceMonitor();
		stopAROLocationTraceMonitor();
		closeTraceFiles();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "onBind(...)");
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Captures the GPS trace data during the trace cycle
	 */
	private class GPSStatesListener implements GpsStatus.Listener {

		@Override
		public void onGpsStatusChanged(int event) {

			switch (event) {

			case GpsStatus.GPS_EVENT_STARTED:

				writeTraceLineToAROTraceFile(bufferedWriter, "ACTIVE", true);
				mGPSActive = true;
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				writeTraceLineToAROTraceFile(bufferedWriter, AroTraceFileConstants.STANDBY, true);
				mGPSActive = false;
				break;
			}
		}
	}

	/**
	 * Starts the GPS peripherals trace collection
	 */
	private void startAROGpsTraceMonitor() {
		Log.i(TAG, "startAROGpsTraceMonitor()");
		mGPSStatesManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mGPSStatesListner = new GPSStatesListener();
		mGPSStatesManager.addGpsStatusListener(mGPSStatesListner);

		// write the initial gps state to the trace file
		final boolean initialGpsState = isLocationServiceEnabled();
		writeGpsStateToTraceFile(initialGpsState);
		prevGpsEnabledState = initialGpsState;

		checkLocationService.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// Current GPS enabled state
				final boolean currentGpsEnabledState = isLocationServiceEnabled();
				if (currentGpsEnabledState != prevGpsEnabledState) {
					writeGpsStateToTraceFile(currentGpsEnabledState);
				}
				prevGpsEnabledState = currentGpsEnabledState;
			}
		}, HALF_SECOND_TARCE_TIMER_REPATE_TIME, HALF_SECOND_TARCE_TIMER_REPATE_TIME);
	}

	private void startAROLocationPassiveTraceMonitor() {
		if (mGPSStatesManager == null) {
			mGPSStatesManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
		mGPSStatesManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
	}

	/**
	 * write the gps state to trace file
	 *
	 * @param currentGpsEnabledState
	 */
	private void writeGpsStateToTraceFile(final boolean currentGpsEnabledState) {
		if (currentGpsEnabledState) {
			Log.d(TAG, "gps enabled: ");
			if (!mGPSActive) {
				writeTraceLineToAROTraceFile(bufferedWriter, AroTraceFileConstants.STANDBY, true);
			}
		} else {
			Log.d(TAG, "gps Disabled: ");
			writeTraceLineToAROTraceFile(bufferedWriter, AroTraceFileConstants.OFF, true);
		}
	}

	/**
	 * Stop the GPS peripherals trace collection
	 */
	private void stopAROGpsTraceMonitor() {
		Log.i(TAG, "stopAROGpsTraceMonitor()");
		if (mGPSStatesListner != null) {
			mGPSStatesManager.removeGpsStatusListener(mGPSStatesListner);
		}
		checkLocationService.cancel();
	}

	/**
	 * Stop the location passive provider collection
	 */
	private void stopAROLocationTraceMonitor() {
		Log.i(TAG, "stopAROLocationTraceMonitor()");
		if (mGPSStatesManager != null) {
			mGPSStatesManager.removeUpdates(locationListener);
			map.clear();
			mGPSStatesManager = null;
		}
	}

	/**
	 * Checks if the GPS radio is turned on and receiving fix
	 *
	 * @return boolean value to represent if the location service is enabled or
	 *         not
	 */
	private boolean isLocationServiceEnabled() {
		boolean enabled = false;
		// first, make sure at least one provider actually exists
		final LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		final boolean gpsExists = (lm.getProvider(LocationManager.GPS_PROVIDER) != null);
		final boolean networkExists = (lm.getProvider(LocationManager.NETWORK_PROVIDER) != null);
		if (gpsExists || networkExists) {
			enabled = ((!gpsExists || lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
					&& (!networkExists || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)));
		}
		return enabled;
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
			Log.e(TAG, "exception in writeTraceLineToAROTraceFile", e);
		}
	}

	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				saveLocationInfo(location);
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}
	};

	private void saveLocationInfo(Location location) {

		Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());
		String city = getResources().getString(R.string.unavailable);
		double dLatitude = location.getLatitude();
		double dLongitude = location.getLongitude();
		String provider = getResources().getString(R.string.unavailable);
		if (location.getProvider().length() > 0) {
			provider = location.getProvider();
		}

		LatLong latLong = new LatLong(location.getLatitude(), location.getLongitude());
		if (map.containsKey(latLong)) {
			city = map.get(latLong);
		} else {
			try {
				List<Address> addresses = new ArrayList<Address>();
				addresses = geo.getFromLocation(dLatitude, dLongitude, 1);
				if(addresses.size() > 0 && addresses.get(0).getLocality() != null &&
						addresses.get(0).getLocality().length() > 1) {
					city = addresses.get(0).getLocality();
					map.put(latLong, city);
				}
			} catch (IOException e) {
				Log.e(TAG, "Error getting city, " + e.getMessage());
			}
		}

		try {
			if (bufferedLocationWriter != null) {
				final String eol = System.getProperty("line.separator");
				bufferedLocationWriter.write(mAroUtils.getDataCollectorEventTimeStamp() + " " + dLatitude + " "
						+ dLongitude + " " + provider + " " + city + eol);
				if (!writeEvents) {

						writeToPrivateData(dLatitude, dLongitude, city, eol);
						writeEvents = true;
				}
				bufferedLocationWriter.flush();
			}
		} catch (IOException e) {
			Log.e(TAG, "exception in writeTraceLineToAROTraceFile", e);
		}
	}


 private void writeToPrivateData(double dLatitude, double dLongitude, String city, String eol) {
	 try (FileOutputStream privateDataStream = new FileOutputStream("" + "/" + "a", true);
		  OutputStreamWriter outputStreamWriter = new OutputStreamWriter(privateDataStream);
		  BufferedWriter buffereDataWriter = new BufferedWriter(outputStreamWriter)) {

			String keyWrd = "KEYWORD";
			String delimiter = ",";
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append(keyWrd);
			strBuilder.append(delimiter);
			strBuilder.append("Location (Longitude)");
			strBuilder.append(delimiter);
			strBuilder.append(dLongitude);
			strBuilder.append(delimiter);
			strBuilder.append("Y"); // yes - meaning will be searched for during
									// analysis
			strBuilder.append(eol);
			strBuilder.append(keyWrd);
			strBuilder.append(delimiter);
			strBuilder.append("Location (Latitude)");
			strBuilder.append(delimiter);
			strBuilder.append(dLatitude);	
			strBuilder.append(delimiter);
			strBuilder.append("Y"); // yes - meaning will be searched for during
									// analysis
			strBuilder.append(eol);
			strBuilder.append(keyWrd);
			strBuilder.append(delimiter);
			strBuilder.append("Location (City)");
			strBuilder.append(delimiter);
			strBuilder.append(city);
			strBuilder.append(delimiter);
			strBuilder.append("Y"); // yes - meaning will be searched for during
									// analysis
			strBuilder.append(eol);
			buffereDataWriter.write(strBuilder.toString());
			buffereDataWriter.flush();
		} catch (IOException excp) {
			Log.e(TAG, "exception in writing to private data file", excp);
		}
	}
}
