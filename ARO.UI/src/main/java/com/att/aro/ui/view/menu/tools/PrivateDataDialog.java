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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.pojo.PrivateDataInfo;
import com.att.aro.core.preferences.UserPreferences;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;

public class PrivateDataDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	
	private JPanel jDialogPanel;
	private IAROView parent;
	private PacketAnalyzerResult currentTraceResult;
	
	private JScrollPane scrollPane;
	private JPanel selectAll;
	private JCheckBox selectAllCheckbox;
	private JPanel privateDataListPanel;
	
	private Map<String, KeywordEntry> entries;
	private Map<JButton, String> removeToType;
	private Set<String> defaultKeywords;
	
	private JPanel ctrlPanel;
	private JPanel buttonGrid;
	private JButton addButton;
	private JButton okButton;
	private JButton cancelButton;
	
	private String errorMessage;
	private int totalEntries;
	private int totalSelected;
	
	private enum DialogItem {
		private_data_dialog_button_add,
		private_data_dialog_button_ok,
		private_data_dialog_button_cancel,
		private_data_dialog_button_remove,
		private_data_dialog_label_select_all,
		private_data_dialog_legend
	}
	
	/**
	 * keyword entry information
	 */
	private class KeywordEntry {
		JCheckBox checkBox;
		boolean selected;
		String type;
		JTextField textField;
		String value;
	}
	
	public PacketAnalyzerResult getCurrentPktAnalyzerResult() {
		return currentTraceResult;
	}

	public void setCurrentPktAnalyzerResult(PacketAnalyzerResult currentTraceResult) {
		this.currentTraceResult = currentTraceResult;
	}

	public PrivateDataDialog(IAROView parent) {
		this.parent = parent;
		((MainFrame) parent).setPrivateDataDialog(this);
		setDefaultKeywords();
		initialize();
	}
	
	private void setDefaultKeywords() {
		defaultKeywords = new HashSet<>();
		defaultKeywords.add(TraceDataConst.PrivateData.EMAIL_ADDRESS);
		defaultKeywords.add(TraceDataConst.PrivateData.PHONE_NUMBER);
		defaultKeywords.add(TraceDataConst.PrivateData.DATE_OF_BIRTH);
		defaultKeywords.add(TraceDataConst.PrivateData.LOCATION);
		defaultKeywords.add(TraceDataConst.PrivateData.USERNAME);
		defaultKeywords.add(TraceDataConst.PrivateData.PASSWORD);
		defaultKeywords.add(TraceDataConst.PrivateData.CALENDAR_EVENT);
		defaultKeywords.add(TraceDataConst.PrivateData.SOCIAL_SECURITY);
		defaultKeywords.add(TraceDataConst.PrivateData.CREDIT_CARD);
	}
	
	private void initialize() {
		PacketAnalyzerResult currentTraceResult = ((MainFrame) parent).getController().getTheModel().getAnalyzerResult();
		setCurrentPktAnalyzerResult(currentTraceResult);
		this.setSize(400, 500);
		this.setTitle(resourceBundle.getString("privatedatasetting.title"));
		this.setLocationRelativeTo(getOwner());
		this.setContentPane(getJDialogPanel());
	}
	
	private JPanel getJDialogPanel() {
		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setLayout(new BorderLayout());
			
			jDialogPanel.add(getScrollPane(), BorderLayout.CENTER);
			jDialogPanel.add(getCtrlPanel(), BorderLayout.SOUTH);
		}
		return jDialogPanel;
	}
	
	/**
	 * ctrl panel contains button grid, which contains + button, ok button and cancel button
	 * @return
	 */
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
			
			buttonGrid.add(getAddButton());
			buttonGrid.add(getOkButton());
			buttonGrid.add(getCancelButton());
		}
		
		return buttonGrid;
	}
	
	/**
	 * + (add) button
	 */
	private JButton getAddButton() {
		if (addButton == null) {
			addButton = new JButton();
			addButton.setText(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_button_add));
			addButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					executeAddButton();
				}
			});
		}
		
		return addButton;
	}
	
	private void executeAddButton() {
		JDialog addKeywordDialog = new AddKeywordDialog(this);
		addKeywordDialog.setVisible(true);
	}
	
	/**
	 * OK button
	 * @return
	 */
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_button_ok));
			okButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					executeOkButton();
				}
			});
		}
		
		return okButton;
	}
	
	/**
	 * click ok button, then save user preference setting and re-analyze
	 */
	private void executeOkButton() {
		if (savePreference()) {
			clean();
			if (getCurrentPktAnalyzerResult() != null) {
				refresh();
			}
		} else {
			String rawMessage = resourceBundle.getString("error.dialog.message");
			String message = MessageFormat.format(rawMessage, errorMessage);
			MessageDialogFactory.getInstance().showErrorDialog(this, message);
		}
	}
	
	/**
	 * re-analyze best practices
	 */
	private void refresh() {
		AnalysisFilter filter = getCurrentPktAnalyzerResult().getFilter();
		((MainFrame) parent).updateFilter(filter);
	}
	
	/**
	 * save preference
	 */
	private boolean savePreference() {
		List<PrivateDataInfo> infos = prepareSerializeUserPreference(entries);
		if (infos == null) {
			return false;
		}
		UserPreferences prefs = UserPreferencesFactory.getInstance().create();
		prefs.setPrivateData(infos);
		return true;
	}
	
	/**
	 * prepare data list and set to UserPreference
	 * @param entries
	 * @return
	 */
	private List<PrivateDataInfo> prepareSerializeUserPreference(Map<String, KeywordEntry> entries) {
		Set<String> types = entries.keySet();
		List<PrivateDataInfo> infos = new LinkedList<PrivateDataInfo>();
		List<String> errors = new LinkedList<String>();
		for(String type : types) {
			KeywordEntry entry = entries.get(type);
			
			String text = entries.get(entry.type).textField.getText();
			boolean isSelected = entries.get(entry.type).checkBox.isSelected();
			if (!validatePrivateData(text) && isSelected) {
				errors.add(entry.type);
				continue;
			}
			
			PrivateDataInfo info = new PrivateDataInfo();
			info.setCategory(TraceDataConst.PrivateData.KEYWORD_CATEGORY);
			info.setType(entry.type);
			info.setValue(text);
			info.setSelected(isSelected);
			infos.add(info);
		}
		
		if (errors.isEmpty()) {
			return infos;
		} else {
			errorMessage = getErrorMessage(errors);
			return null;
		}
	}
	
	/**
	 * validate each private data value
	 * @param text
	 * @return
	 */
	private boolean validatePrivateData(String text) {
		return text != null && !text.isEmpty();
	}
	
	/**
	 * get error message based on the missing values
	 * @param errors
	 * @return
	 */
	private String getErrorMessage(List<String> errors) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < errors.size(); i++) {
			builder.append(errors.get(i));
			if (i < errors.size() - 1) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}
	
	/**
	 * Cancel button
	 * @return
	 */
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
		clean();
	}
	
	/**
	 * scroll pane contains private data list panel
	 * @return
	 */
	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane(getPrivateDataListPanel());
			scrollPane.setBorder(BorderFactory.createTitledBorder(null, 
					ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_legend)));
			scrollPane.setBackground(new Color(238, 238, 238));
		}
		
		return scrollPane;
	}
	
	/**
	 * private data list panel contains select all panel, and private data entries, which are default entries and customized entries
	 * @return
	 */
	private JPanel getPrivateDataListPanel() {
		if (privateDataListPanel == null) {
			privateDataListPanel = new JPanel();
			entries = new HashMap<String, KeywordEntry>();
			removeToType = new HashMap<JButton, String>();
			
			privateDataListPanel.setLayout(new BoxLayout(privateDataListPanel, BoxLayout.Y_AXIS));
			privateDataListPanel.add(getSelectAllPanel());	// add panel contains "select all" check box
			
			List<PrivateDataInfo> infos = deserializeUserPreference();
			setDefaultEntries(infos);
			setCustomizedEntries(infos);
			
			setSelectAllCheckboxSelected(infos, selectAllCheckbox);
		}
		
		return privateDataListPanel;
	}
	
	/**
	 * initiate select all check box to be checked if all the entries have been checked
	 * @param infos
	 * @param selectAllCheckbox
	 */
	private void setSelectAllCheckboxSelected(List<PrivateDataInfo> infos, JCheckBox selectAllCheckbox) {
		totalEntries = infos.size();
		totalSelected = 0;
		for(PrivateDataInfo info : infos) {
			if (info.isSelected()) {
				totalSelected++;
			}
		}
		selectAllCheckbox.setSelected((totalEntries == 0) ? false : (totalSelected == totalEntries));
	}
	
	/**
	 * read user preference
	 * @return
	 */
	private List<PrivateDataInfo> deserializeUserPreference() {
		UserPreferences prefs = UserPreferencesFactory.getInstance().create();
		return prefs.getPrivateData();
	}
	
	/**
	 * set default entries
	 * @param infos
	 */
	private void setDefaultEntries(List<PrivateDataInfo> infos) {
		Map<String, PrivateDataInfo> buffer = new HashMap<>();
		for(PrivateDataInfo info : infos) {
			buffer.put(info.getType(), info);
		}
		
		for(String keyword : defaultKeywords) {
			PrivateDataInfo info;
			if (buffer.containsKey(keyword)) {
				info = buffer.get(keyword);
			} else {
				info = new PrivateDataInfo();
				info.setCategory(TraceDataConst.PrivateData.KEYWORD_CATEGORY);
				info.setType(keyword);
			}
			KeywordEntry entry = initKeywordEntry(info);
			privateDataListPanel.add(getKeywordEntryGrid(entry, false));
			entries.put(entry.type, entry);
		}
	}
	
	/**
	 * set customized entries
	 * @param infos
	 */
	private void setCustomizedEntries(List<PrivateDataInfo> infos) {
		for(PrivateDataInfo info : infos) {
			if (TraceDataConst.PrivateData.KEYWORD_CATEGORY.equals(info.getCategory()) && 
					!defaultKeywords.contains(info.getType())) {
				KeywordEntry entry = initKeywordEntry(info);
				privateDataListPanel.add(getKeywordEntryGrid(entry, true));
				entries.put(entry.type, entry);
			}
		}
	}
	
	/**
	 * used by AddKeywordDialog for adding a new keyword entry
	 * @param info
	 */
	public void addNewEntry(PrivateDataInfo info) {
		KeywordEntry entry = initKeywordEntry(info);
		privateDataListPanel.add(getKeywordEntryGrid(entry, true));
		entries.put(entry.type, entry);
		addEntryCount();
		validate();
	}
	
	private void addEntryCount() {
		totalEntries++;
		selectAllCheckbox.setSelected(totalSelected == totalEntries);
	}
	
	private KeywordEntry initKeywordEntry(PrivateDataInfo info) {
		KeywordEntry entry = new KeywordEntry();
		entry.type = info.getType();
		entry.value = info.getValue();
		entry.selected = info.isSelected();
		return entry;
	}
	
	/**
	 * select all panel, which contains select all check box
	 * @return
	 */
	private JPanel getSelectAllPanel() {
		if (selectAll == null) {
			selectAll = new JPanel();
			selectAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			selectAll.setLayout(new BorderLayout(10, 10));
			
			selectAllCheckbox = getCheckBox();
			selectAll.add(selectAllCheckbox, BorderLayout.WEST);
			
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new BorderLayout());
			labelPanel.add(getLabel(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_label_select_all)), BorderLayout.WEST);
			selectAll.add(labelPanel);
		}
		
		return selectAll;
	}
	
	private JPanel getKeywordEntryGrid(KeywordEntry entry, boolean isRemoveBtnEnabled) {
		JPanel keywordGrid = new JPanel();
		keywordGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		keywordGrid.setLayout(new BorderLayout(10, 10));
		
		keywordGrid.add(getCheckBox(entry), BorderLayout.WEST);
		keywordGrid.add(getKeyword(entry), BorderLayout.CENTER);
		keywordGrid.add(getRemoveButton(entry.type, isRemoveBtnEnabled), BorderLayout.EAST);
		
		return keywordGrid;
	}
	
	/**
	 * generate check box for select all section
	 * @param selected
	 * @return
	 */
	private JCheckBox getCheckBox() {
		final JCheckBox checkBox = new JCheckBox();
		checkBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				changeAllCheckBoxes(checkBox);
			}
		});
		return checkBox;
	}
	
	/**
	 * select/un-select all check boxes
	 */
	private void changeAllCheckBoxes(JCheckBox selectAll) {
		Set<String> keySet = entries.keySet();
		for(String key : keySet) {
			entries.get(key).checkBox.setSelected(selectAll.isSelected());
		}
		if (selectAll.isSelected()) {
			totalSelected = totalEntries;
		} else {
			totalSelected = 0;
		}
	}
	
	/**
	 * generate check box for private data list
	 * @param selected
	 * @param labelName
	 * @return
	 */
	private JCheckBox getCheckBox(KeywordEntry entry) {
		final JCheckBox checkBox = new JCheckBox();
		checkBox.setSelected(entry.selected);
		entry.checkBox = checkBox;
		checkBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkBox.isSelected()) {
					totalSelected++;
					if (totalSelected == totalEntries) {
						selectAllCheckbox.setSelected(true);
					}
				} else {
					totalSelected--;
					if (selectAllCheckbox.isSelected()) {
						selectAllCheckbox.setSelected(false);
					}
				}
			}
		});
		
		return checkBox;
	}
	
	private JPanel getKeyword(KeywordEntry entry) {
		JPanel keyword = new JPanel();
		keyword.setLayout(new BorderLayout());
		
		keyword.add(getLabel(entry.type), BorderLayout.WEST);
		JTextField valueField = getValue(entry.value);
		keyword.add(valueField, BorderLayout.EAST);
		entry.textField = valueField;
		return keyword;
	}
	
	/**
	 * get private data type label
	 * @param labelName
	 * @return
	 */
	private JLabel getLabel(String labelName) {
		JLabel label = new JLabel(labelName);
		return label;
	}
	
	/**
	 * get private data value text field
	 * @param value
	 * @return
	 */
	private JTextField getValue(String value) {
		JTextField textField = new JTextField();
		textField.setPreferredSize(new Dimension(180, 20));
		if (value != null && !value.isEmpty()) {
			textField.setText(value);
		}
		return textField;
	}
	
	/**
	 * get remove button for each entry
	 * @param type
	 * @param isEnabled
	 * @return
	 */
	private JButton getRemoveButton(String type, boolean isEnabled) {
		final JButton remove = new JButton();
		remove.setText(ResourceBundleHelper.getMessageString(DialogItem.private_data_dialog_button_remove));
		remove.setPreferredSize(new Dimension(40, 20));
		remove.setEnabled(isEnabled);
		remove.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				executeRemoveButton(remove);
			}
		});
		removeToType.put(remove, type);
		return remove;
	}
	
	private void executeRemoveButton(JButton button) {
		JPanel keywordGrid = (JPanel) button.getParent();
		privateDataListPanel.remove(keywordGrid);
		
		String type = removeToType.get(button);
		removeToType.remove(button);
		KeywordEntry entry = entries.remove(type);
		removeEntryCount(entry);
		
		validate();
	}
	
	private void removeEntryCount(KeywordEntry entry) {
		totalEntries--;
		if (entry.selected) {
			totalSelected--;
		}
		selectAllCheckbox.setSelected(totalSelected == totalEntries);
	}
	
	/**
	 * clean up dialog and set reference to null
	 */
	private void clean() {
		setVisible(false);
		((MainFrame) parent).setPrivateDataDialog(null);
		dispose();
	}
}
