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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentListener;
import java.text.MessageFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.packetanalysis.IVideoTrafficCollector;
import com.att.aro.core.packetanalysis.impl.VideoTrafficCollectorImpl;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.videoanalysis.impl.VideoPrefsController;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.IAROPrintable;
import com.att.aro.ui.commonui.ImagePanel;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.commonui.UIComponent;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.AROModelObserver;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.bestpracticestab.BpHeaderPanel;
import com.att.aro.ui.view.diagnostictab.ChartPlotOptions;
import com.att.aro.ui.view.diagnostictab.GraphPanel;
import com.att.aro.ui.view.statistics.DateTraceAppDetailPanel;
import com.att.aro.view.images.Images;

public class VideoTab extends TabPanelJScrollPane implements IAROPrintable{
	private static final Logger LOG = LogManager.getLogger(VideoTab.class.getName());
	
	private static final long serialVersionUID = 1L;
	AROModelObserver bpObservable;
	private JPanel container;
	private JPanel mainPanel;
	private IARODiagnosticsOverviewRoute overviewRoute = null;
	private MainFrame aroView;
	private VideoTrafficCollectorImpl videoStreamingAnalysis = (VideoTrafficCollectorImpl) ContextAware.getAROConfigContext().getBean(IVideoTrafficCollector.class);
	private VideoPrefsController videoPrefsController = ContextAware.getAROConfigContext().getBean(VideoPrefsController.class);
	private Insets insets = new Insets(10, 1, 10, 1);
	private Insets headInsets = new Insets(10, 1, 0, 1);
	private Insets noInsets = new Insets(0, 0, 0, 0);
	private ArrayList<TabPanelJPanel> localRefreshList = new ArrayList<>();
	private VideoManifestPanel videoManifestPanel;
	
	private String trace = "";
	private long lastOpenedTrace;
	private StartUpDelayWarningDialog startUpDelayWarningDialog = null;
	private String warningMessage;
	int graphPanelIndex = 0;
 	
	/**
	 * Create the panel.
	 */
	public VideoTab(MainFrame aroView, IARODiagnosticsOverviewRoute overviewRoute) {
		super();
		this.aroView = aroView;
		this.overviewRoute = overviewRoute;

		bpObservable = new AROModelObserver();

		container = new JPanel(new BorderLayout());
		
		String headerTitle = MessageFormat.format(ResourceBundleHelper.getMessageString("videoTab.title")
				, ApplicationConfig.getInstance().getAppBrandName()
				, ApplicationConfig.getInstance().getAppShortName());
				
		container.add(UIComponent.getInstance().getLogoHeader(headerTitle), BorderLayout.NORTH);

		// Summaries, Manifest, Requests
		ImagePanel panel = new ImagePanel(null);
		panel.setLayout(new GridBagLayout());
		panel.add(layoutDataPanel(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
				, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL
				, new Insets(10, 10, 0, 10)
				, 0, 0));
		container.add(panel, BorderLayout.CENTER);
		
		setViewportView(container);
		getVerticalScrollBar().setUnitIncrement(10);
		getHorizontalScrollBar().setUnitIncrement(10);
	}

	@Override
	public JPanel layoutDataPanel() {
		if (mainPanel == null) {
			
			mainPanel = new JPanel(new GridBagLayout());
			mainPanel.setOpaque(false);

			int section = 1;			
			
			mainPanel.add(buildSummariesGroup(), new GridBagConstraints(
					0, section++
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(0, 0, 0, 0)
					, 0, 0));
			
			graphPanelIndex = section++;
			addGraphPanel();
			
			mainPanel.add(buildManifestsGroup(), new GridBagConstraints(
					0, section++
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(10, 0, 0, 0)
					, 0, 0));
			
			mainPanel.add(buildRequestsGroup(), new GridBagConstraints(
					0, section++
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(10, 0, 0, 0)
					, 0, 0));
		}
		return mainPanel;
	}
	
	public void addGraphPanel() {
		if (mainPanel != null) {
			mainPanel.add(buildGraphPanel(), new GridBagConstraints(
					0, graphPanelIndex
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(10, 0, 0, 0)
					, 0, 0));
		}
		aroView.getDiagnosticTab().getGraphPanel().setChartOptions(ChartPlotOptions.getVideoDefaultView());
	}
	
	private void openStartUpDelayWarningDialog() {
		boolean startUpReminder = true;
		boolean videoAnalyzed = CollectionUtils.containsAny(SettingsUtil.retrieveBestPractices(), BestPracticeType.getByCategory(Category.VIDEO));
		boolean startupDelayAnalyzed = SettingsUtil.retrieveBestPractices().contains(BestPracticeType.STARTUP_DELAY);
		if (videoAnalyzed && startupDelayAnalyzed && isStartUpReminderRequired() && aroView != null && aroView.getCurrentTabComponent() == aroView.getVideoTab()) {
			if (null == startUpDelayWarningDialog) {
				startUpDelayWarningDialog = new StartUpDelayWarningDialog(aroView, overviewRoute);
			} else if (null != videoStreamingAnalysis) {
				VideoUsagePrefs videoPrefs = videoPrefsController.loadPrefs();
				startUpReminder = videoPrefs.isStartupDelayReminder();
			}
			startUpDelayWarningDialog.setDialogInfo(warningMessage, startUpReminder);
			startUpDelayWarningDialog.setVisible(true);
		}
	}
	
	private boolean isStartUpReminderRequired() {
		boolean preferenceStartUpReminder = false;
		boolean startupDelaySet = false;
		int manifestsSelected = 0;
		boolean moreManifestsSelected = false;
		boolean result = true;
		
		if (null != videoStreamingAnalysis) {
			VideoUsagePrefs videoPrefs = videoPrefsController.loadPrefs();
			preferenceStartUpReminder = videoPrefs.isStartupDelayReminder();

			if (videoStreamingAnalysis.getStreamingVideoData() != null && CollectionUtils.isNotEmpty(videoStreamingAnalysis.getStreamingVideoData().getVideoStreamMap().values())) {
				for (VideoStream videoStream : videoStreamingAnalysis.getStreamingVideoData().getVideoStreamMap().values()) {
					if (videoStream != null && true == videoStream.isSelected()) {
						// if any of the manifest file is selected
						manifestsSelected++;
						// User updated the startup delay
						if (videoStream.getManifest().getDelay() > 0 && false == startupDelaySet) {
							startupDelaySet = true;
						}
					}
				}
			}
			moreManifestsSelected = (manifestsSelected > 1) ? true : false;

			if (moreManifestsSelected) {
				warningMessage = ResourceBundleHelper.getMessageString("startupdelay.warning.dialog.message1");
			} else {
				warningMessage = ResourceBundleHelper.getMessageString("startupdelay.warning.dialog.message2");
			}

			if (preferenceStartUpReminder == false) {
				result = false;
			}
			if (manifestsSelected == 0) {
				result = false;
			} else if (preferenceStartUpReminder == true && (moreManifestsSelected == false && startupDelaySet == true)) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * TopPanel contains summaries
	 */
	private JPanel buildSummariesGroup() {
		
		JPanel topPanel;
		topPanel = new JPanel(new GridBagLayout());

		topPanel.setOpaque(false);
		topPanel.setBorder(new RoundedBorder(new Insets(20, 20, 20, 20), Color.WHITE));
		int section = 0;

		// Trace Summary, common with Best Practices and Statistics
		DateTraceAppDetailPanel dateTraceAppDetailPanel = new DateTraceAppDetailPanel();
		topPanel.add(dateTraceAppDetailPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
		bpObservable.registerObserver(dateTraceAppDetailPanel);
		
		// Separator
		topPanel.add(UIComponent.getInstance().getSeparator(),
				new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 10, 0), 0, 0));
			
		topPanel.add(getSummaryPane(), new GridBagConstraints(0, section, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
		
		return topPanel;
	}
 

	private JPanel getSummaryPane() {
		JPanel summaryPane = new JPanel(new GridBagLayout());
		summaryPane.setOpaque(false);

		// VideoSummaryPanel
		VideoSummaryPanel videoSummaryPanel = new VideoSummaryPanel();
		localRefreshList.add(videoSummaryPanel);
		bpObservable.registerObserver(videoSummaryPanel);

		Insets inset = new Insets(0, 1, 10, 1);
		summaryPane.add(videoSummaryPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, inset, 0, 0));
 
		return summaryPane;
	}
	
	private JPanel buildGraphPanel() {
		GraphPanel graphPanel = aroView.getDiagnosticTab().getGraphPanel();
		graphPanel.setGraphPanelBorder(true);
		return graphPanel;
	}

	/**
	 * MidPanel contains Video Manifests
	 */
	private JPanel buildManifestsGroup() {
		
		JPanel pane;
		pane = new JPanel(new GridBagLayout());

		int section = 0;
		
		videoManifestPanel = new VideoManifestPanel(overviewRoute, aroView);
		pane.add(videoManifestPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));		
		bpObservable.registerObserver(videoManifestPanel);
        
		JPanel wrapper = getTitledWrapper("video.tab.manifest.title", new LoadManifestDialog(aroView));
		wrapper.add(pane, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));

		pane.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		
		return wrapper;
	}
	
	/**
	 * MidPanel contains Video Requests
	 */
	private JPanel buildRequestsGroup() {
		
		JPanel pane;
		pane = new JPanel(new GridBagLayout());

		int section = 0;
		
		VideoRequestPanel requestPanel = new VideoRequestPanel();
		pane.add(requestPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));
		bpObservable.registerObserver(requestPanel);
		
		JPanel wrapper = getTitledWrapper("video.tab.request.title", null);
		wrapper.add(pane, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));
		return wrapper;
	}
	
	private JPanel getTitledWrapper(String title, JComponent component) {

		JPanel pane = new JPanel(new GridBagLayout());

		pane.setOpaque(false);
		pane.setBorder(new RoundedBorder(new Insets(0, 10, 10, 10), Color.WHITE));

		JPanel fullPanel = new JPanel(new BorderLayout());


		fullPanel.setOpaque(false);

		// Create the header bar
		BpHeaderPanel header = new BpHeaderPanel(ResourceBundleHelper.getMessageString(title));
		header.setImageTitle(Images.BLUE_HEADER.getImage(), null);
		if (component != null) {
			header.add(component, BorderLayout.EAST);
		}
		fullPanel.add(header, BorderLayout.NORTH);
		pane.add(fullPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
				, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL
				, headInsets, 0, 0));
		return pane;
	}
	
	/**
	 * Refreshes the VideoTab using the specified trace analysis
	 * data. This method is typically called when a new trace file is loaded.
	 * 
	 * @param analysisData
	 *            The trace analysis data.
	 */
	@Override
	public void refresh(AROTraceData analyzerResult) {
		AbstractTraceResult result = analyzerResult.getAnalyzerResult().getTraceresult();
		long newTraceTime = ((MainFrame)aroView).getLastOpenedTime();
		if (lastOpenedTrace == newTraceTime &&
				(trace.equals(result.getTraceDirectory()) || trace.equals(result.getTraceFile()))) {
			refreshLocal(analyzerResult);
		} else {
			trace = result.getTraceDirectory() != null ? result.getTraceDirectory() : result.getTraceFile();
			lastOpenedTrace = newTraceTime;
			bpObservable.refreshModel(analyzerResult);
 			updateUI();
		}
		//open set start up popup, if only start up delay is not already set 
		GraphPanel graphPanel = aroView.getDiagnosticTab().getGraphPanel();
		try {
			if (graphPanel.getVcPlot().getStartUpDelayCollection().isEmpty() && result.getTraceResultType() == TraceResultType.TRACE_DIRECTORY) {
				openStartUpDelayWarningDialog();
			}
		} catch (Exception e) {
			LOG.error("Refresh error:", e);
		}
	}
	
	public void refreshLocal(AROTraceData analyzerResult) {
		for (TabPanelJPanel container : localRefreshList) {
			container.refresh(analyzerResult);
		}
		videoManifestPanel.refreshLocal(analyzerResult);
	}

	@Override
	public synchronized void addComponentListener(ComponentListener listener) {
		super.addComponentListener(listener);
	}

	/**
	 * Triggers and expansion of any tableViews that need expanding before returning the container.
	 * @return a JPanel prepared for printing everything
	 */
	@Override
	public JPanel getPrintablePanel() {
		return container;
	}

	@Override
	public void setScrollLocationMap() {
	}

}
