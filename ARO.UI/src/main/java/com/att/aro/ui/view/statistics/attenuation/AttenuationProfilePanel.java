/**
 * 
 */
package com.att.aro.ui.view.statistics.attenuation;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.UIManager;

import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.pojo.CollectOptions;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.IUITabPanelLayoutUpdate;
import com.att.aro.ui.commonui.TabPanelCommon;
import com.att.aro.ui.commonui.TabPanelCommonAttributes;
/**
 * 
 */
public class AttenuationProfilePanel extends JPanel implements IUITabPanelLayoutUpdate{
	private static final long serialVersionUID = 1L;
	private JPanel dataPanel;
	private final TabPanelCommon tabPanelCommon = new TabPanelCommon();
	enum LabelKeys {
		bestPractice_header_attenuator,
		bestPractice_header_attenuator_profile 

	}
	
	public AttenuationProfilePanel(){
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
				.enumKey(LabelKeys.bestPractice_header_attenuator_profile)
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
		tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_profile,
				" " + collectOptions.getAttnrProfileName());

	}
	
	public void resetPanelData(){
		tabPanelCommon.setText(LabelKeys.bestPractice_header_attenuator_profile,
				"");

	}
	
	

}
