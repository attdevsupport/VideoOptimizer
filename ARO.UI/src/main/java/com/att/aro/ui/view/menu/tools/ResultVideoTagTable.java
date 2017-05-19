package com.att.aro.ui.view.menu.tools;

import javax.swing.JTable;

import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;

public class ResultVideoTagTable extends JTable {

	private static final long serialVersionUID = 1L;
	private ResultVideoTagTableModel tableModel;

	public ResultVideoTagTable() {
		tableModel = new ResultVideoTagTableModel();
		setModel(tableModel);
	}
	
	public void update(String[] results, VideoDataTags[] videoDataTags){
		tableModel.update(results, videoDataTags);
	}
	
	public VideoDataTags[] getVideoDataTags() {
		return tableModel.getVideoDataTags();
	}

}
