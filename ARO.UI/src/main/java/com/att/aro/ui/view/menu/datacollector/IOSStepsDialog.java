/*
 * Copyright 2019 AT&T
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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.ui.commonui.BrowserLauncher;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.utils.ResourceBundleHelper;


/**
 * Popup dialog to remind iOS user to share hotspot.
 */

public class IOSStepsDialog extends JDialog {
	
	private static final long serialVersionUID = 1L;
	private JButton okButton;
	private static final String VO_USER_GUIDE_URL = ResourceBundleHelper.getURLResource("help.userguide.url");

	public IOSStepsDialog(DataCollectorSelectNStartDialog dataCollectorSelectNStartDialog) {
		super(dataCollectorSelectNStartDialog);
		setTitle(ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.title"));
		setModal(true);
		// offset from parent dialog
		Point loc = dataCollectorSelectNStartDialog.getLocationOnScreen();
		loc.x += 665;
		setLocation(loc);
		getContentPane().setLayout(new BorderLayout());
		setSize(new Dimension(650, 450));
		
		JLabel stringLabel = new JLabel(MessageFormat.format(ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction"), 
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.info"),
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.step1"),
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.step2"),
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.step3"),
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.step4"),
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.step5"),
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.step6"),
				 ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.step7")));

		JLabel link = new JLabel(ResourceBundleHelper.getMessageString("dlog.collector.option.ios.instruction.learnmore"));
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.setForeground(Color.blue);
		link.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				BrowserLauncher bg = new BrowserLauncher();
				bg.launchURI(VO_USER_GUIDE_URL);
			}
			
		});
		
		GridBagConstraints gbSetting1 = new GridBagConstraints();
		gbSetting1.fill = GridBagConstraints.HORIZONTAL;
		gbSetting1.gridwidth = 2;
		gbSetting1.gridx = 0;
		gbSetting1.gridy = 0;
		gbSetting1.weightx = 1.0;
		gbSetting1.insets = new Insets(2,5,0,0);  
		gbSetting1.anchor = GridBagConstraints.WEST;
		
		GridBagConstraints gbSetting2 = new GridBagConstraints();
		gbSetting2.gridwidth = 1;
		gbSetting2.gridx = 0;
		gbSetting2.gridy = 1;
		gbSetting2.weightx = 9.0;
		gbSetting2.insets = new Insets(2,15,0,0); 
		gbSetting2.anchor = GridBagConstraints.WEST;
		gbSetting2.fill = GridBagConstraints.HORIZONTAL;

		GridBagConstraints gbSetting3 = new GridBagConstraints();
		gbSetting3.anchor = GridBagConstraints.CENTER;
		
		gbSetting3.gridwidth = 1;
		gbSetting3.gridx = 0;
		gbSetting3.gridy = 2;

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new GridBagLayout());
		
		contentPanel.add(stringLabel,gbSetting1);
		contentPanel.add(link,gbSetting2);
		contentPanel.add(getStartButton("Button.ok"),gbSetting3);

		add(contentPanel);
		setContentPane(contentPanel);
		setVisible(true);

	}


	private JButton getStartButton(String buttonStr) {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(ResourceBundleHelper.getMessageString(buttonStr));
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					setVisible(false);
				}
			});
		}
		return okButton;
	}

}
