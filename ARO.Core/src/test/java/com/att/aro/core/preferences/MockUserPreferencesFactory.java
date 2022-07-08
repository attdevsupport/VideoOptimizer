/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.preferences;

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
