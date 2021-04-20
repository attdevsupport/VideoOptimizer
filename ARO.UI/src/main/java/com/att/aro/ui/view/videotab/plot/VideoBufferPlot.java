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
package com.att.aro.ui.view.videotab.plot;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.BufferInSecondsCalculatorImpl;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.diagnostictab.plot.IPlot;

import lombok.Data;

@Data
public class VideoBufferPlot implements IPlot {
	private static final Logger LOGGER = LogManager.getLogger(VideoBufferPlot.class);
	private VideoStream videoStream;
	private int graphCounter = 0;

	private List<VideoEvent> videoEventList = new ArrayList<VideoEvent>();
	private List<VideoEvent> eventList = new ArrayList<VideoEvent>();
	private List<VideoEvent> audioEventList = new ArrayList<VideoEvent>();
	private List<VideoEvent> progressEventList = new ArrayList<VideoEvent>();
	SortedMap<String, VideoEvent> videoEventMap = new TreeMap<String, VideoEvent>();
	SortedMap<VideoEvent, Double> downloadProgressMap = new TreeMap<VideoEvent, Double>();
	private XYSeries videoEventSeries = new XYSeries(SegmentOptions.VIDEO.toString());
	private double maxYValue;
	private double maxXValue;
	private double minYValue;
	private XYSeries bufferProgressSeries = new XYSeries("Buffer Time");
	private SegmentOptions optionSelected;
	private double minXValue;
	private boolean isMuxed = false;
	private List<Double> bufferProgressList = new ArrayList<Double>();
	private List<Double> timestampList = new ArrayList<Double>();
	private Map<Integer, String> seriesDataSets;
	private BufferInSecondsCalculatorImpl bufferInSecondsCalculatorImpl = (BufferInSecondsCalculatorImpl) ContextAware.getAROConfigContext()
			.getBean("bufferInSecondsCalculatorImpl", PlotHelperAbstract.class);
	private TreeMap<VideoEvent, Double> chunkPlayTimeList;

	public VideoBufferPlot(VideoStream videoStream, SegmentOptions optionSelected, Map<Integer, String> seriesDataSets,
			TreeMap<VideoEvent, Double> chunkPlayTimeList) {
		this.videoStream = videoStream;
		this.optionSelected = optionSelected;
		this.seriesDataSets = seriesDataSets;
		this.chunkPlayTimeList = chunkPlayTimeList;
	}

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if (analysis == null) {
			LOGGER.info("no trace data here");
		} else {
			XYItemRenderer videoRenderer = plot.getRenderer();
			videoRenderer.setBaseToolTipGenerator(new XYToolTipGenerator() {

				@Override
				public String generateToolTip(XYDataset dataset, int series, int item) {

					// Tooltip value
					Number timestamp = dataset.getX(series, item);
					Number bufferTime = dataset.getY(series, item);
					StringBuffer tooltipValue = new StringBuffer();

					Map<Double, Long> segmentEndTimeMap = bufferInSecondsCalculatorImpl.getSegmentEndTimeMap();
					Map<Long, Double> segmentStartTimeMap = bufferInSecondsCalculatorImpl.getSegmentStartTimeMap();
					List<VideoEvent> chunksBySegmentId = analysis.getAnalyzerResult().getStreamingVideoData().getStreamingVideoCompiled().getChunksBySegmentID();
					if (CollectionUtils.isEmpty(chunksBySegmentId)) {
						return null;
					}
					double firstSegmentNo = chunksBySegmentId.get(0).getSegmentID();

					DecimalFormat decimalFormat = new DecimalFormat("0.##");
					if (segmentStartTimeMap == null || segmentStartTimeMap.isEmpty()) {
						return "-,-,-";
					}

					List<Long> segmentList = new ArrayList<Long>(segmentEndTimeMap.values());
					Collections.sort(segmentList);
					Long lastSegmentNo = -1L;
					if (segmentList.size() != 0) {
						lastSegmentNo = segmentList.get(segmentList.size() - 1);
					}
					Long segmentNumber = 0L;
					boolean isSegmentPlaying = false;
					boolean startup = false;
					boolean endPlay = false;

					for (double segmentEndTime : segmentEndTimeMap.keySet()) {
						if (segmentEndTime > timestamp.doubleValue()) {
							segmentNumber = segmentEndTimeMap.get(segmentEndTime);
							if (segmentNumber == firstSegmentNo) {
								startup = true;
							}
							if (segmentStartTimeMap.get(segmentNumber) <= timestamp.doubleValue()) {
								tooltipValue.append(decimalFormat.format(segmentNumber) + ",");
								isSegmentPlaying = true;
								startup = false;
							}
						} else if (lastSegmentNo.equals(segmentEndTimeMap.get(segmentEndTime)) 
							   &&  segmentEndTime == timestamp.doubleValue()) {
							endPlay = true;
						}
					}

					if (endPlay || startup) {
						tooltipValue.append("-,");
					} else if (!isSegmentPlaying && !startup) {
						tooltipValue.append("Stall,");
					}

					tooltipValue.append(String.format("%.2f", bufferTime) + "," + String.format("%.2f", timestamp));

					String[] value = tooltipValue.toString().split(",");
					return (MessageFormat.format(ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip"), value[0], value[1], value[2]));
				}

			});
		}

		XYSeriesCollection collection = new XYSeriesCollection();

		if (bufferProgressSeries != null) {
			collection.addSeries(bufferProgressSeries);
		}

		plot.setDataset(collection);
	}

	public void calculateBufferProgress(AROTraceData aroTraceData) {

		bufferProgressSeries.clear();

		double xCoordinate, yCoordinate;
		String ptCoordinate[] = new String[2];

		if (!seriesDataSets.isEmpty()) {
			for (int key : seriesDataSets.keySet()) {
				ptCoordinate = seriesDataSets.get(key).trim().split(",");
				xCoordinate = Double.parseDouble(ptCoordinate[0]);
				yCoordinate = Double.parseDouble(ptCoordinate[1]);
				timestampList.add(xCoordinate);
				bufferProgressList.add(yCoordinate);
				bufferProgressSeries.add(xCoordinate, yCoordinate);
			}
		}

		Collections.sort(bufferProgressList);
		Collections.sort(timestampList);
		minYValue = bufferProgressList.stream().findFirst().get();
		maxYValue = bufferProgressList.stream().reduce((first, second) -> second).get();
		minXValue = timestampList.stream().findFirst().get();
		maxXValue = timestampList.stream().reduce((first, second) -> second).get();
	}
}