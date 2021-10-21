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

import static com.att.aro.ui.view.menu.file.PreferencesDialog.ConfigType.COMBO;
import static com.att.aro.ui.view.menu.file.PreferencesDialog.ConfigType.FILE;
import static com.att.aro.ui.view.menu.file.PreferencesDialog.ConfigType.MEMORY;
import static com.att.aro.ui.view.menu.file.PreferencesDialog.ConfigType.TEXT;
import static javax.swing.BoxLayout.LINE_AXIS;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.settings.Settings;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.settings.impl.JvmSettings;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.NumericInputVerifier;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.menu.datacollector.HelpDialog;

/**
 * This generates a dialog to view/update the system configuration used for
 * video optimizer.
 *
 * @author Bharath Kesireddy
 *
 */

public class PreferencesDialog extends JDialog {
	private static final int BORDER_HEIGHT = 80;
	private static final int BORDER_WIDTH = 15;

	private static final Logger LOGGER = LogManager.getLogger(PreferencesDialog.class);
	private static final long serialVersionUID = 1L;
	private JPanel jContentPane;
	private JPanel buttonPanel;
	private JPanel jButtonGrid;

	private JButton reloadButton;
	private JButton saveButton;
	private JPanel optionsPanel;
	private EnableEscKeyCloseDialog enableEscKeyCloseDialog;
	private final SharedAttributesProcesses parent;
	private final Component callerItem;
	private Map<Config, String> updates = new HashMap<>();
	String logginglevel = "ERROR";
	private Settings settings = SettingsImpl.getInstance();
	private JLabel helpLabel;
	VideoPreferencesPanel videoPreferencesPanel;
	private JTabbedPane tabbedPane;
	private Popup popup;
	private JTextField messageJTextField;
	private static final int DISPLAYTIMER = 3000;
	private Timer timer;
	private static final Color COLOR = new Color(255, 255, 204);
	private ActionListener hider;

	/**
	 * Type of configuration This is used to provide appropriate method for
	 * displaying configuration
	 */
	enum ConfigType {
		TEXT, NUMBER, FILE, COMBO, MEMORY
	}

	/**
	 * Configuration item for dialog Lists all the configurations that are meant to
	 * be displayed on the preferences dialog
	 */
	enum Config {
		MEM("Xmx", "Max heap in GB", MEMORY, JvmSettings.getInstance(), isHeapEnabled()),
		ADB("adb", "Adb Path", FILE, SettingsImpl.getInstance(), true),
		WIRESHARK("WIRESHARK_PATH", "Wireshark Path", FILE, SettingsImpl.getInstance(), Util.isMacOS(), null,
				() -> Util.getWireshark()),
		IDEVICE_SCREENSHOT("iDeviceScreenshot", "iDeviceScreenshot Path", FILE, SettingsImpl.getInstance(),
				Util.isMacOS(), null, () -> Util.getIdeviceScreenshot()),
		FFMPEG("ffmpeg", "FFMpeg Path", FILE, SettingsImpl.getInstance(), true, null, () -> Util.getFFMPEG()),
		FFPROBE("ffprobe", "FFProbe Path", FILE, SettingsImpl.getInstance(), true, null, () -> Util.getFFPROBE()),
		IOS_PROV("iosProv", "iOS Provisioning Profile", FILE, SettingsImpl.getInstance(), Util.isMacOS()),
		IOS_CERT("iosCert", "iOS Certificate", TEXT, SettingsImpl.getInstance(), Util.isMacOS(),
				ResourceBundleHelper.getMessageString("preferences.iosCert.textField.hint"), null),
		IOS_BUNDLE("iosBundle", "iOS Bundle Identifier", TEXT, SettingsImpl.getInstance(), Util.isMacOS()),
		LOG_LVL("logging", "Logging Level", COMBO, SettingsImpl.getInstance(), true,
				ResourceBundleHelper.getMessageString("preferences.logging.dropdown.values"));

		private String name;
		private String desc;
		private ConfigType type;
		private Settings settings;
		private Boolean enabled;
		private String hint; // tool tip text for JTextField
		private Supplier<String> defValue;
		private String comboValues;

		private Config(String name, String desc, ConfigType type, Settings settings, Boolean enabled, String hint,
				Supplier<String> defValue) {
			this(name, desc, type, settings, enabled);
			this.hint = hint;
			this.defValue = defValue;
		}

		private Config(String name, String desc, ConfigType type, Settings settings, Boolean enabled) {
			this.name = name;
			this.desc = desc;
			this.type = type;
			this.settings = settings;
			this.enabled = enabled;
		}

		private Config(String name, String desc, ConfigType type, Settings settings, Boolean enabled,
				String comboValues) {
			this(name, desc, type, settings, enabled);
			this.comboValues = comboValues;
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

		public String getHint() {
			return hint;
		}

		public String getDefValue() {
			return defValue != null ? defValue.get() : null;
		}

		public String getComboValues() {
			return comboValues;
		}

		/**
		 * Heap is enabled when: Linux or Mac/Win64 and memory > 4GB
		 * 
		 * Not enabled when Win32
		 * 
		 * @return
		 */
		private static boolean isHeapEnabled() {
			return Util.isLinuxOS()
					|| (!Util.isWindows32OS() && ((JvmSettings) JvmSettings.getInstance()).getSystemMemory() > 4 * JvmSettings.MULTIPLIER);
		}
	}

	public PreferencesDialog(SharedAttributesProcesses parent, Component caller) {
		super(parent.getFrame());
		this.parent = parent;
		this.callerItem = caller;
		callerItem.setEnabled(false);
		init();
	}

	private void init() {
		this.setContentPane(getJContentPane());
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setTitle("Preferences");
		enableEscKeyCloseDialog = new EnableEscKeyCloseDialog(getRootPane(), this, false);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent event) {
				if (enableEscKeyCloseDialog.consumeEscPressed()) {
					dispose();
				}
			}
		});
		pack();
		setLocationRelativeTo(parent.getFrame());
		getRootPane().setDefaultButton(reloadButton);
	}

	private JComponent getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setPreferredSize(new Dimension(750, 500));
			jContentPane.setLayout(new BorderLayout());
			videoPreferencesPanel = new VideoPreferencesPanel(this);
			tabbedPane = new JTabbedPane();
			tabbedPane.addTab(ResourceBundleHelper.getMessageString("preferences.general.tabtile"), getGeneralTab());
			tabbedPane.addTab(ResourceBundleHelper.getMessageString("preferences.bestpractice.tabtile"),
					BPSelectionPanel.getBPPanel());
			tabbedPane.addTab(ResourceBundleHelper.getMessageString("preferences.video.tabtitle"),
					videoPreferencesPanel);

			this.addComponentListener(new ComponentListener() {
				@Override
				public void componentResized(ComponentEvent e) {
					Dimension d = e.getComponent().getSize();
					tabbedPane.setSize(
							new Dimension((int) d.getWidth() - BORDER_WIDTH, (int) d.getHeight() - BORDER_HEIGHT));
				}

				@Override
				public void componentMoved(ComponentEvent e) {
				}

				@Override
				public void componentShown(ComponentEvent e) {
				}

				@Override
				public void componentHidden(ComponentEvent e) {
				}
			});
			jContentPane.add(getButtonPanel(), BorderLayout.SOUTH);
			jContentPane.add(tabbedPane, BorderLayout.NORTH);
		}
		return jContentPane;
	}

	private JPanel getGeneralTab() {
		JPanel general = new JPanel();
		general.add(getEmptyPanel(), BorderLayout.WEST);
		general.add(getConfigPanel(), BorderLayout.CENTER);
		general.add(getHelpPanel(), BorderLayout.EAST);
		return general;
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(getMessageJTextField(), BorderLayout.NORTH);
			getMessageJTextField().setVisible(false);
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
			jButtonGrid.add(saveButton = getButton("Save Only", (ActionEvent arg) -> saveOnly()));
			jButtonGrid.add(reloadButton = getButton("Save & Reload", (ActionEvent arg) -> saveAndReload()));
			jButtonGrid.add(getButton("Cancel", (ActionEvent arg) -> dispose()));
		}
		return jButtonGrid;
	}

	private JTextField getMessageJTextField() {
		if (messageJTextField == null) {
			messageJTextField = new JTextField();
		}
		return messageJTextField;
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
			Arrays.asList(Config.values()).stream().filter(config -> config.isEnabled())
					.forEach(config -> optionsPanel.add(getConfigCombo(config)));
		}
		return optionsPanel;
	}

	/* Gets a combination of label and update/set a configuration value */
	private Component getConfigCombo(final Config config) {
		JPanel configCombo = new JPanel();
		configCombo.setLayout(new BoxLayout(configCombo, LINE_AXIS));
		configCombo.setAlignmentX(RIGHT_ALIGNMENT);
		JLabel key = new JLabel(config.getDesc());
		key.setBorder(new EmptyBorder(0, 0, 0, 5));
		configCombo.add(key);
		configCombo.setEnabled(config.isEnabled());
		configCombo.add(getValuePanel(config));
		return configCombo;
	}

	private Component getValuePanel(final Config config) {
		int fieldHeight = Util.isLinuxOS() ? 28 : 20;
		JPanel panel = new JPanel();
		Dimension size = new Dimension(300, fieldHeight);
		panel.setLayout(new BoxLayout(panel, LINE_AXIS));
		setSize(panel, size);
		if (config.getConfigType() == ConfigType.TEXT) {
			panel.add(getValueTextField(config, size));
		} else if (config.getConfigType() == ConfigType.MEMORY || config.getConfigType() == ConfigType.NUMBER) {
			double maximumValue = 1;
			JTextField textField = getValueTextField(config, size);
			if (config.getConfigType() == ConfigType.MEMORY) {
				maximumValue = ((JvmSettings) JvmSettings.getInstance()).getMaximumMemoryGB();
			}
			textField.setInputVerifier(new NumericInputVerifier(maximumValue, 2, 1, false, this));
			textField.addKeyListener(getKeyListener(textField));
			textField.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					if (hider != null) {
						closePopup();
					}
				}

				@Override public void focusGained(FocusEvent e) {}
			});
			panel.add(textField);

		} else if (config.getConfigType() == ConfigType.FILE) {
			Dimension txtSize = new Dimension(220, fieldHeight);
			final JTextField textField = getValueTextField(config, txtSize);
			Dimension btnSize = new Dimension(75, fieldHeight);
			JButton btnBrowse = new JButton("Browse");
			setSize(btnBrowse, btnSize);
			btnBrowse.addActionListener((ActionEvent e) -> setPathTextField(textField));
			panel.add(textField);
			panel.add(btnBrowse);
		} else if (config.getConfigType() == ConfigType.COMBO) {
			JComboBox<String> comboBox = new JComboBox<>(getComboValue(config));
			comboBox.addActionListener((ActionEvent e) -> setLoggingLvl((String) comboBox.getSelectedItem()));
			comboBox.setSelectedItem(Util.getLoggingLevel());
			panel.add(comboBox);
		}
		return panel;
	}

	private KeyListener getKeyListener(JTextField textField) {
		return new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getID() == KeyEvent.KEY_TYPED) {
					/*
					 * Will allow numbers 0-9 and backspace, delete and decimal point Cannot (1)
					 * place more than one decimal point ex 1.2.3 or 12..3 (2) have more than 1
					 * significant digit ex:12.34 (3) cannot hav2 more than two Significand ex:123.5
					 */
					int inputCharCode = (int) e.getKeyChar(); // e.getExtendedKeyCode() will not work on Windows
					if (inputCharCode == 10) { // do not process when user has pressed return, [Save] has it's own validation
						e.consume();
						return;
					}
					boolean isAlphabet = inputCharCode != 8 && (inputCharCode != 46 && inputCharCode > 31
							&& (inputCharCode < 48 || inputCharCode > 57));

					int caretPos;
					int decimalPointPos;

					String text = textField.getText();
					String replacementStr = !isAlphabet && inputCharCode != 8 ? String.valueOf((char) inputCharCode) : "";

					if (textField.getSelectedText() != null) {
						// replace char
						if (textField.getSelectedText().equals(textField.getText())) {
							text = replacementStr;
							caretPos = 0;
						} else {
							caretPos = textField.getCaretPosition();
							text = text.substring(0, caretPos) + replacementStr
									+ text.substring(caretPos + textField.getSelectedText().length());
						}
					} else {
						// insert char
						caretPos = textField.getCaretPosition();
						text = text.substring(0, caretPos) + replacementStr + text.substring(caretPos);
					}
					decimalPointPos = text.indexOf(".");

					String errorMessage = null;
					if (isAlphabet) {
						errorMessage = String.format("Invalid, only numbers, from 2 to 99.9 are allowed: %s", text);
					} else if (text.startsWith("0") || text.startsWith(".")) {
						errorMessage = String.format("leading zeroes or leading decimal mark are not allowed: %s", text);
					} else if (decimalPointPos == -1 && caretPos > 1 && inputCharCode != 46) {
						errorMessage = String.format("Too many digits before decimal point, no more than %d is allowed: %s", 2, text);
					} else if (decimalPointPos > -1) {
						if (StringUtils.countMatches(text, ".") > 1) {
							errorMessage = String.format("Too many Decimal points, no more than %d is allowed: %s", 2, text);
						} else if (decimalPointPos > 2) {
							errorMessage = String.format("Too many digits before decimal point, no more than %d is allowed: %s", 2, text);
						} else if ((text.length() - decimalPointPos) > 2) {
							errorMessage = String.format("Too many significant digits, no more than %d is allowed: %s", 1, text);
						}
					}
					if (errorMessage != null) {
						popup(textField, errorMessage);
						LOGGER.debug(errorMessage);
						e.consume();
					} else if (hider != null) {
						closePopup();
					}
				}
			}
		};
	}

	/**
	 * Popup a display offset to the right of the JComponent and slightly lower
	 * 
	 * @param component
	 * @param messageText
	 */
	public void popup(JComponent component, String messageText) {
		try {
			Point position = component.getLocationOnScreen();
			int yOffset = position.y + (int) (component.getHeight() * 0.2);
			int xOffset = position.x + (int) (component.getWidth() * 1.1);

			PopupFactory factory = PopupFactory.getSharedInstance();
			JTextField message = new JTextField(messageText);
			message.setBackground(COLOR);

			if (hider != null) {
				closePopup();
			}
			
			popup = factory.getPopup(component.getParent(), message, xOffset, yOffset);
			this.setPopup(popup);
			popup.show();

			hider = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					timer.stop();
					popup.hide();
				}
			};
			// Hide popup in 3 seconds
			if (timer != null) {
				timer.stop();
			}
			timer = new Timer(DISPLAYTIMER, hider);
			timer.start();

		} catch (IllegalComponentStateException e) {
			LOGGER.error("ERROR: component location cannot be retrieved. " + e.getLocalizedMessage());
		}
	}

	private void closePopup() {
		timer.stop();
		popup.hide();
		while (timer.isRunning()) {
			Util.sleep(100);
		}
		hider = null;
	}

	private String[] getComboValue(Config config) {
		String[] value = new String[0];
		if (config.getComboValues() != null) {
			value = config.getComboValues().split(",");
		}
		return value;
	}

	public void setLoggingLvl(String selectedItem) {
		logginglevel = selectedItem;
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
		setToolTip(config, value);
		if (config.getDefValue() != null) {
			value.setText(config.getDefValue().replaceAll("\"", ""));
		}
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
		value.setEnabled(config.isEnabled());
		return value;
	}

	private void setToolTip(Config config, JTextField textField) {
		String toolTipText = config.getHint();
		if (toolTipText != null) {
			textField.setToolTipText(toolTipText);
		}
	}

	private void saveOnly() {
		saveOptions(false);
	}

	private void saveAndReload() {
		saveOptions(true);
	}

	private void saveOptions(boolean reload) {
		try {
			saveGenTabValues();
			saveBPSelection(reload);
			SettingsImpl.getInstance().setAndSaveAttribute("LOG_LEVEL", logginglevel);
			Util.setLoggingLevel(logginglevel);
			if (videoPreferencesPanel.saveVideoPreferences()) {
				dispose();
			} else {
				this.revalidate();
				this.repaint();
			}

		} catch (IllegalArgumentException iae) {
			displayErrorMessage(iae.getMessage());
			LOGGER.error(iae.getMessage());
		} catch (Exception e) {
			LOGGER.error("Failed to save preferences", e);
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
					"Error occurred while trying to save Preferences", "Error saving preferences",
					JOptionPane.ERROR_MESSAGE);
			displayErrorMessage("Error saving preferences " + e.getMessage());
		}
	}

	private void displayErrorMessage(String message) {
		messageJTextField.setText(message);
		messageJTextField.setVisible(true);
		this.pack();
	}

	private void saveGenTabValues() {
		if (updates != null && updates.size() > 0) {
			updates.forEach((config, value) -> config.getSettings().setAttribute(config.getName(), value));
			settings.saveConfigFile();
		}
	}

	private String getTracePath() {
		String tracePath = null;
		if (((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult() != null && ((MainFrame) parent)
				.getController().getCurrentTraceInitialAnalyzerResult().getTraceresult() != null) {

			tracePath = ((MainFrame) parent).getController().getCurrentTraceInitialAnalyzerResult().getTraceresult()
					.getTraceFile();

		}
		return tracePath;
	}

	private void saveBPSelection(boolean reload) {
		SettingsUtil.saveBestPractices(BPSelectionPanel.getInstance().getCheckedBP());
		if (reload) {
			reloadTrace();
		}
	}

	public void reloadTrace() {
		if (parent instanceof MainFrame) {
			String tracePath = getTracePath();
			if (StringUtils.isNotBlank(tracePath)) {
				parent.updateTracePath(new File(tracePath));
			} else if (StringUtils.isBlank(tracePath) && StringUtils.isNotBlank(parent.getTracePath())) {
				parent.updateTracePath(new File(parent.getTracePath()));
			}
		}
	}

	@Override
	public void dispose() {
		callerItem.setEnabled(true);
		super.dispose();
	}

	private JPanel getHelpPanel() {
		JPanel p = new JPanel();
		p.setPreferredSize(new Dimension(125, 175));
		p.add(getHelpLabel(), BorderLayout.EAST);
		return p;
	}

	private JPanel getEmptyPanel() {
		JPanel p = new JPanel();
		p.setPreferredSize(new Dimension(125, 175));
		return p;
	}

	private JLabel getHelpLabel() {
		String resourceName = ResourceBundleHelper.getImageString("ImageBasePath")
				+ ResourceBundleHelper.getImageString("Image.bpHelpDark");
		ImageIcon imgIcon = new ImageIcon(getClass().getResource(resourceName));
		helpLabel = new JLabel(imgIcon, SwingConstants.RIGHT);
		helpLabel.setPreferredSize(new Dimension(125, 20));
		helpLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new HelpDialog(PreferencesDialog.this, "Preferences");
			}
		});
		return helpLabel;
	}

	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public JButton getReloadButton() {
		return reloadButton;
	}

	public JButton getSaveButton() {
		return saveButton;
	}

	public void setPopup(Popup popup) {
		this.popup = popup;
	}

	public Popup getPopup() {
		return popup;
	}
}
