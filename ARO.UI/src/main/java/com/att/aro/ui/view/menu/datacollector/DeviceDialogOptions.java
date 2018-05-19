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
package com.att.aro.ui.view.menu.datacollector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.apache.commons.lang.StringUtils;

import com.att.aro.core.android.AndroidApiLevel;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.video.pojo.Orientation;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.datacollector.ioscollector.utilities.AppSigningHelper;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class DeviceDialogOptions extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private DataCollectorSelectNStartDialog parent;
	private IAroDevice selectedDevice;

	private String txtLREZ;
	private String txtHDEF;
	private String txtSDEF;
	private String txtNONE;
	private String txtPortrait;
	private String txtLandscape;
	private String rooted;
	private String vpn;
	private String ios;

	private JRadioButton btn_lrez;
	private JRadioButton btn_hdef;
	private JRadioButton btn_sdef;
	private JRadioButton btn_none;

	private JRadioButton btn_portrait;
	private JRadioButton btn_landscape;
	private JPanel videoOrientRadioGrpPanel;

	private JRadioButton btnRooted;
	private JRadioButton btnVpn;
	private JRadioButton btniOS;

	private ButtonGroup radioBtnVpnRoot;
	private VideoOption videoOption;
	private Orientation videoOrient;

	private Label labelCollectorTitle;
	private Label labelAttenuatorTitle;
	private Label labelVideoTitle;
	private Label labelVideoOrientTitle;

	private GridBagLayout contentLayout;
	private GridBagConstraints labelConstraints;
	private GridBagConstraints optionConstraints;

	private AttnrPanel attnrGroupPanel;
	private AttenuatorModel miniAtnr;

	private boolean secure;

	private IDataCollector collector;
	private IDataCollector rootCollector;
	private IDataCollector vpnCollector;
	private IDataCollector iosCollector;
	private int api;

	public DeviceDialogOptions(DataCollectorSelectNStartDialog parent, List<IDataCollector> collectors) {
		this.parent = parent;
		videoOrient = Orientation.LANDSCAPE.toString().toLowerCase().equals(
				SettingsImpl.getInstance().getAttribute("orientation")) ? Orientation.LANDSCAPE : Orientation.PORTRAIT;

		setLayout(new BorderLayout());
		add(getContent(), BorderLayout.CENTER);
		configure(collectors);
	}

	private Component getContent() {
		setUpLabels();
		setUpLayoutProperties();

		JPanel contents = new JPanel(contentLayout);
		videoOrientRadioGrpPanel = getRadioGroupVideoOrient();

		contents.add(labelCollectorTitle, labelConstraints);
		contents.add(getRadioGroupCollector(), optionConstraints);

		contents.add(labelAttenuatorTitle, labelConstraints);
		contents.add(getAttnrGroup(), optionConstraints);

 
		contents.add(labelVideoTitle, labelConstraints);
		contents.add(getRadioGroupVideo(), optionConstraints);

		contents.add(labelVideoOrientTitle, labelConstraints);
		contents.add(videoOrientRadioGrpPanel, optionConstraints);

		return contents;
	}

	private void setUpLayoutProperties() {
		contentLayout = new GridBagLayout();

		labelConstraints = new GridBagConstraints();
		labelConstraints.fill = GridBagConstraints.BOTH;
		labelConstraints.weightx = 0.3;
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;

		optionConstraints = new GridBagConstraints();
		optionConstraints.fill = GridBagConstraints.BOTH;
		optionConstraints.weightx = 1.0;
		optionConstraints.gridwidth = GridBagConstraints.REMAINDER;
	}

	private void setUpLabels() {
		String collectorTitle = ResourceBundleHelper.getMessageString("dlog.collector.option.collector.title");
		String attenuatorTitle = ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.title");
		String videoTitle = ResourceBundleHelper.getMessageString("dlog.collector.option.video.title");
		String videoOrientTitle = ResourceBundleHelper.getMessageString("dlog.collector.option.video.orient.title");

		labelCollectorTitle = new Label(collectorTitle);
		labelAttenuatorTitle = new Label(attenuatorTitle);
		labelVideoTitle = new Label(videoTitle);
		labelVideoOrientTitle = new Label(videoOrientTitle);
	}

	/**
	 * Configure options based on collectors
	 * 
	 * @param collectors
	 */
	public void configure(List<IDataCollector> collectors) {
		if (collectors != null) {
			for (int i = 0; i < collectors.size(); i++) {

				switch (collectors.get(i).getType()) {

				case ROOTED_ANDROID:
					btnRooted.setVisible(true);
					btnRooted.setEnabled(true);					
					rootCollector = collectors.get(i);
					break;

				case NON_ROOTED_ANDROID:
					btnVpn.setVisible(true);
					btnVpn.setEnabled(true);
					vpnCollector = collectors.get(i);
					break;

				case IOS:
					btniOS.setVisible(true);
					btniOS.setEnabled(true);
					iosCollector = collectors.get(i);
					break;

				case DEFAULT:

					break;

				default:
					break;
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		showHideOptions(e);
		if(e.getActionCommand().equals(vpn) || e.getActionCommand().equals(ios) || e.getActionCommand().equals(rooted)) {
			setAttenuateSectionStatus();
		}
	}

	private void showHideOptions(ActionEvent e) {
		String ac = e.getActionCommand();

		// Video Option
		if (ac.equals(txtLREZ)) {
			videoOption = VideoOption.LREZ;
			showVideoOrientation(false);
			return;
		} else if (ac.equals(txtHDEF)) {
			videoOption = VideoOption.HDEF;
			if (getApi(selectedDevice) > AndroidApiLevel.K19.levelNumber()) {
				showVideoOrientation(true);
			} else {
				showVideoOrientation(false);
			}
			return;
		} else if (ac.equals(txtSDEF) && getApi(selectedDevice) > AndroidApiLevel.K19.levelNumber()) {
			videoOption = VideoOption.SDEF;
			if (getApi(selectedDevice) > AndroidApiLevel.K19.levelNumber()) {
				showVideoOrientation(true);
			} else {
				showVideoOrientation(false);
			}
			return;
		} else if (ac.equals(txtNONE)) {
			videoOption = VideoOption.NONE;
			showVideoOrientation(false);
			return;
		} else if (ac.equals(txtPortrait)) {
			videoOrient = Orientation.PORTRAIT;
			SettingsImpl.getInstance().setAndSaveAttribute("orientation", videoOrient.toString().toLowerCase());
			return;
		} else if (ac.equals(txtLandscape)) {
			videoOrient = Orientation.LANDSCAPE;
			SettingsImpl.getInstance().setAndSaveAttribute("orientation", videoOrient.toString().toLowerCase());
			return;
		}// Collector
		else if (ac.equals(rooted)) {
			collector = rootCollector;
			if (btnRooted.isSelected()) {
 
				enableFullVideo(false);
			}
			return;

		} else if (ac.equals(vpn)) {
			collector = vpnCollector;
			if (btnVpn.isSelected()) {

				enableFullVideo(true);
			}

			return;

		}
 
	}

	/**
	 * set attenuate section enabled or disabled based on the selection of
	 * Rooted or VPN or it is a IOS device
	 */
	private void setAttenuateSectionStatus() {
		if (btnVpn.isSelected()||btniOS.isSelected()) {
			attnrGroupPanel.getAttnrRadioGP().reset();
			attnrGroupPanel.setAttenuateEnable(true);
			if(btniOS.isSelected()) {
				attnrGroupPanel.getAttnrRadioGP().setRbAtnrLoadFileEnable(false);
			}
		} else {
			attnrGroupPanel.setAttenuateEnable(false);
		}
	}

	public boolean isSecure() {
		return secure;
	}
 

	public AttnrPanel getAttnrGroup() {
		if (attnrGroupPanel == null) {
			miniAtnr = new AttenuatorModel();
			
			attnrGroupPanel = new AttnrPanel(this,parent, miniAtnr);
			Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			attnrGroupPanel.setBorder(loweredetched);
		}
		return attnrGroupPanel;
	}



	private JPanel getRadioGroupVideo() {
		loadRadioGroupVideo();
		JPanel btnGrp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btnGrp.add(btn_lrez);
		btnGrp.add(btn_hdef);
		btnGrp.add(btn_sdef);
		btnGrp.add(btn_none);

		return btnGrp;
	}

	private JPanel getRadioGroupVideoOrient() {
		loadRadioGroupVideoOrient();
		JPanel btnGrp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btnGrp.add(btn_portrait);
		btnGrp.add(btn_landscape);
		return btnGrp;
	}

	private JPanel getRadioGroupCollector() {
		loadRadioGroupCollector();
		JPanel btnGrp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btnGrp.add(btnRooted);
		btnGrp.add(btnVpn);
		btnGrp.add(btniOS);
		return btnGrp;
	}
 

	private void loadRadioGroupCollector() {
		rooted = ResourceBundleHelper.getMessageString("dlog.collector.option.rooted");
		vpn = ResourceBundleHelper.getMessageString("dlog.collector.option.vpn");
		ios =  ResourceBundleHelper.getMessageString("dlog.collector.option.ios");
		
		btnRooted = new JRadioButton(rooted);
		btnVpn = new JRadioButton(vpn);
		btniOS = new JRadioButton(ios);

		btnRooted.setActionCommand(rooted);
		btnVpn.setActionCommand(vpn);
		btniOS.setActionCommand(ios);

		btnRooted.setMnemonic(KeyEvent.VK_R);
		btnVpn.setMnemonic(KeyEvent.VK_V);
		btniOS.setMnemonic(KeyEvent.VK_W);

		btnRooted.addActionListener(this);
		btnVpn.addActionListener(this);
		btniOS.addActionListener(this);

 		btnVpn.setSelected(true);
 
		// only group the rooted & vpn
		radioBtnVpnRoot = new ButtonGroup();
		radioBtnVpnRoot.add(btnRooted);
		radioBtnVpnRoot.add(btnVpn);
		radioBtnVpnRoot.add(btniOS);		
		
	}

	private void loadRadioGroupVideo() {
		txtLREZ = ResourceBundleHelper.getMessageString("dlog.collector.option.video.orig");
		txtHDEF = ResourceBundleHelper.getMessageString("dlog.collector.option.video.hdef");
		txtSDEF = ResourceBundleHelper.getMessageString("dlog.collector.option.video.sdef");
		txtNONE = ResourceBundleHelper.getMessageString("dlog.collector.option.video.none");

		btn_lrez = new JRadioButton(txtLREZ);
		btn_hdef = new JRadioButton(txtHDEF);
		btn_sdef = new JRadioButton(txtSDEF);
		btn_none = new JRadioButton(txtNONE);

		btn_lrez.addActionListener(this);
		btn_hdef.addActionListener(this);
		btn_sdef.addActionListener(this);
		btn_none.addActionListener(this);

		ButtonGroup radioBtnVideo = new ButtonGroup();
		radioBtnVideo.add(btn_lrez);
		radioBtnVideo.add(btn_hdef);
		radioBtnVideo.add(btn_sdef);
		radioBtnVideo.add(btn_none);

	}

	private void loadRadioGroupVideoOrient() {
		txtPortrait = ResourceBundleHelper.getMessageString("dlog.collector.option.video.orient.portrait");
		txtLandscape = ResourceBundleHelper.getMessageString("dlog.collector.option.video.orient.landscape");

		btn_portrait = new JRadioButton(txtPortrait);
		btn_landscape = new JRadioButton(txtLandscape);

		btn_portrait.addActionListener(this);
		btn_landscape.addActionListener(this);

		ButtonGroup radioBtnVideoOrient = new ButtonGroup();
		radioBtnVideoOrient.add(btn_portrait);
		radioBtnVideoOrient.add(btn_landscape);
	}

	/**
	 * Sets default options based on the selectedIAroDevice
	 * 
	 * @param selectedIAroDevice
	 */
	public boolean setDevice(IAroDevice selectedIAroDevice) {

		selectedDevice = selectedIAroDevice;

		switch (selectedIAroDevice.getPlatform()) {

		case iOS:
			setVisible(true);
			collector = iosCollector;

			btniOS.setEnabled(true);			
			btniOS.setSelected(true);
			btnVpn.setEnabled(false);
			btnRooted.setEnabled(false);
			
			String ver = selectedDevice.getApi();
			int major = StringUtils.isBlank(ver) ? 0 : Integer.valueOf(ver.split("\\.")[0]);
			boolean hdVideo = (major >= 11) ? true : false;
			if (hdVideo && AppSigningHelper.isCertInfoPresent()) {
				btn_hdef.setEnabled(true);
				btn_sdef.setEnabled(false);
				btn_lrez.setEnabled(false);
				btn_hdef.setSelected(true);
				btn_none.setEnabled(false);
				videoOption = VideoOption.HDEF;
			} else {
				enableFullVideo(false);
				// set default video
				btn_lrez.setSelected(true);
				videoOption = VideoOption.LREZ;
			}

			showVideoOrientation(false);
			setAttenuateSectionStatus();
			break;

		case Android:
			api = getApi(selectedIAroDevice);
			setVisible(true);
			enableFullVideo(true);
			// set default video
			btn_lrez.setSelected(true);
			videoOption = VideoOption.LREZ;

			showVideoOrientation(false); // false because LREZ is selected by
											// default

			btniOS.setEnabled(false);
			btnVpn.setEnabled(true);
			btnRooted.setEnabled(true);
			
			String abi = selectedIAroDevice.getAbi();
			if (selectedIAroDevice.isEmulator()) {
				if (abi.contains("x86")) {
					setRootState(true);
					collector = null;
					btnRooted.setEnabled(false);
					btnVpn.setEnabled(false);

				} else {
					setRootState(true);
					collector = rootCollector;
					btnRooted.setEnabled(rootCollector != null);
					btnVpn.setEnabled(false);
					btnRooted.setEnabled(true);
				}
			} else if (selectedIAroDevice.isRooted()) {
				setRootState(true);
				btnRooted.setEnabled(true);
				btnVpn.setEnabled(true);
				btnRooted.setSelected(true);
				collector = rootCollector;
			} else {
				setRootState(false);
				btnRooted.setEnabled(false);
				btnVpn.setEnabled(true);
				btnVpn.setSelected(true);
				collector = vpnCollector;
			}

			// quick hack to allow or disallow full-motion video
			if (!selectedIAroDevice.isEmulator()) {
				if (api < AndroidApiLevel.K19.levelNumber()) {
					// neither screenrecord or media projection is possible
					// below Kitkat Lollipop and on N-Preview or any phone where
					// cannot access api level
					enableFullVideo(false);
					enableVpnCapture(false);
				} else if (api == AndroidApiLevel.K19.levelNumber()) {
					// screenrecord can have limitations on some phones for "HD"
					/*
					 * screenrecord is used to capture SD and HD for Kitkat
					 * devices, while media projection is used to capture SD and
					 * HD for Lollipop or above devices
					 */
					enableFullVideo(!selectedIAroDevice.getModel().equals("SAMSUNG-SM-J320A")
							&& !selectedIAroDevice.getModel().equals("LG-K425"));
					enableVpnCapture(true);
				} else {
					// Not able to confirm if media projection works on the
					// device, so disable options
					enableFullVideo(!selectedIAroDevice.getModel().equals("SAMSUNG-SM-J320A"));
					enableVpnCapture(true);
				}
				if (selectedIAroDevice.isRooted()) {
					btnRooted.setSelected(true);
					enableFullVideo(false);		
				}
			} else {
				enableFullVideo(false);
				enableVpnCapture(false);
			}
 
			setAttenuateSectionStatus();
			break;

		default:
			break;
		}
		return (collector != null);
	}

	private int getApi(IAroDevice aroDevice) {
		if (aroDevice == null) {
			return 0;
		}
		return aroDevice.getApi() == null ? 0 : Integer.valueOf(aroDevice.getApi());
	}

	private void enableVpnCapture(boolean boolFlag) {

		btnVpn.setEnabled(boolFlag);
		btnVpn.setSelected(boolFlag);
		

	}

	/**
	 * enable or disable HD & SD buttons
	 * 
	 * @param boolFlag
	 */
	private void enableFullVideo(boolean boolFlag) {
		btn_hdef.setEnabled(boolFlag);
		btn_sdef.setEnabled(boolFlag);
	}

	/**
	 * Hides/Shows Video Orientation label, Portrait button & Landscape button.
	 * 
	 * @param boolFlag
	 */
	public void showVideoOrientation(boolean boolFlag) {

		labelVideoOrientTitle.setVisible(boolFlag);
		videoOrientRadioGrpPanel.setVisible(boolFlag);

		// Reset selection to settings every time we disable the video
		// orientation option
		if (!boolFlag) {
			(videoOrient == Orientation.LANDSCAPE ? btn_landscape : btn_portrait).setSelected(true);
		}
	}

	/**
	 * sets radioButtons to reflect root status
	 * 
	 * @param rootedState
	 */
	public void setRootState(boolean rootedState) {
		btnRooted.setSelected(rootedState);
		btnVpn.setSelected(!rootedState);
	}

	/**
	 * Retrieve collector name
	 * 
	 * @return collector
	 */
	public IDataCollector getCollector() {
		return collector;
	}

	public VideoOption getVideoOption() {
		return videoOption;
	}

	public Orientation getVideoOrientation() {
		SettingsImpl.getInstance().setAndSaveAttribute("orientation", videoOrient.toString().toLowerCase());
		return videoOrientRadioGrpPanel.isVisible() && btn_landscape.isSelected()
				? Orientation.LANDSCAPE : Orientation.PORTRAIT;
	}

	public AttenuatorModel getMiniAtnr() {
		return miniAtnr;
	}
	public void setMiniAtnr(AttenuatorModel miniAtnr) {
		this.miniAtnr = miniAtnr;
	}

}
