package com.att.aro.core.preferences.impl;

import java.util.HashMap;
import java.util.Map;

import com.att.aro.core.preferences.IPreferenceHandler;

public class MockPreferenceHandlerImpl implements IPreferenceHandler {

	Map<String, String> prefs = new HashMap<String, String>();
	
	@Override
	public String getPref(String name) {
		return prefs.get(name);
	}

	@Override
	public void setPref(String name, String value) {
		prefs.put(name, value);
	}

	@Override
	public void removePref(String name) {
		prefs.remove(name);
	}
}
