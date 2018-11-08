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
		String osname = Util.OS_NAME, osarch = Util.OS_ARCHYTECTURE;
		if (Util.OS_NAME != null && Util.OS_ARCHYTECTURE != null) {
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
		Comparator<Integer> result = Util.getDomainIntSorter();
		assertNotNull(result);
		assertNotEquals(0,result.compare(1, 2));
		assertEquals(0,result.compare(1,1));
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
		assertTrue(validatedLink.contains("\""));
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
	public void testParseForUTC() {

		long result;
		
		result = Util.parseForUTC("2018-01-11T22:14:59.000000Z");
		assertEquals(1515708899000L, result);
		
		result = Util.parseForUTC("2018-01-11T22:14:59");
		assertEquals(1515708899000L, result);
		
		result = Util.parseForUTC("2018-01-11 22:14:59");
		assertEquals(1515708899000L, result);
		
	}
	
	@Test
	public void testWrapPasswordForEcho() throws Exception {
		String raw = "\\one!two$three'four";
		String converted = "$'\\x5cone\\x21two$three\\x27four'";
		String result = Util.wrapPasswordForEcho(raw);
		assertEquals(converted, result);
	}
}
