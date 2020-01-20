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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import javax.swing.JPanel;

import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.view.MainFrame;


public class BpVideoPanel extends BpDetail {
	
	private static final long serialVersionUID = 1L;

	public BpVideoPanel(String title,MainFrame aroView,
			IARODiagnosticsOverviewRoute diagnosticsOverviewRoute) {
		super(title, diagnosticsOverviewRoute);
		
		setBackground(new Color(238,238,238));
		int row = 0;

		addPanel(row++, new BpDetailItem("videoStall"             , BestPracticeType.VIDEO_STALL		, new BPVideoStallTablePanel(), aroView));
		addPanel(row++, new BpDetailItem("startUpDelay"           , BestPracticeType.STARTUP_DELAY		, new StartupDelayComparisonPanel(), aroView));
		addPanel(row++, new BpDetailItem("bufferOccupancy"        , BestPracticeType.BUFFER_OCCUPANCY	, aroView));
		addPanel(row++, new BpDetailItem("networkComparison"      , BestPracticeType.NETWORK_COMPARISON ));
		addPanel(row++, new BpDetailItem("tcpConnection"          , BestPracticeType.TCP_CONNECTION     ));
		addPanel(row++, new BpDetailItem("segmentSize"            , BestPracticeType.CHUNK_SIZE         ));
		addPanel(row++, new BpDetailItem("segmentPacing"          , BestPracticeType.CHUNK_PACING       ));
		addPanel(row++, new BpDetailItem("videoRedundancy"        , BestPracticeType.VIDEO_REDUNDANCY   ));
		addPanel(row++, new BpDetailItem("videoConcurrentSession" , BestPracticeType.VIDEO_CONCURRENT_SESSION, new BPVideoConcurrentSessionTablePanel()));
		addPanel(row++, new BpDetailItem("videoVariableBitrate"   , BestPracticeType.VIDEO_VARIABLE_BITRATE));
		addPanel(row++, new BpDetailItem("videoResolutionQuality" , BestPracticeType.VIDEO_RESOLUTION_QUALITY));
		addPanel(row++, new BpDetailItem("audioStream"            , BestPracticeType.AUDIO_STREAM));
		
		fullPanel.add(dataPanel, BorderLayout.CENTER);
		fullPanel.add(detailPanel, BorderLayout.SOUTH);
		add(fullPanel);
		
		List<BestPracticeType> list = BestPracticeType.getByCategory(Category.VIDEO);
		bpFileDownloadTypes.addAll(list);
	}


	@Override
	public JPanel layoutDataPanel() {
		return null;
	}
	
	@Override
	public void refresh(AROTraceData model) {
		dateTraceAppDetailPanel.refresh(model);
		overViewObservable.refreshModel(model);
		updateHeader(model);
		bpResults = model.getBestPracticeResults();
	}
	
}
