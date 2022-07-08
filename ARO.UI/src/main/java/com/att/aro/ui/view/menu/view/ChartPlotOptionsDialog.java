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
package com.att.aro.ui.view.menu.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.commonui.GUIPreferences;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.ChartPlotOptions;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;

/**
 * Represents the chart plot options dialog.
 *
 *
 */
public class ChartPlotOptionsDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private GUIPreferences guiPreferences = GUIPreferences.getInstance();
	private JPanel jContentPane;
	private JPanel buttonPanel;
	private JPanel jButtonGrid;
	private JButton okButton;
	private JButton cancelButton;
	private JPanel optionsPanel;
	private JPanel jAdvancedOptionsPanel;
	private JCheckBox jCPUStateCheckBox;
	private JCheckBox jGPSStateCheckBox;
	private JCheckBox jRadioStateCheckBox;
	private JCheckBox jBluetoothCheckBox;
	private JCheckBox jCameraStateCheckBox;
	private JCheckBox jScreenStateCheckBox;
	private JCheckBox jBatteryStateCheckBox;
	private JCheckBox jWakelockStateCheckBox;
	private JCheckBox jWifiStateCheckBox;
	private JCheckBox jNetworkTypeCheckBox;
	private JCheckBox jThroughputCheckBox;
	private JCheckBox jThroughputULCheckBox;
	private JCheckBox jThroughputDLCheckBox;
	private JCheckBox jLatencyCheckbox;
	private JCheckBox jConnectionsCheckBox;
	private JCheckBox jUplinkCheckBox;
	private JCheckBox jDownlinkCheckBox;
	private JCheckBox jBurstsCheckBox;
	private JCheckBox jUserInputCheckBox;
	private JCheckBox jRRCStateCheckBox;
	private JCheckBox jAlarmTriggeredCheckBox;
	private JCheckBox jDefaultsCheckBox;
	private JCheckBox vidPresetCheckBox;
	private JCheckBox jVideoBufferOccupancyCheckBox;
	private JCheckBox jVideoVideoChunksCheckBox;
	private JCheckBox jVideoBufferTimeOccupancyCheckBox;
	private JCheckBox jTemperatureStateCheckBox;
	// private JCheckBox jAttenuationCheckBox;
	private JCheckBox jSpeedThrottleCheckBox;
	private List<ChartPlotOptions> currentCheckedOptionList;
	private List<ChartPlotOptions> selectedOptions;
	private List<ChartPlotOptions> defaultOptions;
	private String defaultViewCheckBoxText;
	private String vidPresetCheckBoxText;
	private Map<JCheckBox, ChartPlotOptions> checkBoxPlots;
	private EnableEscKeyCloseDialog enableEscKeyCloseDialog;
	private final SharedAttributesProcesses parent;
	private final JMenuItem callerMenuItem;

	private enum DialogItem {
		chart_options_dialog_defaults, chart_options_dialog_title, chart_options_dialog_button_ok, 
		chart_options_dialog_button_cancel, chart_options_dialog_legend, chart_options_dialog_wakelock, 
		chart_options_dialog_alarm, chart_options_dialog_buffer_occupancy, chart_options_dialog_video_chunks, 
		chart_options_dialog_cpu, chart_options_dialog_gps, chart_options_dialog_wifi, chart_options_dialog_network, 
		chart_options_dialog_ulpackets, chart_options_dialog_dlpackets, chart_options_dialog_bursts, 
		chart_options_dialog_userinput, chart_options_dialog_rrc, chart_options_dialog_radio, chart_options_dialog_bluetooth, 
		chart_options_dialog_camera, chart_options_dialog_battery, chart_options_dialog_screen, 
		chart_options_dialog_throughput, chart_options_dialog_ULthroughput, chart_options_dialog_DLthroughput, chart_options_dialog_latency, chart_options_dialog_connections, chart_options_dialog_bufferTime_occupancy, chart_options_dialog_video, 
		chart_options_dialog_temperature,chart_options_dialog_attenation,chart_options_dialog_speedthrottle
	}

	/**
	 * Initializes a new instance of the ChartPlotOptionsDialog class using the
	 * specified instance of the ApplicationResourceOptimizer as the parent
	 * window, and an instance of the AROAdvancedTabb.
	 *
	 * @param owner
	 *            - The ApplicationResourceOptimizer instance.
	 *
	 * @param actionableClass
	 *            - The AROAdvancedTabb instance.
	 */
	public ChartPlotOptionsDialog(SharedAttributesProcesses parent, JMenuItem callerMenuItem) {
		super(parent.getFrame());
		this.parent = parent;
		this.callerMenuItem = callerMenuItem;
		// grab the selected options from the user pref's file
		this.defaultOptions = ChartPlotOptions.getDefaultList();
		this.defaultViewCheckBoxText = ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_defaults);
		this.vidPresetCheckBoxText = ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_video);
		this.selectedOptions = guiPreferences.getChartPlotOptions();
		// create a check box map to iterate through later
		this.checkBoxPlots = new HashMap<JCheckBox, ChartPlotOptions>();
		// call initialize
		initialize();
	}

	private void enableOptions(boolean enabled) {
		jCPUStateCheckBox.setEnabled(enabled);
		jGPSStateCheckBox.setEnabled(enabled);
		jRadioStateCheckBox.setEnabled(enabled);
		jBluetoothCheckBox.setEnabled(enabled);
		jCameraStateCheckBox.setEnabled(enabled);
		jScreenStateCheckBox.setEnabled(enabled);
		jBatteryStateCheckBox.setEnabled(enabled);
		jTemperatureStateCheckBox.setEnabled(enabled);
		jWakelockStateCheckBox.setEnabled(enabled);
		jWifiStateCheckBox.setEnabled(enabled);
		jNetworkTypeCheckBox.setEnabled(enabled);
		jConnectionsCheckBox.setEnabled(enabled);
		jThroughputCheckBox.setEnabled(enabled);
		jLatencyCheckbox.setEnabled(enabled);
		jUplinkCheckBox.setEnabled(enabled);
		jDownlinkCheckBox.setEnabled(enabled);
		jBurstsCheckBox.setEnabled(enabled);
		jUserInputCheckBox.setEnabled(enabled);
		jRRCStateCheckBox.setEnabled(enabled);
		jAlarmTriggeredCheckBox.setEnabled(enabled);
		jVideoBufferOccupancyCheckBox.setEnabled(enabled);
		jVideoVideoChunksCheckBox.setEnabled(enabled);
		jVideoBufferTimeOccupancyCheckBox.setEnabled(enabled);
		jSpeedThrottleCheckBox.setEnabled(enabled);
	}

	/**
	 * Initializes the dialog.
	 */
	private void initialize() {
		this.setTitle(ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_title));
		this.setContentPane(getJContentPane());
		this.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		if (isUserPrefsSelected(ChartPlotOptions.DEFAULT_VIEW)) {
			enableOptions(false);
		}
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		enableEscKeyCloseDialog = new EnableEscKeyCloseDialog(getRootPane(), this, false);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent event) {
				if (enableEscKeyCloseDialog.consumeEscPressed()) {
					executeCancelButton();
				}
			}
		});
		pack();
		setLocationRelativeTo(parent.getFrame());
		getRootPane().setDefaultButton(okButton);
	}

	/**
	 * /** Initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getButtonPanel(), BorderLayout.SOUTH);
			jContentPane.add(getOptionsPanel(), BorderLayout.CENTER);
		}
		this.currentCheckedOptionList = getCheckedOptions();
		return jContentPane;
	}

	/**
	 * Initializes buttonPanel
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(getJButtonGrid(), BorderLayout.EAST);
		}
		return buttonPanel;
	}

	/**
	 * Initializes jButtonGrid
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel();
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(getJDefaultsCheckBox());
			jButtonGrid.add(getVidPresetCheckBox());
			jButtonGrid.add(getOkButton(), null);
			jButtonGrid.add(getCancelButton(), null);
		}
		return jButtonGrid;
	}

	/**
	 * Initializes and returns the OK Button
	 */
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_button_ok));
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					currentCheckedOptionList = getCheckedOptions();
					if (!validateSelectedOptions(currentCheckedOptionList)) {
						return;
					}
					guiPreferences.setChartPlotOptions(currentCheckedOptionList);
					new Thread(() -> sendGAViews(currentCheckedOptionList)).start();
					Component currentTab = parent.getCurrentTabComponent();
					if (currentTab != null && currentTab instanceof DiagnosticsTab) {
						parent.updateChartSelection(currentCheckedOptionList);
					}
					setVisible(false);
					callerMenuItem.setEnabled(true);
				}
			});
		}
		return okButton;
	}
	
	private void sendGAViews(List<ChartPlotOptions> currentCheckedOptionList) {
		for (ChartPlotOptions option : currentCheckedOptionList) {
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getDiagnosticsViewsEvent(), option.name(), option.name());
		}

	}
	
	private boolean validateSelectedOptions(List<ChartPlotOptions> currentCheckedOptionList) {
		if (currentCheckedOptionList.size() > 11) {
			JOptionPane.showMessageDialog(this, "Please select no more than 11 items.");
			return false;
		}
		return true;
	}

	private void executeCancelButton() {
		updateFromUserPreferences();
		setVisible(false);
		callerMenuItem.setEnabled(true);
	}

	/**
	 * Initializes and returns the cancel Button
	 */
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_button_cancel));
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeCancelButton();
				}
			});
		}
		return cancelButton;
	}

	/**
	 * Initializes optionsPanel
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getOptionsPanel() {
		if (optionsPanel == null) {
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.weightx = 1.0D;
			gridBagConstraints.weighty = 1.0D;
			gridBagConstraints.gridy = 0;
			optionsPanel = new JPanel();
			optionsPanel.setLayout(new GridBagLayout());
			optionsPanel.add(getJAdvancedOptionsPanel(), gridBagConstraints);
		}
		return optionsPanel;
	}

	private GridBagConstraints getGridBagConstraints(int gridy) {
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.gridy = gridy;
		return gridBagConstraints;
	}

	/**
	 * Initializes the panel the contains the list of plot options check boxes.
	 */
	private JPanel getJAdvancedOptionsPanel() {
		if (jAdvancedOptionsPanel == null) {
			jAdvancedOptionsPanel = new JPanel();
			jAdvancedOptionsPanel.setLayout(new GridBagLayout());
			int counter = 0 ;
			jAdvancedOptionsPanel.setBorder(BorderFactory.createTitledBorder(null,
					ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_legend),
					TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
					new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
			jAdvancedOptionsPanel.add(jGPSStateCheckBox = getJCheckBox(jGPSStateCheckBox,
					DialogItem.chart_options_dialog_gps, ChartPlotOptions.GPS), getGridBagConstraints(counter++));
			addActionListener(jGPSStateCheckBox);
			jAdvancedOptionsPanel.add(jRadioStateCheckBox = getJCheckBox(jRadioStateCheckBox,
					DialogItem.chart_options_dialog_radio, ChartPlotOptions.RADIO), getGridBagConstraints(counter++));
			addActionListener(jRadioStateCheckBox);
			jAdvancedOptionsPanel.add(jBluetoothCheckBox = getJCheckBox(jBluetoothCheckBox,
					DialogItem.chart_options_dialog_bluetooth, ChartPlotOptions.BLUETOOTH), getGridBagConstraints(counter++));
			addActionListener(jBluetoothCheckBox);
			jAdvancedOptionsPanel.add(jCameraStateCheckBox = getJCheckBox(jCameraStateCheckBox,
					DialogItem.chart_options_dialog_camera, ChartPlotOptions.CAMERA), getGridBagConstraints(counter++));
			addActionListener(jCameraStateCheckBox);
			jAdvancedOptionsPanel.add(jScreenStateCheckBox = getJCheckBox(jScreenStateCheckBox,
					DialogItem.chart_options_dialog_screen, ChartPlotOptions.SCREEN), getGridBagConstraints(counter++));
			addActionListener(jScreenStateCheckBox);
			jAdvancedOptionsPanel.add(jBatteryStateCheckBox = getJCheckBox(jBatteryStateCheckBox,
					DialogItem.chart_options_dialog_battery, ChartPlotOptions.BATTERY), getGridBagConstraints(counter++));
			addActionListener(jBatteryStateCheckBox);
			jAdvancedOptionsPanel.add(
					jWakelockStateCheckBox = getJCheckBox(jWakelockStateCheckBox,
							DialogItem.chart_options_dialog_wakelock, ChartPlotOptions.WAKELOCK),
					getGridBagConstraints(counter++));
			addActionListener(jWakelockStateCheckBox);
			jAdvancedOptionsPanel.add(jWifiStateCheckBox = getJCheckBox(jWifiStateCheckBox,
					DialogItem.chart_options_dialog_wifi, ChartPlotOptions.WIFI), getGridBagConstraints(counter++));
			addActionListener(jWifiStateCheckBox);
			jAdvancedOptionsPanel.add(jAlarmTriggeredCheckBox = getJCheckBox(jAlarmTriggeredCheckBox,
					DialogItem.chart_options_dialog_alarm, ChartPlotOptions.ALARM), getGridBagConstraints(counter++));
			addActionListener(jAlarmTriggeredCheckBox);
			jAdvancedOptionsPanel.add(jNetworkTypeCheckBox = getJCheckBox(jNetworkTypeCheckBox,
					DialogItem.chart_options_dialog_network, ChartPlotOptions.NETWORK_TYPE), getGridBagConstraints(counter++));
			addActionListener(jNetworkTypeCheckBox);
			jAdvancedOptionsPanel.add(
					jSpeedThrottleCheckBox = getJCheckBox(jSpeedThrottleCheckBox,
							DialogItem.chart_options_dialog_attenation, ChartPlotOptions.SPEED_THROTTLE),
					getGridBagConstraints(counter++));
			addActionListener(jSpeedThrottleCheckBox);
			jAdvancedOptionsPanel.add(jThroughputCheckBox = getJCheckBox(jThroughputCheckBox,
					DialogItem.chart_options_dialog_throughput, ChartPlotOptions.THROUGHPUT),
					getGridBagConstraints(counter++));
			addActionListener(jThroughputCheckBox);
			jAdvancedOptionsPanel.add(jThroughputULCheckBox = getJCheckBox(jThroughputDLCheckBox,
					DialogItem.chart_options_dialog_ULthroughput, ChartPlotOptions.THROUGHPUTUL),
					getGridBagConstraints(counter++));
			addActionListener(jThroughputULCheckBox);
			jAdvancedOptionsPanel.add(jThroughputDLCheckBox = getJCheckBox(jThroughputDLCheckBox,
					DialogItem.chart_options_dialog_DLthroughput, ChartPlotOptions.THROUGHPUTDL),
					getGridBagConstraints(counter++));
			addActionListener(jThroughputDLCheckBox);
			jAdvancedOptionsPanel.add(jLatencyCheckbox = getJCheckBox(jLatencyCheckbox,
					DialogItem.chart_options_dialog_latency, ChartPlotOptions.LATENCY),
					getGridBagConstraints(counter++));
			addActionListener(jLatencyCheckbox);
			jAdvancedOptionsPanel.add(jConnectionsCheckBox = getJCheckBox(jConnectionsCheckBox,
					DialogItem.chart_options_dialog_connections, ChartPlotOptions.CONNECTIONS),
					getGridBagConstraints(counter++));
			addActionListener(jConnectionsCheckBox);		
			jAdvancedOptionsPanel.add(jUplinkCheckBox = getJCheckBox(jUplinkCheckBox,
					DialogItem.chart_options_dialog_ulpackets, ChartPlotOptions.UL_PACKETS), getGridBagConstraints(counter++));
			addActionListener(jUplinkCheckBox);
			jAdvancedOptionsPanel.add(jDownlinkCheckBox = getJCheckBox(jDownlinkCheckBox,
					DialogItem.chart_options_dialog_dlpackets, ChartPlotOptions.DL_PACKETS), getGridBagConstraints(counter++));
			addActionListener(jDownlinkCheckBox);
			jAdvancedOptionsPanel.add(jBurstsCheckBox = getJCheckBox(jBurstsCheckBox,
					DialogItem.chart_options_dialog_bursts, ChartPlotOptions.BURSTS), getGridBagConstraints(counter++));
			addActionListener(jBurstsCheckBox);
			jAdvancedOptionsPanel.add(jUserInputCheckBox = getJCheckBox(jUserInputCheckBox,
					DialogItem.chart_options_dialog_userinput, ChartPlotOptions.USER_INPUT), getGridBagConstraints(counter++));
			addActionListener(jUserInputCheckBox);
			jAdvancedOptionsPanel.add(jRRCStateCheckBox = getJCheckBox(jRRCStateCheckBox,
					DialogItem.chart_options_dialog_rrc, ChartPlotOptions.RRC), getGridBagConstraints(counter++));
			addActionListener(jRRCStateCheckBox);
			jAdvancedOptionsPanel.add(jCPUStateCheckBox = getJCheckBox(jCPUStateCheckBox,
					DialogItem.chart_options_dialog_cpu, ChartPlotOptions.CPU), getGridBagConstraints(counter++));
			addActionListener(jCPUStateCheckBox);
			jAdvancedOptionsPanel
					.add(jVideoBufferTimeOccupancyCheckBox = getJCheckBox(jVideoBufferTimeOccupancyCheckBox,
							DialogItem.chart_options_dialog_bufferTime_occupancy,
							ChartPlotOptions.BUFFER_TIME_OCCUPANCY), getGridBagConstraints(counter++));
			addActionListener(jVideoBufferTimeOccupancyCheckBox);
			jAdvancedOptionsPanel.add(
					jVideoBufferOccupancyCheckBox = getJCheckBox(jVideoBufferOccupancyCheckBox,
							DialogItem.chart_options_dialog_buffer_occupancy, ChartPlotOptions.BUFFER_OCCUPANCY),
					getGridBagConstraints(counter++));
			addActionListener(jVideoBufferOccupancyCheckBox);
			jAdvancedOptionsPanel.add(
					jVideoVideoChunksCheckBox = getJCheckBox(jVideoVideoChunksCheckBox,
							DialogItem.chart_options_dialog_video_chunks, ChartPlotOptions.VIDEO_CHUNKS),
					getGridBagConstraints(counter++));
			addActionListener(jVideoVideoChunksCheckBox);
			jAdvancedOptionsPanel.add(
					jTemperatureStateCheckBox = getJCheckBox(jTemperatureStateCheckBox,
							DialogItem.chart_options_dialog_temperature, ChartPlotOptions.TEMPERATURE),
					getGridBagConstraints(counter++));
			addActionListener(jTemperatureStateCheckBox);
		}
		return jAdvancedOptionsPanel;
	}

	private void addActionListener(JCheckBox checkBox) {
		checkBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(checkBox.isSelected() && !validateSelectedOptions(getCheckedOptions())) {
					checkBox.setSelected(false);
				}
				}
		});
	}

	/**
	 * Returns the list of plot options selected from the list.
	 */
	private List<ChartPlotOptions> getCheckedOptions() {
		List<ChartPlotOptions> list = new ArrayList<ChartPlotOptions>();
		for (JCheckBox cb : checkBoxPlots.keySet()) {
			if (cb.isSelected()) {
				list.add(checkBoxPlots.get(cb));
			}
		}
		return list;
	}

	private boolean isUserPrefsSelected(ChartPlotOptions option) {
		return selectedOptions.contains(option);
	}

	private JCheckBox getJCheckBox(JCheckBox jCheckboxParm, DialogItem dialogItem, ChartPlotOptions chartPlotOption) {
		boolean thisOnesNew = jCheckboxParm == null;
		JCheckBox jCheckbox = (thisOnesNew || jCheckboxParm == null) ? new JCheckBox() : jCheckboxParm;
		if (thisOnesNew) {
			jCheckbox.setText(ResourceBundleHelper.getMessageString(dialogItem));
			jCheckbox.setSelected(isUserPrefsSelected(chartPlotOption));
			checkBoxPlots.put(jCheckbox, chartPlotOption);
		}
		return jCheckbox;
	}

	/**
	 * Initializes jCPUStateCheckBox
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCPUStateCheckBox() {
		if (jCPUStateCheckBox == null) {
			jCPUStateCheckBox = new JCheckBox();
			jCPUStateCheckBox.setText(ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_cpu));
			jCPUStateCheckBox.setSelected(isUserPrefsSelected(ChartPlotOptions.CPU));
			checkBoxPlots.put(jCPUStateCheckBox, ChartPlotOptions.CPU);
		}
		return jCPUStateCheckBox;
	}

	/**
	 * Return status of the CPU check box from the View Options dialog
	 *
	 * @return Returns true is selected, false if not selected.
	 */
	public boolean isCpuCheckBoxSelected() {
		return getJCPUStateCheckBox().isSelected();
	}

	/**
	 * Initializes Default View check box
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJDefaultsCheckBox() {
		if (jDefaultsCheckBox == null) {
			jDefaultsCheckBox = new JCheckBox();
			jDefaultsCheckBox.setText(ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_defaults));
			jDefaultsCheckBox.setSelected(isUserPrefsSelected(ChartPlotOptions.DEFAULT_VIEW));
			ItemListener itemListener = new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent itemEvent) {
					boolean enableItems = !(itemEvent.getStateChange() == ItemEvent.SELECTED);
					if (!enableItems) {
						updateDefaultCheckBoxes();
					}
					enableOptions(enableItems);
				}
			};
			jDefaultsCheckBox.addItemListener(itemListener);
			checkBoxPlots.put(jDefaultsCheckBox, ChartPlotOptions.DEFAULT_VIEW);
		}
		return jDefaultsCheckBox;
	}

	/**
	 * Initializes Video View check box
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getVidPresetCheckBox() {
		if (vidPresetCheckBox == null) {
			vidPresetCheckBox = new JCheckBox();
			vidPresetCheckBox.setText(ResourceBundleHelper.getMessageString(DialogItem.chart_options_dialog_video));
			vidPresetCheckBox.setSelected(isUserPrefsSelected(ChartPlotOptions.DEFAULT_VIEW));
			ItemListener itemListener = new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent itemEvent) {
					boolean enableItems = !(itemEvent.getStateChange() == ItemEvent.SELECTED);
					if (!enableItems) {
						updateVidPresetCheckBoxes();
					}
					enableOptions(enableItems);
				}
			};
			vidPresetCheckBox.addItemListener(itemListener);
			checkBoxPlots.put(vidPresetCheckBox, ChartPlotOptions.DEFAULT_VIDEO_VIEW);
		}
		return vidPresetCheckBox;
	}

	/**
	 * Updates the default check boxes for display. It is called when default
	 * check box is displayed and that is why we skip this check box.
	 */
	private void updateDefaultCheckBoxes() {
		for (JCheckBox checkBox : checkBoxPlots.keySet()) {
			String strText = checkBox.getText();
			if (!strText.equalsIgnoreCase(defaultViewCheckBoxText)) {
				boolean selected = defaultOptions.contains(checkBoxPlots.get(checkBox));
				checkBox.setSelected(selected);
			}
		}
	}

	/**
	 * Updates the video check boxes for display.
	 */
	private void updateVidPresetCheckBoxes() {
		for (JCheckBox checkBox : checkBoxPlots.keySet()) {
			String strText = checkBox.getText();
			if (!strText.equalsIgnoreCase(vidPresetCheckBoxText)) {
				boolean selected = ChartPlotOptions.getVideoDefaultView().contains(checkBoxPlots.get(checkBox));
				checkBox.setSelected(selected);
			}
		}
	}

	/**
	 * Updates the state of the check boxes on the dialog based on current user
	 * preferences.
	 */
	public void updateFromUserPreferences() {
		// grab the selected options from the user pref's file
		this.selectedOptions = guiPreferences.getChartPlotOptions();
		// loop on all check boxes and set selected status based on current/new
		// user pref's
		for (JCheckBox checkBox : checkBoxPlots.keySet()) {
			boolean selected = selectedOptions.contains(checkBoxPlots.get(checkBox));
			checkBox.setSelected(selected);
		}
		// enable all check boxes
		this.enableOptions(!selectedOptions.contains(ChartPlotOptions.DEFAULT_VIEW));
	}
} // @jve:decl-index=0:visual-constraint="132,38"
