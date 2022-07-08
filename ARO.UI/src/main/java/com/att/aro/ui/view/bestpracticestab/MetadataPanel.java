/*
 *  Copyright 2021 AT&T
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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.AroFonts;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.view.MainFrame;

public class MetadataPanel extends AbstractBpPanel {
	private static final long serialVersionUID = 1L;
	
	private JLabel metadataHeaderLabel = new JLabel();	
	private JLabel traceTypeLabel = new JLabel();
	private JLabel deviceOrientationLabel = new JLabel();
	private JLabel targetedAppLabel = new JLabel();
	private JLabel applicationProducerLabel = new JLabel();
	private JLabel traceSourceLabel = new JLabel();
	private JLabel simLabel = new JLabel();
	private JLabel networkLabel = new JLabel();
	private JLabel traceOwnerLabel = new JLabel();
	private JPanel textAreaWrapper = new JPanel();
	private Insets insets = new Insets(2, 2, 2, 2);
	private final double weightX = 0.5;
	private IMetaDataHelper metaDataHelper = ContextAware.getAROConfigContext().getBean(IMetaDataHelper.class);

	/***
	 * Create labels and set font
	 * @param aroView 
	 */
	public MetadataPanel(MainFrame aroView) {
		
		add(layoutDataPanel(), BorderLayout.NORTH);
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
	}

	@Override
	public int print(Graphics arg0, PageFormat arg1, int arg2) throws PrinterException {
		return 0;
	}

	/***
	 * Add Text to labels and define the attributes
	 */
	@Override
	public JPanel layoutDataPanel() {
		if (dataPanel == null) {
			dataPanel = new JPanel(new GridBagLayout());
			dataPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY);
			dataPanel.setBackground(Color.WHITE);
			int idx = 0;
			
			addLabelLineName(metadataHeaderLabel        , "bestPractices.mdata.header"         ,   idx ,2, weightX, insets, AroFonts.HEADER_FONT);  //
			addLabelLineName(traceTypeLabel				, "bestPractices.mdata.traceType"			, ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(deviceOrientationLabel		, "bestPractices.mdata.deviceOrientation"	, ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(targetedAppLabel			, "bestPractices.mdata.targetedApp"			, ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(applicationProducerLabel	, "bestPractices.mdata.applicationProducer" , ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(traceSourceLabel			, "bestPractices.mdata.traceSource"			, ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(simLabel					, "bestPractices.mdata.sim"					, ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(networkLabel				, "bestPractices.mdata.network"				, ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(traceOwnerLabel			, "bestPractices.mdata.traceOwner"			, ++idx, 2, weightX, insets, AroFonts.TEXT_FONT, AroFonts.TEXT_FONT);
		}
		return dataPanel;
	}

	
	private void addLabelLineName(JLabel infoLabel, String labelText, int gridy, int width, double weightx, Insets insets, Font labelFont, Font dataFont) {
		addLabelLineName(infoLabel, labelText, gridy, width, weightx, insets, labelFont);
		infoLabel.setFont(dataFont);
	}

	/***
	 * This method is called to check when VO refreshes(Traceload/Open VO) Based
	 * on the flow load the values or empty text
	 */
	@Override
	public void refresh(AROTraceData model) {
		PacketAnalyzerResult analyzerResults = model.getAnalyzerResult();
		AbstractTraceResult traceResults = analyzerResults.getTraceresult();

		if (traceResults != null) {
			try {
				loadMetadataPanel(traceResults.getMetaData());
			} catch (Exception e) {
				clearDirResults();
			}
		}
	}

	/***
	 * Loads value from the metadata object
	 * 
	 * @param metaDataModel
	 */
	private void loadMetadataPanel(MetaDataModel metaDataModel) {
		traceTypeLabel.setText(metaDataModel.getTraceType());
		deviceOrientationLabel.setText(metaDataModel.getDeviceOrientation());
		targetedAppLabel.setText(metaDataModel.getTargetedApp());
		applicationProducerLabel.setText(metaDataModel.getApplicationProducer());
		traceSourceLabel.setText(metaDataModel.getTraceSource());
		simLabel.setText(metaDataModel.getSim());
		networkLabel.setText(metaDataModel.getNetWork());
		traceOwnerLabel.setText(metaDataModel.getTraceOwner());
	}

	

	/***
	 * Set empty values during VO initial load
	 */
	private void clearDirResults() {	
		
		traceTypeLabel.setText("");
		deviceOrientationLabel.setText("");
		targetedAppLabel.setText("");
		applicationProducerLabel.setText("");
		traceSourceLabel.setText("");
		simLabel.setText("");
		networkLabel.setText("");
		traceOwnerLabel.setText("");
		textAreaWrapper.setVisible(false);
	}
}
