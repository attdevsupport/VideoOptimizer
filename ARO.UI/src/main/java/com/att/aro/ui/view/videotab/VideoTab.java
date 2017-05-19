
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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.UIManager;

import org.springframework.util.FileCopyUtils;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoTabHelper;
import com.att.aro.core.videoanalysis.impl.VideoTabHelperImpl;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.commonui.UIComponent;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;


public class VideoTab extends TabPanelJScrollPane implements ActionListener {

	private JScrollPane scrollPanel;
	private JViewport viewParent;
	private JPanel videoTabPanel;
	private List<AccordionComponent> accordionList;
	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;
	private AROTraceData trace;
	private JPanel mainPanel;
	private JPanel extManifestPanel;
	private JButton downloadBtn;
	private JButton loadBtn;
	private IAROView aroView;
	private VideoTabHelperImpl videoTabHelper = (VideoTabHelperImpl) ContextAware.getAROConfigContext().getBean("videoTabHelperImpl", IVideoTabHelper.class);

	public VideoTab(IAROView aroview, IARODiagnosticsOverviewRoute route) {
		super();
		this.aroView = aroview;
		this.diagnosticsOverviewRoute = route;
		this.aroView = aroview;
		accordionList = new ArrayList<>();
		videoTabPanel = new JPanel(new BorderLayout());
		videoTabPanel.setBackground(Color.white);

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.insets = new Insets(50, 0, 0, 20);

		layoutDataPanel();

		// add the header VO
		String headerTitle = MessageFormat.format(ResourceBundleHelper.getMessageString("videoTab.title"),
				ApplicationConfig.getInstance().getAppBrandName(), ApplicationConfig.getInstance().getAppShortName());

		videoTabPanel.add(UIComponent.getInstance().getLogoHeader(headerTitle), BorderLayout.NORTH);
		videoTabPanel.add(scrollPanel, BorderLayout.CENTER);

		this.setViewportView(videoTabPanel);
		this.getVerticalScrollBar().setUnitIncrement(10);
	}

	// new panel added to video tab
	private JPanel getExternalManifestPanel() {
		if (extManifestPanel == null) {
			extManifestPanel = new JPanel(new FlowLayout());
			extManifestPanel.setBorder(BorderFactory.createTitledBorder("External Manifest"));

			loadBtn = new JButton("Load");
			downloadBtn = new JButton("Download");
			loadBtn.setName("Load");
			downloadBtn.setName("Download");

			loadBtn.addActionListener(this);
			downloadBtn.addActionListener(this);

			extManifestPanel.add(loadBtn);
			// extManifestPanel.add(downloadBtn);
		}
		return extManifestPanel;
	}

	@Override
	public JPanel layoutDataPanel() {
		JPanel panel = getMainPanel();

		// add the scrollable panel here
		scrollPanel = getJScrollPane(); // new JScrollPane();
		scrollPanel.setViewportView(getViewParentScrollPane());

		setVisible(true);
		revalidate();
		repaint();
		return panel;
	}

	private JPanel getMainPanel() {
		if (mainPanel == null) {
			mainPanel = new JPanel(new BorderLayout());
			mainPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			mainPanel.setOpaque(false);
			mainPanel.setBorder(new RoundedBorder(new Insets(10, 10, 10, 10), Color.WHITE));

		}
		return mainPanel;
	}

	private JScrollPane getJScrollPane() {
		if (scrollPanel == null) {
			scrollPanel = new JScrollPane();
			scrollPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			scrollPanel.setBorder(new RoundedBorder(new Insets(20, 20, 20, 20), Color.black));

		}
		return scrollPanel;
	}

	private JViewport getViewParentScrollPane() {
		if (viewParent == null) {
			viewParent = new JViewport();
		}
		viewParent.setView(getTablePanel());
		return viewParent;
	}

	private JPanel getTablePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		int gridy = 0;
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 0;
		constraint.gridy = gridy;
		constraint.insets = new Insets(0, 10, 25, 30);
		constraint.anchor = GridBagConstraints.FIRST_LINE_START;
		constraint.weightx = 1;

		if (trace != null) {
			// go through the video events manifest file to create the number of
			// collapsible/expandable componnents
			accordionList.clear();

			if (trace.getAnalyzerResult().getVideoUsage() != null) {
				for (AROManifest aroManifest : trace.getAnalyzerResult().getVideoUsage().getManifests()) {
					if (!aroManifest.getVideoEventList().isEmpty()) {
						accordionList.add(new AccordionComponent(aroManifest, this.diagnosticsOverviewRoute));
					}
				}
			}
			JLabel subTitleLabel = new JLabel(ResourceBundleHelper.getMessageString("video.tab.title"));
			subTitleLabel.setFont(AROUIManager.HEADER_FONT);
			panel.add(subTitleLabel, constraint);

			// external manifest downloading and uploading interface
			gridy = gridy + 1;
			constraint.gridy = gridy;
			constraint.fill = GridBagConstraints.HORIZONTAL;
			constraint.gridx = 0;
			constraint.insets = new Insets(0, 5, 15, 750);
			constraint.anchor = GridBagConstraints.FIRST_LINE_START;
			constraint.weightx = 1;
			panel.add(getExternalManifestPanel(), constraint);

			// external manifest downloading and uploading interface
			gridy = gridy + 1;
			constraint.gridy = gridy;
			constraint.fill = GridBagConstraints.HORIZONTAL;
			constraint.gridx = 0;
			constraint.insets = new Insets(0, 5, 15, 750);
			constraint.anchor = GridBagConstraints.FIRST_LINE_START;
			constraint.weightx = 1;
			panel.add(getExternalManifestPanel(), constraint);

			// Movie Manifests
			gridy = gridy + 1;
			constraint.gridy = gridy;
			constraint.insets = new Insets(0, 0, 15, 20);
			for (AccordionComponent accordion : accordionList) {
				panel.add(accordion, constraint);
				gridy = gridy + 1;
				constraint = new GridBagConstraints();
				constraint.fill = GridBagConstraints.HORIZONTAL;
				constraint.gridx = 0;
				constraint.gridy = gridy;
				constraint.insets = new Insets(0, 0, 15, 20);
				constraint.anchor = GridBagConstraints.FIRST_LINE_START;
				constraint.weightx = 1;
			}
			//constraint.weighty = 1;
			//panel.add(new JPanel(), constraint);
			
			// Video Requests
			constraint.gridy = gridy;
			constraint.fill = GridBagConstraints.HORIZONTAL;
			constraint.gridx = 0;
			constraint.insets = new Insets(5, 5, 15, 20);
			constraint.anchor = GridBagConstraints.FIRST_LINE_START;
			constraint.weightx = 1;
			constraint.weighty = 1;
	
			panel.add(new AccordionComponent(false, aroView), constraint);

		}

		panel.validate();
		panel.repaint();
		return panel;
	}

	@Override
	public void refresh(AROTraceData analyzerResult) {
		trace = analyzerResult;
		// populateData
		accordionList = new ArrayList<>();
		scrollPanel.setViewportView(getViewParentScrollPane());

	}

	@Override
	public void setScrollLocationMap() {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton btn = (JButton) e.getSource();
		if (btn.getName().equals("Load")) {

			// Open filechooser
			JFileChooser fileChooser = new JFileChooser(this.aroView.getTracePath());
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {

				// save selected file/files inside downloads folder
				fileChooser.getSelectedFile().getPath();
				String downloadsPath = this.aroView.getTracePath() + Util.FILE_SEPARATOR 
						+ "downloads" + Util.FILE_SEPARATOR + fileChooser.getSelectedFile().getName();
				try {
					FileCopyUtils.copy(fileChooser.getSelectedFile(), new File(downloadsPath));
				} catch (IOException e1) {

				}
				// refresh analyzer
				this.aroView.updateTracePath(new File(this.aroView.getTracePath()));
			}
		}

	}
	

}
