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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
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
