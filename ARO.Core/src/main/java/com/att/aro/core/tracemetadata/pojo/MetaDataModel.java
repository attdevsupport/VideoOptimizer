/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.tracemetadata.pojo;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * USED FOR FINDING and retrieving results/Raw data
 * 
 * Date of trace is startUTC
 *  search exact or range
 *  
 *
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)				// allows for changes dropping items or using older versions
@JsonInclude(Include.NON_NULL)
public class MetaDataModel {
	
	public static final String UNKNOWN = "unknown";
	public static final String TRACE_OWNER = "traceOwner";

	@Nonnull private String traceStorage = "";    
	@Nonnull private String traceName = "";    			

	@Nonnull private String description = "";      
	@Nonnull private String traceType = UNKNOWN;             // VOD, Social, other
	@Nonnull private String targetedApp = UNKNOWN;                             
	@Nonnull private String applicationProducer = UNKNOWN;       
	
	// auto filled in after running trace and analyzing, before results export
	@Nonnull private String deviceOrientation = "UNKNOWN";     // PORTRAIT/LANDSCAPE                                                                                                            
	@Nonnull private String phoneMake = "";                                                                                                            
	@Nonnull private String phoneModel = "";  
	private Dimension deviceScreenSize = null;
	
	// Trace Device
	@Nonnull private String os = "";              		// Android/iOS                       
	@Nonnull private String osVersion = "";                                          
	@Nonnull private String targetAppVer = "";    
	@Nonnull private String applicationId = "";  
	@Nonnull private String netWork = "";          
	@Nonnull private String sim = "";      
	private AttenuatorModel attenuation = null;
	
	// Collector details
	@Nonnull private String collectorName = "";
	@Nonnull private String collectorVersion = "";
	
	@Nonnull private String startUTC = "";    
	@Nonnull private Long utc = 0L;    
	
	@Nonnull private Double traceDuration = 0.0;   
	@Nonnull private String traceSource = "";           // MANUAL/MACHINE = "";
	@Nonnull private String traceOwner = "";            
	
	// Video 
	@Nonnull private String URL = "";
	@Nonnull private String VideoName = "";
	@Nonnull private Double totalVideoDownloadtime = 0.0;        
	@Nonnull private Double totalVideoDuration = 0.0;         
	@Nonnull private String traceNotes = "";			// optional, explain what is of interest in this trace, notes can be very large
	@Nonnull private List<MetaStream> videoStreams = new ArrayList<>(); 

	public String getTraceNotes() {
		return traceNotes.replaceAll("\\\\n", System.getProperty("line.separator"));
	}

	public void setTraceNotes(String traceNotes) {
		this.traceNotes = traceNotes.replaceAll(System.getProperty("line.separator"), "\\\\n");
	}
}
