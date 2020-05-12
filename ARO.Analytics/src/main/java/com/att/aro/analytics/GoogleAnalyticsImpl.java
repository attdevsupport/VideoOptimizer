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
package com.att.aro.analytics;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.apache.commons.lang.StringUtils;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.analytics.IGoogleAnalytics;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.util.AnalyticsCommon;

public class GoogleAnalyticsImpl implements IGoogleAnalytics {
	private static final Logger LOGGER = LogManager.getLogger(GoogleAnalyticsImpl.class.getName());
	public static String appName= "";
	public static String appVersion= "";
	public static String gaTracker= "";
	
	private GoogleAnalyticsTracker gat; //Main class where analytics will push the events
    private GAEntry gaFocusPoint;
    
    public GoogleAnalyticsImpl(){
    	 this.gat = new GoogleAnalyticsTracker(appName, appVersion, AnalyticsCommon.GA_TRACK_ID, AnalyticsCommon.GA_CAPACITY);
         this.gaFocusPoint = new GAEntry(appName);
    }

	public void applicationInfo(String analyticsTracker, String applicationName, String applicationVersion){
		LOGGER.debug("applicationInfo called");

		gaTracker = GoogleAnalyticsUtil.getConfigSetting().getAttribute("gaTrackerId"); //Added for getting the Tracker ID from config
		if(gaTracker == null){
			gaTracker = AnalyticsCommon.GA_TRACK_ID;
		}

		if(applicationName != null){
			appName = applicationName;
		}
		
		if(applicationVersion != null){
			appVersion = applicationVersion;
		}
		
		initializeGATracker();
	}
	
	public void sendAnalyticsEvents(String eventCategory, String eventAction){
    
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);

        this.pushEventToCloud(this.gaFocusPoint);
    }
	
	public void sendErrorEvents(String errName, String errDescription, boolean isFatal) {
		this.gaFocusPoint.resetParams();
		this.gaFocusPoint.setExceptionDesc(errDescription);
		this.gaFocusPoint.setErrorName(errName);
		this.gaFocusPoint.setFatal(isFatal);

		this.pushEventToCloud(this.gaFocusPoint);
	}
	
	public void sendExceptionEvents(String exceptionDesc, String source, boolean isFatal){
    
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setExceptionDesc(exceptionDesc);
        this.gaFocusPoint.setDataSource(source);
        this.gaFocusPoint.setFatal(isFatal);

        this.pushEventToCloud(this.gaFocusPoint);
    }
	
	public void sendCrashEvents(String crashDesc){
    
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setExceptionDesc(crashDesc);
        this.gaFocusPoint.setFatal(true);

        this.pushEventToCloud(this.gaFocusPoint);
    }
    
	public void sendViews(String screen){
        this.pushEventToCloud(new GAEntry(ApplicationConfig.getInstance().getAppShortName(), screen, HitType.SCREEN_VIEW));
    }

	public void sendAnalyticsEvents(String eventCategory, String eventAction, String eventLable){
        
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);
        this.gaFocusPoint.setLabel(eventLable);

        this.pushEventToCloud(this.gaFocusPoint);
    }

	public void sendAnalyticsEvents(String eventCategory, String eventAction, String eventLable, String eventValue){
        
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);
        this.gaFocusPoint.setLabel(eventLable);
        this.gaFocusPoint.setLabel(eventValue);

        this.pushEventToCloud(this.gaFocusPoint);
    }
	 
	public void sendAnalyticsStartSessionEvents(String eventCategory,
			String eventAction) {
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.start.param());
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);

        this.pushEventToCloud(this.gaFocusPoint);
	}

	public void sendAnalyticsStartSessionEvents(String eventCategory,
			String eventAction, String eventLable) {
		this.gaFocusPoint.resetParams();
		this.gaFocusPoint.setSession(GASessionValue.start.param());
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);
        this.gaFocusPoint.setLabel(eventLable);

        this.pushEventToCloud(this.gaFocusPoint);
		
	}

	public void sendAnalyticsStartSessionEvents(String eventCategory,
			String eventAction, String eventLable, String eventValue) {
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.start.param());
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);
        this.gaFocusPoint.setLabel(eventLable);
        this.gaFocusPoint.setLabel(eventValue);

        this.pushEventToCloud(this.gaFocusPoint);
	}

//	@Override
	public void sendAnalyticsEndSessionEvents(String eventCategory, String eventAction) {
		this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.end.param());
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);

        this.pushEventToCloud(this.gaFocusPoint);
		
	}

//	@Override
	public void sendAnalyticsEndSessionEvents(String eventCategory,
			String eventAction, String eventLable) {
		this.gaFocusPoint.resetParams();
		this.gaFocusPoint.setSession(GASessionValue.end.param());
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);
        this.gaFocusPoint.setLabel(eventLable);

        this.pushEventToCloud(this.gaFocusPoint);
		
	}

//	@Override
	public void sendAnalyticsEndSessionEvents(String eventCategory,
			String eventAction, String eventLable, String eventValue) {
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.end.param());
        this.gaFocusPoint.setCategory(eventCategory);
        this.gaFocusPoint.setAction(eventAction);
        this.gaFocusPoint.setLabel(eventLable);
        this.gaFocusPoint.setLabel(eventValue);

        this.pushEventToCloud(this.gaFocusPoint);
	}

	public void sendAnalyticsTimings(String timingLabel, long runTime, String timingCategory) {
		this.gaFocusPoint.resetParams();
		this.gaFocusPoint.setHitType(HitType.TIMING);
		this.gaFocusPoint.setTimingCategory(StringUtils.deleteWhitespace(timingCategory));
		this.gaFocusPoint.setTimingVariable(StringUtils.deleteWhitespace(timingLabel));
		this.gaFocusPoint.setTimingValue(String.valueOf(runTime));
		this.pushEventToCloud(this.gaFocusPoint);
	}

	/**
     * 
     * @param aFocusPoint
     */
	 private void pushEventToCloud(GAEntry aFocusPoint){
		 if(this.gat == null){
			 initializeGATracker();
		 }
		if (AnalyticsCommon.GA_SENDFLAG) {
			if ((aFocusPoint.getAction() != null && aFocusPoint.getAction().equals(AnalyticsCommon.GA_ENDAPP))
					|| (AnalyticsCommon.GA_TRACE_ANALYZED.equals(aFocusPoint.getAction()))
					|| (aFocusPoint.getExceptionDesc() != null)
					|| (AnalyticsCommon.GA_VO_SESSION.equals(aFocusPoint.getCategory()))) {
				gat.pushToCloud(aFocusPoint, true);

			} else {
				gat.pushToCloud(aFocusPoint, false);
			}
		}

	}
 
	 private void initializeGATracker(){
		 LOGGER.debug("initializeGATracker called");
	     this.gat = new GoogleAnalyticsTracker(appName, appVersion, gaTracker, AnalyticsCommon.GA_CAPACITY);
	     this.gaFocusPoint = new GAEntry(appName);
	 }

//	@Override
	public void close() {
		gat.close();
		LOGGER.debug("database properly closed");
	}

}
