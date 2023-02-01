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
package com.att.aro.ui.view.menu;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.android.ddmlib.IDevice;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.Util;
import com.att.aro.core.util.VideoUtils;
import com.att.aro.ui.commonui.AROMenuAdder;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.model.listener.BestPracticeResultsListener;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.menu.tools.AWSDialog;
import com.att.aro.ui.view.menu.tools.AWSDialog.AWS;
import com.att.aro.ui.view.menu.tools.ExportReport;
import com.att.aro.ui.view.menu.tools.MSPostDialog;
import com.att.aro.ui.view.menu.tools.MetadataDialog;
import com.att.aro.ui.view.menu.tools.PrivateDataDialog;
import com.att.aro.ui.view.menu.tools.RegexWizard;
import com.att.aro.ui.view.menu.tools.TimeRangeAnalysisDialog;
import com.google.common.collect.Lists;

/**
 * This class adds the menu items under the Tools menu
 * 
 */
public class AROToolMenu implements ActionListener {
	private static final String FILE_NAME = "Logcat_%s_%d.log";
	private static final Logger LOG = LogManager.getLogger(AROToolMenu.class);
	private IAdbService adbservice = ContextAware.getAROConfigContext().getBean(IAdbService.class);
	private AWSDialog uploadDialog;

	private final AROMenuAdder menuAdder = new AROMenuAdder(this);
	private JMenu toolMenu;
	SharedAttributesProcesses parent;

	private enum MenuItem {
		menu_tools, menu_tools_wireshark, menu_tools_timeRangeAnalysis, menu_tools_dataDump, menu_tools_resultExport, menu_tools_htmlExport,
		menu_tools_jsonExport, menu_tools_sessionsExport, menu_tools_excelExport, menu_tools_privateData,
		menu_tools_videoAnalysis, menu_tools_getErrorMsg, menu_tools_clearErrorMsg, menu_tools_videoParserWizard,
		menu_tools_uploadTraceDialog, menu_tools_downloadTraceDialog, menu_tools_editMetadata,
		menu_tools_ms_uploadTraceDialog, menu_tools_ms_downloadTraceDialog
	}

	public AROToolMenu(SharedAttributesProcesses parent) {
		super();
		this.parent = parent;
	}

	/**
	 * @return the toolMenu
	 */
	public JMenu getMenu() {

		toolMenu = new JMenu(ResourceBundleHelper.getMessageString(MenuItem.menu_tools));
		toolMenu.setMnemonic(KeyEvent.VK_UNDEFINED);
		boolean isTracePathEmpty = true;
		isTracePathEmpty = isTracePathEmpty();

		if (Desktop.isDesktopSupported()) {
			boolean noPcap = true;
			if (parent.getTracePath() != null) {
				if (VideoUtils.validateFolder(new File(parent.getTracePath()), VideoUtils.TRAFFIC, VideoUtils.TRAFFIC_EXTENTIONS).size() > 0) {
					noPcap = false;
				}
			}
			toolMenu.add(getMenuItem(MenuItem.menu_tools_wireshark, noPcap));
		}
		toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_timeRangeAnalysis));
		toolMenu.addSeparator();

		// xlsx/xls file name extension filter
        FileNameExtensionFilter xlsxFilter = new FileNameExtensionFilter(
        											ResourceBundleHelper.getMessageString("fileChooser.desc.excel"),
													ResourceBundleHelper.getMessageString("fileChooser.contentType.xls"),
													ResourceBundleHelper.getMessageString("fileChooser.contentType.xlsx"));

		JMenu exportMenu = menuAdder.getMenuInstance(ResourceBundleHelper.getMessageString("menu.tools.resultExport"));
		exportMenu.add(getMenuItem(MenuItem.menu_tools_htmlExport, isTracePathEmpty));
		exportMenu.add(getMenuItem(MenuItem.menu_tools_jsonExport, isTracePathEmpty));
        // Excel export menu item
 		JMenuItem excelExportMenuItem = getMenuItem(MenuItem.menu_tools_excelExport, isTracePathEmpty);
 		excelExportMenuItem.addActionListener(new BestPracticeResultsListener(((MainFrame) parent).getController().getTheModel(), Lists.newArrayList(xlsxFilter), 0));
 		exportMenu.add(excelExportMenuItem);
 		toolMenu.add(exportMenu);

		JMenuItem exportExportMenuItem = getMenuItem(MenuItem.menu_tools_sessionsExport, isTracePathEmpty);
		exportExportMenuItem.addActionListener(new ExportSessionData((MainFrame) parent, Lists.newArrayList(xlsxFilter), 0));
		toolMenu.add(exportExportMenuItem);
		toolMenu.addSeparator();
		toolMenu.add(getMenuItem(MenuItem.menu_tools_editMetadata, 
				(isTracePathEmpty 
				|| !new File(parent.getTracePath(), ".temp_trace").exists()
				|| new File(parent.getTracePath()).isDirectory()
				)
				));

		toolMenu.addSeparator();
		toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_privateData));
		if (ResourceBundleHelper.getMessageString("preferences.test.env").equals(SettingsImpl.getInstance().getAttribute("env"))) {
			toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_getErrorMsg));
			toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_clearErrorMsg));
		}
		toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_videoParserWizard));
		
		if ("dev".equals(SettingsImpl.getInstance().getAttribute("env"))) {
			if (SettingsImpl.getInstance().getAttribute("traceHandlerURL") != null && SettingsImpl.getInstance().checkAttributeValue("env", "dev")) {
				toolMenu.addSeparator();
				toolMenu.add(getMenuItem(MenuItem.menu_tools_ms_uploadTraceDialog, isTracePathEmpty));
			}
		}
		return toolMenu;
	}

	/**
	 * Populate a JMenuItem, will be enabled/disabled depending on the boolean disable
	 * 
	 * @param item
	 * @param disable
	 * @return
	 */
	public JMenuItem getMenuItem(MenuItem item, boolean disable) {
		JMenuItem menuItem = menuAdder.getMenuItemInstance(item);
		menuItem.setEnabled(!disable);
		return menuItem;
	}

	@Override
	public void actionPerformed(ActionEvent aEvent) {
		if (menuAdder.isMenuSelected(MenuItem.menu_tools_wireshark, aEvent)) {
			openPcapAnalysis();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_timeRangeAnalysis, aEvent)) {
			openTimeRangeAnalysis();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_htmlExport, aEvent)) {
			exportHtml();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_jsonExport, aEvent)) {
			exportJson();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_privateData, aEvent)) {
			openPrivateDataDialog();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_getErrorMsg, aEvent)) {
			collectErrorMessage();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_clearErrorMsg, aEvent)) {
			clearErrorMessage();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_videoParserWizard, aEvent)) {
			openRegexWizard();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_uploadTraceDialog, aEvent)) {
			openAWSUploadDialog(AWS.UPLOAD);
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_downloadTraceDialog, aEvent)) {
			openAWSUploadDialog(AWS.DOWNLOAD);
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_ms_uploadTraceDialog, aEvent)
			   || (menuAdder.isMenuSelected(MenuItem.menu_tools_ms_downloadTraceDialog, aEvent))) {
			openATTmsUploadDialog();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_tools_editMetadata, aEvent)) {
			openMetadataDialog(aEvent);
		}
	}

	private void exportHtml() {
		ExportReport exportHtml = new ExportReport(parent, false,
				ResourceBundleHelper.getMessageString("menu.tools.export.error"));
		exportHtml.execute();
	}

	private void exportJson() {
		ExportReport exportJson = new ExportReport(parent, true,
				ResourceBundleHelper.getMessageString("menu.tools.export.error"));
		exportJson.execute();
	}


	private void openPcapAnalysis() {

		// Open PCAP analysis tool - Wireshark most likely
		AROTraceData traceData = ((MainFrame) parent).getController().getTheModel();
		IFileManager fileManager = ContextAware.getAROConfigContext().getBean(IFileManager.class);
		File dir = fileManager.createFile(traceData.getAnalyzerResult().getTraceresult().getTraceDirectory());

		String trafficFile = parent.getTrafficFile();
		File capFile = null;

		if ((capFile = new File(dir, trafficFile)).exists()) {

		} else if ((capFile = new File(dir.getParentFile(), trafficFile)).exists()) {
			dir = dir.getParentFile();
		} else {
			return;
		}

		try {
			Desktop.getDesktop().open(capFile);
		} catch (IllegalArgumentException e) {
			LOG.error("Failed to open traffic file:" + capFile, e);
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(), "Traffic file (" + trafficFile + ") not found");
		} catch (IOException e) {
			LOG.error("Failed to open traffic file:" + capFile, e);
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(), ResourceBundleHelper.getMessageString("menu.tools.error.noPcapApp"));
		}
	}

	private void showNoTraceLoadedError() {
		MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
				ResourceBundleHelper.getMessageString("menu.error.noTraceLoadedMessage"),
				ResourceBundleHelper.getMessageString("menu.error.title"), JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Checks and returns tracePath availability Trace data is present only if trace
	 * path available
	 * 
	 * @param traceData
	 * 
	 */
	private boolean isTracePathEmpty() {
		boolean isTracePathEmpty = false;

		if (((MainFrame) parent).getController() != null) {
			AROTraceData traceData = ((MainFrame) parent).getController().getTheModel();
			if (traceData == null 
					|| traceData.getAnalyzerResult() == null 
					|| traceData.getAnalyzerResult().getTraceresult() == null
					|| traceData.getAnalyzerResult().getTraceresult().getTraceDirectory() == null) {
				isTracePathEmpty = true;
			}
		} else {
			isTracePathEmpty = true;
		}
		return isTracePathEmpty;
	}

	private void openTimeRangeAnalysis() {
		MainFrame mainFrame = ((MainFrame) parent);
		if (mainFrame.getController().getTheModel() != null
				&& mainFrame.getController().getTheModel().getAnalyzerResult() != null) {
			TimeRangeAnalysisDialog timeRangeDialog = new TimeRangeAnalysisDialog(mainFrame.getJFrame(), parent);
			timeRangeDialog.setVisible(true);
		} else {
			showNoTraceLoadedError();
		}
	}

	private void openPrivateDataDialog() {
		PrivateDataDialog privateDataDialog = ((MainFrame) parent).getPrivateDataDialog();
		if (privateDataDialog == null) {
			privateDataDialog = new PrivateDataDialog(parent);
		}
		privateDataDialog.setVisible(true);
		privateDataDialog.setAlwaysOnTop(true);

	}

	private void openAWSUploadDialog(AWS awsMode) {
		uploadDialog = new AWSDialog(awsMode);
		uploadDialog.setAWSMode(awsMode);
		uploadDialog.setVisible(true);
		uploadDialog.setAlwaysOnTop(true);
	}

	private void openMetadataDialog(ActionEvent aEvent) {
		new MetadataDialog(parent, (JMenuItem) aEvent.getSource(), null);
	}

	private void openATTmsUploadDialog() {
		try {
			new MSPostDialog();
		} catch (Exception e) {
			LOG.error("Failed to upload trace :", e);
		}
	}

	private void openRegexWizard() {
		RegexWizard regexWizard = RegexWizard.getInstance(((MainFrame) parent).getJFrame());
		if (regexWizard != null) {
			regexWizard.setVisible(true);
		}
	}

	private void clearErrorMessage() {
		try {
			LOG.debug("clearing logcat");
			getDevice();
			String adbPath = adbservice.getAdbPath(true);
			String[] command = new String[] { adbPath, "logcat", "-c" };
			new ProcessBuilder(command).start();
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(), "Logcat clear Successful");
		} catch (IOException e) {
			LOG.error("Logcat clear failed", e);
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(), e.getMessage(),
					getMsg("logcat.clear.failed"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void collectErrorMessage() {
		try {
			LOG.debug("collecting logcat");
			String fileName = String.format(FILE_NAME, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
					LocalTime.now().toSecondOfDay());
			File outFile = new File(Util.getAROTraceDirAndroid(), fileName);
			IDevice device = getDevice();
			String adbPath = adbservice.getAdbPath(true);
			String[] command = new String[] { adbPath, "logcat", "-d", "2", "-t", "5000", "com.att.arocollector:I" };
			ProcessBuilder procBuilder = new ProcessBuilder(command);
			Process process = procBuilder.redirectOutput(outFile).start();
			process.waitFor(6, TimeUnit.SECONDS);
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
					MessageFormat.format(getMsg("logcat.collection.success"), fileName) + ":\n" + device.getName(),
					"Collection Successful", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException | InterruptedException e) {
			LOG.error("Logcat collection failed", e);
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(), e.getMessage(),
					getMsg("logcat.collection.failed"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private IDevice getDevice() throws IOException {
		IDevice[] devices = adbservice.getConnectedDevices();
		if (devices.length == 0) {
			throw new IOException(getMsg("logcat.no.device"));
		} else if (devices.length > 1) {
			throw new IOException(getMsg("logcat.multiple.devices"));
		}
		IDevice device = devices[0];
		return device;
	}

	private String getMsg(String token) {
		return ResourceBundleHelper.getMessageString(token);
	}
}