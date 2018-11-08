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
package com.att.aro.core.bestpractice.pojo;

public class BufferOccupancyResult extends AbstractBestPracticeResult {

	private double maxBuffer = 0.0;
	private double minBufferByte = 0.0;
	private double avgBufferByte = 0.0;

	private double maxBufferTime = 0.0;
	private double minBufferTime = 0.0;
	private double avgBufferTime = 0.0;

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.BUFFER_OCCUPANCY;
	}

	public double getMaxBuffer() {
		return maxBuffer;
	}

	public void setMaxBuffer(double maxBuffer) {
		this.maxBuffer = maxBuffer;
	}

	public void setMinBufferByte(double minBuffer) {
		this.minBufferByte = minBuffer;
	}

	public void setAvgBufferByte(double avgBufferByte) {
		this.avgBufferByte = avgBufferByte;
	}

	public double getMinBufferByte() {
		return minBufferByte;
	}

	public double getAvgBufferByte() {
		return avgBufferByte;
	}

	public double getMaxBufferTime() {
		return maxBufferTime;
	}

	public void setMaxBufferTime(double maxBufferTime) {
		this.maxBufferTime = maxBufferTime;
	}

	public double getMinBufferTime() {
		return minBufferTime;
	}

	public void setMinBufferTime(double minBufferTime) {
		this.minBufferTime = minBufferTime;
	}

	public double getAvgBufferTime() {
		return avgBufferTime;
	}

	public void setAvgBufferTime(double avgBufferTime) {
		this.avgBufferTime = avgBufferTime;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		BufferOccupancyResult bufferResult = (BufferOccupancyResult) obj;
		if (Double.doubleToLongBits(avgBufferByte) != Double.doubleToLongBits(bufferResult.getAvgBufferByte())
				|| Double.doubleToLongBits(avgBufferTime) != Double.doubleToLongBits(bufferResult.getAvgBufferTime())) {
			return false;
		}
		if (Double.doubleToLongBits(maxBuffer) != Double.doubleToLongBits(bufferResult.getMaxBuffer())
				|| Double.doubleToLongBits(maxBufferTime) != Double.doubleToLongBits(bufferResult.getMaxBufferTime())) {
			return false;
		}
		if (Double.doubleToLongBits(minBufferByte) != Double.doubleToLongBits(bufferResult.getMinBufferByte())
				|| Double.doubleToLongBits(minBufferTime) != Double.doubleToLongBits(bufferResult.getMinBufferTime())) {
			return false;
		}
		if ((!bufferResult.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != bufferResult.getResultType()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(avgBufferByte);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(avgBufferTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxBuffer);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxBufferTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minBufferByte);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minBufferTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
