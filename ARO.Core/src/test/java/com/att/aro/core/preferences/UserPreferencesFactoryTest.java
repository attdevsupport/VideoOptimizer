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
