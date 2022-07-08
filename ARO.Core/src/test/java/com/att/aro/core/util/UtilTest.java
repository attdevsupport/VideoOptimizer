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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.settings.impl.SettingsImpl;

public class UtilTest {
	String aroJpcapLibName,aroJpcapLibFileName;
	@Before
	public void setup(){
		String osname = Util.OS_NAME, osarch = Util.OS_ARCHITECTURE;
		if (Util.OS_NAME != null && Util.OS_ARCHITECTURE != null) {
			if (osname.contains("Windows") && osarch.contains("64")) { // _______ 64 bit Windows jpcap64.DLL
				aroJpcapLibName = "jpcap64";
				aroJpcapLibFileName = aroJpcapLibName + ".dll";

			} else if (osname.contains("Windows")) { // _________________________ 32 bit Windows jpcap.DLL
				aroJpcapLibName = "jpcap";
				aroJpcapLibFileName = aroJpcapLibName + ".dll";

			} else if (osname.contains("Linux") && osarch.contains("amd64")) { // 64 bit Linux libjpcap64.so
				aroJpcapLibName = "jpcap64";
				aroJpcapLibFileName = "lib" + aroJpcapLibName + ".so";

			} else if (osname.contains("Linux") && osarch.contains("i386")) { //  32 bit Linux libjpcap.so
				aroJpcapLibName = "jpcap32";
				aroJpcapLibFileName = "lib" + aroJpcapLibName + ".so";

			} else { // _________________________________________________________ Mac OS X libjpcap.jnilib
				aroJpcapLibName = "jpcap";
				aroJpcapLibFileName = "lib" + aroJpcapLibName + ".jnilib";
			}
		}
	}
	@Test
	public void isMacOS(){
		boolean ismac = Util.isMacOS();
		String os = System.getProperty("os.name");
		boolean hasmac = os.contains("Mac");
		assertEquals(hasmac, ismac);
	}
	@Test
	public void isWindowsOS(){
		boolean iswin = Util.isWindowsOS();
		String os = System.getProperty("os.name");
		boolean haswin = os.contains("Windows");
		assertEquals(haswin, iswin);
	}
	
	@Test
	public void isWindows32OS(){
		boolean iswin = Util.isWindows32OS();
		String os = System.getProperty("os.name");
		boolean haswin32 = os.contains("Windows") && !Util.isWindows64OS();
		assertEquals(haswin32, iswin);
	}
	
	@Test
	public void isWindows64OS(){
		boolean iswin = Util.isWindows64OS();
		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		boolean haswin64 = os.contains("Windows") && arch.contains("64");
		assertEquals(haswin64, iswin);
	}
	
	@Test
	public void isLinuxOS(){
		boolean iswin = Util.isLinuxOS();
		String os = System.getProperty("os.name");
		boolean haswinLinux = os.contains("Linux");
		assertEquals(haswinLinux, iswin);
	}
	
	@Test
	public void getAppPath(){
		String getAppPath = Util.getAppPath();
		String appPath = System.getProperty("user.dir");
		assertEquals(appPath, getAppPath);
	}
	
	@Test
	public void getAROTraceDirIOS(){
		String dirname = Util.getAROTraceDirIOS();
		boolean hasname = dirname.contains("VideoOptimizerTraceIOS");
		assertTrue(hasname);
	}
	@Test
	public void getAROTraceDirAndroid(){
		String dirname = Util.getAROTraceDirAndroid();
		boolean hasname = dirname.contains("VideoOptimizerAndroid");
		assertTrue(hasname);
	}
	
	@Test
	public void getVideoOptimizerLibrary(){
		String dirname = Util.getVideoOptimizerLibrary();
		boolean hasname = dirname.contains("VideoOptimizerLibrary");
		assertTrue(hasname);
	}
	@Test
	public void getCurrentRunningDir(){
		String dir = Util.getCurrentRunningDir();
		assertNotNull(dir);
	}
	@Test
	public void escapeRegularExpressionChar(){
		String str = "string with regex . char $ % *";
		String newstr = Util.escapeRegularExpressionChar(str);
		boolean hasspecialchar = newstr.contains("\\$");
		assertEquals(true, hasspecialchar);
	}
	@Test
	public void getDefaultAppName(){
		String name = Util.getDefaultAppName("");
		assertEquals("unknown", name);
		name = Util.getDefaultAppName("test");
		assertEquals("test",name);
	}
	@Test
	public void getDefaultString(){
		String name = Util.getDefaultString("", "default");
		assertEquals("default",name);
		name = Util.getDefaultString(null, "default");
		assertEquals("default",name);
	}
	@Test
	public void isEmptyIsBlank(){
		boolean isemptyorblank = Util.isEmptyIsBlank(null);
		assertTrue(isemptyorblank);
		isemptyorblank = Util.isEmptyIsBlank(" ");
		assertTrue(isemptyorblank);
	}
	@Test
	public void normalizeTime(){
		double value = Util.normalizeTime(-0.00, 1.2);
		assertNotNull(value > 0);
	}
	@Test
	public void makeLibFilesFromJar(){
		String foldername = Util.makeLibFilesFromJar(aroJpcapLibFileName);
		assertNotNull(foldername);
		foldername = Util.makeLibFilesFromJar(null);
		assertNull(foldername);
	}
	@Test
	public void makeLibFolder(){
		String libfolder = Util.makeLibFolder(aroJpcapLibFileName, new File(aroJpcapLibName));
		assertNotNull(libfolder);
		libfolder = Util.makeLibFolder(aroJpcapLibFileName, new File(""));
		assertTrue(libfolder.equals(""));
		libfolder = Util.makeLibFolder("", new File(""));
		assertTrue(libfolder.equals(""));
	}
	@Test
	public void makeLibFile() {
		boolean result = Util.makeLibFile(aroJpcapLibFileName, aroJpcapLibFileName, null);
		assertFalse(result);
		result = Util.makeLibFile(null, null, null);
		assertFalse(result);
	}
	
	@Test
	public void testComparator() {
		Comparator<String> result = Util.getDomainSorter();
		assertNotNull(result);
		assertNotNull(result.compare("", ""));
		assertNotEquals(0,result.compare("100.100.100.100", "100.100.100.101"));
		assertEquals(0,result.compare("100.100.100.100", "100.100.100.100"));
	}
	
	@Test
	public void testFloatComparator() {
		Comparator<String> result = Util.getFloatSorter();
		assertNotNull(result);
		assertNotEquals(0,result.compare("22.13", "1.02"));
		assertEquals(0,result.compare("3.19", "3.19"));
	}
	
	@Test
	public void testIntComparator() {
		Comparator<Integer> result = Util.getIntSorter();
		assertNotNull(result);
		assertEquals(0,result.compare(1,1));
		assertEquals(-1,result.compare(1, 2));
		assertEquals(1,result.compare(2, 1));
	}

	
	@Test
	public void test_intCheckPassFailorWarning() {
		BPResultType bpResultType = Util.checkPassFailorWarning(2, 1, 4);
		assertEquals(bpResultType, BPResultType.WARNING);
	}
	
	@Test
	public void test_checkPassFailorWarning() {
		BPResultType bpResultType = Util.checkPassFailorWarning(0.2, 0.1, 0.4);
		assertEquals(bpResultType, BPResultType.WARNING);
	}
	
	@Test
	public void testGetLogLvl() {
		assertNotNull(Util.getLoggingLvl("INFO"));
		assertEquals(Level.INFO, Util.getLoggingLvl("INFO"));
	}
	
	@Test
	public void testGetLoggingLvl() {
		assertNotNull(Util.getLoggingLvl("ERROR"));
		SettingsImpl.getInstance().setAttribute("LOG_LEVEL", "ERROR");
		assertEquals("ERROR", Util.getLoggingLevel());
	}
	
	@Test
	public void test_EmptySpace() {
		String adblink = "C:\\Program Files\\test\\adbfolder";
		String validatedLink = Util.validateInputLink(adblink);
		assertNotNull(validatedLink);
		assertTrue(validatedLink.contains(" "));
	}
	@Test
	public void getEditCap(){
		String editCap = Util.getEditCap();
		assertNotNull(editCap);
	}
	
	@Test
	public void getFFPROBE(){
		String editCap = Util.getFFPROBE();
		assertNotNull(editCap);
	}
	
	
	@Test
	public void getFFMPEG(){
		String editCap = Util.getFFMPEG();
		assertNotNull(editCap);
	}
	
	@Test
	public void testFormatDecimal_NegativeMaxFractionDigitReplacedByZero() {
		
		BigDecimal number = new BigDecimal("19801.1").setScale(2, RoundingMode.HALF_UP);
		String formattedNumber = Util.formatDecimal(number, -1, 0);
		
		assertEquals("19801", formattedNumber);
	}
	
	@Test
	public void testParseForUTC_when_no_dashes_or_colons() {
		long result;
		result = Util.parseForUTC("20191101T0243301975739");	assertEquals(1572601410198L, result);
		result = Util.parseForUTC("20191101T024330197");	    assertEquals(1572601410197L, result);
		result = Util.parseForUTC("20180111T221459");		    assertEquals(1515737699000L, result);
		result = Util.parseForUTC("20180111T221459456");	    assertEquals(1515737699456L, result);
		result = Util.parseForUTC("2018-01-11T22:14:59");	    assertEquals(1515737699000L, result);
		result = Util.parseForUTC("20180111T221459000");	    assertEquals(1515737699000L, result);
		result = Util.parseForUTC("20180111T221459");		    assertEquals(1515737699000L, result);
		result = Util.parseForUTC("20191106T172805265");	    assertEquals(1573090085265L, result);
		result = Util.parseForUTC("20161122T223027123");	    assertEquals(1479882627123L, result);
	}
	
	@Test
	public void testParseForUTC() throws Exception{
		assertEquals(1649962687000L, Util.parseForUTC("2022-04-14T18:58:07Z"));
		assertEquals(1649962687000L, Util.parseForUTC("2022-04-14T11:58:07-0700"));
		assertEquals(1649962687000L, Util.parseForUTC("2022-04-14T11:58:07"));
		assertEquals(1649962698735L, Util.parseForUTC("2022-04-14 11:58:18.073562"));
		assertEquals(1649962698735L, Util.parseForUTC("2022-04-14T18:58:18.073562Z"));
	}
	
	@Test
	public void roundTripTestUTC() throws Exception{
		String thatTimeStr = "18:58:07.123";
		double thatTime = Util.parseTimeOfDay(thatTimeStr, false);
		String nowTimeStr = Util.formatHHMMSSs(thatTime).trim();
		double nownowTime = Util.parseTimeOfDay(nowTimeStr, false);
		assertEquals(thatTime, nownowTime, 0);
	}
	
	@Test
	public void testWrapPasswordForEcho() throws Exception {
		String raw = "\\one!two$three'four";
		String converted = "$'\\x5cone\\x21two$three\\x27four'";
		String result = Util.wrapPasswordForEcho(raw);
		assertEquals(converted, result);
	}
	
	@Test
	public void testParseTimeOfDay()  throws Exception{
		assertEquals(59.567, Util.parseTimeOfDay("59.567", false), 0);
		assertEquals(86400.0, Util.parseTimeOfDay("23:59:60", false), 0);
		assertEquals(86400.0, Util.parseTimeOfDay("11:59:60", true), 0);
		assertEquals(43200.0, Util.parseTimeOfDay("11:59:60", false), 0);
		assertEquals(86400.0, Util.parseTimeOfDay("24:00:00", false), 0);
		
		try {
			Util.parseTimeOfDay("12:00:01", true);
		} catch (Exception e) {
			assertEquals("Illegal time value in (12:00:01) when using PM flag (adding 12hr of seconds)", e.getMessage());
		}
		try {
			Util.parseTimeOfDay("25:00:00", false);
		} catch (Exception e) {
			assertEquals("Illegal time value (25) in (25:00:00)", e.getMessage());
		}
		try {
			Util.parseTimeOfDay("20:67:00", false);
		} catch (Exception e) {
			assertEquals("Illegal time value (67) in (20:67:00)", e.getMessage());
		}
		try {
			Util.parseTimeOfDay("00:00:16878289", false);
		} catch (Exception e) {
			assertEquals("Illegal time value (16878289) in (00:00:16878289)", e.getMessage());
		}
		try {
			Util.parseTimeOfDay("00:00:-30", false);
		} catch (Exception e) {
			assertEquals("Illegal time value (-30) in (00:00:-30)", e.getMessage());
		}
	}
}
