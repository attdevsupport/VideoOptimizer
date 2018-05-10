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
package com.att.aro.ui.model.bestpractice;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.att.aro.core.bestpractice.pojo.MultipleConnectionsEntry;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class SimultnsConnTableModel extends DataTableModel<MultipleConnectionsEntry> {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final int COL1_MIN = 90;
	private static final int COL1_MAX = 200;
	private static final int COL1_PREF = 150;
	private static final int COL2_MIN = 130;
	private static final int COL2_MAX = 240;
	private static final int COL2_PREF = 90;
	private static final int COL3_MIN = 150;
	private static final int COL3_MAX = 260;
	private static final int COL3_PREF = 110;
	private static final int COL4_MIN = 90;
	private static final int COL4_PREF = 350;
	public static final int COL_1 = 0;
	public static final int COL_2 = 1;
	public static final int COL_3 = 2;
	public static final int COL_4 = 3;
	private static final String[] COLUMNS = {
			ResourceBundleHelper.getMessageString("connections.simultaneous.table.col1"),
			ResourceBundleHelper.getMessageString("connections.simultaneous.table.col2"),
			ResourceBundleHelper.getMessageString("connections.simultaneous.table.col3"),
			ResourceBundleHelper.getMessageString("connections.simultaneous.table.col4"), };

	public SimultnsConnTableModel() {
		super(COLUMNS);
	}

	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		TableColumnModel cols = super.createDefaultTableColumnModel();
		TableColumn col;
		col = cols.getColumn(COL_1);
		col.setMinWidth(COL1_MIN);
		col.setPreferredWidth(COL1_PREF);
		col.setMaxWidth(COL1_MAX);
		col = cols.getColumn(COL_2);
		col.setMinWidth(COL2_MIN);
		col.setPreferredWidth(COL2_PREF);
		col.setMaxWidth(COL2_MAX);
		col = cols.getColumn(COL_3);
		col.setMinWidth(COL3_MIN);
		col.setPreferredWidth(COL3_PREF);
		col.setMaxWidth(COL3_MAX);
		col = cols.getColumn(COL_4);
		col.setMinWidth(COL4_MIN);
		col.setPreferredWidth(COL4_PREF);
		return cols;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return Double.class;
		case COL_2:
		case COL_3:
		case COL_4:
			return int.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	@Override
	protected Object getColumnValue(MultipleConnectionsEntry item, int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return item.getTimeStamp();
		case COL_2:
			return item.getIpValue();
		case COL_3:
			return item.getHostName();
		case COL_4:
			return item.getConcurrentSessions();
		default:
			return null;
		}
	}
}
