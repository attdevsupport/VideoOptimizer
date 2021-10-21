package com.att.aro.core.video.pojo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import com.att.aro.core.videoanalysis.pojo.MediaType;

@Data
public class Track {

	private int trackNumber;
	private String trackName;
	private String manifest;
	private MediaType mediaType;
	private float mediaBandwidth;
	private int maxSegmentIndex = 0;
	@Setter(AccessLevel.NONE)
	private List<Integer> segmentSizes;
	@Setter(AccessLevel.NONE)
	private List<Double> segmentDurations;
	
	public void setSegmentSizes(List<Integer> segmentSizes) {
		this.maxSegmentIndex = segmentSizes.get(0);
		segmentSizes.remove(0);
		this.segmentSizes = segmentSizes;
	}
	
	public void setSegmentDurations(List<Double> segmentDurations) {
		this.segmentDurations = new ArrayList<>();
		segmentDurations.remove(0);
		this.segmentDurations = segmentDurations;
	}
}