/**
 * Copyright 2015 AT&T
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
 * 
 * @author Nathan F Syfrig
 *
 */
package com.att.aro.core.settings;

import java.util.Map;

/**
 * @author Nathan F Syfrig
 *
 */
public interface Settings {
	Map<String, String> listAttributes();
	String getAttribute(String name);
	String setAttribute(String name, String value);
	String removeAttribute(String name);
	String setAndSaveAttribute(String name, String value);
	String removeAndSaveAttribute(String name);
	default boolean checkAttributeValue(String attribName, String value) {
		String attribValue = getAttribute(attribName);
		if (attribValue != null && attribValue.equals(value)) {
			return true;
		}
		return false;
	}
	void saveConfigFile();
}
