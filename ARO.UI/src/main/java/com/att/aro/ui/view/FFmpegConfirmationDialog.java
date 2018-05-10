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
package com.att.aro.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.util.FFmpegConfirmationImpl;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class FFmpegConfirmationDialog extends ConfirmationDialog {
	private static final long serialVersionUID = 1L;
	
	private JLabel ffmpegMsgLabel;
	private JCheckBox dontShowAgainCheckBox;

	@InjectLogger
	private static ILogger log;

	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	public FFmpegConfirmationImpl ffmpegImpl = ContextAware.getAROConfigContext().getBean("ffmpegConfirmationImpl",
			FFmpegConfirmationImpl.class);

	public void createDialog() {
		setUndecorated(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(resourceBundle.getString("ffmpeg.dialog.title"));
		setResizable(false);
		setBounds(400, 300, 400, 150);
		setPreferredSize(new Dimension(400, 150));
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 10, 0, 0);
		constraint.weightx = 1;

		ffmpegMsgLabel = new JLabel(getLabelMsg());
		ffmpegMsgLabel.setFont(new Font("ffmpegLabel", Font.PLAIN, 12));
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new GridBagLayout());
		labelPanel.add(ffmpegMsgLabel, constraint);
		panel.add(labelPanel);

		dontShowAgainCheckBox = new JCheckBox("Don't show this message again", false);
		JPanel checkboxPanel = new JPanel(new BorderLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		checkboxPanel.add(dontShowAgainCheckBox);
		panel.add(checkboxPanel);

		JPanel btnPanel = new JPanel(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(1, 150, 1, 180));
		okBtn = new JButton("OK");
		okBtn.setFont(new Font("okBtn", Font.PLAIN, 12));

		//okBtn.setPreferredSize(new Dimension(15, 10));
		btnPanel.add(okBtn);
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (dontShowAgainCheckBox.isSelected()) {
					ffmpegImpl.saveFfmpegDontShowAgainStatus(true);
				}
				dispose();
			}
		});

		panel.add(btnPanel);

		pack();
		panel.setSize(panel.getPreferredSize());
		panel.validate();
	}

	public String getLabelMsg() {
		return resourceBundle.getString("ffmpeg.dialog.message");
	}

}
