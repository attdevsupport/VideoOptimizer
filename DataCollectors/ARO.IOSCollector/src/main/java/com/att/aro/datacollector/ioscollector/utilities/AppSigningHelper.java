/*
*  Copyright 2017 AT&T
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

package com.att.aro.datacollector.ioscollector.utilities;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.resourceextractor.IReadWriteFileExtractor;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.Util;
import com.att.aro.datacollector.ioscollector.app.IOSAppException;

public final class AppSigningHelper {
	private static final Logger LOGGER = LogManager.getLogger(AppSigningHelper.class.getName());	
	private static final String IDEVICE_DEBUG = "/usr/local/bin/idevicedebug";
	private static final String IDEVICE_INSTALLER = "/usr/local/bin/ideviceinstaller";	
	private static final String VO_APP_FILE = "VideoOptimizer.app";
	private static final String VO_ZIP_FILE = "VideoOptimizer.zip";
	private static final String CODE_SIGNATURE_FOLDER_NAME = "_CodeSignature";
	private static final String PROVISIONING_PROFILE_NAME = "embedded.mobileprovision";
	private static final String ENTITLEMENTS_PLIST_FILENAME = "entitlements.plist";
	private static final String INFO_PLIST_FILENAME = "Info.plist";	
	private static final String PROV_FILE_APP_ID_KEY = "application-identifier";
	private static final String PROV_FILE_TEAM_ID_KEY = "com.apple.developer.team-identifier";
	private static final String PROV_FILE_ACCESS_GRP_KEY = "keychain-access-groups";
	private static final String INFO_PLIST_BUNDLE_ID_KEY = 	"CFBundleIdentifier";
	private static final String VO_APP_ID_VALUE = "com.att.VideoOptimizer"; 
	private static final String SIGNATURE_REPLACED_MSG = ": replacing existing signature";
	private static final String APP_INSTALL_COMPLETE_TXT = "Complete";
	private static final String APP_NEEDS_TRUST_TXT = "error: process launch failed: Security";
	private static final String[] FILES_TO_SIGN = {"libswiftRemoteMirror.dylib"};	
	private static final String APP_PATH = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + VO_APP_FILE;
	private static final String ZIP_PATH = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + VO_ZIP_FILE;
	private static final String ENTITLEMENTS_PLIST_PATH = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + ENTITLEMENTS_PLIST_FILENAME;
	
	private final IExternalProcessRunner extProcRunner = SpringContextUtil.getInstance().getContext().getBean(IExternalProcessRunner.class);
	private final IReadWriteFileExtractor fileExtractor = SpringContextUtil.getInstance().getContext().getBean(IReadWriteFileExtractor.class);
	private final IFileManager fileManager = SpringContextUtil.getInstance().getContext().getBean(IFileManager.class);
	
	private static AppSigningHelper INSTANCE;

	private ProvProfile provProfile;
	private String packageName = "com.att.vo.test";
	private boolean signed = false;

	private AppSigningHelper() {

	}
	
	public static synchronized AppSigningHelper getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new AppSigningHelper();
		}
		return INSTANCE;
	}
	
	public void extractAndSign(String devProvProfilePath, String certName) throws IOSAppException {
		if(signed) {
			return;
		}
		extractVoZip();
		unZipAndClean();
		provProfile = new ProvProfile(devProvProfilePath);
		if(isProvProfileExpired()) {
			throw new IOSAppException(ErrorCodeRegistry.getProvProfileileExpiredError());
		}
		removeCodeSignatureDir();	
		createAndUpdatePlists();	
		replaceProvProfile(devProvProfilePath);
		signFiles(certName, provProfile.getCodesignId(), FILES_TO_SIGN);
		removeEntitlementsPlist();
		signed = true;
	}

	public void deployAndLaunchApp() throws IOSAppException {
		String cmdOutput = extProcRunner.executeCmdRunner(IDEVICE_INSTALLER + " --install " + Util.wrapText(APP_PATH), true, "success");
		verifyAppDeployed(cmdOutput);
		extractPackageName();
		launchApp();
	}
	
	private void extractPackageName() throws IOSAppException {
		String command = "codesign -dv " + Util.wrapText(APP_PATH);
		String res = extProcRunner.executeCmd(command);
		StringReader sr = new StringReader(res);
		Properties p = new Properties();
		try {
			p.load(sr);
			String packageName = p.getProperty("Identifier");
			if(packageName == null) {
				throw new IOSAppException("Error recognizing provisioning profile : Failed to find package name");
			}
			this.packageName = packageName;
		} catch (IOException e) {
			throw new IOSAppException("Error recognizing provisioning profile : Failed to find package name");
		}
	}
	
	private void launchApp() {
		String command = IDEVICE_DEBUG + " run " + packageName;
		executeCmd(command);
	}
	
	public void relaunchApp() {
		String command = IDEVICE_DEBUG + " run " + packageName + " -state" + " stop_rec";
		executeCmd(command);
	}
	
	public void executeCmd(String cmd) {
		System.out.println(cmd);
		ProcessBuilder pbldr = new ProcessBuilder();
		if (!Util.isWindowsOS()) {
			pbldr.command(new String[] { "bash", "-c", cmd });
		} else {
			pbldr.command(new String[] { "CMD", "/C", cmd });
		}
		try {
			Process proc = pbldr.start();
			try {
				Thread.sleep(1000 * 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			proc.destroy();
		} catch (IOException e) {
			//Do nothing
		}
	}

	private void extractVoZip() throws IOSAppException {
		if (!fileExtractor.extractFiles(ZIP_PATH, VO_ZIP_FILE, AppSigningHelper.class.getClassLoader())) {
			throw new IOSAppException(ErrorCodeRegistry.getAppSavingError().getDescription());
		}
	}
	
	private void unZipAndClean() throws IOSAppException {
		executeCmd(Commands.unzipFile());
		if (!fileManager.fileExist(APP_PATH)) {
			throw new IOSAppException(ErrorCodeRegistry.getAppUnzipError());
		}
		executeCmd(Commands.removeFileOrDir(ZIP_PATH));
	}

	private boolean isProvProfileExpired() throws IOSAppException {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
		Instant dateTime = Instant.from(formatter.parse(provProfile.getExpiration()));
		return dateTime.compareTo(Instant.now()) < 0;
	}

	public String getPackageName() {
		return packageName;
	}

	private void removeEntitlementsPlist() {	
		/* Not verifying file removed here as we will
		 * check if this file exists when user launches 
		 * VO next time.
		 */
		extProcRunner.executeCmd(Commands.removeFileOrDir(ENTITLEMENTS_PLIST_PATH));
	}

  	// Sign app and all the dylib files
	private void signFiles(String certName, String id, String[] filesToSign) throws IOSAppException {
		String line = extProcRunner.executeCmd(Commands.signApp(certName, provProfile.getCodesignId()));				
		verifyFileSigned(line, VO_APP_FILE);
		line = extProcRunner.executeCmd(Commands.signFrameworkFiles(certName, provProfile.getCodesignId()));		
		String numOfFiles = extProcRunner.executeCmd(Commands.getNumOfFilesInDir(APP_PATH + Util.FILE_SEPARATOR + "Frameworks"));
		if (numOfFiles.matches("^[0-9]*$")) {
			int numFiles = Integer.valueOf(numOfFiles);
			int numFilesReplaced = StringUtils.countMatches(line, SIGNATURE_REPLACED_MSG);
			if (numFiles != numFilesReplaced) {
				throw new IOSAppException(ErrorCodeRegistry.getFileSigningError());
			}
		}
		
		for (String fileToSign:filesToSign) {
			line = extProcRunner.executeCmd(Commands.signFile(certName, provProfile.getCodesignId(), fileToSign));
			verifyFileSigned(line, fileToSign);
		}

		line = extProcRunner.executeCmd(Commands.signApp(certName, provProfile.getCodesignId()));
		verifyFileSigned(line, VO_APP_FILE);
	}

	private void replaceProvProfile(String devProvProfilePath) throws IOSAppException {
		try {
			Path filePath = Paths.get(APP_PATH + Util.FILE_SEPARATOR + PROVISIONING_PROFILE_NAME);
			FileTime ftBefore = Files.getLastModifiedTime(filePath);
			// format path to be used for command line
			devProvProfilePath = devProvProfilePath.replaceAll(" ", "\\\\ ");
			extProcRunner.executeCmd(Commands.copyProvProfile(devProvProfilePath));
			FileTime ftAfter = Files.getLastModifiedTime(filePath);	
			verifyFileUpdated(PROVISIONING_PROFILE_NAME, ftBefore, ftAfter);
		} catch (IOException e) {
			LOGGER.error("Error getting provisioning profile last modified time", e);
		}
	}

	private void createAndUpdatePlists() throws IOSAppException {
		// In case there is an old one, we need to remove it
		// or there will be problem parsing the file b/c
		// new content will be appended to the old file
		extProcRunner.executeCmd(Commands.removeFileOrDir(ENTITLEMENTS_PLIST_PATH));
		verifyEntitlementsPlistRemoved();
		extProcRunner.executeCmd(Commands.createEntitlementsPlist());            
		verifyEntitlementsPlistCreated();

		try {

			Path infoPlistPath = Paths.get(APP_PATH + Util.FILE_SEPARATOR + INFO_PLIST_FILENAME);
			FileTime infoFTBefore = Files.getLastModifiedTime(infoPlistPath);
			updatePlists();
			FileTime infoFTAfter = Files.getLastModifiedTime(infoPlistPath);
	
			verifyEntitlementsUpdated();
			verifyFileUpdated(INFO_PLIST_FILENAME, infoFTBefore, infoFTAfter);
	
		} catch (IOException e) {
			LOGGER.error("Error getting plist file last modified time", e);
		}
	}

	private void verifyEntitlementsUpdated() throws IOSAppException {
		/*
		 * Not able to use file modified time or file size to
		 * verify file got updated. Look for a string that 
		 * should have been replaced instead. 
		 * (entitlements.plist is a small file)
		 */
		try {
			List<String> lines = Files.readAllLines(Paths.get(ENTITLEMENTS_PLIST_PATH), StandardCharsets.UTF_8);
			for (String line:lines) {
				if (line.contains(VO_APP_ID_VALUE)) {
					throw new IOSAppException(ErrorCodeRegistry.getFileUpdateError(ENTITLEMENTS_PLIST_FILENAME));
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error verifying entitlements.plist was updated", e);
		}
	}

	private void verifyEntitlementsPlistRemoved() throws IOSAppException {		
		String line = extProcRunner.executeCmd(Commands.listDirectory(Util.getVideoOptimizerLibrary()));
		if (line.contains(ENTITLEMENTS_PLIST_FILENAME)) {
			throw new IOSAppException(ErrorCodeRegistry.getRemoveEntitlementsFileError());
		}
	}

	private void updatePlists() throws IOSAppException {
		updateEntitlementsPlist();
		updateInfoPlist();
	}
	
	private void updateEntitlementsPlist() throws IOSAppException {	
		extProcRunner.executeCmd(Commands.updatePlistEntry(ENTITLEMENTS_PLIST_PATH, 
				":" + PROV_FILE_APP_ID_KEY, provProfile.getAppId()));
		extProcRunner.executeCmd(Commands.updatePlistEntry(ENTITLEMENTS_PLIST_PATH, 
				":" + PROV_FILE_TEAM_ID_KEY, provProfile.getTeamId()));
		extProcRunner.executeCmd(Commands.updatePlistEntry(ENTITLEMENTS_PLIST_PATH, 
				":" + PROV_FILE_ACCESS_GRP_KEY + ":0 ", provProfile.getAppId()));
	}
	
	private void updateInfoPlist() throws IOSAppException {	
		extProcRunner.executeCmd(Commands.updatePlistEntry(APP_PATH + Util.FILE_SEPARATOR + INFO_PLIST_FILENAME, 
				":" + INFO_PLIST_BUNDLE_ID_KEY, provProfile.getCodesignId()));
	}
	
	private void removeCodeSignatureDir() throws IOSAppException {
		extProcRunner.executeCmd(Commands.removeFileOrDir(APP_PATH + Util.FILE_SEPARATOR + CODE_SIGNATURE_FOLDER_NAME));	
		verifyCodeSignatureFolderRemoved();
	}

	private void verifyFileSigned(String cmdOutput, String filename) throws IOSAppException {
		String replacedMsg = filename + SIGNATURE_REPLACED_MSG;
		if (!cmdOutput.contains(replacedMsg)) {
			throw new IOSAppException(ErrorCodeRegistry.getFileSigningError());
		}
	}
	
	private void verifyFileUpdated(String filename, FileTime before, FileTime after) 
			throws IOSAppException {
		if (after.toMillis() <= before.toMillis()) {
			throw new IOSAppException(ErrorCodeRegistry.getFileUpdateError(filename));
		}
	}

	private void verifyCodeSignatureFolderRemoved() throws IOSAppException {
		String line = extProcRunner.executeCmd(Commands.listDirectory(APP_PATH));
		if (line.contains(CODE_SIGNATURE_FOLDER_NAME)) {
			throw new IOSAppException(ErrorCodeRegistry.getRemoveCodeSignatureError());
		}
	}
	
	private void verifyEntitlementsPlistCreated() throws IOSAppException {
		String line = extProcRunner.executeCmd(Commands.listDirectory(Util.getVideoOptimizerLibrary()));
		if (!line.contains(ENTITLEMENTS_PLIST_FILENAME)) {
			throw new IOSAppException(ErrorCodeRegistry.getCreateEntitlementsFileError());
		}
	}

	public String executeProcessExtractionCmd(String processList, String iosDeployPath) {
		TreeMap<Date, String> pidList = new TreeMap<>();
		if (processList != null) {
			String[] lineArr = processList.split(Util.LINE_SEPARATOR);
			SimpleDateFormat formatter = new SimpleDateFormat("hh:mma");
			for (String str : lineArr) {
				String[] strArr = str.split(" +");
				try {
					if (str.contains(iosDeployPath) && strArr.length >= 8) {
						Date timestamp = formatter.parse(strArr[8]);
						pidList.put(timestamp, strArr[1]);
					}

				} catch (ParseException e) {
					LOGGER.error("Exception during pid extraction");
				}
			}
		}

		return pidList.lastEntry().getValue();
	}
	
	public int getIosVersion(){
		String cmd = "instruments -w device";
		String deviceList = extProcRunner.executeCmd(cmd);
		String[] devicesArray = deviceList.split("\n");
		int iosVersion = -1;
		for(String device: devicesArray){
			if((!device.contains("Simulator")) && device.contains("iPhone")){
				try{
					String versionStr = device.substring(device.indexOf("(")+1, device.indexOf(")"));
					if(versionStr.indexOf(".") != -1)
						iosVersion = Integer.valueOf(versionStr.substring(0, versionStr.indexOf(".")));
				}catch(NumberFormatException e){
					LOGGER.error("Non numeric value cannot represent ios version: " + iosVersion);
				}
				break;
			}
		}
		return iosVersion;
	}
	
	private void verifyAppDeployed(String cmdOutput) throws IOSAppException {
		System.out.println(cmdOutput);
		if (!cmdOutput.contains(APP_INSTALL_COMPLETE_TXT)) {
			throw new IOSAppException(ErrorCodeRegistry.getAppDeploymentError());
		}
		if (cmdOutput.contains(APP_NEEDS_TRUST_TXT)) {
			throw new IOSAppException(ErrorCodeRegistry.getAppTrustError());
		}
	}
	
	private final static class Commands {
		
		private Commands() {};
		
		static String unzipFile() {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("unzip " + Util.wrapText(Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + VO_ZIP_FILE));
			strBuilder.append(" -d ");
			strBuilder.append(Util.wrapText(Util.getVideoOptimizerLibrary()));
			return strBuilder.toString();
		}
		
		static String removeFileOrDir(String path) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("rm -rf ");		
			strBuilder.append(Util.wrapText(path));
			return strBuilder.toString();
		}
		
		static String createEntitlementsPlist() {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("codesign -d --entitlements :");		
			strBuilder.append(Util.wrapText(ENTITLEMENTS_PLIST_PATH));
			strBuilder.append(" ");
			strBuilder.append(Util.wrapText(APP_PATH));
			return strBuilder.toString();
		}		
		
		static String copyProvProfile(String devProvProfilePath) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("cp ");
			strBuilder.append(Util.wrapText(devProvProfilePath));
			strBuilder.append(" ");
			strBuilder.append(Util.wrapText(APP_PATH + Util.FILE_SEPARATOR + PROVISIONING_PROFILE_NAME));
			return strBuilder.toString();
		}
			
		static String signApp(String certName, String id) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("codesign -f -s \"");
			strBuilder.append(certName);
			strBuilder.append("\" -i ");
			strBuilder.append(id);
			strBuilder.append(" --entitlements ");
			strBuilder.append(Util.wrapText(ENTITLEMENTS_PLIST_PATH));
			strBuilder.append(" ");
			strBuilder.append(Util.wrapText(APP_PATH));
			return strBuilder.toString();
		}
		
		static String signFrameworkFiles(String certName, String id) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("codesign -f -s \"");
			strBuilder.append(certName);
			strBuilder.append("\" -i ");
			strBuilder.append(id);
			strBuilder.append(" --entitlements ");
			strBuilder.append(Util.wrapText(ENTITLEMENTS_PLIST_PATH));
			strBuilder.append(" ");
			strBuilder.append(Util.wrapText(APP_PATH + Util.FILE_SEPARATOR + "Frameworks/*"));
			return strBuilder.toString();
		}
		
		static String signFile(String certName, String id, String filename) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("codesign -f -s \"");
			strBuilder.append(certName);
			strBuilder.append("\" -i ");
			strBuilder.append(id);
			strBuilder.append(" --entitlements ");
			strBuilder.append(Util.wrapText(ENTITLEMENTS_PLIST_PATH));
			strBuilder.append(" ");
			strBuilder.append(Util.wrapText(APP_PATH + Util.FILE_SEPARATOR + filename));
			return strBuilder.toString();
		}

		static String listDirectory(String path) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("ls ");
			strBuilder.append(Util.wrapText(path));
			return strBuilder.toString();
		}	
		
		static String getNumOfFilesInDir(String dirPath) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("ls -1 ");
			strBuilder.append(Util.wrapText(dirPath));
			strBuilder.append(" | wc -l");
			return strBuilder.toString();
		}		
		
		static String updatePlistEntry(String filePath, String entry, String value) {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("/usr/libexec/PlistBuddy -c \"Set ");
			strBuilder.append(entry);
			strBuilder.append(" ");
			strBuilder.append(value);
			strBuilder.append("\" ");
			strBuilder.append(Util.wrapText(filePath));
			return strBuilder.toString();
		}
	}

	public static boolean isCertInfoPresent() {
		String provProfile = SettingsImpl.getInstance().getAttribute("iosProv");
		String certName = SettingsImpl.getInstance().getAttribute("iosCert");
		return StringUtils.isNotBlank(provProfile) && StringUtils.isNotBlank(certName);
	}

}