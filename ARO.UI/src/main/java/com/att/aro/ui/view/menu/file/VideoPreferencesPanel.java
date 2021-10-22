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
package com.att.aro.ui.view.menu.file;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.utils.NumericInputVerifier;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VideoPreferencesPanel extends JPanel {

	private static final Logger LOG = LogManager.getLogger(VideoPreferencesPanel.class.getName());

	private static final long serialVersionUID = 1L;
	private static final int MAX_BUFFER = 5000;
	private static final int MAX_STALLTRIGGERTIME = 10;
	private static final int MAX_STALLRECOVERY = 10;
	private static final int MAX_TARGETEDSTARTUPDELAY = 10;
	private static final float MAX_NEARSTALL = 0.5f;
	private static final int MAX_STARTUPDELAY = 50;
	private static final int MAX_STALLDURATION = 10;
	private static final int MAX_REDUNDANCY = 100;

	private JPanel jDialogPanel;
	private VideoUsagePrefs videoUsagePrefs;
	private ObjectMapper mapper;
	private PreferenceHandlerImpl prefs;
	private JTextField stallTriggerTimeEdit;
	private JTextField maxBufferEdit;
	private JTextField stallPausePointEdit;
	private JTextField stallRecoveryEdit;
	private JTextField targetedStartupDelayEdit;
	private JCheckBox startupDelayReminder;
	private JComboBox<DUPLICATE_HANDLING> duplicateHandlingEditCombo;
	private JTextField nearStallEdit;
	private JTextField startupDelayEdit;
	private JTextField stallDurationWarnEdit;
	private JTextField stallDurationFailEdit;
	private JTextField segRedundancyWarnEdit;
	private JTextField segRedundancyFailEdit;
	private int idx;
	private VideoPreferenceModel videoPreferenceModel;
	private NumericInputVerifier inputVerifier;

	private JTextField errorField;

	private PreferencesDialog preferencesDialog;

	private void loadVideoUsagePreferences() {
		mapper = new ObjectMapper();
		prefs = PreferenceHandlerImpl.getInstance();

		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null && !temp.equals("null")) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
				videoPreferenceModel = new VideoPreferenceModel();
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage(), e);
			}
		} else {
			try {
				videoUsagePrefs = ContextAware.getAROConfigContext().getBean("videoUsagePrefs", VideoUsagePrefs.class); // new
																														// VideoUsagePrefs();
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to serialize :" + e.getMessage(), e);
			}
		}
	}

	public VideoPreferencesPanel(PreferencesDialog preferencesDialog) {
		idx = 0;
		this.preferencesDialog= preferencesDialog;

		loadVideoUsagePreferences();

		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setPreferredSize(new Dimension(700, 400));
			jDialogPanel.setAlignmentX(CENTER_ALIGNMENT);
			jDialogPanel.setLayout(new GridBagLayout());

			jDialogPanel.add(getWarnFailPanel(),
					getGridBagConstraints(1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 0));
			jDialogPanel.add(new JPanel(),
					getGridBagConstraints(1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 1));

			jDialogPanel.add(getVideoPrefencesPanel(),
					getGridBagConstraints(1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 2));

		}

		this.setLayout(new FlowLayout(FlowLayout.CENTER));
		this.add(jDialogPanel);

	}


	public GridBagConstraints getGridBagConstraints(int column, double weightx, double weighty, int anchor, int fill) {
		GridBagConstraints gbc = new GridBagConstraints(column, idx, 1, 1, weightx, weighty, anchor, fill,
				new Insets(0, 0, 0, 0), 5, 0);

		return gbc;
	}

	public GridBagConstraints getGridBagConstraints(double weightx, double weighty, int anchor, int fill, int index) {
		GridBagConstraints gbc = new GridBagConstraints(0, index, 1, 1, weightx, weighty, anchor, fill,
				new Insets(0, 0, 0, 0), 5, 0);

		return gbc;
	}

	private Component getWarnFailPanel() {

		Label startupDelayLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.startupDelay"));
		Label stallDurationLabel = new Label(ResourceBundleHelper.getMessageString("videoStall.table.col3"));
		Label segRedundancyLabel = new Label(ResourceBundleHelper.getMessageString("segmentRedundancy.title"));

		startupDelayEdit = new JTextField(String.format("%.3f",
				Double.valueOf(videoUsagePrefs.getStartUpDelayWarnVal() == 0.0 ?
						Double.valueOf( ResourceBundleHelper.getMessageString("preferences.video.defaultStartUpDelayWarnVal"))
						: videoUsagePrefs.getStartUpDelayWarnVal())), 5);

		stallDurationWarnEdit = new JTextField(String.format("%.3f",
				Double.valueOf(videoUsagePrefs.getStallDurationWarnVal() == 0.0 ?
						Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.defaultStallDurationWarnVal"))
						: videoUsagePrefs.getStallDurationWarnVal())), 5);
		stallDurationFailEdit = new JTextField(String.format("%.3f",
				Double.valueOf(videoUsagePrefs.getStallDurationFailVal() == 0.0 ?
						Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.defaultStallDurationFailVal"))
						: videoUsagePrefs.getStallDurationFailVal())), 5);

		segRedundancyWarnEdit = new JTextField(String.valueOf(videoUsagePrefs.getSegmentRedundancyWarnVal() == 0.0 ? 
				ResourceBundleHelper.getMessageString("preferences.video.defaultSegmentRedundancyWarnVal") : videoUsagePrefs.getSegmentRedundancyWarnVal()), 5);
		segRedundancyFailEdit = new JTextField(String.valueOf(videoUsagePrefs.getSegmentRedundancyFailVal() == 0.0 ? 
				ResourceBundleHelper.getMessageString("preferences.video.defaultSegmentRedundancyFailVal")
				: videoUsagePrefs.getSegmentRedundancyFailVal()), 5);

		startupDelayEdit.setInputVerifier(getNumericInputVerifier(MAX_STARTUPDELAY, 0.01, 3));
		startupDelayEdit.addKeyListener(getKeyListener(startupDelayEdit));

		inputVerifier = getNumericInputVerifier(MAX_STALLDURATION, 0.01, 3);
		stallDurationWarnEdit.setInputVerifier(inputVerifier);
		stallDurationWarnEdit.addKeyListener(getKeyListener(stallDurationWarnEdit));
		
		stallDurationFailEdit.setInputVerifier(inputVerifier);
		stallDurationFailEdit.addKeyListener(getKeyListener(stallDurationFailEdit));

		inputVerifier = getNumericInputVerifier(MAX_REDUNDANCY, 0.01, 3);
		segRedundancyWarnEdit.setInputVerifier(inputVerifier);
		segRedundancyWarnEdit.addKeyListener(getKeyListener(segRedundancyWarnEdit));
		
		segRedundancyFailEdit.setInputVerifier(inputVerifier);
		segRedundancyFailEdit.addKeyListener(getKeyListener(segRedundancyFailEdit));

		idx = 0;

		GridBagLayout gridBagLayout = new GridBagLayout();

		JPanel panel = new JPanel(gridBagLayout);
		panel.setAlignmentX(CENTER_ALIGNMENT);
		panel.setBorder(new RoundedBorder(new Insets(1, 10, 10, 10), null));
		addHeaderLine(panel);
		addTopLine(startupDelayLabel, startupDelayEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		addMultipleEditsLine(stallDurationLabel, stallDurationWarnEdit, stallDurationFailEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		addMultipleEditsLine(segRedundancyLabel, segRedundancyWarnEdit, segRedundancyFailEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.count")));
		addDefaultButton(panel, 1, inputVerifier);
		return panel;

	}

	private NumericInputVerifier getNumericInputVerifier(double max, double min, int significands) {
		return new NumericInputVerifier(max, min, significands, true, preferencesDialog);
	}

	private Component getVideoPrefencesPanel() {

		stallTriggerTimeEdit = new JTextField(String.format("%.3f", videoUsagePrefs.getStallTriggerTime()), 5);
		maxBufferEdit = new JTextField(String.format("%.2f", videoUsagePrefs.getMaxBuffer()), 5);
		stallPausePointEdit = new JTextField(String.format("%.4f", videoUsagePrefs.getStallPausePoint()), 5);
		stallRecoveryEdit = new JTextField(String.format("%.4f", videoUsagePrefs.getStallRecovery()), 5);
		targetedStartupDelayEdit = new JTextField(String.format("%.2f", videoUsagePrefs.getStartupDelay()), 5);
		nearStallEdit = new JTextField(String.format("%.4f", videoUsagePrefs.getNearStall()), 5);

		stallTriggerTimeEdit.setInputVerifier(getNumericInputVerifier(MAX_STALLTRIGGERTIME, 0.01, 3));
		stallTriggerTimeEdit.addKeyListener(getKeyListener(stallTriggerTimeEdit));
		
		maxBufferEdit.setInputVerifier(getNumericInputVerifier(MAX_BUFFER, 0, 2));
		maxBufferEdit.addKeyListener(getKeyListener(maxBufferEdit));
		
		stallPausePointEdit.setInputVerifier(getNumericInputVerifier(MAX_STALLRECOVERY, 0, 4));
		stallPausePointEdit.addKeyListener(getKeyListener(stallPausePointEdit));
		
		stallRecoveryEdit.setInputVerifier(getNumericInputVerifier(MAX_STALLRECOVERY, 0, 4));
		stallRecoveryEdit.addKeyListener(getKeyListener(stallRecoveryEdit));
		
		targetedStartupDelayEdit.setInputVerifier(getNumericInputVerifier(MAX_TARGETEDSTARTUPDELAY, 0, 2));
		targetedStartupDelayEdit.addKeyListener(getKeyListener(targetedStartupDelayEdit));
		
		NumericInputVerifier numericInputVerifier = getNumericInputVerifier(MAX_NEARSTALL, 0.01, 4);
		nearStallEdit.setInputVerifier(numericInputVerifier);
		nearStallEdit.addKeyListener(getKeyListener(nearStallEdit));
		
		startupDelayReminder = new JCheckBox();
		
		duplicateHandlingEditCombo = new JComboBox<>();
		for (DUPLICATE_HANDLING item : DUPLICATE_HANDLING.values()) {
			duplicateHandlingEditCombo.addItem(item);
		}
		duplicateHandlingEditCombo.setSelectedItem(videoUsagePrefs.getDuplicateHandling());
		idx = 0;

		GridBagLayout gridBagLayout = new GridBagLayout();

		JPanel panel = new JPanel(gridBagLayout);
		panel.setAlignmentX(CENTER_ALIGNMENT);

		panel.setBorder(new RoundedBorder(new Insets(10, 10, 10, 10), null));

		addVideoPreference(panel);
		addDefaultButton(panel, 2, numericInputVerifier);
		return panel;

	}

	private void addVideoPreference(JPanel panel) {
		Label stallTriggerTimeLabel = new Label(
				ResourceBundleHelper.getMessageString("video.usage.dialog.stallTriggerTime"));
		Label maxBufferLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.maxBuffer"));
		Label duplicateHandlingLabel = new Label(
				ResourceBundleHelper.getMessageString("video.usage.dialog.duplicateHandling"));
		Label stallPausePointLabel = new Label(
				ResourceBundleHelper.getMessageString("video.usage.dialog.stallPausePoint"));
		Label stallRecoveryLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.stallRecovery"));
		Label targetedStartupDelayLabel = new Label(
				ResourceBundleHelper.getMessageString("video.usage.dialog.targetedStartupDelay"));
		Label nearStallLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.nearStall"));
		
		addLine(targetedStartupDelayLabel, targetedStartupDelayEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		addLine(stallTriggerTimeLabel, stallTriggerTimeEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		addLine(stallPausePointLabel, stallPausePointEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		addLine(stallRecoveryLabel, stallRecoveryEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		addLine(nearStallLabel, nearStallEdit, panel,
				new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		addLine(maxBufferLabel, maxBufferEdit, panel, new Label(ResourceBundleHelper.getMessageString("units.mbytes")));
		addLineComboBox(duplicateHandlingLabel, duplicateHandlingEditCombo, panel);
		
	}

	private KeyListener getKeyListener(JTextField textField) {
		KeyListener keyListener = new KeyListener() {
         	
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if(!textField.getInputVerifier().verify(textField) || !validateVideoPreferenceConfiguration()) {
					toggleOtherFields(false);
				} else {
					toggleOtherFields(true);
				}			
			}
			
		};
		return keyListener;
	}
	
	private void toggleOtherFields(boolean isEnabled) {
		preferencesDialog.getTabbedPane().setEnabledAt(0, isEnabled);
		preferencesDialog.getTabbedPane().setEnabledAt(1, isEnabled);
		preferencesDialog.getSaveButton().setEnabled(isEnabled);
		preferencesDialog.getReloadButton().setEnabled(isEnabled);
		duplicateHandlingEditCombo.setEnabled(isEnabled);	
	}


	private void addLine(Label label, JTextField edit, JPanel panel, Label units) {
		edit.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(label, new GridBagConstraints(0, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		panel.add(units, new GridBagConstraints(1, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		panel.add(edit, new GridBagConstraints(2, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 42, 0, 0), 0, 0));
		idx++;
	}

	private void addTopLine(Label label, JTextField edit, JPanel panel, Label units) {
		edit.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(label, new GridBagConstraints(0, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		panel.add(units, new GridBagConstraints(1, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		panel.add(edit, new GridBagConstraints(2, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		panel.add(new JLabel(" "), new GridBagConstraints(3, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 50), 0, 0));
		idx++;
	}

	private void addMultipleEditsLine(Label label, JTextField feild1, JTextField feild2, JPanel panel, Label units) {
		feild1.setHorizontalAlignment(SwingConstants.RIGHT);
		feild2.setHorizontalAlignment(SwingConstants.RIGHT);

		panel.add(label, getGridBagConstraints(0, 0.1, 0.2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
		panel.add(units, getGridBagConstraints(1, 0.1, 0.2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));

		panel.add(feild1, new GridBagConstraints(2, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(feild2, new GridBagConstraints(3, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		panel.add(new JLabel(" "), new GridBagConstraints(4, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 158), 0, 0));

		idx++;
	}

	private void addDefaultButton(JPanel panel, int panelNumber, NumericInputVerifier numericInputVerifier) {

		ActionListener actionListener;
		if (panelNumber == 1) {
			actionListener = (ActionEvent arg) -> setDefault();
		} else {
			actionListener = (ActionEvent arg) -> setPreferencesDefault();
		}

		panel.add(getDefaultButton(ResourceBundleHelper.getMessageString("preferences.video.default"), actionListener), new GridBagConstraints(4, idx, 1, 1, 0.1, 0.2,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		idx++;
	}

	private void setDefault() {
		
		if(preferencesDialog.getPopup() != null) {
			preferencesDialog.getPopup().hide();
		}	
		toggleOtherFields(true);
		
		startupDelayEdit.setText(String.format("%.3f", Double.valueOf(
				ResourceBundleHelper.getMessageString("preferences.video.defaultStartUpDelayWarnVal"))));
		stallDurationWarnEdit.setText(
				String.format("%.3f", Double.valueOf(
				ResourceBundleHelper.getMessageString("preferences.video.defaultStallDurationWarnVal"))));
		
		stallDurationFailEdit.setText(String.format("%.3f", Double.valueOf(
				ResourceBundleHelper.getMessageString("preferences.video.defaultStallDurationFailVal"))));	
		
		segRedundancyWarnEdit.setText(ResourceBundleHelper.getMessageString("preferences.video.defaultSegmentRedundancyWarnVal"));
		segRedundancyFailEdit.setText(ResourceBundleHelper.getMessageString("preferences.video.defaultSegmentRedundancyFailVal"));
		
		this.repaint();
	}

	

	private void setPreferencesDefault() {
		
		if(preferencesDialog.getPopup() != null) {
			preferencesDialog.getPopup().hide();
		}	
		toggleOtherFields(true);
		
		stallTriggerTimeEdit.setText(String.format("%.3f",
				Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.stallTriggerTime"))));
		maxBufferEdit.setText(String.format("%.2f",
				Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.maxBuffer"))));
		stallPausePointEdit.setText(String.format("%.4f",
				Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.stallPausePoint"))));
		stallRecoveryEdit.setText(String.format("%.4f",
				Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.stallRecovery"))));
		targetedStartupDelayEdit.setText(String.format("%.2f",
				Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.targetedStartupDelay"))));
		nearStallEdit.setText(String.format("%.4f",
				Double.valueOf(ResourceBundleHelper.getMessageString("preferences.video.nearStall"))));
		startupDelayReminder.setSelected(true);
		duplicateHandlingEditCombo.setSelectedItem(DUPLICATE_HANDLING.HIGHEST);
		this.repaint();
	}

	private JButton getDefaultButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}

	private void addHeaderLine(JPanel panel) {
		panel.add(new Label(" "), new GridBagConstraints(0, idx, 1, 1, 0.3, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(new Label(" "), new GridBagConstraints(1, idx, 1, 1, 0.3, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(new Label("Warning"), new GridBagConstraints(2, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(new Label("Fail"), new GridBagConstraints(3, idx, 1, 1, 0.1, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 25, 0, 0), 0, 0));

		idx++;
	}

	private void addLineComboBox(Label label, JComponent editable, JPanel panel) {
		panel.add(label, new GridBagConstraints(0, idx, 8, 1, 1.0, 0.3, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(editable, new GridBagConstraints(2, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));
		idx++;
	}

	public boolean saveVideoAnalysisConfiguration() {
		boolean validationFailure = true;
		if (preSaveCheck(maxBufferEdit, stallTriggerTimeEdit, stallPausePointEdit, stallRecoveryEdit,
				targetedStartupDelayEdit, nearStallEdit, startupDelayEdit, stallDurationWarnEdit, stallDurationFailEdit,
				segRedundancyWarnEdit, segRedundancyFailEdit)) {
			loadVideoUsagePreferences();
			savePreference();
			validationFailure = false;
		}
		if (validationFailure && errorField != null) {
			errorField.requestFocus();
		}

		return validationFailure;
	}

	/**
	 * save preference
	 */
	private boolean savePreference() {

		videoUsagePrefs.setStartUpDelayWarnVal(Double.parseDouble(startupDelayEdit.getText()));
		videoUsagePrefs.setStallDurationWarnVal(Double.parseDouble(stallDurationWarnEdit.getText()));
		videoUsagePrefs.setStallDurationFailVal(Double.parseDouble(stallDurationFailEdit.getText()));
		videoUsagePrefs.setSegmentRedundancyWarnVal(Double.parseDouble(segRedundancyWarnEdit.getText()));
		videoUsagePrefs.setSegmentRedundancyFailVal(Double.parseDouble(segRedundancyFailEdit.getText()));

		videoUsagePrefs.setMaxBuffer(Double.valueOf(maxBufferEdit.getText()));
		videoUsagePrefs.setStallTriggerTime(Double.valueOf(stallTriggerTimeEdit.getText()));
		videoUsagePrefs.setDuplicateHandling((DUPLICATE_HANDLING) duplicateHandlingEditCombo.getSelectedItem());
		videoUsagePrefs.setStallPausePoint(Double.valueOf(stallPausePointEdit.getText()));
		videoUsagePrefs.setStallRecovery(Double.valueOf(stallRecoveryEdit.getText()));
		videoUsagePrefs.setStartupDelay(Double.valueOf(targetedStartupDelayEdit.getText()));
		videoUsagePrefs.setNearStall(Double.valueOf(nearStallEdit.getText()));
		videoPreferenceModel = new VideoPreferenceModel();
		String temp;
		try {
			temp = mapper.writeValueAsString(videoUsagePrefs);
		} catch (IOException e) {
			return false;
		}
		if (temp != null && !temp.equals("null")) {
			prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
		}
		return true;

	}

	/**
	 * Gather results of all validations. Return true only if all are true
	 * 
	 * @param editField
	 * @return
	 */
	private boolean preSaveCheck(JTextField... editField) {
		setErrorField(null);
		for (JTextField text : editField) {
			if (!((NumericInputVerifier) text.getInputVerifier()).getResult()) {
				setErrorField(text);
				return false;
			}
		}
		return true;
	}
	
	private void setErrorField(JTextField editField) {
		errorField = editField;		
	}

	@Override
	public Dimension getPreferredSize() {
		return (new Dimension(1000, 300));
	}

	public boolean saveVideoPreferences() {
		boolean result = true;
		videoPreferenceModel = new VideoPreferenceModel();
		if (!validateVideoPreferenceConfiguration() || saveVideoAnalysisConfiguration()) {
			throw new IllegalArgumentException(videoPreferenceModel.getValidationError().toString());
		}
		return result;
	}

	private boolean validateVideoPreferenceConfiguration() {
		boolean isValid = videoPreferenceModel.stallDurationValidation(Double.parseDouble(stallDurationWarnEdit.getText()),
				Double.parseDouble(stallDurationFailEdit.getText()))
				&& videoPreferenceModel.segmentRedundancyValidation(Double.parseDouble(segRedundancyWarnEdit.getText()),
						Double.parseDouble(segRedundancyFailEdit.getText()));

		if (!isValid || StringUtils.isNotBlank(videoPreferenceModel.getValidationError())) {

			String errorText = videoPreferenceModel.getValidationError();
			if (videoPreferenceModel.getErrorComponent() == 1) {
				stallDurationFailEdit.requestFocus();
				inputVerifier.popup(stallDurationFailEdit, errorText);
			} else if (videoPreferenceModel.getErrorComponent() == 2) {
				segRedundancyFailEdit.requestFocus();
				inputVerifier.popup(segRedundancyFailEdit, errorText);
				
			}
		}
		return isValid;
	}
}