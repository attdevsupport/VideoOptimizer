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
package com.att.aro.console;

import com.beust.jcommander.Parameter;

public class Commands {
	@Parameter(names = {"--help","-h","-?"}, description="show help menu", help=true)
	private boolean help = false;
	
	@Parameter(names= "--startcollector", description="start collector on device or emulator")
	private String startcollector = null;
	
	@Parameter(names= "--ask", description="start collector on device or emulator")
	private String ask = null;
	
	@Parameter(names = "--analyze", description="analyze trace file or folder")
	private String analyze = null;
	
	@Parameter(names="--output", description="provide output location of report")
	private String output = null;	
	
	@Parameter(names="--overwrite", description="overwrite output")
	private String overwrite = "no";
	
	@Parameter(names="--format", description="format of report: json or html")
	private String format = "json";
	
	@Parameter(names="--deviceid", description="device id or serial number for device to run collector on")
	private String deviceid = null;
	
	@Parameter(names="--video", description="yes or no - record video while capturing trace")
	private String video = "no";
	
	@Parameter(names="--sudo", description="admin password, OSX only")
	private String sudo = "";
	
	@Parameter(names="--listcollectors", description="list available data collector")
	private boolean listcollectors = false;

	@Parameter(names="--listdevices", description="list available devices")
	private boolean listdevices = false;

	@Parameter(names="--verbose", description="verbose output - more than just the important messages")
	private boolean verbose = false;
	
	@Parameter(names="--secure", description="enable secure collector")
	private boolean secure = false;
	
	@Parameter(names="--certInstall", description="install certificate if secure is enabled")
	private boolean certInstall = false;
	
	@Parameter(names="--throttleUL", description="enable throttle upload throughtput (64k - 100m (102400k))")
	private String throttleUL = "-1";	
	
	@Parameter(names="--throttleDL", description="enable throttle download throughtput (64k - 100m (102400k))")
	private String throttleDL = "-1";
	
	@Parameter(names="--profile", description="provide profile location")
	private String attenuationprofile = null;	
 	
	
	public boolean isListcollector() {
		return listcollectors;
	}

	public void setListcollector(boolean listcollectors) {
		this.listcollectors = listcollectors;
	}

	public boolean isHelp() {
		return help;
	}

	public void setHelp(boolean help) {
		this.help = help;
	}

	public String getStartcollector() {
		return startcollector;
	}

	public void setStartcollector(String startcollector) {
		this.startcollector = startcollector;
	}
	
	public String getSudo() {
		return sudo;
	}

	public void setSudo(String sudo) {
		this.sudo = sudo;
	}

	public String getAsk() {
		return ask;
	}

	public void setAsk(String ask) {
		this.ask = ask;
	}

	public String getAnalyze() {
		return analyze;
	}

	public void setAnalyze(String analyze) {
		this.analyze = analyze;
	}

	public String getOverwrite() {
		return this.overwrite;
	}

	public String getOutput() {
		return output;
	}
	
	public void setOverwrite(String overwrite) {
		this.overwrite = overwrite;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getDeviceid() {
		return deviceid;
	}

	public void setDeviceid(String deviceid) {
		this.deviceid = deviceid;
	}

	public String getVideo() {
		return video;
	}

	public void setVideo(String video) {
		this.video = video;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public boolean isListDevices() {
		return listdevices;
	}
	
	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}
	
	public boolean isCertInstall() {
		return certInstall;
	}

	public void setCertInstall(boolean certInstall) {
		this.certInstall = certInstall;
	}

	public String getThrottleUL() {
		return throttleUL;
	}

	public void setThrottleUL(String throttleUL) {
		this.throttleUL = throttleUL;
	}

	public String getThrottleDL() {
		return throttleDL;
	}

	public void setDownlink(String throttleDL) {
		this.throttleDL = throttleDL;
	}
	
	public String getAttenuationprofile() {
		return attenuationprofile;
	}

	public void setAttenuationprofile(String attenuationprofile) {
		this.attenuationprofile = attenuationprofile;
	}



	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("Commands: ");

		if (analyze != null) {
			sb.append(", analyze:" + getAnalyze());
		}
		if (deviceid != null) {
			sb.append(", deviceid:" + getDeviceid());
		}
		if (format != null) {
			sb.append(", format:" + getFormat());
		}
		if (overwrite != null) {
			sb.append(", overwrite:" + getOverwrite());
		}
		if (output != null) {
			sb.append(", output:" + getOutput());
		}		
		if (ask != null) {
			sb.append(", ask:" + getAsk());
		}
		if (startcollector != null) {
			sb.append(", collector:" + getStartcollector());
		}
		if (!sudo.isEmpty()) {
			sb.append(", sudo:" + getSudo());
		}
		if (help) {
			sb.append(", help");
		}
		if (listcollectors) {
			sb.append(", listcollectors");
		}
		if (listdevices) {
			sb.append(", listdevices");
		}
		if (verbose) {
			sb.append(", verbose");
		}
		if (secure) {
			sb.append(", secure");
		}
		if (certInstall) {
			sb.append(", certInstall");
		}
		if (attenuationprofile != null) {
			sb.append(", attenuationprofile:" + getAttenuationprofile());
		}
		if(!throttleDL.isEmpty()) {
			sb.append(", throttleUL: " + getThrottleUL());
		}
		if(!throttleUL.isEmpty()) {
			sb.append(", throttleDL: " + getThrottleDL());
		}
		if (video != null) {
			sb.append(", " + getVideo());
		}
		

		return sb.toString();
	}

}
