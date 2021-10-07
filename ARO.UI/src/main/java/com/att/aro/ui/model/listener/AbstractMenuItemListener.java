package com.att.aro.ui.model.listener;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.utils.ResourceBundleHelper;

import lombok.Data;

@Data
public abstract class AbstractMenuItemListener implements ActionListener {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractMenuItemListener.class);

	protected static final String COMMA_SEPARATOR = ",";

	protected DataTable<?> table;

	private boolean exportSessionData = false;

	public AbstractMenuItemListener(DataTable<?> table) {
		this.table = table;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		File defaultFile = null;
		exportTable(defaultFile);
	}

	private void exportTable(File defaultFile) {
			JFileChooser chooser = getDefaultFileChooser(defaultFile);
			saveFile(chooser);
	}

	protected JFileChooser getDefaultFileChooser(File file) {
		JFileChooser chooser;
		if (file != null) {
			chooser = new JFileChooser(file);
		} else {
			String defaultFilePath = UserPreferencesFactory.getInstance().create().getTracePath();
			if (table != null && table.getName() != null) {
				defaultFilePath = defaultFilePath + Util.FILE_SEPARATOR + table.getName();
			} else {
				defaultFilePath = defaultFilePath + Util.FILE_SEPARATOR + "table";
			}

			chooser = new JFileChooser();
			chooser.setSelectedFile(new File(defaultFilePath));
		}

		String titleDialog = ResourceBundleHelper.getMessageString("fileChooser.Title");
		chooser.setDialogTitle(titleDialog);
		// Set allowed file extensions
		FileNameExtensionFilter csvFilter = new FileNameExtensionFilter(
				ResourceBundleHelper.getMessageString("fileChooser.desc.csv"),
				ResourceBundleHelper.getMessageString("fileChooser.contentType.csv"));
		FileNameExtensionFilter xlsxFilter = new FileNameExtensionFilter(
				ResourceBundleHelper.getMessageString("fileChooser.desc.excel"),
				ResourceBundleHelper.getMessageString("fileChooser.contentType.xls"),
				ResourceBundleHelper.getMessageString("fileChooser.contentType.xlsx"));
		chooser.addChoosableFileFilter(csvFilter);
		chooser.addChoosableFileFilter(xlsxFilter);
		chooser.setFileFilter(xlsxFilter);
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
					LOG.error("Something went wrong while exporting table {}", table != null ? table.getName() : "",
							exception);
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