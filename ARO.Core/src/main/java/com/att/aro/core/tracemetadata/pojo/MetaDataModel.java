package com.att.aro.core.tracemetadata.pojo;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

/**
 * USED FOR FINDING and retrieving results/Raw data
 * 
 * Date of trace is startUTC
 *  search exact or range
 *  
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)				// allows for changes dropping items or using older versions
@JsonInclude(Include.NON_NULL)
public class MetaDataModel {
	@Nonnull private String description = "";            
	@Nonnull private String traceType = "";               
	@Nonnull private String targetedApp = "";                             
	@Nonnull private String applicationProducer = "";             
	
	// auto filled in after running trace and analyzing, before results export
	@Nonnull private String deviceOrientation = "";     // PORTRAIT/LANDSCAPE                                                                                                            
	@Nonnull private String phoneMake = "";                                                                                                            
	@Nonnull private String phoneModel = "";                        
	@Nonnull private String os = "";              		// Android/iOS                       
	@Nonnull private String osVersion = "";                                          
	@Nonnull private String targetAppVer = "";          

	@Nonnull 
	@Getter(AccessLevel.NONE) private String startUTC = "";    
	
	@Nonnull private String traceDuration = "";   
	@Nonnull private String traceSource = "";           // MANUAL/MACHINE  = "";
	@Nonnull private String traceOwner = "";            
	
	public String getStartUTC() {
		try{
			ZonedDateTime.parse(startUTC, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return startUTC;
		} catch(DateTimeParseException ex) {
			try {
				ZonedDateTime dateTime = ZonedDateTime.parse(startUTC,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()));
				return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			} catch(DateTimeParseException ex2) {
				return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((applicationProducer == null) ? 0 : applicationProducer.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((deviceOrientation == null) ? 0 : deviceOrientation.hashCode());
		result = prime * result + ((os == null) ? 0 : os.hashCode());
		result = prime * result + ((osVersion == null) ? 0 : osVersion.hashCode());
		result = prime * result + ((phoneMake == null) ? 0 : phoneMake.hashCode());
		result = prime * result + ((phoneModel == null) ? 0 : phoneModel.hashCode());
		result = prime * result + ((startUTC == null) ? 0 : startUTC.hashCode());
		result = prime * result + ((targetAppVer == null) ? 0 : targetAppVer.hashCode());
		result = prime * result + ((targetedApp == null) ? 0 : targetedApp.hashCode());
		result = prime * result + ((traceOwner == null) ? 0 : traceOwner.hashCode());
		result = prime * result + ((traceSource == null) ? 0 : traceSource.hashCode());
		result = prime * result + ((traceType == null) ? 0 : traceType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MetaDataModel other = (MetaDataModel) obj;
		if (applicationProducer == null) {
			if (other.applicationProducer != null) {
				return false;
			}
		} else if (!applicationProducer.equals(other.applicationProducer)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (deviceOrientation == null) {
			if (other.deviceOrientation != null) {
				return false;
			}
		} else if (!deviceOrientation.equals(other.deviceOrientation)) {
			return false;
		}
		
		if (os == null) {
			if (other.os != null) {
				return false;
			}
		} else if (!os.equals(other.os)) {
			return false;
		}
		if (osVersion == null) {
			if (other.osVersion != null) {
				return false;
			}
		} else if (!osVersion.equals(other.osVersion)) {
			return false;
		}
		if (phoneMake == null) {
			if (other.phoneMake != null) {
				return false;
			}
		} else if (!phoneMake.equals(other.phoneMake)) {
			return false;
		}
		if (phoneModel == null) {
			if (other.phoneModel != null) {
				return false;
			}
		} else if (!phoneModel.equals(other.phoneModel)) {
			return false;
		}
		if (startUTC == null) {
			if (other.startUTC != null) {
				return false;
			}
		} else if (!startUTC.equals(other.startUTC)) {
			return false;
		}
		if (targetAppVer == null) {
			if (other.targetAppVer != null) {
				return false;
			}
		} else if (!targetAppVer.equals(other.targetAppVer)) {
			return false;
		}
		if (targetedApp == null) {
			if (other.targetedApp != null) {
				return false;
			}
		} else if (!targetedApp.equals(other.targetedApp)) {
			return false;
		}
		if (traceOwner == null) {
			if (other.traceOwner != null) {
				return false;
			}
		} else if (!traceOwner.equals(other.traceOwner)) {
			return false;
		}
		if (traceSource == null) {
			if (other.traceSource != null) {
				return false;
			}
		} else if (!traceSource.equals(other.traceSource)) {
			return false;
		}
		if (traceType == null) {
			if (other.traceType != null) {
				return false;
			}
		} else if (!traceType.equals(other.traceType)) {
			return false;
		}
		
		if (traceDuration == null) {
			if (other.traceDuration != null) {
				return false;
			}
		} else if (!traceDuration.equals(other.traceDuration)) {
			return false;
		}
		return true;
	}

}
