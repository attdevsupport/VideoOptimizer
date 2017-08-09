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
package com.att.aro.ui.model.overview;

public class TraceInfo {
	private String dateValue ="";
	private String traceValue="";
	private Integer byteCountTotal = new Integer(0);
	private String networkType = "";
	private String profileValue = "";
	private Integer downlinkValue = new Integer(0);
	private Integer uplinkValue = new Integer(0);
 
	private boolean attnrProfile = false;
	private String attnrProfileName = "";

	public String getDateValue() {
		return dateValue;
	}
	public void setDateValue(String dateValue) {
		this.dateValue = dateValue;
	}
	public String getTraceValue() {
		return traceValue;
	}
	public void setTraceValue(String traceValue) {
		this.traceValue = traceValue;
	}
	public Integer getByteCountTotal() {
		return byteCountTotal;
	}
	public void setByteCountTotal(Integer byteCountTotal) {
		this.byteCountTotal = byteCountTotal;
	}
	public String getNetworkType() {
		return networkType;
	}
	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}
	public String getProfileValue() {
		return profileValue;
	}
	public void setProfileValue(String profileValue) {
		this.profileValue = profileValue;
	}
	public Integer getDownlinkValue() {
		return downlinkValue;
	}
	public void setDownlinkValue(Integer downlinkValue) {
		this.downlinkValue = downlinkValue;
	}
	
	public Integer getUplinkValue() {
		return uplinkValue;
	}
	public void setUplinkValue(Integer uplinkValue) {
		this.uplinkValue = uplinkValue;
	}
	public boolean isAttnr_Profile() {
		return attnrProfile;
	}
	public void setAttnr_Profile(boolean attnr_Profile) {
		this.attnrProfile = attnr_Profile;
	}
	public String getAttnrProfileName() {
		return attnrProfileName;
	}
	public void setAttnrProfileName(String attnrProfileName) {
		this.attnrProfileName = attnrProfileName;
	}

}
