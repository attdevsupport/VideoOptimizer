package com.att.aro.analytics;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
//import java.util.Date;

/**
 *
 * Focus point of the application. It can represent data points like application load, application module load, user actions, error events etc.
 *
 * Created by Harikrishna Yaramachu on 3/18/14.
 */
public class FocusPoint {

    private String name;
 //   private FocusPoint parentFocusPoint;


    //Event Tracking
    public String eventCategory;
    public String eventAction;
    public String eventLabel;
    public String eventValue = "0";

    public String exceptionDesc;
    public String dataSource;
    public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getExceptionDesc() {
		return exceptionDesc;
	}

	public void setExceptionDesc(String exceptionDesc) {
		this.exceptionDesc = exceptionDesc;
	}

	public boolean isFatal() {
		return isFatal;
	}

	public void setFatal(boolean isFatal) {
		this.isFatal = isFatal;
	}



	public boolean isFatal;
    
    //session
    public String session;


    public FocusPoint(){

    }

    public FocusPoint(String name) {
        this.name = name;
    }
/*
    public FocusPoint(String name, FocusPoint parentFocusPoint) {
        this(name);
        this.parentFocusPoint = parentFocusPoint;
    }
*/
    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
		this.name = name;
	}

	public void resetParams(){
        this.eventAction = null;
        this.eventCategory = null;
        this.eventLabel = null;
        this.eventValue = "0";
        this.session = null;
    }
	
	public void resetSession(){
		this.session = null;
	}

    public void setEventCategory(String eventCategory) {
        this.eventCategory = this.encode(eventCategory);
    }

    public void setEventAction(String eventAction) {
        this.eventAction = this.encode(eventAction);
    }

    public void setEventLabel(String eventLabel) {
        this.eventLabel = this.encode(eventLabel);
    }

    public void setEventValue(String eventValue) {
        this.eventValue = eventValue;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public String getEventAction() {
        return eventAction;
    }

    public String getEventLabel() {
        return eventLabel;
    }

    public String getEventValue() {
        return eventValue;
    }

    public String getSession() {
        return session;
    }



    private String encode(String name) {
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return name;
        }
    }

}
