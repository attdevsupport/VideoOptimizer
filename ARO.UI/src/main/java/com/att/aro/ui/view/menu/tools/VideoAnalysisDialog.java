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
package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.codehaus.jackson.map.ObjectMapper;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

public class VideoAnalysisDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@InjectLogger
	private static ILogger log;

	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	
	private JPanel jDialogPanel;
	private PacketAnalyzerResult currentTraceResult;
	
	private IAROView parent;
	private JPanel ctrlPanel;
	private JPanel buttonGrid;
	private JButton okButton;
	private JButton cancelButton;
	

	private VideoUsagePrefs videoUsagePrefs;

	private JTextField startupDelayEdit;

	private JTextField arrivalToPlayEdit;

	private ObjectMapper mapper;

	private PreferenceHandlerImpl prefs;

	private JTextField stallTriggerTimeEdit;

	private JTextField maxBufferEdit;

	private JTextField duplicateHandlingEdit;
	
	/**
	 * Load VideoUsage Preferences
	 */
	private void loadPrefs() {
		mapper = new ObjectMapper();
		prefs = PreferenceHandlerImpl.getInstance();
		
		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			} catch (IOException e) {
				log.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
			}
		} else {
			try {
				videoUsagePrefs = new VideoUsagePrefs();
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				log.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
			}
		}
	}

	private enum DialogItem {
		private_data_dialog_button_ok,
		private_data_dialog_button_cancel
	}
	
	public PacketAnalyzerResult getCurrentPktAnalyzerResult() {
		return currentTraceResult;
	}

	public void setCurrentPktAnalyzerResult(PacketAnalyzerResult currentTraceResult) {
		this.currentTraceResult = currentTraceResult;
	}

	public VideoAnalysisDialog(IAROView parent) {
		this.parent = parent;
		((MainFrame) parent).setVideoAnalysisDialog(this);
		loadPrefs();
		initialize();
	}
	
	private void initialize() {
		this.setSize(500, 200);
		this.setModal(false);
		this.setTitle(resourceBundle.getString("video.usage.dialog.legend"));
		this.setLocationRelativeTo(getOwner());
		this.setContentPane(getJDialogPanel());
		this.setAlwaysOnTop(true);
	}
	
	private JPanel getJDialogPanel() {
		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setLayout(new BorderLayout());
			
			jDialogPanel.add(getPrefencesPanel(), BorderLayout.CENTER);
			jDialogPanel.add(getCtrlPanel(), BorderLayout.SOUTH);
		}
		return jDialogPanel;
	}
	
	private Component getPrefencesPanel() {

		Label startupDelayLabel      = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.startupDelay"));
		Label arrivalToPlayLabel     = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.arrivalToPlay"));
		Label stallTriggerTimeLabel  = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.stallTriggerTime"));
		Label maxBufferLabel         = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.maxBuffer"));
		Label duplicateHandlingLabel = new Label(ResourceBundleHelper.getMessageString("video.usage.dialog.duplicateHandling"));

		startupDelayEdit      = new JTextField(String.format("%8.2f", videoUsagePrefs.getStartupDelay())     );
		arrivalToPlayEdit     = new JTextField(String.format("%8.2f", videoUsagePrefs.getArrivalToPlay())    );
		stallTriggerTimeEdit  = new JTextField(String.format("%8.2f", videoUsagePrefs.getStallTriggerTime()) );
		maxBufferEdit         = new JTextField(String.format("%8.2f", videoUsagePrefs.getMaxBuffer())        );
		duplicateHandlingEdit = new JTextField(videoUsagePrefs.getDuplicateHandling().toString()             );

		int idx = 0;
		JPanel panel = new JPanel(new GridBagLayout());

		idx = addLine(startupDelayLabel    , startupDelayEdit    , idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.seconds")));
//		idx = addLine(arrivalToPlayLabel   , arrivalToPlayEdit   , idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		idx = addLine(stallTriggerTimeLabel, stallTriggerTimeEdit, idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.seconds")));
		idx = addLine(maxBufferLabel       , maxBufferEdit       , idx, panel  ,new Label(ResourceBundleHelper.getMessageString("units.mbytes")));

		return panel;
		
	}

	private int addLine(Label label, JTextField edit, int idx, JPanel panel, Label units) {
		panel.add(label, new GridBagConstraints(0, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		panel.add(edit,  new GridBagConstraints(2, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
		panel.add(units, new GridBagConstraints(3, idx, 1, 1, 1.0, 0.2, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,  0, 0, 0), 0, 0));
		return ++idx;
	}
	
	/**
	 * ctrl panel contains button grid, which contains + button, ok button and cancel button
	 * @return
	 */
	private JPanel getCtrlPanel() {
		if (ctrlPanel == null) {
			ctrlPanel = new JPanel();
			ctrlPanel.setLayout(new BorderLayout(0, 0));
			ctrlPanel.add(getButtonGrid());
		}
		
		return ctrlPanel;
	}
	
	private JPanel getButtonGrid() {
		if (buttonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(2);
			buttonGrid = new JPanel();
			buttonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			buttonGrid.setLayout(gridLayout);
			
			buttonGrid.add(getOkButton());
			buttonGrid.add(getCancelButton());
		}
		
		return buttonGrid;
	}
	
	/**
	 * OK button
	 * @return
	 */
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_button_ok));
			okButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					executeOkButton();
				}
			});
		}
		
		return okButton;
	}
	
	/**
	 * click ok button, then save user preference setting and re-analyze
	 */
	private void executeOkButton() {
		if (savePreference()) {
			clean();
		}
	}
	
	/**
	 * save preference
	 */
	private boolean savePreference() {
		videoUsagePrefs.setStartupDelay(Double.valueOf(startupDelayEdit.getText()));
		videoUsagePrefs.setArrivalToPlay(Double.valueOf(arrivalToPlayEdit.getText()));
		videoUsagePrefs.setMaxBuffer(Double.valueOf(maxBufferEdit.getText()));
		videoUsagePrefs.setStallTriggerTime(Double.valueOf(stallTriggerTimeEdit.getText()));
		
		String temp;
		try {
			temp = mapper.writeValueAsString(videoUsagePrefs);
		} catch (IOException e) {
			return false;
		}
		prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
		return true;
	}
	
	/**
	 * Cancel button
	 * @return
	 */
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_button_cancel));
			cancelButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					executeCancelButton();
				}
			});
		}
		
		return cancelButton;
	}
	
	private void executeCancelButton() {
		clean();
	}
	
	private void clean() {
		setVisible(false);
		((MainFrame) parent).setVideoAnalysisDialog(null);
		dispose();
	}
}
