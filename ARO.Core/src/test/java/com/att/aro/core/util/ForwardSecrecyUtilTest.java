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
package com.att.aro.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.att.aro.core.util.ForwardSecrecyUtil.ForwardSecrecyBlackList;

public class ForwardSecrecyUtilTest {

	@Test
	public void testContainsKey() {
		assertTrue(ForwardSecrecyUtil.containsKey("0x0004"));
	}

	@Test
	public void testNotContainsKey() {
		assertNotEquals(true, ForwardSecrecyUtil.containsKey("0xc001"));
	}
	
	@Test
	public void testGetCipherIdentifier() {
		ForwardSecrecyBlackList cipher = ForwardSecrecyUtil.getCipherIdentifier("0x0004");
		assertEquals(ForwardSecrecyBlackList.RSA_WITH_RC4_128_MD5, cipher);
	}
	
	@Test
	public void testToString() {
		assertEquals("0x0004", ForwardSecrecyBlackList.RSA_WITH_RC4_128_MD5.toString());
	}
	
	@Test
	public void testInit() {
		ForwardSecrecyUtil.init();
	}
}
