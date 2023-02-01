package com.att.aro.core.util;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;

public class VLCConfirmationImpl {

	@Autowired
	private IExternalProcessRunner extProcessRunner;

	private String result;

	public boolean checkVlc() {
		if (Util.isMacOS()) {
			result = extProcessRunner.executeCmd(Util.getVlc() + "/Contents/MacOS/VLC --version");
			return result.contains("VLC version");
		} else if (Util.isWindowsOS()) {
			return new File(Util.getVlc()).exists();
		} else {
			result = extProcessRunner.executeCmd("vlc --version");
			return result.contains("VLC version");
		}
	}
}
