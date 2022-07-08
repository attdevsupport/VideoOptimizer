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
package com.att.aro.core.android;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import com.att.aro.core.BaseTest;

public class AndroidApiLevelTest extends BaseTest {

	@Test
	public void testGetCodeName(){		
		AndroidApiLevel apiLevel = AndroidApiLevel.K19;	
		assertEquals("Kitkat", apiLevel.codeName());
	}
	
	@Test
	public void testGetLevelNumber(){
		AndroidApiLevel apiLevel = AndroidApiLevel.M23;	
		assertEquals(23, apiLevel.levelNumber());
	}
	
	@Test
	public void testGetVersions() {
		
		AndroidApiLevel apiLevel = AndroidApiLevel.L21;	
		String[] expectedVersions = {"5.0","5.0.1","5.0.2"};
		
		for (int i = 0; i < expectedVersions.length; i++) {
			assertEquals(expectedVersions[i], apiLevel.versions()[i]);
		}
	}
}
