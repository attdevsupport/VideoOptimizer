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
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.view.MainFrame;

public class VideoManifestPanel extends TabPanelJScrollPane{

	private static final long serialVersionUID = 1L;
	private JPanel videoManifestPanel;
	private List<AccordionComponent> accordionList = new ArrayList<>();
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
		for (AccordionComponent accordion : accordionList) {
			panel.add(accordion);
		}
		return panel;
	}
	
	@Override
	public void refresh(AROTraceData analyzerResult) {
		accordionList.clear();
		if (analyzerResult != null && analyzerResult.getAnalyzerResult() != null && analyzerResult.getAnalyzerResult().getVideoUsage() != null) {
			for (AROManifest aroManifest : analyzerResult.getAnalyzerResult().getVideoUsage().getManifests()) {
				if (aroManifest != null && aroManifest.getVideoEventList() != null && !aroManifest.getVideoEventList().isEmpty()) {
					AccordionComponent component = new AccordionComponent(aroManifest, this.overviewRoute, analyzerResult, aroView);
					component.setVisible(false);
					accordionList.add(component);
				}
			}
		}
		videoManifestPanel.remove(manifestPanel);
		manifestPanel = getManifestPanel();
		videoManifestPanel.add(manifestPanel);
		videoManifestPanel.updateUI();
	}
	
	public void refreshLocal(AROTraceData analyzerResult) {
		for (AccordionComponent accordion : accordionList) {
			accordion.updateTitleButton(analyzerResult);
		}
	}

	public void refreshSize(int width) {
		for (AccordionComponent accordion : accordionList) {
			accordion.resize(width);
		}
	}
	
	@Override
	public JPanel layoutDataPanel() {
		return null;
	}
	
	@Override
	public void setScrollLocationMap() {
	}
}
