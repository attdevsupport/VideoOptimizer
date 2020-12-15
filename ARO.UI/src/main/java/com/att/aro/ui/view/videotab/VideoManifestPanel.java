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
package com.att.aro.ui.view.videotab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.view.MainFrame;

public class VideoManifestPanel extends TabPanelJScrollPane{

	private static final long serialVersionUID = 1L;
	private JPanel videoManifestPanel;
	private List<SegmentPanel> segmentTableList = new ArrayList<>();
	private IARODiagnosticsOverviewRoute overviewRoute;
	private JPanel manifestPanel;
	private MainFrame aroView;

	public VideoManifestPanel(IARODiagnosticsOverviewRoute overviewRoute, MainFrame aroView) {

		this.overviewRoute = overviewRoute;
		this.aroView = aroView;
		videoManifestPanel = new JPanel();
		videoManifestPanel.setLayout(new BoxLayout(videoManifestPanel, BoxLayout.PAGE_AXIS));
		videoManifestPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));

		manifestPanel = getManifestPanel();
		videoManifestPanel.add(manifestPanel);
		setViewportView(videoManifestPanel);
	}

	private JPanel getManifestPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
		for (SegmentPanel segmentTable : segmentTableList) {
			panel.add(segmentTable);
		}
		return panel;
	}
	
	@Override
	public void refresh(AROTraceData analyzerResult) {
		segmentTableList.clear();
		Collection<VideoStream> videoStreamMap = null ;
		if (analyzerResult != null && analyzerResult.getAnalyzerResult() != null && analyzerResult.getAnalyzerResult().getStreamingVideoData() != null) {
		 videoStreamMap = analyzerResult.getAnalyzerResult().getStreamingVideoData().getVideoStreamMap().values();	
		 updateGraphPanel(analyzerResult, videoStreamMap);	
			for (VideoStream videoStream : videoStreamMap) {
				if (videoStream != null && videoStream.getVideoEventMap() != null
						&& !videoStream.getVideoEventMap().isEmpty()) {
					SegmentPanel segmentPanel = new SegmentPanel(videoStream, this.overviewRoute, analyzerResult, aroView,
							this);
					segmentPanel.setVisible(false);
					segmentTableList.add(segmentPanel);
				}
			}
		}
		
		videoManifestPanel.remove(manifestPanel);
		manifestPanel = getManifestPanel();
		videoManifestPanel.add(manifestPanel);
		videoManifestPanel.updateUI();		
	}

	public void updateGraphPanel(AROTraceData analyzerResult, Collection<VideoStream> videoStreamMap) {
		VideoTab videoTab = aroView.getVideoTab();
		VideoGraphPanel graphPanel = videoTab.getGraphPanel();
		if (videoStreamMap != null && videoStreamMap.size() > 0) {
			VideoStream videoStream = videoStreamMap.iterator().next();
			if (videoStream.getVideoSegmentEventList().size() > 0
					|| videoStream.getAudioSegmentEventList().size() > 0) {
				graphPanel.refresh(analyzerResult, videoStream, null, null);
				graphPanel.setVisible(videoStreamMap.size() == 1);
			}
		} else {
			graphPanel.setVisible(false);
		}
	}
	
	public void refreshLocal(AROTraceData analyzerResult) {
		for (SegmentPanel segmentTable : segmentTableList) {
			segmentTable.updateTitleButton(analyzerResult);
		}
	}
	
	@Override
	public JPanel layoutDataPanel() {
		return null;
	}
	
	@Override
	public void setScrollLocationMap() {
	}
	
	 public List<SegmentPanel> getSegmentTableList() {
	        return segmentTableList;
	    }
}