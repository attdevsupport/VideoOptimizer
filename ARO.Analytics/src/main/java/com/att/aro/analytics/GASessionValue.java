package com.att.aro.analytics;

/**
 * Created by Harikrishna Yaramachu on 4/17/14.
 */
public enum GASessionValue {

    start ("start"), //version
    end ("end");

    private String param;

    private GASessionValue(String param){
        this.param = param;
    }

    public String param(){
        return param;
    }

}
