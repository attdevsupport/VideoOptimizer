/*
 *  Copyright 2017,2020 AT&T
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
import java.awt.geom.Ellipse2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.BufferOccupancyBPResult;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.XYPair;
import com.att.aro.core.videoanalysis.impl.BufferOccupancyCalculatorImpl;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class BufferOccupancyPlot implements IPlot {

	private static final Logger LOG = LogManager.getLogger(BufferOccupancyPlot.class.getName());
	private static final String BUFFEROCCUPANCY_TOOLTIP = ResourceBundleHelper.getMessageString("bufferoccupancy.tooltip");
	private Shape shape  = new Ellipse2D.Double(0,0,10,10);

	private List<Double> bufferSizeList = new ArrayList<>();
	
	XYSeriesCollection bufferFillDataCollection = new XYSeriesCollection();
	XYSeries seriesByteBuffer;
	BufferOccupancyCalculatorImpl bufferOccupancyCalculatorImpl= (BufferOccupancyCalculatorImpl) ContextAware.getAROConfigContext().getBean("bufferOccupancyCalculatorImpl",PlotHelperAbstract.class);
	private StreamingVideoData streamingVideoData;
	private VideoStream videoStream;
	
	public void clearPlot(XYPlot plot){
		plot.setDataset(null);	
	}
	
	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		
		if (analysis != null) {

			streamingVideoData = analysis.getAnalyzerResult().getStreamingVideoData();
			
			double maxBuffer = 0;
			bufferFillDataCollection.removeAllSeries();
			
			for (VideoStream videoStream : streamingVideoData.getVideoStreams()) {
				if (videoStream.isSelected()) {
					this.videoStream = videoStream;
					LOG.debug("VideoStream :" + videoStream.getManifest().getVideoName());
					seriesByteBuffer = new XYSeries("Byte Buffer");
					ArrayList<XYPair> byteBufferList = videoStream.getByteBufferList();
					double yPlotValue = 0.00;
					for (XYPair xy : byteBufferList) {
						 yPlotValue = xy.getYVal();
						
						if (maxBuffer < yPlotValue) {
							maxBuffer = yPlotValue;
						}
						bufferSizeList.add(yPlotValue);
						seriesByteBuffer.add(xy.getXVal(), yPlotValue);
						LOG.debug(String.format("%.3f\t%.0f", xy.getXVal(), yPlotValue));
						
					}
					Collections.sort(bufferSizeList);
					bufferFillDataCollection.addSeries(seriesByteBuffer);
					LOG.debug(videoStream.getToolTipDetailMap());
				}
			}

			XYItemRenderer renderer = new StandardXYItemRenderer();
			renderer.setBaseToolTipGenerator(toolTipGenerator());
			renderer.setSeriesStroke(0, new BasicStroke(2.0f));
			renderer.setSeriesPaint(0, Color.blue);
			renderer.setSeriesShape(0, shape);

			plot.setRenderer(renderer);
			
			BufferOccupancyBPResult bufferOccupancyResult = bufferOccupancyCalculatorImpl.setMaxBuffer(maxBuffer);
			bufferOccupancyResult.setBufferByteDataSet(bufferSizeList);
			analysis.getAnalyzerResult().setBufferOccupancyResult(bufferOccupancyResult);
		}

		plot.setDataset(bufferFillDataCollection);
	}

	public XYToolTipGenerator toolTipGenerator() {
		return new XYToolTipGenerator() {

			@Override
			public String generateToolTip(XYDataset dataset, int series, int item) {

				// Tooltip value
				Number timestamp = dataset.getX(series, item);
				Number bufferSize = dataset.getY(series, item);

				VideoEvent event = videoStream.getToolTipDetailMap().get(item).getVideoEvent();
				double segmentID = event.getSegmentID();
				ContentType type = event.getContentType();
				double play = event.getPlayTime();

				return (MessageFormat.format(BUFFEROCCUPANCY_TOOLTIP, String.format("%s", type.toString()),
						String.format("%.0f", segmentID), String.format("%.2f", (double) bufferSize / (1000 * 1000)),
						String.format("%.3f", timestamp), String.format("%.3f", play)));

			}
		};
	}
	
}
