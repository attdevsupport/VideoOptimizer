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
import java.util.List;

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
import com.att.aro.core.peripheral.pojo.AttenuatorEvent;
import com.att.aro.core.peripheral.pojo.AttenuatorEvent.AttnrEventFlow;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.ContextAware;

public class AttenuatorPlot implements IPlot {
	public ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if (analysis == null) {
			logger.info("didn't get analysis trace data!");
		} else {
			TraceResultType resultType = analysis.getAnalyzerResult().getTraceresult().getTraceResultType();
			if (resultType.equals(TraceResultType.TRACE_FILE)) {
				logger.info("didn't get analysis trace folder!");

			} else {
				XYSeries seriesDL = new XYSeries(0);
				XYSeries seriesUP = new XYSeries(1);

				TraceDirectoryResult traceresult = (TraceDirectoryResult) analysis.getAnalyzerResult().getTraceresult();

				List<AttenuatorEvent> attnrInfos = traceresult.getAttenautionEvent();

				calculateNode(seriesDL, seriesUP, attnrInfos);

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
		XYSeriesCollection sercollection = new XYSeriesCollection();
		sercollection.addSeries(seriesDL);
		sercollection.addSeries(seriesUP);
		
		XYStepRenderer renderer = new XYStepRenderer();
		XYPlot plot1 = (XYPlot) plot;
		plot1.getRangeAxis().setAutoRangeMinimumSize(2.0);//for the data set is constant value(ex. 0)		
		renderer = (XYStepRenderer) plot1.getRenderer();
		renderer.setBaseShapesVisible(true);
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));
		renderer.setSeriesStroke(1, new BasicStroke(4.0f));
		
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
					streamInfo = "Downlink Delay";
				}else{
					streamInfo = "Uplink Delay";
				}
				return displayInfo.append("Time: "+  tempx +" , "+ streamInfo+" : " + tempy+ "ms").toString();
			}
		});
		plot.setRenderer(renderer);
		plot.setDataset(sercollection);

	}

	/**
	 * @param seriesDL
	 * @param seriesUP
	 * @param attnrInfos
	 */
	private void calculateNode(XYSeries seriesDL, XYSeries seriesUP, List<AttenuatorEvent> attnrInfos) {
		try {

			if (attnrInfos.size() > 1) {

				double firstTimeDTamp = 0L;
				double firstTimeUTemp = 0L;
				// initial time
				firstTimeDTamp = attnrInfos.get(0).getTimeStamp();
				firstTimeUTemp = attnrInfos.get(1).getTimeStamp();

				for (AttenuatorEvent event : attnrInfos) {

					if (AttnrEventFlow.DL.equals(event.getAtnrFL())) {
						double tempTime1 = (event.getTimeStamp() - firstTimeDTamp) / 1000;
						seriesDL.add(tempTime1, (double)event.getDelayTime());
						logger.info("Time stamp: " + tempTime1+ " " +event.getDelayTime());
					} else if (AttnrEventFlow.UL.equals(event.getAtnrFL())) {
						double tempTime2 = (event.getTimeStamp() - firstTimeUTemp) / 1000;
						seriesUP.add(tempTime2, (double)event.getDelayTime());
						logger.info("Time stamp: " + tempTime2+ " " +event.getDelayTime());
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
