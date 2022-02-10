package com.att.aro.core.peripheral.pojo;

import com.att.aro.core.datacollector.DataCollectorType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * Model to hold the attributes of the attenuation collections
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)				// allows for changes dropping items or using older versions
@JsonInclude(Include.NON_NULL)
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

		@Getter(AccessLevel.NONE)
		private boolean throttleDLEnable;

		@Getter(AccessLevel.NONE)
		private boolean throttleULEnable;
		
		private DataCollectorType deviceType;
		
		/**
		 * The method is used for recording the user's choice for throttle check box
		 * @return boolean
		 */
		public boolean isThrottleDLEnabled() {
			return throttleDLEnable;
		}
		
		public boolean isThrottleULEnabled() {
			return throttleULEnable;
		}
		 
}
