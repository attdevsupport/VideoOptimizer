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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.preferences.UserPreferences;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.core.util.CrashHandler;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.AROMenuAdder;
import com.att.aro.ui.commonui.AROPrintablePanel;
import com.att.aro.ui.commonui.IAROPrintable;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.commonui.TabPanelCommon;
import com.att.aro.ui.exception.AROUIIllegalStateException;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.SharedAttributesProcesses.TabPanels;
import com.att.aro.ui.view.menu.file.ADBPathDialog;
import com.att.aro.ui.view.menu.file.MissingTraceFiles;
import com.att.aro.ui.view.menu.file.PreferencesDialog;

/**
 *
 *
 */
public class AROFileMenu implements ActionListener, MenuListener {
	private static final Logger LOG = LogManager.getLogger(AROFileMenu.class.getSimpleName());	

	ApplicationContext context = SpringContextUtil.getInstance().getContext();

	private IFileManager fileManager = context.getBean(IFileManager.class);
	
	private final AROMenuAdder menuAdder = new AROMenuAdder(this);

	private JMenu fileMenu = null;
	private SharedAttributesProcesses parent;
	private JMenu recentMenu;
	private TabPanelCommon tabPanelCommon = new TabPanelCommon();
	private JMenuItem printItem;
	Map<String, String> recentMenuItems = new LinkedHashMap<>();

	private UserPreferences userPreferences = UserPreferencesFactory.getInstance().create();

	private enum MenuItem {
		menu_file,
		menu_file_open,
		menu_file_pcap,
		menu_file_adb,
		menu_file_pref,
		menu_file_print,
		menu_file_exit,
		error_printer,
		error_printer_notprintable,
		file_missingAlert,
	}

	public AROFileMenu(SharedAttributesProcesses parent){
		super();
		this.parent = parent;
	}
		
	/**
	 * 
	 * @return
	 */
	public JMenu getMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu(ResourceBundleHelper.getMessageString(MenuItem.menu_file));
			fileMenu.setMnemonic(KeyEvent.VK_UNDEFINED);
			fileMenu.addActionListener(this);
			fileMenu.addMenuListener(this);

			fileMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_file_open));
			fileMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_file_pcap));
			recentMenu = menuAdder.getMenuInstance(ResourceBundleHelper.getMessageString("menu.file.recent"));
			recentMenu.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					boolean getRecentMenuItems = false;
					if (recentMenuItems.isEmpty() || recentMenuItems.size() != Util.getRecentOpenMenuItems().size()) {
						getRecentMenuItems = true;
					}
					if (!getRecentMenuItems) {
						Set<String> keyset = Util.getRecentOpenMenuItems().keySet();
						Iterator<String> iterator = keyset.iterator();
						for (String key : recentMenuItems.keySet()) {
							String keyValue = iterator.hasNext() ? iterator.next() : null;
							if (!key.equals(keyValue)) {
								getRecentMenuItems = true;
								break;
							}
						}
					}
					if (getRecentMenuItems) {
						recentMenuItems = Util.getRecentOpenMenuItems();
						recentMenu.removeAll();
						for (Map.Entry<String, String> entry : recentMenuItems.entrySet()) {
							recentMenu.add(menuAdder.getMenuItemInstance(entry.getKey(),
									ResourceBundleHelper.getMessageString("menu.file.recent")));
						}
					}
				}
			});
		
			fileMenu.add(recentMenu);
			fileMenu.addSeparator();
			fileMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_file_pref));
			fileMenu.addSeparator();

			printItem = menuAdder.getMenuItemInstance(MenuItem.menu_file_print);
			TabPanels tabbedPanel = parent.getCurrentTabPanel();
			printItem.setEnabled(tabbedPanel == TabPanels.tab_panel_best_practices
					  || tabbedPanel == TabPanels.tab_panel_video_tab
					  || tabbedPanel == TabPanels.tab_panel_statistics
							  );
			fileMenu.add(printItem);
			fileMenu.addSeparator();

			fileMenu.add(menuAdder.getMenuItemInstance(MenuItem.menu_file_exit));
		}
		return fileMenu;
	}

	/**
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent aEvent) {
		if (menuAdder.isMenuSelected(MenuItem.menu_file_open, aEvent)) {
			openTraceFolder(aEvent, false);
		} else if (menuAdder.isMenuSelected(MenuItem.menu_file_pcap, aEvent)) {
			File tracePath = null;
			Object event = aEvent.getSource();
			if (event instanceof JMenuItem) {
				tracePath = chooseFileOrFolder(JFileChooser.FILES_ONLY, ResourceBundleHelper.getMessageString(MenuItem.menu_file_pcap));
				if (tracePath != null) {
					parent.updateTracePath(tracePath);
					userPreferences.setLastTraceDirectory(tracePath.getParentFile().getParentFile());
					GoogleAnalyticsUtil.getAndIncrementTraceCounter();
				}
			}
		} else if (menuAdder.isMenuSelected(MenuItem.menu_file_pref, aEvent)) {
			new PreferencesDialog(parent, (JMenuItem) aEvent.getSource()).setVisible(true);
		} else if (menuAdder.isMenuSelected(MenuItem.menu_file_adb, aEvent)) {
			new ADBPathDialog(parent).setVisible(true);
		} else if (menuAdder.isMenuSelected(MenuItem.menu_file_print, aEvent)) {
			handlePrint();
		} else if (menuAdder.isMenuSelected(MenuItem.menu_file_exit, aEvent)) {
			parent.dispose();
			System.exit(0);
		} else if (aEvent.getSource() != null) {
			JMenuItem jmenuSource = (JMenuItem) aEvent.getSource();
			if (jmenuSource.getName() != null && jmenuSource.getName().equals(ResourceBundleHelper.getMessageString("menu.file.recent"))) {
				openTraceFolder(aEvent, true);
			}
		}
	}

	private void openTraceFolder(ActionEvent aEvent, boolean isRecent) {
		try {
			File traceFolder = null;
			Object event = aEvent.getSource();
			if (event instanceof JMenuItem) {
				if (!isRecent) {
					traceFolder = chooseFileOrFolder(JFileChooser.DIRECTORIES_ONLY, ResourceBundleHelper.getMessageString(MenuItem.menu_file_open));
					traceFolder = new File(fileManager.deAlias(traceFolder).toString());
				} else {
					traceFolder = new File(recentMenuItems.get(aEvent.getActionCommand()));
				}

				if (traceFolder.isDirectory()) {
					if (!isTrafficFilePresent(traceFolder)) {
						showErrorDialog(ResourceBundleHelper.getMessageString("trafficFile.notFound"));
					} else if (isTraceFolderEmpty(traceFolder)) {
						// has a traffic file, but no other trace files, so not a trace folder, should be opened as a pcap file directly
						showErrorDialog(ResourceBundleHelper.getMessageString("invalid.traceFolder"));
					} else {
						MissingTraceFiles missingTraceFiles = new MissingTraceFiles(traceFolder);
						Set<File> missingFiles = missingTraceFiles.retrieveMissingFiles();
						if (missingFiles.size() > 0) {
							LOG.warn(MessageFormat.format(ResourceBundleHelper.getMessageString(MenuItem.file_missingAlert), missingTraceFiles.formatMissingFiles(missingFiles)));
						}
						parent.updateTracePath(traceFolder);
						userPreferences.setLastTraceDirectory(traceFolder.getParentFile());
						GoogleAnalyticsUtil.getAndIncrementTraceCounter();
					}
				}
			}
		} catch (OutOfMemoryError err) {
			LOG.error(err.getMessage(), err);
			showErrorDialog("Video Optimizer failed to load the trace: Trace is too big to load");
		}
		this.fileMenu.repaint();
		this.fileMenu.updateUI();
	}
	
	private void showErrorDialog(String errorString) {
		MessageDialogFactory.getInstance().showErrorDialog(parent.getFrame(), errorString);
	}

	private boolean isTrafficFilePresent(File traceFolder) {
		File checkPath = new File(traceFolder, TraceDataConst.FileName.PCAP_FILE);
		return checkPath.exists();
	}

	private boolean isTraceFolderEmpty(File traceFolder) {
		boolean isTraceFolderEmpty = true;
		File[] traceFiles;
		if ((traceFiles = traceFolder.listFiles()) != null) {
			if (traceFiles.length > 0) {
				isTraceFolderEmpty = false;
			}
		}
		return isTraceFolderEmpty;
	}

	/**
	 * 
	 * @param mode
	 * @param title
	 * @return
	 */
	private File chooseFileOrFolder(int mode, String title) {
		File tracePath = null;
		// open window to select from workspace/file system
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(mode);
		String defaultDir = userPreferences.getLastTraceDirectory() == null ? System.getProperty("user.home") : userPreferences.getLastTraceDirectory().toString();
		if (parent.getTracePath() != null && parent.getTracePath().lastIndexOf(File.separator) > -1) {
			defaultDir = parent.getTracePath().substring(0, parent.getTracePath().lastIndexOf(File.separator));
		}
		chooser.setCurrentDirectory(new File(defaultDir));

		if (mode == JFileChooser.FILES_ONLY) {
			// chooser.addChoosableFileFilter(pcapfilter);
			FileNameExtensionFilter pcapfilter = new FileNameExtensionFilter("Pcap files (*.cap, *.pcap)", "cap", "pcap");
			chooser.setFileFilter(pcapfilter);
		}
		if (chooser.showOpenDialog(parent.getCurrentTabComponent()) == JFileChooser.APPROVE_OPTION) {
			tracePath = chooser.getSelectedFile();
		}
		return tracePath;
	}

	private void handlePrint() {
		final JComponent currentTabComponent = (JComponent) parent.getCurrentTabComponent();
		if (currentTabComponent instanceof IAROPrintable) {
			final IAROPrintable aroPrintable = (IAROPrintable) currentTabComponent;

			final PrinterJob printJob = PrinterJob.getPrinterJob();
			if (printJob.printDialog()) {

				new Thread(new Runnable() {
					@Override
					public void run() {
						Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
						printJob.setPrintable(new AROPrintablePanel(
								aroPrintable.getPrintablePanel()));
						try {
							printJob.print();
						} catch (PrinterException e) {
							String[] messageWrapper = new String[1];
							messageWrapper[0] = e.getLocalizedMessage();
							new MessageDialogFactory().showErrorDialog(null,
									tabPanelCommon.getText(MenuItem.error_printer,
											messageWrapper));
						}

					}
				}).start();
			}
		} else {
			throw new AROUIIllegalStateException(tabPanelCommon.getText(
					MenuItem.error_printer_notprintable));
		}
	}


	/**
	 * Need to determine whether the print option is enabled or not when menu is opened.
	 */
	@Override
	public void menuSelected(MenuEvent event) {
		TabPanels currentTabPanel = parent.getCurrentTabPanel();
		printItem.setEnabled(currentTabPanel == TabPanels.tab_panel_best_practices ||
				currentTabPanel == TabPanels.tab_panel_statistics);
	}
	@Override
	public void menuDeselected(MenuEvent event) { // Noop
	}
	@Override
	public void menuCanceled(MenuEvent event) { // Noop
	}
}