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

import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.TemperatureEvent;
import com.att.aro.core.peripheral.pojo.ThermalStatus;
import com.att.aro.core.peripheral.pojo.ThermalStatusInfo;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class TemperaturePlot implements IPlot {
	private static final Logger LOGGER = LogManager.getLogger(TemperaturePlot.class);
	private List<TemperatureEvent> temperatureInfos;
	private List<ThermalStatusInfo> thermalStatusInfos;

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		XYSeries series = new XYSeries(0);
		XYIntervalSeriesCollection thermalDataSeries = new XYIntervalSeriesCollection();

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
				thermalStatusInfos = traceresult.getThermalstatusInfos();
				NumberAxis axis = new NumberAxis();
				axis.setAutoRange(true);
				List<Integer> tempLists = new ArrayList<>(); // Calculate max and min temperature
				if (CollectionUtils.isNotEmpty(temperatureInfos)) {
					for (TemperatureEvent bi : temperatureInfos) {
						series.add(bi.getTimeRecorded(), bi.getcelciusTemperature());
						tempLists.add(bi.getcelciusTemperature());
					}
					TemperatureEvent last = temperatureInfos.get(temperatureInfos.size() - 1);
					if (filter.getTimeRange() != null) {
						series.add(filter.getTimeRange().getEndTime().doubleValue(), last.getcelciusTemperature());
					} else {
						series.add(traceresult.getTraceDuration(), last.getcelciusTemperature());
					}
					XYItemRenderer renderer = plot.getRenderer(0);
					renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
						@Override
						public String generateToolTip(XYDataset dataset, int series, int item) {
							return toolTipContent(item);
						}
					});
					Collections.sort(tempLists);
					axis.setRange(Math.round(tempLists.get(0) / 1.2),
							Math.round(tempLists.get(tempLists.size() - 1) * 1.2));
					axis.setAutoRange(false);
					plot.setRenderer(0, renderer);
				}
				if (CollectionUtils.isNotEmpty(thermalStatusInfos)) {
					Map<ThermalStatus, XYIntervalSeries> seriesMap = new EnumMap<ThermalStatus, XYIntervalSeries>(
							ThermalStatus.class);
					for (ThermalStatus tstatus : ThermalStatus.values()) {
						XYIntervalSeries series2 = new XYIntervalSeries(tstatus);
						seriesMap.put(tstatus, series2);
						thermalDataSeries.addSeries(series2);
					}
					Iterator<ThermalStatusInfo> iter = thermalStatusInfos.iterator();
					if (iter.hasNext()) {
						while (iter.hasNext()) {
							ThermalStatusInfo info = iter.next();
							seriesMap.get(info.getThermalStatus()).add(info.getBeginTimeStamp(),
									info.getBeginTimeStamp(), info.getEndTimeStamp(), 0.5, 0, 100);
						}
					}
					XYBarRenderer barRenderer = new XYBarRenderer();
					barRenderer.setDrawBarOutline(false);
					barRenderer.setUseYInterval(true);
					barRenderer.setAutoPopulateSeriesPaint(false);
					barRenderer.setShadowVisible(false);
					barRenderer.setGradientPaintTransformer(null);
					barRenderer.setBarPainter(new StandardXYBarPainter());

					setRenderingColorForDataSeries(barRenderer, thermalDataSeries);
					barRenderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
						@Override
						public String generateToolTip(XYDataset dataset, int series, int item) {
							ThermalStatus info = (ThermalStatus) thermalDataSeries.getSeries(series).getKey();
							return MessageFormat.format(ResourceBundleHelper.getMessageString("network.tooltip"),
									dataset.getX(series, item), ResourceBundleHelper.getEnumString(info));
						}
					});
					plot.setRenderer(1, barRenderer);
					plot.setRangeAxis(axis);
				}
			}

			plot.setDataset(0, new XYSeriesCollection(series));
			plot.setDataset(1, thermalDataSeries);
		}
	}

	public String toolTipContent(int item) {
		TemperatureEvent bi = temperatureInfos.get(Math.min(item, temperatureInfos.size() - 1));
		StringBuffer displayInfo = new StringBuffer(
				ResourceBundleHelper.getMessageString("temperature.tooltip.prefix"));
		displayInfo.append(MessageFormat.format(ResourceBundleHelper.getMessageString("temperature.tooltip.content"),
				bi.getcelciusTemperature()));
		displayInfo.append(ResourceBundleHelper.getMessageString("temperature.tooltip.suffix"));

		return displayInfo.toString();
	}

	private void setRenderingColorForDataSeries(XYItemRenderer renderer, XYIntervalSeriesCollection dataSeries) {
		renderer.setSeriesPaint(dataSeries.indexOf(ThermalStatus.THROTTLING_NONE), Color.GREEN);// 0
		renderer.setSeriesPaint(dataSeries.indexOf(ThermalStatus.THROTTLING_LIGHT), Color.BLUE);// 1
		renderer.setSeriesPaint(dataSeries.indexOf(ThermalStatus.THROTTLING_MODERATE), Color.YELLOW);// 2
		renderer.setSeriesPaint(dataSeries.indexOf(ThermalStatus.THROTTLING_SEVERE), Color.MAGENTA);// 3
		renderer.setSeriesPaint(dataSeries.indexOf(ThermalStatus.THROTTLING_CRITICAL), Color.RED);// 4
		renderer.setSeriesPaint(dataSeries.indexOf(ThermalStatus.THROTTLING_EMERGENCY), Color.DARK_GRAY);// 5
		renderer.setSeriesPaint(dataSeries.indexOf(ThermalStatus.UNKNOWN), Color.WHITE);// read error
	}

}
