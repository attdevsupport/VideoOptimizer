package com.att.aro.analytics;

/**
 * Created by Harikrishna Yaramachu on 4/17/14.
 */
public class GASendingParameters {


    //Event Tracking
    public String eventCategory;
    public String eventAction;
    public String eventLabel;
    public String eventValue;

    //session
    public String session;

    //Timing
    public String timingCategory;
    public String timingVariable;
    public String timingTime;
    public String timingLabel;
    public String loadTime;

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

    public String getTimingCategory() {
        return timingCategory;
    }

    public String getTimingVariable() {
        return timingVariable;
    }

    public String getTimingTime() {
        return timingTime;
    }

    public String getTimingLabel() {
        return timingLabel;
    }

    public String getLoadTime() {
        return loadTime;
    }


    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public void setEventAction(String eventAction) {
        this.eventAction = eventAction;
    }

    public void setEventLabel(String eventLabel) {
        this.eventLabel = eventLabel;
    }

    public void setEventValue(String eventValue) {
        this.eventValue = eventValue;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public void setTimingCategory(String timingCategory) {
        this.timingCategory = timingCategory;
    }

    public void setTimingVariable(String timingVariable) {
        this.timingVariable = timingVariable;
    }

    public void setTimingTime(String timingTime) {
        this.timingTime = timingTime;
    }

    public void setTimingLabel(String timingLabel) {
        this.timingLabel = timingLabel;
    }

    public void setLoadTime(String loadTime) {
        this.loadTime = loadTime;
    }
}
