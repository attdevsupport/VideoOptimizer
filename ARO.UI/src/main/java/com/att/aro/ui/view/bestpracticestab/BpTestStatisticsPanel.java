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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.CollectOptions;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.utils.CommonHelper;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class BpTestStatisticsPanel extends AbstractBpPanel {
	private static final long serialVersionUID = 1L;

	private JLabel summaryHeaderLabel;
	private JLabel statisticsHeaderLabel;
	private JLabel httpsDataNotAnalyzedLabel;
	private JLabel durationLabel;
	private JLabel totalDataLabel;
	private JLabel energyConsumedLabel;
	private JLabel summaryFillerHeaderLabel;
	private JLabel testFillerHeaderLabel;
	
	private JLabel attenuatorHeaderLabel;
	private JLabel downlinkLabel;
	private JLabel uplinkLabel;

	private NumberFormat pctFmt = null;
	private NumberFormat intFormat = null;
	private NumberFormat numFormat;
	private DecimalFormat decFormat;
	private Insets insets = new Insets(2, 2, 2, 2);
	private final double weightX = 0.5;

	public BpTestStatisticsPanel() {
		
		final int borderGap = 10;

		this.setLayout(new BorderLayout(borderGap, borderGap));
	//	this.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
		this.setBackground(Color.WHITE);
		this.setBorder(BorderFactory.createEmptyBorder(0, 0, borderGap, 0));

		pctFmt  = NumberFormat.getPercentInstance();
		pctFmt.setMaximumFractionDigits(2);
		intFormat = NumberFormat.getIntegerInstance();
		numFormat = NumberFormat.getNumberInstance();
		decFormat = new DecimalFormat("#.##");
		
	    summaryHeaderLabel             = new JLabel();
        statisticsHeaderLabel          = new JLabel();

        attenuatorHeaderLabel		   = new JLabel();
        downlinkLabel				   = new JLabel();
        uplinkLabel					   = new JLabel();
        
        durationLabel                  = new JLabel();
        energyConsumedLabel            = new JLabel();
        httpsDataNotAnalyzedLabel      = new JLabel();
        summaryFillerHeaderLabel       = new JLabel();
        testFillerHeaderLabel          = new JLabel();
        totalDataLabel                 = new JLabel();
        
		JLabel appScoreLabel           = new JLabel();
		JLabel causesScoreLabel        = new JLabel();
		JLabel effectsScoreLabel       = new JLabel();
		JLabel httpsDataAnalyzedLabel  = new JLabel();
		JLabel totalAppScoreLabel      = new JLabel();
		JLabel totalhttpsDataLabel     = new JLabel();
        
        summaryHeaderLabel        .setFont(SUMMARY_FONT);
        statisticsHeaderLabel     .setFont(SUMMARY_FONT);
        attenuatorHeaderLabel 	  .setFont(SUMMARY_FONT);
                                                         
        appScoreLabel             .setFont(TEXT_FONT);   
        causesScoreLabel          .setFont(TEXT_FONT);   
        durationLabel             .setFont(TEXT_FONT);   
        effectsScoreLabel         .setFont(TEXT_FONT);   
        energyConsumedLabel       .setFont(TEXT_FONT);   
        httpsDataAnalyzedLabel    .setFont(TEXT_FONT);   
        httpsDataNotAnalyzedLabel .setFont(TEXT_FONT);   
        summaryFillerHeaderLabel  .setFont(TEXT_FONT);   
        testFillerHeaderLabel     .setFont(TEXT_FONT);   
        totalAppScoreLabel        .setFont(TEXT_FONT);   
        totalDataLabel            .setFont(TEXT_FONT);   
        totalhttpsDataLabel       .setFont(TEXT_FONT);
        
        downlinkLabel			  .setFont(TEXT_FONT);
        uplinkLabel				  .setFont(TEXT_FONT);
        
        add(layoutDataPanel(), BorderLayout.CENTER);

	}

	/**
	 * Creates the JPanel containing the Date , Trace and Application details
	 * 
	 * @return the dataPanel
	 */
	@Override
	public JPanel layoutDataPanel() {

 		
		if (dataPanel == null) {
			dataPanel = new JPanel(new GridBagLayout());
			dataPanel.setBackground(Color.WHITE);//UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));

			int idx = 0;
			
			addLabelLineName(summaryHeaderLabel        , "bestPractices.header.summary"         ,   idx ,2, weightX, insets, SUMMARY_FONT);  // 
			addLabelLineName(testFillerHeaderLabel     , " "                                    , ++idx ,2, weightX, insets, TEXT_FONT);     // 		
			addLabelLineName(statisticsHeaderLabel     , "bestPractices.header.statistics"      , ++idx ,2, weightX, insets, HEADER_FONT);   // 
			                                                                                                                             // 
			addLabelLineName(httpsDataNotAnalyzedLabel , "bestPractices.HTTPSDataNotAnalyzed"   , ++idx ,2, weightX, insets, TEXT_FONT);     // HTTPS data not analyzed\:
			/*
			 * addLabelLine(totalhttpsDataLabel , "bestPractices.TotalHTTPSData"
			 * , ++idx ,2, weightX, insets, TEXT_FONT); // Total HTTPS data\:
			 * addLabelLine(httpsDataAnalyzedLabel ,
			 * "bestPractices.HTTPSDataAnalyzed" , ++idx ,2, weightX, insets,
			 * TEXT_FONT); // HTTPS data analyzed\:
			 */			
			addLabelLineName(durationLabel             , "bestPractices.duration"               , ++idx ,2, weightX, insets, TEXT_FONT);     // 
			addLabelLineName(totalDataLabel            , "bestPractices.totalDataTransfered"    , ++idx ,2, weightX, insets, TEXT_FONT);     // 
			addLabelLineName(energyConsumedLabel       , "bestPractices.energyConsumed"         , ++idx ,2, weightX, insets, TEXT_FONT);     // 
			addLabelLineName(summaryFillerHeaderLabel  , " "                                    , ++idx ,2, weightX, insets, TEXT_FONT);     // 
			
			addLabelLineName(attenuatorHeaderLabel     , "bestPractice.header.attenuator"      , ++idx ,2, weightX, insets, HEADER_FONT);
			addLabelLineName(downlinkLabel			   , "bestPractice.header.attenuator.downlink", ++idx ,2, weightX, insets, TEXT_FONT);
			addLabelLineName(uplinkLabel			   , "bestPractice.header.attenuator.uplink", ++idx ,2, weightX, insets, TEXT_FONT);
			addLabelLineName(summaryFillerHeaderLabel  , " "                                    , ++idx ,2, weightX, insets, TEXT_FONT);     // 
			
		}
		return dataPanel;
	}

	@Override
	public int print(Graphics arg0, PageFormat arg1, int arg2) throws PrinterException {
		return 0;
	}

	@Override
	public void refresh(AROTraceData model) {
				
		PacketAnalyzerResult analyzerResults = model.getAnalyzerResult();
		
		int httpsDataNotAnalyzed = analyzerResults.getStatistic().getTotalHTTPSByte();
		double httpsDataNotAnalyzedKB = (double)httpsDataNotAnalyzed/1024;
		double httpsDataNotAnalyzedPct = (double)httpsDataNotAnalyzed/analyzerResults.getStatistic().getTotalByte();
		
		httpsDataNotAnalyzedLabel.setText(MessageFormat.format(
				ResourceBundleHelper.getMessageString("bestPractices.HTTPSDataNotAnalyzedValue"),
				pctFmt.format(httpsDataNotAnalyzedPct), 
				decFormat.format(httpsDataNotAnalyzedKB)));
		
		// Total Data Transferred\:
		totalDataLabel.setText(MessageFormat.format(
				ResourceBundleHelper.getMessageString("bestPractices.totalDataTransferedValue"),
				intFormat.format(analyzerResults.getStatistic().getTotalByte())));
		
		// Duration:
		String duration = decFormat.format(analyzerResults.getTraceresult().getTraceDuration() / 60);
		durationLabel.setText(MessageFormat.format(
				ResourceBundleHelper.getMessageString("bestPractices.durationValue"),
				duration));

		// Energy Consumed:
		energyConsumedLabel.setText(MessageFormat.format(
				ResourceBundleHelper.getMessageString("bestPractices.energyConsumedValue"),
				decFormat.format(analyzerResults.getEnergyModel().getTotalEnergyConsumed())));		
		
		//Attenuator :
		if (TraceResultType.TRACE_DIRECTORY.equals(model.getAnalyzerResult().getTraceresult().getTraceResultType())) {
			TraceDirectoryResult traceResult = (TraceDirectoryResult) model.getAnalyzerResult().getTraceresult();
			CollectOptions collectOptions = traceResult.getCollectOptions();
			if (collectOptions != null) {
				setSpeedThrottleValue(collectOptions);
 			}
		} else {
			clearSpeedThrottleValue();
		}
	}
	
	private void clearSpeedThrottleValue() {
		downlinkLabel.setText("");
		uplinkLabel.setText("");		
	}

	private void setSpeedThrottleValue(CollectOptions collectOptions){
		JLabel downlinkLabelTemp = getLabelMap().get(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.downlink"));
		JLabel uplinkLabelTemp = getLabelMap().get(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.uplink"));
		
		if(collectOptions.isAttnrProfile()){
  			downlinkLabelTemp.setText("Attenuation Profile: ");
  			downlinkLabel.setText(collectOptions.getAttnrProfileName());
 			uplinkLabelTemp.setVisible(false);
 			uplinkLabel.setVisible(false);
		}else{
			int dsDelay = collectOptions.getDsDelay();
			int usDelay = collectOptions.getUsDelay();
			int speedThrottleDL = collectOptions.getThrottleDL();
			int speedThrottleUL = collectOptions.getThrottleUL();
			downlinkLabelTemp.setText(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.downlink"));
			uplinkLabelTemp.setText(ResourceBundleHelper.getMessageString("bestPractice.header.attenuator.uplink"));
			uplinkLabelTemp.setVisible(true);
			uplinkLabel.setVisible(true);
			CommonHelper attenuator = new CommonHelper();
			if(dsDelay > 0 || usDelay > 0){
				this.downlinkLabel.setText(attenuator.transferSignalSignDownload(dsDelay) + " - " + numFormat.format(dsDelay) + " ms");
				this.uplinkLabel.setText(attenuator.transferSignalSignUpload(usDelay) + " - " + numFormat.format(usDelay) + " ms");
			}else{

				if(speedThrottleDL < 0){
					this.downlinkLabel.setText(ResourceBundleHelper.getMessageString("waterfall.na"));
				}else{
					this.downlinkLabel.setText(attenuator.numberTransferSignalDL(speedThrottleDL) + 
							" - " + attenuator.messageConvert(speedThrottleDL));
				}				
				if(speedThrottleUL < 0 ){
					this.uplinkLabel.setText(ResourceBundleHelper.getMessageString("waterfall.na"));
				}else{
					this.uplinkLabel.setText(attenuator.numberTransferSignalUL(speedThrottleUL) + 
							" - " + attenuator.messageConvert(speedThrottleUL));
				}
			}

		}
		
	}
	

}
