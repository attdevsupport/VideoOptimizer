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
package com.att.aro.ui.view.diagnostictab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * The ChartPlotOptions enumeration defines constant values that specify the
 * items of diagnostic information that can be plotted on the Diagnostics Chart.
 * The user selects which of these items to plot on the Diagnostics Chart by
 * marking individual check boxes in the View Options dialog box.
 */
public enum ChartPlotOptions {

	CPU, TEMPERATURE, ALARM, BURST_COLORS, BATTERY, WAKELOCK, SCREEN, RADIO, BLUETOOTH, CAMERA, GPS, 
	WIFI, ATTENUATION, SPEED_THROTTLE, UL_PACKETS, DL_PACKETS, BURSTS, USER_INPUT, RRC, THROUGHPUT, THROUGHPUTUL, THROUGHPUTDL, LATENCY, CONNECTIONS, 
	NETWORK_TYPE, BUFFER_OCCUPANCY, VIDEO_CHUNKS, BUFFER_TIME_OCCUPANCY, 
	DEFAULT_VIEW, DEFAULT_VIDEO_VIEW;

	private static final String DELIM = ";";
	
	private static final Logger LOGGER = LogManager.getLogger(ChartPlotOptions.class.getName());

	/**
	 * Returns a String containing the chart plot options selected by the user
	 * in the View Options dialog box.
	 * 
	 * @param list
	 *            - A List of ChartPlotOptions enumeration values that specify
	 *            the user selected chart plot options.
	 * 
	 * @return A string containing the user selected chart plot options.
	 * 
	 * @see ChartPlotOptions
	 */
	public static String toUserPrefsListString(List<ChartPlotOptions> list) {
		StringBuilder sbuilder = new StringBuilder();
		for (ChartPlotOptions cpo : list) {
			sbuilder.append(cpo.name()).append(DELIM);
		}
		return sbuilder.toString();
	}

	/**
	 * Returns the list of chart plot options selected by the user in the View
	 * Options dialog box. These options specify which items are plotted on the
	 * Diagnostics Chart.
	 * 
	 * @param delimitedPrefsString
	 *            - The delimeter string that is used to separate the returned
	 *            list of chart plot options.
	 * 
	 * @return A List of ChartPlotOptions enumeration values that specify the
	 *         user selected chart plot options.
	 */
	public static List<ChartPlotOptions> toUserPrefsList(
			String delimitedPrefsString) {
		List<ChartPlotOptions> list = new ArrayList<ChartPlotOptions>();

		if (delimitedPrefsString == null) {
			return null;
		}

		String[] tokens = delimitedPrefsString.split(DELIM);
		for (String str : tokens) {
			if (str != null && !"".equals(str.trim())) {
				try {
					ChartPlotOptions cpo = ChartPlotOptions.valueOf(str);
					list.add(cpo);
				} catch (IllegalArgumentException e) {
					LOGGER.warn("Unrecognized chart plot option in preferences: " + str);
				}
			}
		}
		return list;
	}

	/**
	 * Returns the default list of chart plot options. These are the options
	 * that are selected when the user marks the Default View checkbox in the
	 * View Options dialog box.
	 * 
	 * @return A List of ChartPlotOptions enumeration values that specify the
	 *         default chart plot options.
	 */
	public static List<ChartPlotOptions> getDefaultList() {
		List<ChartPlotOptions> defaultChartPlotList = new ArrayList<ChartPlotOptions>();
		defaultChartPlotList.add(BURST_COLORS);
		defaultChartPlotList.add(UL_PACKETS);
		defaultChartPlotList.add(DL_PACKETS);
		defaultChartPlotList.add(BURSTS);
		defaultChartPlotList.add(USER_INPUT);
		defaultChartPlotList.add(RRC);
		defaultChartPlotList.add(THROUGHPUT);
		defaultChartPlotList.add(THROUGHPUTUL);
		defaultChartPlotList.add(THROUGHPUTDL);
		defaultChartPlotList.add(LATENCY);
		return Collections.unmodifiableList(defaultChartPlotList);
	}

	public static List<ChartPlotOptions> getVideoDefaultView() {
		List<ChartPlotOptions> defaultChartPlotList = new ArrayList<ChartPlotOptions>();
		defaultChartPlotList.add(THROUGHPUT);
		defaultChartPlotList.add(THROUGHPUTUL);
		defaultChartPlotList.add(THROUGHPUTDL);
		defaultChartPlotList.add(UL_PACKETS);
		defaultChartPlotList.add(DL_PACKETS);
		defaultChartPlotList.add(BUFFER_OCCUPANCY);
		defaultChartPlotList.add(VIDEO_CHUNKS);
		defaultChartPlotList.add(BUFFER_TIME_OCCUPANCY);
		return Collections.unmodifiableList(defaultChartPlotList);
	}
}
