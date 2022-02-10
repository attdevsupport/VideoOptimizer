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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.XYPair;
import com.att.aro.core.videoanalysis.impl.BufferInSecondsCalculatorImpl;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoStream.StreamStatus;
import com.att.aro.core.videoanalysis.pojo.VideoStream.ToolTipDetail;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.diagnostictab.plot.IPlot;

import lombok.Getter;

public class VideoBufferPlot implements IPlot {
	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP_LOAD = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip.load");
	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP_PLAY = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip.play");
	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP_STALL = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip.stall");

	private static final Logger LOGGER = LogManager.getLogger(VideoBufferPlot.class);
	private VideoStream videoStream;
	@Getter private double maxYValue;
	@Getter private double maxXValue;
	@Getter private double minYValue;
	@Getter private double minXValue;
	private XYSeries bufferProgressSeries = new XYSeries("Buffer Time");
	
	private BufferInSecondsCalculatorImpl bufferInSecondsCalculatorImpl = (BufferInSecondsCalculatorImpl) ContextAware.getAROConfigContext()
			.getBean("bufferInSecondsCalculatorImpl", PlotHelperAbstract.class);

	/*
	 * launched by:
	 *  -> SegmentBufferGraphPanel.refresh(AROTraceData, VideoStream, JCheckBox, JCheckBox)
	 */
	public VideoBufferPlot(AROTraceData aroTraceData, VideoStream videoStream) {
		this.videoStream = videoStream;
		calculateBufferProgress(aroTraceData);
	}

	/*
	 * called by:
	 *  -> SegmentBufferGraphPanel.refresh(AROTraceData, VideoStream, JCheckBox, JCheckBox)
	 */
	@Override
	public void populate(XYPlot plot, AROTraceData aroTraceData) {
		if (aroTraceData == null) {
			LOGGER.info("no trace data here");
		} else {
			XYItemRenderer videoRenderer = plot.getRenderer();
			videoRenderer.setBaseToolTipGenerator(toolTipGenerator());
		}
		plot.setDataset(new XYSeriesCollection(bufferProgressSeries));
	}
	
	/*
	 * called by:
	 *  -> VideoBufferPlot.VideoBufferPlot(AROTraceData, VideoStream)
	 *     -> SegmentBufferGraphPanel.refresh(AROTraceData, VideoStream, JCheckBox, JCheckBox)
	 */
	public void calculateBufferProgress(AROTraceData aroTraceData) {

		bufferProgressSeries.clear();

		if (aroTraceData != null && aroTraceData.getAnalyzerResult().getStreamingVideoData() != null) {
			for (VideoStream videoStream : aroTraceData.getAnalyzerResult().getStreamingVideoData().getVideoStreams()) {
				if (videoStream.isSelected()) {
					LOGGER.debug("VideoStream :" + videoStream.getManifest().getVideoName());

					for (XYPair xy : videoStream.getPlayTimeList()) {
						bufferProgressSeries.add(xy.getXVal(), xy.getYVal());
					}
				}
			}
		}

		minYValue = bufferProgressSeries.getMinY();
		maxYValue = bufferProgressSeries.getMaxY();
		minXValue = bufferProgressSeries.getMinX();
		maxXValue = bufferProgressSeries.getMaxX();
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

}