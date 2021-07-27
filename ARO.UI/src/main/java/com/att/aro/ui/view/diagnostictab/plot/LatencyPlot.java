/*
 *  Copyright 2021 AT&T
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
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class LatencyPlot implements IPlot {

private XYSeries series;

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		series = new XYSeries(0);
		if (analysis != null) {
			List<Session> sessionList = analysis.getAnalyzerResult().getSessionlist();
			List<Session> tooltipList = new ArrayList<Session>(sessionList.size());
			if (sessionList.size() > 0) {
				for (Session session : sessionList) {
					if (session.getLatency() >= 0) {
						series.add(session.getSessionStartTime(), session.getLatency());
						tooltipList.add(session);
					}

				}
			} else {
				return;
			}

			plot.setDataset(new XYSeriesCollection(series));
			plot.getRenderer().setBaseToolTipGenerator(new XYToolTipGenerator() {

				@Override
				public String generateToolTip(XYDataset dataset, int series, int item) {
					return item < tooltipList.size() ? getToolTip(tooltipList.get(item)) : "";
				}

				private String getToolTip(Session session) {
					StringBuffer tooltipValue = new StringBuffer();
					tooltipValue.append(String.format("%.2f,%s,%.3f,%.3f", session.getSessionStartTime(),
							Util.formatDoubleToMicro(session.getLatency()), session.getSynTime(),
							session.getSynAckTime()));
					String[] value = tooltipValue.toString().split(",");
					return (MessageFormat.format(ResourceBundleHelper.getDefaultBundle().getString("latency.tooltip"),
							value[0], value[1], value[2], value[3]));

				}
			});
		}
	}

}
