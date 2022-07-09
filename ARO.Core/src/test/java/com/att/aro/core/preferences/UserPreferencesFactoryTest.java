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
package com.att.aro.core.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.att.aro.core.BaseTest;

public class UserPreferencesFactoryTest extends BaseTest {
	
	@Test
	public void testGetInstance() {
		
		UserPreferencesFactory factory = UserPreferencesFactory.getInstance();
		
		assertNotNull(factory);
		assertEquals(UserPreferencesFactory.class, factory.getClass());
	}
	
	@Test
	public void testCreate() {
		
		UserPreferencesFactory factory = UserPreferencesFactory.getInstance();
		UserPreferences userPrefs = factory.create();
		
		assertNotNull(userPrefs);
		assertEquals(UserPreferences.class, userPrefs.getClass());
 	}
}
