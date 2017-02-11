package com.att.aro.core.preferences;

import org.junit.Test;
import com.att.aro.core.BaseTest;
import com.att.aro.core.configuration.pojo.Profile3G;
import com.att.aro.core.configuration.pojo.ProfileLTE;
import com.att.aro.core.configuration.pojo.ProfileType;
import com.att.aro.core.configuration.pojo.ProfileWiFi;
import com.att.aro.core.peripheral.pojo.PrivateDataInfo;
import com.att.aro.core.preferences.UserPreferences;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class UserPreferencesTest extends BaseTest {
	
	private static final String PREF_KEY_LAST_TRACE_DIR = "TD_PATH";
	private static final String VALID_DIR = Paths.get("").toAbsolutePath().toString();
	private static final String INVALID_DIR = "/test/dir";
	private static final String PROFILE_PATH = "PROFILE_PATH";
	private static final String PROFILE = "PROFILE";
	private static final String PROFILE_3G = "PROFILE_3G";
	private static final String PROFILE_LTE = "PROFILE_LTE";
	private static final String PROFILE_WIFI = "PROFILE_WIFI";
	private static final String EXPORT_PATH = "EXPORT_PATH";
	private static final String PRIVATE_DATA = "PRIVATE_DATA";

	@Test
	public void testGetInstance() {
	
		UserPreferences userPref = UserPreferences.getInstance();
		assertNotNull(userPref);
		assertEquals(UserPreferences.class, userPref.getClass());
	}
	
	@Test
	public void testGetLastTraceDirectory_Null() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		
		File actualTraceDirectory = userPrefs.getLastTraceDirectory();
		assertEquals(null, actualTraceDirectory);
	}
	
	@Test
	public void testGetLastTraceDirectory_NotNull() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		mockPrefHandler.setPref(PREF_KEY_LAST_TRACE_DIR, VALID_DIR);

		File actualTraceDirectory = userPrefs.getLastTraceDirectory();
		assertEquals(new File(VALID_DIR), actualTraceDirectory);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetLastTraceDirectory_InvalidDir() {
			
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();		
		userPrefs.setLastTraceDirectory(new File(INVALID_DIR));
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		assertEquals(null, mockPrefHandler.getPref(PREF_KEY_LAST_TRACE_DIR));
	}
	
	@Test
	public void testSetLastTraceDirectory_NullDir() {

		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();						
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		mockPrefHandler.setPref(PREF_KEY_LAST_TRACE_DIR, "some-path");
		
		userPrefs.setLastTraceDirectory(null);
		assertEquals(null, mockPrefHandler.getPref(PREF_KEY_LAST_TRACE_DIR));
	}
	
	@Test
	public void testSetLastTraceDirectory_ValidDir_ReplaceExisting() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();						
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		mockPrefHandler.setPref(PREF_KEY_LAST_TRACE_DIR, "some-path");
		
		userPrefs.setLastTraceDirectory(new File(VALID_DIR));
		
		assertEquals(VALID_DIR, mockPrefHandler.getPref(PREF_KEY_LAST_TRACE_DIR));
	}
	
	@Test
	public void testSetLastTraceDirectory_ValidDir_NewPref() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();						
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();		
		userPrefs.setLastTraceDirectory(new File(VALID_DIR));
		
		assertEquals(VALID_DIR, mockPrefHandler.getPref(PREF_KEY_LAST_TRACE_DIR));
	}
	
	@Test
	public void testGetLastProfile() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		String expectedProfile = "testProfile";
		
		mockPrefHandler.setPref(PROFILE, expectedProfile);
		
		String actualProfile = userPrefs.getLastProfile();
		assertEquals(expectedProfile, actualProfile);
	}
	
	@Test
	public void testGetLastProfile_ProfileType_3G() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		String expectedProfile = "test3GProfile";
		
		mockPrefHandler.setPref(PROFILE_3G, expectedProfile);
		
		String actualProfile = userPrefs.getLastProfile(ProfileType.T3G);
		assertEquals(expectedProfile, actualProfile);
	}
	
	@Test
	public void testGetLastProfile_ProfileType_LTE() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		String expectedProfile = "testLTEProfile";
		
		mockPrefHandler.setPref(PROFILE_LTE, expectedProfile);
		
		String actualProfile = userPrefs.getLastProfile(ProfileType.LTE);
		assertEquals(expectedProfile, actualProfile);
	}
	
	@Test
	public void testGetLastProfile_ProfileType_WIFI() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		String expectedProfile = "testWIFIProfile";
		
		mockPrefHandler.setPref(PROFILE_WIFI, expectedProfile);
		
		String actualProfile = userPrefs.getLastProfile(ProfileType.WIFI);
		assertEquals(expectedProfile, actualProfile);
	}
	
	@Test
	public void testGetLastProfile_ProfileType_Null() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();

		String actualProfile = userPrefs.getLastProfile(null);
		assertNull(actualProfile);
	}
	
	@Test
	public void testSetLastProfile_ProfileType_3G() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		
		Profile3G profile3G = new Profile3G();
		String expectedProfileName = "testProfile3G";
		profile3G.setName(expectedProfileName);	
		
		userPrefs.setLastProfile(profile3G);
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		assertEquals(expectedProfileName, mockPrefHandler.getPref(PROFILE));
		assertEquals(expectedProfileName, mockPrefHandler.getPref(PROFILE_3G));
		assertNull(mockPrefHandler.getPref(PROFILE_LTE));
		assertNull(mockPrefHandler.getPref(PROFILE_WIFI));
	}
	
	@Test
	public void testSetLastProfile_ProfileType_LTE() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		
		ProfileLTE profileLTE = new ProfileLTE();
		String expectedProfileName = "testProfileLTE";
		profileLTE.setName(expectedProfileName);	
		
		userPrefs.setLastProfile(profileLTE);
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		assertEquals(expectedProfileName, mockPrefHandler.getPref(PROFILE));
		assertEquals(expectedProfileName, mockPrefHandler.getPref(PROFILE_LTE));
		assertNull(mockPrefHandler.getPref(PROFILE_3G));
		assertNull(mockPrefHandler.getPref(PROFILE_WIFI));
	}
	
	@Test
	public void testSetLastProfile_ProfileType_Wifi() {
	
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		
		ProfileWiFi profileWifi = new ProfileWiFi();
		String expectedProfileName = "testProfileWIFI";
		profileWifi.setName(expectedProfileName);	
		
		userPrefs.setLastProfile(profileWifi);
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		assertEquals(expectedProfileName, mockPrefHandler.getPref(PROFILE));
		assertEquals(expectedProfileName, mockPrefHandler.getPref(PROFILE_WIFI));
		assertNull(mockPrefHandler.getPref(PROFILE_3G));
		assertNull(mockPrefHandler.getPref(PROFILE_LTE));
	}	

	@Test
	public void testSetLastProfile_NullProfile() {		

		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		userPrefs.setLastProfile(null);
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		assertNull(mockPrefHandler.getPref(PROFILE));
		assertNull(mockPrefHandler.getPref(PROFILE_3G));
		assertNull(mockPrefHandler.getPref(PROFILE_LTE));	
		assertNull(mockPrefHandler.getPref(PROFILE_WIFI));	
 	}
	
	@Test
	public void testSetLastProfile_NullProfileName() {		

		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		
		Profile3G profile3G = new Profile3G();
		userPrefs.setLastProfile(profile3G);
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		assertNull(mockPrefHandler.getPref(PROFILE));
		assertNull(mockPrefHandler.getPref(PROFILE_3G));
		assertNull(mockPrefHandler.getPref(PROFILE_LTE));	
		assertNull(mockPrefHandler.getPref(PROFILE_WIFI));	
 	}
		
	@Test
	public void testGetLastProfileDirectory() {

		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		
		String testProfilePath = VALID_DIR;
		mockPrefHandler.setPref(PROFILE_PATH, testProfilePath);
		
		File actualProfileDir = userPrefs.getLastProfileDirectory();
		assertEquals(new File(VALID_DIR), actualProfileDir);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetLastProfileDirectory_NotDirectory() {
	
		File tmpFile = null;
		
		try {
		
			tmpFile = File.createTempFile("testLastProfileDir", null, new File(VALID_DIR));		
			
			UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
			userPrefs.setLastProfileDirectory(tmpFile);
			
			IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
			assertEquals(mockPrefHandler.getPref(PROFILE_PATH), null);	
			
		} catch (IOException e) {
			
			fail(e.getMessage());
		
		} finally {
			
			if (tmpFile != null) {
				tmpFile.deleteOnExit();
			}
		}
	}
	
	@Test
	public void testSetLastProfileDirectory_ValidDirectory() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		userPrefs.setLastProfileDirectory(new File(VALID_DIR));
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		String actualProfileDir = mockPrefHandler.getPref(PROFILE_PATH);
		
		assertEquals(VALID_DIR, actualProfileDir);
	}
	
	@Test
	public void testGetLastExportDirectory_IsDirectory() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		mockPrefHandler.setPref(EXPORT_PATH, VALID_DIR);
		
		assertEquals(new File(VALID_DIR), userPrefs.getLastExportDirectory());
	}
	
	@Test
	public void testGetLastExportDirectory_NotDirectory() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		String currentFile = VALID_DIR + File.separator + this.getClass().getName();
		mockPrefHandler.setPref(EXPORT_PATH, currentFile);
		
		assertNull(userPrefs.getLastExportDirectory());
	}
	
	@Test
	public void testSetLastExportDirectory_Null() {	
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		userPrefs.setLastExportDirectory(null);
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		
		assertNull(mockPrefHandler.getPref(EXPORT_PATH));
	}
	
	@Test
	public void testSetLastExportDirectory_DirectoryProvidedNotExist() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		userPrefs.setLastExportDirectory(new File(INVALID_DIR));
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		
		assertNull(mockPrefHandler.getPref(EXPORT_PATH));
	}
	
	@Test
	public void testSetLastExportDirectory_DirectoryProvidedIsDirectory() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		userPrefs.setLastExportDirectory(new File(VALID_DIR));
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
	
		assertEquals(VALID_DIR, mockPrefHandler.getPref(EXPORT_PATH));
	}
	
	@Test
	public void testSetLastExportDirectory_DirectoryProvidedIsFile() {

		File tmpFile = null;
		
		try {
		
			tmpFile = File.createTempFile("testLastExportDir", null, new File(VALID_DIR));		
			
			UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
			userPrefs.setLastExportDirectory(tmpFile);
			
			IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
			
			assertEquals(VALID_DIR, mockPrefHandler.getPref(EXPORT_PATH));		
			
		} catch (IOException e) {
			
			fail(e.getMessage());
		
		} finally {
			
			if (tmpFile != null) {
				tmpFile.deleteOnExit();
			}
		}
	}
	
	@Test
	public void testGetPrivateData_PrivateDataPreferenceExists() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		
		mockPrefHandler.setPref(PRIVATE_DATA, "KEYWORD,Phone Number,123-123-1234,Y;PATTERN,PATTERNNAME,PATTERNVALUE,N");
		List<PrivateDataInfo> privateData = userPrefs.getPrivateData();
		
		assertEquals(privateData.size(), 2);
		assertEquals(privateData.get(0).getCategory(), "KEYWORD");
		assertEquals(privateData.get(0).getType(), "Phone Number");
		assertEquals(privateData.get(0).getValue(), "123-123-1234");
		assertEquals(privateData.get(0).isSelected(), true);
		assertEquals(privateData.get(1).getCategory(), "PATTERN");
		assertEquals(privateData.get(1).getType(), "PATTERNNAME");
		assertEquals(privateData.get(1).getValue(), "PATTERNVALUE");
		assertEquals(privateData.get(1).isSelected(), false);
	}
	
	@Test
	public void testGetPrivateData_PrivateDataPreferenceNotYetExist() {
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();		
		List<PrivateDataInfo> privateData = userPrefs.getPrivateData();
		
		assertEquals(privateData.size(), 0);
	}
	
	@Test
	public void testSetPrivateData() {

		PrivateDataInfo phoneNumber = new PrivateDataInfo();
		phoneNumber.setCategory("KEYWORD");
		phoneNumber.setType("Phone Number");
		phoneNumber.setValue("123-123-1234");
		phoneNumber.setSelected(true);
		
		PrivateDataInfo somePattern = new PrivateDataInfo();
		somePattern.setCategory("PATTERN");
		somePattern.setType("PATTERNNAME");
		somePattern.setValue("PATTERNVALUE");
		somePattern.setSelected(false);
				
		List<PrivateDataInfo> privateData = new ArrayList<PrivateDataInfo>();
		privateData.add(phoneNumber);
		privateData.add(somePattern);
		
		UserPreferences userPrefs = MockUserPreferencesFactory.getInstance().create();
		userPrefs.setPrivateData(privateData);
		
		IPreferenceHandler mockPrefHandler = userPrefs.getPreferenceHandler();
		String actualPrivateDataPersisted = mockPrefHandler.getPref(PRIVATE_DATA);
		
		String expectedPrivateDataPersisted = 
				"KEYWORD,Phone Number,123-123-1234,Y;PATTERN,PATTERNNAME,PATTERNVALUE,N";	

		assertEquals(expectedPrivateDataPersisted, actualPrivateDataPersisted);
	}

}
