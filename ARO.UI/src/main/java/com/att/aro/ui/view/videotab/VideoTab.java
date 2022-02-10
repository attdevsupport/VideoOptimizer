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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.IAROPrintable;
import com.att.aro.ui.commonui.ImagePanel;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.AROModelObserver;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.bestpracticestab.BpHeaderPanel;
import com.att.aro.view.images.Images;

import lombok.Getter;

@Getter
public class VideoTab extends TabPanelJScrollPane implements IAROPrintable{
	
	private static final long serialVersionUID = 1L;
	AROModelObserver bpObservable;
	private JPanel container;
	private JPanel mainPanel;
	private IARODiagnosticsOverviewRoute overviewRoute = null;
	private MainFrame aroView;
	private Insets headInsets = new Insets(10, 1, 0, 1);
	private Insets noInsets = new Insets(0, 0, 0, 0);
	private ArrayList<TabPanelJPanel> localRefreshList = new ArrayList<>();
	private VideoManifestPanel videoManifestPanel;
	private LoadManifestDialog loadManifestDialog;
	private VideoTabProfilePanel videoTabProfilePanel;
	
	private String trace = "";
	private long lastOpenedTrace;
	private SegmentThroughputGraphPanel throughputGraphPanel;
	private SegmentProgressGraphPanel progressGraphPanel;
	private SegmentBufferGraphPanel bufferGraphPanel;
	private JPanel progressPanel;
	private JPanel throughputPanel;
	private JPanel bufferPanel;
	
	/**
	 * Create the panel.
	 */
	public VideoTab(MainFrame aroView, IARODiagnosticsOverviewRoute overviewRoute) {
		super();
		this.aroView = aroView;
		this.overviewRoute = overviewRoute;

		bpObservable = new AROModelObserver();

		container = new JPanel(new BorderLayout());
		
		// Summaries, Manifest, Requests
		ImagePanel panel = new ImagePanel(null);
		panel.setLayout(new GridBagLayout());
		panel.add(layoutDataPanel(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
				, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL
				, new Insets(10, 10, 0, 10)
				, 0, 0));
		container.add(panel, BorderLayout.NORTH);
		
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

			mainPanel.add(getProfilePanel().layoutDataPanel(), new GridBagConstraints(
					0, section++
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(0, 0, 0, 0)
					, 0, 0));

			mainPanel.add(getThroughputPanel(), new GridBagConstraints(
					0, section++
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(10, 0, 0, 0)
					, 0, 0));
			
			mainPanel.add(getProgressPanel(), new GridBagConstraints(
					0, section++
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(10, 0, 0, 0)
					, 0, 0));
			
			mainPanel.add(getBufferPanel(), new GridBagConstraints(
					0, section++
					, 1, 1
					, 1.0, 0.0
					, GridBagConstraints.EAST
					, GridBagConstraints.HORIZONTAL
					, new Insets(10, 0, 0, 0)
					, 0, 0));
			
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
	
	public JPanel getThroughputPanel() {
		if (throughputPanel == null) {
			JPanel segmentThroughputGraphPanel = new JPanel(new GridBagLayout());
			int section = 0;
			throughputGraphPanel = new SegmentThroughputGraphPanel(aroView);
			throughputGraphPanel.setZoomFactor(2);
			throughputGraphPanel.setMaxZoom(4);
			segmentThroughputGraphPanel.add(throughputGraphPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));

			throughputPanel = getTitledWrapper("video.tab.throughputGraph.title");
			throughputPanel.add(segmentThroughputGraphPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
					GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));
			throughputPanel.setVisible(false);
			throughputPanel.addMouseListener(mouseListener(segmentThroughputGraphPanel));
		}
		return throughputPanel;
	}

	private MouseListener mouseListener(JPanel panel) {
		return new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				  if(e.getClickCount()==2){
					  panel.setVisible(!panel.isVisible());
			        }
				
			}
		};
	}
	
	public JPanel getProgressPanel() {
		if (progressPanel == null) {
			JPanel segmentProgressGraphPanel = new JPanel(new GridBagLayout());
			int section = 0;
			progressGraphPanel = new SegmentProgressGraphPanel(aroView);
			progressGraphPanel.setZoomFactor(2);
			progressGraphPanel.setMaxZoom(4);
			segmentProgressGraphPanel.add(progressGraphPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));

			progressPanel = getTitledWrapper("video.tab.progressGraph.title");
			progressPanel.add(segmentProgressGraphPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
					GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));
			progressPanel.setVisible(false);
			progressPanel.addMouseListener(mouseListener(segmentProgressGraphPanel));
		}
		return progressPanel;
	}
	
	/**
	 * Video Stream Playtime Buffer Display
	 * @return
	 */
	public JPanel getBufferPanel() {
		if (bufferPanel == null) {
			JPanel segmentProgressGraphPanel = new JPanel(new GridBagLayout());
			int section = 0;
			bufferGraphPanel = new SegmentBufferGraphPanel(aroView);
			bufferGraphPanel.setZoomFactor(2);
			bufferGraphPanel.setMaxZoom(4);
			segmentProgressGraphPanel.add(bufferGraphPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));

			bufferPanel = getTitledWrapper("video.tab.bufferGraph.title");
			bufferPanel.add(segmentProgressGraphPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
					GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));
			bufferPanel.setVisible(false);
			bufferPanel.addMouseListener(mouseListener(segmentProgressGraphPanel));
		}
		return bufferPanel;
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
		loadManifestDialog = new LoadManifestDialog(aroView);
        JPanel wrapper = getTitledWrapper("video.tab.manifest.title", new ExportManifestDialog(videoManifestPanel), loadManifestDialog);
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
		
		VideoRequestPanel requestPanel = new VideoRequestPanel(aroView);
		pane.add(requestPanel, new GridBagConstraints(0, section++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));
		bpObservable.registerObserver(requestPanel);
		
		JPanel wrapper = getTitledWrapper("video.tab.request.title");
		wrapper.add(pane, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, noInsets, 0, 0));
		return wrapper;
	}
	
	private JPanel getTitledWrapper(String title, JComponent... components) {

		JPanel pane = new JPanel(new GridBagLayout());

		pane.setOpaque(false);
		pane.setBorder(new RoundedBorder(new Insets(0, 10, 10, 10), Color.WHITE));

		JPanel fullPanel = new JPanel(new BorderLayout());

		fullPanel.setOpaque(false);

		// Create the header bar
		BpHeaderPanel header = new BpHeaderPanel(ResourceBundleHelper.getMessageString(title));
		header.setImageTitle(Images.BLUE_HEADER.getImage(), null);
		// Add buttons to title bar
		JPanel buttonPanel = null;
		if (components.length > 0) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			for (JComponent component : components) {
				if (ExportManifestDialog.class.getName().equals(component.getName())) {
					buttonPanel.add(components[0], BorderLayout.WEST);
				}

				if (LoadManifestDialog.class.getName().equals(component.getName())) {
					buttonPanel.add(components[1], BorderLayout.EAST);
				}
			}

			if (buttonPanel != null && buttonPanel.getComponentCount() != 0) {
				header.add(buttonPanel, BorderLayout.EAST);
			}
		}
		fullPanel.add(header, BorderLayout.NORTH);
		pane.add(fullPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, headInsets, 0, 0));
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
	public void refresh(AROTraceData traceData) {
		AbstractTraceResult result = traceData.getAnalyzerResult().getTraceresult();
		long newTraceTime = ((MainFrame)aroView).getLastOpenedTime();
		getProfilePanel().refresh(traceData);
		loadManifestDialog.refresh(traceData);
		if (lastOpenedTrace == newTraceTime &&
				(trace.equals(result.getTraceDirectory()) || trace.equals(result.getTraceFile()))) {
			refreshLocal(traceData, loadManifestDialog.getAnalysisfilter().isCSI());
		} else {
			trace = result.getTraceDirectory() != null ? result.getTraceDirectory() : result.getTraceFile();
			lastOpenedTrace = newTraceTime;
			bpObservable.refreshModel(traceData);
			updateUI();
		}
	}
	
	/**
	 * Initializes and returns the profile panel.
	 */
	public VideoTabProfilePanel getProfilePanel() {
		if (videoTabProfilePanel == null) {
			videoTabProfilePanel = new VideoTabProfilePanel();
		}
		return videoTabProfilePanel;
	}
	
	public void refreshLocal(AROTraceData traceData, boolean isCSI) {
		
		for (TabPanelJPanel container : localRefreshList) {
			container.refresh(traceData);
		}
		if (isCSI) {
			videoManifestPanel.refresh(traceData);
		} else {
			videoManifestPanel.refreshLocal(traceData);
		}
		this.updateUI();
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