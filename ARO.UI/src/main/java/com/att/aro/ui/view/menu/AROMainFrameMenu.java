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

import javax.swing.JMenuBar;

import com.att.aro.ui.view.SharedAttributesProcesses;
/**
 *
 *
 */
public class AROMainFrameMenu {

	SharedAttributesProcesses parent;
	private JMenuBar menuBar;
	private AROFileMenu aFileMenu;
	private AROProfileMenu aProfileMenu;
	private AROToolMenu aToolMenu;
	private AROViewMenu aViewMenu;
	private ARODataCollectorMenu aDataCollectorMenu;
	private AROHelpMenu aHelpMenu;
	
	public AROMainFrameMenu(SharedAttributesProcesses parent){
		super();
		this.parent = parent;
		this.aFileMenu = new AROFileMenu(parent);
		this.aProfileMenu = new AROProfileMenu(parent);
		this.aToolMenu = new AROToolMenu(parent);
		this.aViewMenu = new AROViewMenu(parent);
		this.aDataCollectorMenu = new ARODataCollectorMenu(parent);
		this.aHelpMenu = AROHelpMenu.getInstance();
		this.menuBar = new JMenuBar();
	}
	
	public JMenuBar getAROMainFileMenu(){
		
		if (menuBar.getMenuCount() == 0) {
			menuBar.add(aFileMenu.getMenu());
			menuBar.add(aProfileMenu.getMenu());
			menuBar.add(aToolMenu.getMenu());
			menuBar.add(aViewMenu.getMenu());
			menuBar.add(aDataCollectorMenu.getMenu());
			menuBar.add(aHelpMenu.getMenu());
		} else {
			menuBar.remove(3);
			menuBar.add(aViewMenu.getMenu(), 3);
			menuBar.remove(2);
			menuBar.add(aToolMenu.getMenu(), 2);
		}
		
		return menuBar;
	}
}
