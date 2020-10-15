package com.att.aro.core.videoanalysis.pojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Table item information for comparing network throughput and segment information 
 * during downloading the video
 */
@Data
@AllArgsConstructor
public class SegmentComparison {

	private String name = "";
	private int count;
	private int track;
	private double declaredBandwidth;
	private List<Double> calculatedThroughputList;

	public double getCalculatedThroughput() {
		Double sum = 0.0;
		if (!calculatedThroughputList.isEmpty()) {
			for (Double throughtput : calculatedThroughputList) {
				sum += throughtput;
			}
			double size = calculatedThroughputList.size();
			return sum.doubleValue() / size / 1000.0;
		}
		return sum;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder();
		strblr.append("QualityTime:").append(name).append("\n track: ").append(track).append("\n bitrateDeclared:")
				.append(String.format("%.3f", declaredBandwidth)).append("\n calculate network bitrate:")
				.append(String.format("%.3f", getCalculatedThroughput()));
		return strblr.toString();

	}
}
