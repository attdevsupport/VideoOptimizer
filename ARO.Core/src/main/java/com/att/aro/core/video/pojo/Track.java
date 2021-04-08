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
		if (manifest.equals("MP4")) {
			if (this.mediaType != null && this.mediaType == MediaType.VIDEO) {
				this.segmentSizes = new ArrayList<>();
				for (int i = 2; i <= segmentSizes.size(); i += 2) {
					this.segmentSizes.add(segmentSizes.get(i-2) + segmentSizes.get(i-1));
				}
			} else {
				this.segmentSizes = segmentSizes;
			}
		} else {
			this.segmentSizes = segmentSizes;
		}
	}
	
	public void setSegmentDurations(List<Double> segmentDurations) {
		this.segmentDurations = new ArrayList<>();
		segmentDurations.remove(0);
		if (manifest.equals("MP4")) {
			if (this.mediaType != null && this.mediaType == MediaType.VIDEO) {
				for (int i = 2; i <= segmentDurations.size(); i += 2) {
					this.segmentDurations.add(segmentDurations.get(i-2) + segmentDurations.get(i-1));
				}
			} else {
				this.segmentDurations = segmentDurations;
			}
		} else {
			this.segmentDurations = segmentDurations;
		}
	}
}