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
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class AttnrRadioGroupPanel extends JPanel implements ItemListener{

	private static final long serialVersionUID = 1L;

	private String attnrSlider;
	private String attnrLoadFile;
	private String attnrNone;

	private JRadioButton sliderBtn;
	private JRadioButton loadFileBtn;
	private JRadioButton defaultBtn;

	private ButtonGroup radioAttnGroup;
	private AttnrPanel parentPanel;
	private DataCollectorSelectNStartDialog startDialog;
	private AttnrThroughputThrottlePanel attnrTTPanel;
	private AttnrLoadProfilePanel attnrLoadPanel;
	private DeviceDialogOptions deviceInfo;
	private AttenuatorModel miniAtnr;

	public AttnrRadioGroupPanel(AttnrPanel jp, AttenuatorModel miniAtnr, DataCollectorSelectNStartDialog startDialog, DeviceDialogOptions deviceInfo) {

		setLayout(new FlowLayout());
		this.parentPanel = jp;
		this.startDialog = startDialog;
		this.miniAtnr = miniAtnr;
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
			if(attnrSlider.equals(itemStr)) {
				parentPanel.getAttnrHolder().remove(getLoadProfilePanel());
				getLoadProfilePanel().resetComponent();
				parentPanel.getAttnrHolder().add(getThroughputPanel(), BorderLayout.CENTER);
				parentPanel.getAttnrHolder().revalidate();
				parentPanel.getAttnrHolder().repaint();
	
				miniAtnr.setConstantThrottle(true);
				miniAtnr.setFreeThrottle(false);
				miniAtnr.setLoadProfile(false);
				if(ResourceBundleHelper.getMessageString("preferences.test.env").equals(SettingsImpl.getInstance().getAttribute("env"))) {
					startDialog.resizeUltra();
				} else {
					startDialog.resizeLarge();
				}
				DataCollectorType collectorType = deviceInfo.getCollector().getType();
				if(DataCollectorType.IOS.equals(collectorType) && deviceInfo.isSharedNetworkActive()){
					MessageDialogFactory.getInstance().showInformationDialog(this, deviceInfo.messageComposed(),
							DeviceDialogOptions.ATTENUATION_TITLE);
				}else if(DataCollectorType.IOS.equals(collectorType)) {
					new IOSStepsDialog(startDialog);
				}
			}else if(attnrLoadFile.equals(itemStr)) {
				parentPanel.getAttnrHolder().remove(getThroughputPanel());
				getThroughputPanel().resetComponent();
				parentPanel.getAttnrHolder().add(getLoadProfilePanel());
				parentPanel.getAttnrHolder().revalidate();
				parentPanel.getAttnrHolder().repaint();
	
				miniAtnr.setConstantThrottle(false);
				miniAtnr.setFreeThrottle(false);
				miniAtnr.setLoadProfile(true);
				resizeStartDialog();
			}else if(attnrNone.equals(itemStr)) {
				resizeStartDialog();
				reset();
			}else {
				resizeStartDialog();
				reset();
	
			}
		}
	}

	private void resizeStartDialog() {
		if(ResourceBundleHelper.getMessageString("preferences.test.env").equals(SettingsImpl.getInstance().getAttribute("env"))) {
			startDialog.resizeLarge();
		} else {
			startDialog.resizeMedium();
		}
	}


	public AttnrLoadProfilePanel getLoadProfilePanel() {
		if (attnrLoadPanel == null) {
			attnrLoadPanel = new AttnrLoadProfilePanel(miniAtnr);
		}
		return attnrLoadPanel;
	}

	public AttnrThroughputThrottlePanel getThroughputPanel() {
		if (attnrTTPanel == null) {
			attnrTTPanel = new AttnrThroughputThrottlePanel(miniAtnr);
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

		miniAtnr.setConstantThrottle(false);
		miniAtnr.setFreeThrottle(true);
		miniAtnr.setLoadProfile(false);
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
}
