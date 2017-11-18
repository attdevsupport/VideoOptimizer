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

import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoStallTableModel extends DataTableModel<VideoStall> {
	private static final long serialVersionUID = 1L;

	private static final int COL1_MIN = 200;
	private static final int COL1_PREF = 400;

	private static final int COL2_MIN = 50;
	private static final int COL2_MAX = 65;
	private static final int COL2_PREF = 80;

	private static final int COL3_MIN = 75;
	private static final int COL3_MAX = 90;
	private static final int COL3_PREF = 100;
	
	private static final int COL4_MIN = 75;
	private static final int COL4_MAX = 100;
	private static final int COL4_PREF = 150;
	
	private static final int COL5_MIN = 75;
	private static final int COL5_MAX = 100;
	private static final int COL5_PREF = 150;
	
	private static final int COL6_MIN = 125;
	private static final int COL6_MAX = 130;
	private static final int COL6_PREF = 150;

	private static final int COL_1 = 0;
	public static final int COL_2 = 1;
	public static final int COL_3 = 2;
	public static final int COL_4 = 3;
	public static final int COL_5 = 4;
	public static final int COL_6 = 5;
	

	DecimalFormat decimalFormat = new DecimalFormat("0.##");
	private static final String[] COLUMNS = {
			ResourceBundleHelper.getMessageString("videoStall.table.col1"),
			ResourceBundleHelper.getMessageString("videoStall.table.col2"),
			ResourceBundleHelper.getMessageString("videoStall.table.col3"),
			ResourceBundleHelper.getMessageString("videoStall.table.col4"),
			ResourceBundleHelper.getMessageString("videoStall.table.col5"),
			ResourceBundleHelper.getMessageString("videoStall.table.col6"),};

	public VideoStallTableModel() {
		super(COLUMNS);
	}

	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		TableColumnModel cols = super.createDefaultTableColumnModel();
		TableColumn col;

		col = cols.getColumn(COL_1);
		col.setMinWidth(COL1_MIN);
		col.setPreferredWidth(COL1_PREF);
		//col.setMaxWidth(COL1_MAX);

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
		col.setMaxWidth(COL4_MAX);
		
		col = cols.getColumn(COL_5);
		col.setMinWidth(COL5_MIN);
		col.setPreferredWidth(COL5_PREF);
		col.setMaxWidth(COL5_MAX);
		
		col = cols.getColumn(COL_6);
		col.setMinWidth(COL6_MIN);
		col.setPreferredWidth(COL6_PREF);
		col.setMaxWidth(COL6_MAX);
		
		return cols;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case COL_1 | COL_3 | COL_4 | COL_5 | COL_6:
			return String.class;
		case COL_2:
			return Integer.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	@Override
	protected Object getColumnValue(VideoStall item, int columnIndex) {
		switch (columnIndex) {
		case COL_1:
			return item.getSegmentTryingToPlay().getAroManifest().getVideoName();
		case COL_2:
			return Integer.valueOf(decimalFormat.format(item.getSegmentTryingToPlay().getSegment()));
		case COL_3:
			return decimalFormat.format(item.getStallEndTimeStamp()-item.getStallStartTimeStamp());
		case COL_4:
			return decimalFormat.format(item.getStallStartTimeStamp());
		case COL_5:
			return decimalFormat.format(item.getStallEndTimeStamp());
		case COL_6:
			return item.getStallState();
		default:
			return null;
		}
	}
}
