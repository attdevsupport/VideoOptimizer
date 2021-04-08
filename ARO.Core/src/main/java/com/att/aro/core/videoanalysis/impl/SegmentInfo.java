package com.att.aro.core.videoanalysis.impl;

import com.att.aro.core.videoanalysis.pojo.Manifest;

import lombok.Data;

@Data
public class SegmentInfo {
	private int segmentID;
	private Manifest.ContentType contentType = Manifest.ContentType.UNKNOWN;
	private double startTime;
	private double duration;
	private boolean video;
	private String quality = "";
	private int size;
	private double bitrate;
	private int resolutionHeight = 0;
	private boolean thumbnailExtracted;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(" SegmentInfo :");
		strblr.append(" segmentID :").append(segmentID);
		strblr.append(", contentType :").append(contentType);
		strblr.append(String.format(", startTime   :%.6f", startTime));
		strblr.append(", duration :").append(duration);
		strblr.append(", video :").append(video);
		strblr.append(", quality :").append(quality);
		strblr.append(", size :").append(size);
		strblr.append(", bitrate :").append(bitrate);
		return strblr.toString();
	}	
}
