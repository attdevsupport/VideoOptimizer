package com.att.aro.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.att.aro.core.util.WeakCipherUtil.WeakCipherBlackList;

public class WeakCipherUtilTest {

	@Test
	public void testContainsKey() {
		assertTrue(WeakCipherUtil.containsKey("0x0006"));
	}

	@Test
	public void testNotContainsKey() {
		assertNotEquals(true, WeakCipherUtil.containsKey("0x0004"));
	}
	
	@Test
	public void testGetCipherIdentifier() {
		WeakCipherBlackList weakCipher = WeakCipherUtil.getCipherIdentifier("0x0006");
		assertEquals(WeakCipherBlackList.RSA_EXPORT_WITH_RC2_CBC_40_MD5, weakCipher);
	}
	
	@Test
	public void testToString() {
		assertEquals("0x0006", WeakCipherBlackList.RSA_EXPORT_WITH_RC2_CBC_40_MD5.toString());
	}
	
	@Test
	public void testInit() {
		WeakCipherUtil.init();
	}
}
