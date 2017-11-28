package com.att.aro.core.peripheral.pojo;

/*
 * Model to hold the attributes of the attenuation collections
 */

public class AttenuatorModel {
		
		private int delayUS;
		private int delayDS;
		private int throttleUL;
		private int throttleDL;				
		private String localPath = "";
		private String atnrProfileName = "";
		private boolean loadProfile;
		private boolean freeThrottle;
		private boolean constantThrottle;
		private boolean throttleDLEnable;
		private boolean throttleULEnable;
		
		public boolean isConstantThrottle() {
			return constantThrottle;
		}
		public void setConstantThrottle(boolean constantThrottle) {
			this.constantThrottle = constantThrottle;
		}
		public int getDelayUS() {
			return delayUS;
		}
		public void setDelayUS(int delayUS) {
			this.delayUS = delayUS;
		}
		public int getDelayDS() {
			return delayDS;
		}
		public void setDelayDS(int delayDS) {
			this.delayDS = delayDS;
		}
		public String getLocalPath() {
			return localPath;
		}
		public void setLocalPath(String localPath) {
			this.localPath = localPath;
		}
		public String getAtnrProfileName() {
			return atnrProfileName;
		}
		public void setAtnrProfileName(String atnrProfileName) {
			this.atnrProfileName = atnrProfileName;
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
		public boolean isLoadProfile() {
			return loadProfile;
		}
		public void setLoadProfile(boolean loadProfile) {
			this.loadProfile = loadProfile;
		}
		public boolean isFreeThrottle() {
			return freeThrottle;
		}
		public void setFreeThrottle(boolean freeThrottle) {
			this.freeThrottle = freeThrottle;
		}
		
		/**
		 * The method is used for recording the user's choice for throttle check box
		 * @return boolean
		 */
		public boolean isThrottleDLEnable() {
			return throttleDLEnable;
		}
		public void setThrottleDLEnable(boolean throttleDLEnable) {
			this.throttleDLEnable = throttleDLEnable;
		}
		public boolean isThrottleULEnable() {
			return throttleULEnable;
		}
		public void setThrottleULEnable(boolean throttleULEnable) {
			this.throttleULEnable = throttleULEnable;
		}
		 
}
