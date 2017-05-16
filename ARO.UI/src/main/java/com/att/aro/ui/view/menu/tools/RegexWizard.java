
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ILogger;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.videoanalysis.IVideoAnalysisConfigHelper;
import com.att.aro.core.videoanalysis.impl.VideoAnalysisConfigHelperImpl;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;
import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;

public class RegexWizard extends JDialog implements ActionListener, FocusListener {

	private static final long serialVersionUID = 1L;

	private IAROView parent;

	private ILogger log = ContextAware.getAROConfigContext().getBean(ILogger.class);
	
	@Autowired
	private VideoAnalysisConfigHelperImpl voConfigHelper = (VideoAnalysisConfigHelperImpl) ContextAware.getAROConfigContext().getBean(IVideoAnalysisConfigHelper.class);
	private VideoAnalysisConfig videoConfig = new VideoAnalysisConfig();
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();

	private JPanel fieldPanel;
	private JLabel requestLbl;
	private JLabel regexRequestLbl;
	private JLabel headerLbl;
	private JLabel regexHeaderLbl;
	private JLabel responseLbl;
	private JLabel regexResponseLbl;

	private JTextField errorField;
	private JTextField requestField;
	private JTextField regexRequestField;
	private JTextField headerField;
	private JTextField regexHeaderField;
	private JTextField responseField;
	private JTextField regexResponseField;

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

	public RegexWizard(IAROView parent) {
		this.parent = parent;
		((MainFrame) parent).setRegexWizard(this);
		setDefaultCloseOperation(RegexWizard.DISPOSE_ON_CLOSE);
		this.setSize(800, 700);
		this.setModal(false);
		this.setTitle(resourceBundle.getString("videoParser.wizard.title"));
		this.setLocationRelativeTo(getOwner());
		this.setAlwaysOnTop(true);
		this.setResizable(false);

		setLayout(new GridBagLayout());
		GridBagConstraints constraint = new GridBagConstraints();

		constraint.fill = GridBagConstraints.BOTH;
		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.weightx = 1;
		constraint.weighty = 0.6;
		constraint.gridwidth = 2;
		add(getFieldPanel(), constraint);

		constraint.gridy = 1;
		constraint.gridwidth = 1;
		constraint.weighty = 0.38;
		constraint.weightx = 0.4;
		constraint.insets = new Insets(0, 2, 0, 5);
		add(getMatcherPanel(), constraint);

		constraint.weightx = 0.5;
		constraint.gridx = 1;
		constraint.insets = new Insets(0, 1, 0, 2);
		add(getResultPanel(), constraint);

		constraint.weightx = 1;
		constraint.gridy = 2;
		constraint.gridx = 0;
		constraint.weighty = 0.02;
		constraint.gridwidth = 2;
		add(getBottomPanel(), constraint);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				clear();
			}
		});
		pack();
	}

	public RegexWizard(IAROView parent, HttpRequestResponseInfo request) {
		this(parent);
		
		if (!request.getObjName().contains(".m3u8") && !request.getObjName().contains(".mpd")) {
			populateURLFields(request.getObjUri().toString(), request.getAllHeaders(), request.getAssocReqResp().getAllHeaders());
			videoConfig = voConfigHelper.findConfig(request.getObjUri().toString());
			if (videoConfig != null) {

				configField.setText(videoConfig.getDesc());

				regexRequestField.setText(videoConfig.getRegex());
				regexResponseField.setText(videoConfig.getResponseRegex());
				regexHeaderField.setText(videoConfig.getHeaderRegex());
				String[] result = extractResult(videoConfig);
				displayResult(result);
			} else {
				videoConfig = new VideoAnalysisConfig();
			}
		}
	}

	public void clear() {
		// clear entries (all fields & videoConfig)
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
		lengthMinField.setText("");
		lengthMaxField.setText("");
		// resultField.setText("");
		configField.setText("");

		displayResult(null);

		this.videoConfig = new VideoAnalysisConfig();
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

	private JPanel getFieldPanel() {
		if (fieldPanel == null) {
			fieldPanel = new JPanel();
			fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

			requestLbl = new JLabel("Sample request:");
			regexRequestLbl = new JLabel("Regex pattern for request:");
			headerLbl = new JLabel("Sample header:");
			regexHeaderLbl = new JLabel("Regex pattern for header:");
			responseLbl = new JLabel("Sample response:");
			regexResponseLbl = new JLabel("Regex pattern for response:");

			errorField = new JTextField();
			errorField.setEditable(false);
			errorField.setBackground(fieldPanel.getBackground());
			errorField.setForeground(Color.red);
			errorField.setFont(errorField.getFont().deriveFont(Font.BOLD));

			requestField = new JTextField();
			requestField.setName("RequestField");
			requestField.addFocusListener(this);
			regexRequestField = new JTextField();

			responseField = new JTextField();
			responseField.setName("ResponseField");
			responseField.addFocusListener(this);
			regexResponseField = new JTextField();

			headerField = new JTextField();
			headerField.setName("HeaderField");
			headerField.addFocusListener(this);
			regexHeaderField = new JTextField();

			regexRequestField.setName("RegexRequestField");
			regexRequestField.addFocusListener(this);

			regexHeaderField.setName("RegexHeaderField");
			regexHeaderField.addFocusListener(this);

			regexResponseField.setName("RegexResponseField");
			regexResponseField.addFocusListener(this);

			fieldPanel.add(errorField);

			fieldPanel.add(requestLbl);
			fieldPanel.add(requestField);

			fieldPanel.add(regexRequestLbl);
			fieldPanel.add(regexRequestField);

			fieldPanel.add(headerLbl);
			fieldPanel.add(headerField);

			fieldPanel.add(regexHeaderLbl);
			fieldPanel.add(regexHeaderField);

			fieldPanel.add(responseLbl);
			fieldPanel.add(responseField);

			fieldPanel.add(regexResponseLbl);
			fieldPanel.add(regexResponseField);

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

			ignore = new JRadioButton("Ignore");
			match = new JRadioButton("Match");
			ButtonGroup groupBtn = new ButtonGroup();
			groupBtn.add(ignore);
			groupBtn.add(match);

			keep = new JCheckBox("Keep");
			alpha = new JCheckBox("Alpha");
			characters = new JCheckBox("Characters");
			charField = new JTextField(20);

			numeric = new JCheckBox("Numeric");
			lengthMin = new JCheckBox("Length min");
			lengthMax = new JCheckBox("Length max");
			lengthMinField = new JTextField(10);
			lengthMaxField = new JTextField(10);

			matcherPanel.add(ignore, constraint);

			constraint.gridy = 1;
			constraint.anchor = GridBagConstraints.WEST;
			matcherPanel.add(match, constraint);

			constraint.anchor = GridBagConstraints.EAST;

			enterBtn = new JButton("Enter");
			enterBtn.setName("Enter");
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

	private JPanel getResultPanel() {
		if (resultPanel == null) {
			resultPanel = new JPanel();
			resultPanel.setLayout(new GridBagLayout());
			Color bgColor = resultPanel.getBackground();
			resultPanel.setBorder(new RoundedBorderPanel(bgColor));

			resultLbl = new JLabel("Result:");
			resultPanel.add(resultLbl, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 5), 0, 0));

			resultsTable = getDataTable();
			JScrollPane scrollableTableArea = new JScrollPane(resultsTable);

			Dimension dim = matcherPanel.getPreferredSize();
			scrollableTableArea.setPreferredSize(new Dimension(dim.width, dim.height - (resultLbl.getPreferredSize().height * 2)));
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

		JComboBox<String> comboBox = new JComboBox<>();
		EnumSet<VideoDataTags> all = EnumSet.allOf(VideoDataTags.class);
		for (VideoDataTags videoDataTag : all) {
			comboBox.addItem(videoDataTag.toString());
		}
		comboColumn.setCellEditor(new DefaultCellEditor(comboBox));

		// Set up tool tips for the sport cells.
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setToolTipText("Select a Video Data Tag");
		comboColumn.setCellRenderer(renderer);

	}

	private JPanel getBottomPanel() {
		if (bottomPanel == null) {
			bottomPanel = new JPanel(new FlowLayout());
			JLabel configName = new JLabel("Configuration Name: ");
			configField = new JTextField(45);
			loadBtn = new JButton("Load");
			cancelBtn = new JButton("Close");
			saveBtn = new JButton("Save");

			loadBtn.setName("Load");
			loadBtn.addActionListener(this);

			saveBtn.setName("Save");
			saveBtn.addActionListener(this);

			cancelBtn.setName("Close");
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
		if (btn.getName().equals("Enter")) {
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
			String[] result = extractResult(videoConfig);
			displayResult(result);

		} else if (btn.getName().equals("Load")) {
			JFileChooser fileChooser = new JFileChooser(voConfigHelper.getFolderPath());
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if (file.exists()) {
					configField.setText(file.getPath());
					errorField.setText("");
					VideoAnalysisConfig voConfig = voConfigHelper.loadConfigFile(file.getName());
					// get VideoAnalysisConfig type and setText of fields
					if (voConfig != null) {
						this.videoConfig = voConfig;
						regexRequestField.setText(voConfig.getRegex());
						regexResponseField.setText(voConfig.getResponseRegex());
						regexHeaderField.setText(voConfig.getHeaderRegex());
						String[] result = extractResult(voConfig);
						displayResult(result);
					} else {
						JOptionPane.showMessageDialog(this, "Failed to load the configuration file.", "Failure", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		} else if (btn.getName().equals("Close")) {
			clear();
			this.dispose();
			this.setVisible(false);
		} else if (btn.getName().equals("Save")) { // Saving after testing the regex by clicking on enter
			saveConfig();
		}
	}

	private void saveConfig() {
		if (this.videoConfig != null) {
			VideoDataTags[] xref = resultsTable.getVideoDataTags();
			if (xref.length > 0) {
				this.videoConfig.setXref(xref);
			}
		}
		// update object from fields
		updateConfigAndTags();

		if (this.videoConfig != null && voConfigHelper.validateConfig(videoConfig)) {
			try {
				JFileChooser fileChooser = new JFileChooser(voConfigHelper.getFolderPath());
				// fileChooser.setCurrentDirectory(new File(voConfigHelper.getFolderPath()));
				if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					voConfigHelper.saveConfigFile(videoConfig.getVideoType(), fileChooser.getSelectedFile().getName().replaceAll("\\.json", ""), videoConfig.getType(), videoConfig.getRegex(),
							videoConfig.getHeaderRegex(), videoConfig.getResponseRegex(), videoConfig.getXref());
				}
			} catch (JsonGenerationException e1) {
				log.error("VideoAnalysisConfig failed Jason generation :" + e1.getMessage());
			} catch (JsonMappingException e1) {
				log.error("VideoAnalysisConfig failed to de-serialize :" + e1.getMessage());
			} catch (IOException e1) {
				log.error("VideoAnalysisConfig failed to save :" + e1.getMessage());
			} catch (Exception e1) {
				log.error("VideoAnalysisConfig failed to load :" + e1.getMessage());
			}

		} else {
			log.error("VideoAnalysisConfig is invalid: capture groups not equal to cross references");
			JOptionPane.showMessageDialog(this,
					String.format("%s config is invalid: capture groups not equal to cross references", videoConfig != null ? videoConfig.getDesc() : "unknown")
					, "Failure"
					, JOptionPane.ERROR_MESSAGE);
		}

	}

	/**
	 * Manages adjustments to xref to match CaptureGroups
	 */
	private void updateConfigAndTags() {

		if (videoConfig == null) {
			return;
		}
		
		int[] venn = null;
		venn = findArrayVenn(videoConfig.getRegex() + videoConfig.getHeaderRegex() + videoConfig.getResponseRegex(),
				regexRequestField.getText() + regexHeaderField.getText() + regexResponseField.getText());
		if (venn != null && videoConfig.getXref() != null) {
			VideoDataTags[] xref = shift(venn[0], venn[1], VideoDataTags.unknown, videoConfig.getXref());
			if (xref != null) {
				videoConfig.setXref(xref);
				resultsTable.update(new String[videoConfig.getXref().length], videoConfig.getXref());
			}
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

		String[] res = voConfigHelper.match(videoConfig, requestField.getText(),headerField.getText(),responseField.getText());
		if (sbError.length() > 0) {
			errorField.setForeground(Color.red);
			errorField.setText(String.format("ERRORS :%s" ,sbError.toString()));
			displayResult(null);
		} else {
			errorField.setForeground(darkGreen);
			errorField.setText(String.format("Success %d capture groups", res!=null?res.length:0) );
		}

		videoConfig.setXref(resultsTable.getVideoDataTags());

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
		if (videoDataTags == null || (videoDataTags.length + delta) < 0) {
			return null;
		}
		VideoDataTags[] newTags = new VideoDataTags[videoDataTags.length + delta];
		Arrays.fill(newTags, VideoDataTags.unknown);
			
		if (newTags.length != 0) {
			int idy = 0;
			for (int idx = 0; idx < videoDataTags.length; idx++) {
				if (idx == position && delta != 0) {
					if (delta < 0) { // remove unused CaptureGroups xrefs
						idx -= delta; // skips idx ahead
						idx--;
					} else { // insert new CaptureGroups xrefs
						while (0 < delta--) {
							newTags[idy++] = tag;
						}
						newTags[idy++] = videoDataTags[idx];
					}
					position = -1;
				} else {
					newTags[idy++] = videoDataTags[idx];
				}
			}
		}
		return newTags;
	}

	/**
	 * <pre>
	 * Based on Venn diagram. Compares to Strings for CaptureGroups.
	 * 
	 * @param oStr
	 * @param nStr
	 * @return int[] null if no difference
     *         [0]:starting point
     *         [1]: number of insert or removal(if negative value)
     *         [2]: number of CaptureGroups in nStr
	 */
	private int[] findArrayVenn(String oStr, String nStr) {
		if (oStr.equals(nStr)) {
			return null;
		}

		String[] oStrng = oStr.replaceAll("\\\\\\(", "xxy").split("\\(");
		String[] nStrng = nStr.replaceAll("\\\\\\(", "xxy").split("\\(");

		int[] vennNumber = new int[3];
		vennNumber[2] = nStrng.length;
		vennNumber[1] = nStrng.length - oStrng.length;
		if (vennNumber[1] == 0) {
			return null;
		} else {
			for (int idx = 0; idx < oStrng.length && idx < nStrng.length; idx++) {
				if (!nStrng[idx].equals(oStrng[idx])) {
					vennNumber[0] = idx;
					return vennNumber;
				}
			}
		}
		return vennNumber;
	}

	Pattern pat = Pattern.compile("(\\(.+\\))");

	private String[] extractResult(VideoAnalysisConfig voConfig) {
		String[] result = voConfigHelper.match(voConfig, requestField.getText(), headerField.getText(), responseField.getText());
		return result;
	}

	public void displayResult(String[] result) {
		if (videoConfig != null) {
			resultsTable.update(result, videoConfig.getXref());
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
			if (lengthMinField.getText() != null && (!lengthMinField.getText().isEmpty())) {
				min = Integer.parseInt(lengthMinField.getText());
			}
			if (lengthMaxField.getText() != null && (!lengthMaxField.getText().isEmpty())) {
				max = Integer.parseInt(lengthMaxField.getText());
			}
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
		JTextField field = (JTextField) e.getSource();
		if (field.getName().equals(requestField.getName())) {
			setRequestHighlightedText(field.getSelectedText());
			updateFocus(true, false, false);
		} else if (field.getName().equals(headerField.getName())) {
			setHeaderHighlightedText(field.getSelectedText());
			updateFocus(false, false, true);
		} else if (field.getName().equals(responseField.getName())) {
			setResponseHighlightedText(field.getSelectedText());
			updateFocus(false, true, false);
		} else {// if(field.getName().equals(regexRequestField.getName()) || field.getName().equals(regexResponseField.getName()) ||
				// field.getName().equals(regexHeaderField.getName())){
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

}
