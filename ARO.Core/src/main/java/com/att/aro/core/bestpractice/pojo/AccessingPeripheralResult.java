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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AccessingPeripheralResult extends AbstractBestPracticeResult {
	private double activeGPSRatio = 0.0;
	private double activeGPSDuration = 0.0;
	private double activeBluetoothRatio = 0.0;
	private double activeBluetoothDuration = 0.0;
	private double activeCameraRatio = 0.0;
	private double activeCameraDuration = 0.0;
	
	@JsonIgnore
	private String exportAllGPSDesc;
	@JsonIgnore
	private String exportAllBTDesc;
	@JsonIgnore
	private String exportAllCamDesc;
	
	public double getActiveGPSRatio() {
		return activeGPSRatio;
	}

	public void setActiveGPSRatio(double activeGPSRatio) {
		this.activeGPSRatio = activeGPSRatio;
	}

	public double getActiveBluetoothRatio() {
		return activeBluetoothRatio;
	}

	public void setActiveBluetoothRatio(double activeBluetoothRatio) {
		this.activeBluetoothRatio = activeBluetoothRatio;
	}

	public double getActiveCameraRatio() {
		return activeCameraRatio;
	}

	public void setActiveCameraRatio(double activeCameraRatio) {
		this.activeCameraRatio = activeCameraRatio;
	}
	
	public double getActiveGPSDuration() {
		return activeGPSDuration;
	}

	public void setActiveGPSDuration(double activeGPSDuration) {
		this.activeGPSDuration = activeGPSDuration;
	}

	public double getActiveBluetoothDuration() {
		return activeBluetoothDuration;
	}

	public void setActiveBluetoothDuration(double activeBluetoothDuration) {
		this.activeBluetoothDuration = activeBluetoothDuration;
	}

	public double getActiveCameraDuration() {
		return activeCameraDuration;
	}

	public void setActiveCameraDuration(double activeCameraDuration) {
		this.activeCameraDuration = activeCameraDuration;
	}

	
	public String getExportAllGPSDesc() {
		return exportAllGPSDesc;
	}

	public void setExportAllGPSDesc(String exportAllGPSDesc) {
		this.exportAllGPSDesc = exportAllGPSDesc;
	}

	public String getExportAllBTDesc() {
		return exportAllBTDesc;
	}

	public void setExportAllBTDesc(String exportAllBTDesc) {
		this.exportAllBTDesc = exportAllBTDesc;
	}

	public String getExportAllCamDesc() {
		return exportAllCamDesc;
	}

	public void setExportAllCamDesc(String exportAllCamDesc) {
		this.exportAllCamDesc = exportAllCamDesc;
	}

	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.ACCESSING_PERIPHERALS;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		AccessingPeripheralResult peripheralResult = (AccessingPeripheralResult) obj;
		if (Double.doubleToLongBits(activeBluetoothDuration) != Double
				.doubleToLongBits(peripheralResult.getActiveBluetoothDuration())
				|| Double.doubleToLongBits(activeBluetoothRatio) != Double
						.doubleToLongBits(peripheralResult.getActiveBluetoothRatio())) {
			return false;
		}
		if (Double.doubleToLongBits(activeCameraDuration) != Double
				.doubleToLongBits(peripheralResult.getActiveCameraDuration())
				|| Double.doubleToLongBits(activeCameraRatio) != Double
						.doubleToLongBits(peripheralResult.getActiveCameraRatio())) {
			return false;
		}
		if (Double.doubleToLongBits(activeGPSDuration) != Double
				.doubleToLongBits(peripheralResult.getActiveGPSDuration())
				|| Double.doubleToLongBits(activeGPSRatio) != Double
						.doubleToLongBits(peripheralResult.getActiveGPSRatio())) {
			return false;
		}
		if ((!peripheralResult.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != peripheralResult.getResultType()) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(activeBluetoothDuration);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(activeBluetoothRatio);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(activeCameraDuration);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(activeCameraRatio);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(activeGPSDuration);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(activeGPSRatio);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		
		return result;
	}
}
