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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.att.aro.core.videoanalysis.XYPair;
import com.att.aro.core.videoanalysis.impl.BufferInSecondsCalculatorImpl;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoStream.StreamStatus;
import com.att.aro.core.videoanalysis.pojo.VideoStream.ToolTipDetail;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

import lombok.Getter;
import lombok.Setter;

public class BufferInSecondsPlot implements IPlot {

	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP_LOAD = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip.load");
	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP_PLAY = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip.play");
	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP_STALL = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip.stall");

	private static final Logger LOG = LogManager.getLogger(BufferInSecondsPlot.class.getName());
	
	XYSeriesCollection seriesPlayTimeBufferCollection = new XYSeriesCollection();
	XYSeries seriesPlayTimeBuffer;
	 
	@Getter
	private Map<Integer, String> seriesDataSets;
	private List<Double> bufferTimeList = new ArrayList<>();

	BufferInSecondsCalculatorImpl bufferInSecondsCalculatorImpl = 
			(BufferInSecondsCalculatorImpl) ContextAware.getAROConfigContext()
			.getBean("bufferInSecondsCalculatorImpl", PlotHelperAbstract.class);
	
	private VideoStream videoStream;

	@Getter
	@Setter
	private TreeMap<VideoEvent, Double> chunkPlayTimeMap;

	@Override
	public void populate(XYPlot plot, AROTraceData aroTraceData) {
		seriesPlayTimeBufferCollection.removeAllSeries();
		
		if (aroTraceData != null && aroTraceData.getAnalyzerResult().getStreamingVideoData() != null) {

			bufferInSecondsCalculatorImpl.setStreamingVideoData(aroTraceData.getAnalyzerResult().getStreamingVideoData());
			
			for (VideoStream videoStream : aroTraceData.getAnalyzerResult().getStreamingVideoData().getVideoStreams()) {
				if (videoStream.isSelected()) {
					this.videoStream = videoStream;
					LOG.debug("VideoStream :" + videoStream.getManifest().getVideoName());
					seriesPlayTimeBuffer = new XYSeries("Play Time Buffer");
					double yPlotValue = 0.00;
					int cntr = 0;
					for (XYPair xy : videoStream.getPlayTimeList()) {
						 yPlotValue = xy.getYVal();
						LOG.debug(String.format("%d\t%.3f\t%.3f", cntr++, xy.getXVal(), xy.getYVal()));
						bufferTimeList.add(yPlotValue);
						seriesPlayTimeBuffer.add(xy.getXVal(), yPlotValue);
						
					}
					Collections.sort(bufferTimeList);
					seriesPlayTimeBufferCollection.addSeries(seriesPlayTimeBuffer);
					LOG.debug(videoStream.getByteToolTipDetailMap());
				}
			}

			BufferTimeBPResult bufferTimeResult = bufferInSecondsCalculatorImpl.updateBufferTimeResult(bufferTimeList);
			aroTraceData.getAnalyzerResult().setBufferTimeResult(bufferTimeResult);

			plot.setRenderer(createRenderer());

		}
		plot.setDataset(seriesPlayTimeBufferCollection);
	}

	public XYItemRenderer createRenderer() {
		XYItemRenderer renderer = new StandardXYItemRenderer();
		XYToolTipGenerator xyToolTipGenerator = toolTipGenerator();
		if (xyToolTipGenerator != null) {
			renderer.setBaseToolTipGenerator(xyToolTipGenerator);
		}
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));
		renderer.setSeriesPaint(0, Color.MAGENTA);
		Shape shape = new Rectangle2D.Double(0, 0, 50, 50);//new Ellipse2D.Double(0, 0, 50, 50);
		renderer.setSeriesShape(0, shape);
		return renderer;
	}

	public XYToolTipGenerator toolTipGenerator() {
		return new XYToolTipGenerator() {

			@Override
			public String generateToolTip(XYDataset dataset, int series, int item) {

				ToolTipDetail ttd = videoStream.getPlayTimeToolTipDetailMap().get(item);
				if (ttd.getStreamStatus().equals(StreamStatus.Load)) {
					return (MessageFormat.format(BUFFER_TIME_OCCUPANCY_TOOLTIP_LOAD
							, String.format("%d", item)
							, String.format("%.0f", ttd.getSegmentID())
							, String.format("%.2f", ttd.getCurrentTotal())
							, String.format("%.3f", (double) dataset.getX(series, item))
							));
				} else if (ttd.getStreamStatus().equals(StreamStatus.Play)) {
					return (MessageFormat.format(BUFFER_TIME_OCCUPANCY_TOOLTIP_PLAY
							, String.format("%d", item)
							, String.format("%.0f", ttd.getSegmentID())
							, String.format("%.2f", ttd.getCurrentTotal())
							, String.format("%.3f", (double) dataset.getX(series, item))
							, String.format("%.3f", ttd.getPlayTime())
							, String.format("%.3f", ttd.getPlayTimeEnd())
							));
				} else {
					return (MessageFormat.format(BUFFER_TIME_OCCUPANCY_TOOLTIP_STALL
							, String.format("%d", item)
							, String.format("%.0f", ttd.getSegmentID())
							, String.format("%.2f", ttd.getCurrentTotal())
							, String.format("%.3f", (double) dataset.getX(series, item))
							, String.format("%.3f", ttd.getPlayTime())
							, String.format("%.3f", ttd.getPlayTimeEnd())
							));
				}
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