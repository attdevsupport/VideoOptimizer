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
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.NetworkType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoTabProfilePanel extends TabPanelJPanel {
	public VideoTabProfilePanel() {
	}
	private static final long serialVersionUID = 1L;
	  
	private JLabel dateValueLabel;
	private JLabel traceValueLabel;
	
	private JLabel oSValueLabel;
	private JLabel networkTypeValueLabel;
		
	private JLabel deviceValueLabel;
	private JLabel traceDurationValueLabel;

	private static final Font LABEL_FONT = new Font("TEXT_FONT", Font.BOLD, 12);
	private static final Font TEXT_FONT = new Font("TEXT_FONT", Font.PLAIN, 12);
	
	private static final Logger LOG = LogManager.getLogger(VideoTabProfilePanel.class.getName());

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
		
		dateValueLabel = createValueLabel();
		traceValueLabel = createValueLabel();
		networkTypeValueLabel = createValueLabel();
		oSValueLabel = createValueLabel();
		deviceValueLabel = createValueLabel();
		traceDurationValueLabel = createValueLabel();
		
		this.traceValueLabel.addMouseListener(getOpenFolderAdapter());
		this.traceValueLabel.setToolTipText(ResourceBundleHelper.getMessageString("trace.hyperlink"));
		this.traceValueLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
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
		dataPanel.add(networkTypeLabel, new GridBagConstraints(4, 0, 1, 1, wightX,
				0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insets,
				0, 0));
		dataPanel.add(networkTypeValueLabel, new GridBagConstraints(5, 0,
				1, 1, wightX, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, insets, 0, 0));
		
		JLabel oSLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractices.os.version"),
				JLabel. TRAILING);
		oSLabel.setFont(LABEL_FONT);
		dataPanel.add(oSLabel, new GridBagConstraints(6, 0, 1, 1, wightX,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));
		dataPanel.add(oSValueLabel,	 new GridBagConstraints(7, 0, 1, 1, wightX,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));
		
		JLabel traceLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractices.trace"));
		
		traceLabel.setFont(LABEL_FONT);
		dataPanel.add(traceLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
						insets, 0, 0));
		
		dataPanel.add(traceValueLabel, new GridBagConstraints(1, 1, 3, 1,
				0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, insets, 0, 0));//longer
		
		JLabel deviceLabel = new JLabel(
				ResourceBundleHelper.getMessageString("bestPractices.devicemodel"),
				JLabel.TRAILING);
		deviceLabel.setFont(LABEL_FONT);
		dataPanel.add(deviceLabel, new GridBagConstraints(4, 1, 1, 1, wightX,
				0.0, // Change made here 2 to 4
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));

		dataPanel.add(deviceValueLabel,	 new GridBagConstraints(5, 1, 1, 1, wightX,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				insets, 0, 0));
		

		JLabel traceDurationLabel = new JLabel(
				ResourceBundleHelper.getMessageString("overview.info.bytecounttotal"),JLabel.RIGHT);
		traceDurationLabel.setFont(LABEL_FONT);
		dataPanel.add(traceDurationLabel, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE,
						insets, 10, 0));
		dataPanel.add(traceDurationValueLabel, new GridBagConstraints(7, 1, 1, 1,
				0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				insets, 0, 0));
		
		return dataPanel;
	}
	
	private JLabel createValueLabel() {
		JLabel valueLabel = new JLabel();
		valueLabel.setFont(TEXT_FONT);
		valueLabel.setHorizontalTextPosition(JLabel. TRAILING);

		return valueLabel;
	}

	public void refresh(AROTraceData aModel){
		this.dateValueLabel.setText(aModel.getAnalyzerResult().getTraceresult().getTraceDateTime().toString());
		
		File directory = new File(aModel.getAnalyzerResult().getTraceresult().getTraceDirectory());
		this.traceValueLabel.setText(getTracePath(directory.getName()));


		this.traceDurationValueLabel.setText(Long.toString(aModel.getAnalyzerResult().getStatistic().getTotalByte()));
		

		if (TraceResultType.TRACE_DIRECTORY.equals(aModel.getAnalyzerResult().getTraceresult().getTraceResultType())) {
			TraceDirectoryResult traceDirectoryResult = (TraceDirectoryResult)aModel.getAnalyzerResult().getTraceresult();
			this.networkTypeValueLabel.setText(traceDirectoryResult.getNetworkTypesList()!=null ? getNetworkTypeList(traceDirectoryResult.getNetworkTypesList()) : "" );
			this.oSValueLabel.setText(traceDirectoryResult.getOsType()+ " / " +traceDirectoryResult.getOsVersion());
			this.deviceValueLabel.setText(traceDirectoryResult.getDeviceMake() + " / " + traceDirectoryResult.getDeviceModel());
			
		} else {
			this.networkTypeValueLabel.setText("");
			this.oSValueLabel.setText("");
			this.deviceValueLabel.setText("");
			
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
	
	private String getNetworkTypeList(List<NetworkType> networkTypesList) {			

		if (networkTypesList != null && !networkTypesList.isEmpty()) {
			StringBuffer networksList = new StringBuffer();
			for (NetworkType networkType : networkTypesList) {
				if (networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO)
						|| networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_LTE_CA)
						|| networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_NR_NSA)
						|| networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE)) {
					networksList.append(ResourceBundleHelper.getMessageString("NetworkType." + networkType.toString()));
				} else {
					networksList.append(networkType.toString());
				}

				networksList.append(" , ");
			}
			return networksList.toString().substring(0, networksList.toString().lastIndexOf(","));
		} else {
			return "";
		}
	
	}
}
