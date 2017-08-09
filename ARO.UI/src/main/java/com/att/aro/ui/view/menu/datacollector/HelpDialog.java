/*
 * Copyright 2016 AT&T
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Display help for MultiDevice selector Dialog
 * 
 *
 */
public class HelpDialog extends JDialog implements KeyListener{

	private static final long serialVersionUID = 1L;
	
	private JPanel contentPanel;
	private JScrollPane scrollPane;

	private JButton startButton;

	private JPanel jButtonGrid;

	/**
	 * Create the panel.
	 */
	public HelpDialog(DataCollectorSelectNStartDialog dataCollectorSelectNStartDialog) {
		super(dataCollectorSelectNStartDialog);
		setTitle(ResourceBundleHelper.getMessageString("collector.help.title"));
		setModal(true);
		setAlwaysOnTop(true);
		
		// offset from parent dialog
		Point loc = dataCollectorSelectNStartDialog.getLocationOnScreen();
		loc.x *= 2;
		loc.y /= 4;
		setLocation(loc);
		
		getContentPane().setLayout(new BorderLayout());

		setSize(new Dimension(800, 370));
		getContentPane().add(layoutDataPanel(), BorderLayout.CENTER);
		setBackground(UIManager.getColor("List.selectionInactiveBackground"));
		setVisible(true);
	}

	public JPanel layoutDataPanel() {

		JPanel layout = new JPanel(new BorderLayout());
		layout.add(getContentPanel(),BorderLayout.CENTER);
		layout.add(getJButtonGrid(),BorderLayout.SOUTH);

		return layout;
	}

	private JPanel getContentPanel() {
		if (this.contentPanel == null) {
			this.contentPanel = new JPanel(new BorderLayout());
			this.contentPanel.add(getScrollPane(), BorderLayout.CENTER);
			this.contentPanel.setBackground(UIManager.getColor("List.selectionInactiveBackground"));
		}
		return this.contentPanel;
	}

	/**
	 * Returns the Scroll Pane for the FileCompressionTable.
	 */
	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane(getTextArea());
		}
		return scrollPane;
	}
	
	/**
	 * @return the textArea
	 */
	public JTextPane getTextArea() {
		JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setEditable(false);
		StringBuffer sb = new StringBuffer();
		sb.append(MessageFormat.format(ResourceBundleHelper.getMessageString("collector.help.content"), 
										ApplicationConfig.getInstance().getAppShortName()));
		sb.append(ResourceBundleHelper.getMessageString("collector.help.devices"));
		if (Util.isMacOS()) {
			sb.append(ResourceBundleHelper.getMessageString("collector.help.devices.ios"));
		}
		sb.append(ResourceBundleHelper.getMessageString("collector.help.devices.android"));
 		textPane.setText(sb.toString());
		return textPane;
	}

	/**
	 * This method initializes jButtonGrid
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel();
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(getStartButton("Button.ok"), null);
		}
		return jButtonGrid;
	}

	/**
	 * This method initializes okButton
	 * @param buttonStr 
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getStartButton(String buttonStr) {
		if (startButton == null) {
			startButton = new JButton();
			startButton.setText(ResourceBundleHelper.getMessageString(buttonStr));
			startButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					setVisible(false);
				}
			});
			
		}
		return startButton;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
