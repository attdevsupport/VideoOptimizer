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
package com.att.arocollector.privatedata;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

class PhoneNumberCollector extends AbstractDeviceDataCollector {

	private TelephonyManager teleManager;
	private static final String TAG = PhoneNumberCollector.class.getSimpleName();
	
	PhoneNumberCollector(Context context, String dataFilePath) {
	
		super(dataFilePath);
		teleManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}
	
	@Override
	List<NameValuePair> getData() {	
		
		String phoneNumber = teleManager.getLine1Number();
		Log.d(TAG, "Device Phone Number: " + phoneNumber);
		
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new NameValuePair(PrivateDataCollectionConst.DEVICE_PHONE_NUMBER, phoneNumber));		
		return data;
	}
}
