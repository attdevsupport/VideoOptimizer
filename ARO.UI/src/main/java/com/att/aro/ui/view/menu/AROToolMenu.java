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
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.android.ddmlib.IDevice;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.AROMenuAdder;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.menu.tools.AWSDialog;
import com.att.aro.ui.view.menu.tools.AWSDialog.AWS;
import com.att.aro.ui.view.menu.tools.ExportReport;
import com.att.aro.ui.view.menu.tools.PrivateDataDialog;
import com.att.aro.ui.view.menu.tools.RegexWizard;
import com.att.aro.ui.view.menu.tools.TimeRangeAnalysisDialog;

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
		menu_tools,
		menu_tools_wireshark,
		menu_tools_timerangeanalysis,
		menu_tools_dataDump,
		menu_tools_htmlExport,
		menu_tools_jsonExport,
		menu_tools_privateData,
		menu_tools_videoAnalysis,
		menu_tools_getErrorMsg,
		menu_tools_clearErrorMsg,
		menu_tools_videoParserWizard,
		menu_tools_uploadTraceDialog,
		menu_tools_downloadTraceDialog,
	}

	public AROToolMenu(SharedAttributesProcesses parent){
		super();
		this.parent = parent;
	}
	
	/**
	 * @return the toolMenu
	 */
	public JMenu getMenu() {
		
		if(toolMenu == null){
			toolMenu = new JMenu(ResourceBundleHelper.getMessageString(MenuItem.menu_tools));
			toolMenu.setMnemonic(KeyEvent.VK_UNDEFINED);
			boolean isTracePathEmpty = true;
			isTracePathEmpty = isTracePathEmpty();
			if (Desktop.isDesktopSupported()) {
				toolMenu.add(getMenuItem(MenuItem.menu_tools_wireshark, isTracePathEmpty));
			}
			toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_timerangeanalysis));
			toolMenu.addSeparator();
			toolMenu.add(getMenuItem(MenuItem.menu_tools_htmlExport,isTracePathEmpty));
			toolMenu.add(getMenuItem(MenuItem.menu_tools_jsonExport,isTracePathEmpty));
			toolMenu.addSeparator();
			toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_privateData));
			toolMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_tools_videoParserWizard));

		}
		return toolMenu;
	}

	public JMenuItem getMenuItem(MenuItem item, boolean isTracePathEmpty) {
		JMenuItem menuItem = menuAdder.getMenuItemInstance(item);
		menuItem.setEnabled(!isTracePathEmpty);
		return menuItem;
	}

	@Override
	public void actionPerformed(ActionEvent aEvent) {
		if(menuAdder.isMenuSelected(MenuItem.menu_tools_wireshark, aEvent)){
			openPcapAnalysis();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_timerangeanalysis, aEvent)){
			openTimeRangeAnalysis();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_htmlExport, aEvent)){
			exportHtml();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_jsonExport, aEvent)){
			exportJson();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_privateData, aEvent)) {
			openPrivateDataDialog();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_getErrorMsg, aEvent)) {
			collectErrorMessage();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_clearErrorMsg, aEvent)) {
			clearErrorMessage();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_videoParserWizard, aEvent)){
			openRegexWizard();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_uploadTraceDialog, aEvent)){
			openAWSUploadDialog(AWS.UPLOAD);
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_downloadTraceDialog, aEvent)){
			openAWSUploadDialog(AWS.DOWNLOAD);
		}
	}

	private void exportHtml(){
		ExportReport exportHtml = new ExportReport(parent, false, ResourceBundleHelper.getMessageString("menu.tools.export.error"));
		exportHtml.execute();
	}
	
	private void exportJson(){
		ExportReport exportJson = new ExportReport(parent, true, ResourceBundleHelper.getMessageString("menu.tools.export.error"));
		exportJson.execute();
	}
	
	private void openPcapAnalysis() {

		// Open PCAP analysis tool
		AROTraceData traceData = ((MainFrame) parent).getController().getTheModel();
		IFileManager fileManager = ContextAware.getAROConfigContext().getBean(IFileManager.class);
		File dir = fileManager.createFile(traceData.getAnalyzerResult().getTraceresult().getTraceDirectory());
		File[] trafficFiles;
		if (fileManager.isFile(dir.getAbsolutePath())) {
			trafficFiles = new File[] { new File(dir.getAbsolutePath()) };
		} else {
			trafficFiles = getTrafficTextFiles(dir);

		}
		if (trafficFiles != null && trafficFiles.length > 0) {
			try {
				for (File trafficFile : trafficFiles) {
					if (trafficFile.getName().equals(TraceDataConst.FileName.PCAP_FILE)) {
						Desktop.getDesktop().open(trafficFile);
						break;
					}
				}
			} catch (NullPointerException e) {
				MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
						ResourceBundleHelper.getMessageString("menu.tools.error.noPcap"));
			} catch (IllegalArgumentException e) {
				MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
						ResourceBundleHelper.getMessageString("menu.tools.error.noPcap"));
			} catch (IOException e) {
				MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
						ResourceBundleHelper.getMessageString("menu.tools.error.noPcapApp"));
			}
		}
	}

	private void showNoTraceLoadedError() {
		MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
				ResourceBundleHelper.getMessageString("menu.error.noTraceLoadedMessage"),
				ResourceBundleHelper.getMessageString("menu.error.title"), JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Checks and returns tracePath availability 
	 * Trace data is present only if trace path available
	 * 
	 * @param traceData
	 * 
	 */
	private boolean isTracePathEmpty() {
		boolean isTracePathEmpty = false;
		if (((MainFrame) parent).getController() != null) {
			AROTraceData traceData = ((MainFrame) parent).getController().getTheModel();
			if (traceData == null || traceData.getAnalyzerResult() == null
					|| traceData.getAnalyzerResult().getTraceresult() == null
					|| traceData.getAnalyzerResult().getTraceresult().getTraceDirectory() == null) {
				isTracePathEmpty = true;
			}
		} else {
			isTracePathEmpty = true;
		}
		return isTracePathEmpty;
	}

	/**
	 * Returns a list of cap or pcap files in the specified folder
	 * @param dir
	 * 			the specified folder where to look for pcap/cap files
	 * @return
	 * 			a list of the pcap files found in the specified folder
	 */
	private File[] getTrafficTextFiles(File dir) {
	    return dir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.indexOf("traffic")>-1)
					return true;
				else
					return false;
		    }
	    });
	}
	
	private void openTimeRangeAnalysis() {
		MainFrame mainFrame = ((MainFrame)parent);
		if (mainFrame.getController().getTheModel()!=null && mainFrame.getController().getTheModel().getAnalyzerResult()!=null){
			PacketAnalyzerResult analysisData = mainFrame.getController().getTheModel().getAnalyzerResult();
			TimeRangeAnalysisDialog timeRangeDialog = new TimeRangeAnalysisDialog(mainFrame.getJFrame(), analysisData);
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
		if (privateDataDialog != null) {
			privateDataDialog.setVisible(true);
			privateDataDialog.setAlwaysOnTop(true);
		}
	}
	
	private void openAWSUploadDialog(AWS awsMode) {
		uploadDialog = new AWSDialog(awsMode);
		uploadDialog.setAWSMode(awsMode);
		uploadDialog.setVisible(true);
		uploadDialog.setAlwaysOnTop(true);
	}

	private void openRegexWizard(){
		RegexWizard regexWizard = RegexWizard.getInstance();
		if (regexWizard != null) {
			regexWizard.setVisible(true);
			regexWizard.setAlwaysOnTop(true);
		}
	}

	private void clearErrorMessage() {
		try {
			LOG.debug("clearing logcat");
			getDevice();
			String adbPath = adbservice.getAdbPath();
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
			String adbPath = adbservice.getAdbPath();
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