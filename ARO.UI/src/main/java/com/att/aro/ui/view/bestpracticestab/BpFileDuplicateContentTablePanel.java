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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.Color;
import java.util.Collection;

import javax.swing.JTable;

import com.att.aro.core.packetanalysis.pojo.CacheEntry;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.model.bestpractice.BPDuplicateContentTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 *
 *
 */
public class BpFileDuplicateContentTablePanel extends AbstractBpDetailTablePanel {
	private static final long serialVersionUID = 1L;
	int noOfRecords;

	public BpFileDuplicateContentTablePanel() {
		super();
	}

	@Override
	void initTableModel() {
		tableModel = new BPDuplicateContentTableModel();
	}

	/**
	 * Sets the data for the Duplicate Content table.
	 *
	 * @param data
	 *            - The data to be displayed in the Duplicate Content table.
	 */
	public void setData(Collection<CacheEntry> data) {
		setVisible(!data.isEmpty());
		setScrollSize(MINIMUM_ROWS);
		((BPDuplicateContentTableModel) tableModel).setData(data);
		autoSetZoomBtn();
	}

	/**
	 * Initializes and returns the RequestResponseTable.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public DataTable<CacheEntry> getContentTable() {
		if (contentTable == null) {
			contentTable = new DataTable<CacheEntry>(tableModel);
			contentTable.setName(ResourceBundleHelper.getMessageString("file.duplicate.content.tableName"));
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) contentTable.getPopup();
            popupMenu.initialize();
		}
		return contentTable;
	}

	@Override
	public void refresh(AROTraceData analyzerResult) {
	}
}
