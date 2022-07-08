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
