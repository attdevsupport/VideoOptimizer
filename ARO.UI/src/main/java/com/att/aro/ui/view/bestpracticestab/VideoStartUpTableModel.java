package com.att.aro.ui.view.bestpracticestab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.videoanalysis.pojo.VideoStartup;

public class VideoStartUpTableModel extends AbstractTableModel{

	private static final long serialVersionUID = 1L;
	private List<VideoStartup> data = new ArrayList<>();

	public void setData(Collection<VideoStartup> data) {
		this.data.clear();
		if (data != null) {
			this.data.addAll(data);
		}
		fireTableDataChanged();
	}

	public List<VideoStartup> getData() {
		return data;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return null;
	}

	@Override
	public int getColumnCount() {
		return 0;
	}



}
