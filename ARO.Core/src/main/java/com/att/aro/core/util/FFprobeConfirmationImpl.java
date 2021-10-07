package com.att.aro.core.util;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.videoanalysis.impl.VideoPrefsController;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

public class FFprobeConfirmationImpl {
	@Autowired
	private VideoUsagePrefs videoUsagePrefs;

	@Autowired
	private IExternalProcessRunner extProcessRunner;

	@Autowired
	private VideoPrefsController videoPrefsController;

	private String result;

	public boolean checkFFprobeExistance() {
		if (!ffprobeDontShowAgainStatus()) {
			String cmd = Util.getFFPROBE() + " -version";
			result = extProcessRunner.executeCmd(cmd);
			String[] lines = result.split("\\n");
			if (lines != null && lines.length != 0) {
				if (!lines[0].contains("ffprobe version")) {
					// ffprobe not installed
					return false;
				}
			} else {
				// ffprobe is not installed
				return false;
			}
		}
		return true;
	}

	public String getResult() {
		return result;
	}

	public boolean ffprobeDontShowAgainStatus() {
		videoUsagePrefs = videoPrefsController.loadPrefs();
		if (videoUsagePrefs != null) {
			return videoUsagePrefs.isFfprobeConfirmationShowAgain();
		}
		return false;
	}

	public boolean saveFfprobeDontShowAgainStatus(boolean status) {
		videoUsagePrefs = videoPrefsController.loadPrefs();
		videoUsagePrefs.setFfprobeConfirmationShowAgain(status);
		return videoPrefsController.save();
	}

}
