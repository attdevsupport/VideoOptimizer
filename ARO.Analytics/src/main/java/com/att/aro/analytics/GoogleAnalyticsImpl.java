/**
 * 
 */
package com.att.aro.analytics;

import java.util.logging.Logger;

import com.att.aro.core.analytics.IGoogleAnalytics;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.util.AnalyticsCommon;

/**
 *
 *
 */
public class GoogleAnalyticsImpl implements IGoogleAnalytics {
	private static final Logger LOGGER = Logger.getLogger(GoogleAnalyticsImpl.class.getName());
	public static String appName= "";
	public static String appVersion= "";
	public static String gaTracker= "";
	
	private GoogleAnalyticsTracker gat; //Main class where analytics will push the events 
    private FocusPoint gaFocusPoint;
    
    public GoogleAnalyticsImpl(){
    	 this.gat = new GoogleAnalyticsTracker(appName, appVersion, AnalyticsCommon.GA_TRACK_ID, AnalyticsCommon.GA_CAPACITY);
         this.gaFocusPoint = new FocusPoint(appName);
    }

	/* (non-Javadoc)
	 * @see com.att.aro.analytics.IGoogleAnalytics#applicationInfo(java.lang.String, java.lang.String)
	 */
//	@Override
	public void applicationInfo(String analyticsTracker, String applicationName, String applicationVersion){
		LOGGER.info("applicationInfo called");

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
	
    /* (non-Javadoc)
	 * @see com.att.aro.analytics.IGoogleAnalytics#sendAnalyticsEvents(java.lang.String, java.lang.String)
	 */
//	@Override
	public void sendAnalyticsEvents(String eventCategory, String eventAction){
    
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);

        this.pushEventToCloud(this.gaFocusPoint); 
    }
	
	
    /* (non-Javadoc)
	 * @see com.att.aro.analytics.IGoogleAnalytics#sendAnalyticsEvents(java.lang.String, java.lang.String)
	 */
//	@Override
	public void sendExceptionEvents(String exceptionDesc, String source, boolean isFatal){
    
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setExceptionDesc(exceptionDesc);
        this.gaFocusPoint.setDataSource(source);
        this.gaFocusPoint.setFatal(isFatal);

        this.pushEventToCloud(this.gaFocusPoint); 
    }
	
    /* (non-Javadoc)
	 * @see com.att.aro.analytics.IGoogleAnalytics#sendAnalyticsEvents(java.lang.String, java.lang.String)
	 */
//	@Override
	public void sendCrashEvents(String crashDesc, String source){
    
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setExceptionDesc(crashDesc);
        this.gaFocusPoint.setDataSource(source);
        this.gaFocusPoint.setFatal(true);

        this.pushEventToCloud(this.gaFocusPoint); 
    }
    
    /* (non-Javadoc)
	 * @see com.att.aro.analytics.IGoogleAnalytics#sendAnalyticsEvents(java.lang.String, java.lang.String, java.lang.String)
	 */
//	@Override
	public void sendAnalyticsEvents(String eventCategory, String eventAction, String eventLable){
        
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);
        this.gaFocusPoint.setEventLabel(eventLable);

        this.pushEventToCloud(this.gaFocusPoint); 
    }

    /* (non-Javadoc)
	 * @see com.att.aro.analytics.IGoogleAnalytics#sendAnalyticsEvents(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
//	@Override
	public void sendAnalyticsEvents(String eventCategory, String eventAction, String eventLable, String eventValue){
        
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);
        this.gaFocusPoint.setEventLabel(eventLable);
        this.gaFocusPoint.setEventLabel(eventValue);

        this.pushEventToCloud(this.gaFocusPoint); 
    }
	 
//    @Override
	public void sendAnalyticsStartSessionEvents(String eventCategory,
			String eventAction) {
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.start.param());
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);

        this.pushEventToCloud(this.gaFocusPoint); 
	}

//	@Override
	public void sendAnalyticsStartSessionEvents(String eventCategory,
			String eventAction, String eventLable) {
		this.gaFocusPoint.resetParams();
		this.gaFocusPoint.setSession(GASessionValue.start.param());
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);
        this.gaFocusPoint.setEventLabel(eventLable);

        this.pushEventToCloud(this.gaFocusPoint); 
		
	}

//	@Override
	public void sendAnalyticsStartSessionEvents(String eventCategory,
			String eventAction, String eventLable, String eventValue) {
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.start.param());
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);
        this.gaFocusPoint.setEventLabel(eventLable);
        this.gaFocusPoint.setEventLabel(eventValue);

        this.pushEventToCloud(this.gaFocusPoint); 
	}

//	@Override
	public void sendAnalyticsEndSessionEvents(String eventCategory, String eventAction) {
		this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.end.param());
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);

        this.pushEventToCloud(this.gaFocusPoint); 
		
	}

//	@Override
	public void sendAnalyticsEndSessionEvents(String eventCategory,
			String eventAction, String eventLable) {
		this.gaFocusPoint.resetParams();
		this.gaFocusPoint.setSession(GASessionValue.end.param());
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);
        this.gaFocusPoint.setEventLabel(eventLable);

        this.pushEventToCloud(this.gaFocusPoint); 
		
	}

//	@Override
	public void sendAnalyticsEndSessionEvents(String eventCategory,
			String eventAction, String eventLable, String eventValue) {
        this.gaFocusPoint.resetParams();
        this.gaFocusPoint.setSession(GASessionValue.end.param());
        this.gaFocusPoint.setEventCategory(eventCategory);
        this.gaFocusPoint.setEventAction(eventAction);
        this.gaFocusPoint.setEventLabel(eventLable);
        this.gaFocusPoint.setEventLabel(eventValue);

        this.pushEventToCloud(this.gaFocusPoint); 
	}

	/**
     * 
     * @param aFocusPoint
     */
	 private void pushEventToCloud(FocusPoint aFocusPoint){
		 if(this.gat == null){
			 initializeGATracker();
		 }
	    if(AnalyticsCommon.GA_SENDFLAG){
		        if((aFocusPoint.getEventAction()!=null && aFocusPoint.getEventAction().equals(AnalyticsCommon.GA_ENDAPP))
		        		|| (aFocusPoint.getExceptionDesc()!=null)){
		        	
		            gat.pushToCloud(aFocusPoint, true);

		        }else{
		        	gat.pushToCloud(aFocusPoint, false);
		        } 
	    }
	        
	   }
	 
	 private void initializeGATracker(){
		 LOGGER.info("initializeGATracker called");
	     this.gat = new GoogleAnalyticsTracker(appName, appVersion, gaTracker, AnalyticsCommon.GA_CAPACITY);
	     this.gaFocusPoint = new FocusPoint(appName);
	 }

//	@Override
	public void close() {
		gat.close();
		LOGGER.info("database properly closed");
	}

}
