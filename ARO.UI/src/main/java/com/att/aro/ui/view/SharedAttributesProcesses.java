/*
 *  Copyright 2015 AT&T
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

package com.att.aro.ui.view;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.android.ddmlib.IDevice;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.pojo.CollectorStatus;
import com.att.aro.core.mobiledevice.pojo.IAroDevices;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.view.diagnostictab.ChartPlotOptions;
import com.att.aro.ui.view.diagnostictab.GraphPanel;
import com.att.aro.ui.view.video.IVideoPlayer;

/**
 * This encapsulates the mechanism for maintaining which tabbed UI panel you are
 * on. Some menu selection items are enabled or disabled based on this (example:
 * Print is only available for Best Practices and Statistics tabs)
 * 
 *
 *
 */
public interface SharedAttributesProcesses extends IAROView {
	public enum TabPanels {
		tab_panel_other,
		tab_panel_best_practices,
		tab_panel_video_tab,
		tab_panel_statistics
	}

	void updateProfile(Profile profile);
	void updateReportPath(File path);
	void updateFilter(AnalysisFilter filter);
	String getTracePath();
	void notifyPropertyChangeListeners(String property, Object oldValue, Object newValue);
	void notifyActionListeners(int key, String command);
	
	// collector control

	void startVideoCollector(String msg);

	void stopCollector();

	void haltCollector();

	IDevice[] getConnectedDevices();

	IAroDevices getAroDevices();

	List<IDataCollector> getAvailableCollectors();

	CollectorStatus getCollectorStatus();

	String[] getApplicationsList(String id);

	
	Frame getFrame();

	TabPanels getCurrentTabPanel();
	Component getCurrentTabComponent();

	Profile getProfile();
	boolean isVideoPlayerSelected();
	public IVideoPlayer getVideoPlayer();
	public GraphPanel getGraphPanel();
	void updateVideoPlayerSelected(boolean videoSelected);
	boolean isModelPresent();
	void dataDump(File dir) throws IOException;
	void updateChartSelection(List<ChartPlotOptions> optionsSelected);
	void dispose();
	void setTrafficFile(String trafficFile);
	void setPcapTempWrap(boolean b);
	
}
