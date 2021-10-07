/*
 *  Copyright 2021 AT&T
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
package com.att.aro.ui.model.video;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoRequestTableModel extends DataTableModel<HttpRequestResponseInfo> {

	private static final long serialVersionUID = 1L;
	private static final String[] COLUMN_NAMES = {
			ResourceBundleHelper.getMessageString("videotab.videorequest.table.time"),
			ResourceBundleHelper.getMessageString("videotab.videorequest.table.url") };

	private static final int TIME_COL = 0;
	private static final int REQUEST_URL_COL = 1;

	public VideoRequestTableModel() {
		super(COLUMN_NAMES);
	}

	@Override
	protected Object getColumnValue(HttpRequestResponseInfo item, int columnIndex) {
		if (columnIndex == TIME_COL) {
			return item != null ? String.format("%6.3f", item.getTimeStamp()) : "";
		} else if (columnIndex == REQUEST_URL_COL) {
			return item != null ? item.getObjUri().toString() : "";
		}
		return "";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case TIME_COL:
			return String.class;
		case REQUEST_URL_COL:
			return String.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

}
