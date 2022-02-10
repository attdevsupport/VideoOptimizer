package com.att.aro.ui.commonui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.menu.file.PreferencesDialog;


public class ApplicationRestartDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final SharedAttributesProcesses parent;

	private JPanel jContentPane;
	private JPanel jButtonPanel;
	private JPanel jButtonGrid;


	/**
	 * Initializes a new instance of the AROProgressDialog class using
	 * the specified parent window, and status message.
	 * 
	 * @param parent
	 *            The parent window.
	 * @param message
	 *            The status message to be displayed in the progress dialog.
	 */
	public ApplicationRestartDialog(SharedAttributesProcesses parent, String message, String title) {
		super(parent.getFrame(), title);

		this.parent = parent;
		setResizable(false);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		setContentPane(getContentPane(message));

		pack();
		setLocationRelativeTo(parent.getFrame());
	}

	private JComponent getContentPane(String message) {
		if (jContentPane == null) {
			jContentPane = new JPanel(new BorderLayout());

			JLabel label = new JLabel("<html>" + message + "</html>", SwingConstants.CENTER);
			label.setFont(new Font("Arial", Font.PLAIN, 15));
			JPanel lblPanel = new JPanel(new BorderLayout());
			lblPanel.add(label);

	        jContentPane.add(lblPanel, BorderLayout.CENTER);
			jContentPane.add(getButtonPanel(), BorderLayout.SOUTH);
			jContentPane.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

			jContentPane.setPreferredSize(new Dimension(550, 175));
		}

		return jContentPane;
	}

	private JPanel getButtonPanel() {
		if (jButtonPanel == null) {
			jButtonPanel = new JPanel();
			BorderLayout layout = new BorderLayout();
			layout.setHgap(0);
			jButtonPanel.setLayout(new BorderLayout());
			jButtonPanel.add(getButtonGrid(), BorderLayout.EAST);
		}

		return jButtonPanel;
	}

	private JPanel getButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel();
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(getButton("Edit Memory Settings", (ActionEvent arg) -> launchPrefs()));
			jButtonGrid.add(getButton("Ok", (ActionEvent arg) -> dispose()));
		}

		return jButtonGrid;
	}

	private JButton getButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}
	
	private void launchPrefs(){
		new PreferencesDialog(parent, this).setVisible(true);
		dispose();
	}
}
