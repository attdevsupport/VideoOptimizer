package com.att.aro.analytics;

/**
 * Interface for the URL building strategy
 *
 * Created by Harikrishna Yaramachu on 3/18/14.
 */
public interface URLBuildingStrategy {

    public String buildURL(FocusPoint focusPoint);
    public void setRefererURL(String refererURL);

}
