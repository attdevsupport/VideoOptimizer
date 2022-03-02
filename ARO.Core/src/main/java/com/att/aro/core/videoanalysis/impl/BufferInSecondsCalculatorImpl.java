/*
 *  Copyright 2017 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.att.aro.core.videoanalysis.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.pojo.BufferTimeBPResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.videoanalysis.AbstractBufferOccupancyCalculator;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BufferInSecondsCalculatorImpl extends AbstractBufferOccupancyCalculator {

	protected static final Logger LOG = LogManager.getLogger(BufferInSecondsCalculatorImpl.class.getName());

	double beginBuffer, endBuffer;

	Double dlTime = 0D;
	Double dlDuration = 0D;

	enum SortOrder {
		LAST, FIRST, HIGHEST
	}
	
	/**
	 * <pre>
	 * A mapped point set for graphics display key=(x-coord)timeStamp, (y-coord)duration Each key-value describes a point in the graph. A line will be drawn from
	 * {key} point to the {key+1} point ex: 0=61.287999868392944, 0.0 , 1=61.287999868392944, 2.75275 , 2=62.986000061035156, 2.75275 , 3=62.986000061035156,
	 * 6.089416666666667
	 */
	Map<Integer, String> seriesDataSets = new TreeMap<Integer, String>();

	int key;
	int skey = 0;
	int debugkey = 0;

	private List<VideoStall> videoStallResult;
	private BufferTimeBPResult bufferTimeResult;

	@Autowired
	private VideoChunkPlotterImpl videoChunkPlotterImpl;

	double possibleStartPlayTime;

	String dataSetValues = "";
	
	@Override
	public double drawVeDone(List<VideoEvent> veDone, double beginByte) {
		// purposely removed
		return 0;
	}

	@Override
	public double bufferDrain(double buffer) {
		// purposely removed
		return 0;
	}

	public Map<Long, Double> getSegmentStartTimeMap() {
		return videoChunkPlotterImpl.getSegmentStartTimeMap();
	}

	public Map<Double, Long> getSegmentEndTimeMap() {
		Map<Long, Double> segmentStartTimeMap = getSegmentStartTimeMap();
		Map<Double, Long> segmentEndTimeMap = new HashMap<>();
		if (segmentStartTimeMap != null) {
			for (VideoEvent ve : streamingVideoData.getStreamingVideoCompiled().getFilteredSegments()) {
				if (ve != null) {
					Double startTime = segmentStartTimeMap.get(new Double(ve.getSegmentID()).longValue());
					double segmentPlayEndTime = (startTime != null ? startTime : 0.0) + ve.getDuration();
					segmentEndTimeMap.put(segmentPlayEndTime, new Double(ve.getSegmentID()).longValue());
				}
			}
		}
		return segmentEndTimeMap;
	}

	public BufferTimeBPResult updateBufferTimeResult(List<Double> bufferTimeBPResult) {
		bufferTimeResult = new BufferTimeBPResult(bufferTimeBPResult);
		return bufferTimeResult;
	}
}