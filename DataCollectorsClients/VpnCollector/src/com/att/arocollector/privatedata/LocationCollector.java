/*
 *  Copyright 2017 AT&T
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

class LocationCollector extends AbstractDeviceDataCollector {

	private Context context;
	private LocationManager locationManager;
	private String longitude;
	private String latitude;
	private String city;
	
	private static final String TAG = LocationCollector.class.getSimpleName();
	
	private LocationListener locationListener = new LocationListener() {
		
		@Override
		public void onLocationChanged(Location location) {		
			if (location != null) {			
 				saveLocationInfo(location);
				locationManager.removeUpdates(this);
			}
		}
		
		@Override
		public void onProviderEnabled(String provider) {}
		
		@Override
		public void onProviderDisabled(String provider) {}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
	};

	LocationCollector(Context context, String dataFilePath) {
		
		super(dataFilePath);
		this.context = context;
		locationManager  = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	@Override
	List<NameValuePair> getData() {	
		
		getLocation();
		
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new NameValuePair(PrivateDataCollectionConst.LOCATION_LONGITUDE, longitude));		
		data.add(new NameValuePair(PrivateDataCollectionConst.LOCATION_LATITUDE, latitude));	
		data.add(new NameValuePair(PrivateDataCollectionConst.LOCATION_CITY, city));		
		
		Log.d(TAG, PrivateDataCollectionConst.LOCATION_LONGITUDE + ": " + longitude);
		Log.d(TAG, PrivateDataCollectionConst.LOCATION_LATITUDE + ": " + latitude);
		Log.d(TAG, PrivateDataCollectionConst.LOCATION_CITY + ": " + city);
		
		return data;
	}
	
	private void getLocation() {

		Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (lastLocation == null) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		} else {
			saveLocationInfo(lastLocation);	
		}		
	}
	
	private void saveLocationInfo(Location location) {
		
		Geocoder geo = new Geocoder(context, Locale.getDefault());
		List<Address> addresses;

		double longitude_d = location.getLongitude();	
		longitude = Double.toString(longitude_d);
		
		double latitude_d = location.getLatitude();		
		latitude = Double.toString(latitude_d);
		
		try {
		
			addresses = geo.getFromLocation(latitude_d, longitude_d, 1);
			city = addresses.get(0).getLocality();
			
		} catch (IOException e) {
			
			Log.e(TAG, "Error getting city, " + e.getMessage());
		}
		

	}
}
