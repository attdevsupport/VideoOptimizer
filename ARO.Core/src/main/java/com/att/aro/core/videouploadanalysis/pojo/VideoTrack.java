package com.att.aro.core.videouploadanalysis.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VideoTrack {
	private String type;
	private String trk;
	private String utc;

	@JsonProperty("Type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@JsonProperty("Trk")
	public String getTrk() {
		return trk;
	}

	public void setTrk(String trk) {
		this.trk = trk;
	}

	@JsonProperty("UTC")
	public String getUtc() {
		return utc;
	}

	public void setUtc(String utc) {
		this.utc = utc;
	}

	public String toString() {
		return "Type: " + getType() + ", Trk: " + getTrk() + ", UTC: " + getUtc();
	}
}
