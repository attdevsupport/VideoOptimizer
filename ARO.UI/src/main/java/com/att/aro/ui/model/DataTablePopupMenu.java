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
package com.att.aro.ui.model;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 *
 *
 * Represents the default popup menu for a data table in the ARO Data Analyzer.
 */
public class DataTablePopupMenu extends JPopupMenu {
	private static final char COMMA_SEPARATOR = ',';

	private static final long serialVersionUID = 1L;

	private DataTable<?> table;

	private JMenuItem exportMenuItem;

	private File exportPath = UserPreferencesFactory.getInstance().create().getLastExportDirectory();
	private String titleDialog = ResourceBundleHelper.getMessageString("fileChooser.Title");

	/**
	 * Initializes a new instance of the DataTablePopupMenu class using the
	 * specified DataTable object.
	 * 
	 * @param table The DataTable to associate with the DataTablePopupMenu.
	 */
	public DataTablePopupMenu(DataTable<?> table) {
		this.table = table;
		initialize();
	}

	public void setExportPath(File exportPath) {
		this.exportPath = exportPath;
	}

	private File getVideoRequestExportDriectory() {
		String tracePath = PreferenceHandlerImpl.getInstance().getPref("TRACE_PATH");
		File exportPathDirectory = UserPreferencesFactory.getInstance().create().getLastExportDirectory();
		if (tracePath != null) {
			tracePath = tracePath + "/exports";
			File exportPath = new File(tracePath);
			if (!exportPath.isDirectory()) {
				exportPath.mkdirs();
			}
			exportPathDirectory = exportPath;
		}
		return exportPathDirectory;
	}

	public void setTitleDialog(String titleDialog) {
		this.titleDialog = titleDialog;
	}

	/**
	 * Method to put the Export menu item in table.
	 */
	private void initialize() {
		this.add(getExportMenuItem());
	}

	/**
	 * Method to initialize the Export menu item for the table.
	 * 
	 * @return the exportMenuItem
	 */
	private JMenuItem getExportMenuItem() {
		if (exportMenuItem == null) {
			exportMenuItem = new JMenuItem(ResourceBundleHelper.getMessageString("table.export"));
			exportMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent aEvent) {
					File defaultFile = null;
					if (table == null) {
						return;
					}
					if ("VideoRequestTable".equals(table.getName())) {
						setExportPath(getVideoRequestExportDriectory());
						String defaultFileName = exportPath.getAbsolutePath() + "/VideoRequestTable.csv";
						defaultFile = new File(defaultFileName);
					}

					boolean isSessionsTable = table.getName() != null
							&& "sessionTable".equalsIgnoreCase(table.getName());
					if (isSessionsTable) {
						boolean isAnyRowSelected = false;
						for (int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex) {
							if (isAnyRowSelected = isAnyRowSelected
								|| ((Boolean) table.getValueAt(rowIndex, 1)).booleanValue()) {
								break;
							}
						}

						if (!isAnyRowSelected) {
							MessageDialogFactory.showMessageDialog(null,
									ResourceBundleHelper.getMessageString("tcp.error.noRowSelected"));
							return;
						}
					}
					exportTable(defaultFile, isSessionsTable);
				}

			});
		}
		return exportMenuItem;
	}

	private void exportTable(File defaultFile, boolean isSessionsTable) {
					JFileChooser chooser = new JFileChooser(exportPath);
					if (defaultFile != null) {
						chooser.setSelectedFile(defaultFile);
					}
					chooser.setDialogTitle(titleDialog);
					FileNameExtensionFilter filter = new FileNameExtensionFilter(
							ResourceBundleHelper.getMessageString("fileChooser.desc.csv"),
							ResourceBundleHelper.getMessageString("fileChooser.contentType.csv"));
					chooser.setFileFilter(filter);
					chooser.addChoosableFileFilter(null);
					chooser.setApproveButtonText(ResourceBundleHelper.getMessageString("fileChooser.Save"));
					chooser.setMultiSelectionEnabled(false);
					try {
						saveFile(chooser, isSessionsTable);
						if ("VideoRequestTable".equals(table.getName()) && chooser.getCurrentDirectory() != exportPath) {
							exportPath.delete();
						}
					} catch (Exception exp) {
						String errorMsg = MessageFormat.format(ResourceBundleHelper.getMessageString("exportall.errorFileOpen"),
								ApplicationConfig.getInstance().getAppShortName());
						MessageDialogFactory.getInstance().showErrorDialog(new Window(new Frame()), errorMsg + exp.getMessage());
		}
	}

	/**
	 * Method to export the table content in to the CSV file format.
	 * 
	 * @param chooser {@link JFileChooser} object to validate the save option.
	 */
	private void saveFile(JFileChooser chooser, boolean isSessionsTable) throws Exception {
		Frame frame = Frame.getFrames()[0];// get parent frame
		if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			if (!chooser.getFileFilter().accept(file)) {
				file = new File(file.getAbsolutePath() + "."
						+ ResourceBundleHelper.getMessageString("fileChooser.contentType.csv"));
			}
			if (file.exists()) {
				if (MessageDialogFactory.getInstance().showConfirmDialog(frame,
						MessageFormat.format(ResourceBundleHelper.getMessageString("fileChooser.fileExists"),
								file.getAbsolutePath()),
						JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
					saveFile(chooser, isSessionsTable);
					return;
				}
			}

			FileWriter writer = new FileWriter(file);
			try {
				writeFile(writer, isSessionsTable);

			} finally {
				writer.close();
			}
			if (file.getName().contains(".csv")) {
				if (MessageDialogFactory.getInstance().showExportConfirmDialog(frame) == JOptionPane.YES_OPTION) {
					try {
						Desktop desktop = Desktop.getDesktop();
						desktop.open(file);
					} catch (UnsupportedOperationException unsupportedException) {
						MessageDialogFactory.showMessageDialog(frame,
								ResourceBundleHelper.getMessageString("Error.unableToOpen"));
					}
				}
			} else {
				MessageDialogFactory.showMessageDialog(frame,
						ResourceBundleHelper.getMessageString("table.export.success"));
			}
		}
	}

	/**
	 * Method to convert the {@link Object} values in to {@link String} values.
	 * 
	 * @param val {@link Object} value retrieved from the table cell.
	 * @return Cell data in string format.
	 */
	private String createCSVEntry(Object val) {
		StringBuffer writer = new StringBuffer();
		String str = val != null ? val.toString() : "";
		writer.append('"');
		for (char strChar : str.toCharArray()) {
			switch (strChar) {
			case '"':
				// Add an extra
				writer.append("\"\"");
				break;
			default:
				writer.append(strChar);
			}
		}
		writer.append('"');
		return writer.toString();
	}

	private void writeFile(FileWriter writer, boolean isSessionsTable) throws IOException {

		List<Integer> selectedRows = new ArrayList<>();
		int count = 0;

		if (isSessionsTable) {
			for (int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex) {
				if (((Boolean) table.getValueAt(rowIndex, 1)).booleanValue()) {
					selectedRows.add(rowIndex);
			}
			}
		} else {
			for (int index = 0; index < table.getSelectedRows().length; index++) {
				selectedRows.add(table.getSelectedRows()[index]);
			}
		}

		if (selectedRows.size() > 0) {
			count = selectedRows.size();
		} else if (!isSessionsTable) {
			count = table.getRowCount();
					}
		for (int rowIndex = 0; rowIndex < count; ++rowIndex) {
			for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
					if (columnIndex > 0) {
						writer.append(COMMA_SEPARATOR);
					}
					if (columnIndex == 1 && isSessionsTable) {
						continue;
				}
					if (rowIndex == 0 && columnIndex == 0) {
						createHeader(writer, isSessionsTable);
			}
					if (selectedRows.size() > 0) {
						writer.append(createCSVEntry(table.getValueAt(selectedRows.get(rowIndex), columnIndex)));

		} else {
			writer.append(createCSVEntry(table.getValueAt(rowIndex, columnIndex)));
					}
					}

			writer.append(Util.LINE_SEPARATOR);
				}

			}

	private void createHeader(FileWriter writer, boolean isSessionsTable) throws IOException {

		for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
			if (columnIndex > 0) {
				writer.append(',');
			}
			if (columnIndex == 1 && isSessionsTable) {
				continue;
		}
			writer.append(createCSVEntry(table.getColumnModel().getColumn(columnIndex).getHeaderValue()));
		}
		writer.append(Util.LINE_SEPARATOR);

	}
}
