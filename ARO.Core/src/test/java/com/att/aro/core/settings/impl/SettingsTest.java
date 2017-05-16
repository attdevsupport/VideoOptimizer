///**
// * 
// */
//package com.att.aro.core.settings.impl;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.File;
//import java.util.Properties;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import com.att.aro.core.settings.Settings;
//import com.att.aro.core.util.TestingUtil;
//
//public class SettingsTest {
//	private static final String configFilePath = SettingsImpl.CONFIG_FILE_PATH + ".test";
//
//	private Settings settings;
//	private static File configFileFile;
//
//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//		configFileFile = new File(configFilePath);
//		TestingUtil.SetFinalField(SettingsImpl.class, "CONFIG_FILE_PATH", configFilePath);
//	}
//
//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//		configFileFile.delete();
//	}
//
//	@Before
//	public void setUp() throws Exception {
//		settings = SettingsImpl.getInstance();
//	}
//
//	@Test
//	public void testAttributeReadWrite() {
//		SettingsImpl.ConfigFileAttributes.adb.toString(); // for code-coverage
//		settings.setAndSaveAttribute("myTestAttribute", "myTestValue");
//		Properties properties = ((SettingsImpl) settings).configProperties;
//		assertEquals(properties.getProperty("myTestAttribute"), "myTestValue");
//		assertEquals(settings.getAttribute("myTestAttribute"), "myTestValue");
//	}
//
//	@Test
//	public void testAttributeNotEmptyReadWrite() {
//		settings.setAttribute("myOtherAttribute", "myOtherValue");
//		settings.setAndSaveAttribute("myTestAttribute", "myTestValue");
//		assertTrue(settings.listAttributes().containsKey("myTestAttribute"));
//		assertEquals(settings.getAttribute("myOtherAttribute"), "myOtherValue");
//		assertEquals(settings.getAttribute("myTestAttribute"), "myTestValue");
//	}
//
//	@Test
//	public void testRemoveAttribute() {
//		settings.setAndSaveAttribute("myTestAttribute", "myTestValue");
//		settings.removeAttribute("myOtherAttribute");
//		settings.saveConfigFile();
//		settings.removeAndSaveAttribute("myTestAttribute");
//		assertTrue(!"myTestValue".equals(settings.getAttribute("myTestAttribute")));
//	}
//
//}
