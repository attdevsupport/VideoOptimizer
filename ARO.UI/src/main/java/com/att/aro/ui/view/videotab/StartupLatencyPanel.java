/*
 *  Copyright 2022 AT&T
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
package com.att.aro.ui.view.videotab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class StartupLatencyPanel extends JPanel {

	private static final int MAX_WIDTH = 550;

	private static final String USER_LABEL = "User Request Time : ";

	private static final long serialVersionUID = 1L;
	
	private static final int MAX_HEIGHT = 500;
	
	private StartupLatancyTableModel startupLatancyTableModel = new StartupLatancyTableModel();

	public StartupLatencyPanel(VideoStream videoStream) {
		add(getStartupDelayPanel(videoStream));
	}

	private Component getStartupDelayPanel(VideoStream videoStream) {
		JPanel startupDelayPanel = new JPanel();
		startupDelayPanel.setPreferredSize(new Dimension(MAX_WIDTH, MAX_HEIGHT));
		startupDelayPanel.add(getRequestTimeLabel(videoStream), BorderLayout.NORTH);
		startupDelayPanel.add(getScrollPane(videoStream), BorderLayout.SOUTH);
		startupDelayPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));	
		startupDelayPanel.setBorder(BorderFactory.createTitledBorder(ResourceBundleHelper.getMessageString("video.usage.dialog.startupDelay")));
		return startupDelayPanel;
	}

	private JScrollPane getScrollPane(VideoStream videoStream) {
		JScrollPane jScrollPane = new JScrollPane(getStartupDelayTable(videoStream)); 
		return jScrollPane;
	}

	private JLabel getRequestTimeLabel(VideoStream videoStream) {
		JLabel requestTimeLabel = new JLabel();
		requestTimeLabel.setText(USER_LABEL +  videoStream.getPlayRequestedTime());
		return requestTimeLabel;
	}

	/**
	 * Initializes and returns the the DataTable that contains HTTP delay
	 * informations.
	 */

	public JTable getStartupDelayTable(VideoStream videoStream) {	
		Collection<VideoEvent> videoEventList = validateVideoEvents(videoStream.getVideoEventMap().values());
		startupLatancyTableModel.setData(videoEventList);	
		DataTable<VideoEvent> startupLatancyTable = new DataTable<VideoEvent>(startupLatancyTableModel);
		startupLatancyTable.setAutoCreateRowSorter(true);
		startupLatancyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		startupLatancyTable.setGridColor(Color.LIGHT_GRAY);
		return startupLatancyTable;		
	}

	private Collection<VideoEvent> validateVideoEvents(Collection<VideoEvent> videoEventList) {
		Collection<VideoEvent> normalisedVideoEventList = new ArrayList<VideoEvent>();
		for (Iterator<VideoEvent> iterator = videoEventList.iterator(); iterator.hasNext();) {
			VideoEvent videoEvent = iterator.next();
			if( videoEvent.isSelected() && videoEvent.isNormalSegment() ) {
				normalisedVideoEventList.add(videoEvent);
			}
		}
		return normalisedVideoEventList;
	}

}