package com.att.aro.core.util;

import java.io.IOException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.packetanalysis.IVideoUsageAnalysis;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

public class FFmpegConfirmationImpl {

	private VideoUsagePrefs videoUsagePrefs;

	@Autowired
	private IExternalProcessRunner extProcessRunner;

	@Autowired
	private IVideoUsageAnalysis videoUsage;

	private String result;

	public boolean checkFFmpegExistance() {
		if (!ffmpegDontShowAgainStatus()) {
			String cmd = Util.getFFMPEG() + " -version";
			result = extProcessRunner.executeCmd(cmd);
			String[] lines = result.split("\\n");
			// Check & launch dialog if dont show again is false
			if (lines.length != 0 && lines != null) {
				if (!lines[0].contains("ffmpeg version")) { // ffmpeg not
															// installed
					return false;
				}
			} else { // ffmpeg not installed
				return false;
			}
		}
		return true;
	}

	public String getResult() {
		return result;
	}

	public boolean ffmpegDontShowAgainStatus() {
		videoUsage.loadPrefs();
		videoUsagePrefs = videoUsage.getVideoUsagePrefs();
		if (videoUsagePrefs != null) {
			return videoUsagePrefs.isFfmpegConfirmationShowAgain();
		}
		return false;
	}

	public boolean saveFfmpegDontShowAgainStatus(boolean status) {

		videoUsagePrefs.setFfmpegConfirmationShowAgain(status);

		ObjectMapper mapper = new ObjectMapper();
		String temp;

		try {
			temp = mapper.writeValueAsString(videoUsagePrefs);
		} catch (IOException e) {
			return false;
		}
		videoUsage.getPrefs().setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
		return true;
	}

}
