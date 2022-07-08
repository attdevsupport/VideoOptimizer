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
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.AroFonts;
import com.att.aro.ui.commonui.ContextAware;

public class TraceInfoPanel extends AbstractBpPanel {
	private static final long serialVersionUID = 1L;
	
	private JLabel traceStorageLabel = new JLabel();
	private JLabel descriptionLabel = new JLabel();
	
	
	private Insets insets = new Insets(1, 1, 1, 1);
	private final double weightX = 0.5;
	private IMetaDataHelper metaDataHelper = ContextAware.getAROConfigContext().getBean(IMetaDataHelper.class);

	private MetaDataModel metaDataModel;


	/***
	 * Create labels and set font
	 * @param aroView 
	 */
	public TraceInfoPanel() {
		add(layoutDataPanel());
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
			addLabelLineName(traceStorageLabel			, "bestPractices.mdata.traceStorage"		, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);
			addLabelLineName(descriptionLabel			, "bestPractices.mdata.description"			, ++idx, 2, weightX, insets, AroFonts.LABEL_FONT, AroFonts.TEXT_FONT);					
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
		AbstractTraceResult traceResults = model.getAnalyzerResult().getTraceresult();	
		if (traceResults != null) {
			if (traceResults.getTraceResultType() == TraceResultType.TRACE_DIRECTORY && metaDataHelper != null) {
				try {
					loadMetadataPanel(((TraceDirectoryResult)traceResults).getMetaData());
				} catch (Exception e) {
					clearDirResults();
				}
			} else {
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
		this.metaDataModel = metaDataModel;
		traceStorageLabel.setText(metaDataModel.getTraceStorage());
		descriptionLabel.setText(metaDataModel.getDescription());
	}
	
	/***
	 * Set empty values during VO initial load
	 */
	private void clearDirResults() {	
		traceStorageLabel.setText("");
		descriptionLabel.setText("");
	}
}
