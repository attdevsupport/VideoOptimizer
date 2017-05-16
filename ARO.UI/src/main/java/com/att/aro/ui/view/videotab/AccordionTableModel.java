package com.att.aro.ui.view.videotab;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class AccordionTableModel extends AbstractTableModel {
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
		// TODO Auto-generated method stub
		return this.videoEventList.size();
	}

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return columnNames.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
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