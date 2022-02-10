package com.att.aro.core.tracemetadata.pojo;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MetaStream {
	private String description = "";
	private String type = "";
	private String video = "";
	private String videoOrientation = "";
	private Double videoDuration = null;

	private HashMap<Integer, Integer> videoResolutionMap = new HashMap<>();
	private HashMap<Double, Integer> videoBitrateMap = new HashMap<>();
	private Double videoDownloadtime = null;
	private Integer videoSegmentTotal = null;

	private HashMap<String, Integer> audioChannelMap = new HashMap<>();
	private HashMap<Double, Integer> audioBitrateMap = new HashMap<>();
	private Double audioDownloadtime = null;
	private Integer audioSegmentTotal = null;
}
