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

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.att.aro.core.videoanalysis.pojo.QualityTime;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class AdaptiveBitrateTableModel extends DataTableModel<QualityTime> {
	private static final long serialVersionUID = 1L;

	private static final int COL1_MIN = 100;
	private static final int COL1_MAX = 400;
	private static final int COL1_PREF = 100;

	private static final int COL2_MIN = 100;
	private static final int COL2_MAX = 400;
	private static final int COL2_PREF = 100;

	private static final int COL3_MIN = 100;
	private static final int COL3_MAX = 400;
	private static final int COL3_PREF = 100;
	
	private static final int COL4_MIN = 100;
	private static final int COL4_MAX = 400;
	private static final int COL4_PREF = 100;
	
	private static final int COL5_MIN = 100;
	private static final int COL5_MAX = 400;
	private static final int COL5_PREF = 100;
	
	private static final int COL6_MIN = 100;
	private static final int COL6_MAX = 400;
	private static final int COL6_PREF = 100;
	
	private static final int COL7_MIN = 100;
	private static final int COL7_MAX = 400;
	private static final int COL7_PREF = 100;
	
	private static final int COL8_MIN = 100;
	private static final int COL8_MAX = 400;
	private static final int COL8_PREF = 100;

	public static final int COL_1 = 0;
	public static final int COL_2 = 1;
	public static final int COL_3 = 2;
	public static final int COL_4 = 3;
	public static final int COL_5 = 4;
	public static final int COL_6 = 5;
	public static final int COL_7 = 6;
	public static final int COL_8 = 7;
	
	private static final String[] COLUMNS = {
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col1"),     //	track           
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col2"),     //	resolution      
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col3"),     //	playback %     
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col4"),     //	segment count  
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col5"),     //	durationTotal   
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col6"),     //	durationTotal   
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col7"),     //	bitrateDeclared 
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col8")};    //	bitrateAverage  

	public AdaptiveBitrateTableModel() {
		super(COLUMNS);
	}

	TableColumnModel cols;
	TableColumn col;
	
	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		cols = super.createDefaultTableColumnModel();
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		col = cols.getColumn(COL_1);
		col.setCellRenderer(centerRenderer);
		col.setMinWidth(COL1_MIN);
		col.setPreferredWidth(COL1_PREF);
		col.setMaxWidth(COL1_MAX);

		col = cols.getColumn(COL_2);
		col.setCellRenderer(centerRenderer);
		col.setMinWidth(COL2_MIN);
		col.setPreferredWidth(COL2_PREF);
		col.setMaxWidth(COL2_MAX);

		col = cols.getColumn(COL_3);
		col.setCellRenderer(rightRenderer);
		col.setMinWidth(COL3_MIN);
		col.setPreferredWidth(COL3_PREF);
		col.setMaxWidth(COL3_MAX);

		col = cols.getColumn(COL_4);
		col.setCellRenderer(centerRenderer);
		col.setMinWidth(COL4_MIN);
		col.setPreferredWidth(COL4_PREF);
		col.setMaxWidth(COL4_MAX);

		col = cols.getColumn(COL_5);
		col.setCellRenderer(rightRenderer);
		col.setMinWidth(COL5_MIN);
		col.setPreferredWidth(COL5_PREF);
		col.setMaxWidth(COL5_MAX);

		col = cols.getColumn(COL_6);
		col.setCellRenderer(rightRenderer);
		col.setMinWidth(COL6_MIN);
		col.setPreferredWidth(COL6_PREF);
		col.setMaxWidth(COL6_MAX);

		col = cols.getColumn(COL_7);
		col.setCellRenderer(centerRenderer);
		col.setMinWidth(COL7_MIN);
		col.setPreferredWidth(COL7_PREF);
		col.setMaxWidth(COL7_MAX);

		col = cols.getColumn(COL_8);
		col.setCellRenderer(centerRenderer);
		col.setMinWidth(COL8_MIN);
		col.setPreferredWidth(COL8_PREF);
		col.setMaxWidth(COL8_MAX);

		return cols;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case -1 :
			return String.class;
		case COL_1:
		case COL_2:
		case COL_4:
		case COL_7:
		case COL_8:
//			return Integer.class;
		case COL_3:
		case COL_5:
		case COL_6:
			return Double.class;
		default:
			System.out.println("default");
			return super.getColumnClass(columnIndex);
		}
	}

	@Override
	protected Object getColumnValue(QualityTime item, int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return (int)item.getTrack();
		case COL_2:
			return item.getResolution() != 0 ? (int) item.getResolution() : "NA";
		case COL_3:
			return String.format("%5.2f ", item.getPercentage());
		case COL_4:
			return (int)item.getCount();
		case COL_5:
			return String.format("%.2f ", item.getDuration());
		case COL_6:
			return String.format("%.6f ", item.getSegmentPosition());
		case COL_7:
			return String.format("%.0f ", item.getBitrateDeclared());
		case COL_8:
			return String.format("%.0f ", item.getBitrateAverage());
		default:
			return null;
		}
	}
}
