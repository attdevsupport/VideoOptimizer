package com.att.aro.analytics;

/**
 * Created by Harikrishna Yaramachu on 3/27/14.
 */
public enum GAHitTypes {

    typeP ("pageview"),
    typeA ("screenview"),
    typeE ("event"),
    typeEX ("exception"),
    typeI ("item"),
    typeTI ("timing"),
    typeT ("transaction"), //For future reference
    typeS ("social");   //For future reference

    private final String type;

    private GAHitTypes(String s) {
        type = s;
    }

    public String type(){
        return type;
    }
}
