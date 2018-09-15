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
