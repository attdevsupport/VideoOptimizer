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

import java.math.BigDecimal;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.att.aro.core.bestpractice.pojo.HttpsUsageEntry;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class HttpsUsageTableModel extends DataTableModel<HttpsUsageEntry> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final int COL1_MIN = 90;
	private static final int COL1_MAX = 130;
	private static final int COL1_PREF = 130;
	
	private static final int COL2_MIN = 90;
	private static final int COL2_PREF = 130;
	
	private static final int COL3_MIN = 90;
	private static final int COL3_MAX = 135;
	private static final int COL3_PREF = 135;
	
	private static final int COL4_MIN = 90;
	private static final int COL4_MAX = 135;
	private static final int COL4_PREF = 135;
	
	private static final int COL5_MIN = 90;
	private static final int COL5_MAX = 130;
	private static final int COL5_PREF = 130;
	
	private static final int COL6_MIN = 70;
	private static final int COL6_MAX = 80;
	private static final int COL6_PREF = 80;
	
	private static final int COL7_MIN = 70;
	private static final int COL7_MAX = 80;
	private static final int COL7_PREF = 80;
	
	private static final int COL8_MIN = 70;
	private static final int COL8_MAX = 80;
	private static final int COL8_PREF = 80;
	
	private static final int COL_1 = 0;
	private static final int COL_2 = 1;
	private static final int COL_3 = 2;
	private static final int COL_4 = 3;
	private static final int COL_5 = 4;
	private static final int COL_6 = 5;
	private static final int COL_7 = 6;
	private static final int COL_8 = 7;
	
	private static final String[] COLUMNS = {
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col1"),
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col2"),
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col3"),
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col4"),
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col5"),
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col6"),
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col7"),
			ResourceBundleHelper.getMessageString("security.httpsUsage.table.col8")
	};
	
	public HttpsUsageTableModel() {
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
		
		col = cols.getColumn(COL_3);
		col.setMinWidth(COL3_MIN);
		col.setPreferredWidth(COL3_PREF);
		col.setMaxWidth(COL3_MAX);
	
		col = cols.getColumn(COL_4);
		col.setMinWidth(COL4_MIN);
		col.setPreferredWidth(COL4_PREF);
		col.setMaxWidth(COL4_MAX);
		
		col = cols.getColumn(COL_5);
		col.setMinWidth(COL5_MIN);
		col.setPreferredWidth(COL5_PREF);
		col.setMaxWidth(COL5_MAX);
		
		col = cols.getColumn(COL_6);
		col.setMinWidth(COL6_MIN);
		col.setPreferredWidth(COL6_PREF);
		col.setMaxWidth(COL6_MAX);
		
		col = cols.getColumn(COL_7);
		col.setMinWidth(COL7_MIN);
		col.setPreferredWidth(COL7_PREF);
		col.setMaxWidth(COL7_MAX);
		
		col = cols.getColumn(COL_8);
		col.setMinWidth(COL8_MIN);
		col.setPreferredWidth(COL8_PREF);
		col.setMaxWidth(COL8_MAX);
		
		return cols;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return String.class;
		case COL_2:
			return String.class;
		case COL_3:
			return Integer.class;
		case COL_4:
			return Integer.class;
		case COL_5:
			return BigDecimal.class;
		case COL_6:
			return BigDecimal.class;
		case COL_7:
			return BigDecimal.class;
		case COL_8:
			return BigDecimal.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}
	
	@Override
	protected Object getColumnValue(HttpsUsageEntry item, int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return item.getIPAddress();
		case COL_2:
			return item.getParentDomainName();
		case COL_3:
			return item.getTotalNumHttpConnections();
		case COL_4:
			return item.getTotalNumConnections();
		case COL_5:
			return item.getHttpConnectionsPercentage();
		case COL_6:
			return item.getTotalHttpTrafficInKB();
		case COL_7:
			return item.getTotalTrafficInKB();
		case COL_8:
			return item.getTotalHttpTrafficPercentage();
		default:
			return null;
		}
	}

}
