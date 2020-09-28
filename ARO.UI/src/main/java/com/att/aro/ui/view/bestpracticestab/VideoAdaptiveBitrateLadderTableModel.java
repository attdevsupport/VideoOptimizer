package com.att.aro.ui.view.bestpracticestab;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.videoanalysis.pojo.QualityTime;

public class VideoAdaptiveBitrateLadderTableModel extends AbstractTableModel{

	private static final long serialVersionUID = 1L;
	private List<QualityTime> data = new ArrayList<>();

	public void setData(List<QualityTime> newData) {
		this.data.clear();
		if (newData != null) {
			this.data.addAll(newData);
		}
		fireTableDataChanged();
	}

	public List<QualityTime> getData() {
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
