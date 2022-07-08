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
package com.att.aro.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.att.aro.core.settings.impl.SettingsImpl;

public interface TestingUtil {
	
	static void SetFinalField(Class<SettingsImpl> classInst, String name, String value)
			throws NoSuchFieldException, IllegalAccessException {
		Field pathField = classInst.getDeclaredField(name);
		pathField.setAccessible(true);
		Field modifier = Field.class.getDeclaredField("modifiers");
		modifier.setAccessible(true);
		modifier.setInt(pathField, pathField.getModifiers() & ~Modifier.FINAL);
		pathField.set(null, value);
	}

}
