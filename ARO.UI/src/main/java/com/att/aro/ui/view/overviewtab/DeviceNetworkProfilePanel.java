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
package com.att.aro.ui.view.overviewtab;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.CollectOptions;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.utils.CommonHelper;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class DeviceNetworkProfilePanel extends TabPanelJPanel {
	public DeviceNetworkProfilePanel() {
	}
	private static final long serialVersionUID = 1L;
	  
	private JLabel dateValueLabel;
	private JLabel traceValueLabel;
	private JLabel byteCountTotalLabel; // GregStory

	private JLabel networkTypeValueLabel;
	private JLabel profileValueLabel;
		
	private JLabel downlinkValueLabel;
	private JLabel uplinkValueLabel;
	private JLabel downlinkLabel;
	private JLabel uplinkLabel;

	private static final Font LABEL_FONT = new Font("TEXT_FONT", Font.BOLD, 12);
	private static final Font TEXT_FONT = new Font("TEXT_FONT", Font.PLAIN, 12);
	
	private static final Logger LOG = LogManager.getLogger(DeviceNetworkProfilePanel.class.getName());

	public JPanel layoutDataPanel() {
		final int borderGap = 10;
		setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
		setLayout(new BorderLayout(borderGap, borderGap));
		
		add(getDataPanel(), BorderLayout.CENTER);
		
		return this;
	}

	/**
	 * Creates the JPanel containing the Date , Trace, network profile and
	 * profile name.
	 * 
	 * @return the dataPanel
	 */
	private JPanel getDataPanel() {
		final double wightX = 0.5;

		JPanel dataPanel  = new JPanel(new GridBagLayout());
		
		dateValueLabel = new JLabel();
		dateValueLabel.setFont(TEXT_FONT);
		dateValueLabel.setHorizontalTextPosition(JLabel. TRAILING);

		traceValueLabel = new JLabel();
		traceValueLabel.setFont(TEXT_FONT);
		traceValueLabel.setHorizontalTextPosition(JLabel. TRAILING);

		traceValueLabel.addMouseListener(getOpenFolderAdapter());
		traceValueLabel.setToolTipText(ResourceBundleHelper.getMessageString("trace.hyperlink"));
		traceValueLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		byteCountTotalLabel = new JLabel();
		byteCountTotalLabel.setFont(TEXT_FONT);
		byteCountTotalLabel.setHorizontalTextPosition(JLabel. TRAILING);

		networkTypeValueLabel = new JLabel();
		networkTypeValueLabel.setFont(TEXT_FONT);
		networkTypeValueLabel.setHorizontalTextPosition(JLabel. TRAILING);

		profileValueLabel = new JLabel();
		profileValueLabel.setFont(TEXT_FONT);
		profileValueLabel.setHorizontalTextPosition(JLabel. TRAILING);

		downlinkValueLabel = new JLabel();
		downlinkValueLabel.setHorizontalTextPosition(JLabel. TRAILING);
        downlinkValueLabel.setFont(TEXT_FONT);

        uplinkValueLabel = new JLabel();
        uplinkValueLabel.setHorizontalTextPosition(JLabel. TRAILING);        
        uplinkValueLabel.setFont(TEXT_FONT);

		Insets insets = new Insets(1, 1, 1, 1);
		JLabel dateLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractices.date"));
		dateLabel.setFont(LABEL_FONT);
		
		dataPanel.add(dateLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
						insets, 0, 0));

		dataPanel.add(dateValueLabel, new GridBagConstraints(1, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.PAGE_START, GridBagConstraints.NONE,
				insets, 0, 0));
		
		downlinkLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.downlink"),
				JLabel. TRAILING);
		
		dataPanel.add(downlinkLabel, new GridBagConstraints(2, 0, 1, 1, wightX,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));
		dataPanel.add(downlinkValueLabel,	 new GridBagConstraints(3, 0, 1, 1, wightX,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));

		uplinkLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.uplink"),
				JLabel.TRAILING);
		
		dataPanel.add(uplinkLabel, new GridBagConstraints(4, 0, 1, 1, wightX,
				0.0, // Change made here 2 to 4
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));

		dataPanel.add(uplinkValueLabel,	 new GridBagConstraints(5, 0, 1, 1, wightX,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));
		
		JLabel networkTypeLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractices.networktype"),
				JLabel.RIGHT);
		networkTypeLabel.setFont(LABEL_FONT);
		dataPanel.add(networkTypeLabel, new GridBagConstraints(6, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insets,
				0, 0));
		dataPanel.add(networkTypeValueLabel, new GridBagConstraints(7, 0,
				1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, insets, 0, 0));
		JLabel traceLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractices.trace"));
		
		traceLabel.setFont(LABEL_FONT);
		dataPanel.add(traceLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
						insets, 0, 0));
		
		dataPanel.add(traceValueLabel, new GridBagConstraints(1, 1, 3, 1,
				0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, insets, 0, 0));//longer

		JLabel countLabel = new JLabel(
				ResourceBundleHelper.getMessageString("overview.info.bytecounttotal"),JLabel.RIGHT);
		countLabel.setFont(LABEL_FONT);
		dataPanel.add(countLabel, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE,
						insets, 10, 0));
		dataPanel.add(byteCountTotalLabel, new GridBagConstraints(5, 1, 1, 1,
				0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				insets, 0, 0));

		JLabel profileLabel = new JLabel(
				ResourceBundleHelper.getMessageString("overview.info.profile"),
				JLabel.RIGHT);
		profileLabel.setFont(LABEL_FONT);
		dataPanel.add(profileLabel, new GridBagConstraints(6, 1, 1, 1, 0.0,
						0.0, // Change made here 2 to 4
						GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
						insets, 0, 0));
		dataPanel.add(profileValueLabel, new GridBagConstraints(7, 1, 1, 1,
				0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));
		
		return dataPanel;
	}
	
	public void refresh(AROTraceData aModel){
		this.dateValueLabel.setText(aModel.getAnalyzerResult().getTraceresult().getTraceDateTime().toString());

		File directory = new File(aModel.getAnalyzerResult().getTraceresult().getTraceDirectory());
		this.traceValueLabel.setText(getTracePath(directory.getName()));

		this.byteCountTotalLabel.setText(Long.toString(aModel.getAnalyzerResult().getStatistic().getTotalByte()));
		this.profileValueLabel.setText(aModel.getAnalyzerResult().getProfile().getName());

		if (TraceResultType.TRACE_DIRECTORY.equals(aModel.getAnalyzerResult().getTraceresult().getTraceResultType())) {
			TraceDirectoryResult tracedirectoryResult = (TraceDirectoryResult)aModel.getAnalyzerResult().getTraceresult();
			this.networkTypeValueLabel.setText(tracedirectoryResult.getNetworkTypesList());
			CollectOptions collectOptions = tracedirectoryResult.getCollectOptions();
			if (collectOptions != null) {
				if(collectOptions.isAttnrProfile()){
 					this.downlinkLabel.setText("Attenuation Profile: ");
					this.downlinkValueLabel.setText(collectOptions.getAttnrProfileName());
					this.uplinkLabel.setVisible(false);
					this.uplinkValueLabel.setVisible(false);
				}else{
					int dsDelay = collectOptions.getDsDelay();
					int usDelay = collectOptions.getUsDelay();
					int speedThrottleDL = collectOptions.getThrottleDL();
					int speedThrottleUL = collectOptions.getThrottleUL();
					this.downlinkLabel.setText(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.downlink"));
					this.uplinkLabel.setText(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.uplink"));
					this.uplinkLabel.setVisible(true);
					this.uplinkValueLabel.setVisible(true);
					CommonHelper attenuator = new CommonHelper();
					NumberFormat numFormat =  NumberFormat.getIntegerInstance();
					if(dsDelay > 0 || usDelay > 0){
						this.downlinkValueLabel.setText(attenuator.transferSignalSignDownload(dsDelay) + " - " + numFormat.format(dsDelay) + " ms");
						this.uplinkValueLabel.setText(attenuator.transferSignalSignUpload(usDelay) + " - " + numFormat.format(usDelay) + " ms");
					}else{
						//check dl and ul
						if(speedThrottleDL < 0){
							this.downlinkValueLabel.setText(ResourceBundleHelper.getMessageString("waterfall.na"));
						}else{
							this.downlinkValueLabel.setText(attenuator.numberTransferSignalDL(speedThrottleDL) + 
									" - " + attenuator.messageConvert(speedThrottleDL));
						}
						
						if(speedThrottleUL < 0 ){
							this.uplinkValueLabel.setText(ResourceBundleHelper.getMessageString("waterfall.na"));
						}else{
							this.uplinkValueLabel.setText(attenuator.numberTransferSignalUL(speedThrottleUL) + 
									" - " + attenuator.messageConvert(speedThrottleUL));
						}
					}
 
				}
			} 
		} else {
			this.networkTypeValueLabel.setText("");
			this.downlinkLabel.setText(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.downlink"));
			this.uplinkLabel.setText(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.uplink"));
			this.uplinkLabel.setVisible(true);
			this.uplinkValueLabel.setVisible(true);
			this.downlinkValueLabel.setText("");
			this.uplinkValueLabel.setText("");
		}
	}

	private String getTracePath(String traceDirectory) {
		StringBuilder tracePath = new StringBuilder();
		tracePath.append("<html><a href=\"#\">");
		tracePath.append(traceDirectory);
		tracePath.append("</a></html>");
		return tracePath.toString();
	}
	
	private MouseAdapter getOpenFolderAdapter() {
		MouseAdapter openFolderAction = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Desktop desktop = null;
				if (Desktop.isDesktopSupported()) {
					desktop = Desktop.getDesktop();
					try {
						File traceFile = new File(PreferenceHandlerImpl.getInstance().getPref("TRACE_PATH"));
						if (traceFile != null && traceFile.exists()) {
							if (traceFile.isDirectory()) {
								desktop.open(traceFile);
							} else {
								desktop.open(traceFile.getParentFile());
							}
						}
					} catch (IOException ex) {
						LOG.error("Error opening the Trace Folder : " +ex.getMessage());
					}
				}
			}
		};
		return openFolderAction;
	}
}
