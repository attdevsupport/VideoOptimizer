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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;

public class ConnectionsPlot implements IPlot {

	private XYSeries series;
	private final List<String> toolTipList = new ArrayList<String>(1000);
	double[] start;
	double[] end;

	private static final Logger LOGGER = LogManager.getLogger(ConnectionsPlot.class.getName());

	public void populate(XYPlot plot, AROTraceData analysis) {
		series = new XYSeries(0);
		if (analysis != null) {
			List<Session> sessionList = analysis.getAnalyzerResult().getSessionlist();
			if (sessionList != null && sessionList.size() > 0) {
				calculateSimultConnections(sessionList);
			} else {
				return;
			}
		}

		plot.setDataset(new XYSeriesCollection(series));
		plot.getRenderer().setBaseToolTipGenerator(new XYToolTipGenerator() {

			@Override
			public String generateToolTip(XYDataset dataset, int series, int item) {
				// Tooltip displays no of connections
				return item < toolTipList.size() ? toolTipList.get(item) : "";
			}

		});

	}

	/*
	 * @param sessionList This method calculates the concurrent/simultaneous
	 * connections for every session by comparing with each and every other session
	 */

	public void calculateSimultConnections(List<Session> sessionList) {
		Collections.sort(sessionList);

		start = new double[sessionList.size()];
		List<Double> endTimeList = new ArrayList<Double>();
		int iterator = 0;
		int startTimeCounter = 0;
		int endTimeCounter = 0;
		double startTime = 0.0;
		double endTime = 0.0;
		int currentOverlap = 0;

		for (Session session : sessionList) {
			start[iterator] = session.getSessionStartTime();
			iterator++;
			if (Util.isTestMode()) {
				LOGGER.debug("Start : " + session.getSessionStartTime());
			}
			if ((!session.isUdpOnly() && session.isSessionComplete()) || session.isUdpOnly()) {
				endTimeList.add(session.getSessionEndTime());
				if (Util.isTestMode()) {
					LOGGER.debug("End : " + session.getSessionEndTime());
				}
			}
		}

		if (!endTimeList.isEmpty()) {
			Double[] endListTime = endTimeList.toArray(new Double[endTimeList.size()]);
			end = ArrayUtils.toPrimitive(endListTime);
		}

		Arrays.sort(start);
		Arrays.sort(end);

		series.add(start[startTimeCounter], currentOverlap);
		toolTipList.add(Util.formatDouble(start[startTimeCounter]) + " : " + String.valueOf(currentOverlap));

		while (startTimeCounter < start.length && endTimeCounter < end.length) {
			startTime = start[startTimeCounter];
			endTime = end[endTimeCounter];
			if (startTime < endTime) {
				currentOverlap++;
				startTimeCounter++;
				series.add(startTime, currentOverlap);
				toolTipList.add("Time : " + Util.formatDouble(startTime) + " -> " + String.valueOf(currentOverlap));
			} else if (startTime == endTime) {
				startTimeCounter++;
				endTimeCounter++;
			} else {
				currentOverlap--;
				endTimeCounter++;
				series.add(endTime, currentOverlap);
				toolTipList.add("Time : " + Util.formatDouble(endTime) + " -> " + String.valueOf(currentOverlap));
			}
		}
		while (startTimeCounter < start.length) {
			startTime = start[startTimeCounter];
			currentOverlap++;
			startTimeCounter++;
			series.add(startTime, currentOverlap);
			toolTipList.add("Time : " + Util.formatDouble(startTime) + " -> " + String.valueOf(currentOverlap));
		}

		while (endTimeCounter < end.length) {
			endTime = end[endTimeCounter];
			currentOverlap--;
			endTimeCounter++;
			series.add(endTime, currentOverlap);
			toolTipList.add("Time : " + Util.formatDouble(endTime) + " -> " + String.valueOf(currentOverlap));
		}
	}
}