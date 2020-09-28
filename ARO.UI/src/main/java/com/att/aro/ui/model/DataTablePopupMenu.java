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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.att.aro.ui.model.listener.DefaultMenuItemListener;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Represents the default popup menu for a data table in the ARO Data Analyzer.<br><br>
 *
 * After creating an  object of this class, users are required to call method {@see #initialize()} to associate menuitems to pop up menu.
 * By default, the pop up menu associates a default export item to the menu whose behavior is to export either all or selected rows from the datatable.<br><br>
 *
 * Users can also use {@see #DataTablePopupMenu(DataTable, List)} constructor or {@see #setMenuItems(List)} to provide their custom menu items to the pop up menu.
 *
 *  @author arpitbansal ab090c
 *
 */
public class DataTablePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = 1L;

	private DataTable<?> table;

	private List<JMenuItem> menuItems;

	/**
	 * Initializes a new instance of the DataTablePopupMenu class using the specified DataTable object.
	 * A default export menu item is added to pop up menu with a behavior of exporting all the rows and columns in xls, xlsx or csv format.
	 *
	 * @param table The DataTable to associate with the DataTablePopupMenu.
	 */
	public DataTablePopupMenu(DataTable<?> table) {
		this.table = table;
		
		// Add default table export menu item to pop up
		menuItems = new ArrayList<>();
		menuItems.add(getExportMenuItem());
	}

	/**
	 * Initializes a new instance of the DataTablePopupMenu class using the specified DataTable object and menu items.
	 *
	 * @param table The DataTable to associate with the DataTablePopupMenu
	 * @param menuItems Menu items to be added to the pop up menu
	 */
	public DataTablePopupMenu(DataTable<?> table, List<JMenuItem> menuItems) {
	    this.table = table;
	    this.menuItems = menuItems;
	}

	/**
	 * Initializes the menu items on the current pop up menu
	 */
	public void initialize() {
	    if (menuItems != null) {
	        menuItems.stream()
	                 .filter(item -> item != null)
                     .forEach(item -> this.add(item));
	    }
	}

	public List<JMenuItem> getMenuItems() {
	    return menuItems;
	}

	public void setMenuItems(List<JMenuItem> menuItems) {
        this.menuItems = menuItems;
    }

	/**
	 * Adds additional menu items
	 * @param items
	 */
	public void addMenuItems(List<JMenuItem> items) {
        if (items != null) {
            if (menuItems == null) {
                menuItems = new ArrayList<>();
            }

            items.stream()
                 .filter(item -> item != null)
                 .forEach(item -> menuItems.add(item));
        }
    }

	private JMenuItem getExportMenuItem() {
	    JMenuItem exportMenuItem = new JMenuItem(ResourceBundleHelper.getMessageString("table.export"));
	    exportMenuItem.addActionListener(new DefaultMenuItemListener(table));
	    return exportMenuItem;
	}
}
