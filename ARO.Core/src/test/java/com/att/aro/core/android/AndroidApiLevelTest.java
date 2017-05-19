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
