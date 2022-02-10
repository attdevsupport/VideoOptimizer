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
package com.att.aro.ui.view.menu.datacollector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class AttnrRadioGroupPanel extends JPanel implements ItemListener{

	private final int HEIGHT_RESET = 0;

	private final int HEIGHT_LOAD_PROFILE_SECTION = 30;

	private final int HEIGHT_ATTENUATOR_SECTION = 124;

	private static final long serialVersionUID = 1L;

	private String attnrSlider;
	private String attnrLoadFile;
	private String attnrNone;

	private JRadioButton sliderBtn;
	private JRadioButton loadFileBtn;
	private JRadioButton defaultBtn;

	private int priorHeightAdjustment = 0;
	private ButtonGroup radioAttnGroup;
	private AttnrPanel parentPanel;
	private DataCollectorSelectNStartDialog startCollectDialog;
	private AttnrThroughputThrottlePanel attnrTTPanel;
	private AttnrLoadProfilePanel attnrLoadPanel;
	private DeviceDialogOptions deviceInfo;
	private AttenuatorModel attenuatorModel;

	public AttnrRadioGroupPanel(AttnrPanel jp, AttenuatorModel attenuatorModel, DataCollectorSelectNStartDialog startCollectDialog, DeviceDialogOptions deviceInfo) {

		setLayout(new FlowLayout());
		this.parentPanel = jp;
		this.startCollectDialog = startCollectDialog;
		this.attenuatorModel = attenuatorModel;
		this.deviceInfo = deviceInfo;
		attnrSlider = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.slider");
		attnrLoadFile = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.loadfile");
		attnrNone = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.none");

		sliderBtn = new JRadioButton(attnrSlider);
		loadFileBtn = new JRadioButton(attnrLoadFile);
		defaultBtn = new JRadioButton(attnrNone);

		radioAttnGroup = new ButtonGroup();
		radioAttnGroup.add(sliderBtn);
		radioAttnGroup.add(loadFileBtn);
		radioAttnGroup.add(defaultBtn);

		sliderBtn.addItemListener(this);
		loadFileBtn.addItemListener(this);
		defaultBtn.addItemListener(this);
		defaultBtn.setSelected(true);

		add(defaultBtn);
		add(sliderBtn);
		add(loadFileBtn);
	}

	public ButtonGroup getRadioAttnGroup() {
		return radioAttnGroup;
	}

	public void setRadioAttnGroup(ButtonGroup radioAttnGroup) {
		this.radioAttnGroup = radioAttnGroup;
	}

	@Override
	public void itemStateChanged(ItemEvent event) {
		JRadioButton item = (JRadioButton) event.getItem();
		String itemStr = item.getText();
		if (event.getStateChange() == ItemEvent.SELECTED) {
			if (attnrSlider.equals(itemStr)) { // Slider
				parentPanel.getAttnrHolder().remove(getLoadProfilePanel());
				getLoadProfilePanel().resetComponent();
				parentPanel.getAttnrHolder().add(getThroughputPanel(), BorderLayout.CENTER);
				parentPanel.getAttnrHolder().revalidate();
				parentPanel.getAttnrHolder().repaint();

				attenuatorModel.setConstantThrottle(true);
				attenuatorModel.setFreeThrottle(false);
				attenuatorModel.setLoadProfile(false);
				resizeDialog(HEIGHT_ATTENUATOR_SECTION);
				DataCollectorType collectorType = deviceInfo.getCollector().getType();
				if (DataCollectorType.IOS.equals(collectorType) && deviceInfo.isSharedNetworkActive()) {
					MessageDialogFactory.getInstance().showInformationDialog(this, deviceInfo.messageComposed(), ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.attenuation.title"));
				} else if (DataCollectorType.IOS.equals(collectorType)) {
					new IOSStepsDialog(startCollectDialog);
				}
			} else if (attnrLoadFile.equals(itemStr)) { // Profile
				parentPanel.getAttnrHolder().remove(getThroughputPanel());
				getThroughputPanel().resetComponent();
				parentPanel.getAttnrHolder().add(getLoadProfilePanel());
				parentPanel.getAttnrHolder().revalidate();
				parentPanel.getAttnrHolder().repaint();

				attenuatorModel.setConstantThrottle(false);
				attenuatorModel.setFreeThrottle(false);
				attenuatorModel.setLoadProfile(true);
				resizeDialog(HEIGHT_LOAD_PROFILE_SECTION);
			} else if (attnrNone.equals(itemStr)) { // None
				resizeDialog(HEIGHT_RESET);
				reset();
			} else {
				resizeDialog(HEIGHT_RESET);
				reset();

			}
		}
	}
	
	/** <pre>
	 * Apply new dialog size adjustment after removing prior sizing (if previously applied).
	 *  Negative values will reduce the height.
	 *  Positive values will increase the height.
	 *  A zero value will reverse any priorHeightAdjustment.
	 * 
	 * @param adjustment 
	 */
	private void resizeDialog(int adjustment) {
		if (priorHeightAdjustment != 0) {
			if (adjustment != 0) {
				startCollectDialog.resizer(-priorHeightAdjustment);
				priorHeightAdjustment = adjustment;
			} else {
				adjustment = -priorHeightAdjustment;
				priorHeightAdjustment = 0;
			}
		} else { // priorHeightAdjustment == 0
			priorHeightAdjustment = adjustment;
		}
		startCollectDialog.resizer(adjustment);
	}

	public AttnrLoadProfilePanel getLoadProfilePanel() {
		if (attnrLoadPanel == null) {
			attnrLoadPanel = new AttnrLoadProfilePanel(attenuatorModel);
		}
		return attnrLoadPanel;
	}

	public AttnrThroughputThrottlePanel getThroughputPanel() {
		if (attnrTTPanel == null) {
			attnrTTPanel = new AttnrThroughputThrottlePanel(attenuatorModel);
		}
		return attnrTTPanel;
	}

	public void enableComponents(Container container, boolean enable) {
		Component[] components = container.getComponents();
		for (Component component : components) {
			component.setEnabled(enable);
			if (component instanceof Container) {
				enableComponents((Container) component, enable);
			}
		}
	}

	public void setRbAtnrLoadFileEnable(boolean enable) {
		loadFileBtn.setEnabled(enable);
	}

	public void reset() {
		parentPanel.getAttnrHolder().remove(getThroughputPanel());
		getThroughputPanel().resetComponent();
		parentPanel.getAttnrHolder().remove(getLoadProfilePanel());
		getLoadProfilePanel().resetComponent();
		parentPanel.getAttnrHolder().revalidate();
		parentPanel.getAttnrHolder().repaint();

		attenuatorModel.setConstantThrottle(false);
		attenuatorModel.setFreeThrottle(true);
		attenuatorModel.setLoadProfile(false);
		defaultBtn.setSelected(true);
	}
	
	public JRadioButton getSliderBtn() {
		return sliderBtn;
	}

	public void setSliderBtn(JRadioButton sliderBtn) {
		this.sliderBtn = sliderBtn;
	}

	public JRadioButton getLoadFileBtn() {
		return loadFileBtn;
	}

	public void setLoadFileBtn(JRadioButton loadFileBtn) {
		this.loadFileBtn = loadFileBtn;
	}
	
	public JRadioButton getDefaultBtn() {
		return defaultBtn;
	}

	public void setDefaultBtn(JRadioButton defaultBtn) {
		this.defaultBtn = defaultBtn;
	}

	public void reselectPriorOptions(AttenuatorModel attenuatorModel, boolean isIOS) {

		DataCollectorType deviceType = deviceInfo.getCollector().getType();
		switch (deviceType) {
		case NON_ROOTED_ANDROID:
		case IOS:
			this.attenuatorModel = attenuatorModel;
			attnrTTPanel.setAttenuatorModel(attenuatorModel);
			attnrLoadPanel.setAttenuatorModel(attenuatorModel);
			if (attenuatorModel.isConstantThrottle() && !attenuatorModel.isFreeThrottle()) {
				sliderBtn.setSelected(true);
				parentPanel.getAttnrHolder().remove(getLoadProfilePanel());
				getLoadProfilePanel().resetComponent();
				parentPanel.getAttnrHolder().add(getThroughputPanel(), BorderLayout.CENTER);
				parentPanel.getAttnrHolder().revalidate();
				parentPanel.getAttnrHolder().repaint();
				attnrTTPanel.reselectPriorOptions(attenuatorModel);
			} else if (!attenuatorModel.isConstantThrottle() && attenuatorModel.isLoadProfile()) {
				loadFileBtn.setSelected(true);
				parentPanel.getAttnrHolder().remove(getThroughputPanel());
				getThroughputPanel().resetComponent();
				parentPanel.getAttnrHolder().add(getLoadProfilePanel());
				parentPanel.getAttnrHolder().revalidate();
				parentPanel.getAttnrHolder().repaint();
				attnrLoadPanel.reselectPriorOptions(attenuatorModel);
			}
			break;
		case ROOTED_ANDROID:
			AttenuatorModel attenuatorModelReset = new AttenuatorModel();// reset
			this.attenuatorModel = attenuatorModelReset;
			deviceInfo.setAttenuatorModel(attenuatorModelReset);
			attnrTTPanel.setAttenuatorModel(attenuatorModelReset);
			attnrLoadPanel.setAttenuatorModel(attenuatorModelReset);
			loadFileBtn.setEnabled(false);
			break;
		case DEFAULT:
			defaultBtn.setSelected(true);
			break;
		}
	}
}
