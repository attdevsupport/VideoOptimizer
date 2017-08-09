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
package com.att.aro.core.videoanalysis.pojo;

public class VideoBufferData {
	
	private String bufferType;
	private double average;
	private double minimum;
	private double maximum;

	public VideoBufferData( String bufferType,
							double average,
							double minimum,
							double maximum) {
		this.bufferType = bufferType;
		this.average = average;
		this.minimum = minimum;
		this.maximum = maximum;
	}
	

	public String getBufferType() {
		return bufferType;
	}

	public void setBufferType(String bufferType) {
		this.bufferType = bufferType;
	}

	public double getAverage() {
		return average;
	}
	
	public void setAverage(double byteAverage) {
		this.average = byteAverage;
	}
	
	public double getMinimum() {
		return minimum;
	}
	
	public void setMinimum(double byteMinimum) {
		this.minimum = byteMinimum;
	}

	public double getMaximum() {
		return maximum;
	}

	public void setMaximum(double byteMaximum) {
		this.maximum = byteMaximum;
	}
}
