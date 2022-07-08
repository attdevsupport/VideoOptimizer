/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.ui.model.bestpractice;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.att.aro.core.videoanalysis.pojo.SegmentComparison;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class NetworkComparisonTableModel extends DataTableModel<SegmentComparison> {

	private static final long serialVersionUID = 1L;

	private static final int COL_TRACK_MIN_WIDTH = 90;
	private static final int COL_TRACK_PREF_WIDTH = 150;
	
	private static final int COL_COUNT_MIN_WIDTH = 90;
	private static final int COL_COUNT_PREF_WIDTH = 150;

	private static final int COL_DECLARED_MANIFEST_BANDWIDTH_MIN_WIDTH = 100;//Declared Manifest Bandwidth
	private static final int COL_DECLARED_MANIFEST_BANDWIDTH_PREF_WIDTH = 150;

	private static final int COL_CALCULATED_NETWORK_BITRATE_MIN_WIDTH = 100;//Calculated Network Bitrate
	private static final int COL_CALCULATED_NETWORK_BITRATE_PREF_WIDTH = 350;

	public static final int COL_TRACK_INDEX = 0;
	public static final int COL_COUNT_INDEX = 1;
	public static final int COL_DECLARED_MANIFEST_BANDWIDTH_INDEX = 2;
	public static final int COL_CALCULATED_NETWORK_BITRATE_INDEX = 3;
	

	public NetworkComparisonTableModel() {
		super(COLUMNS);
		// TODO Auto-generated constructor stub
	}

	private static final String[] COLUMNS = { ResourceBundleHelper.getMessageString("networkComparison.trackno"), // track
			ResourceBundleHelper.getMessageString("adaptiveBitrateTable.table.col4"),
			ResourceBundleHelper.getMessageString("networkComparison.declared"), // bitrateDeclared
			ResourceBundleHelper.getMessageString("networkComparison.calculated") // calculate
	};

	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		TableColumn col;
		
		TableColumnModel cols = super.createDefaultTableColumnModel();

		col = cols.getColumn(COL_TRACK_INDEX);
		col.setMinWidth(COL_TRACK_MIN_WIDTH);
		col.setPreferredWidth(COL_TRACK_PREF_WIDTH);

		col = cols.getColumn(COL_COUNT_INDEX);
		col.setMinWidth(COL_COUNT_MIN_WIDTH);
		col.setPreferredWidth(COL_COUNT_PREF_WIDTH);

		col = cols.getColumn(COL_DECLARED_MANIFEST_BANDWIDTH_INDEX);
		col.setMinWidth(COL_DECLARED_MANIFEST_BANDWIDTH_MIN_WIDTH);
		col.setPreferredWidth(COL_DECLARED_MANIFEST_BANDWIDTH_PREF_WIDTH);

		col = cols.getColumn(COL_CALCULATED_NETWORK_BITRATE_INDEX);
		col.setMinWidth(COL_CALCULATED_NETWORK_BITRATE_MIN_WIDTH);
		col.setPreferredWidth(COL_CALCULATED_NETWORK_BITRATE_PREF_WIDTH);

		return cols;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case COL_TRACK_INDEX:
			return Integer.class;
		case COL_COUNT_INDEX:
			return Integer.class;	
		case COL_DECLARED_MANIFEST_BANDWIDTH_INDEX:
			return String.class;
		case COL_CALCULATED_NETWORK_BITRATE_INDEX:
			return String.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	@Override
	protected Object getColumnValue(SegmentComparison item, int columnIndex) {
		switch (columnIndex) {
		case COL_TRACK_INDEX:
			return item.getTrack();
		case COL_COUNT_INDEX:
			return item.getCount();
		case COL_DECLARED_MANIFEST_BANDWIDTH_INDEX:
			return String.format("%.0f ", item.getDeclaredBandwidth());
		case COL_CALCULATED_NETWORK_BITRATE_INDEX:
			return String.format("%.2f ", item.getCalculatedThroughput());
		default:
			return null;
		}
	}

}
