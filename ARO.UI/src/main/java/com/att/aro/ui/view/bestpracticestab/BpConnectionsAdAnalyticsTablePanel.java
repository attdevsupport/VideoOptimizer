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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.Color;
import java.util.Collection;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.att.aro.core.bestpractice.pojo.MultipleConnectionsEntry;
import com.att.aro.core.util.Util;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.bestpractice.AdAnalyticsTableModel;
import com.att.aro.ui.model.bestpractice.SimultnsConnTableModel;

public class BpConnectionsAdAnalyticsTablePanel extends AbstractBpDetailTablePanel {
	private static final long serialVersionUID = 1L;
	int noOfRecords;

	public BpConnectionsAdAnalyticsTablePanel() {
		super();
	}

	@Override
	void initTableModel() {
		tableModel = new AdAnalyticsTableModel();
	}

	/**
	 * Sets the data for the Duplicate Content table.
	 *
	 * @param data
	 *            - The data to be displayed in the Duplicate Content table.
	 */
	public void setData(Collection<MultipleConnectionsEntry> data) {
		setVisible(!data.isEmpty());
		setScrollSize(MINIMUM_ROWS);
		((AdAnalyticsTableModel) tableModel).setData(data);
		autoSetZoomBtn();
	}

	/**
	 * Initializes and returns the RequestResponseTable.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public DataTable<MultipleConnectionsEntry> getContentTable() {
		if (contentTable == null) {
			contentTable = new DataTable<MultipleConnectionsEntry>(tableModel);
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
			contentTable.setRowSorter(sorter);
			sorter.setComparator(SimultnsConnTableModel.COL_2, Util.getDomainSorter());
			sorter.setComparator(SimultnsConnTableModel.COL_3, Util.getDomainSorter());
			sorter.setComparator(SimultnsConnTableModel.COL_4, Util.getDomainIntSorter());
			sorter.toggleSortOrder(SimultnsConnTableModel.COL_1);
		}
		return contentTable;
	}
}
