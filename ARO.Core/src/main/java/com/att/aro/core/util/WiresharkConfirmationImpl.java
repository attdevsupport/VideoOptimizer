package com.att.aro.core.util;

import java.io.File;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.settings.impl.SettingsImpl;

public class WiresharkConfirmationImpl {

	@Autowired
	private IExternalProcessRunner extProcessRunner;
	private static final Logger LOGGER = LogManager.getLogger(WiresharkConfirmationImpl.class);

	private String wiresharkWinDefaultPath = "C:\\Program Files\\Wireshark\\Wireshark.exe";
	private String result;

	public boolean checkWireshark() {
		boolean isPresent = false;
		if (Util.isMacOS()) {
			result = extProcessRunner.executeCmd(Util.getWireshark() + "/Contents/MacOS/Wireshark --version");
			isPresent = result.contains("Wireshark") && result.contains("Copyright");
		} else if (Util.isWindowsOS()) {
			// Assuming wireshark path is set environmental variables
			result = extProcessRunner.executeCmd("Wireshark --version");
			isPresent = result.contains("Wireshark") && result.contains("Copyright");
			// check if wireshark path already set in the preference
			if (!isPresent && (new File(Util.getWireshark())).exists()) {
				result = extProcessRunner.executeCmd("\"" + Util.getWireshark() + "\"" + " --version");
				isPresent = result.contains("Wireshark") && result.contains("Copyright");
			} else if (!isPresent && (new File(wiresharkWinDefaultPath)).exists()) { // Checking if wireshark.exe file is in default location
				result = extProcessRunner.executeCmd("\"" + wiresharkWinDefaultPath + "\"" + " --version");
				isPresent = result.contains("Wireshark") && result.contains("Copyright");
				if (isPresent) {
					SettingsImpl.getInstance().setAndSaveAttribute("WIRESHARK_PATH", wiresharkWinDefaultPath);
				}
			}
		} else {
			result = extProcessRunner.executeCmd("wireshark --version");
			isPresent = result.contains("Wireshark") && result.contains("Copyright");
		}
		return isPresent;
	}
}
