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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Re-factor Attenuation check box panel to the individual class 
 */
public class AttnrConstantThrottlePanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LogManager.getLogger(AttnrConstantThrottlePanel.class);	
	private JCheckBox cbDlAttenuator;
	private JCheckBox cbUlAttenuator;
	private String downLink;
	private String upLink;
	private JLabel msJLabel;
	private JLabel fsJLabel;
	private JLabel msUpJLabel;
	private JLabel fsUpJLabel;
	
	private JSlider delayDLJSlider;
	private JSlider delayULJSlider;
	private int fpsDL = 0;
	private int fpsUL = 0;
	@SuppressWarnings("unused")
	private int delayTimeDL = 0;
	@SuppressWarnings("unused")
	private int delayTimeUL = 0;

	private static final int DL_FPS_4G = 10;
	private static final int DL_FPS_3G = 125;
	private static final int DL_FPS_MID3 = 2000;

	private static final int UL_FPS_4G = 27;
	private static final int UL_FPS_3G = 77;
	private static final int UL_FPS_MID3 = 84;
	private AttenuatorModel miniAtnr;
 
	public AttnrConstantThrottlePanel(AttenuatorModel miniAtnr){
		setLayout(new GridBagLayout());
		this.miniAtnr = miniAtnr;
		loadRadioGroupAttenuate();
 		getGBConstant();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String ac = e.getActionCommand();
		if (ac.equals(downLink)) {
			if (cbDlAttenuator.isSelected()) {
 				setDownload(true, 1, 1 + " ", 1, fpsDL);
			} else {
 				setDownload(false, 0, 0 + " ", 0, fpsDL);
			}
 		} else if (ac.equals(upLink)) {
			if (cbUlAttenuator.isSelected()) {
				setUpload(true,1,1 + " ",1,fpsUL);
			} else {
				setUpload(false,0,0 + " ",0,fpsUL);
			}
 		}		
	}

	/**
	 * Method for organized attenuation attribute setting
	 */
	private void setUpload(boolean setUpSlider, int setUpSliderNum, String textUpLabel, int setUpInit, int fpsUL) {
		delayULJSlider.setEnabled(setUpSlider);
		delayULJSlider.setValue(setUpSliderNum);
		fsUpJLabel.setText(textUpLabel);
		setUpFps(setUpInit);
		setDelayUpTime(fpsUL);
	}

	private void setDownload(boolean setDownSlider, int setDownSliderNum, String textDownLabel, int setDownInit, int fpsDL){
		delayDLJSlider.setEnabled(setDownSlider);
		delayDLJSlider.setValue(setDownSliderNum);
		fsJLabel.setText(textDownLabel);
		setFps(setDownInit);
		setDelayTime(fpsDL);
	}
	
	private class DLSliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			if (!source.getValueIsAdjusting()) {
				int fps_local = (int) source.getValue();
				setFps(fps_local);
				LOG.info("Set DelayTime: " + fpsDL);
				setDelayTime(fpsDL);
				fsJLabel.setText(fpsDL + " ");
				miniAtnr.setDelayDS(fpsDL);
			}
		}
	}

	private class ULSliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			if (!source.getValueIsAdjusting()) {
				int fps_local = (int) source.getValue();
				setUpFps(fps_local);
				LOG.info("Set DelayTime: " + fpsUL);
				setDelayUpTime(fpsUL);
				fsUpJLabel.setText(fpsUL + " ");
				miniAtnr.setDelayUS(fpsUL);
			}
		}
	}

	private void getGBConstant() {
		GridBagConstraints gbSetting1 = new GridBagConstraints();
		gbSetting1.fill = GridBagConstraints.HORIZONTAL;
		gbSetting1.gridx = 0;
		gbSetting1.gridy = 0;
		gbSetting1.weightx = 1.0;
		gbSetting1.anchor = GridBagConstraints.WEST;

		GridBagConstraints gbSetting2 = new GridBagConstraints();
		gbSetting2.gridwidth = 2;
		gbSetting2.gridx = 2;
		gbSetting2.gridy = 0;
		gbSetting2.weightx = 9.0;
		gbSetting2.anchor = GridBagConstraints.WEST;
		gbSetting2.fill = GridBagConstraints.HORIZONTAL;

		GridBagConstraints gbSetting3 = new GridBagConstraints();
		gbSetting3.anchor = GridBagConstraints.WEST;
		gbSetting3.gridwidth = 2;
		gbSetting3.gridx = 1;
		gbSetting3.gridy = 1;

		GridBagConstraints gbSetting4 = new GridBagConstraints();
		gbSetting4.anchor = GridBagConstraints.WEST;
		gbSetting4.gridx = 3;
		gbSetting4.gridy = 1;

		GridBagConstraints gbSetting5 = new GridBagConstraints();
		gbSetting5.fill = GridBagConstraints.HORIZONTAL;
		gbSetting5.gridx = 0;
		gbSetting5.gridy = 2;
		gbSetting5.weightx = 1.0;
		gbSetting5.anchor = GridBagConstraints.WEST;

		GridBagConstraints gbSetting6 = new GridBagConstraints();
		gbSetting6.fill = GridBagConstraints.HORIZONTAL;
		gbSetting6.gridwidth = 2;
		gbSetting6.gridx = 2;
		gbSetting6.gridy = 2;
		gbSetting6.weightx = 9.0;
		gbSetting6.anchor = GridBagConstraints.WEST;

		GridBagConstraints gbSetting7 = new GridBagConstraints();
		gbSetting7.anchor = GridBagConstraints.WEST;
		gbSetting7.gridwidth = 2;
		gbSetting7.gridx = 1;
		gbSetting7.gridy = 3;

		GridBagConstraints gbSetting8 = new GridBagConstraints();
		gbSetting8.anchor = GridBagConstraints.WEST;
		gbSetting8.gridx = 3;
		gbSetting8.gridy = 3;
		
		add(cbDlAttenuator, gbSetting1);
		add(delayDLJSlider, gbSetting2);
		add(msJLabel, gbSetting4);
		add(fsJLabel, gbSetting3);

		add(cbUlAttenuator, gbSetting5);
		add(delayULJSlider, gbSetting6);
		add(msUpJLabel, gbSetting8);
		add(fsUpJLabel, gbSetting7);
		setEnabled(false);

	}
	
	private void loadRadioGroupAttenuate() {

		downLink = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.downlink");
		upLink = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.uplink");

		cbDlAttenuator = new JCheckBox(downLink);
		cbDlAttenuator.setActionCommand(downLink);
		cbDlAttenuator.addActionListener(this);

		cbUlAttenuator = new JCheckBox(upLink);
		cbUlAttenuator.setActionCommand(upLink);
		cbUlAttenuator.addActionListener(this);

		fsJLabel = new JLabel("0");
		msJLabel = new JLabel(" ms Down Stream Delay");

		fsUpJLabel = new JLabel("0");
		msUpJLabel = new JLabel(" ms Up Stream Delay");

		Hashtable<Integer, JLabel> labelTableDL = new Hashtable<>();
		labelTableDL.put(new Integer(DL_FPS_3G),
				new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.3g")));
		labelTableDL.put(new Integer(DL_FPS_4G),
				new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.4g")));
		labelTableDL.put(new Integer(DL_FPS_MID3),
				new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.2g")));

		Hashtable<Integer, JLabel> labelTableUL = new Hashtable<>();
		labelTableUL.put(new Integer(UL_FPS_3G),
				new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.3g")));
		labelTableUL.put(new Integer(UL_FPS_4G),
				new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.4g")));
		labelTableUL.put(new Integer(UL_FPS_MID3),
				new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.2g")));

		delayDLJSlider = new LogarithmicJSlider(1, 2000, 1);
		delayDLJSlider.setLabelTable(labelTableDL);
		delayDLJSlider.setPaintLabels(true);
		delayDLJSlider.setPaintTicks(true);
		delayDLJSlider.setMajorTickSpacing(10);
		delayDLJSlider.setMinorTickSpacing(10);
		delayDLJSlider.setEnabled(false);
		delayDLJSlider.addChangeListener(new DLSliderListener());

		delayULJSlider = new JSlider(1, 100, 1);
		delayULJSlider.setLabelTable(labelTableUL);
		delayULJSlider.setPaintLabels(true);
		delayULJSlider.setPaintTicks(true);
		delayULJSlider.setMajorTickSpacing(10);
		delayULJSlider.setMinorTickSpacing(10);
		delayULJSlider.setEnabled(false);
		delayULJSlider.addChangeListener(new ULSliderListener());

	}
	
	public void resetComponenet(){
		cbDlAttenuator.setSelected(false);
		cbUlAttenuator.setSelected(false);
		delayDLJSlider.setValue(0);
		delayDLJSlider.setEnabled(false);
		delayULJSlider.setValue(0);
		delayULJSlider.setEnabled(false);		
		miniAtnr.setDelayDS(0);
		miniAtnr.setDelayUS(0);
	}
	
	
	public void setDelayUpTime(int delayUpTime) {
		this.delayTimeUL = delayUpTime;
	}

	public void setDelayTime(int delayTime) {
		this.delayTimeDL = delayTime;
	}

	public int getFps() {
		return fpsDL;
	}

	public void setFps(int fps) {
		this.fpsDL = fps;
	}

	public int getUpFps() {
		return fpsUL;
	}

	public void setUpFps(int upFps) {
		this.fpsUL = upFps;
	}

}
