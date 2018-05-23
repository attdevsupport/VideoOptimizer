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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.pojo.CollectorStatus;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.impl.LoggerImpl;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.mobiledevice.pojo.IAroDevices;
import com.att.aro.core.util.NetworkUtil;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.commonui.IosPasswordDialog;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;

/**
 *
 *
 */
public class ARODataCollectorMenu implements ActionListener , MenuListener{

	LoggerImpl log = new LoggerImpl(this.getClass().getName());
	private static final String SharedNetIF= "bridge100";

	private IFileManager fileManager = new FileManagerImpl();

	private JMenu dataCollectorMenu;
	private SharedAttributesProcesses parent;
	private JMenuItem dataCollectorStartMenuItem;
	private JMenuItem dataCollectorStopMenuItem;

	private String menuItemDatacollectorStart = ResourceBundleHelper.getMessageString("menu.datacollector.start");
	private String menuItemDatacollectorStop = ResourceBundleHelper.getMessageString("menu.datacollector.stop");

	/**
	 * @wbp.parser.entryPoint
	 */
	public ARODataCollectorMenu(SharedAttributesProcesses parent){
		super();
		this.parent = parent;
	}

	/**
	 * @return the dataCollectorMenu
	 */
	public JMenu getMenu() {
		
		if(dataCollectorMenu == null){
			dataCollectorMenu = new JMenu(ResourceBundleHelper.getMessageString("menu.datacollector"));
			dataCollectorMenu.setMnemonic(KeyEvent.VK_UNDEFINED);
			
			dataCollectorMenu.addActionListener(this);
			dataCollectorMenu.addMenuListener(this);
			
			dataCollectorMenu.add(getJdataCollectorStart());
			dataCollectorMenu.add(getJdataCollectorStop());
		}
		setStartMenuItem(true);
		return dataCollectorMenu;
	}

	private JMenuItem getJdataCollectorStart(){
		dataCollectorStartMenuItem = getMenuItemInstance();
		dataCollectorStartMenuItem.setText(menuItemDatacollectorStart);
		dataCollectorStartMenuItem.addActionListener(this);
		return dataCollectorStartMenuItem;
	}
	
	private JMenuItem getJdataCollectorStop(){
		dataCollectorStopMenuItem = getMenuItemInstance();
		dataCollectorStopMenuItem.setText(menuItemDatacollectorStop);
		dataCollectorStopMenuItem.addActionListener(this);
		return dataCollectorStopMenuItem;
	}
	
	private JMenuItem getMenuItemInstance(){
		return new JMenuItem();
	}

	@Override
	public void actionPerformed(ActionEvent aEvent) {
		
		if(aEvent.getActionCommand().equalsIgnoreCase(menuItemDatacollectorStart)){
			
			Object event = aEvent.getSource();
			if (event instanceof JMenuItem){

				List<IDataCollector> collectors = parent.getAvailableCollectors();
				
				if (collectors == null || collectors.isEmpty()){
					MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame()
							, ResourceBundleHelper.getMessageString("collector.nocollectors")
							, ResourceBundleHelper.getMessageString("menu.error.title")
							, JOptionPane.ERROR_MESSAGE);
							return;
				}
				
				log.info("collector count:" + collectors.size());
				for (IDataCollector collector : collectors) {
					log.info(collector.getName());
				}
				
				IAroDevices aroDevices = parent.getAroDevices();
				IAroDevice device = null;
				
				if (aroDevices.size() != 0) {
					device = chooseDevice(aroDevices, collectors);
				} else {
					MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
							ResourceBundleHelper.getMessageString("collector.nodevices"),
							ResourceBundleHelper.getMessageString("menu.error.title"), JOptionPane.INFORMATION_MESSAGE);
					return;

				}
				
				if (device == null) {
					MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame()
							, ResourceBundleHelper.getMessageString("collector.cancelled")
							, ResourceBundleHelper.getMessageString("menu.info.title")
							, JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
			}
			
		} else if (aEvent.getActionCommand().equalsIgnoreCase(menuItemDatacollectorStop)) {
			((MainFrame) parent).stopCollector();
			setStartMenuItem(true);
		}
	}

	private IAroDevice chooseDevice(IAroDevices aroDevices, List<IDataCollector> collectors) {

		ArrayList<IAroDevice> deviceList = aroDevices.getDeviceList();

		IAroDevice device = null;

 		int delayTimeDL = 0;
		int throttleDL = 0;
		int throttleUL = 0;
		boolean throttleDLEnable = false;
		boolean throttleULEnable = false;
		boolean secure = false;
		boolean installCert = false;
		boolean profileBoolean = false;
		
		String traceFolderName = "";
		String profileLocation = "";
		DataCollectorSelectNStartDialog dialog = new DataCollectorSelectNStartDialog(((MainFrame) parent).getJFrame(), deviceList, traceFolderName, collectors, true);

		if (dialog.getResponse()){
			
			device = dialog.getDevice();
			traceFolderName = dialog.getTraceFolder();
			device.setCollector(dialog.getCollectorOption());
  			/*debug purpose*/
			delayTimeDL = dialog.getDeviceOptionPanel().getMiniAtnr().getDelayDS();
			dialog.getDeviceOptionPanel().getMiniAtnr().getDelayUS();
			throttleDL = dialog.getDeviceOptionPanel().getMiniAtnr().getThrottleDL();
			throttleUL = dialog.getDeviceOptionPanel().getMiniAtnr().getThrottleUL();
			throttleDLEnable = dialog.getDeviceOptionPanel().getMiniAtnr().isThrottleDLEnable();
			throttleULEnable = dialog.getDeviceOptionPanel().getMiniAtnr().isThrottleULEnable();
			profileLocation = dialog.getDeviceOptionPanel().getMiniAtnr().getLocalPath();
			profileBoolean = dialog.getDeviceOptionPanel().getMiniAtnr().isLoadProfile();			
			log.info("set U delay: "+ delayTimeDL + "set D delay: "+ delayTimeDL 
					+ "set U throttle: "+ throttleUL + "set D throttle: "+ throttleDL 
					+ "set profile: " + profileBoolean+ "set profileLocation: "+ profileLocation);
			
			if (device.isPlatform(IAroDevice.Platform.iOS)) {
				IDataCollector iosCollector = findIOSCollector(collectors);
				
				if((throttleDLEnable||throttleULEnable) && !NetworkUtil.isNetworkUp(SharedNetIF)) {
 					 MessageDialogFactory.getInstance().showInformationDialog(
								((MainFrame) parent).getJFrame(),
								ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.attenuation.finalwarning")	,							
								ResourceBundleHelper.getMessageString("dlog.collector.option.attenuator.attenuation.noshared" ));
 					 return null;
				}
				
				if (!checkSetSuPassword(iosCollector)) {
					return null;
				}else{
					log.info("pw validated");
				}				
			}

			String traceFolderPath = (device.getPlatform().equals(IAroDevice.Platform.Android))
					?Util.getAROTraceDirAndroid() + System.getProperty("file.separator") + traceFolderName
					:Util.getAROTraceDirIOS()     + System.getProperty("file.separator") + traceFolderName;

			String currentPath = ((MainFrame) parent).getTracePath();

			if (fileManager.directoryExistAndNotEmpty(traceFolderPath)) {
				int result = folderExistsDialog();
				if (result == JOptionPane.OK_OPTION) {
					if (traceFolderPath.equals(currentPath)) {
						new MessageDialogFactory().showErrorDialog(null, ResourceBundleHelper.getMessageString("viewer.contentUnwritable"));

						return null;
					}
					fileManager.deleteFolderContents(traceFolderPath);
				} else {
					return null;
				}
			}

 			Hashtable<String,Object> extras = new Hashtable<String,Object>();
			extras.put("video_option", dialog.getRecordVideoOption());
			extras.put("videoOrientation", dialog.getVideoOrientation());
			extras.put("AttenuatorModel", dialog.getDeviceOptionPanel().getMiniAtnr());
			if (secure) {
				extras.put("secure", secure);
				extras.put("installCert", installCert);
			}

			((MainFrame) parent).startCollector(device, traceFolderName, extras);
			
		}else {
			traceFolderName = null;
		}
		
		dialog.dispose();

		return device;
	}
	
	
	/**
	 * Check su password ask for password if empty
	 * only valid for iOS and Mac OSX
	 * 
	 * @param iosCollector
	 * @return 
	 */
	private boolean checkSetSuPassword(IDataCollector iosCollector){
		boolean validated = false;
		if (iosCollector.getPassword().isEmpty()) {
			String password = "invalid";
			String hint = "";
			do {
				password = requestPassword(hint);
				if (password == null){
					return false;
				}
				if (iosCollector.setPassword(password)){
					validated = true;
					break;
				} else {
					hint = "invalid";
				}
			} while (true);
		} else {
			validated = true;
		}
		return validated;
	}
	

	/**
	 * ask user for a password
	 * @param hint 
	 * @return
	 */
	private String requestPassword(String hint) {
		IosPasswordDialog dialog = new IosPasswordDialog(((MainFrame) parent).getJFrame(), hint);
		return dialog.getPassword();
	}
	
	private IDataCollector findIOSCollector(List<IDataCollector> collectors) {
		if (Util.isMacOS()) {
			for (IDataCollector collector : collectors) {
				if (collector.getType().equals(DataCollectorType.IOS)) {
					return collector;
				}
			}
		}
		return null;
	}
	
	/**
	 * Check for prior existence of trace folder. Generate dialog to ask about reuse, if it does exist.
	 * @return 
	 */
	private int folderExistsDialog() {
		MessageDialogFactory.getInstance();
		String mssg = ResourceBundleHelper.getMessageString("Error.tracedirexists");
		String title = MessageFormat.format(ResourceBundleHelper.getMessageString("aro.title.short"), 
											ApplicationConfig.getInstance().getAppShortName());
		int dialogResults = MessageDialogFactory.showConfirmDialog(((MainFrame) parent).getJFrame(), mssg, title, JOptionPane.OK_CANCEL_OPTION);
		log.info("replace directory :"+dialogResults);
		return dialogResults;
	}

	/**
	 * Controls state of the dataCollectorStartMenuItem and dataCollectorStopMenuItem.
	 * Only one or the other should be active.
	 * 
	 * @param active
	 */
	public void setStartMenuItem(boolean active) {
		log.debug(active?"set start":"set stop");
		dataCollectorStartMenuItem.setEnabled(active);
		dataCollectorStopMenuItem.setEnabled(!active);
	}

	@Override
	public void menuSelected(MenuEvent e) {
		CollectorStatus collectorStatus = parent.getCollectorStatus();
		setStartMenuItem(collectorStatus == null || collectorStatus.equals(CollectorStatus.STOPPED) || collectorStatus.equals(CollectorStatus.CANCELLED));
	}

	@Override
	public void menuDeselected(MenuEvent e) {

	}

	@Override
	public void menuCanceled(MenuEvent e) {

	}
}
