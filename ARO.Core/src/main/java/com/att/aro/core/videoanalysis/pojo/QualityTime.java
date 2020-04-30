package com.att.aro.core.videoanalysis.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QualityTime{	
	/**
	 * total number of member variables
	 */
	public static final int COLUMN_COUNT = 3;

	private String name = "";
	private int track;
	private double duration;
	private double percentage;
}