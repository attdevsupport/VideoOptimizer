package com.att.aro.core.videoanalysis.videoframe;

import lombok.Data;

@Data
public class FrameRequest {
	
	public FrameRequest(JobType jobType, double startTimeStamp, int count, Double targetFrame, FrameReceiver frameReceiver) {
		this.jobType = jobType;
		this.startTimeStamp = startTimeStamp;
		this.count = count;
		this.targetFrame = targetFrame;
		this.frameReceiver = frameReceiver;
		this.tryCount = 0;
	}

	public enum JobType {
		PRELOAD,
		COLLECT_FRAMES,
		DROP
	}
	
	private int tryCount;
	private double startTimeStamp;
	private Double targetFrame;
	private JobType jobType;
	private Integer count;
	private FrameReceiver frameReceiver;
}
