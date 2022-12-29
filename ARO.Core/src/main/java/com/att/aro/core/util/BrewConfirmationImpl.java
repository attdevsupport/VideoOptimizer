package com.att.aro.core.util;

import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.settings.impl.SettingsImpl;

public class BrewConfirmationImpl {

	private static final Logger LOG = LogManager.getLogger(BrewConfirmationImpl.class);
	private static final String BREW_VERSION_CHECK = "brew_version_check";

	@Autowired
	private IExternalProcessRunner extProcessRunner;

	public boolean isBrewUpToDate(String localBrewVersion, String targetBrewVersion) {
		boolean flag = false;

		VersionComparator local = new VersionComparator();

		if (local.compare(localBrewVersion, targetBrewVersion) < 0) {
			LOG.error("Please update the version because the latest is " + localBrewVersion + "target: "
					+ targetBrewVersion);
		} else {
			LOG.debug("your is up to date:  " + localBrewVersion + "target: " + targetBrewVersion);
			flag = true;
		}
		return flag;
	}

	public String getLocalBrewVersion() {
		String brewReturn = extProcessRunner.executeCmdRunner("brew --version | sed 1q | tr -d 'Homebrew '", true,
				"success", true, true);
		if (!brewReturn.isEmpty()) {
			try {
				if (brewReturn.contains("-")) {
					brewReturn = brewReturn.substring(0, brewReturn.indexOf("-"));
				}
			} catch (NumberFormatException e) {
				LOG.error("Non numeric value cannot represent ios version: " + brewReturn);
			}
		}
		return brewReturn;

	}

	public void saveLastBrewVersion() {
		String recentBrewVersion = getLastBrewVersion();
		if (StringUtils.isEmpty(recentBrewVersion)) {
			SettingsImpl.getInstance().setAndSaveAttribute(BREW_VERSION_CHECK, getSuggestBrewVersion());
		} else {
			if (!isBrewUpToDate(recentBrewVersion, getSuggestBrewVersion())) {
				SettingsImpl.getInstance().setAndSaveAttribute(BREW_VERSION_CHECK, getSuggestBrewVersion());
			}
		}
	}

	public String getLastBrewVersion() {
		return SettingsImpl.getInstance().getAttribute(BREW_VERSION_CHECK);
	}

	public String getSuggestBrewVersion() {
		return ResourceBundle.getBundle("build").getString("homebrew.version");
	}

	public boolean checkBrewVersion() {
		boolean isBrewUptoDate;

		String localBrew = getLocalBrewVersion();
		String suggestBrew = getSuggestBrewVersion();

		isBrewUptoDate = isBrewUpToDate(localBrew, suggestBrew);
		LOG.debug("brew status: " + isBrewUptoDate);

		return isBrewUptoDate;
	}

}
