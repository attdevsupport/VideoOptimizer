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
package com.att.aro.ui.view.menu.datacollector;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.ui.model.DataTableModel;

/**
 *
 *
 */
public class DeviceTableModel extends DataTableModel<IAroDevice> {

	private static final long serialVersionUID = 1L;

	private static final int STATE_COL = 0;
	private static final int OS_COL = 1;
	private static final int API_COL = 2;
	private static final int MODEL_COL = 3;
	private static final int DEV_NAME_COL = 4;

	private static final int STATE_COL_MIN = 70;
	private static final int STATE_COL_MAX = 100;
	private static final int STATE_COL_PREF = 70;

	private static final int OS_COL_MIN = 50;
	private static final int OS_COL_MAX = 100;
	private static final int OS_COL_PREF = 60;

	private static final int API_COL_MIN = 50;
	private static final int API_COL_MAX = 50;
	private static final int API_COL_PREF = 50;

	private static final int MODEL_COL_MIN = 100;
	private static final int MODEL_COL_MAX = 150;
	private static final int MODEL_COL_PREF = 150;

	private static final int DEV_NAME_COL_MIN = 50;
	private static final int DEV_NAME_COL_MAX = 270;
	private static final int DEV_NAME_COL_PREF = 270;

	private static final String[] columns = { 
		// ResourceBundleHelper.getMessageString("textFileCompression.table.col1")
			"State" 
			, "OS" 
			, "API"
			, "Model"
			, "Device Name" };

	/**
	 * Initializes a new instance of the CacheAnalysisTableModel class.
	 */
	public DeviceTableModel() {
		super(columns);
	}

	/**
	 * Returns a TableColumnModel that is based on the default table column
	 * model for the DataTableModel class. The TableColumnModel returned by this
	 * method has the same number of columns in the same order and structure as
	 * the table column model in the DataTableModel. When a DataTable object is
	 * created, this method is used to create the TableColumnModel if one is not
	 * specified. This method may be overridden in order to provide
	 * customizations to the default column model, such as providing a default
	 * column width and/or adding column renderers and editors.
	 * 
	 * @return A TableColumnModel object.
	 */
	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		TableColumnModel cols = super.createDefaultTableColumnModel();
		TableColumn col;

		col = cols.getColumn(STATE_COL);
		col.setMinWidth(STATE_COL_MIN);
		col.setPreferredWidth(STATE_COL_PREF);
		col.setMaxWidth(STATE_COL_MAX);

		col = cols.getColumn(OS_COL);
		col.setMinWidth(OS_COL_MIN);
		col.setPreferredWidth(OS_COL_PREF);
		col.setMaxWidth(OS_COL_MAX);

		col = cols.getColumn(API_COL);
		col.setMinWidth(API_COL_MIN);
		col.setPreferredWidth(API_COL_PREF);
		col.setMaxWidth(API_COL_MAX);

		col = cols.getColumn(MODEL_COL);
		col.setMinWidth(MODEL_COL_MIN);
		col.setPreferredWidth(MODEL_COL_PREF);
		col.setMaxWidth(MODEL_COL_MAX);

		col = cols.getColumn(DEV_NAME_COL);
		col.setMinWidth(DEV_NAME_COL_MIN);
		col.setPreferredWidth(DEV_NAME_COL_PREF);
		col.setMaxWidth(DEV_NAME_COL_MAX);

		return cols;
	}

	/**
	 * Returns a class representing the specified column. This method is
	 * primarily used to sort numeric columns.
	 * 
	 * @param columnIndex
	 *            The index of the specified column.
	 * 
	 * @return A class representing the specified column.
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case STATE_COL:
		case API_COL:
			return Double.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	/**
	 * This is the one method that must be implemented by subclasses. This
	 * method defines how the data object managed by this table model is mapped
	 * to its columns when displayed in a row of the table. The getValueAt()
	 * method uses this method to retrieve table cell data.
	 * 
	 * @param item
	 *            An object containing the column information. columnIndex The
	 *            index of the specified column.
	 *
	 * @return The table column value calculated for the object.
	 */
	@Override
	protected Object getColumnValue(IAroDevice item, int columnIndex) {
		switch (columnIndex) {
		case STATE_COL:
			return item.getState().equals(IAroDevice.AroDeviceState.Unknown)?"":item.getState();
		case OS_COL:
			return item.getOS();
		case API_COL:
			return item.getApi();
		case MODEL_COL:
			return item.getModel();
		case DEV_NAME_COL:
			return item.getDeviceName();
		default:
			return null;
		}
	}

}
