package com.att.aro.core.videoanalysis.videoframe;

import lombok.Data;

@Data
public class FrameStatus {
	private boolean success = false;
	private int firstFrame = -1;
	private int frameCount;
	private int addedCount;
	private int duplicates;
	private String executionResults;
	FrameRequest frameRequest;
}
