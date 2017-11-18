package com.att.aro.core.peripheral.pojo;

import static java.lang.Integer.parseInt;

import java.util.Properties;

import com.att.aro.core.video.pojo.Orientation;

public class CollectOptions {
	
	private int dsDelay = 0;
	private int usDelay = 0;
	// -1 is no speed throttle (and default) 
	private int throttleDL = -1;  
	private int throttleUL = -1; 
 	private boolean attnrProfile = false;
	private String attnrProfileName = "";
	private SecureStatus secureStatus = SecureStatus.UNKNOWN;
	private int totalLines = 0;
	private Orientation orientation = Orientation.PORTRAIT;

	public CollectOptions() {
	}


	public CollectOptions(Properties properties) {
		dsDelay = parseInt(properties.getProperty("dsDelay", "0"));
		usDelay = parseInt(properties.getProperty("usDelay", "0"));
		throttleDL = parseInt(properties.getProperty("throttleDL", "0"));
		throttleUL = parseInt(properties.getProperty("throttleUL", "0"));
 		secureStatus = SecureStatus.valueOf(properties.getProperty("secure", "UNKNOWN").toUpperCase());
		orientation = Orientation.valueOf(properties.getProperty("orientation", "PORTRAIT").toUpperCase());
		attnrProfile = Boolean.valueOf(properties.getProperty("attnrProfile", "false"));
		attnrProfileName = properties.getProperty("attnrProfileName", "");
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
