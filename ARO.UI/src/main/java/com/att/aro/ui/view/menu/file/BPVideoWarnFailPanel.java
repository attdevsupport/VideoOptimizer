/*
 *  Copyright 2018 AT&T
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
package com.att.aro.ui.view.menu.file;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.ui.commonui.ContextAware;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This dialog provides options to configure that the video best practices
 * should be gauged against
 *
 * @author Dinesh
 *
 */
public class BPVideoWarnFailPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTable table;
	private ObjectMapper mapper;
	private PreferenceHandlerImpl prefs;
	private VideoUsagePrefs videoUsagePrefs;
	public static final Logger LOGGER = Logger.getLogger(BPVideoWarnFailPanel.class.getName());
	private JTextField compileResultsField = new JTextField();
	String sError = "";
	private VideoPreferenceTableModel model;
	private JPanel videoPreferenceTab;
	private VideoAnalysisDialog videoAnalysisPane;

	public BPVideoWarnFailPanel() {
		JPanel mainPanel = new JPanel();
		this.add(mainPanel);
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints constraint = new GridBagConstraints();
		mainPanel.add(getGridPanel(), constraint);
		mainPanel.add(getDefaultButton("Default", (ActionEvent arg) -> setDefault()), constraint);
		compileResultsField.setEditable(false);
		if (sError.isEmpty()) {
			compileResultsField.setBackground(mainPanel.getBackground());
			compileResultsField.setForeground(Color.red);
			compileResultsField.setFont(compileResultsField.getFont().deriveFont(Font.BOLD));
			compileResultsField.setText("");
			compileResultsField.setVisible(false);
		} else {
			compileResultsField.setVisible(true);
			compileResultsField.setForeground(Color.red);
			compileResultsField.setText(String.format("ERRORS: %s", sError));
		}
		constraint.anchor = GridBagConstraints.FIRST_LINE_START;
		constraint.gridy = 300;
		constraint.gridwidth = 2;
		mainPanel.add(compileResultsField, constraint);
		mainPanel.updateUI();
	}

	public String getSbError() {
		return sError;
	}

	private void setDefault() {
		model.setDefault();
		compileResultsField.setVisible(false);
	}

	private JButton getDefaultButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}

	private Component getGridPanel() {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(getTable());
		int height = 70;
		if (Util.isWindowsOS()||Util.isLinuxOS()) {
			height = 78;
		}
		scrollPane.setPreferredSize(new Dimension(550, height));	
		scrollPane.revalidate();
		return scrollPane;
	}

	private JTable getTable() {
		if (table == null) {
			model = new VideoPreferenceTableModel(loadPrefs());
			table = new JTable(model);
			table.setGridColor(Color.LIGHT_GRAY);
			table.setFocusable(false);
			table.setRowSelectionAllowed(false);
			table.setShowGrid(true);
			table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
			table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
			table.getColumnModel().getColumn(0).setPreferredWidth(225);
			table.getColumnModel().getColumn(1).setPreferredWidth(50);
			table.getColumnModel().getColumn(2).setPreferredWidth(50);
			table.getModel().addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent e) {
					sError = model.getValidationError();
					if (!sError.toString().isEmpty()) {
						compileResultsField.setVisible(true);
						compileResultsField.setForeground(Color.red);
						compileResultsField.setText(String.format("ERROR : %s", sError.toString()));
					} else {
						compileResultsField.setText("");
						compileResultsField.setVisible(false);
					}
				}
			});
		} else {
			model.setData(loadPrefs());
			table.setModel(model);
		}
		return table;
	}

	private List<VideoPreferenceInfo> loadPrefs() {
		mapper = new ObjectMapper();
		prefs = PreferenceHandlerImpl.getInstance();
		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null && !temp.isEmpty() && !temp.contains("null")) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			} catch (IOException e) {
				LOGGER.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
			}
		} else {
			try {
				videoUsagePrefs = ContextAware.getAROConfigContext().getBean("videoUsagePrefs",VideoUsagePrefs.class);// new VideoUsagePrefs();
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				LOGGER.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
			}
		}
		List<VideoPreferenceInfo> videoPreferenceList = new ArrayList<VideoPreferenceInfo>();
		if(checkModelData(videoUsagePrefs)){
			VideoPreferenceTableModel model = new VideoPreferenceTableModel();
			return model.getDefaultValues();
		  }
		VideoPreferenceInfo vp = new VideoPreferenceInfo("Startup Delay (seconds)",
				videoUsagePrefs.getStartUpDelayWarnVal(), videoUsagePrefs.getStartUpDelayFailVal());
		videoPreferenceList.add(vp);
		vp = new VideoPreferenceInfo("Stall Duration (seconds)", videoUsagePrefs.getStallDurationWarnVal(),
				videoUsagePrefs.getStallDurationFailVal());
		videoPreferenceList.add(vp);
		vp = new VideoPreferenceInfo("Segment Redundancy (%)", videoUsagePrefs.getSegmentRedundancyWarnVal(),
				videoUsagePrefs.getSegmentRedundancyFailVal());
		videoPreferenceList.add(vp);
		return videoPreferenceList;
	}

	public boolean checkModelData(VideoUsagePrefs videoUsagePrefs) {
		if (videoUsagePrefs.getStartUpDelayWarnVal() == null || videoUsagePrefs.getStartUpDelayFailVal() == null
				|| videoUsagePrefs.getStallDurationWarnVal() == null
				|| videoUsagePrefs.getStallDurationFailVal() == null
				|| (videoUsagePrefs.getSegmentRedundancyWarnVal() == 0
						&& videoUsagePrefs.getSegmentRedundancyFailVal() == 0)) {
			return true;

		} else {
			return false;
		}
	}
	
	public void saveWarnFail() {
		PreferenceHandlerImpl prefs = PreferenceHandlerImpl.getInstance();
		if (model != null) {
			if (StringUtils.isNotBlank(model.getValidationError().toString())) {
				throw new IllegalArgumentException(model.getValidationError().toString());
			}
			if (model.getVideoUsagePrefs() != null) {
				String temp = "";
				try {
					temp = mapper.writeValueAsString(model.getVideoUsagePrefs());
					if (temp != null && !temp.equals("null")) {
						prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
					} else {
						LOGGER.error("Saving Video Preference failed : model data was null");
					}
				} catch (IOException e) {
					LOGGER.error("Saving Video Preference failed :" + e.getMessage());
				}
			}
		}
		if (getVideoAnalysisTab().saveVideoAnalysisConfiguration()) {
			throw new IllegalArgumentException(model.getValidationError().toString());
		}
	}

	public JPanel getVideoPreferenceTab() {
		if (videoPreferenceTab == null) {
			videoPreferenceTab = new JPanel();
			videoPreferenceTab.setPreferredSize(new Dimension(700, 400));
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this, getVideoAnalysisTab());
			splitPane.setPreferredSize(videoPreferenceTab.getPreferredSize());
			splitPane.setDividerLocation(150);
			splitPane.setEnabled(false);
			videoPreferenceTab.add(splitPane);
		}
		return videoPreferenceTab;
	}

	public VideoAnalysisDialog getVideoAnalysisTab() {
		if (videoAnalysisPane == null) {
			videoAnalysisPane = new VideoAnalysisDialog();
		}
		return videoAnalysisPane;
	}
}
