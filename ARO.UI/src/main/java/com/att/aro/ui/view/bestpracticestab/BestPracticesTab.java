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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.IAROPrintable;
import com.att.aro.ui.commonui.ImagePanel;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.commonui.UIComponent;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.AROModelObserver;
import com.att.aro.ui.view.MainFrame;

/**
 * 
 */
public class BestPracticesTab extends TabPanelJScrollPane implements IAROPrintable {

	private static final long serialVersionUID = 1L;

	AROModelObserver bpObservable;

	private JPanel container;
	private JPanel mainPanel;
	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;
	private MainFrame aroView;
	private DateTraceAppDetailPanel dateTraceAppDetailPanel;

	Insets insets = new Insets(10, 1, 10, 1);

	private BpDetailResultsPanel bpDetailResultsPanel;

	private Map<String, Point> titleToLocation;

	/**
	 * Create the panel.
	 */
	public BestPracticesTab(MainFrame aroView, IARODiagnosticsOverviewRoute diagnosticsOverviewRoute) {
		super();
		this.aroView = aroView;
		this.diagnosticsOverviewRoute = diagnosticsOverviewRoute;

		bpObservable = new AROModelObserver();

		container = new JPanel(new BorderLayout());

		String headerTitle = ApplicationConfig.getInstance().getAppBrandName() + " "
				+ MessageFormat.format(ResourceBundleHelper.getMessageString("bestPractices.header.result"),
						ApplicationConfig.getInstance().getAppCombinedName());

		container.add(UIComponent.getInstance().getLogoHeader(headerTitle), BorderLayout.NORTH);

		ImagePanel panel = new ImagePanel(null);
		panel.setLayout(new GridBagLayout());

		panel.add(layoutDataPanel(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
		container.add(panel, BorderLayout.CENTER);
		setViewportView(container);
		getVerticalScrollBar().setUnitIncrement(10);
		getHorizontalScrollBar().setUnitIncrement(10);

		titleToLocation = new HashMap<String, Point>();

	}

	@Override
	public JPanel layoutDataPanel() {
		if (mainPanel == null) {

			mainPanel = new JPanel(new GridBagLayout());
			// mainPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			mainPanel.setOpaque(false);

			int section = 1;

			mainPanel.add(buildTopGroup(), new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

			// BP Detail -aka- AROBpDetailedResultPanel
			bpDetailResultsPanel = new BpDetailResultsPanel(aroView, diagnosticsOverviewRoute);
			mainPanel.add(bpDetailResultsPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
			bpObservable.registerObserver(bpDetailResultsPanel);

		}
		return mainPanel;
	}

	/**
	 * 
	 */
	private JPanel buildTopGroup() {

		JPanel topPanel = new JPanel(new GridBagLayout());

		topPanel.setOpaque(false);
		topPanel.setBorder(new RoundedBorder(new Insets(20, 20, 20, 20), Color.WHITE));

		JSplitPane summaryPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		int section = 0;

		summaryPanel.setOpaque(false);
		summaryPanel.setBorder(new RoundedBorder(new Insets(20, 20, 20, 20), Color.WHITE));

		dateTraceAppDetailPanel = new DateTraceAppDetailPanel();
		bpObservable.registerObserver(dateTraceAppDetailPanel);
		dateTraceAppDetailPanel.setPreferredSize(getPreferredSize());

		// BP Overall -aka- AROBpOverallResulsPanel
		BpTestStatisticsPanel testStatisticsPanel = new BpTestStatisticsPanel();
		bpObservable.registerObserver(testStatisticsPanel);

		TraceNotesPanel traceNotesPanel = new TraceNotesPanel(aroView);
		traceNotesPanel.setMinimumSize(traceNotesPanel.getPreferredSize());
		bpObservable.registerObserver(traceNotesPanel);

		TraceInfoPanel traceInfoPanel = new TraceInfoPanel();
		traceInfoPanel.setMinimumSize(traceInfoPanel.getPreferredSize());
		bpObservable.registerObserver(traceInfoPanel);

		JSplitPane traceNotesSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, traceInfoPanel, traceNotesPanel);
		traceNotesSplitPane.setResizeWeight(0.7);
		traceNotesSplitPane.setDividerSize(0);
		traceNotesSplitPane.setOpaque(false);
		traceNotesSplitPane.setBorder(null);

		JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dateTraceAppDetailPanel, testStatisticsPanel);
		topSplitPane.setResizeWeight(0.7);
		// topSplitPane.setDividerSize(0);
		topSplitPane.setOpaque(false);
		topSplitPane.setBorder(null);

		JSplitPane leftSplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, traceNotesSplitPane);
		leftSplitPanel.setResizeWeight(0);
		leftSplitPanel.setDividerSize(0);
		leftSplitPanel.setOpaque(false);
		leftSplitPanel.setBorder(null);

		MetadataPanel metadataPanel = new MetadataPanel(aroView);
		metadataPanel.setMinimumSize(metadataPanel.getPreferredSize());
		bpObservable.registerObserver(metadataPanel);

		DeviceInfoPanel deviceInfoPanel = new DeviceInfoPanel(aroView);
		deviceInfoPanel.setMinimumSize(deviceInfoPanel.getPreferredSize());
		bpObservable.registerObserver(deviceInfoPanel);

		JSplitPane rightSplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, deviceInfoPanel, metadataPanel);
		rightSplitPanel.setResizeWeight(0);
		rightSplitPanel.setDividerSize(0);
		rightSplitPanel.setOpaque(false);
		rightSplitPanel.setBorder(null);

		summaryPanel.add(leftSplitPanel);
		summaryPanel.add(rightSplitPanel);
		summaryPanel.setResizeWeight(0);

		topPanel.add(summaryPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
		
		// Separator
		topPanel.add(UIComponent.getInstance().getSeparator(), new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 10, 0), 0, 0));

		// BP Overall -aka- AROBpOverallResulsPanel
		BpTestsConductedPanel bpTestsConductedPanel = new BpTestsConductedPanel(this);
		topPanel.add(bpTestsConductedPanel, new GridBagConstraints(0, section++ // int gridx, int gridy
				, 1, 1 // , int gridwidth, int gridheight
				, 0.0, 0.0 // , double weightx, double weighty
				, GridBagConstraints.CENTER // , int anchor
				, GridBagConstraints.HORIZONTAL // , int fill
				, new Insets(0, 0, 0, 0) // , Insets insets
				, 0, 0)); // , int ipadx, int ipady
		bpObservable.registerObserver(bpTestsConductedPanel);

		return topPanel;
	}

	/**
	 * Refreshes the AROBestPracticesTab using the specified trace analysis data.
	 * This method is typically called when a new trace file is loaded.
	 * 
	 * @param analysisData The trace analysis data.
	 */
	@Override
	public void refresh(AROTraceData aModel) {
		bpObservable.refreshModel(aModel);
	}

	@Override
	public synchronized void addComponentListener(ComponentListener listener) {
		super.addComponentListener(listener);
	}

	/**
	 * Triggers and expansion of any tableViews that need expanding before returning the container.
	 * 
	 * @return a JPanel prepared for printing everything
	 */
	@Override
	public JPanel getPrintablePanel() {
		bpDetailResultsPanel.expand();
		return container;
	}

	@Override
	public void setScrollLocationMap() {
		int startY = getStartPointY(); // start point
		int screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

		ArrayList<BpDetail> sectionList = bpDetailResultsPanel.getExpandableList();

		for (BpDetail section : sectionList) {
			ArrayList<BpDetailItem> itemList = section.getExpandableList();

			int headerHeight = (int) (section.getHeader().getBounds().getHeight()
					+ section.getDateTraceAppDetailPanel().getBounds().getHeight());
			startY += headerHeight + 10; // Insets from BestPracticesTab, for the top of each section

			for (BpDetailItem item : itemList) {
				Point p = new Point(0, startY + screenHeight);
				titleToLocation.put(item.getNameTextLabel().getText(), p);

				startY += item.getBounds().getHeight() + 2; // Insets from BpDetail panel
			}

			startY += 10; // Insets from BestPracticesTab, for the bottom of each section
		}
	}

	private int getStartPointY() {
		return (int) bpDetailResultsPanel.getLocation().getY();
	}

	public Map<String, Point> getTitleToLocation() {
		return titleToLocation;
	}

	public DateTraceAppDetailPanel getDateTraceAppDetailPanel() {
		return dateTraceAppDetailPanel;
	}

}
