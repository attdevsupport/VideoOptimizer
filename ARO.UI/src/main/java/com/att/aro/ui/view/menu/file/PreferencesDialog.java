/*
 *  Copyright 2015 AT&T
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
package com.att.aro.ui.view.menu.file;

import static com.att.aro.ui.view.menu.file.PreferencesDialog.ConfigType.TEXT;
import static javax.swing.BoxLayout.LINE_AXIS;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.log4j.Logger;

import com.att.aro.core.settings.Settings;
import com.att.aro.core.settings.impl.JvmSettings;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;

/**
 * This generates a dialog to view/update the system configuration used for
 * video optimizer.
 * 
 * @author Bharath Kesireddy
 * 
 */
public class PreferencesDialog extends JDialog {
	private static final Logger LOGGER = Logger.getLogger(PreferencesDialog.class);
	private static final long serialVersionUID = 1L;
	private JPanel jContentPane;
	private JPanel buttonPanel;
	private JPanel jButtonGrid;
	private JButton okButton;
	private JPanel optionsPanel;
	private EnableEscKeyCloseDialog enableEscKeyCloseDialog;
	private final SharedAttributesProcesses parent;
	private final JMenuItem callerMenuItem;
	private Map<Config, String> updates = new HashMap<>();

	private Settings settings = SettingsImpl.getInstance();

	/**
	 * Type of configuration This is used to provide appropriate method for
	 * displaying configuration
	 */
	enum ConfigType {
		TEXT, NUMBER, FILE
	}

	/**
	 * Configuration item for dialog Lists all the configurations that are meant
	 * to be displayed on the preferences dialog
	 */
	enum Config {
		// TODO Should update it to replace adb in next iteration after demo
		// ADB("adb2", "adb path", FILE, SettingsImpl.getInstance(), true),
		MEM("Xmx", "Max heap in MB", TEXT, JvmSettings.getInstance(), !(Util.isWindows32OS()||Util.isLinuxOS()));

		private String name;
		private String desc;
		private ConfigType type;
		private Settings settings;
		private Boolean enabled;

		private Config(String name, String desc, ConfigType type, Settings settings, Boolean enabled) {
			this.name = name;
			this.desc = desc;
			this.type = type;
			this.settings = settings;
			this.enabled = enabled;
		}

		public String getName() {
			return name;
		}

		public String getDesc() {
			return desc;
		}

		public ConfigType getConfigType() {
			return type;
		}

		public Settings getSettings() {
			return settings;
		}

		public Boolean isEnabled() {
			return enabled;
		}
	}

	public PreferencesDialog(SharedAttributesProcesses parent, JMenuItem callerMenuItem) {
		super(parent.getFrame());
		this.parent = parent;
		this.callerMenuItem = callerMenuItem;
		init();
	}

	private void init() {
		this.setContentPane(getJContentPane());
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.setTitle("Preferences");
		enableEscKeyCloseDialog = new EnableEscKeyCloseDialog(getRootPane(), this, false);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent event) {
				if (enableEscKeyCloseDialog.consumeEscPressed()) {
					closeDialog();
				}
			}
		});
		pack();
		setLocationRelativeTo(parent.getFrame());
		getRootPane().setDefaultButton(okButton);
	}

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setPreferredSize(new Dimension(500, 120));
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getButtonPanel(), BorderLayout.SOUTH);
			jContentPane.add(getConfigPanel(), BorderLayout.CENTER);
		}
		return jContentPane;
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(getJButtonGrid(), BorderLayout.EAST);
		}
		return buttonPanel;
	}

	private JPanel getJButtonGrid() {
		if (jButtonGrid == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			gridLayout.setHgap(10);
			jButtonGrid = new JPanel();
			jButtonGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			jButtonGrid.setLayout(gridLayout);
			jButtonGrid.add(okButton = getButton("Save & Close", (ActionEvent arg) -> saveAndClose()));
			jButtonGrid.add(getButton("Cancel", (ActionEvent arg) -> closeDialog()));
		}
		return jButtonGrid;
	}

	private JButton getButton(String text, ActionListener al) {
		JButton button = new JButton();
		button.setText(text);
		button.addActionListener(al);
		return button;
	}

	private JPanel getConfigPanel() {
		if (optionsPanel == null) {
			optionsPanel = new JPanel();
			optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
			optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			Arrays.asList(Config.values()).stream().forEach(config -> optionsPanel.add(getConfigCombo(config)));
		}
		return optionsPanel;
	}

	/* Gets a combination of label and update/set a configuration value */
	private Component getConfigCombo(final Config config) {
		JPanel configCombo = new JPanel();
		configCombo.setLayout(new BoxLayout(configCombo, LINE_AXIS));
		configCombo.setAlignmentX(RIGHT_ALIGNMENT);
		JLabel key = new JLabel(config.getDesc());
		configCombo.add(key);
		configCombo.setEnabled(config.isEnabled());
		configCombo.add(getValuePanel(config));
		return configCombo;
	}

	private Component getValuePanel(final Config config) {
		JPanel panel = new JPanel();
		Dimension size = new Dimension(300, 20);
		panel.setLayout(new BoxLayout(panel, LINE_AXIS));
		setSize(panel, size);
		if (config.getConfigType() == ConfigType.TEXT || config.getConfigType() == ConfigType.NUMBER) {
			panel.add(getValueTextField(config, size));
		} else if (config.getConfigType() == ConfigType.FILE) {
			Dimension txtSize = new Dimension(220, 20);
			final JTextField textField = getValueTextField(config, txtSize);
			Dimension btnSize = new Dimension(75, 20);
			JButton btnBrowse = new JButton("Browse");
			setSize(btnBrowse, btnSize);
			btnBrowse.addActionListener((ActionEvent e) -> setPathTextField(textField));
			panel.add(textField);
			panel.add(btnBrowse);
		}
		return panel;
	}

	private void setSize(Component comp, Dimension btnSize) {
		comp.setPreferredSize(btnSize);
		comp.setMinimumSize(btnSize);
		comp.setMaximumSize(btnSize);
	}

	private void setPathTextField(JTextField textField) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(FILES_ONLY);
		File file = new File(textField.getText());
		if (file.exists()) {
			fileChooser.setSelectedFile(file);
		}
		int option = fileChooser.showOpenDialog(this);
		if (option == APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
			textField.setText(fileChooser.getSelectedFile().getPath());
		}
	}

	private JTextField getValueTextField(final Config config, Dimension size) {
		JTextField value = new JTextField(config.getSettings().getAttribute(config.getName()), 5);
		setSize(value, size);
		value.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateText(e.getDocument());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateText(e.getDocument());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateText(e.getDocument());
			}

			private void updateText(Document document) {
				try {
					updates.put(config, document.getText(0, document.getLength()));
				} catch (BadLocationException e) {
					LOGGER.error("Error reading the textbox for:" + config.getName(), e);
				}
			}
		});
		return value;
	}

	private void saveAndClose() {
		try {
			saveValues();
			closeDialog();
		} catch (RuntimeException e) {
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(), e.getMessage(),
					"Failed to save preferences", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void saveValues() {
		if (updates != null && updates.size() > 0) {
			updates.forEach((config, value) -> config.getSettings().setAttribute(config.getName(), value));
			settings.saveConfigFile();
		}
	}

	private void closeDialog() {
		setVisible(false);
		callerMenuItem.setEnabled(true);
	}

}
