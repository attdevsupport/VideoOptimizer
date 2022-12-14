/*
 *  Copyright 2014 AT&T
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

import static org.apache.commons.lang.StringEscapeUtils.escapeCsv;
import static org.apache.commons.lang.StringEscapeUtils.escapeJava;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.settings.impl.SettingsImpl;

public final class Util {

	public final static Logger LOG = Logger.getLogger(Util.class.getName());
	
	public static final StringParse stringParse = new StringParse();
	public static final String WIRESHARK_PATH = "WIRESHARK_PATH";
	public static final String FFMPEG = "ffmpeg";
	public static final String RECENT_TRACES = "RECENT_TRACES";
	private static final int RECENT_TRACES_MAXSIZE = 15;
	public static final String FFPROBE = "ffprobe";
	public static final String IDEVICESCREENSHOT = "iDeviceScreenshot";
	public static final String JDK_VERSION = System.getProperty("java.version");
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
	public static final String SYSTEM_ROOT = System.getenv("SystemRoot");
	private static final String QUOTE = "\"";
	private static final double TIME_CORRECTION = 1.0E9;
	private static Comparator<String> comparator;
	private static Comparator<String> floatValComparator;
	private static Comparator<Integer> intComparator;
	private static Logger logger = LogManager.getLogger(Util.class.getName());
	private static final IExternalProcessRunner EXTERNAL_PROCESS_RUNNER = SpringContextUtil.getInstance().getContext()
			.getBean(IExternalProcessRunner.class);
	public static final String OS_NAME = System.getProperty("os.name");
	public static final String OS_VERSION = extractVersion();
	public static final String OS_ARCHITECTURE = System.getProperty("os.arch");
	
	private static final Pattern htmlEncodePattern = Pattern.compile("%[a-fA-F0-9]");	
	
	public static String APK_FILE_NAME = "VPNCollector-4.5.%s.apk";
	
	public static final String ARO_PACKAGE_NAME = "com.att.arocollector";

	public static final String USER_PATH = 
			isMacOS()
			? StringParse.findLabeledDataFromString("PATH=\"", "\"", EXTERNAL_PROCESS_RUNNER.executeCmd("/usr/libexec/path_helper"))
			: System.getProperty("user.dir");

	public static String getUserPath() {
		return USER_PATH;
	}
	
	public static void restart(boolean isError) {
		LOG.info("Restarting now!");
		try {
			// java binary
			String java = System.getProperty("java.home") + "/bin/java";

			// vm arguments
			List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
			StringBuilder vmArgsOneLine = new StringBuilder();
			String applicationPath = "";
			for (String arg : vmArguments) {
				// if it's the agent argument : we ignore it otherwise the
				// address of the old application and the new one will be in conflict
				if (!arg.contains("-agentlib")) {
					vmArgsOneLine.append("\"" + arg + "\"");
					vmArgsOneLine.append(" ");
				}

				if (arg.contains("exe4j.moduleName") && (Util.isMacOS() || Util.isWindowsOS())) {
					String[] split = arg.split("="); // -Dexe4j.moduleName=/Applications/VideoOptimizer.app
					if (split != null && split.length == 2) {
						applicationPath = split[1];
						vmArgsOneLine.append("\"-Xdock:icon=" + applicationPath + "/Contents/Resources/app.icns\" ");
					}

					if (Util.isWindowsOS()) {
						break;
					}
				}
			}

			final String command;
			if (Util.isWindowsOS()) {
				command = "\"" + applicationPath + "\"";
			} else {
				// init the command to execute, add the vm args
				final StringBuilder cmd = new StringBuilder("\"" + java + "\" " + vmArgsOneLine);
				// program main and program arguments
				String[] mainCommand = System.getProperty("sun.java.command").split(" ");
				// program main is a jar
				if (mainCommand[0].endsWith(".jar")) {
					// if it's a jar, add -jar mainJar
					cmd.append("-jar " + new File(mainCommand[0]).getPath());
				} else {
					// else it's a .class, add the classpath and mainClass
					cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
				}
	
				// finally add program arguments
				for (int i = 1; i < mainCommand.length; i++) {
					cmd.append(" ");
					cmd.append(mainCommand[i]);
				}

				command = cmd.toString();
			}

			// execute the command in a shutdown hook, to be sure that all the
			// resources have been disposed before restarting the application
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						LOG.trace(command + " error=" + isError);
						EXTERNAL_PROCESS_RUNNER.executeCmd(command + " error=" + isError, false, false);
						LOG.info("Successfully Restarted the system!");
					} catch (Exception e) {
						LOG.error("Error while restarting", e);
					}
				}
			});

			System.exit(0);
		} catch (Exception e) {
			LOG.error("Error while trying to restart the application", e);
		}
	}

	private static String extractVersion() {
		if (OS_VERSION != null) {
			return OS_VERSION;
		}
		String version = "unknown";

		if (isWindowsOS()) {
			String cmd = "systeminfo|findstr Build";
			String result = EXTERNAL_PROCESS_RUNNER.executeCmd(cmd);
			version = StringParse.findLabeledDataFromString("OS Version:", System.lineSeparator(), result).trim();
		} else if (isMacOS()) {
			String cmd = "sw_vers";
			String result = EXTERNAL_PROCESS_RUNNER.executeCmd(cmd);
			version = StringParse.findLabeledDataFromString("ProductVersion:	", System.lineSeparator(), result);
		} else if (isLinuxOS()) {
			String cmd = "lsb_release -rs";
			version = EXTERNAL_PROCESS_RUNNER.executeCmd(cmd);
		}
		return version;
	}

	public static boolean isMacOS() {
		return Util.OS_NAME.contains("Mac OS");
	}

	/**
	 * Determines whether OS is Windows, which can be 32 bit or 64 bit.
	 */
	public static boolean isWindowsOS() {
		return Util.OS_NAME.contains("Windows");
	}

	/**
	 * Determines whether OS is Windows 32 bit.
	 */
	public static boolean isWindows32OS() {
		return Util.OS_NAME.contains("Windows") && !isWindows64OS();
	}

	/**
	 * Determines whether OS is Windows 64 bit.
	 */
	public static boolean isWindows64OS() {
		String arch = System.getenv("PROCESSOR_ARCHITECTURE");
		String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
		return (arch != null && arch.endsWith("64")) || (wow64Arch != null && wow64Arch.endsWith("64"));
	}

	/**
	 * Determines whether OS is Linux.
	 */
	public static boolean isLinuxOS() {
		return Util.OS_NAME.contains("Linux") || Util.OS_NAME.contains("LINUX");
	}

	/**
	 * Returns the path to the Java Application
	 *
	 * @return path to the Application
	 */
	public static String getAppPath() {
		return System.getProperty("user.dir");
	}

	/**
	 * Returns package.Class::methodName of method enclosing call to this method
	 * 
	 * @return
	 */
	public static String getMethod() {
		StackTraceElement traceElement = Thread.currentThread().getStackTrace()[2];
		String name = null; // traceElement.getClassName() + "::" +
							// traceElement.getMethodName();
		name = ((traceElement.getFileName()).split("\\."))[0] + "::" + traceElement.getMethodName() + "(...)";
		return name;
	}

	/**
	 * location to save trace data such as pcap, video etc. used by non-rooted
	 * IOS
	 * 
	 * @return
	 */
	public static String getAROTraceDirIOS() {
		return System.getProperty("user.home") + FILE_SEPARATOR + "VideoOptimizerTraceIOS";
	}

	/**
	 * location of AroLibrary
	 * 
	 * @return
	 */
	@Deprecated
	public static String getAroLibrary() {
		return System.getProperty("user.home") + FILE_SEPARATOR + "AroLibrary";
	}

	/**
	 * location of VideoOptimizerLibrary
	 * 
	 * @return
	 */
	public static String getVideoOptimizerLibrary() {
		return System.getProperty("user.home") + FILE_SEPARATOR + "VideoOptimizerLibrary";
	}

	/**
	 * Location of VideoOptimizerLibrary/VideoOptimizerLibrary
	 * 
	 * @return
	 */
	public static String getExtractedDrivers() {
		return Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + "ExtractedDrivers" + Util.FILE_SEPARATOR;
	}

	/**
	 * location to save trace data such as pcap, video etc. used by non-rooted
	 * Android
	 * 
	 * @return
	 */
	public static String getAROTraceDirAndroid() {
		return System.getProperty("user.home") + FILE_SEPARATOR + "VideoOptimizerAndroid";
	}

	/**
	 * will return the full path of dir where ARO.jar is running from.
	 * 
	 * @return full path of directory
	 */
	public static String getCurrentRunningDir() {
		String dir = "";
		File filepath = new File(Util.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		dir = filepath.getParent();
		return dir;
	}

	/**
	 * Escape regular expression char so that it won't execute
	 * 
	 * @param str
	 *            String to escape special chars
	 * @return
	 */
	public static String escapeRegularExpressionChar(String str) {
		String token = str.replace("$", "\\$");
		token = token.replace("^", "\\^");
		token = token.replace("*", "\\*");
		token = token.replace(".", "\\.");
		token = token.replace("?", "\\?");
		return token;
	}

	/**
	 * <pre>
	 * Returns a string representing Unknown App if appName is empty, blank, or
	 * null. Otherwise returns appName.
	 */
	public static String getDefaultAppName(String appName) {
		return getDefaultString(appName, "unknown");
	}

	/**
	 * <pre>
	 * Returns defaultStr if str is empty, blank, or null. Otherwise returns
	 * str.
	 */
	public static String getDefaultString(String str, String defaultStr) {
		return isEmptyIsBlank(str) ? defaultStr : str;
	}

	/**
	 * Returns false if sting is empty, blank, or null
	 */
	public static Boolean isEmptyIsBlank(String str) {
		return (str == null || str.trim().isEmpty());
	}

	/**
	 * <pre>
	 * Normalizes the collected time with respect to the trace start time.
	 *
	 * @param time
	 *            - The time value to be normalized.
	 * @param pcapTime
	 *            - The trace start time.
	 * @return The normalized time in double.
	 */
	public static double normalizeTime(double time, double pcapTime) {
		double tmpTime;
		// The comparison check here is for backward compatibility
		tmpTime = time > TIME_CORRECTION ? time - pcapTime : time;
		if (tmpTime < 0) {
			tmpTime = 0.0;
		}
		return tmpTime;
	}

	/**
	 * Convert hh:mm:ss.sss to seconds
	 * Convert mm:ss.sss to seconds
	 * 
	 * @param hhmmss
	 * @param pm		flag to indicate PM
	 * @return
	 */
	public static double parseTimeOfDay(String hhmmss, boolean pm) throws Exception {
		double seconds = 0;
		String[] timeparts = fillFormat(hhmmss).split(":");
		int[] max = { 24, 60, 60 };
		if (pm) {
			max[0] = 12;
		}
		int[] multiplier = { 3600, 60, 1 };
		for (int idx = 0; idx < timeparts.length; idx++) {
			if (Double.valueOf(timeparts[idx]) > max[idx]) {
				throw new Exception(String.format("Illegal time value (%s) in (%s)", timeparts[idx], hhmmss));
			}
			seconds += (double) (Double.valueOf(timeparts[idx]) * multiplier[idx]);
		}

		if (pm) {
			if (seconds > 43200) {
				throw new Exception(String.format("Illegal time value in (%s) when using PM flag (adding 12hr of seconds)", hhmmss));
			}
			seconds += 43200;
		}
		return seconds;
	}
	
	/**
	 * Guarantee that hh:mm:ss.s has all fields
	 * 
	 * @param hhmmss
	 * @return
	 */
	private static String fillFormat(String hhmmss) {
		int sectionCount = StringUtils.countMatches(hhmmss, ":");
		if (sectionCount < 2) {
			for (int idx = 2-sectionCount; idx > 0; idx--) {
				hhmmss = "00:" + hhmmss;
			}
		}
		return hhmmss;
	}

	/**
	 * <pre>
	 * Supported date formats: 
	 * 
	 *  2018-01-11T22:14:59.123000Z 
	 *  2018-01-11T22:14:59
	 *  2018-01-11 22:14:59
	 *  
	 *	0123456789012345678901
	 *	20191101T0243301975739	where characters beyond 18 are rounded into results from the first 18
	 *  20191106T172805265
	 *  20180111T221459000
	 *  20180111T221459
	 * 
	 * Nanoseconds are truncated
	 * 
	 * @param creationTime
	 * @return
	 */
	public static long parseForUTC(String creationTime) {
		if (creationTime.contains("-")) {
			return parseForUTC(creationTime, "yyyy-MM-dd HH:mm:ss");
		} else {
			long rounding = 0;
			if (creationTime.length() < 16) {
				creationTime += "000";
			} else if (creationTime.length() > 18) {
				if (Double.valueOf(creationTime.substring(18, 19)) > 4) {
					rounding = 1;
				}
				creationTime = creationTime.substring(0, 18);
			}
			return parseForUTC(creationTime, "yyyyMMdd HHmmssSSS") + rounding;
		}
	}
	
	/**
	 * 
	 * @param creationTime
	 * @param sdFormatStr format string for SimpleDateFormat(sdFormatStr)
	 * @return
	 */
	public static long parseForUTC(String creationTime, String sdFormatStr) {
		String[] tzOffset;
		long milli = 0;
		long mSec = 0;
		if (creationTime != null) {

			int msecPos = creationTime.indexOf('.') + 1;
			SimpleDateFormat sdf = new SimpleDateFormat(sdFormatStr);
			if (creationTime.contains("Z")) {
				creationTime = creationTime.replaceAll("Z", "");
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			} else if ((tzOffset = stringParse.parse(creationTime, ":.*(-\\d{4})")) != null) {
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"+tzOffset[0]));
			} else {
				sdf.setTimeZone(TimeZone.getTimeZone("PST"));
			}
			Date date = null;
			try {
				mSec = msecPos > 0 ? Long.valueOf(creationTime.substring(msecPos).replaceAll("\\d{3}Z", "")) : 0;
				while (mSec > 1000) { // only allow 3 digits of milliseconds
					mSec /= 10;
				}
				creationTime = creationTime.replaceAll("\\d{3}Z", "");
				date = sdf.parse(creationTime.replace('T', ' '));
				milli = date.getTime() + mSec;
			} catch (Exception e) {
				logger.error("Date parsing error :" + e.getMessage());
			}
		}
		return milli;
	}

	private static String getTimeZone() {
		String timezoneID = SettingsImpl.getInstance().getAttribute("timezoneID"); // user can manually store any timezonID for special purposes, manually only
		if (timezoneID == null || timezoneID.isEmpty()) {
			timezoneID = TimeZone.getDefault().getID(); // default to local timezoneID of computer
		}
		return timezoneID;
	}


	
	public static String formatYMD(long timestamp) {
		String pattern = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		return simpleDateFormat.format(new Date(timestamp));
	}

	/**
	 * convert a double into HH:MM:SS.sss
	 * 
	 * @param seconds
	 * @return HH:MM:SS.sss
	 */
	public static String formatHHMMSSs(Double seconds) {
		int intSeconds = seconds.intValue();
		String result = formatHHMMSS(intSeconds);
		result += String.format("%.06f", seconds - intSeconds).substring(1);
		return result;
	}
	
	public static String formatHHMMSS(int seconds) {
		String theTime = "";
		int sec = seconds % 60;
		seconds /= 60;
		int minute = seconds % 60;
		seconds /= 60;
		int hour = seconds % 60;
		try {
			theTime = String.format("%02d:%02d:%02d", hour, minute, sec);
		} catch (Exception exception) {
			theTime = exception.getMessage();
		}
		return theTime;
	}

	/**
	 * Convert remaining time (-0h00m00s000ms) to milliseconds
	 *
	 * @return result in milliseconds
	 */
	public static double convertTime(String time) {
		double result = 0;
		int start = 0;
		int end = 0;
		// Change to positive number
		if (time.indexOf("-") == 0 || time.indexOf("+") == 0) {
			time = time.substring(1);
		}
		if (time.indexOf('T') == 0) {
			time = time.substring(1);
		}
		end = time.indexOf("d");
		if (end > 0) {
			result += Integer.parseInt(time.substring(0, end)) * 24 * 60 * 60 * 1000;
			start = end + 1;
		}
		end = time.indexOf("h");
		if (end > start) {
			result += Integer.parseInt(time.substring(start, end)) * 60 * 60 * 1000;
			start = end + 1;
		}
		end = time.indexOf("m");
		if (end > start && end != time.indexOf("ms")) {
			result += Integer.parseInt(time.substring(start, end)) * 60 * 1000;
			start = end + 1;
		}
		end = time.indexOf("s");
		if (end > start && end != (time.indexOf("ms") + 1)) {
			result += Integer.parseInt(time.substring(start, end)) * 1000;
			start = end + 1;
		}
		end = time.indexOf("ms");
		if (end > start) {
			result += Integer.parseInt(time.substring(start, end));
		}
		return result;
	}

	/**
	 * helper method for debugging info
	 * 
	 * @param bArray
	 *            to generate characters or '.' if not in text range
	 * @return a string of hex characters inside of a pair of square brackets
	 */
	public static String byteArrayToString(byte[] recPayload) {
		StringBuffer sb = new StringBuffer("");
		for (int i = 0; i < recPayload.length; i++) {
			if (recPayload[i] >= 32 || recPayload[i] == '\n' || recPayload[i] == '\r' || recPayload[i] == '\t') {
				sb.append((char) recPayload[i]);
			} else {
				sb.append(".");
			}
		}
		sb.append("");
		return sb.toString();
	}

	/**
	 * helper method for debugging info
	 * 
	 * @param bArray
	 *            to generate characters or '.' if not in text range
	 * @return a string of hex characters inside of a pair of square brackets
	 */
	public static String byteArrayToString(byte[] recPayload, int len) {
		StringBuffer sb = new StringBuffer("[");
		int gotoLen = recPayload.length < len ? recPayload.length : len;
		for (int i = 0; i < gotoLen; i++) {
			if (recPayload[i] > 32) {
				sb.append((char) recPayload[i]);
			} else {
				sb.append(".");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Combine all elements of a String[] into a String
	 *
	 * @param String[]
	 * @return String
	 */
	public static String condenseStringArray(String[] stringArray) {
		StringBuffer strBuf = new StringBuffer();
		if (stringArray != null) {
			for (String string : stringArray) {
				strBuf.append(string);
			}
		}
		return strBuf.toString();
	}

	/**
	 * helper method for debugging info
	 * 
	 * @param bArray
	 *            to generate the hex characters
	 * @return a string of hex characters inside of a pair of square brackets
	 */
	public static String byteArrayToHex(byte[] bArray) {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < bArray.length; i++) {
			sb.append(String.format("%02X", bArray[i]));
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1123 format.
	 */
	private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1036 format.
	 */
	private static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";
	/**
	 * Date format pattern used to parse HTTP date headers in ANSI C
	 * <code>asctime()</code> format.
	 */
	private static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
	private static final String PATTERN_ASCTIME2 = "EEE MMM d HH:mm:ss zzz yyyy";
	private static DateFormat rfc1123 = new SimpleDateFormat(PATTERN_RFC1123);
	private static DateFormat rfc1036 = new SimpleDateFormat(PATTERN_RFC1036);
	private static DateFormat asctime = new SimpleDateFormat(PATTERN_ASCTIME);
	private static DateFormat asctime2 = new SimpleDateFormat(PATTERN_ASCTIME2);
	private static DateFormat[] dateFormats = { rfc1123, rfc1036, asctime, asctime2 };
	private static final Date BEGINNING_OF_TIME = new Date(0);

	/**
	 * Parses HTTP date formats. Synchronized because DateFormat objects are not
	 * thread-safe. If defaultForExpired is true and value is an invalid
	 * dateFormat (such as -1 or 0 meaning already expired), the returned Date
	 * will be "beginning of time" Jan 1 1970.
	 *
	 * @param value
	 * @param defaultForExpired
	 *            boolean - true/false provide default "beginning of time" Jan 1
	 *            1970 GMT Date
	 * @return formated Date value else null.
	 */
	public static Date readHttpDate(String value, boolean defaultForExpired) {
		if (value != null) {
			for (DateFormat dateFormat : dateFormats) {
				try {
					return dateFormat.parse(value.trim());
				} catch (ParseException e) {
					// logger.error(e.getMessage());
				}
			}
		}
		if (defaultForExpired) {
			return BEGINNING_OF_TIME;
		}
		// logger.warn("Unable to parse HTTP date: " + value);
		return null;
	}

	/**
	 * Pull the given file name from Jar and write into the AroLibrary
	 *
	 * @param filename
	 */
	public static String makeLibFilesFromJar(String filename) {
		String targetLibFolder = getVideoOptimizerLibrary();
		ClassLoader aroClassloader = Util.class.getClassLoader();
		try {
			InputStream is = aroClassloader.getResourceAsStream(filename);
			if (is != null) {
				File libfolder = new File(targetLibFolder);
				// if (!libfolder.exists() || !libfolder.isDirectory() || new
				// File(libfolder+File.separator+filename).exists()) {
				targetLibFolder = makeLibFolder(filename, libfolder);
				if (targetLibFolder != null) {
					makeLibFile(filename, targetLibFolder, is);
				} else {
					return null;
				}
			}
			return targetLibFolder;
		} catch (Exception e) {
			logger.error("Failed to extract " + filename + " ," + e.getMessage());
			return null;
		}
	}

	/**
	 * <pre>
	 * makes a folder in the targetLibFolder location. Mac {home}/AROLibrary Win
	 *
	 * if it fails it will create AROLibrary the current application execution
	 * folder
	 *
	 * @param filename
	 * @param currentRelativePath
	 * @param targetLibFolder
	 * @return
	 */
	public static String makeLibFolder(String filename, File libFolder) {
		String targetLibFolder = libFolder.toPath().toString();
		Path currentRelativePath = Paths.get("");
		try {
			Files.createDirectories(libFolder.toPath());
		} catch (IOException ioe1) {
			// if no write access rights to the path folder then extract the lib
			// to a default local folder
			targetLibFolder = currentRelativePath.toAbsolutePath().toString() + File.separator + "AROLibrary";
			try {
				Files.createDirectories(libFolder.toPath());
			} catch (IOException ioe2) {
				return null;
			}
		}
		return targetLibFolder;
	}

	/**
	 * <pre>
	 * "makes" a file inside the targetLibFolder by extracting it from the resources inside AROCore.jar
	 *
	 * Note: The previous version is deleted before extraction.
	 * </pre>
	 *
	 * @param filename
	 * @param targetLibFolder
	 * @param is
	 */
	public static boolean makeLibFile(String filename, String targetLibFolder, InputStream is) {
		try {
			File result = new File(targetLibFolder, filename);
			result.delete();
			OutputStream os = null;
			if (result.createNewFile()) {
				os = new FileOutputStream(result);
				byte[] buffer = new byte[4096];
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
				try {
					is.close();
					os.close();
				} catch (IOException ioe2) {
					// todo
				}
			}
			return true;
		} catch (Exception ioe) {
			return false;
		}
	}

	/**
	 * Load the JNI library from the folder specified by java.library.path
	 * 
	 * @param libName
	 */
	public static boolean loadSystemLibrary(String filename) {
		try {
			System.loadLibrary(filename);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Load the JNI library directly by using the file name
	 * 
	 * @param filename
	 * @param targetLibFolder
	 */
	public static boolean loadLibrary(String filename, String targetLibFolder) {
		try {
			System.load(targetLibFolder + File.separator + filename);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Formats a number so that the number of digits in the fraction portion of
	 * it is bound by a maximum value and a minimum value. <br>
	 * <br>
	 * Examples with maxFractionDigits being 3 and minFractionDigits being 0:
	 * <br>
	 * 2.4535 -> 2.454 <br>
	 * 20 -> 20 <br>
	 * 24535 -> 24535 <br>
	 * 2.5 -> 2.5 <br>
	 * 2.460 -> 2.46 <br>
	 * 2.40 -> 2.4 <br>
	 * 3.12 -> 3.12 <br>
	 * 9.888 -> 9.888
	 *
	 * @param number
	 * @param maxFractionDigits
	 *            maximum number of fraction digits, replaced by 0 if it is a
	 *            negative value
	 * @param minFractionDigits
	 *            minimum number of fraction digits, replaced by 0 if it is a
	 *            negative value
	 * @return
	 */
	public static String formatDecimal(BigDecimal number, int maxFractionDigits, int minFractionDigits) {
		DecimalFormat df = new DecimalFormat();
		df.setGroupingUsed(false);
		df.setMaximumFractionDigits(maxFractionDigits);
		df.setMinimumFractionDigits(minFractionDigits);
		return df.format(number);
	}

	public static String getBinPath() {
		String path;
		if (isWindowsOS() || isMacOS()) {
			path = "";
		} else {
			path = "/usr/bin/";
		}		
		return path;
	}

	public static String getWireshark() {
		String config = SettingsImpl.getInstance().getAttribute(WIRESHARK_PATH);
		if (StringUtils.isNotBlank(config)) {
			return config;
		}

		if (Util.isWindowsOS()) {
			return "Wireshark.exe";
		} else {
			if (Util.isMacOS()) {
				return "/Applications/Wireshark.app";
			} else {
				return getBinPath() + "wireshark";
			}
		}
	}

	public static String getDumpCap() {
		return getWiresharkLibraryPath("dumpcap");
	}

	public static String getTshark() {
		return getWiresharkLibraryPath("tshark");
	}

	public static String getCapinfos() {
		return getWiresharkLibraryPath("capinfos");
	}

	private static String getWiresharkLibraryPath(String libraryName) {
		String path;
		if (isWindowsOS()) {
			path = libraryName + ".exe";
		} else {
			if (isMacOS()) {
				path = getWireshark() + "/Contents/MacOS/" + libraryName;
				return validateInputLink(path);
			} else {
				path = getBinPath() + libraryName;
			}
		}

		return path;
	}

	public static String getFFMPEG() {
		String config = SettingsImpl.getInstance().getAttribute(FFMPEG);
		if (StringUtils.isNotBlank(config)) {
			return validateInputLink(config);
		}
		return "ffmpeg";
	}

	public static String getFFPROBE() {
		String config = SettingsImpl.getInstance().getAttribute(FFPROBE);
		if (StringUtils.isNotBlank(config)) {
			return validateInputLink(config);
		}
		return "ffprobe";
	}

	public static String getIfuse() {
		return getBinPath() + "ifuse";
	}

	public static String getEditCap() {
		return getBinPath() + "editcap";
	}

	public static boolean isJPG(File imgfile, String imgExtn) {
		boolean isJPG = false;
		if (imgfile.isFile() && imgfile.length() > 0) {
			if ((imgExtn.equalsIgnoreCase("jpeg") || imgExtn.equalsIgnoreCase("jpg"))) {
				try (FileInputStream fis = new FileInputStream(imgfile);
						BufferedInputStream bis = new BufferedInputStream(fis);
						DataInputStream inputStrm = new DataInputStream(bis)) {
					if (inputStrm.readInt() == 0xffd8ffe0) {
						isJPG = true;
					}
				} catch (Exception e) {
					logger.info("Image Format check jpeg exception : ", e);
				}
			}
		}
		return isJPG;
	}

	public static double doubleFileSize(double mdataSize) {
		return Double.valueOf(new DecimalFormat("###.#").format(mdataSize / 1024.00));
	}

	/**
	 * Extracts the object name from the request.
	 * 
	 * @param hrri
	 * @return object name
	 */
	public static String extractFullNameFromRequest(HttpRequestResponseInfo hrri) {
		HttpRequestResponseInfo req = hrri.getDirection().equals(HttpDirection.RESPONSE) ? hrri.getAssocReqResp()
				: hrri;
		String extractedName = "";
		String objectName = "";
		if (req != null) {
			String fullPathName = req.getObjName();
			if (fullPathName != null) {
				objectName = fullPathName.substring(fullPathName.lastIndexOf("/") + 1);
				int pos = objectName.lastIndexOf("/") + 1;
				extractedName = objectName.substring(pos);
			}
		}
		return extractedName;
	}

	/**
	 * <pre>
	 * Sorts domain numbers and names. matches on ###.###.###.### where # is any
	 * number. then builds a double based on address to compare. if alpha is
	 * involved then alpha comparison is used.
	 * 
	 * @return
	 */
	public static Comparator<String> getDomainSorter() {
		if (comparator == null) {
			comparator = new Comparator<String>() {
				Pattern pattern = Pattern.compile("([0-9]*)\\.([0-9]*)\\.([0-9]*)\\.([0-9]*)");

				@Override
				public int compare(String o1, String o2) {
					if (pattern != null && pattern.matcher(o1).find() && pattern.matcher(o2).find()) {
						return getDomainVal(o1).compareTo(getDomainVal(o2));
					}
					return o1.compareTo(o2);
				}

				/**
				 * Convert xxx.xxx.xxx.xxx to a Double
				 *
				 * @param domain
				 * @return
				 */
				private Double getDomainVal(String domain) {
					Double val = 0D;
					String[] temp = domain.split("\\.");
					if (temp != null) {
						try {
							for (int idx = 0; idx < temp.length; idx++) {
								val = (val * 256) + Integer.valueOf(temp[idx]);
							}
						} catch (NumberFormatException e) {
							val = 0D;
						}
					}
					return val;
				}
			};
		}
		return comparator;
	}

	public static BPResultType checkPassFailorWarning(int resultSize, int warning, int fail) {
		BPResultType bpResultType = BPResultType.PASS;
		if (resultSize >= warning) {
			if (resultSize >= fail) {
				bpResultType = BPResultType.FAIL;
			} else {
				bpResultType = BPResultType.WARNING;
			}
		}
		return bpResultType;
	}

	/**
	 * <pre>
	 * Compares and returns a PASS, WARNING, or FAIL.
	 *
	 * @param resultValue
	 *            discovered value from analysis test
	 * @param warning
	 *            trigger, values below this are pass
	 * @param fail
	 *            trigger values from here and up are a failure
	 * @return
	 */
	public static BPResultType checkPassFailorWarning(double resultValue, double warning, double fail) {
		BPResultType bpResultType = BPResultType.PASS;
		if (resultValue >= warning) {
			if (resultValue >= fail) {
				bpResultType = BPResultType.FAIL;
			} else {
				bpResultType = BPResultType.WARNING;
			}
		}
		return bpResultType;
	}
	
	public static BPResultType checkPassFailorWarning(double resultValue, double warning) {
		BPResultType bpResultType = BPResultType.PASS;
		if (resultValue >= warning) {
			bpResultType = BPResultType.WARNING;
		}
		return bpResultType;
	}

	public static Level getLoggingLvl(String loggingLvl) {
		Level level = Level.ERROR;
		if (loggingLvl != null) {
			if (loggingLvl.equalsIgnoreCase("WARN")) {
				level = Level.WARN;
			} else if (loggingLvl.equalsIgnoreCase("INFO")) {
				level = Level.INFO;
			} else if (loggingLvl.equalsIgnoreCase("DEBUG")) {
				level = Level.DEBUG;
			} else if (loggingLvl.equalsIgnoreCase("TRACE")) {
				level = Level.TRACE;
			}
		}
		return level;
	}

	public static void setLoggingLevel(String logginglevel) {
		setLoggingLevel(getLoggingLvl(logginglevel));
	}

	public static void setLoggingLevel(Level loggingLevel) {
		Logger.getRootLogger().setLevel(loggingLevel);
	}

	public static String getLoggingLevel() {
		String logLevel = "ERROR";
		if (SettingsImpl.getInstance().getAttribute("LOG_LEVEL") != null) {
			logLevel = SettingsImpl.getInstance().getAttribute("LOG_LEVEL");
		}
		return logLevel;
	}
	
	public static String getAttribute(String key) {
		String value;
		return (value = SettingsImpl.getInstance().getAttribute(key)) != null ? value : "";
	}
	
	public static boolean checkMode(String key, String matchVal) {
		return SettingsImpl.getInstance().checkAttributeValue(key, matchVal);
	}
	
	public static boolean checkDevMode() {
		return checkMode("env", "dev");
	}
	
	public static boolean isTestMode() {
		return checkMode("test_only", "true");
	}

	public static Comparator<String> getFloatSorter() {
		if (floatValComparator == null) {
			floatValComparator = new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return Double.compare(Double.parseDouble(o1), Double.parseDouble(o2));
				}
			};
		}
		return floatValComparator;
	}

	public static Comparator<Integer> getIntSorter() {
		intComparator = new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return Integer.compare(a, b);
			}
		};
		return intComparator;
	}

	public static String getIdeviceScreenshot() {
		String config = SettingsImpl.getInstance().getAttribute(IDEVICESCREENSHOT);
		if (StringUtils.isNotBlank(config)) {
			return config;
		}
		return "idevicescreenshot";
	}

	public static String percentageFormat(double inputValue) {
		DecimalFormat dFormat = new DecimalFormat("#.##");
		return dFormat.format(inputValue);
	}

	public static String extractFullNameFromLink(String objName) {
		String imageName = "";
		if (objName.indexOf("?v=") > 0 && (objName.indexOf("&") > 0)) {
			imageName = objName.substring(objName.indexOf("?v=") + 3, objName.indexOf("&"));
		}
		return imageName;
	}

	public static String parseImageName(String originalImage, HttpRequestResponseInfo reqResp) {
		String imageName = Util.extractFullNameFromLink(originalImage);
		if (!imageName.isEmpty()) {
			originalImage = imageName + "." + reqResp.getContentType()
					.substring(reqResp.getContentType().indexOf("image/") + 6, reqResp.getContentType().length());
		}
		return originalImage;
	}

	/**
	 * Extract a PNG from a video at a defined time
	 * 
	 * @param timestamp
	 *            in seconds from start of video
	 * @param videoPath
	 *            path of video file
	 * @param ximagePath
	 *            destination path for PNG file
	 * @return destination path for PNG file
	 */
	public static String extractFrameToPNG(double timestamp, String videoPath, String ximagePath) {
		String cmd = String.format("%s -y -i " + "\"%s\"" + " -ss %.0f -vframes 1 \"%s\"", Util.getFFMPEG(), videoPath, timestamp, ximagePath);
		EXTERNAL_PROCESS_RUNNER.executeCmd(cmd);
		return ximagePath;
	}

	public static String validateInputLink(String inputValue) {
		if (StringUtils.isNotBlank(inputValue) && Util.isWindowsOS() && inputValue.split("\\\\").length > 1) {
			inputValue = wrapText(inputValue);
		}
		return inputValue;
	}

	public static String escapeChars(String inputValue) {
		String result = inputValue;
		char[] chArray = inputValue.toCharArray();
		for (int i = 0; i < chArray.length; i++) {
			int ascii = (int) chArray[i];
			// number ranges in the condition check showing special characters
			// ascii value ranges
			if ((ascii >= 32 && ascii <= 45) || (ascii >= 58 && ascii <= 64) || (ascii >= 91 && ascii <= 96)
					|| (ascii >= 123 && ascii <= 126)) {
				if (!result.contains("\\" + chArray[i])) {
					result = result.replace("" + chArray[i], "\\" + chArray[i]);
				}
			}
		}

		return result;
	}

	/***
	 * Reads the RECENT_TRACES parameter from the config.properties and creates a
	 * map with folder as key and tracepath as value
	 * 
	 * @return Map<String, String
	 */
	public static Map<String, String> getRecentOpenMenuItems() {
		Map<String, String> recentMenuItems = new LinkedHashMap<String, String>();
		String recentTraces = SettingsImpl.getInstance().getAttribute(RECENT_TRACES);
		if (!StringUtils.isEmpty(recentTraces)) {
			if (recentTraces.charAt(0) != '\"') {
				// If see any really old, single recent format, wipe it out
				SettingsImpl.getInstance().setAndSaveAttribute(RECENT_TRACES, "");
			} else {
				String[] recentItems = getRecentlyOpenedTraces();
				if (recentItems != null) {
					for (int i = 0; i < recentItems.length; i++) {
						if (recentItems[i] != null) {
							recentItems[i] = checkForCsvEscapedCharacters(recentItems[i]);
							String recentMenuItem = getTraceName(recentItems[i]);
							recentItems[i] = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeCsv(recentItems[i]));
							if (!recentMenuItem.isEmpty() && new File(recentItems[i]).exists()) {
								recentMenuItems.put(recentItems[i], recentMenuItem);
							}
						}
					}
				}
			}
		}
		return recentMenuItems;
	}

	private static String checkForCsvEscapedCharacters(String recentItem) {
		if (recentItem.contains("\"") || recentItem.contains(",") || recentItem.contains(LINE_SEPARATOR)) {
			StringBuilder sb = new StringBuilder();
			sb.append(QUOTE);
			sb.append(recentItem);
			sb.append(QUOTE);
			return sb.toString();
		}
		return recentItem;
	}
	
	public static String[] getRecentlyOpenedTraces() {
		String recentMenuConfig = SettingsImpl.getInstance().getAttribute(RECENT_TRACES);
		String[] recentMenuItem = recentMenuConfig.split("(?<!\\\\)\",\"");
		recentMenuItem[0] = recentMenuItem[0].replaceFirst("\"", "");
		int len = recentMenuItem.length - 1;
		recentMenuItem[len] = (recentMenuItem[len].charAt(recentMenuItem[len].length() - 1) == '\"'
								? recentMenuItem[len].substring(0, recentMenuItem[len].length() - 1)
								: recentMenuItem[len]);
		return recentMenuItem;
	}
	/***
	 * Extracts the trace name from the tracepath
	 * 
	 * @param tracePath
	 * @return String tracename
	 */
	private static String getTraceName(String tracePath) {
		String traceName = "";
		if (tracePath != null) {
			int index = tracePath.lastIndexOf(FILE_SEPARATOR);
			if (index > 0) {
				traceName = tracePath.substring(index + 1);
				if (traceName.charAt(traceName.length() - 1) == '\"') {
					traceName = QUOTE + traceName;
				}
			}
			traceName = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeCsv(traceName));
		}
		return traceName;
	}
	
	/**<pre>
	 * Convert and wrap password for echo through bash to sudo.
	 * The order of conversion is important, whack(\) must be converted first!
	 * 
	 * \ is converted to \x5c
	 * ! is converted to \x21
	 * ' is converted to \x27
	 * null converted to empty string, does not need wrapping
	 * 
	 * @param password
	 * @return wrapped password
	 */
	public static String wrapPasswordForEcho(String password) {
		if (password == null) {
			return "";
		}
		return "$'" + password.replace("\\", "\\x5c").replace("!", "\\x21").replace("'", "\\x27") + "'";
	}

	public static String wrapText(String path){
		if(path == null){
			return "";
		}
		return "\"" + path + "\"";
	}
	
	/**
	 * 
	 * @param traceDirectory is a folder or a cap or pcap file
	 * @return String of full path
	 */
	private static String applyTraceNamingRules(String traceDirectory) {
		File trace = new File(traceDirectory);
		if (trace.isDirectory()) {
			return trace.toString();
		} else {
			if ((new File(trace.getParentFile(), ".temp_trace").exists())) {
				// only cap, pcap, pcapng type files will be added
				return new File(trace.getParentFile().getParentFile(), trace.getName()).toString();
			} else {
				return trace.getParentFile().toString();
			}
		}
	}
	
	/***
	 * Updates the recent trace Directory to the RECENT_TRACES in
	 * config.properties Makes sure there are only 5 or less items in the
	 * attribute
	 * 
	 * @param file
	 */
	public static void updateRecentItem(String file) {
		StringBuilder recentMenuBuilder = new StringBuilder();
		String value = escapeCsv(escapeJava(applyTraceNamingRules(file)));
		
		if (value.startsWith(QUOTE)) {
			recentMenuBuilder.append(value);
		} else {
			recentMenuBuilder.append(QUOTE);
			recentMenuBuilder.append(value);
			recentMenuBuilder.append(QUOTE);
		}
		
		int counter = 1;
		String recentMenuConfig = SettingsImpl.getInstance().getAttribute(RECENT_TRACES);
		if (recentMenuConfig != null && !recentMenuConfig.isEmpty()) {
			if (recentMenuConfig.charAt(0) == '\"') {
				String[] recentMenu = getRecentlyOpenedTraces();
				if (recentMenu != null) {
					for (int i = 0; i < recentMenu.length; i++) {
						if (counter < RECENT_TRACES_MAXSIZE) {
							recentMenu[i] = checkForCsvEscapedCharacters(recentMenu[i]);
							String recentMenuPath = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeCsv(recentMenu[i]));
							if (!compareValues(recentMenu[i], value) && new File(recentMenuPath).exists() && !(new File(recentMenuPath, ".temp_trace").exists())) {
								recentMenuBuilder.append(",");
								String recentValue = recentMenu[i];
								if (recentValue.startsWith(QUOTE)) {
									recentMenuBuilder.append(recentValue);
								} else {
									recentMenuBuilder.append(QUOTE);
									recentMenuBuilder.append(recentValue);
									recentMenuBuilder.append(QUOTE);
								}
								counter++;
							}
						}
					}
				}
			}
		}
		SettingsImpl.getInstance().setAndSaveAttribute("RECENT_TRACES", recentMenuBuilder.toString());
	}
	
	public static boolean isFilesforAnalysisAvailable(File folderPath) {
		boolean isFilesforAnalysisAvailable = false;
		if (folderPath != null && folderPath.exists() && folderPath.isDirectory()) {
			File[] listOfFiles = folderPath.listFiles();
			if (listOfFiles != null && listOfFiles.length != 0) {
				for (int i = 0; i < listOfFiles.length; i++) {
					if (listOfFiles[i].isFile() && listOfFiles[i].getPath().lastIndexOf(".jp") > 0) {
						isFilesforAnalysisAvailable = isJPG(listOfFiles[i],
								listOfFiles[i].getPath().substring(listOfFiles[i].getPath().lastIndexOf(".jp") + 1));
						if (isFilesforAnalysisAvailable) {
							break;
						}
					}
				}
			}
		}
		return isFilesforAnalysisAvailable;
	}

	public static boolean fileExists(String folder, String file) {
		return Files.exists(Paths.get(folder, file));
	}

	private static boolean compareValues(String recentMenu, String value) {
		if (value.startsWith(QUOTE) && value.endsWith(QUOTE) && (!recentMenu.startsWith(QUOTE))) {
			value = value.substring(1, value.length() - 1);
		}
		return recentMenu.equals(value);
	}
	
	public static String formatDouble(double toFormat) {
		DecimalFormat decimalFormatter = new DecimalFormat("#.###");
		return decimalFormatter.format(toFormat);	
	}
	
	public static String formatDoubleToMicro(double toFormat) {
		DecimalFormat decimalFormatter = new DecimalFormat("#.######");
		return decimalFormatter.format(toFormat);	
	}
	
	/**
	 * <p>Decode html<\p>
	 * Will not decode if there are no HTML codes starting with '%'<br>
	 * The Pattern "%[a-fA-F0-9]" is used to find the existence of HTML encoding.<br>
	 * No check is done for the existence of HTTP at the start the oString.
	 * 
	 * @param oString
	 * @return
	 */
	public static String decodeUrlEncoding(String oString) {
		String diff = oString;
		try {
			if (htmlEncodePattern.matcher(oString).find()) { // don't decode if not encoded
				diff = StringUtils.isEmpty(oString) ? "" : URLDecoder.decode(oString, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			LOG.error(String.format("Failed to decode %s :%s", e.getMessage(), oString));
		}
		return diff;
	}
	
	public static String encodeUrlEncoding(String oString) {
		String diff = oString;
		try {
			diff = StringUtils.isEmpty(oString) ? "" : URLEncoder.encode(diff, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			LOG.error(String.format("Failed to encode %s :%s", e.getMessage(), oString));
		}
		return diff;
	}
	
	public static String getNpcapPath1() {
		return Util.isWindowsOS()
				? SYSTEM_ROOT + FILE_SEPARATOR + "system32" + FILE_SEPARATOR + "Npcap" + FILE_SEPARATOR
						+ "wpcap.dll"
				: "";
	}

	public static String getNpcapPath2() {
		return Util.isWindowsOS()
				? SYSTEM_ROOT + FILE_SEPARATOR + "system32" + FILE_SEPARATOR + "Npcap" + FILE_SEPARATOR
						+ "Packet.dll"
				: "";
	}

	public static String getWpcapPath() {
		return Util.isWindowsOS() ? SYSTEM_ROOT + FILE_SEPARATOR + "system32" + FILE_SEPARATOR + "wpcap.dll" : "";
	}

	/**
	 * A handy sleep method for when you do not care about interrupted sleep
	 * 
	 * @param seconds
	 */
	public static void sleep(int seconds) {
		try {
			Thread.sleep(seconds);
		} catch (Exception e) {
			// do not care about this
		}
	}
	
	/**
	 * Provides a height adjustment based on Platform
	 * @param baseHeight
	 * @return
	 */
	public static int getAdjustedHeight(int baseHeight) {
		return Util.isMacOS() ? baseHeight : baseHeight + 10;
	}
	
	/**
	 * Provides a width adjustment based on Platform
	 * @param baseWidth
	 * @return
	 */
	public static int getAdjustedWidth(int baseWidth) {
		return Util.isMacOS() ? baseWidth : baseWidth + 5;
	}

	public static IExternalProcessRunner getExternalProcessRunner() {
		return EXTERNAL_PROCESS_RUNNER;
	}

	public static StringParse getStringParse() {
		return stringParse;
	}
}