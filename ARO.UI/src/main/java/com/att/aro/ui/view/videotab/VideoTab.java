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
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.collections.CollectionUtils;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.packetanalysis.IVideoUsageAnalysis;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
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
import com.att.aro.ui.view.video.VideoUtil;
import com.att.aro.view.images.Images;

public class VideoTab extends TabPanelJScrollPane implements IAROPrintable{

	private static final long serialVersionUID = 1L;
	AROModelObserver bpObservable;
	private JPanel container;
	private JPanel mainPanel;
	private IARODiagnosticsOverviewRoute overviewRoute = null;
	private MainFrame aroView;
	private IVideoUsageAnalysis videoUsageAnalysis = ContextAware.getAROConfigContext().getBean(IVideoUsageAnalysis.class);
	private VideoUsagePrefs videoUsagePrefs;
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
	private AmvotsPanel amvotsPane;
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
	
	public void addGraphPanel(){
		if(mainPanel != null){
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
	
	private boolean isVideoAvailable(AbstractTraceResult traceResult) {
		String traceDirectory = traceResult.getTraceDirectory();	
		TraceResultType traceResultType = traceResult.getTraceResultType();
		if (traceResultType == TraceResultType.TRACE_FILE
				|| !(VideoUtil.mp4VideoExists(traceDirectory) || VideoUtil.movVideoExists(traceDirectory))) {
			return false;
		}else{
			return true;
		}
	}

	public void openStartUpDelayWarningDialog(String tracePath) {
		if(tracePath == null) {
			return;
		}
		File trace = new File(tracePath);
		if (trace.exists() && trace.isDirectory() && VideoUtil.mp4VideoExists(tracePath)) {
			openStartUpDelayWarningDialog();
		}
	}
	
	private void openStartUpDelayWarningDialog() {
		boolean startUpReminder = true;
		boolean videoAnalyzed = CollectionUtils.containsAny(SettingsUtil.retrieveBestPractices(),
				BestPracticeType.getByCategory(Category.VIDEO));
		boolean startupDelayAnalyzed = SettingsUtil.retrieveBestPractices().contains(BestPracticeType.STARTUP_DELAY);
		if (videoAnalyzed && startupDelayAnalyzed && isStartUpReminderRequired() && aroView != null
				&& aroView.getCurrentTabComponent() == aroView.getVideoTab()) {
			if(null == startUpDelayWarningDialog){
				startUpDelayWarningDialog = new StartUpDelayWarningDialog(aroView, overviewRoute);
			}else if(null != videoUsageAnalysis){
				videoUsageAnalysis.loadPrefs();
				startUpReminder = videoUsageAnalysis.getVideoUsagePrefs().isStartupDelayReminder();
			}
			startUpDelayWarningDialog.setDialogInfo(warningMessage, startUpReminder);
			startUpDelayWarningDialog.setVisible(true);
			startUpDelayWarningDialog.setAlwaysOnTop(true);
		}
	}
	
	private boolean isStartUpReminderRequired(){
		boolean preferenceStartUpReminder = false;
		boolean startupDelaySet = false;
		int manifestsSelected = 0;
		boolean moreManifestsSelected = false;
		boolean result = true;
		
		if (null != videoUsageAnalysis) {
			videoUsageAnalysis.loadPrefs();
			videoUsagePrefs = videoUsageAnalysis.getVideoUsagePrefs();
			preferenceStartUpReminder = videoUsagePrefs.isStartupDelayReminder();

			if (videoUsageAnalysis.getVideoUsage() != null && CollectionUtils.isNotEmpty(videoUsageAnalysis.getVideoUsage().getManifests())) {
				for (AROManifest aroManifest : videoUsageAnalysis.getVideoUsage().getManifests()) {
					if (aroManifest != null && true == aroManifest.isSelected()) {
						// if any of the manifest file is selected
						manifestsSelected = manifestsSelected + 1;
						// User updated the startup delay
						if (aroManifest.getDelay() > 0 && false == startupDelaySet) {
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

	private JPanel getUploadAnalysisPane() {
		amvotsPane = new AmvotsPanel();
		localRefreshList.add(amvotsPane);
		return amvotsPane;
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

		inset = new Insets(10, 40, 10, 1);
		JPanel upload = getUploadAnalysisPane();
		summaryPane.add(upload, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
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
			amvotsPane.refresh(analyzerResult);
			updateUI();
		}
		if (isVideoAvailable(result)) {
			openStartUpDelayWarningDialog();
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
