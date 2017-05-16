package com.att.aro.analytics;

/**
 * Created by Harikrishna Yaramachu on 3/31/14.
 */
public enum GACommonParameter {

    anonymizeip ("aip="),
    queuetime ("qt="),
    cachebuster ("z="),
    sessionstart ("sc="),
    sessionend ("sc="),
    userlanguage ("ul="),
    applicationname ("an="),
    applicationversion ("av="),
    eventcategory ("ec="),
    eventaction ("ea="),
    eventlabel ("el="),
    eventvalue ("ev="),
    timingcategory ("utc="),
    timingvariablename ("utv="),
    timingtime ("utt="),
    timimglabel ("utl="),
    exceptiondesc ("exd="),
    datasource ("ds="),
    isexceptionfatal ("exf=");

    private String param;

    private GACommonParameter(String param){
        this.param = param;
    }

    public String param(){
        return param;
    }
}
