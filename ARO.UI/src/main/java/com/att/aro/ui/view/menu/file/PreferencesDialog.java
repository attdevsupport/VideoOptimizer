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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.settings.Settings;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.settings.impl.JvmSettings;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.EnableEscKeyCloseDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
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
	private JButton okButton;
	private JPanel optionsPanel;
	private EnableEscKeyCloseDialog enableEscKeyCloseDialog;
	private final SharedAttributesProcesses parent;
	private final Component callerItem;
	private Map<Config, String> updates = new HashMap<>();
	String logginglevel = "ERROR";
	private Settings settings = SettingsImpl.getInstance();
	private JLabel helpLabel;
	BPVideoWarnFailPanel bpVideoWarnFailPanel;
	private JTabbedPane tabbedPane;
	/**
	 * Type of configuration This is used to provide appropriate method for
	 * displaying configuration
	 */
	enum ConfigType {
		TEXT, NUMBER, FILE, COMBO
	}

	/**
	 * Configuration item for dialog Lists all the configurations that are meant
	 * to be displayed on the preferences dialog
	 */
	enum Config {
		MEM("Xmx", "Max heap in MB", TEXT, JvmSettings.getInstance(), isHeapEnabled()),
		ADB("adb", "Adb Path", FILE, SettingsImpl.getInstance(), true),
		DUMP_CAP("dumpCap", "Dumpcap Path", FILE, SettingsImpl.getInstance(), Util.isMacOS(), null, ()->Util.getDumpCap()),
		IDEVICE_SCREENSHOT("iDeviceScreenshot", "iDeviceScreenshot Path", FILE, SettingsImpl.getInstance(),
				Util.isMacOS(), null, () -> Util.getIdeviceScreenshot()),
		FFMPEG("ffmpeg", "FFMpeg Path", FILE, SettingsImpl.getInstance(), true, null, () -> Util.getFFMPEG()),
		FFPROBE("ffprobe", "FFProbe Path", FILE, SettingsImpl.getInstance(), true, null, () -> Util.getFFPROBE()),
		IOS_PROV("iosProv", "iOS Provisioning Profile", FILE, SettingsImpl.getInstance(), Util.isMacOS()),
		IOS_CERT("iosCert", "iOS Certificate", TEXT, SettingsImpl.getInstance(), Util.isMacOS(),
				ResourceBundleHelper.getMessageString("preferences.iosCert.textField.hint"), null),
		LOG_LVL("logging", "Logging Level", COMBO, SettingsImpl.getInstance(),true,ResourceBundleHelper.getMessageString("preferences.logging.dropdown.values"));

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

		private static boolean isHeapEnabled() {
			boolean isLinuxOrWin32 = Util.isWindows32OS() || Util.isLinuxOS();
			boolean isMoreThan4GB = ((JvmSettings) JvmSettings.getInstance()).getSystemMemory() > 4096;
			return !isLinuxOrWin32 && isMoreThan4GB;
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
		getRootPane().setDefaultButton(okButton);
	}

	private JComponent getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setPreferredSize(new Dimension(750, 500));
			jContentPane.setLayout(new BorderLayout());
			bpVideoWarnFailPanel = new BPVideoWarnFailPanel();
			tabbedPane = new JTabbedPane();
			tabbedPane.addTab(ResourceBundleHelper.getMessageString("preferences.general.tabtile"), getGeneralTab());
			tabbedPane.addTab(ResourceBundleHelper.getMessageString("preferences.bestpractice.tabtile"),
					BPSelectionPanel.getBPPanel());
			tabbedPane.addTab(ResourceBundleHelper.getMessageString("preferences.video.tabtitle"),
					bpVideoWarnFailPanel.getVideoPreferenceTab());
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
			jButtonGrid.add(getButton("Cancel", (ActionEvent arg) -> dispose()));
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
		} else if (config.getConfigType() == ConfigType.COMBO) {
			JComboBox<String> comboBox = new JComboBox<>(getComboValue(config));
			comboBox.addActionListener((ActionEvent e) -> setLoggingLvl((String) comboBox.getSelectedItem()));
			comboBox.setSelectedItem(Util.getLoggingLevel());
			panel.add(comboBox);
		}
		return panel;
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
			value.setText(config.getDefValue());
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

	private void saveAndClose() {
		try {
			saveGenTabValues();
			saveBPSelection();
			bpVideoWarnFailPanel.saveWarnFail();
			SettingsImpl.getInstance().setAndSaveAttribute("LOG_LEVEL", logginglevel);
			Util.setLoggingLevel(logginglevel);
			dispose();
		} catch (IllegalArgumentException iae) {
			LOGGER.error("Failed to save preferences due to failure on video bp panel");
			this.repaint();
		} catch (Exception e) {
			LOGGER.error("Failed to save preferences", e);
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
					"Error occurred while trying to save Preferences", "Error saving preferences",
					JOptionPane.ERROR_MESSAGE);
		}
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
	
	private void saveBPSelection() {
		SettingsUtil.saveBestPractices(BPSelectionPanel.getInstance().getCheckedBP());
		String tracePath="";
		if (parent instanceof MainFrame) {
			String path = getTracePath();
			tracePath = path != null ? path : tracePath;
		}
		if(tracePath != null && !tracePath.equals("")) {
			parent.updateTracePath(new File(tracePath));
		} else if (parent.getTracePath() != null && !"".equals(parent.getTracePath().trim())) {
			parent.updateTracePath(new File(parent.getTracePath()));
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
	
	public JTabbedPane getTabbedPane(){
		return tabbedPane;
	}
}
