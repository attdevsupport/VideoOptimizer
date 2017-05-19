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
package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import com.att.aro.core.peripheral.pojo.PrivateDataInfo;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class AddKeywordDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	
	private JPanel jDialogPanel;
	
	private JPanel inputPanel;
	
	private JPanel ctrlPanel;
	private JPanel buttonGrid;
	private JButton okButton;
	private JButton cancelButton;
	private JTextField textField;
	
	private enum DialogItem {
		private_data_dialog_button_ok,
		private_data_dialog_button_cancel
	}
	
	public AddKeywordDialog(Window owner) {
		super(owner);
		initialize();
	}

	private void initialize() {
		this.setSize(400, 200);
		this.setResizable(false);
		this.setModal(true);
		this.setAlwaysOnTop(true);
		this.setTitle(resourceBundle.getString("addkeyword.title"));
		this.setLocationRelativeTo(getOwner());
		this.setContentPane(getJDialogPanel());
	}
	
	private JPanel getJDialogPanel() {
		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setLayout(new BorderLayout());
			
			jDialogPanel.add(getInputPanel(), BorderLayout.CENTER);
			jDialogPanel.add(getCtrlPanel(), BorderLayout.SOUTH);
		}
		
		return jDialogPanel;
	}
	
	private JPanel getInputPanel() {
		if (inputPanel == null) {
			inputPanel = new JPanel();
			SpringLayout layout = new SpringLayout();
			inputPanel.setLayout(layout);
			
			JLabel label = getTypeLabel();
			inputPanel.add(label);
			JTextField textField = getTypeValue();
			inputPanel.add(textField);
			
			layout.putConstraint(SpringLayout.WEST, label, 80, SpringLayout.WEST, inputPanel);
			layout.putConstraint(SpringLayout.WEST, textField, 130, SpringLayout.WEST, inputPanel);
			layout.putConstraint(SpringLayout.NORTH, label, 50, SpringLayout.NORTH, inputPanel);
			layout.putConstraint(SpringLayout.NORTH, textField, 50, SpringLayout.NORTH, inputPanel);
		}
		
		return inputPanel;
	}
	
	private JLabel getTypeLabel() {
		JLabel label = new JLabel();
		label.setText(resourceBundle.getString("addkeyword.type"));
		return label;
	}
	
	private JTextField getTypeValue() {
		if (textField == null) {
			textField = new JTextField();
			textField.setPreferredSize(new Dimension(180, 20));
		}
		
		return textField;
	}
	
	private JPanel getCtrlPanel() {
		if (ctrlPanel == null) {
			ctrlPanel = new JPanel();
			ctrlPanel.setLayout(new BorderLayout());
			ctrlPanel.add(getButtonGrid(), BorderLayout.CENTER);
		}
		
		return ctrlPanel;
	}
	
	private JPanel getButtonGrid() {
		if (buttonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(2);
			buttonGrid = new JPanel();
			buttonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			buttonGrid.setLayout(gridLayout);
			
			buttonGrid.add(getOkButton());
			buttonGrid.add(getCancelButton());
		}
		
		return buttonGrid;
	}
	
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_button_ok));
			final Window helper = this;
			okButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!textField.getText().isEmpty()) {
						executeOkButton();
					} else {
						String message = resourceBundle.getString("error.addkeyword.message");
						MessageDialogFactory.getInstance().showErrorDialog(helper, message);
					}
				}
			});
		}
		
		return okButton;
	}
	
	private void executeOkButton() {
		PrivateDataInfo info = new PrivateDataInfo();
		info.setType(textField.getText());
		((PrivateDataDialog) this.getOwner()).addNewEntry(info);
		setVisible(false);
		dispose();
	}
	
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_button_cancel));
			cancelButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					executeCancelButton();
				}
			});
		}
		
		return cancelButton;
	}
	
	private void executeCancelButton() {
		setVisible(false);
		dispose();
	}
}
