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

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.att.aro.ui.commonui.AROMenuAdder;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.menu.tools.TimeRangeAnalysisDialog;
import com.att.aro.ui.view.menu.view.ChartPlotOptionsDialog;
import com.att.aro.ui.view.menu.view.FilterApplicationsAndIpDialog;
/**
 *
 *
 */
public class AROViewMenu implements ActionListener, MenuListener {
		
	private final AROMenuAdder menuAdder = new AROMenuAdder(this);

	private final SharedAttributesProcesses parent;
	private JMenu viewMenu;
	private ChartPlotOptionsDialog menuViewOptionsDialog;

	private final JCheckBoxMenuItem menuViewVideo = menuAdder.getCheckboxMenuItemInstance(MenuItem.menu_view_video);
	private final JMenuItem menuViewApps = menuAdder.getMenuItemInstance(MenuItem.menu_view_apps);
	private final JMenuItem menuToolsExcludetimerangeanalysis = menuAdder.getMenuItemInstance(MenuItem.menu_tools_excludetimerangeanalysis);
	private final JMenuItem menuViewOptions = menuAdder.getMenuItemInstance(MenuItem.menu_view_options);

	private enum MenuItem {
		menu_view,
		menu_view_video,
		menu_view_apps,
		menu_tools_excludetimerangeanalysis,
		menu_view_options
	}


	public AROViewMenu(SharedAttributesProcesses parent) {
		this.parent = parent;
	}
	

	public JMenu getMenu() {
		if (viewMenu == null) {
			viewMenu = new JMenu(ResourceBundleHelper.getMessageString(MenuItem.menu_view));
			viewMenu.setMnemonic(KeyEvent.VK_UNDEFINED);

			viewMenu.addActionListener(this);
			viewMenu.addMenuListener(this);

			menuViewVideo.setSelected(parent.isVideoPlayerSelected());
			viewMenu.add(menuViewVideo);
			viewMenu.addSeparator();
			viewMenu.add(menuViewApps);
			menuViewApps.setEnabled(parent.isModelPresent());
			viewMenu.add(menuToolsExcludetimerangeanalysis);
			
			menuToolsExcludetimerangeanalysis.setEnabled(parent.isModelPresent());
			viewMenu.addSeparator();
			viewMenu.add(menuViewOptions);
		}
		return viewMenu;
	}

	@Override
	public void actionPerformed(ActionEvent aEvent) {
		if(menuAdder.isMenuSelected(MenuItem.menu_view_video, aEvent)) {
			parent.updateVideoPlayerSelected(!parent.isVideoPlayerSelected());
			menuViewVideo.setSelected(parent.isVideoPlayerSelected());
		} else if(menuAdder.isMenuSelected(MenuItem.menu_view_apps, aEvent)) {
			new FilterApplicationsAndIpDialog(parent).setVisible(true);
		} else if(menuAdder.isMenuSelected(MenuItem.menu_tools_excludetimerangeanalysis, aEvent)) {
			openTimeRangeAnalysis();
		} else if(menuAdder.isMenuSelected(MenuItem.menu_view_options, aEvent)) {
			synchronized(this) {
				if(menuViewOptionsDialog == null) {
					menuViewOptionsDialog = new ChartPlotOptionsDialog(parent, menuViewOptions);
				}
			}
			menuViewOptionsDialog.setVisible(true);
		}
	}

	private void openTimeRangeAnalysis() {
		MainFrame mainFrame = ((MainFrame) parent);
		if (mainFrame.getController().getTheModel() != null
				&& mainFrame.getController().getTheModel().getAnalyzerResult() != null) {
			TimeRangeAnalysisDialog timeRangeDialog = new TimeRangeAnalysisDialog(mainFrame.getJFrame(), parent);
			timeRangeDialog.setVisible(true);
		} else {
			MessageDialogFactory.showMessageDialog(((MainFrame) parent).getJFrame(),
					ResourceBundleHelper.getMessageString("menu.error.noTraceLoadedMessage"),
					ResourceBundleHelper.getMessageString("menu.error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void menuSelected(MenuEvent event) {
		// There is a dependency to having a valid trace path specified
		menuViewVideo.setSelected(parent.isVideoPlayerSelected());
		menuViewApps.setEnabled(parent.isModelPresent());
		menuToolsExcludetimerangeanalysis.setEnabled(parent.isModelPresent());
	}
	
	@Override
	public void menuDeselected(MenuEvent e) { // NoOp
	}
	
	@Override
	public void menuCanceled(MenuEvent e) { // NoOp
	}
}
