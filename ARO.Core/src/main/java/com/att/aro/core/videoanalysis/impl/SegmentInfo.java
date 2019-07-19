package com.att.aro.core.videoanalysis.impl;

import lombok.Data;

@Data
public class SegmentInfo {
	int segmentID;
	boolean video;
	String quality = "";
	double duration;
	int size;
	double startTime;
	double bitrate;
}
