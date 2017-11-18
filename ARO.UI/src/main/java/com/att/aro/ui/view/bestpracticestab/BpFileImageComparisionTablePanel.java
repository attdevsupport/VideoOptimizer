package com.att.aro.ui.view.bestpracticestab;

import java.awt.Color;
import java.util.Collection;

import javax.swing.JTable;

import com.att.aro.core.bestpractice.pojo.ImageMdataEntry;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.model.ImageComparatorDataTable;
import com.att.aro.ui.model.bestpractice.ImageComparatorTableModel;



public class BpFileImageComparisionTablePanel extends AbstractBpImageComparatorTablePanel {

	private static final long serialVersionUID = 1L;

	int noOfRecords;
	
	public BpFileImageComparisionTablePanel() {
		super();
		
	}
	
	@Override
	void initTableModel() {
		tableModel = new ImageComparatorTableModel();
	}
	
	/**
	 * Sets the data for the Duplicate Content table.
	 * 
	 * @param data
	 *            - The data to be displayed in the Duplicate Content table.
	 */
	public void setData(Collection<ImageMdataEntry> data) {
		
		setVisible(!data.isEmpty());

		setScrollSize(MINIMUM_ROWS);
		((ImageComparatorTableModel)tableModel).setData(data);
		autoSetZoomBtn();
	}

	/**
	 * Initializes and returns the RequestResponseTable.
	 */
	@SuppressWarnings("unchecked")
	public ImageComparatorDataTable<ImageMdataEntry> getContentTable() {
		if (contentTable == null) {
			contentTable = new ImageComparatorDataTable<ImageMdataEntry>(tableModel);
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		}

		return contentTable;
	}

	@Override
	public void refresh(AROTraceData analyzerResult) {
		
	}

}
