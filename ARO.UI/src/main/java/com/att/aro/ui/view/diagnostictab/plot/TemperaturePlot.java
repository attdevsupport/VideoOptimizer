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

import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.TemperatureEvent;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class TemperaturePlot implements IPlot {
	private static final Logger LOGGER = LogManager.getLogger(TemperaturePlot.class);	
	private List<TemperatureEvent> temperatureInfos;

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		XYSeries series = new XYSeries(0);
		if (analysis == null) {
			LOGGER.info("analysis data is null");
		} else {
			TraceResultType resultType = analysis.getAnalyzerResult().getTraceresult().getTraceResultType();
			if (resultType.equals(TraceResultType.TRACE_FILE)) {
				LOGGER.info("didn't get analysis trace data!");
			} else {
				TraceDirectoryResult traceresult = (TraceDirectoryResult) analysis.getAnalyzerResult().getTraceresult();
				AnalysisFilter filter = analysis.getAnalyzerResult().getFilter();
				temperatureInfos = traceresult.getTemperatureInfos();
				
				if (temperatureInfos.size() > 0) {
					for (TemperatureEvent bi : temperatureInfos) {
						series.add(bi.getTimeRecorded(), bi.getcelciusTemperature());
					}

					TemperatureEvent last = temperatureInfos.get(temperatureInfos.size() - 1);
					if (filter.getTimeRange() != null) {
						series.add(filter.getTimeRange().getEndTime(), last.getcelciusTemperature());
					} else {
						series.add(traceresult.getTraceDuration(), last.getcelciusTemperature());
					}
				}

				XYItemRenderer renderer = plot.getRenderer();
				renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
					@Override
					public String generateToolTip(XYDataset dataset, int series, int item) {
							return toolTipContent(item);
					}
				});
			}
			plot.setDataset(new XYSeriesCollection(series));
		}
	}
	
	public String toolTipContent(int item){
		TemperatureEvent bi = temperatureInfos.get(Math.min(item, temperatureInfos.size() - 1));
		StringBuffer displayInfo = new StringBuffer(
				ResourceBundleHelper.getMessageString("temperature.tooltip.prefix"));
		displayInfo.append(MessageFormat.format(
				ResourceBundleHelper.getMessageString("temperature.tooltip.content"),
				bi.getcelciusTemperature()));
		displayInfo.append(ResourceBundleHelper.getMessageString("temperature.tooltip.suffix"));

		return displayInfo.toString();
	}
	
}
