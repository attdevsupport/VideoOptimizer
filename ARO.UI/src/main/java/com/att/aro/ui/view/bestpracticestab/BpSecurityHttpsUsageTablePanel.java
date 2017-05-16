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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.Color;
import java.util.Collection;

import javax.swing.JTable;

import com.att.aro.core.bestpractice.pojo.HttpsUsageEntry;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.bestpractice.HttpsUsageTableModel;

public class BpSecurityHttpsUsageTablePanel extends AbstractBpDetailTablePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public BpSecurityHttpsUsageTablePanel() {
		super();
	}

	@Override
	void initTableModel() {
		// TODO Auto-generated method stub
		tableModel = new HttpsUsageTableModel();
	}
	
	public void setData(Collection<HttpsUsageEntry> data) {
		if (!data.isEmpty()) {
			setVisible(true);
		} else {
			setVisible(false);
		}
		
		setScrollSize(MINIMUM_ROWS);
		((HttpsUsageTableModel) tableModel).setData(data);
		autoSetZoomBtn();
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataTable<HttpsUsageEntry> getContentTable() {
		// TODO Auto-generated method stub
		if(contentTable == null) {
			contentTable = new DataTable<HttpsUsageEntry>(tableModel);
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		}
		
		return contentTable;
	}

}
