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

import java.text.DecimalFormat;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.att.aro.core.bestpractice.pojo.VideoConcurrentSession;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoConcurrentSessionTableModel extends DataTableModel<VideoConcurrentSession> {
	private static final long serialVersionUID = 1L;

	private static final int COL1_MIN = 90;
	private static final int COL1_PREF = 275;

	private static final int COL2_MIN = 90;
	private static final int COL2_MAX = 200;
	private static final int COL2_PREF = 175;

	private static final int COL3_MIN = 90;
	private static final int COL3_MAX = 600;
	private static final int COL3_PREF = 350;

	private static final int COL_1 = 0;
	private static final int COL_2 = 1;
	private static final int COL_3 = 2;

	DecimalFormat decimalFormat = new DecimalFormat("0.##");
	private static final String[] COLUMNS = {
			ResourceBundleHelper.getMessageString("videoConcurrentSession.table.col1"),
			ResourceBundleHelper.getMessageString("videoConcurrentSession.table.col2"),
			ResourceBundleHelper.getMessageString("videoConcurrentSession.table.col3"), };

	public VideoConcurrentSessionTableModel() {
		super(COLUMNS);
	}

	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		TableColumnModel cols = super.createDefaultTableColumnModel();
		TableColumn col;

		col = cols.getColumn(COL_1);
		col.setMinWidth(COL1_MIN);
		col.setPreferredWidth(COL1_PREF);

		col = cols.getColumn(COL_2);
		col.setMinWidth(COL2_MIN);
		col.setPreferredWidth(COL2_PREF);
		col.setMaxWidth(COL2_MAX);

		col = cols.getColumn(COL_3);
		col.setMinWidth(COL3_MIN);
		col.setPreferredWidth(COL3_PREF);
		col.setMaxWidth(COL3_MAX);

		return cols;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return String.class;
		case COL_2:
			return int.class;
		case COL_3:
			return String.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	@Override
	protected Object getColumnValue(VideoConcurrentSession item, int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return item.getVideoName();
		case COL_2:
			return item.getConcurrentSessionCount();
		case COL_3:
			return decimalFormat.format(item.getConcurrencyDuration());
		default:
			return null;
		}
	}
}
