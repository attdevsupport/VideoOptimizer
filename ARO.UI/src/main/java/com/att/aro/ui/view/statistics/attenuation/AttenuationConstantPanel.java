/*
 *  Copyright 2018 AT&T
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
package com.att.aro.ui.view.statistics.attenuation;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.text.NumberFormat;

import javax.swing.JPanel;
import javax.swing.UIManager;

import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.pojo.CollectOptions;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.IUITabPanelLayoutUpdate;
import com.att.aro.ui.commonui.TabPanelCommon;
import com.att.aro.ui.commonui.TabPanelCommonAttributes;
import com.att.aro.ui.utils.CommonHelper;
import com.att.aro.ui.utils.ResourceBundleHelper;  
/**
 * 
 */
public class AttenuationConstantPanel extends JPanel implements IUITabPanelLayoutUpdate{

	private static final long serialVersionUID = 1L;
	private JPanel dataPanel;
	private final TabPanelCommon tabPanelCommon = new TabPanelCommon();
	enum LabelKeys {
		bestPractice_header_attenuator, 
		bestPractice_header_attenuator_downlink,  
		bestPractice_header_attenuator_uplink 
	}
	
	public AttenuationConstantPanel(){
		tabPanelCommon.initTabPanel(this);
		add(layoutDataPanel(), BorderLayout.WEST);

	}
	
	/* (non-Javadoc)
	 * @see com.att.aro.ui.commonui.IUITabPanelLayoutUpdate#layoutDataPanel()
	 */
	@Override
	public JPanel layoutDataPanel() {
		dataPanel = tabPanelCommon.initDataPanel(
				UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));

		Insets insets = new Insets(2, 2, 2, 2);
		Insets bottomBlankLineInsets = new Insets(2, 2, 8, 2);
		TabPanelCommonAttributes attributes = tabPanelCommon.addLabelLine(
			new TabPanelCommonAttributes.Builder()
				.enumKey(LabelKeys.bestPractice_header_attenuator)
				.contentsWidth(1)
				.insets(insets)
				.insetsOverride(bottomBlankLineInsets)
				.header()
				.build());
		attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder()
				.copyNextLine(attributes)
				.enumKey(LabelKeys.bestPractice_header_attenuator_downlink)
				.build());
		attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder()
				.copyNextLine(attributes)
				.enumKey(LabelKeys.bestPractice_header_attenuator_uplink)
				.build());
		return dataPanel;
	}

	/* (non-Javadoc)
	 * @see com.att.aro.ui.commonui.IUITabPanelLayoutUpdate#refresh(com.att.aro.core.pojo.AROTraceData)
	 */
	@Override
	public void refresh(AROTraceData analyzerResult) {
		TraceDirectoryResult traceResult = (TraceDirectoryResult) analyzerResult.getAnalyzerResult().getTraceresult();
		CollectOptions collectOptions = traceResult.getCollectOptions();
		refresh(collectOptions);
	}
	
	private void refresh(CollectOptions collectOptions){
		int dsDelay = collectOptions.getDsDelay();
		int usDelay = collectOptions.getUsDelay();
		int speedThrottleDL = collectOptions.getThrottleDL();
		int speedThrottleUL = collectOptions.getThrottleUL();
		CommonHelper attenuator = new CommonHelper();
		NumberFormat numFormat = NumberFormat.getIntegerInstance();
		
		if(dsDelay > 0 || usDelay > 0){
			
			tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_downlink,
					attenuator.transferSignalSignDownload(dsDelay) + " - " + numFormat.format(dsDelay) + " ms");
			tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_uplink,
					attenuator.transferSignalSignUpload(usDelay) + " - " + numFormat.format(usDelay) + " ms");
		}else{
			if(speedThrottleDL < 0){
				tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_downlink,
						ResourceBundleHelper.getMessageString("waterfall.na"));
			}else{
				tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_downlink,
						attenuator.numberTransferSignalDL(speedThrottleDL) + " - "
								+  attenuator.messageConvert(speedThrottleDL));
			}				
			if(speedThrottleUL < 0 ){
				tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_uplink,
						ResourceBundleHelper.getMessageString("waterfall.na"));
			}else{
				tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_uplink,
						attenuator.numberTransferSignalUL(speedThrottleUL) + " - "
								+ attenuator.messageConvert(speedThrottleUL));
			}
		}
	}
	
	public void resetPanelData(){
		tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_downlink,
				"");
		tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_uplink,
				"");
 	}

	
}
