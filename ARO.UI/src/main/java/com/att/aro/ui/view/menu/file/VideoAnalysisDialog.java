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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.NumericInputVerifier;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VideoAnalysisDialog extends JPanel {

	private static final long serialVersionUID = 1L;
	private static int MAXBUFFER = 5000;
	private static int MAXSTALLTRIGGERTIME = 10;
	private static int MAXSTALLRECOVERY = 10;
	private static int MAXTARGETEDSTARTUPDELAY = 10;
	private static float MAXNEARSTALL = 0.5f;

	private static final Logger LOG = LogManager.getLogger(VideoAnalysisDialog.class.getName());
	
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
	
	/**
	 * Load VideoUsage Preferences
	 */
	private void loadPrefs() {
		mapper = new ObjectMapper();
		prefs = PreferenceHandlerImpl.getInstance();
		
		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null && !temp.equals("null")) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
			}
		} else {
			try {
				videoUsagePrefs = ContextAware.getAROConfigContext().getBean("videoUsagePrefs",VideoUsagePrefs.class); //new VideoUsagePrefs();
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
			}
		}
	}

	public VideoAnalysisDialog() {
		loadPrefs();
		initialize();
	}
	
	private void initialize() {
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		this.add(getJDialogPanel());
	}
	
	private JPanel getJDialogPanel() {
		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			jDialogPanel.add(getPrefencesPanel());
		}
		return jDialogPanel;
	}
	
	private Component getPrefencesPanel() {

		Label stallTriggerTimeLabel  = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.stallTriggerTime"));
		Label maxBufferLabel         = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.maxBuffer"));
		Label duplicateHandlingLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.duplicateHandling"));
		Label stallPausePointLabel	 = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.stallPausePoint"));
		Label stallRecoveryLabel	 = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.stallRecovery"));
		Label startupDelayReminderLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.startupDelayReminder"));
		Label targetedStartupDelayLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.targetedStartupDelay"));
		Label nearStallLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.nearStall"));
		
		stallTriggerTimeEdit  = new JTextField(String.format("%.3f", videoUsagePrefs.getStallTriggerTime()) ,5);
		maxBufferEdit         = new JTextField(String.format("%.2f", videoUsagePrefs.getMaxBuffer())        ,5);
		stallPausePointEdit   = new JTextField(String.format("%.4f", videoUsagePrefs.getStallPausePoint())  ,5);
		stallRecoveryEdit     = new JTextField(String.format("%.4f", videoUsagePrefs.getStallRecovery())    ,5);
		targetedStartupDelayEdit = new JTextField(String.format("%.2f", videoUsagePrefs.getStartupDelay())  ,5);
		nearStallEdit = new JTextField(String.format("%.4f", videoUsagePrefs.getNearStall()), 5);

		stallTriggerTimeEdit.setInputVerifier(new NumericInputVerifier(MAXSTALLTRIGGERTIME, 0.01, 3));
		maxBufferEdit		.setInputVerifier(new NumericInputVerifier(MAXBUFFER, 0, 2));
		stallPausePointEdit .setInputVerifier(new NumericInputVerifier(MAXSTALLRECOVERY, 0, 4));
		stallRecoveryEdit   .setInputVerifier(new NumericInputVerifier(MAXSTALLRECOVERY, 0, 4));
		targetedStartupDelayEdit.setInputVerifier(new NumericInputVerifier(MAXTARGETEDSTARTUPDELAY, 0, 2));
		nearStallEdit.setInputVerifier(new NumericInputVerifier(MAXNEARSTALL, 0.01, 4));
		startupDelayReminder  = new JCheckBox();
		startupDelayReminder.setSelected(videoUsagePrefs.isStartupDelayReminder());
		duplicateHandlingEditCombo = new JComboBox<>();
		for (DUPLICATE_HANDLING item : DUPLICATE_HANDLING.values()) {
			duplicateHandlingEditCombo.addItem(item);
		}
		duplicateHandlingEditCombo.setSelectedItem(videoUsagePrefs.getDuplicateHandling());
		int idx = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		
		idx = addLine(targetedStartupDelayLabel, targetedStartupDelayEdit, idx, panel, new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		idx = addLine(stallTriggerTimeLabel, stallTriggerTimeEdit, idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		idx = addLine(stallPausePointLabel , stallPausePointEdit , idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		idx = addLine(stallRecoveryLabel   , stallRecoveryEdit   , idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		idx = addLine(nearStallLabel, nearStallEdit, idx, panel, new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		idx = addLine(maxBufferLabel       , maxBufferEdit       , idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.mbytes")));
		idx = addLineComboBox(startupDelayReminderLabel, startupDelayReminder, idx, panel);
		idx = addLineComboBox(duplicateHandlingLabel       , duplicateHandlingEditCombo       , idx, panel);

		return panel;
		
	}

	private int addLine(Label label, JTextField edit, int idx, JPanel panel, Label units) {
		edit.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(label, new GridBagConstraints(0, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(edit,  new GridBagConstraints(2, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
		panel.add(units, new GridBagConstraints(3, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
		return ++idx;
	}
	
	private int addLineComboBox(Label label, JComponent editable, int idx, JPanel panel){
		panel.add(label, new GridBagConstraints(0, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(editable,  new GridBagConstraints(2, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
		return ++idx;
	}
	
	public boolean saveVideoAnalysisConfiguration() {
		boolean validationFailure = true;
		if (preSaveCheck(maxBufferEdit, stallTriggerTimeEdit, stallPausePointEdit, stallRecoveryEdit, targetedStartupDelayEdit, nearStallEdit)) {
			loadPrefs();
			executeOkButton();
			validationFailure = false;
		}
		return validationFailure;
	}
	
	/**
	 * click Save&Close button, then save user preference setting and re-analyze
	 */
	private void executeOkButton() {
		savePreference();
	}
	
	/**
	 * save preference
	 */
   private boolean savePreference() {

		videoUsagePrefs.setMaxBuffer(Double.valueOf(maxBufferEdit.getText()));
		videoUsagePrefs.setStallTriggerTime(Double.valueOf(stallTriggerTimeEdit.getText()));
		videoUsagePrefs.setDuplicateHandling((DUPLICATE_HANDLING) duplicateHandlingEditCombo.getSelectedItem());
		videoUsagePrefs.setStallPausePoint(Double.valueOf(stallPausePointEdit.getText()));
		videoUsagePrefs.setStallRecovery(Double.valueOf(stallRecoveryEdit.getText()));
		videoUsagePrefs.setStartupDelayReminder(startupDelayReminder.isSelected());
		videoUsagePrefs.setStartupDelay(Double.valueOf(targetedStartupDelayEdit.getText()));
		videoUsagePrefs.setNearStall(Double.valueOf(nearStallEdit.getText()));
		
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
	private boolean preSaveCheck(JTextField ... editField) {
		
		for ( JTextField text: editField) {
			if (!((NumericInputVerifier)text.getInputVerifier()).getResult()){
				return false;
			}
		}
		return true;
	}
}
