package com.att.aro.core.util;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;

public class PcapConfirmationImpl {

	@Autowired
	private IExternalProcessRunner extProcessRunner;

	@Autowired
	private IStringParse stringParse;

	private String result = "";

	public boolean checkPcapVersion() {

		String cmd = "tcpdump --version";
		result = extProcessRunner.executeCmd(cmd);

		stringParse.parse(result, "version");
		String version = StringParse.findLabeledDataFromString("libpcap version", " ", result);
		if (version == null || version.isEmpty()) {
			return false;
		}
		String[] least = { "1", "8", "0" };
		if (Util.isMacOS()) {
			String[] points = version.split("\\.");
			for (int idx = 0; idx < least.length; idx++) {
				if (idx == points.length) {
					break;
				}
				if (points[idx].compareTo(least[idx]) < 0) {
					return false;
				}
			}
		}
		return true;
	}

	public String getResult() {
		return result;
	}

}
