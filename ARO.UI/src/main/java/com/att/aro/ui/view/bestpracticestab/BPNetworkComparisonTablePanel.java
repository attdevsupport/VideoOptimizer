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

	@Override
	public DataTable<SegmentComparison> getContentTable() {
		if (contentTable == null) {
			contentTable = new DataTable<SegmentComparison>(tableModel);
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
