/**
 * 
 */
package com.att.aro.ui.view.menu.datacollector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.att.aro.core.ILogger;
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Throttle for the throughput
 */
public class AttnrThroughputThrottlePanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 1L;

	private ILogger log = ContextAware.getAROConfigContext().getBean(ILogger.class);

	private JCheckBox cbDlAttenuator;
	private JCheckBox cbUlAttenuator;
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
			if (cbDlAttenuator.isSelected()) {
 				setDownload(true, MAX_NUM, MAX_NUM + " ", MAX_NUM, fpsDL);
			} else {
 				setDownload(false, MAX_NUM, MAX_NUM + " ", MAX_NUM, fpsDL);
			}
 		} else if (ac.equals(upLink)) {
			if (cbUlAttenuator.isSelected()) {
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
	
	private class DLSliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			if (!source.getValueIsAdjusting()) {
				int fps_local = (int) source.getValue();
				setFps(fps_local);
				log.info("Set DelayTime: " + fpsDL);
				setThrottoleDL(fpsDL);
				fsJLabel.setText(addUnit(Integer.toString(fpsDL)));
				miniAtnr.setThrottleDL(fpsDL);
			}
		}
	}

	private class ULSliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			if (!source.getValueIsAdjusting()) {
				int fps_local = (int) source.getValue();
				setUpFps(fps_local);
				log.info("Set DelayTime: " + fpsUL);
				setThrottleUL(fpsUL);
				fsUpJLabel.setText(addUnit(Integer.toString(fpsUL)));
				miniAtnr.setThrottleUL(fpsUL);
 				
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
		//add(msJLabel, gbSetting4);
		add(fsJLabel, gbSetting3);

		add(cbUlAttenuator, gbSetting5);
		add(delayULJSlider, gbSetting6);
		//add(msUpJLabel, gbSetting8);
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
		delayDLJSlider.setLabelTable(labelTableDL);
		delayDLJSlider.setPaintLabels(true);
		delayDLJSlider.setPaintTicks(true);
		delayDLJSlider.setMajorTickSpacing(100);
		delayDLJSlider.setMinorTickSpacing(10);
		delayDLJSlider.setEnabled(false);
		delayDLJSlider.addChangeListener(new DLSliderListener());
		delayDLJSlider.setInverted(true);
 		
		delayULJSlider = new LogarithmicJSlider(UL_FPS_MID3, MAX_NUM, MAX_NUM);// 100Mb = 1048576 kb
		delayULJSlider.setLabelTable(labelTableUL);
		delayULJSlider.setPaintLabels(true);
		delayULJSlider.setPaintTicks(true);
		delayULJSlider.setMajorTickSpacing(100);
		delayULJSlider.setMinorTickSpacing(10);
		delayULJSlider.setEnabled(false);
		delayULJSlider.addChangeListener(new ULSliderListener());
		delayULJSlider.setInverted(true);
 		
	}
	
	private String addUnit(String value) {
		String tempString = value.trim();
		Integer temp = Integer.parseInt(tempString);
		if(temp > 1024){
			NumberFormat numFormat =  NumberFormat.getIntegerInstance(); 
			return numFormat.format(temp.intValue() / 1024).concat(" Mbps");
		}else{
			return value.concat(" kbps");	
		}
		
	}
	
	public void resetComponent(){
		cbDlAttenuator.setSelected(false);
		cbUlAttenuator.setSelected(false);
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

}
