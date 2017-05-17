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
package com.att.arocollector.privatedata;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AROPrivateDataCollectorService extends Service{

	private static final String TAG = AROPrivateDataCollectorService.class.getSimpleName();
	public static final String ARO_PRIVATE_DATA_COLLECTOR_SERVICE = "com.att.arocollector.privatedata.ARO_PRIVATE_DATA_COLLECTOR_SERVICE";
	private static final String PRIVATEDATA_FILE = "private_data";

	private String dataFilePath;
	private List<AbstractDeviceDataCollector> collectors;

	/**
	 * Setup and start monitoring
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand(...)");
		dataFilePath = intent.getStringExtra("TRACE_DIR") + "/" + PRIVATEDATA_FILE;
		collectors = new ArrayList<AbstractDeviceDataCollector>();
		cleanUpFile();
		collect();
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void cleanUpFile() {
		
		File privateDataFile = new File(dataFilePath);
		
		if (privateDataFile.exists() && !privateDataFile.delete()) {
				
			Log.e(TAG, "Deleting " + PRIVATEDATA_FILE + " failed, "
					+ "file may contain data collected from previous collections.");			
		}
	}

	/**
	 * Currently, we are collecting 
	 * 1) IMEI
	 * 2) MAC Address
	 * 3) Phone Number
	 * 4) Contact Information (Name, Phone Number and Email)
	 */
	public void collect() {
			
		collectors.add(new IMEICollector(getApplicationContext(), dataFilePath));
		collectors.add(new MACAddressCollector(getApplicationContext(), dataFilePath));
		collectors.add(new PhoneNumberCollector(getApplicationContext(), dataFilePath));
		collectors.add(new ContactsDataCollector(getApplicationContext(), dataFilePath, 
				PrivateDataCollectionConfig.NUMBER_OF_CONTACTS_TO_COLLECT));
		
		for (AbstractDeviceDataCollector collector: collectors) {
			collector.collect();
		}
		this.stopSelf();
	}

	@Override
	public boolean stopService(Intent name) {
		Log.i(TAG, "stopService()");
		return super.stopService(name);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "onBind(...)");
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * stop monitoring and close trace file
	 */
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
		super.onDestroy();
	}
}
