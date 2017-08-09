package com.att.aro.core.preferences;

import com.att.aro.core.preferences.UserPreferences;
import com.att.aro.core.preferences.impl.MockPreferenceHandlerImpl;

public class MockUserPreferencesFactory {

	private static MockUserPreferencesFactory instance;
	
	private MockUserPreferencesFactory() {}
	
	public static synchronized MockUserPreferencesFactory getInstance() {
		if (instance == null) {
			instance = new MockUserPreferencesFactory();
		}
		return instance;
	}
	
	public UserPreferences create() {
		
		UserPreferences userPreferences = UserPreferences.getInstance();
		userPreferences.setPreferenceHandler(new MockPreferenceHandlerImpl());
		
		return userPreferences;
	}

}
