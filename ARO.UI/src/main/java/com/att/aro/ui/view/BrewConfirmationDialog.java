package com.att.aro.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.att.aro.core.util.BrewConfirmationImpl;
import com.att.aro.ui.commonui.ContextAware;

public class BrewConfirmationDialog extends ConfirmationDialog {

	private BrewConfirmationImpl brewConfirmationImpl = ContextAware.getAROConfigContext()
			.getBean("brewConfirmationImpl", BrewConfirmationImpl.class);

	private static final long serialVersionUID = 1L;
	private JLabel brewLabel;
	private JCheckBox dontShowAgainCheckBox;

	@Override
	public void createDialog() {
		// TODO Auto-generated method stub
		super.createDialog();
		setUndecorated(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(resourceBundle.getString("brew.dialog.title"));
		setResizable(false);
		setBounds(500, 400, 600, 350);
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

		brewLabel = new JLabel(getLabelMsg());
		brewLabel.setFont(new Font("brewLabel", Font.PLAIN, 12));
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new GridBagLayout());
		labelPanel.add(brewLabel, constraint);
		panel.add(labelPanel);

		dontShowAgainCheckBox = new JCheckBox("Skip this version.", false);
		JPanel checkboxPanel = new JPanel(new BorderLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		checkboxPanel.add(dontShowAgainCheckBox);
		panel.add(checkboxPanel);

		JPanel btnPanel = new JPanel(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(1, 150, 1, 180));
		okBtn = new JButton("OK");
		okBtn.setFont(new Font("okBtn", Font.BOLD, 15));
		btnPanel.add(okBtn);
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (dontShowAgainCheckBox.isSelected()) {
					brewConfirmationImpl.saveLastBrewVersion();
				}
				dispose();
			}
		});
		panel.add(btnPanel);

		pack();
		panel.setSize(panel.getPreferredSize());
		panel.validate();

	}

	@Override
	public String getLabelMsg() {
		// TODO Auto-generated method stub
		return MessageFormat.format(resourceBundle.getString("brew.dialog.message"),
				brewConfirmationImpl.getLocalBrewVersion(), brewConfirmationImpl.getSuggestBrewVersion());
	}

}
