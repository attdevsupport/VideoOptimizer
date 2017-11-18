/*
 *  Copyright 2012 AT&T
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
package com.att.aro.core.preferences.impl;

import java.util.prefs.Preferences;

import com.att.aro.core.ILogger;
import com.att.aro.core.impl.LoggerImpl;
import com.att.aro.core.preferences.IPreferenceHandler;

public final class PreferenceHandlerImpl implements IPreferenceHandler {

	public static final String ARO_NODE_NAME = "/com/att/aro";
	private static PreferenceHandlerImpl instance = new PreferenceHandlerImpl();
	private Preferences prefs;
	
	private ILogger logger = new LoggerImpl(PreferenceHandlerImpl.class.getName());	

	public static PreferenceHandlerImpl getInstance() {
		return instance;
	}

	/**
	 * Private constructor. Use getInstance()
	 */
	private PreferenceHandlerImpl() {
		prefs = Preferences.userRoot().node(ARO_NODE_NAME);
	}
	
	@Override
	public String getPref(String prefKey) {
		
		if (prefKey == null) {
			logger.error("Null preference key!");
			return null;  
		}
		
		String prefValue = prefs.get(prefKey, null);
		logger.debug("Retrieving key:" + prefKey + ";value:" + prefValue);
		return prefValue;
	}
	
	@Override
	public void setPref(String prefKey, String prefValue) {
		
		if (prefKey == null || prefValue == null || prefValue.equals("null")) {
			logger.error("Preference key and preference value cannot be null! "
					+ "key:" + prefKey + " value:" + prefValue);
		} else {
			logger.debug("Storing key:" + prefKey + ";value:" + prefValue);
			prefs.put(prefKey, prefValue);
		}
	}
	
	@Override
	public void removePref(String prefKey) {
		
		if (prefKey == null) {
			logger.error("Preference key cannot be null! ");
		} else {
			logger.debug("Removing preference - key:" + prefKey);
			prefs.remove(prefKey);
		}
	}
	
}