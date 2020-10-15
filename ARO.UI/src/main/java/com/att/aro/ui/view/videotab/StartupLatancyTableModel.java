package com.att.aro.ui.view.videotab;

import java.text.DecimalFormat;

import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class StartupLatancyTableModel extends DataTableModel<VideoEvent> {

	private static final long serialVersionUID = 1L;
		
	public static final String[] COLUMNS = { ResourceBundleHelper.getMessageString("video.userevent.segment"),
			ResourceBundleHelper.getMessageString("video.userevent.dlStartTime"),
			ResourceBundleHelper.getMessageString("video.userevent.dlStartDelay"),
			ResourceBundleHelper.getMessageString("video.userevent.playBackTime"),
			ResourceBundleHelper.getMessageString("video.userevent.playBackDelay") };

	public static final int SEGMENT_NO = 0;
	private static final int DL_START_TIME = 1;
	private static final int DL_START_DELAY = 2;
	private static final int PLAYBACK_TIME = 3;
	private static final int PLAYBACK_DELAY = 4;
	double playRequestedTime = 0.0;

	/**
	 * Initializes a new instance of the ImageSizeTableModel.
	 */
	public StartupLatancyTableModel() {
		super(COLUMNS);
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
	protected Object getColumnValue(VideoEvent videoEvent, int columnIndex) {
		playRequestedTime = playRequestedTime == 0.0 ? videoEvent.getPlayRequestedTime() : playRequestedTime;
		switch (columnIndex) {
		case SEGMENT_NO:
			return Integer.valueOf(String.format("%.0f", videoEvent.getSegmentID()));
		case DL_START_TIME:
			return formatDouble(videoEvent.getDLTimeStamp());
		case DL_START_DELAY:
			return formatDouble(videoEvent.getDLTimeStamp() - playRequestedTime);
		case PLAYBACK_TIME:
			return formatDouble(Double.valueOf(String.format("%.6f", videoEvent.getPlayTime())));
		case PLAYBACK_DELAY:
			return formatDouble(Double.valueOf(String.format("%.6f", videoEvent.getPlayTime())) - playRequestedTime);
		default:
			return 0.0;
		}
	}
	
	public static String formatDouble(double toFormat) {
		DecimalFormat decimalFormatter = new DecimalFormat("#.###");
		return decimalFormatter.format(toFormat);	
	}

}