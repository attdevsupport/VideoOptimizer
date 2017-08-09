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

import static java.lang.Integer.parseInt;

import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.att.aro.core.peripheral.impl.CollectOptionsReaderImpl;
import com.att.aro.core.video.pojo.Orientation;

public class CollectOptions {
	private static Logger logger = Logger.getLogger(CollectOptionsReaderImpl.class.getName());

	private int dsDelay = 0;// download stream
	private int usDelay = 0;// upload stream
	private int throttleDL = 0;
	private int throttleUL = 0;
	private boolean attnrProfile = false;
	private String attnrProfileName = "";
	private SecureStatus secureStatus = SecureStatus.UNKNOWN;
	private int totalLines = 0;
	private Orientation orientation = Orientation.PORTRAIT;

	public CollectOptions() {

	}

	public CollectOptions(int dsDelay, int usDelay, int throttleDL, int throttleUL, boolean attnrProfile,
			String attnrProfileName, SecureStatus secureStatus, int totalLines, Orientation orientation) {
		super();
		this.dsDelay = dsDelay;
		this.usDelay = usDelay;
		this.throttleDL = throttleDL;
		this.throttleUL = throttleUL;
		this.attnrProfile = attnrProfile;
		this.attnrProfileName = attnrProfileName;
		this.secureStatus = secureStatus;
		this.totalLines = totalLines;
		this.orientation = orientation;
	}

	public CollectOptions(Properties properties) {
		HashMap <String,String> hashMap = new HashMap <String,String>();
		hashMap.put("dsDelay", "0");
		hashMap.put("usDelay", "0");
		hashMap.put("throttleDL", "0");
		hashMap.put("throttleUL", "0");
		hashMap.put("secure", "UNKNOWN");
		hashMap.put("orientation", "PORTRAIT");
		hashMap.put("attnrProfile", "false");
		hashMap.put("attnrProfileName", "");
		
		for(String key: hashMap.keySet()){
			String temp = properties.getProperty(key);
			if(!"".equals(temp)){
				if("dsDelay" .equals(key)){
					dsDelay = parseInt(properties.getProperty("dsDelay", "0"));
					
				}else if ("usDelay".equals(key)){
					usDelay = parseInt(properties.getProperty("usDelay", "0"));
					
				}else if("throttleDL".equals(key)){
					throttleDL = parseInt(properties.getProperty("throttleDL", "0"));
					
				}else if("throttleUL".equals(key)){
					throttleUL = parseInt(properties.getProperty("throttleUL", "0"));
					
				}else if("secure".equals(key)){
					secureStatus = SecureStatus.valueOf(properties.getProperty("secure", "UNKNOWN").toUpperCase());
					
				}else if("orientation".equals(key)){
					orientation = Orientation.valueOf(properties.getProperty("orientation", "PORTRAIT").toUpperCase());
					
				}else if("attnrProfile".equals(key)){
					attnrProfile = Boolean.valueOf(properties.getProperty("attnrProfile", "false"));
					
				}else if("attnrProfileName".equals(key)){
					attnrProfileName = properties.getProperty("attnrProfileName", "");
					
				}else{
					logger.info("Collection options: " + "wrong file format or wrong content");
				}
 			}
		}

	}

	public String getOrientation() {
		return orientation.name();
	}

	public void setOrientation(String orientation) {
		this.orientation = Orientation.valueOf(orientation);
	}

	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}

	public enum SecureStatus {
		UNKNOWN, TRUE, FALSE;
	}

	public int getUsDelay() {
		return usDelay;
	}

	public void setUsDelay(int usDelay) {
		this.usDelay = usDelay;
	}

	public SecureStatus getSecureStatus() {
		return secureStatus;
	}

	public void setSecureStatus(SecureStatus secureStatus) {
		this.secureStatus = secureStatus;
	}

	public int getDsDelay() {
		return dsDelay;
	}

	public void setDsDelay(int dsDelay) {
		this.dsDelay = dsDelay;
	}

	public int getThrottleDL() {
		return throttleDL;
	}

	public void setThrottleDL(int throttleDL) {
		this.throttleDL = throttleDL;
	}

	public int getThrottleUL() {
		return throttleUL;
	}

	public void setThrottleUL(int throttleUL) {
		this.throttleUL = throttleUL;
	}

	public int getTotalLines() {
		return totalLines;
	}

	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}

	public boolean isAttnrProfile() {
		return attnrProfile;
	}

	public void setAttnrProfile(boolean attnrProfile) {
		this.attnrProfile = attnrProfile;
	}

	public String getAttnrProfileName() {
		return attnrProfileName;
	}

	public void setAttnrProfileName(String attnrProfileName) {
		this.attnrProfileName = attnrProfileName;
	}

}
