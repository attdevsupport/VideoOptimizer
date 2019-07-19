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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoAnalysisConfigHelper;
import com.att.aro.core.videoanalysis.impl.RegexMatchLbl;
import com.att.aro.core.videoanalysis.impl.VideoAnalysisConfigHelperImpl;
import com.att.aro.core.videoanalysis.pojo.RegexMatchResult;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;
import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.NumericInputVerifier;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class RegexWizard extends JDialog implements ActionListener, FocusListener, ComponentListener {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LogManager.getLogger(RegexWizard.class);	
	private static boolean errorOccured = false;
	private Map<RegexMatchLbl, VideoDataTags[]> prevXrefMap;
	@Autowired
	private VideoAnalysisConfigHelperImpl voConfigHelper = (VideoAnalysisConfigHelperImpl) ContextAware.getAROConfigContext().getBean(IVideoAnalysisConfigHelper.class);
	private VideoAnalysisConfig videoConfig = new VideoAnalysisConfig();
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	private VideoAnalysisConfig savedVoConfig;

	private JPanel fieldPanel;

	private JTextField compileResultsField;
	private JTextArea requestField;
	private JTextArea regexRequestField;
	private JTextArea headerField;
	private JTextArea regexHeaderField;
	private JTextArea responseField;
	private JTextArea regexResponseField;
	private JScrollPane headerScrollPane;
	private JScrollPane requestScrollPane;
	private JScrollPane responseScrollPane;
	private JScrollPane reqRegexScrollPane;
	private JScrollPane resRegexScrollPane;
	private JScrollPane headerRegexScrollPane;
	
	private JCheckBox cbRequest;
	private JCheckBox cbHeader;
	private JCheckBox cbResponse;

	private JLabel resultLbl;
	private JPanel resultPanel;
	private JPanel matcherPanel;
	private JPanel bottomPanel;
	private JTextField configField;
	private JButton loadBtn;
	private JButton cancelBtn;
	private JButton saveBtn;
	private JButton enterBtn;

	private JRadioButton ignore;
	private JRadioButton match;

	private JCheckBox keep;
	private JCheckBox alpha;
	private JCheckBox characters;
	private JTextField charField;
	private JCheckBox numeric;
	private JCheckBox lengthMin;
	private JCheckBox lengthMax;
	private JTextField lengthMinField;
	private JTextField lengthMaxField;

	private String requestHighlightedText;
	private String headerHighlightedText;
	private String responseHighlightedText;
	private String pattern;

	private boolean headerFocusON;
	private boolean requestFocusON;
	private boolean responseFocusON;

	private ResultVideoTagTable resultsTable;

	private Color darkGreen = new Color(0,127,0);

	private int[] positionArray;
	
	private TableCellEditor cellEditor;

	public static RegexWizard regexWizard = new RegexWizard();
	
	public static RegexWizard getInstance(){
		regexWizard.clear();
		regexWizard.init();
		return regexWizard;
	}
	
	private RegexWizard() {
		getFieldPanel();
		getMatcherPanel();
		getResultPanel();
		getBottomPanel();
	}

	public void init() {
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendViews("VideoParserWizard");
		setDefaultCloseOperation(RegexWizard.DISPOSE_ON_CLOSE);
		enableCheckBoxes(true);		
		Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
		double screenHeight = screenDimension.getHeight();
		double screenWidth = screenDimension.getWidth();
		int height = Util.isLinuxOS() ? (int) (screenHeight * 0.9): (int) (screenHeight * 0.8);
		int width = (int) (screenWidth * 0.75);
		this.setPreferredSize(new Dimension(width, height));		
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setTitle(resourceBundle.getString("videoParser.wizard.title"));
		this.setLocationRelativeTo(getOwner());
		int xPt = ((int) screenWidth - width)/2;
		int yPt = ((int) screenHeight - height)/2;
		this.setLocation(xPt, yPt);
		this.setResizable(false);

		setLayout(new GridBagLayout());
		GridBagConstraints constraint = new GridBagConstraints();

		constraint.fill = GridBagConstraints.BOTH;
		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.weightx = 1;
		constraint.weighty = 0.8;
		constraint.gridwidth = 2;
		add(getFieldPanel(), constraint);

		constraint.gridy = 1;
		constraint.gridwidth = 1;
		constraint.weighty = 0.2;
		constraint.weightx = 0.6;
		constraint.insets = new Insets(0, 2, 0, 5);
		add(getMatcherPanel(), constraint);

		constraint.weightx = 0.4;
		constraint.gridx = 1;
		constraint.insets = new Insets(0, 1, 0, 2);
		add(getResultPanel(), constraint);

		constraint.weightx = 1;
		constraint.gridy = 2;
		constraint.gridx = 0;
		constraint.weighty = 0.0;
		constraint.gridwidth = 2;
		add(getBottomPanel(), constraint);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				clear();
			}
		});
		pack();
	}

	public int[] getPositionArray() {
		return positionArray;
	}

	public void setRequest(HttpRequestResponseInfo request) {
			populateURLFields(request.getObjUri().toString(), request.getAllHeaders(), request.getAssocReqResp().getAllHeaders());
			videoConfig = voConfigHelper.findConfig(request.getObjUri().toString());
			if (videoConfig != null) {
			Map<RegexMatchLbl, VideoDataTags[]> map = new LinkedHashMap<>();
			if(videoConfig.getXrefMap() != null){
				map = new LinkedHashMap<>(videoConfig.getXrefMap());
			}
			savedVoConfig = new VideoAnalysisConfig(videoConfig.getVideoType(), videoConfig.getDesc(),
					videoConfig.getType(), new String(videoConfig.getRegex()), new String(videoConfig.getHeaderRegex()),
					new String(videoConfig.getResponseRegex()), videoConfig.getXref(), map);
			configField.setText(videoConfig.getDesc());

				regexRequestField.setText(videoConfig.getRegex());
				regexResponseField.setText(videoConfig.getResponseRegex());
				regexHeaderField.setText(videoConfig.getHeaderRegex());
				Map<RegexMatchLbl, RegexMatchResult> result = extractResult(videoConfig);
				displayResult(result);
				enableCheckBoxes(false);
			} else {
				videoConfig = new VideoAnalysisConfig();
			}
			pack();
	}
	
	private void enableCheckBoxes(boolean state){
		cbRequest.setSelected(state);
		cbHeader.setSelected(state);
		cbResponse.setSelected(state);
	}

	public void clear() {
		// clear entries (all fields & videoConfig)
		compileResultsField.setText("");
		
		requestField.setText("");
		regexRequestField.setText("");
		responseField.setText("");
		regexResponseField.setText("");
		headerField.setText("");
		regexHeaderField.setText("");

		ignore.setSelected(false);
		match.setSelected(false);
		alpha.setSelected(false);
		characters.setSelected(false);
		charField.setText("");
		numeric.setSelected(false);
		lengthMax.setSelected(false);
		lengthMin.setSelected(false);
		lengthMinField.setText("0");
		lengthMaxField.setText("0");
		configField.setText("");

		lengthMinField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				updateValidations();
			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		
		lengthMaxField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
			}

			@Override
			public void focusGained(FocusEvent e) {
				updateValidations();
			}
		});
		
		videoConfig = new VideoAnalysisConfig();
		displayResult(null);
	}

	private void updateValidations() {
		Integer min = parseInt(lengthMinField, 0);
		Integer max = parseInt(lengthMaxField, min);
		if (max < min) {
			max = min;
			lengthMaxField.setText(max.toString());
		}
		setValidation(lengthMaxField, 500, min, 0);
	}
	
	private void populateURLFields(String request, String header, String response) {
		if (fieldPanel != null) {
			clear();
			requestField.setText(request);
			headerField.setText(header);
			responseField.setText(response);

			requestField.setCaretPosition(0);
			headerField.setCaretPosition(0);
			responseField.setCaretPosition(0);
		}
	}
	
	private JScrollPane createScrollPaneForTextArea(JTextArea textArea, String name, String title) {
		textArea.setName(name);
		textArea.setLineWrap(true);
		textArea.addFocusListener(this);

		JScrollPane scrollPane = new JScrollPane(textArea);
		TitledBorder ttlBorder = BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createRaisedBevelBorder(), BorderFactory.createLoweredBevelBorder()), title);
		ttlBorder.setTitleColor(Color.BLUE);
		ttlBorder.setTitleFont(ttlBorder.getTitleFont().deriveFont(Font.BOLD));
		scrollPane.setBorder(ttlBorder);

		return scrollPane;
	}
	
	private JPanel getFieldPanel() {
		if (fieldPanel == null) {
				        
			fieldPanel = new JPanel();
			fieldPanel.addComponentListener(this);
			fieldPanel.setLayout(new GridBagLayout());
			
			GridBagConstraints constraint = new GridBagConstraints();
			constraint.gridx = 0;
			constraint.gridy = 0;
			constraint.insets = new Insets(0, 5, 0, 5);
			constraint.anchor = GridBagConstraints.FIRST_LINE_START;
			constraint.weightx = 1;
			constraint.fill=GridBagConstraints.HORIZONTAL;
			
			cbRequest = new JCheckBox(ResourceBundleHelper.getMessageString("videotab.label.checkbox"), true);
			cbRequest.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					requestField.setEditable(cbRequest.isSelected());
				}
			});
			cbHeader = new JCheckBox(ResourceBundleHelper.getMessageString("videotab.label.checkbox"), true);
			cbHeader.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					headerField.setEditable(cbHeader.isSelected());
				}
			});
			cbResponse = new JCheckBox(ResourceBundleHelper.getMessageString("videotab.label.checkbox"), true);
			cbResponse.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					responseField.setEditable(cbResponse.isSelected());
				}
			});
			
			compileResultsField = new JTextField();
			compileResultsField.setEditable(false);
			compileResultsField.setBackground(fieldPanel.getBackground());
			compileResultsField.setForeground(Color.red);
			compileResultsField.setFont(compileResultsField.getFont().deriveFont(Font.BOLD));
			fieldPanel.add(compileResultsField, constraint);
			
			requestField = new JTextArea();
			regexRequestField = new JTextArea();
			responseField = new JTextArea();
			regexResponseField = new JTextArea();
			headerField = new JTextArea();
			regexHeaderField = new JTextArea();

			requestScrollPane = createScrollPaneForTextArea(requestField,
					ResourceBundleHelper.getMessageString("videotab.field.request"),
					ResourceBundleHelper.getMessageString("videotab.label.request"));
			headerScrollPane = createScrollPaneForTextArea(headerField,
					ResourceBundleHelper.getMessageString("videotab.field.header"),
					ResourceBundleHelper.getMessageString("videotab.label.header"));
			responseScrollPane = createScrollPaneForTextArea(responseField,
					ResourceBundleHelper.getMessageString("videotab.field.response"),
					ResourceBundleHelper.getMessageString("videotab.label.response"));
			reqRegexScrollPane = createScrollPaneForTextArea(regexRequestField,
					ResourceBundleHelper.getMessageString("videotab.field.regex.request"),
					ResourceBundleHelper.getMessageString("videotab.label.regex.request"));
			resRegexScrollPane = createScrollPaneForTextArea(regexResponseField,
					ResourceBundleHelper.getMessageString("videotab.field.regex.response"),
					ResourceBundleHelper.getMessageString("videotab.label.regex.response"));
			headerRegexScrollPane = createScrollPaneForTextArea(regexHeaderField,
					ResourceBundleHelper.getMessageString("videotab.field.regex.header"),
					ResourceBundleHelper.getMessageString("videotab.label.regex.header"));
			
			constraint.gridy = 1;
			constraint.weighty = 0.0;
			constraint.anchor = GridBagConstraints.WEST;
			constraint.fill=GridBagConstraints.NONE;
			constraint.anchor = GridBagConstraints.EAST;
			fieldPanel.add(cbRequest, constraint);
			
			constraint.gridy = 2;
			constraint.anchor = GridBagConstraints.WEST;
			constraint.fill = GridBagConstraints.BOTH;
			constraint.weighty = Util.isLinuxOS() ? 0.0 : 0.3;
			fieldPanel.add(requestScrollPane, constraint);

			constraint.gridy = 3;
			constraint.weighty = 0.0;
			constraint.fill=GridBagConstraints.HORIZONTAL;
			constraint.anchor = GridBagConstraints.WEST;
			fieldPanel.add(reqRegexScrollPane, constraint);
			
			constraint.gridy = 4;
			constraint.fill=GridBagConstraints.NONE;
			constraint.anchor = GridBagConstraints.WEST;
			constraint.anchor = GridBagConstraints.EAST;
			fieldPanel.add(cbHeader, constraint);
			constraint.gridy = 5;
			constraint.fill = GridBagConstraints.BOTH;
			constraint.weighty = Util.isLinuxOS() ? 0.0 : 0.3;
			fieldPanel.add(headerScrollPane, constraint);
			constraint.gridy = 6;
			constraint.weighty = 0.0;
			fieldPanel.add(headerRegexScrollPane, constraint);

			constraint.gridy = 7;
			constraint.fill=GridBagConstraints.NONE;
			constraint.anchor = GridBagConstraints.WEST;
			constraint.anchor = GridBagConstraints.EAST;
			fieldPanel.add(cbResponse, constraint);

			constraint.gridy = 8;
			constraint.anchor = GridBagConstraints.WEST;
			constraint.fill = GridBagConstraints.BOTH;
			constraint.weighty = Util.isLinuxOS() ? 0.0 : 0.3;
			fieldPanel.add(responseScrollPane, constraint);
			
			constraint.gridy = 9;
			constraint.weighty = 0.0;
			constraint.fill = GridBagConstraints.HORIZONTAL;
			fieldPanel.add(resRegexScrollPane, constraint);
		}
		return fieldPanel;
	}

	private JPanel getMatcherPanel() {
		if (matcherPanel == null) {
			matcherPanel = new JPanel();
			matcherPanel.setLayout(new GridBagLayout());
			Color bgColor = matcherPanel.getBackground();
			matcherPanel.setBorder(new RoundedBorderPanel(bgColor));

			GridBagConstraints constraint = new GridBagConstraints();
			constraint.gridx = 0;
			constraint.gridy = 0;
			constraint.insets = new Insets(0, 0, 0, 0);
			constraint.anchor = GridBagConstraints.FIRST_LINE_START;
			constraint.weightx = 0.5;

			ignore = new JRadioButton(ResourceBundleHelper.getMessageString("videoTab.ignore"));
			match = new JRadioButton(ResourceBundleHelper.getMessageString("videoTab.match"));
			
			ButtonGroup groupBtn = new ButtonGroup();
			groupBtn.add(ignore);
			groupBtn.add(match);

			keep = new JCheckBox(ResourceBundleHelper.getMessageString("videoTab.keep"));
			alpha = new JCheckBox(ResourceBundleHelper.getMessageString("videoTab.alpha"));
			characters = new JCheckBox(ResourceBundleHelper.getMessageString("videoTab.characters"));
			charField = new JTextField(20);

			numeric = new JCheckBox(ResourceBundleHelper.getMessageString("videoTab.numeric"));
			lengthMin = new JCheckBox(ResourceBundleHelper.getMessageString("videoTab.min.length"));
			lengthMax = new JCheckBox(ResourceBundleHelper.getMessageString("videoTab.max.length"));
			lengthMinField = new JTextField(10);
			lengthMaxField = new JTextField(10);
			setValidation(lengthMinField, 500, 0, 0);
			setValidation(lengthMaxField, 500, 0, 0);
			
			matcherPanel.add(ignore, constraint);

			constraint.gridy = 1;
			constraint.anchor = GridBagConstraints.WEST;
			matcherPanel.add(match, constraint);

			constraint.anchor = GridBagConstraints.EAST;

			enterBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.enter"));
			enterBtn.setName(ResourceBundleHelper.getMessageString("videoTab.enter"));
			enterBtn.addActionListener(this);
			matcherPanel.add(enterBtn, constraint);

			constraint.anchor = GridBagConstraints.WEST;
			constraint.gridy = 2;
			matcherPanel.add(keep, constraint);

			constraint.gridy = 3;
			matcherPanel.add(alpha, constraint);

			constraint.gridy = 4;
			JPanel panelChar = new JPanel(new FlowLayout());
			panelChar.add(characters);
			panelChar.add(charField);
			matcherPanel.add(panelChar, constraint);

			constraint.gridy = 5;
			matcherPanel.add(numeric, constraint);

			constraint.gridy = 6;
			JPanel panelNumericLength = new JPanel();
			panelNumericLength.setLayout(new BoxLayout(panelNumericLength, BoxLayout.Y_AXIS));
			JPanel panelMinLength = new JPanel(new FlowLayout());
			panelMinLength.add(lengthMin);
			panelMinLength.add(lengthMinField);
			panelNumericLength.add(panelMinLength);
			JPanel panelMaxLength = new JPanel(new FlowLayout());
			panelMaxLength.add(lengthMax);
			panelMaxLength.add(lengthMaxField);
			panelNumericLength.add(panelMaxLength);
			matcherPanel.add(panelNumericLength, constraint);

			constraint.weighty = 1;

			matcherPanel.add(new JPanel(), constraint);
		}
		return matcherPanel;
	}

	private void setValidation(JTextField numericTextField, double max, double min, int significands) {
		numericTextField.setInputVerifier(new NumericInputVerifier(max, min, significands));
	}

	private JPanel getResultPanel() {
		if (resultPanel == null) {
			resultPanel = new JPanel();
			resultPanel.setLayout(new GridBagLayout());
			Color bgColor = resultPanel.getBackground();
			resultPanel.setBorder(new RoundedBorderPanel(bgColor));

			resultLbl = new JLabel(ResourceBundleHelper.getMessageString("videoTab.result"));
			resultPanel.add(resultLbl, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 5), 0, 0));

			resultsTable = getDataTable();
			JScrollPane scrollableTableArea = new JScrollPane(resultsTable);
			Dimension dim = matcherPanel.getPreferredSize();
			scrollableTableArea.setPreferredSize(new Dimension(dim.width, dim.height - (resultLbl.getPreferredSize().height * 2)));
			scrollableTableArea.setMinimumSize(new Dimension(382, 185));
			resultPanel.add(scrollableTableArea,
					new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 5), 0, 0));
		}

		return resultPanel;
	}

	private ResultVideoTagTable getDataTable() {
		ResultVideoTagTable table = new ResultVideoTagTable();
		configVDTagsComboBox(table, 1);
		table.setOpaque(true);
		return table;
	}

	private void configVDTagsComboBox(JTable table, int columnIndex) {

		TableColumnModel columnModel = table.getColumnModel();
		TableColumn comboColumn = columnModel.getColumn(columnIndex);

		EnumSet<VideoDataTags> allVDTags = EnumSet.allOf(VideoDataTags.class);
		comboColumn.setCellEditor(new ComboBoxCellEditor(allVDTags));

		/*
		 * allows clearing a problem when cell editor is interrupted, very deep problem. 
		 * Only shows if: (A) combobox selection is interupted, (B) dialog is
		 * closed , and (C) Wizard is entered from the menu in this exact order
		 */
		cellEditor = comboColumn.getCellEditor();
		ComboBoxCellRenderer renderer = new ComboBoxCellRenderer();
		renderer.setToolTipText(ResourceBundleHelper.getMessageString("videoTab.tooltip"));
		comboColumn.setCellRenderer(renderer);
	}
	
	private class ComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

		private String xrefValue;
		private EnumSet<VideoDataTags> allVideoTags;

		public ComboBoxCellEditor(EnumSet<VideoDataTags> allVideoTags) {
			this.allVideoTags = allVideoTags;
		}

		@Override
		public Object getCellEditorValue() {
			return this.xrefValue;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			JComboBox<String> comboBox = new JComboBox<>();

			for (VideoDataTags videoDataTag : allVideoTags) {
				comboBox.addItem(videoDataTag.toString());
			}
			this.xrefValue = value.toString();
			comboBox.setSelectedItem(value.toString());
			comboBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent evt) {
					JComboBox<String> combo = (JComboBox<String>) evt.getSource();
					xrefValue = (String) combo.getSelectedItem();
					fireEditingStopped();
				}
			});

			comboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					fireEditingCanceled();
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {

				}
			});

			comboBox.setBackground(Color.WHITE);
			return comboBox;
		}

		@Override
		public boolean stopCellEditing() {
			return super.stopCellEditing();
		}
	}
	
	private class ComboBoxCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setBackground(Color.WHITE);
			setForeground(Color.BLACK);
			return this;
		}
	}

	private JPanel getBottomPanel() {
		if (bottomPanel == null) {
			bottomPanel = new JPanel(new FlowLayout());
			JLabel configName = new JLabel(ResourceBundleHelper.getMessageString("videoTab.configuration.name"));
			configField = new JTextField(45);
			loadBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.load"));
			cancelBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.close"));
			saveBtn = new JButton(ResourceBundleHelper.getMessageString("videoTab.save"));

			loadBtn.setName(ResourceBundleHelper.getMessageString("videoTab.load"));
			loadBtn.addActionListener(this);

			saveBtn.setName(ResourceBundleHelper.getMessageString("videoTab.save"));
			saveBtn.addActionListener(this);

			cancelBtn.setName(ResourceBundleHelper.getMessageString("videoTab.close"));
			cancelBtn.addActionListener(this);

			bottomPanel.add(configName);
			bottomPanel.add(configField);
			bottomPanel.add(loadBtn);
			bottomPanel.add(cancelBtn);
			bottomPanel.add(saveBtn);
		}
		return bottomPanel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton btn = (JButton) e.getSource();
		if (btn.getName().equals(ResourceBundleHelper.getMessageString("videoTab.enter"))) {
			doEnter();
		} else if (btn.getName().equals(ResourceBundleHelper.getMessageString("videoTab.load"))) {
			doLoad();
		} else if (btn.getName().equals(ResourceBundleHelper.getMessageString("videoTab.close"))) {
			doClose();
		} else if (btn.getName().equals(ResourceBundleHelper.getMessageString("videoTab.save"))) { // Saving after testing the regex by clicking on enter
			saveConfig();
		}
	}

	private void doClose() {
		signalStopCellEditing();
		this.dispose();
		this.setVisible(false);
	}

	private void doLoad() {
		if (!validateFields()) {
			return;
		}
		JFileChooser fileChooser = new JFileChooser(voConfigHelper.getFolderPath());
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (file.exists()) {
				configField.setText(file.getPath());
				compileResultsField.setText("");
				VideoAnalysisConfig voConfig = voConfigHelper.loadConfigFile(file.getAbsolutePath());
				if (voConfig != null) {
					signalStopCellEditing();
					this.videoConfig = voConfig;
					Map<RegexMatchLbl, VideoDataTags[]> map = new LinkedHashMap<>();
					if(videoConfig.getXrefMap() != null){
						map = new LinkedHashMap<>(videoConfig.getXrefMap());
					}
					savedVoConfig = new VideoAnalysisConfig(videoConfig.getVideoType(), videoConfig.getDesc(),
							videoConfig.getType(), new String(videoConfig.getRegex()),
							new String(videoConfig.getHeaderRegex()), new String(videoConfig.getResponseRegex()),
							videoConfig.getXref(), map);
					regexRequestField.setText(voConfig.getRegex());
					regexResponseField.setText(voConfig.getResponseRegex());
					regexHeaderField.setText(voConfig.getHeaderRegex());
					Map<RegexMatchLbl, RegexMatchResult> result = extractResult(voConfig);
					displayResult(result);
				} else {
					JOptionPane.showMessageDialog(this, "Failed to load the configuration file.", "Failure", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	private void doEnter() {
		if (!validateFields()) {
			return;
		}
		if (errorOccured) {
			errorOccured = false;
		} else {
			prevXrefMap = resultsTable.getVideoDataTagsMap();
		}
		if (requestFocusON) {
			pattern = generateRegexPattern(getRequestHighlightedText());
			regexRequestField.setText(regexRequestField.getText() + pattern);

			updateConfigAndTags();
			videoConfig.setRegex(regexRequestField.getText());

		} else if (headerFocusON) {
			pattern = generateRegexPattern(getHeaderHighlightedText());
			regexHeaderField.setText(regexHeaderField.getText() + pattern);

			updateConfigAndTags();
			videoConfig.setHeaderRegex(regexHeaderField.getText());
		} else if (responseFocusON) {
			pattern = generateRegexPattern(getResponseHighlightedText());
			regexResponseField.setText(regexResponseField.getText() + pattern);

			updateConfigAndTags();
			videoConfig.setResponseRegex(regexResponseField.getText());

		} else {
			updateConfigAndTags();
			videoConfig.setRegex(regexRequestField.getText());
			videoConfig.setHeaderRegex(regexHeaderField.getText());
			videoConfig.setResponseRegex(regexResponseField.getText());
			videoConfig.setType("GET");
		}

		requestFocusON = false;
		headerFocusON = false;
		responseFocusON = false;
		Map<RegexMatchLbl, RegexMatchResult> result = extractResult(videoConfig);
		displayResult(result);
		if(errorOccured){
			videoConfig = new VideoAnalysisConfig(savedVoConfig.getVideoType(), savedVoConfig.getDesc(), savedVoConfig.getType(), 
					savedVoConfig.getRegex(), savedVoConfig.getHeaderRegex(), savedVoConfig.getResponseRegex(), savedVoConfig.getXref(), savedVoConfig.getXrefMap());
			prevXrefMap = new HashMap<>(videoConfig.getXrefMap());
		}
	}

	private boolean validateFields() {
		return (lengthMaxField.getInputVerifier().verify(lengthMaxField)
				&& lengthMinField.getInputVerifier().verify(lengthMinField));
	}

	/*<pre>
	 * issues a stopCellEditing to resolve problems from interrupted combobox selections.
	 *  allows clearing a problem when cell editor is interrupted, very deep problem. 
	 *  Only shows if: 
	 *   (A) combobox selection is interrupted
	 *   (B) dialog is closed
	 *   (C) Wizard is entered from the menu in this exact order
	 */
	private void signalStopCellEditing() {
		if (cellEditor != null) {
			cellEditor.stopCellEditing();
		}
	}

	private void saveConfig() {
		if (this.videoConfig != null) {
			if (!validateFields()) {
				return;
			}
			requestFocusON = false;
			headerFocusON = false;
			responseFocusON = false;
			doEnter();
			VideoDataTags[] xref = resultsTable.getVideoDataTags();
			Map<RegexMatchLbl, VideoDataTags[]> xrefMap = resultsTable.getVideoDataTagsMap();
			if (xref.length > 0) {
				this.videoConfig.setXref(xref);
				this.videoConfig.setXrefMap(xrefMap);
			}
		}

		if (this.videoConfig != null && voConfigHelper.validateConfig(videoConfig)) {
			try {
				JFileChooser fileChooser = new JFileChooser(voConfigHelper.getFolderPath());
				if (videoConfig.getDesc() == null || videoConfig.getDesc().isEmpty()) {
					videoConfig.setDesc("ConfigFile");
				}
				fileChooser.setSelectedFile(new File(videoConfig.getDesc()));
				
				if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					voConfigHelper.saveConfigFile(
							videoConfig.getVideoType()
							, fileChooser.getSelectedFile().getName().replaceAll("\\.json", "")
							, videoConfig.getType()
							, videoConfig.getRegex()
							, videoConfig.getHeaderRegex()
							, videoConfig.getResponseRegex()
							, videoConfig.getXref());
					videoConfig.setDesc(fileChooser.getSelectedFile().getName().replaceAll("\\.json", ""));
					configField.setText(videoConfig.getDesc());
					Map<RegexMatchLbl, VideoDataTags[]> map = new LinkedHashMap<>(videoConfig.getXrefMap());
					savedVoConfig = new VideoAnalysisConfig(videoConfig.getVideoType(), videoConfig.getDesc(),
							videoConfig.getType(), new String(videoConfig.getRegex()),
							new String(videoConfig.getHeaderRegex()), new String(videoConfig.getResponseRegex()),
							videoConfig.getXref(), map);
				}
			} catch (JsonGenerationException e1) {
				LOG.error("VideoAnalysisConfig failed Jason generation :" + e1.getMessage());
			} catch (JsonMappingException e1) {
				LOG.error("VideoAnalysisConfig failed to de-serialize :" + e1.getMessage());
			} catch (IOException e1) {
				LOG.error("VideoAnalysisConfig failed to save :" + e1.getMessage());
			} catch (Exception e1) {
				LOG.error("VideoAnalysisConfig failed to load :" + e1.getMessage());
			}

		} else {
			LOG.error("VideoAnalysisConfig is invalid: capture groups not equal to cross references");
			JOptionPane.showMessageDialog(this,
					String.format("%s config is invalid: capture groups not equal to cross references", videoConfig != null ? videoConfig.getDesc() : "unknown")
					, "Failure"
					, JOptionPane.ERROR_MESSAGE);
		}

	}

	private VideoDataTags[] copyXrefTags(VideoDataTags[] reqXref, VideoDataTags[] headerXref,
			VideoDataTags[] responseXref, int size) {
		VideoDataTags[] xrefTot = new VideoDataTags[size];
		int idx = 0;
		if (reqXref != null) {
			for (VideoDataTags tg : reqXref) {
				xrefTot[idx] = tg;
				idx++;
			}
		}
		if (headerXref != null) {
			for (VideoDataTags tg : headerXref) {
				xrefTot[idx] = tg;
				idx++;
			}
		}
		if (responseXref != null) {
			for (VideoDataTags tg : responseXref) {
				xrefTot[idx] = tg;
				idx++;
			}
		}
		return xrefTot;
	}
	
	/**
	 * Manages adjustments to xref to match CaptureGroups
	 */
	private void updateConfigAndTags() {

		if (videoConfig == null || videoConfig.getXrefMap() == null) {
			return;
		}

		voConfigHelper.setSavedVoConfig(savedVoConfig);
		if(prevXrefMap == null){
			prevXrefMap = resultsTable.getVideoDataTagsMap();
		}
		
		VideoDataTags[] reqXref = voConfigHelper.findXref(videoConfig, videoConfig.getRegex(),
				regexRequestField.getText(), requestField.getText(), RegexMatchLbl.REQUEST,
				prevXrefMap);
		VideoDataTags[] headerXref = voConfigHelper.findXref(videoConfig, videoConfig.getHeaderRegex(),
				regexHeaderField.getText(), headerField.getText(), RegexMatchLbl.HEADER,
				prevXrefMap);
		VideoDataTags[] responseXref = voConfigHelper.findXref(videoConfig, videoConfig.getResponseRegex(),
				regexResponseField.getText(), responseField.getText(), RegexMatchLbl.RESPONSE,
				prevXrefMap);
		int size = 0;

		if (reqXref != null && reqXref.length != 0) {
			size = size + reqXref.length;
			videoConfig.getXrefMap().put(RegexMatchLbl.REQUEST, reqXref);
		} else {
			videoConfig.getXrefMap().remove(RegexMatchLbl.REQUEST);
		}

		if (headerXref != null && headerXref.length != 0) {
			size = size + headerXref.length;
			videoConfig.getXrefMap().put(RegexMatchLbl.HEADER, headerXref);
		} else {
			videoConfig.getXrefMap().remove(RegexMatchLbl.HEADER);
		}

		if (responseXref != null && responseXref.length != 0) {
			size = size + responseXref.length;
			videoConfig.getXrefMap().put(RegexMatchLbl.RESPONSE, responseXref);
		} else {
			videoConfig.getXrefMap().remove(RegexMatchLbl.RESPONSE);
		}

		VideoDataTags[] xrefTgs = copyXrefTags(reqXref, headerXref, responseXref, size);
		if (xrefTgs != null) {
			videoConfig.setXref(xrefTgs);
			resultsTable.update(new LinkedHashMap<>(), videoConfig.getXrefMap());
		}

		StringBuilder sbError = new StringBuilder();
		String result = videoConfig.setRegex(regexRequestField.getText());
		if (!result.isEmpty()) {
			sbError.append("Request :");
			sbError.append(result);
		}

		result = videoConfig.setHeaderRegex(regexHeaderField.getText());
		if (!result.isEmpty()) {
			if (sbError.length() > 0) {
				sbError.append("  ,");
			}
			sbError.append("Header :");
			sbError.append(result);
		}

		result = videoConfig.setResponseRegex(regexResponseField.getText());
		if (!result.isEmpty()) {
			if (sbError.length() > 0) {
				sbError.append("  ,");
			}
			sbError.append("Response :");
			sbError.append(result);
		}

		if (videoConfig.getVideoType() != null) {
			videoConfig.setVideoType(videoConfig.getVideoType());
		} else {
			if (!requestField.getText().isEmpty()) {
				if (requestField.getText().contains(".ism")) {
					videoConfig.setVideoType(VideoType.SSM);
				} else if (requestField.getText().contains("dtvn-live") || requestField.getText().contains(".hls")
						|| requestField.getText().contains("directvlst")
						|| requestField.getText().contains("directvaav")
						|| requestField.getText().contains(".directv")) {
					videoConfig.setVideoType(VideoType.HLS);
				} else if (requestField.getText().contains("_video_")) {
					videoConfig.setVideoType(VideoType.DASH);
				} else {
					videoConfig.setVideoType(VideoType.UNKNOWN);
				}
			}
		}

		Map<RegexMatchLbl, RegexMatchResult> resMap = voConfigHelper.match(videoConfig, requestField.getText(),
				headerField.getText(), responseField.getText());
		int len = resMap.values().stream().mapToInt(i -> i.getResult().length).sum();
		String[] res = new String[len];
		int index = 0;
		for (RegexMatchResult matchRes : resMap.values()) {
			for (String str : matchRes.getResult()) {
				res[index] = str;
				index++;
			}
		}
		if (sbError.length() > 0) {
			errorOccured = true;
			compileResultsField.setForeground(Color.red);
			compileResultsField.setText(String.format("ERRORS: %s" ,sbError.toString()));
			displayResult(null);
		} else {
			compileResultsField.setForeground(darkGreen);
			compileResultsField.setText(String.format("Success: %d capture groups", res != null ? res.length : 0));
		}
		compileResultsField.setCaretPosition(0);

	}

	/**
	 * Shift tags in the videoDataTags array.
	 * 
	 * @param position
	 *            to start inserting or removing
	 * @param delta
	 * @param tag
	 * @param videoDataTags
	 * @return new array of VideoDataTags
	 */
	public VideoDataTags[] shift(int position, int delta, VideoDataTags tag, VideoDataTags[] videoDataTags) {
		boolean insert = true;
		if (delta < 0) {
			insert = false;
		}
		if (videoDataTags == null || (videoDataTags.length + delta) < 0) {
			return null;
		}
		VideoDataTags[] newTags = new VideoDataTags[videoDataTags.length + delta];
		Arrays.fill(newTags, VideoDataTags.unknown);
		int pos = 0;
		List<Integer> posList = null;
		if (positionArray != null) {
			posList = Arrays.stream(positionArray).boxed().collect(Collectors.toList());
			if (delta > 0) {
				int[] arr = new int[posList.size()];
				int j = 0;
				for (int i : posList) {
					arr[j] = i % newTags.length;
					j++;
				}
				posList.clear();
				posList = Arrays.stream(arr).boxed().collect(Collectors.toList());
			}
		}
	
		if (newTags.length != 0) {
			int idy = 0;
			for (int idx = 0; idx < videoDataTags.length; idx++) {
				if ((idx == position && delta != 0) || (posList != null && posList.contains(idx) && !insert)) {
					if (delta > 0) {
						// insert new CaptureGroups xrefs
						while (0 < delta--) {
							if (positionArray != null) {
								newTags[positionArray[pos] % newTags.length] = tag;
								pos++;
							} else {
								newTags[idy++] = tag;
							}
						}

						if (posList != null && !(posList.contains(idy))) {
							newTags[idy++] = videoDataTags[idx];
						} else if (posList != null && (posList.contains(idy))) {
							idy++;
							idx--;
						} else {
							newTags[idy++] = videoDataTags[idx];
						}
					}
					position = -1;
				} else {
					if (idy < newTags.length && (posList == null || !posList.contains(idy))) {
						newTags[idy++] = videoDataTags[idx];
					} else if (posList != null && posList.contains(idy) && insert) {
						idy++;
						idx--;
					} else if (posList != null && posList.contains(idy) && (!insert)) { // remove
						newTags[idy++] = videoDataTags[idx];
					}

				}
			}
			if (delta > 0) {
				while (0 < delta--) {
					newTags[idy++] = tag;
				}
			}
		}
		return newTags;
	}

	public VideoDataTags[] shift(int position, int delta, VideoDataTags tag, VideoDataTags[] videoDataTags,
			boolean caller) {
		if (!caller) {
			positionArray = null;
		}
		return shift(position, delta, tag, videoDataTags);
	}

	Pattern pat = Pattern.compile("(\\(.+\\))");

	private Map<RegexMatchLbl, RegexMatchResult> extractResult(VideoAnalysisConfig voConfig) {
		Map<RegexMatchLbl, RegexMatchResult> result = voConfigHelper.match(voConfig, requestField.getText(), headerField.getText(), responseField.getText());
		return result;
	}

	public void displayResult(Map<RegexMatchLbl, RegexMatchResult> result) {
		if (videoConfig != null) {
			resultsTable.update(result, videoConfig.getXrefMap());
		}
	}

	private String generateRegexPattern(String highlightedText) {
		String resultPattern = "";

		if ((match.isSelected() || keep.isSelected()) && (!highlightedText.trim().isEmpty())) {
			resultPattern = matchSelection(highlightedText);
		}

		if (alpha.isSelected()) {
			if (characters.isSelected()) {
				String[] charList = charField.getText().split(",");
				StringBuffer sb = new StringBuffer();
				for (int ch = 0; ch < charList.length; ch++) {
					sb.append("|" + charList[ch].trim());
				}
				if (numeric.isSelected()) {
					sb.append("0-9");
				}

				resultPattern = "[" + resultPattern + sb.toString() + "]";

			} else {
				if (numeric.isSelected()) {
					resultPattern = "[a-zA-Z0-9\\_\\-]*";
				} else {
					resultPattern = "[a-zA-Z\\_\\-]*";
				}
			}
		} else if (numeric.isSelected()) {
			resultPattern = numericSelection();
		}

		if (ignore.isSelected()) {
			resultPattern = ".+";
		}

		if (keep.isSelected() && (!resultPattern.trim().isEmpty())) {
			resultPattern = keepSelection(resultPattern);
		}
		return resultPattern;
	}

	private String numericSelection() {
		StringBuffer numericPattern = new StringBuffer();
		numericPattern.append("\\d");
		if (lengthMin.isSelected() || lengthMax.isSelected()) {
			int min = -1, max = -1;
			
			min = parseInt(lengthMinField, 0);
			max = parseInt(lengthMaxField, min);
			
			if ((min == 1 && max == 1) || (min < 1 && max == 1)) {
				// pattern for only one digit
			} else if (min > 1) {
				numericPattern.append("{" + min);

				if (max > 1 && max > min) {
					numericPattern.append("," + max + "}");
				} else if (max > 1 && max == min) {
					numericPattern.append("}");
				} else {
					numericPattern.append(",}");
				}
			} else if (max > 1) {
				numericPattern.append("{1," + max + "}");
			} else {
				numericPattern.append("+");
			}
		} else {
			numericPattern.append("+");
		}

		return numericPattern.toString();

	}

	private int parseInt(JTextField field, int defaultVal) {
		int val;
		if (field == null || StringUtils.isEmpty(field.getText())) {
			val = defaultVal;
		} else {
			try {
				Double dVal = Double.parseDouble(field.getText());
				val = dVal.intValue();
			} catch (NumberFormatException e) {
				val = defaultVal;
				LOG.error("NumberFormatException {" + field.getText() + "} ", e);
			}
		}
		return val;
	}

	private String matchSelection(String text) {
		return formatSpecialCharacters(text);
	}

	private String keepSelection(String text) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		sb.append(text);
		sb.append(")");
		return sb.toString();
	}

	private String formatSpecialCharacters(String text) {
		boolean hasSpecialChars = !StringUtils.isAlphanumeric(text);
		StringBuffer sb = new StringBuffer();
		if (hasSpecialChars) {
			Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
			for (int i = 0; i < text.length(); i++) {
				Matcher match = pattern.matcher(text.substring(i, i + 1));
				if (match.find()) { // is a special character
					sb.append("\\" + text.charAt(i));
				} else {
					sb.append(text.charAt(i));
				}
			}
			return sb.toString();
		} else {
			return text;
		}
	}

	@Override
	public void focusGained(FocusEvent e) {
	}

	@Override
	public void focusLost(FocusEvent e) {
		JTextArea field = (JTextArea) e.getSource();
		if (field.getName().equals(requestField.getName())) {
			setRequestHighlightedText(field.getSelectedText());
			updateFocus(true, false, false);
		} else if (field.getName().equals(headerField.getName())) {
			setHeaderHighlightedText(field.getSelectedText());
			updateFocus(false, false, true);
		} else if (field.getName().equals(responseField.getName())) {
			setResponseHighlightedText(field.getSelectedText());
			updateFocus(false, true, false);
		} else {
			clearHighlightedTexts();
			if (field.getName().equals(regexRequestField.getName())) {
				updateFocus(true, false, false);
			} else if (field.getName().equals(regexResponseField.getName())) {
				updateFocus(false, true, false);
			} else {
				updateFocus(false, false, true);

			}
		}
	}

	private void updateFocus(boolean request, boolean response, boolean header) {
		requestFocusON = request;
		responseFocusON = response;
		headerFocusON = header;
	}

	private void clearHighlightedTexts() {
		setRequestHighlightedText("");
		setHeaderHighlightedText("");
		setResponseHighlightedText("");
	}

	public String getRequestHighlightedText() {
		if (this.requestHighlightedText != null) {
			String val = this.requestHighlightedText;
			this.requestHighlightedText = "";
			return val;
		} else {
			return "";
		}
	}

	public void setRequestHighlightedText(String highlightedText) {
		this.requestHighlightedText = highlightedText;
	}

	public String getHeaderHighlightedText() {
		if (headerHighlightedText != null) {
			String val = headerHighlightedText;
			headerHighlightedText = "";
			return val;
		} else {
			return "";
		}
	}

	public void setHeaderHighlightedText(String headerHighlightedText) {
		this.headerHighlightedText = headerHighlightedText;
	}

	public String getResponseHighlightedText() {
		if (responseHighlightedText != null) {
			String val = responseHighlightedText;
			responseHighlightedText = "";
			return val;
		} else {
			return "";
		}
	}

	public void setResponseHighlightedText(String responseHighlightedText) {
		this.responseHighlightedText = responseHighlightedText;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		pack();
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
}