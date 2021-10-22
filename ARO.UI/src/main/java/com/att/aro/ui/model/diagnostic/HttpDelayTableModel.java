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
package com.att.aro.ui.model.diagnostic;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.collections.CollectionUtils;

import com.att.aro.core.packetanalysis.pojo.HttpDelayInfo;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class HttpDelayTableModel extends DataTableModel<HttpRequestResponseInfo> {

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMNS = { ResourceBundleHelper.getMessageString("httpDelay.requestTime"),
			ResourceBundleHelper.getMessageString("httpDelay.firstPacketTime"),
			ResourceBundleHelper.getMessageString("httpDelay.firstPacketDelay"),
			ResourceBundleHelper.getMessageString("httpDelay.lastPacketTime"),
			ResourceBundleHelper.getMessageString("httpDelay.lastPacketDelay"),
			ResourceBundleHelper.getMessageString("httpDelay.contentLength") };

	private static final int REQ_TIME_COL = 0;
	private static final int FIRST_PACKET_ARRIVAL_COL = 1;
	private static final int FIRST_PACKET_DELAY = 2;
	private static final int LAST_PACKET_ARRIVAL_COL = 3;
	private static final int LAST_PACKET_DELAY = 4;
	private static final int PAYLOAD_COL = 5;

	public void refresh(Session session) {
		if (session.getRequestResponseInfo() != null) {
			List<HttpRequestResponseInfo> requestResponseInfoList = new ArrayList<HttpRequestResponseInfo>();
			for (HttpRequestResponseInfo httpRequestResponseInfo : session.getRequestResponseInfo()) {
				if (httpRequestResponseInfo.getDirection() == HttpDirection.REQUEST && httpRequestResponseInfo.getAssocReqResp() != null) {
					requestResponseInfoList.add(httpRequestResponseInfo);
				}
			}
			if (CollectionUtils.isNotEmpty(requestResponseInfoList)) {
				setData(requestResponseInfoList);
			}
		}

	}

	/**
	 * Initializes a new instance of the RequestResponseDetailsPanel class.
	 */
	public HttpDelayTableModel() {
		super(COLUMNS);

	}

	/**
	 * Returns a TableColumnModel that is based on the default table column model
	 * for the DataTableModel class. The TableColumnModel returned by this method
	 * has the same number of columns in the same order and structure as the table
	 * column model in the DataTableModel. When a DataTable object is created, this
	 * method is used to create the TableColumnModel if one is not specified. This
	 * method may be overridden in order to provide customizations to the default
	 * column model, such as providing a default column width and/or adding column
	 * renderers and editors.
	 * 
	 * @return A TableColumnModel object.
	 */
	@Override
	public TableColumnModel createDefaultTableColumnModel() {
		TableColumnModel cols = super.createDefaultTableColumnModel();
		TableColumn col = cols.getColumn(REQ_TIME_COL);
		col.setCellRenderer(new NumberFormatRenderer(new DecimalFormat("0.000")));
		return cols;
	}

	/**
	 * Returns a class representing the specified column. This method is primarily
	 * used to sort numeric columns.
	 * 
	 * @param columnIndex The index of the specified column.
	 * 
	 * @return A class representing the specified column.
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case REQ_TIME_COL:
			return Double.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	/**
	 * This is the one method that must be implemented by subclasses. This method
	 * defines how the data object managed by this table model is mapped to its
	 * columns when displayed in a row of the table. The getValueAt() method uses
	 * this method to retrieve table cell data.
	 * 
	 * @param item A object containing the column information. columnIndex The index
	 *             of the specified column.
	 * 
	 * @return An object containing the table column value.
	 */

	@Override
	protected Object getColumnValue(HttpRequestResponseInfo item, int columnIndex) {
		if (item.getHttpDelayInfo() != null && item.getDirection() == HttpDirection.REQUEST
				&& item.getAssocReqResp() != null) {
			HttpDelayInfo httpDelayInfo = item.getHttpDelayInfo();
			if (httpDelayInfo.getLastPacketTimeStamp() != null) {
				switch (columnIndex) {
				case REQ_TIME_COL:
					return httpDelayInfo.getRequestTimeStamp();
				case FIRST_PACKET_ARRIVAL_COL:
					return httpDelayInfo.getFirstPacketTimeStamp();
				case FIRST_PACKET_DELAY:
					return httpDelayInfo.getFirstPacketDelay();
				case LAST_PACKET_ARRIVAL_COL:
					return httpDelayInfo.getLastPacketTimeStamp();
				case PAYLOAD_COL:
					return httpDelayInfo.getContentLength();
				case LAST_PACKET_DELAY:
					return httpDelayInfo.getLastPacketDelay();
				}
			}
		}
		return null;
	}

}
