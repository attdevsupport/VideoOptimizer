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
 * Encapsulates the data from a user generated event. 
 */
public class TemperatureEvent implements Serializable {
	private static final long serialVersionUID = 1L;

	private double timeRecorded;
	private int celciusTemperature;
	private int farenheitTemperature;
	

	/**
	 * Initializes an instance of the UserEvent class, using the specified event type, 
	 * press time, and release time.
	 * 
	 * @param eventType The event type. One of the values of the UserEventType enumeration.
	 * @param pressTime The time at which the event was initiated (such as a key being pressed down).
	 * @param releaseTime The time at which the event was ended (such as a key being released).
	 */
	public TemperatureEvent(double timeRecorded, int celciusTemperature, int farenheitTemperature) {
		this.timeRecorded = timeRecorded;
		this.celciusTemperature = celciusTemperature;
		this.farenheitTemperature = farenheitTemperature;
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
	 * Returns the temperature in celcius
	 * 
	 * @return celcius temperature.
	 */
	public int getcelciusTemperature() {
		return celciusTemperature;
	}

	/**
	 * Returns the temperature in farenheit 
	 * 
	 * @return farenheit temperature.
	 */
	public int getfarenheitTemperature() {
		return farenheitTemperature;
	}

	/**
	 * Sets the temperature in celcius. 
	 * 
	 * @param celcius temperature.
	 * 
	 */
	public void setcelciusTemperature(int dCelciusTemperature) {
		celciusTemperature = dCelciusTemperature;
	}

	/**
	 * Sets the temperature in farenheit. 
	 * 
	 * @param farenheit temperature.
	 * 
	 */
	public void setfarenheitTemperature(int dFarenheitTemperature) {
		farenheitTemperature = dFarenheitTemperature;
	}
}
