package com.att.aro.core.util;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.videoanalysis.impl.VideoPrefsController;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

public class FFmpegConfirmationImpl {
	@Autowired
	private VideoUsagePrefs videoUsagePrefs;

	@Autowired
	private IExternalProcessRunner extProcessRunner;

	@Autowired
	private VideoPrefsController videoPrefsController;

	private String result;

	public boolean checkFFmpegExistance() {
		if (!ffmpegDontShowAgainStatus()) {
			String cmd = Util.getFFMPEG() + " -version";
			result = extProcessRunner.executeCmd(cmd);
			String[] lines = result.split("\\n");
			if (lines != null && lines.length != 0) {
				if (!lines[0].contains("ffmpeg version")) {
					// ffmpeg not installed
					return false;
				}
			} else {
				// ffmpeg is not installed
				return false;
			}
		}
		return true;
	}

	public String getResult() {
		return result;
	}

	public boolean ffmpegDontShowAgainStatus() {
		videoUsagePrefs = videoPrefsController.loadPrefs();
		if (videoUsagePrefs != null) {
			return videoUsagePrefs.isFfmpegConfirmationShowAgain();
		}
		return false;
	}

	public boolean saveFfmpegDontShowAgainStatus(boolean status) {
		videoUsagePrefs = videoPrefsController.loadPrefs();
		videoUsagePrefs.setFfmpegConfirmationShowAgain(status);
		return videoPrefsController.save();
	}

}
