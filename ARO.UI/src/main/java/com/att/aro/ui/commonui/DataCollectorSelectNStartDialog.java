/*
 * Copyright 2016 AT&T
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

package com.att.aro.ui.commonui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.video.pojo.Orientation;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.menu.datacollector.AttnrRadioGroupPanel;
import com.att.aro.ui.view.menu.datacollector.DeviceDialogOptions;
import com.att.aro.ui.view.menu.datacollector.DeviceTablePanel;
import com.att.aro.ui.view.menu.datacollector.HelpDialog;

/**
 * Represents the dialog that is used to start the ARO Data Collector. The
 * dialog prompts the user to enter a trace folder name, and starts the ARO Data
 * Collector on the device emulator when the Start button is clicked.
 */
public class DataCollectorSelectNStartDialog extends JDialog implements KeyListener{
	private static final Dimension PREFERRED_SIZE_LARGE = new Dimension(650, 575);
	private static final Dimension PREFERRED_SIZE_MEDIUM = new Dimension(650, 440);
	private static final Dimension PREFERRED_SIZE_SMALL = new Dimension(650, 400);
	private static final Dimension PREFERRED_SIZE_ULTRA = new Dimension(650, 675);

	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = LogManager.getLogger(DataCollectorSelectNStartDialog.class);	
	private static final int TRACE_FOLDER_ALLOWED_LENGTH = 50;

	private boolean proceed;
	
	private JPanel jContentPane;
	private JPanel buttonPanel;
	private JPanel jButtonGrid;
	private JButton startButton;
	private JButton cancelButton;
	private JPanel optionsPanel;
	private JPanel jAdvancedOptionsPanel;

	private JLabel traceFolderLabel;
	private JTextField traceFolderNameField;
	private DeviceTablePanel deviceTablePanel;
	private DeviceDialogOptions deviceOptionPanel;
	private ArrayList<IAroDevice> deviceList;

	private List<IDataCollector> collectors;
	private JPanel headerPanel;
	private JLabel lblNewLabel;

	private JLabel helpLabel;
	
	private SharedAttributesProcesses mainframeParent;

	/**
	 * Initializes a new instance of the DataCollectorStartDialog class using
	 * the specified instance of the ApplicationResourceOptimizer, and
	 * DatacollectorBridge.
	 * 
	 * @param owner
	 *            - The ApplicationResourceOptimizer instance.
	 * 
	 * @param aroDataCollectorBridge
	 *            - The DataCollectorBridge instance for capturing traces from a
	 *            device emulator.
	 * @wbp.parser.constructor
	 */
	public DataCollectorSelectNStartDialog(Frame owner) {
		this(owner, null, null, null, null, true);
	}

	/**
	 * Initializes a new instance of the DataCollectorStartDialog class using
	 * the specified instance of the ApplicationResourceOptimizer,
	 * DatacollectorBridge, trace folder name, and video flag.
	 * 
	 * @param owner
	 *            The ApplicationResourceOptimizer instance.
	 * 
	 * @param aroDataCollectorBridge
	 *            The DataCollectorBridge instance for capturing traces from a
	 *            device emulator.
	 * 
	 * @param traceFolderName
	 *            The name of the folder in which the ARO Data Collector trace
	 *            files should be stored.
	 * 
	 * @param recordVideo
	 *            A boolean value that indicates whether to record video for
	 *            this trace or not.
	 */
	public DataCollectorSelectNStartDialog(Frame owner, SharedAttributesProcesses mainframeParent, ArrayList<IAroDevice> deviceList, String traceFolderName, List<IDataCollector> collectors, boolean recordVideo) {
		super(owner);
		initialize(mainframeParent, deviceList, traceFolderName, collectors, recordVideo);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize(SharedAttributesProcesses mainframeParent, ArrayList<IAroDevice> deviceList, String traceFolderName, List<IDataCollector> collectors, boolean recordVideo) {
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendViews("StartCollectorWindow");
		this.setModal(true);
		this.setTitle(ResourceBundleHelper.getMessageString("dlog.collector.title"));
		
		this.setMainframeParent(mainframeParent);
		this.deviceList = deviceList;
		this.collectors = collectors;
		
		this.setContentPane(getJContentPane());
		this.pack();
		this.setLocationRelativeTo(getOwner());
		this.getRootPane().setDefaultButton(getStartButton());

		getJTraceFolderTextField().setText(traceFolderName);
		getJTraceFolderTextField().selectAll();
		
		deviceOptionPanel.showVideoOrientation(false);
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel(new BorderLayout());
			jContentPane.setBorder(new EmptyBorder(10,10,10,10));
			jContentPane.add(getDeviceSelectionPanel(), BorderLayout.PAGE_START);
			jContentPane.add(getTraceOptionsPanel(),BorderLayout.CENTER);
			jContentPane.add(getButtonPanel(), BorderLayout.PAGE_END);
		}
		return jContentPane;
	}
	
	public void resizeLarge() {
		jContentPane.setPreferredSize(PREFERRED_SIZE_LARGE);
		this.pack();
		this.setLocationRelativeTo(getOwner());
	}
	
	public void resizeMedium() {
		jContentPane.setPreferredSize(PREFERRED_SIZE_MEDIUM);
		this.pack();
		this.setLocationRelativeTo(getOwner());
	}
	
	public void resizeSmall() {
		jContentPane.setPreferredSize(PREFERRED_SIZE_SMALL);
		this.pack();
		this.setLocationRelativeTo(getOwner());
	}
	
	public void resizeUltra() {
		jContentPane.setPreferredSize(PREFERRED_SIZE_ULTRA);
		this.pack();
		this.setLocationRelativeTo(getOwner());
	}
	
	/**
	 * 
	 * @return JPanel
	 */
	private JPanel getDeviceSelectionPanel() {
		JPanel deviceSelectionPanel = new JPanel(new GridBagLayout());

		deviceTablePanel = getTablePanel(deviceList);
		deviceOptionPanel = getDeviceOptionPanel();

		deviceTablePanel.setSubscriber(deviceOptionPanel);
		deviceTablePanel.autoSelect();

		deviceOptionPanel.showVideoOrientation(true);
		
		Insets insets = new Insets(0, 0, 0, 0);
		deviceSelectionPanel.add(getHeaderPanel(),new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
		deviceSelectionPanel.add(deviceTablePanel,new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
		deviceSelectionPanel.add(deviceOptionPanel,new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
		
		return deviceSelectionPanel;
	}

	public IDataCollector getCollectorOption(){
		return deviceOptionPanel.getCollector();
	}
	
	public DeviceDialogOptions getDeviceOptionPanel() {
		if (deviceOptionPanel == null) {
			deviceOptionPanel = new DeviceDialogOptions(this, collectors);
			deviceOptionPanel.setVisible(true);
		}
		return deviceOptionPanel;
	}

	private DeviceTablePanel getTablePanel(ArrayList<IAroDevice> deviceList) {
		DeviceTablePanel deviceTablePanel = new DeviceTablePanel();
		deviceTablePanel.subscribe(this);
		deviceTablePanel.setData(deviceList);
		return deviceTablePanel;
	}
	
	/**
	 * This method initializes buttonPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(getJButtonGrid(), BorderLayout.CENTER);
		}
		return buttonPanel;
	}

	/**
	 * This method initializes jButtonGrid
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			jButtonGrid = new JPanel();
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			jButtonGrid.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
			jButtonGrid.add(getCancelButton());
			jButtonGrid.add(getStartButton());
		}
		return jButtonGrid;
	}

	/**
	 * This method initializes okButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getStartButton() {
		if (startButton == null) {
			startButton = new JButton();
			startButton.setText(ResourceBundleHelper.getMessageString("Button.start"));
			startButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					LOG.debug("Button.start");
					String traceFolderName = traceFolderNameField.getText();
					AttnrRadioGroupPanel radioGP = getDeviceOptionPanel().getAttnrGroup().getAttnrRadioGP();					
					if (radioGP.getSliderBtn().isSelected()) {
						boolean isAttenuateSelected = radioGP.getThroughputPanel().getDownloadCheckBox().isSelected()
								|| radioGP.getThroughputPanel().getUploadCheckBox().isSelected();
						if (!isAttenuateSelected) {
							LOG.info("need to set at least one DL or UL for the throttle");
							proceed = false;
					        showAttenuationError();
							return;
						}
					}	
					// don't allow whitespace
					traceFolderName = traceFolderName.replaceAll("\\s", "");					
					if (!traceFolderName.isEmpty()) {
						proceed = true;
						if (traceFolderNameField.getText() != null) {
							if (isContainsSpecialCharacterorSpace(traceFolderNameField.getText())) {
								JOptionPane.showMessageDialog(getCollectorDialogComponent()
															, ResourceBundleHelper.getMessageString("Error.specialchar")
															, MessageFormat.format(ResourceBundleHelper.getMessageString("aro.title.short"), 
																					ApplicationConfig.getInstance().getAppShortName())
															, JOptionPane.ERROR_MESSAGE);
								return;
							} else if (traceFolderNameField.getText().toString().length() > TRACE_FOLDER_ALLOWED_LENGTH) {
								JOptionPane.showMessageDialog(getCollectorDialogComponent()
															, ResourceBundleHelper.getMessageString("Error.tracefolderlength")
															, MessageFormat.format(ResourceBundleHelper.getMessageString("aro.title.short"), 
																					ApplicationConfig.getInstance().getAppShortName())
															, JOptionPane.ERROR_MESSAGE);
								return;
							} else {
								DataCollectorSelectNStartDialog.this.dispose();
							}
						}
					} else {
						LOG.info("traceFolderName is blank");
						proceed = false;
						JOptionPane.showMessageDialog(getCollectorDialogComponent()
								, ResourceBundleHelper.getMessageString("Error.tracefolderempty")
								, MessageFormat.format(ResourceBundleHelper.getMessageString("aro.title.short"), 
														ApplicationConfig.getInstance().getAppShortName())
								, JOptionPane.ERROR_MESSAGE);					
						traceFolderNameField.requestFocus();
					} 
				}
			});
			if (deviceTablePanel.getSelection() != null && traceFolderNameField != null){
				startButton.setEnabled(true);
			} else {
				startButton.setEnabled(false);
			}
		}
		return startButton;
	}
	

	private DataCollectorSelectNStartDialog getCollectorDialogComponent(){
		return this;
	}

	private void showAttenuationError() {
		JOptionPane pane = new JOptionPane();
		pane.setMessage(ResourceBundleHelper.getMessageString("Error.checkbox.attenuator"));
		pane.setMessageType(JOptionPane.ERROR_MESSAGE);
		JDialog dialog = pane.createDialog(
				MessageFormat.format(ResourceBundleHelper.getMessageString("aro.title.short"),
				ApplicationConfig.getInstance().getAppShortName())); 
		dialog.setAlwaysOnTop(true); 
		dialog.setVisible(true);
	}

	public void enableStart(boolean flag) {
		if (flag == false 
				|| (startButton != null 
					&& !traceFolderNameField.getText().isEmpty()
					&& deviceTablePanel.getSelection() != null)) {
			
			startButton.setEnabled(flag);
			startButton.setSelected(false);
		}
	}

	/**
	 * This method initializes cancelButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(ResourceBundleHelper.getMessageString("Button.cancel"));
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					proceed = false;
					LOG.debug(ResourceBundleHelper.getMessageString("Button.cancel"));
					DataCollectorSelectNStartDialog.this.dispose();
				}
			});
		}
		return cancelButton;
	}

	/**
	 * This method initializes optionsPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getTraceOptionsPanel() {
		if (optionsPanel == null) {
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0D;
			gridBagConstraints.weighty = 1.0D;
			gridBagConstraints.gridy = 0;
			optionsPanel = new JPanel();
			optionsPanel.setLayout(new GridBagLayout());
			optionsPanel.add(getTraceNamePanel(), gridBagConstraints);
		}
		return optionsPanel;
	}

	/**
	 * This method initializes jAdvancedOptionsPanel
	 * <pre>
	 * Provides 
	 *   1 Label
	 *   2 TextField to enter trace folder name
	 *   3 Record Video check box
	 * </pre>
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getTraceNamePanel() {
		if (jAdvancedOptionsPanel == null) {
			jAdvancedOptionsPanel = new JPanel();
			jAdvancedOptionsPanel.setBorder(BorderFactory.createTitledBorder(null
																			, ""
																			, TitledBorder.DEFAULT_JUSTIFICATION
																			, TitledBorder.DEFAULT_POSITION
																			, new Font("Dialog", Font.BOLD, 12)
																			, new Color(51, 51, 51)));
			jAdvancedOptionsPanel.setLayout(new GridLayout(0, 1, 0, 0));
			jAdvancedOptionsPanel.add(getJTraceFolderLabel());
			jAdvancedOptionsPanel.add(getJTraceFolderTextField());
		}
		return jAdvancedOptionsPanel;
	}

	/**
	 * This method initializes TraceFolderLabel
	 */
	private JLabel getJTraceFolderLabel() {
		if (traceFolderLabel == null) {
			traceFolderLabel = new JLabel(ResourceBundleHelper.getMessageString("collector.folder"), SwingConstants.LEFT);
			traceFolderLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 14));
		}
		return traceFolderLabel;
	}

	private JTextField getJTraceFolderTextField() {
		if (traceFolderNameField == null) {
			traceFolderNameField = new JTextField(25);
		}
		traceFolderNameField.addKeyListener(this);
		traceFolderNameField.setActionCommand("TraceFolderName");
		return traceFolderNameField;
	}


	private boolean isContainsSpecialCharacterorSpace(String tracefolername) {
		boolean isContainsSC = false;
		if (tracefolername != null && !tracefolername.equals("")) {
			// Pattern to include alphanumeric with "-"
			Matcher m = Pattern.compile("[^a-zA-Z0-9-_]").matcher(tracefolername);
			if (m.find()) {
				isContainsSC = true;
			} else {
				isContainsSC = false;
			}
		} else {
			isContainsSC = true;
		}

		return isContainsSC;

	}

	/**
	 * Activate this dialog
	 * @return true if "Start" is selected, false if "Cancel"
	 */
	public boolean getResponse() {
		this.setVisible(true);
		if (proceed) {
			LOG.debug("tracefolder :" + traceFolderNameField.getName() + " video :" + (getRecordVideo() ? "checked" : "unchecked"));
		}
		return proceed;
	}

	/**
	 * @return true if video if not VideoOption.NONE
	 */
	public boolean getRecordVideo() {
		return (!getRecordVideoOption().equals(VideoOption.NONE));
	}

	/**
	 * @return true if video checkbox is checked, false if not
	 */
	public VideoOption getRecordVideoOption() {
		return deviceOptionPanel.getVideoOption();
	}

	public Orientation getVideoOrientation() {
		return deviceOptionPanel.getVideoOrientation();
	}
	
	/**
	 * @return traceFolderName - name of the trace folder
	 */
	public String getTraceFolder() {
		return traceFolderNameField.getText();
	}

	public IAroDevice getDevice() {
		return deviceTablePanel.getSelection();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		enableStart(true);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		//ignore
	}

	@Override
	public void keyReleased(KeyEvent e) {
		//ignore
	}

	private JPanel getHeaderPanel() {
		if (headerPanel == null) {
			headerPanel = new JPanel();
			headerPanel.setLayout(new BorderLayout(0, 0));
			headerPanel.add(getLblNewLabel());
			headerPanel.add(getHelpLabel(), BorderLayout.EAST);
		}
		return headerPanel;
	}
	private JLabel getLblNewLabel() {
		if (lblNewLabel == null) {
			lblNewLabel = new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.selectdevice"));
			lblNewLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 15));
		}
		return lblNewLabel;
	}

	private JLabel getHelpLabel() {
		if (helpLabel == null) {
			String resourceName = ResourceBundleHelper.getImageString("ImageBasePath")
					+ ResourceBundleHelper.getImageString("Image.bpHelpDark");
			ImageIcon imgIcon = new ImageIcon(getClass().getResource(resourceName));
			helpLabel = new JLabel(imgIcon);
			helpLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					new HelpDialog(DataCollectorSelectNStartDialog.this);

				}
			});
		}
		return helpLabel;
	}

	public SharedAttributesProcesses getMainframeParent() {
		return mainframeParent;
	}

	public void setMainframeParent(SharedAttributesProcesses mainframeParent) {
		this.mainframeParent = mainframeParent;
	}

}
