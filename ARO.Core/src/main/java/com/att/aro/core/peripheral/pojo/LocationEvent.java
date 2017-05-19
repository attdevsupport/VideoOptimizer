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
package com.att.aro.core.peripheral.pojo;

import java.io.Serializable;

/**
 * Encapsulates the data from a passive provider location generated event. 
 */
public class LocationEvent implements Serializable {
	private static final long serialVersionUID = 1L;

	private double timeRecorded;
	private double latitude;
	private double longitude;
	private String provider;
	private String locality;
	
	/**
	 * Initializes an instance of the LocationEvent class, using the latitude, longitude & locality.
	 * 
	 * @param timeRecorded
	 * @param latitude
	 * @param longitude
	 * @param provider
	 * @param locality
	 */
	public LocationEvent(double timeRecorded, double latitude, double longitude, String provider, String locality) {
		this.timeRecorded = timeRecorded;
		this.latitude = latitude;
		this.longitude = longitude;
		this.provider = provider;
		this.locality = locality;
	}
	
	/**
	 * Returns the time at which the event was initiated (such as a key being pressed down).
	 * 
	 * @return The press time.
	 */
	public double getTimeRecorded() {
		return timeRecorded;
	}

	/**
	 * Returns the latitude
	 * 
	 * @return latitude.
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Returns the longitude 
	 * 
	 * @return longitude.
	 */
	public double getLongitude() {
		return longitude;
	}
	
	/**
	 * Returns the provider 
	 * 
	 * @return provider - NETWORK/GPS.
	 */
	public String getProvider() {
		return provider;
	}
	
	/**
	 * Returns the locality 
	 * 
	 * @return locality.
	 */
	public String getLocality() {
		return locality;
	}

	/**
	 * Sets the latitude. 
	 * 
	 * @param latitude.
	 * 
	 */
	public void setLatitude(double dLatitude) {
		latitude = dLatitude;
	}

	/**
	 * Sets the longitude. 
	 * 
	 * @param longitude.
	 * 
	 */
	public void setLongitude(double dLongitude) {
		longitude = dLongitude;
	}
	
	/**
	 * Sets the provider. 
	 * 
	 * @param provider.
	 * 
	 */
	public void setProvider(String sProvider) {
		provider = sProvider;
	}
	
	/**
	 * Sets the locality. 
	 * 
	 * @param locality.
	 * 
	 */
	public void setLongitudee(String sLocality) {
		locality = sLocality;
	}
}
