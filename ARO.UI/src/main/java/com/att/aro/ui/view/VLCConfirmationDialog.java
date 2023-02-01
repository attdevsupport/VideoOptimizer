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
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.core.util.VLCConfirmationImpl;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VLCConfirmationDialog extends ConfirmationDialog {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();

	private JLabel vlcMsgLabel;
	public VLCConfirmationImpl vlcImpl = ContextAware.getAROConfigContext().getBean("vlcConfirmationImpl", VLCConfirmationImpl.class);

	public void createDialog() {
		setUndecorated(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(resourceBundle.getString("vlc.dialog.title"));
		setResizable(false);
		setBounds(600, 500, 600, 350);
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

		vlcMsgLabel = new JLabel(getLabelMsg());
		vlcMsgLabel.setFont(new Font("vlcLabel", Font.PLAIN, 12));
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new GridBagLayout());
		labelPanel.add(vlcMsgLabel, constraint);
		panel.add(labelPanel);

		JPanel btnPanel = new JPanel(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(1, 150, 1, 180));
		okBtn = new JButton("OK");
		okBtn.setFont(new Font("okBtn", Font.PLAIN, 12));

		// okBtn.setPreferredSize(new Dimension(15, 10));
		btnPanel.add(okBtn);
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		panel.add(btnPanel);

		pack();
		panel.setSize(panel.getPreferredSize());
		panel.validate();
	}

	public String getLabelMsg() {
		return resourceBundle.getString("vlc.dialog.message");
	}

}
