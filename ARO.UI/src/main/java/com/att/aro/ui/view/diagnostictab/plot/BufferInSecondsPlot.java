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
package com.att.aro.ui.view.diagnostictab.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.BufferTimeBPResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.BufferInSecondsCalculatorImpl;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

import lombok.Getter;

public class BufferInSecondsPlot implements IPlot {

	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip");

	XYSeriesCollection bufferFillDataCollection = new XYSeriesCollection();
	XYSeries seriesBufferFill;
	@Getter
	private Map<Integer, String> seriesDataSets;
	private Shape shape = new Rectangle2D.Double(0, 0, 50, 50);//new Ellipse2D.Double(0, 0, 50, 50);
	private List<Double> bufferTimeList = new ArrayList<>();

	BufferInSecondsCalculatorImpl bufferInSecondsCalculatorImpl = (BufferInSecondsCalculatorImpl) ContextAware.getAROConfigContext()
			.getBean("bufferInSecondsCalculatorImpl", PlotHelperAbstract.class);

	@Getter
	private TreeMap<VideoEvent, Double> chunkPlayTimeList;
	private static final Logger LOG = LogManager.getLogger(BufferInSecondsPlot.class.getName());

	public void setChunkPlayTimeList(Map<VideoEvent, Double> chunkPlayTime) {
		this.chunkPlayTimeList = (TreeMap<VideoEvent, Double>) chunkPlayTime;
	}

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if (analysis != null) {
			StreamingVideoData streamingVideoData = analysis.getAnalyzerResult().getStreamingVideoData();
			bufferFillDataCollection.removeAllSeries();
			seriesBufferFill = new XYSeries("Buffer Against Play Time");
			seriesDataSets = new TreeMap<>();

			// stalls loaded here
			seriesDataSets = bufferInSecondsCalculatorImpl.populate(streamingVideoData, chunkPlayTimeList);
			// updating video stall result in packetAnalyzerResult
			analysis.getAnalyzerResult().setVideoStalls(bufferInSecondsCalculatorImpl.getVideoStallResult());
			analysis.getAnalyzerResult().setNearStalls(bufferInSecondsCalculatorImpl.getVideoNearStallResult());

			bufferTimeList.clear();
			double xCoordinate, yCoordinate;
			String ptCoordinate[] = new String[2]; // to hold x & y values
			double videoPlayStartTime = 0;
			videoPlayStartTime = chunkPlayTimeList.get(chunkPlayTimeList.firstKey());

			List<VideoEvent> filteredSegments = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments();
			double lastArrivedSegmentTimeStamp = filteredSegments.get(filteredSegments.size() - 1).getEndTS();
			if (!seriesDataSets.isEmpty()) {
				for (int key : seriesDataSets.keySet()) {
					ptCoordinate = seriesDataSets.get(key).trim().split(",");
					xCoordinate = Double.parseDouble(ptCoordinate[0]);
					yCoordinate = Double.parseDouble(ptCoordinate[1]);
					if (xCoordinate >= videoPlayStartTime && xCoordinate <= lastArrivedSegmentTimeStamp) {
						bufferTimeList.add(yCoordinate);
					}
					seriesBufferFill.add(xCoordinate, yCoordinate);
				}
			}

			Collections.sort(bufferTimeList);
			BufferTimeBPResult bufferTimeResult = bufferInSecondsCalculatorImpl.updateBufferTimeResult(bufferTimeList);
			analysis.getAnalyzerResult().setBufferTimeResult(bufferTimeResult);
			// populate collection
			bufferFillDataCollection.addSeries(seriesBufferFill);

			XYItemRenderer renderer = new StandardXYItemRenderer();
			XYToolTipGenerator xyToolTipGenerator = toolTipGenerator(streamingVideoData);
			if (xyToolTipGenerator != null) {
				renderer.setBaseToolTipGenerator(xyToolTipGenerator);
			}
			renderer.setSeriesStroke(0, new BasicStroke(2.0f));
			renderer.setSeriesPaint(0, Color.MAGENTA);

			renderer.setSeriesShape(0, shape);

			plot.setRenderer(renderer);

		}
		plot.setDataset(bufferFillDataCollection);
	}

	public XYToolTipGenerator toolTipGenerator(StreamingVideoData streamingVideoData) {
		return new XYToolTipGenerator() {

			@Override
			public String generateToolTip(XYDataset dataset, int series, int item) {

				// Tooltip value
				Number timestamp = dataset.getX(series, item);
				Number bufferTime = dataset.getY(series, item);
				StringBuffer tooltipValue = new StringBuffer();

				Map<Double, Long> segmentEndTimeMap = bufferInSecondsCalculatorImpl.getSegmentEndTimeMap();
				Map<Long, Double> segmentStartTimeMap = bufferInSecondsCalculatorImpl.getSegmentStartTimeMap();
				List<VideoEvent> chunksBySegmentId = streamingVideoData.getStreamingVideoCompiled().getChunksBySegmentID();
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
				return (MessageFormat.format(BUFFER_TIME_OCCUPANCY_TOOLTIP, value[0], value[1], value[2]));
			}
		};
	}

	public void clearPlot(XYPlot bufferTimePlot) {
		bufferTimePlot.setDataset(null);
	}

	public VideoEvent isDataItemStallPoint(double xDataValue, double yDataValue) {
		VideoEvent segmentToPlay = null;
		List<VideoStall> videoStallResults = bufferInSecondsCalculatorImpl.getVideoStallResult();
		if (seriesDataSets == null || seriesDataSets.isEmpty()) {
			LOG.error("ERROR: Checking data item/segment for a stall point: dataset is null or empty.");
			return segmentToPlay;
		}
		String[] dataSetArray = seriesDataSets.get(seriesDataSets.size() - 1).split(",");
		if (dataSetArray.length > 1 && dataSetArray[1] != null) {
			if (videoStallResults != null && (!videoStallResults.isEmpty())) {
				VideoStall stallPoint = videoStallResults.get(videoStallResults.size() - 1);
				double lastDataSet_YValue = Double.parseDouble(dataSetArray[1]);
				if ((stallPoint.getStallEndTimestamp() == 0 || stallPoint.getStallEndTimestamp() == stallPoint.getStallEndTimestamp())
						&& stallPoint.getStallEndTimestamp() == xDataValue 
						&& lastDataSet_YValue == yDataValue) {
					segmentToPlay = stallPoint.getSegmentTryingToPlay();
				}
			}
		} else {
			LOG.error("ERROR: Checking data item/segment for a stall point: dataset y-value is null");
		}
		return segmentToPlay;
	}

}
