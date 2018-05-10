/*
 *  Copyright 2018 AT&T
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
package com.att.aro.ui.view.videotab;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class AccordionTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	String[] columnNames = { "Segment No.", "DL Start Time", "DL End Time", "Quality", "Bitrate", "Total Bytes",
			"Duration" };
	List<VideoEvent> videoEventList;

	AccordionTableModel(Collection<VideoEvent> videoEventList) {
		this.videoEventList = new ArrayList<>();
		for (VideoEvent ve : videoEventList) {
			this.videoEventList.add(ve);
		}
	}

	public List<VideoEvent> getVideoEventCollection() {
		return videoEventList;
	}

	@Override
	public int getRowCount() {
		return this.videoEventList.size();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object value = "";
		VideoEvent videoSegment = this.videoEventList.get(rowIndex);
		switch (columnIndex) {
		case 0:
			value = String.format("%.0f", videoSegment.getSegment());
			break;
		case 1:
			value = String.format("%.3f", videoSegment.getStartTS());
			break;
		case 2:
			value = String.format("%.3f", videoSegment.getEndTS());
			break;
		case 3:
			value = videoSegment.getQuality();
			break;
		case 4:
			value = String.format("%.0f", videoSegment.getBitrate());
			break;
		case 5:
			value = String.format("%.0f", videoSegment.getTotalBytes());
			break;
		case 6:
			DecimalFormat decimalFormat = new DecimalFormat("0.##");
			value = decimalFormat.format(videoSegment.getDuration());
			break;
		}
		return value;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

}