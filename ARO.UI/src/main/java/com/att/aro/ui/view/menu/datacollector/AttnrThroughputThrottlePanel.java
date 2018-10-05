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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Throttle for the throughput
 */
public class AttnrThroughputThrottlePanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LogManager.getLogger(AttnrThroughputThrottlePanel.class);	
	private JCheckBox downloadCheckBox;
	private JCheckBox uploadCheckBox;
	private String downLink;
	private String upLink;
	private JLabel fsJLabel;
	private JLabel fsUpJLabel;
	
	private JSlider delayDLJSlider;
	private JSlider delayULJSlider;	
 
	private static final int MAX_NUM = 102400; // 100Mb = 1048576 kb
	private static final int DL_FPS_4G = 12288; // 64Mb = 65536   kb
	private static final int DL_FPS_3G = 5120;	// 10Mb = 10240   kb
	private static final int DL_FPS_MID3 = 64; //  64kb
	private static final int UL_FPS_4G = 12288;
	private static final int UL_FPS_3G = 5120;
	private static final int UL_FPS_MID3 = 64;
	@SuppressWarnings("unused")
	private static final int RESET_NUM = -1;
 	@SuppressWarnings("unused")
	private int throttleDL = MAX_NUM;
	@SuppressWarnings("unused")
	private int throttleUL = MAX_NUM;
	private int fpsDL = MAX_NUM;// 100Mb = 1048576 kb
	private int fpsUL = MAX_NUM;

	private AttenuatorModel miniAtnr;
	
	private enum SliderOption{
		ULSlide, DLSlide;
	}
	
	public AttnrThroughputThrottlePanel(AttenuatorModel miniAtnr){
		setLayout(new GridBagLayout());
		this.miniAtnr = miniAtnr;
		loadRadioGroupAttenuate();
 		getGBConstant();
 		resetComponent();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String ac = e.getActionCommand();
		if (ac.equals(downLink)) {
			if (downloadCheckBox.isSelected()) {
 				setDownload(true, MAX_NUM, MAX_NUM + " ", MAX_NUM, fpsDL);
			} else {
 				setDownload(false, MAX_NUM, MAX_NUM + " ", MAX_NUM, fpsDL);
			}
 		} else if (ac.equals(upLink)) {
			if (uploadCheckBox.isSelected()) {
				setUpload(true,MAX_NUM,MAX_NUM + " ",MAX_NUM,fpsUL);
			} else {
				setUpload(false,MAX_NUM,MAX_NUM + " ",MAX_NUM,fpsUL);
			}
 		}		
	}

	/**
	 * Method for organized attenuation attribute setting
	 */
	private void setUpload(boolean setUpSlider, int setUpSliderNum, String textUpLabel, int setUpInit, int fpsUL) {
		delayULJSlider.setEnabled(setUpSlider);
		delayULJSlider.setValue(setUpSliderNum);
		fsUpJLabel.setText(addUnit(textUpLabel));
		setUpFps(setUpInit);
		setThrottleUL(fpsUL);
		miniAtnr.setThrottleUL(MAX_NUM);
		miniAtnr.setThrottleULEnable(setUpSlider);
	}

	private void setDownload(boolean setDownSlider, int setDownSliderNum, String textDownLabel, int setDownInit, int fpsDL){
		delayDLJSlider.setEnabled(setDownSlider);
		delayDLJSlider.setValue(setDownSliderNum);
		fsJLabel.setText(addUnit(textDownLabel));
		setFps(setDownInit);
		setThrottoleDL(fpsDL);
		miniAtnr.setThrottleDL(MAX_NUM);
		miniAtnr.setThrottleDLEnable(setDownSlider);
		
	}
	
	private class SliderMouseListener implements MouseMotionListener {
		
		@Override
		public void mouseDragged(MouseEvent e) {
			JSlider source = (JSlider) e.getSource();
			int fps_local = (int) source.getValue();
			if(SliderOption.DLSlide.name().equals(source.getName())){
				setFps(fps_local);
				LOG.info("Set DelayTime: " + fpsDL);
				setThrottoleDL(fpsDL);
				fsJLabel.setText(addUnit(Integer.toString(fpsDL)));
				miniAtnr.setThrottleDL(fpsDL);
			}else{
				setUpFps(fps_local);
				LOG.info("Set DelayTime: " + fpsUL);
				setThrottleUL(fpsUL);
				fsUpJLabel.setText(addUnit(Integer.toString(fpsUL)));
				miniAtnr.setThrottleUL(fpsUL);
			}
			
		}

		@Override
		public void mouseMoved(MouseEvent e) {

		}
	}

	private class SliderKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			JSlider source = (JSlider) e.getSource();
			if (source.getValueIsAdjusting()) {
				return;
			}
			if (SliderOption.DLSlide.name().equals(source.getName())) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) { // increase
					int fps_local = keyEventTriggered(KeyEvent.VK_LEFT, (int) source.getValue(),
							fsJLabel.getText().split(" ")[1]);
					updateAttenuationValue(SliderOption.DLSlide, fps_local);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) { // decrease
					int fps_local = keyEventTriggered(KeyEvent.VK_RIGHT, (int) source.getValue(),
							fsJLabel.getText().split(" ")[1]);
					updateAttenuationValue(SliderOption.DLSlide, fps_local);
				}
			} else {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {// increase
					int fps_local = keyEventTriggered(KeyEvent.VK_LEFT, (int) source.getValue(),
							fsUpJLabel.getText().split(" ")[1]);
					updateAttenuationValue(SliderOption.ULSlide, fps_local);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {// decrease
					int fps_local = keyEventTriggered(KeyEvent.VK_RIGHT, (int) source.getValue(),
							fsUpJLabel.getText().split(" ")[1]);
					updateAttenuationValue(SliderOption.ULSlide, fps_local);
				}
			}
		}
	}
	
	private int keyEventTriggered(int keyEvent, int val, String unit){
		double attenuationValue = Double.parseDouble(Integer.toString(val));
		double increment = unit.trim().equals("kbps") ? 1.0 : 9.0;
		if(keyEvent == KeyEvent.VK_LEFT){
			attenuationValue = attenuationValue + increment;
		}else if (keyEvent == KeyEvent.VK_RIGHT){
			attenuationValue = attenuationValue - increment;
		}	
		int result = (int)attenuationValue;
		return result;
	}

	private void updateAttenuationValue(SliderOption option, int value){
		if(option == SliderOption.ULSlide){
			delayULJSlider.setValue(value);
			setUpFps(value);
			LOG.info("Set DelayTime: " + fpsUL);
			setThrottleUL(fpsUL);
			fsUpJLabel.setText(addUnit(Integer.toString(fpsUL)));
			miniAtnr.setThrottleUL(fpsUL);
		}else if(option == SliderOption.DLSlide){
			delayDLJSlider.setValue(value);
			setFps(value);
			LOG.info("Set DelayTime: " + fpsDL);
			setThrottoleDL(fpsDL);
			fsJLabel.setText(addUnit(Integer.toString(fpsDL)));
			miniAtnr.setThrottleDL(fpsDL);
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
		
		add(downloadCheckBox, gbSetting1);
		add(delayDLJSlider, gbSetting2);
		//add(msJLabel, gbSetting4);
		add(fsJLabel, gbSetting3);

		add(uploadCheckBox, gbSetting5);
		add(delayULJSlider, gbSetting6);
		//add(msUpJLabel, gbSetting8);
		add(fsUpJLabel, gbSetting7);
		setEnabled(false);

	}
	
	private void loadRadioGroupAttenuate() {

		downLink = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.downlink");
		upLink = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.uplink");

		downloadCheckBox = new JCheckBox(downLink);
		downloadCheckBox.setActionCommand(downLink);
		downloadCheckBox.addActionListener(this);

		uploadCheckBox = new JCheckBox(upLink);
		uploadCheckBox.setActionCommand(upLink);
		uploadCheckBox.addActionListener(this);

		fsJLabel = new JLabel(addUnit(Integer.toString(MAX_NUM)));
		
		fsUpJLabel = new JLabel(addUnit(Integer.toString(MAX_NUM)));
		
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

		delayDLJSlider = new LogarithmicJSlider(DL_FPS_MID3,MAX_NUM,  MAX_NUM);// 100Mb = 1048576 kb
		delayDLJSlider.setName(SliderOption.DLSlide.name());
		delayDLJSlider.setLabelTable(labelTableDL);
		delayDLJSlider.setPaintLabels(true);
		delayDLJSlider.setPaintTicks(true);
		delayDLJSlider.setMajorTickSpacing(100);
		delayDLJSlider.setMinorTickSpacing(10);
		delayDLJSlider.setEnabled(false);
		delayDLJSlider.addMouseMotionListener(new SliderMouseListener());
		delayDLJSlider.addKeyListener(new SliderKeyListener());
		delayDLJSlider.setInverted(true);
 		
		delayULJSlider = new LogarithmicJSlider(UL_FPS_MID3, MAX_NUM, MAX_NUM);// 100Mb = 1048576 kb
		delayULJSlider.setName(SliderOption.ULSlide.name());
		delayULJSlider.setLabelTable(labelTableUL);
		delayULJSlider.setPaintLabels(true);
		delayULJSlider.setPaintTicks(true);
		delayULJSlider.setMajorTickSpacing(100);
		delayULJSlider.setMinorTickSpacing(10);
		delayULJSlider.setEnabled(false);
		delayULJSlider.addMouseMotionListener(new SliderMouseListener());
		delayULJSlider.addKeyListener(new SliderKeyListener());
		delayULJSlider.setInverted(true);
 		
	}
	
	private String addUnit(String value) {
		String tempString = value.trim();
		Double temp = Double.parseDouble(tempString);
		double tempDouble =  temp.doubleValue() / 1024;
		if(temp > 1024){
			DecimalFormat decFormat = new DecimalFormat("#.##");
			decFormat.setRoundingMode(RoundingMode.HALF_UP);
	        return decFormat.format(tempDouble).concat(" Mbps");
		}else{
			return value.concat(" kbps");	
		}
	}
	
	public void resetComponent(){
		downloadCheckBox.setSelected(false);
		uploadCheckBox.setSelected(false);
		miniAtnr.setThrottleDLEnable(false);
		miniAtnr.setThrottleULEnable(false);
		delayDLJSlider.setValue(MAX_NUM);
		delayDLJSlider.setEnabled(false);
		delayULJSlider.setValue(MAX_NUM);
		delayULJSlider.setEnabled(false);
		miniAtnr.setThrottleDL(MAX_NUM);
		miniAtnr.setThrottleUL(MAX_NUM);

	}
	public void setThrottleUL(int throughputUL) {
		this.throttleUL = throughputUL;
	}

	public void setThrottoleDL(int throughputDL) {
		this.throttleDL = throughputDL;
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
	public JCheckBox getUploadCheckBox() {
		return uploadCheckBox;
	}

	public void setUploadCheckBox(JCheckBox uploadCheckBox) {
		this.uploadCheckBox = uploadCheckBox;
	}

	public JCheckBox getDownloadCheckBox() {
		return downloadCheckBox;
	}

	public void setDownloadCheckBox(JCheckBox downloadCheckBox) {
		this.downloadCheckBox = downloadCheckBox;
	}

}
