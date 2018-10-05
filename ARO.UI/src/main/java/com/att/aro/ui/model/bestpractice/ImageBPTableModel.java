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

import static java.text.MessageFormat.format;

import java.text.DecimalFormat;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.bestpractice.pojo.ImageMdataEntry;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.model.NumberFormatRenderer;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * Represents the data table model for text Image Size result table. This class
 * implements the aro.commonui.DataTableModel class using ImageSizeEntry
 * objects.
 */
public class ImageBPTableModel extends DataTableModel<ImageMdataEntry> {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LogManager.getLogger(ImageBPTableModel.class.getName());

	private static final int COL1_MIN = 70;
	private static final int COL1_MAX = 100;
	private static final int COL1_PREF = 70;

	private static final int COL2_MIN = 300;
	private static final int COL2_PREF = 300;

	private static final int COL3_MIN = 80;
	private static final int COL3_MAX = 120;
	private static final int COL3_PREF = 80;

	private static final int COL4_MIN = 80;
	private static final int COL4_MAX = 120;
	private static final int COL4_PREF = 70;

	private static final int COL5_MIN = 80;
	private static final int COL5_MAX = 120;
	private static final int COL5_PREF = 80;

	private static final int COL_1 = 0;
	private static final int COL_2 = 1;
	private static final int COL_3 = 2;
	private static final int COL_4 = 3;
	private static final int COL_5 = 4;
	private static final String[] COLUMNS = { ResourceBundleHelper.getMessageString("imageMetadata.table.col1"),
			ResourceBundleHelper.getMessageString("imageMetadata.table.col2"),
			ResourceBundleHelper.getMessageString("imageMetadata.table.col3"),
			ResourceBundleHelper.getMessageString("imageMetadata.table.col4"),
			ResourceBundleHelper.getMessageString("imageMetadata.table.col5") };

	/**
	 * Initializes a new instance of the ImageSizeTableModel.
	 */
	public ImageBPTableModel() {
		super(ImageBPTableModel.COLUMNS);
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
	 * @return TableColumnModel object
	 */
	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		TableColumnModel cols = super.createDefaultTableColumnModel();
		TableColumn col;

		col = cols.getColumn(ImageBPTableModel.COL_1);
		col.setCellRenderer(new NumberFormatRenderer(new DecimalFormat("0.000")));
		col.setMinWidth(ImageBPTableModel.COL1_MIN);
		col.setPreferredWidth(ImageBPTableModel.COL1_PREF);
		col.setMaxWidth(ImageBPTableModel.COL1_MAX);

		col = cols.getColumn(ImageBPTableModel.COL_2);
		col.setMinWidth(ImageBPTableModel.COL2_MIN);
		col.setPreferredWidth(ImageBPTableModel.COL2_PREF);

		col = cols.getColumn(ImageBPTableModel.COL_3);
		col.setMinWidth(ImageBPTableModel.COL3_MIN);
		col.setPreferredWidth(ImageBPTableModel.COL3_PREF);
		col.setMaxWidth(ImageBPTableModel.COL3_MAX);

		col = cols.getColumn(ImageBPTableModel.COL_4);
		col.setMinWidth(ImageBPTableModel.COL4_MIN);
		col.setPreferredWidth(ImageBPTableModel.COL4_PREF);
		col.setMaxWidth(ImageBPTableModel.COL4_MAX);

		col = cols.getColumn(ImageBPTableModel.COL_5);
		col.setMinWidth(ImageBPTableModel.COL5_MIN);
		col.setPreferredWidth(ImageBPTableModel.COL5_PREF);
		col.setMaxWidth(ImageBPTableModel.COL5_MAX);

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
		ImageBPTableModel.LOGGER.debug(format("getColumnClass, idx: {0}", columnIndex));
		switch (columnIndex) {
		case COL_1:
			return Double.class;
		case COL_2:
			return String.class;
		case COL_3:
			return String.class;
		case COL_4:
			return String.class;
		case COL_5:
			return String.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	/**
	 * Defines how the data object managed by this table model is mapped to its
	 * columns when displayed in a row of the table.
	 * 
	 * @param item
	 *            An object containing the column information.
	 * @param columnIndex
	 *            The index of the specified column.
	 * 
	 * @return The table column value calculated for the object.
	 */
	@Override
	protected Object getColumnValue(ImageMdataEntry item, int columnIndex) {
		ImageBPTableModel.LOGGER.trace(format("getColumnValue, idx:{0}", columnIndex));
		switch (columnIndex) {
		case COL_1:
			return item.getTimeStamp();
		case COL_2:
			return item.getHttpObjectName();
		case COL_3:
			return item.getImageSize();
		case COL_4:
			return item.getFormattedSize();
		case COL_5:
			return item.getPercentSavings();
		default:
			return null;
		}
	}

}
