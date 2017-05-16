package com.att.aro.ui.view.menu.tools;

import java.util.EnumSet;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;

public class ResultVideoTagTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;

	private String[] columnNames = { "Data", "XREF"};
	
	private Object[][] data = new Object[0][2];

	private EnumSet<VideoDataTags> vdt;

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.length;
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	public Class getColumnClass(int col) {
		return getValueAt(0, col).getClass();
	}

	public boolean isCellEditable(int row, int col) {
		if (col < 0) {
			return false;
		} else {
			return true;
		}
	}

	/**<pre>
	 * Column 0 is String data,
	 * Column 1 is VideoDataTag
	 */
	public void setValueAt(Object value, int row, int col) {
		if (col == 1 && value.getClass().equals(String.class)) {
			data[row][col] = getVideoDataTag((String) value);
		} else {
			data[row][col] = value;
		}
		fireTableCellUpdated(row, col);
	}

	/**<pre>
	 * Locate and return VideoDataTag that matches value
	 * This method only called due to user action in a combobox, so efficiency is not of high importance.
	 * 
	 * @param value
	 * @return
	 */
	private VideoDataTags getVideoDataTag(String value) {
		if (vdt == null) {
			vdt = EnumSet.allOf(VideoDataTags.class);
		}
		for (VideoDataTags tag : vdt) {
			if (value.equals(tag.toString())){
				return tag;
			}
		}
		return VideoDataTags.unknown;
	}

	public VideoDataTags[] getVideoDataTags() {
		VideoDataTags[] tags = new VideoDataTags[data.length];
		for (int idx = 0; idx < tags.length; idx++) {
			try {
				tags[idx]=(VideoDataTags) data[idx][1];
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return tags;
	}

	public void update(String[] results, VideoDataTags[] videoDataTags) {
		if (results != null) {
			int resSize = results.length;
			int vdtSize = videoDataTags.length;
			data = new Object[resSize][2];
			for (int idx = 0; idx < results.length; idx++) {
				data[idx][0] = results[idx];
				data[idx][1] = vdtSize > idx ? videoDataTags[idx] : VideoDataTags.unknown;
			}
			fireTableDataChanged();
		} else {
			data = new Object[0][2];
		}
	}

}
