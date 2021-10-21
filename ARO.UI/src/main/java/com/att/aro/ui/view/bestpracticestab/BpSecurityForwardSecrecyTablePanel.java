/*
 *  Copyright 2017 AT&T
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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.Color;
import java.util.Collection;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.att.aro.core.bestpractice.pojo.ForwardSecrecyEntry;
import com.att.aro.core.util.Util;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.model.bestpractice.ForwardSecrecyTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class BpSecurityForwardSecrecyTablePanel extends AbstractBpDetailTablePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public BpSecurityForwardSecrecyTablePanel() {
		super();
	}

	@Override
	void initTableModel() {
		tableModel = new ForwardSecrecyTableModel();
	}
	
	public void setData(Collection<ForwardSecrecyEntry> data) {
		setVisible(!data.isEmpty());
		setScrollSize(MINIMUM_ROWS);
		((ForwardSecrecyTableModel) tableModel).setData(data);
		autoSetZoomBtn();
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataTable<ForwardSecrecyEntry> getContentTable() {
		if(contentTable == null) {
			contentTable = new DataTable<ForwardSecrecyEntry>(tableModel);
			contentTable.setName(ResourceBundleHelper.getMessageString("security.forward.secrecy.tableName"));
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
			contentTable.setRowSorter(sorter);
			sorter.setComparator(0, Util.getDomainSorter());

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) contentTable.getPopup();
            popupMenu.initialize();
		}
		
		return contentTable;
	}

}
