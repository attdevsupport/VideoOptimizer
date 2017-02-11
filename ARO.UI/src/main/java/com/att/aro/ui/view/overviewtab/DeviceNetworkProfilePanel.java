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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.model.overview.TraceInfo;
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
	
	private JLabel downlinkLabel;
	private JLabel uplinkLabel;
	
	private JLabel downlinkValueLabel;
	private JLabel uplinkValueLabel;

	private static final Font LABEL_FONT = new Font("TEXT_FONT", Font.BOLD, 12);
	private static final Font TEXT_FONT = new Font("TEXT_FONT", Font.PLAIN, 12);

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

		final int gridX = 7;
		final double wightX = 0.5;

		JPanel dataPanel  = new JPanel(new GridBagLayout());
		
		dateValueLabel = new JLabel();
		dateValueLabel.setFont(TEXT_FONT);
		dateValueLabel.setHorizontalTextPosition(JLabel. TRAILING);

		traceValueLabel = new JLabel();
		traceValueLabel.setFont(TEXT_FONT);
		traceValueLabel.setHorizontalTextPosition(JLabel. TRAILING);

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
	
	public void setData(TraceInfo info){
		this.dateValueLabel.setText(info.getDateValue());
		this.traceValueLabel.setText(info.getTraceValue());
		this.byteCountTotalLabel.setText(info.getByteCountTotal().toString());
		this.profileValueLabel.setText(info.getProfileValue());
		this.networkTypeValueLabel.setText(info.getNetworkType());
	}
	
	public void refresh(AROTraceData aModel){
		
		TraceInfo tInfo = new TraceInfo();
		tInfo.setDateValue(aModel.getAnalyzerResult().getTraceresult().getTraceDateTime().toString());
		tInfo.setTraceValue(aModel.getAnalyzerResult().getTraceresult().getTraceDirectory());
		tInfo.setByteCountTotal(aModel.getAnalyzerResult().getStatistic().getTotalByte());
		tInfo.setProfileValue(aModel.getAnalyzerResult().getProfile().getName());
		if(aModel.getAnalyzerResult().getTraceresult().getTraceResultType().equals(TraceResultType.TRACE_DIRECTORY)){
			TraceDirectoryResult tracedirectoryResult = (TraceDirectoryResult)aModel.getAnalyzerResult().getTraceresult();
			tInfo.setNetworkType(tracedirectoryResult.getNetworkTypesList());			
		}
		
		setData(tInfo);
	}

}
