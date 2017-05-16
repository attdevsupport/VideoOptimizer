/*
  *  Copyright 2012 AT&T
  *  
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
package com.att.aro.ui.commonui;

import java.util.List;
import com.att.aro.core.preferences.IPreferenceHandler;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.ui.view.diagnostictab.ChartPlotOptions;

/*
 * TODO: There is the difference between the way we get UserPreferences 
 * (in ARO.Core) and the way we get GUIPreferences - we get UserPreferences 
 * through UserPreferencesFactory but we get GUIPreferences directly through 
 * its getInstance() method. Do we want to have a factory for GUIPreferences?
 */
public class GUIPreferences {

	private static GUIPreferences instance;

	private static final String CHART_PLOT_OPTIONS = "CHART_PLOT_OPTIONS";

	private IPreferenceHandler prefHandler;
	
	static {
		instance = new GUIPreferences();
	}

	/**
	 * Gets a static instance of the UIPreferences class.
	 * 
	 * @return A static UIPreferences object.
	 */
	public static GUIPreferences getInstance() {
		return instance;
	}

	/**
	 * Private constructor. Use getInstance()
	 */
	private GUIPreferences() {
		prefHandler = PreferenceHandlerImpl.getInstance();
	}

	/**
	 * Set the list of chart plot options.
	 * 
	 * @param chartPlotOptions
	 *            A List of ChartPlotOptions objects containing the user
	 *            configurable list of items to plot on the Diagnostic Chart.
	 */
	public void setChartPlotOptions(List<ChartPlotOptions> chartPlotOptions) {
		if (chartPlotOptions == null) {
			throw new IllegalArgumentException("List of chart plot options must be a non-null object.");
		}
		String optionsString = ChartPlotOptions.toUserPrefsListString(chartPlotOptions);
		prefHandler.setPref(CHART_PLOT_OPTIONS, optionsString);
	}

	/**
	 * Retrieves the chart plot options. The user configurable list of items to
	 * plot on the Diagnostic Chart.
	 * 
	 * @return A List of ChartPlotOptions objects containing the information.
	 */
	public List<ChartPlotOptions> getChartPlotOptions() {
		String chartPlotsOptionPrefsString = prefHandler.getPref(CHART_PLOT_OPTIONS);
		List<ChartPlotOptions> list = ChartPlotOptions.toUserPrefsList(chartPlotsOptionPrefsString);
		return list == null ? ChartPlotOptions.getDefaultList() : list;
	}
	
}
