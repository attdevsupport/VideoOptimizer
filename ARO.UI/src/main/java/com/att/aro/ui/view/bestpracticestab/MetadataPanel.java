package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.ui.commonui.ContextAware;

public class MetadataPanel extends AbstractBpPanel {
	private static final long serialVersionUID = 1L;
	JLabel emptySpaceLabel;
	JLabel descriptionLabel;
	JLabel traceTypeLabel;
	JLabel deviceOrientationLabel;
	JLabel targetedAppLabel;
	JLabel applicationProducerLabel;
	JLabel traceSourceLabel;
	JLabel traceOwnerLabel;
	
	private Insets insets = new Insets(2, 2, 2, 2);
	private final double weightX = 0.5;
	private IMetaDataHelper metaDataHelper = ContextAware.getAROConfigContext().getBean(IMetaDataHelper.class);

	/***
	 * Create labels and set font
	 */
	public MetadataPanel() {
		emptySpaceLabel = new JLabel();
		descriptionLabel = new JLabel();
		traceTypeLabel = new JLabel();
		deviceOrientationLabel = new JLabel();
		targetedAppLabel = new JLabel();
		applicationProducerLabel = new JLabel();
		traceSourceLabel = new JLabel();
		traceOwnerLabel = new JLabel();
		
		descriptionLabel.setFont(TEXT_FONT);
		traceTypeLabel.setFont(TEXT_FONT);
		deviceOrientationLabel.setFont(TEXT_FONT);
		targetedAppLabel.setFont(TEXT_FONT);
		applicationProducerLabel.setFont(TEXT_FONT);
		traceSourceLabel.setFont(TEXT_FONT);
		traceOwnerLabel.setFont(TEXT_FONT);
		
		add(layoutDataPanel(), BorderLayout.NORTH);
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
			dataPanel.setBackground(Color.WHITE);// UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			int idx = 0;
			addLabelLineName(emptySpaceLabel, " ", ++idx, 2, weightX, insets, TEXT_FONT);
			addLabelLineName(descriptionLabel, "bestPractices.mdata.description", ++idx, 2, weightX, insets, TEXT_FONT);
			addLabelLineName(traceTypeLabel, "bestPractices.mdata.traceType", ++idx, 2, weightX, insets, TEXT_FONT);
			addLabelLineName(deviceOrientationLabel, "bestPractices.mdata.deviceOrientation", ++idx, 2, weightX, insets,
					TEXT_FONT);
			addLabelLineName(targetedAppLabel, "bestPractices.mdata.targetedApp", ++idx, 2, weightX, insets, TEXT_FONT);
			addLabelLineName(applicationProducerLabel, "bestPractices.mdata.applicationProducer", ++idx, 2, weightX,
					insets, TEXT_FONT);
			addLabelLineName(traceSourceLabel, "bestPractices.mdata.traceSource", ++idx, 2, weightX, insets, TEXT_FONT);
			addLabelLineName(traceOwnerLabel, "bestPractices.mdata.traceOwner", ++idx, 2, weightX, insets, TEXT_FONT);
		}
		return dataPanel;
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
		descriptionLabel.setText(metaDataModel.getDescription());
		traceTypeLabel.setText(metaDataModel.getTraceType());
		deviceOrientationLabel.setText(metaDataModel.getDeviceOrientation());
		targetedAppLabel.setText(metaDataModel.getTargetedApp());
		applicationProducerLabel.setText(metaDataModel.getApplicationProducer());
		traceSourceLabel.setText(metaDataModel.getTraceSource());
		traceOwnerLabel.setText(metaDataModel.getTraceOwner());
		}

	/***
	 * Set empty values during VO initial load
	 */
	private void clearDirResults() {
		descriptionLabel.setText("");
		traceTypeLabel.setText("");
		deviceOrientationLabel.setText("");
		targetedAppLabel.setText("");
		applicationProducerLabel.setText("");
		traceSourceLabel.setText("");
		traceOwnerLabel.setText("");
	}
}
