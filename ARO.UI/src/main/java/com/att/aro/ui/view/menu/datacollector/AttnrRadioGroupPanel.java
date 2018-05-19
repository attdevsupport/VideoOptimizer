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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.impl.LoggerImpl;
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.core.util.NetworkUtil;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class AttnrRadioGroupPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private static final String SharedNetIF = "bridge100";
	private static final String PORT_NUMBER = "8080";
	private LoggerImpl log = new LoggerImpl(this.getClass().getName());

	private String attnrSlider;
	private String attnrLoadFile;
	private String attnrNone;

	private JRadioButton attnrSliderRBtn;
	private JRadioButton rbAtnrLoadFile;
	private JRadioButton rbAtnrNone;
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

		attnrSliderRBtn = new JRadioButton(attnrSlider);
		rbAtnrLoadFile = new JRadioButton(attnrLoadFile);
		rbAtnrNone = new JRadioButton(attnrNone);

		radioAttnGroup = new ButtonGroup();
		radioAttnGroup.add(attnrSliderRBtn);
		radioAttnGroup.add(rbAtnrLoadFile);
		radioAttnGroup.add(rbAtnrNone);

		attnrSliderRBtn.addActionListener(this);
		rbAtnrLoadFile.addActionListener(this);
		rbAtnrNone.addActionListener(this);
		rbAtnrNone.setSelected(true);

		add(rbAtnrNone);

		add(attnrSliderRBtn);

		add(rbAtnrLoadFile);
	}

	@Override
	public void actionPerformed(ActionEvent ac) {

		if (attnrSlider.equals(ac.getActionCommand())) {
			parentPanel.getAttnrHolder().remove(getLoadProfilePanel());
			getLoadProfilePanel().resetComponent();
			parentPanel.getAttnrHolder().add(getThroughputPanel(), BorderLayout.CENTER);
			parentPanel.getAttnrHolder().revalidate();
			parentPanel.getAttnrHolder().repaint();

			miniAtnr.setConstantThrottle(true);
			miniAtnr.setFreeThrottle(false);
			miniAtnr.setLoadProfile(false);
			startDialog.resizeLarge();
			if(DataCollectorType.IOS.equals(deviceInfo.getCollector().getType())) {
				MessageDialogFactory.getInstance().showInformationDialog(this, messageComposed(),
						ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.attenuation.title"));
			}

		} else if (attnrLoadFile.equals(ac.getActionCommand())) {
			parentPanel.getAttnrHolder().remove(getThroughputPanel());
			getThroughputPanel().resetComponent();
			parentPanel.getAttnrHolder().add(getLoadProfilePanel());
			parentPanel.getAttnrHolder().revalidate();
			parentPanel.getAttnrHolder().repaint();

			miniAtnr.setConstantThrottle(false);
			miniAtnr.setFreeThrottle(false);
			miniAtnr.setLoadProfile(true);
			startDialog.resizeMedium();

		} else if (attnrNone.equals(ac.getActionCommand())) {
			reset();
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
		rbAtnrLoadFile.setEnabled(enable);
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
		startDialog.resizeMedium();
		rbAtnrNone.setSelected(true);
	}

	private String messageComposed() {
		return MessageFormat.format(
				ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.attenuation.reminder"),
				ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.warning"),
				ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.status"),
				detectWifiSharedActive(), ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.ip"),
				detectWifiShareIP(), ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.port"),
				PORT_NUMBER);

	}

	private String detectWifiShareIP() {
		if (NetworkUtil.isNetworkUp(SharedNetIF)) {
			List<InetAddress> listIp = NetworkUtil.listNetIFIPAddress(SharedNetIF);
			for (InetAddress ip : listIp) {
				if (ip instanceof Inet4Address) {
					String ipString = ip.toString().replaceAll("/", "");
					return ipString;
				}
			}
		}
		return "N/A ";
	}

	private String detectWifiSharedActive() {
		if (NetworkUtil.isNetworkUp(SharedNetIF)) {
			return "Active";
		}
		return "InActive";
	}
}
