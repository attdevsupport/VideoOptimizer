package com.att.aro.ui.model.listener;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.google.common.collect.Lists;


public abstract class AbstractMenuItemListener implements ActionListener {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractMenuItemListener.class);

	protected static final String COMMA_SEPARATOR = ",";

	protected DataTable<?> table;
	protected String tableName;
	private List<FileNameExtensionFilter> fileNameExtensionFilters;
	private int defaultExtensionFilterIndex;


	public AbstractMenuItemListener(DataTable<?> table) {
		this.table = table;
		fileNameExtensionFilters = getDefaultxtensionFilters();
		defaultExtensionFilterIndex = 1; // Default file filter as xlsx filter
	}

	/**
	 *
	 * @param table
	 * @param fileNameExtensionFilters List of file extension filters to display in the file chooser dialog
	 * @param defaultExtensionFilterIndex Default selected filter index to display in the file chooser dialog
	 */
	public AbstractMenuItemListener(DataTable<?> table, List<FileNameExtensionFilter> fileNameExtensionFilters, int defaultExtensionFilterIndex) {
		this.table = table;
		this.fileNameExtensionFilters = CollectionUtils.isNotEmpty(fileNameExtensionFilters) ? fileNameExtensionFilters : getDefaultxtensionFilters();
		this.defaultExtensionFilterIndex = (defaultExtensionFilterIndex >= 0 && defaultExtensionFilterIndex < fileNameExtensionFilters.size()) ? defaultExtensionFilterIndex : 0;
	}


	private List<FileNameExtensionFilter> getDefaultxtensionFilters() {
		FileNameExtensionFilter csvFilter = new FileNameExtensionFilter(
				ResourceBundleHelper.getMessageString("fileChooser.desc.csv"),
				ResourceBundleHelper.getMessageString("fileChooser.contentType.csv"));
		FileNameExtensionFilter xlsxFilter = new FileNameExtensionFilter(
				ResourceBundleHelper.getMessageString("fileChooser.desc.excel"),
				ResourceBundleHelper.getMessageString("fileChooser.contentType.xls"),
				ResourceBundleHelper.getMessageString("fileChooser.contentType.xlsx"));
		return Lists.newArrayList(csvFilter, xlsxFilter);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		File defaultFile = null;
		exportTable(defaultFile);
	}

	private void exportTable(File defaultFile) {
		try {
			JFileChooser chooser = getDefaultFileChooser(defaultFile);
			saveFile(chooser);
		} catch (Exception e) {
			LOG.error("Something went wrong while exporting table {}", table != null ? table.getName() : "null", e);
			String errorMsg = MessageFormat.format(ResourceBundleHelper.getMessageString("exportall.error"),
					ApplicationConfig.getInstance().getAppShortName());
			MessageDialogFactory.getInstance().showErrorDialog(new Window(new Frame()), errorMsg + e.getMessage());
		}
	}

	protected JFileChooser getDefaultFileChooser(File file) {
		JFileChooser chooser;
		if (file != null) {
			chooser = new JFileChooser(file);
		} else {
			String defaultFilePath = UserPreferencesFactory.getInstance().create().getTracePath();
			File filePathObj = new File(defaultFilePath);

			String traceFolderName = "_";
			if (filePathObj.isDirectory()) {
				traceFolderName = StringUtils.replace(filePathObj.getName(), " ", "_") + "_";
			} else {
				if (filePathObj.getParentFile() != null) {
					traceFolderName = StringUtils.replace(filePathObj.getParentFile().getName(), " ", "_") + "_";
				}
			}

			String tableName = StringUtils.isNotBlank(this.tableName) ? this.tableName
										: (table != null && StringUtils.isNotBlank(table.getName())) ? table.getName() : "table";
			defaultFilePath = defaultFilePath + Util.FILE_SEPARATOR + traceFolderName + tableName;

			chooser = new JFileChooser();
			chooser.setSelectedFile(new File(defaultFilePath));
		}

		String titleDialog = ResourceBundleHelper.getMessageString("fileChooser.Title");
		chooser.setDialogTitle(titleDialog);
		// Set allowed file extensions
		for (FileNameExtensionFilter filter : fileNameExtensionFilters) {
			chooser.addChoosableFileFilter(filter);
		}
		chooser.setFileFilter(fileNameExtensionFilters.get(defaultExtensionFilterIndex));
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setApproveButtonText(ResourceBundleHelper.getMessageString("fileChooser.Save"));
		chooser.setMultiSelectionEnabled(false);

		return chooser;
	}

	/**
	 * Method to export the table content in to the CSV, XLS or XLSX file formats.
	 * 
	 * @param chooser {@link JFileChooser} object to validate the save option.
	 */
	private void saveFile(JFileChooser chooser) {
		Frame frame = Frame.getFrames()[0];// get parent frame
		boolean isCSV = true;

		if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();

			if (chooser.getFileFilter().getDescription()
					.equals(ResourceBundleHelper.getMessageString("fileChooser.desc.excel"))) {
				isCSV = false;
				if (!chooser.getFileFilter().accept(file)) {
					file = new File(file.getAbsolutePath() + "."
							+ ResourceBundleHelper.getMessageString("fileChooser.contentType.xlsx"));
				}
			} else {
				if (!file.getAbsolutePath().endsWith(".csv")) {
					file = new File(file.getAbsolutePath() + "."
							+ ResourceBundleHelper.getMessageString("fileChooser.contentType.csv"));
				}
			}

			if (file.exists()) {
				if (MessageDialogFactory.getInstance().showConfirmDialog(frame,
						MessageFormat.format(ResourceBundleHelper.getMessageString("fileChooser.fileExists"),
								file.getAbsolutePath()),
						JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
					saveFile(chooser);
					return;
				}
			}

			final File fileToWrite = file;
			final boolean isWriteToCSV = isCSV;
			
			Runnable exportData = () -> {
				try {
					if (isWriteToCSV) {
						writeCSV(fileToWrite);
					} else {
						writeExcel(fileToWrite);
					}
					if (MessageDialogFactory.getInstance()
							.showExportConfirmDialog(frame) == JOptionPane.YES_OPTION) {
						Desktop desktop = Desktop.getDesktop();
						desktop.open(fileToWrite);
					}
				} catch (IOException exception) {
					LOG.error("Something went wrong while exporting table {}", table != null ? table.getName() : "null", exception);
					String errorMsg = MessageFormat.format(ResourceBundleHelper.getMessageString("exportall.error"),
							ApplicationConfig.getInstance().getAppShortName());
					MessageDialogFactory.getInstance().showErrorDialog(new Window(new Frame()),
							errorMsg + exception.getMessage());
				}
			};

			// start the thread
			new Thread(exportData).start();
			
		}
	}

	/**
	 * Method to convert the {@link Object} values in to {@link String} values.
	 * 
	 * @param val   {@link Object} value retrieved from the table cell.
	 * @param isCSV if entry is for CSV file or not
	 * @return Cell data in string format.
	 */
	protected String createCSVEntry(Object val, boolean isCSV) {
		StringBuilder writer = new StringBuilder();
		String str = val != null ? val.toString() : "";

		if (isCSV) {
			writer.append('"');
		}

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

		if (isCSV) {
			writer.append('"');
		}

		return writer.toString();
	}

	public abstract void writeExcel(File file) throws IOException;

	public abstract void writeCSV(File file) throws IOException;
}