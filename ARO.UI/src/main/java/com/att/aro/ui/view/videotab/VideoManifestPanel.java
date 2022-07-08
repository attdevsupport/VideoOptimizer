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

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.commons.collections.MapUtils;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;

import lombok.Getter;
import lombok.Setter;


public class VideoManifestPanel extends TabPanelJScrollPane{

	private static final long serialVersionUID = 1L;
	private JPanel videoManifestPanel;
	private List<SegmentTablePanel> segmentTableList = new ArrayList<>();
	private IARODiagnosticsOverviewRoute overviewRoute;
	private JPanel manifestPanel;
	private MainFrame aroView;

	@Getter
	@Setter
	private TreeMap<VideoEvent, Double> chunkPlayTimeList;

	public VideoManifestPanel(IARODiagnosticsOverviewRoute overviewRoute, MainFrame aroView) {

		this.overviewRoute = overviewRoute;
		this.aroView = aroView;
		videoManifestPanel = new JPanel();
		videoManifestPanel.setLayout(new BoxLayout(videoManifestPanel, BoxLayout.PAGE_AXIS));
		videoManifestPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));

		manifestPanel = getManifestPanel(null);
		videoManifestPanel.add(manifestPanel);
		setViewportView(videoManifestPanel);
	}

	private JPanel getManifestPanel(AROTraceData analyzerResult) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
		if (segmentTableList != null && !segmentTableList.isEmpty()) {
			for (SegmentTablePanel segmentTable : segmentTableList) {
				panel.add(segmentTable);
			}
		} else {
			if (analyzerResult != null 
					&& analyzerResult.getAnalyzerResult() != null 
					&& analyzerResult.getAnalyzerResult().getStreamingVideoData() != null 
					&& analyzerResult.getAnalyzerResult().getStreamingVideoData().getRequestMap() != null
					&& !analyzerResult.getAnalyzerResult().getStreamingVideoData().getRequestMap().isEmpty()) {
				JLabel lbl = new JLabel(ResourceBundleHelper.getMessageString("videotab.nostream.segments"));
				lbl.setFont(new Font("accordionLabel", Font.ITALIC, 14));
				panel.add(lbl);
			}
		}
		return panel;
	}
	
	@Override
	public void refresh(AROTraceData analyzerResult) {
		segmentTableList.clear();

		if (analyzerResult != null && analyzerResult.getAnalyzerResult() != null && analyzerResult.getAnalyzerResult().getStreamingVideoData() != null) {
			Collection<VideoStream> videoStreamMap = analyzerResult.getAnalyzerResult().getStreamingVideoData().getVideoStreams();
			updateGraphPanels(analyzerResult, videoStreamMap);

			int counter = 1;
			for (VideoStream videoStream : videoStreamMap) {
				if (MapUtils.isNotEmpty(videoStream.getVideoEventMap())) {
					SegmentTablePanel segmentPanel = new SegmentTablePanel(videoStream, this.overviewRoute, analyzerResult, aroView, this, counter++);
					segmentPanel.setVisible(false);
					segmentTableList.add(segmentPanel);
				}
			}
		}

		videoManifestPanel.remove(manifestPanel);
		manifestPanel = getManifestPanel(analyzerResult);
		videoManifestPanel.add(manifestPanel);
		videoManifestPanel.updateUI();
	}

	public void updateGraphPanels(AROTraceData analyzerResult, Collection<VideoStream> videoStreamMap) {
		VideoTab videoTab = aroView.getVideoTab();
		if (videoStreamMap != null && videoStreamMap.size() > 0) {
			Iterator<VideoStream> videoStreamIterator = videoStreamMap.iterator();
			while (videoStreamIterator.hasNext()) {
				VideoStream videoStream = videoStreamIterator.next();
				SegmentThroughputGraphPanel throughputGraphPanel = videoTab.getThroughputGraphPanel();
				SegmentProgressGraphPanel progressGraphPanel = videoTab.getProgressGraphPanel();
				SegmentBufferGraphPanel bufferGraphPanel = videoTab.getBufferGraphPanel();

				if (videoStream.getVideoSegmentEventList().size() > 0 || videoStream.getAudioSegmentEventList().size() > 0) {
					throughputGraphPanel.refresh(analyzerResult, videoStream, null, null);
					progressGraphPanel.refresh(analyzerResult, videoStream, null, null);

					boolean isStartupDelaySet = (videoStream.getPlayRequestedTime() != null || videoStream.getVideoPlayBackTime() != null);

					if (isStartupDelaySet && videoStream.isCurrentStream()) {
						bufferGraphPanel.refresh(analyzerResult, videoStream, null, null);
					}

					boolean isGraphVisible = videoStreamMap.size() == 1;
					toggleGraphPanels(videoTab, isGraphVisible, isStartupDelaySet);
				} else {
					toggleGraphPanels(videoTab, false, false);
				}
			}
		} else {
			toggleGraphPanels(videoTab, false, false);
		}
	}

	private void toggleGraphPanels(VideoTab videoTab, boolean isGraphVisible , boolean isStartupDelaySet) {
		videoTab.getThroughputPanel().setVisible(isGraphVisible);
		videoTab.getProgressPanel().setVisible(isGraphVisible);
		videoTab.getBufferPanel().setVisible(isGraphVisible && isStartupDelaySet);
	}
	
	public void refreshLocal(AROTraceData analyzerResult) {
		for (SegmentTablePanel segmentTable : segmentTableList) {
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
	
	public List<SegmentTablePanel> getSegmentTableList() {
		return segmentTableList;
	}

}