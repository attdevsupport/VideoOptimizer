/*
 *  Copyright 2018 AT&T
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
import java.util.List;

import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.ILogger;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.SpeedThrottleEvent;
import com.att.aro.core.peripheral.pojo.SpeedThrottleEvent.SpeedThrottleFlow;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Speed Throttle indicated number and time line plot 
 */
public class SpeedThrottlePlot implements IPlot {
	public ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);
	private XYSeriesCollection serCollection = new XYSeriesCollection();
	
 	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
 		serCollection.removeAllSeries();
		if (analysis == null) {
			logger.info("There is no trace data in the pojo!");
		} else {
			TraceResultType resultType = analysis.getAnalyzerResult().getTraceresult().getTraceResultType();
			if (resultType.equals(TraceResultType.TRACE_FILE)) {
				logger.info("This is trace file pojo so there is no speed throttle info involved here.");
			} else {
				XYSeries seriesDL = new XYSeries(0);
				XYSeries seriesUP = new XYSeries(1);

				TraceDirectoryResult traceresult = (TraceDirectoryResult) analysis.getAnalyzerResult().getTraceresult();
				List<SpeedThrottleEvent> speedThrottleInfos = traceresult.getSpeedThrottleEvent();
				calculateNode(seriesDL, seriesUP, speedThrottleInfos);
				setDataPlot(plot, seriesDL, seriesUP);
 			}
		}
	}

	/**
	 * @param plot
	 * @param seriesDL
	 * @param seriesUP
	 */
	private void setDataPlot(XYPlot plot, XYSeries seriesDL, XYSeries seriesUP) {
		serCollection.addSeries(seriesDL);
		serCollection.addSeries(seriesUP);
		
		XYStepRenderer renderer = new XYStepRenderer();
		
		LogAxis rangeAxis = new LogAxis();	// new API 
        rangeAxis.setAutoRange(true);
        rangeAxis.setVisible(false);
  		plot.setRangeAxis(rangeAxis);
   		plot.setRangePannable(true);
		plot.setRangeCrosshairVisible(true);
		
		renderer = (XYStepRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(true);
		renderer.setSeriesStroke(0, new BasicStroke(1.0f));
		renderer.setSeriesStroke(1, new BasicStroke(2.5f));
		renderer.setSeriesPaint(0, Color.blue);
		renderer.setSeriesPaint(1, Color.red);
		renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		renderer.setDefaultEntityRadius(6);
		
		renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
			@Override
			public String generateToolTip(XYDataset dataset, int series, int item) {
				StringBuffer displayInfo = new StringBuffer();
				java.lang.Number tempx = dataset.getX(series, item);
				java.lang.Number tempy = dataset.getY(series, item);
				// series 0 -> downstream , stries 1 -> upstream
				String streamInfo = "";
				if(series ==0){
					streamInfo = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.downlink");
				}else{
					streamInfo = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.uplink");
				}
				return displayInfo.append("Time: "+  tempx +" , "+ streamInfo+" : " + tempy+ " kbps").toString();
			}
		});
		plot.setRenderer(renderer);
		plot.setDataset(serCollection);
		

	}

	/**
	 * @param seriesDL
	 * @param seriesUP
	 * @param attnrInfos
	 */
	private void calculateNode(XYSeries seriesDL, XYSeries seriesUP, List<SpeedThrottleEvent> speedThrottleInfos) {
		try {

			if (speedThrottleInfos.size() > 1) {

				double firstTimeDTamp = 0L;
				double firstTimeUTemp = 0L;
				// initial time
				firstTimeDTamp = speedThrottleInfos.get(0).getTimeStamp();
				firstTimeUTemp = speedThrottleInfos.get(1).getTimeStamp();

				for (SpeedThrottleEvent event : speedThrottleInfos) {

					if (SpeedThrottleFlow.DLT.equals(event.getThrottleFlow())) {
						double tempTime1 = (event.getTimeStamp() - firstTimeDTamp) / 1000;
						double throttleDLS = (double)event.getThrottleSpeed();
						if(throttleDLS>0){
							seriesDL.add(tempTime1, throttleDLS);
						}else{
							seriesDL.add(tempTime1, null);
						}
						logger.info("Time stamp: " + tempTime1+ " " +throttleDLS);

					} else if (SpeedThrottleFlow.ULT.equals(event.getThrottleFlow())) {
						double tempTime2 = (event.getTimeStamp() - firstTimeUTemp) / 1000;
						double throttleULS = (double)event.getThrottleSpeed();
						if(throttleULS>0){
							seriesUP.add(tempTime2, throttleULS);
						}else{
							seriesUP.add(tempTime2, null);
						}
						logger.info("Time stamp: " + tempTime2+ " " +throttleULS);
					} else {
						logger.info("wrong record for attenuation event");
					}
				}
			} else {
				// no data
				return;
			}

		} catch (IndexOutOfBoundsException exception1) {
			logger.info("No sufficient data in the attenuation log file", exception1);
		} catch (Exception exception2) {
			logger.info("Wrong Format", exception2);
		}
	}

}
