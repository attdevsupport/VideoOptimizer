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

import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.SegmentComparison;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.model.bestpractice.NetworkComparisonTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class BPNetworkComparisonTablePanel extends AbstractBpDetailTablePanel {

	private static final long serialVersionUID = 1L;

	@Override
	void initTableModel() {
		tableModel = new NetworkComparisonTableModel();

	}

	public void setData(Collection<SegmentComparison> data) {
		setVisible(data != null && !data.isEmpty());
		setScrollSize(MINIMUM_ROWS);
		((NetworkComparisonTableModel) tableModel).setData(data);
		autoSetZoomBtn();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public DataTable<SegmentComparison> getContentTable() {
		if (contentTable == null) {
			contentTable = new DataTable<SegmentComparison>(tableModel);
			contentTable.setName(ResourceBundleHelper.getMessageString("video.network.comparison.tableName"));
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
			contentTable.setRowSorter(sorter);
			sorter.setComparator(NetworkComparisonTableModel.COL_TRACK_INDEX, Util.getIntSorter());
			sorter.setComparator(NetworkComparisonTableModel.COL_COUNT_INDEX, Util.getIntSorter());
			sorter.setComparator(NetworkComparisonTableModel.COL_CALCULATED_NETWORK_BITRATE_INDEX, Util.getFloatSorter());
			sorter.setComparator(NetworkComparisonTableModel.COL_DECLARED_MANIFEST_BANDWIDTH_INDEX, Util.getFloatSorter());
			sorter.toggleSortOrder(NetworkComparisonTableModel.COL_TRACK_INDEX);			// set default sort

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) contentTable.getPopup();
            popupMenu.initialize();
		}
		return contentTable;
	}

}
