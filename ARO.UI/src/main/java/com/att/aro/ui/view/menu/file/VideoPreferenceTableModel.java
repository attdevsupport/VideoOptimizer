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
package com.att.aro.ui.view.menu.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoPreferenceTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private VideoUsagePrefs videoUsagePrefs;
	private ObjectMapper mapper;
	private PreferenceHandlerImpl prefs;
	public String validationError = "";
	public boolean isDisable = true;
	public static final Logger LOGGER = LogManager.getLogger(BPVideoWarnFailPanel.class.getName());
	public static final int BP_NAME_COLUMN = 0;
	public static final int WARNING_COLUMN = 1;
	public static final int FAILURE_COLUMN = 2;
	public static final int STARTUP_DELAY_ROW = 0;
	public static final int STALL_DURATION_ROW = 1;
	public static final int SEGMENT_REDUNDANCY_ROW = 2;
	private static final int DECIMAL_POS = 4;
	String[] columnNames = { "Best Practice", "Warning", "Fail" };
	List<VideoPreferenceInfo> videoPreferenceList = new ArrayList<>();
	private static int MAXSTARTUPDELAY = 50;
	private static int MAXSTALLDURATION = 10;
	private static int MAXREDUNDANCY = 100;
	String segmentRedundancyWarnVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultSegmentRedundancyWarnVal");
	String startUpDelayWarnVal = ResourceBundleHelper.getMessageString("preferences.video.defaultStartUpDelayWarnVal");
	String stallDurationWarnVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultStallDurationWarnVal");
	String startUpDelayFailVal = ResourceBundleHelper.getMessageString("preferences.video.defaultStartUpDelayFailVal");
	String stallDurationFailVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultStallDurationFailVal");
	String segmentRedundancyFailVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultSegmentRedundancyFailVal");

 	VideoPreferenceTableModel(){}
 	
	VideoPreferenceTableModel(Collection<VideoPreferenceInfo> videoPreferences) {
		setData(videoPreferences);
	}

	public void setData(Collection<VideoPreferenceInfo> videoPreferences) {
		for (VideoPreferenceInfo vp : videoPreferences) {
			this.videoPreferenceList.add(vp);
		}
	}

	public List<VideoPreferenceInfo> getVideoPreferenceCollection() {
		return videoPreferenceList;
	}

	@Override
	public int getRowCount() {
		return this.videoPreferenceList.size();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object value = "";
		VideoPreferenceInfo videoPreference = this.videoPreferenceList.get(rowIndex);
		if (rowIndex != SEGMENT_REDUNDANCY_ROW) {
			switch (columnIndex) {
			case BP_NAME_COLUMN:
				value = videoPreference.getBestPractice();
				break;
			case WARNING_COLUMN:
				value = videoPreference.getWarningCriteria();
				break;
			case FAILURE_COLUMN:
				value = videoPreference.getFailCriteria();
				break;
			}
		} else {
			switch (columnIndex) {
			case BP_NAME_COLUMN:
				value = videoPreference.getBestPractice();
				break;
			case WARNING_COLUMN:
				value = videoPreference.getWarningCriteriaInt();
				break;
			case FAILURE_COLUMN:
				value = videoPreference.getFailCriteriaInt();
				break;
			}
		}
		return value;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		boolean isCellEditable = false;
		if (col != 0) {
			isCellEditable = true;
		}
		return isCellEditable;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		VideoPreferenceInfo videoPref = videoPreferenceList.get(row);
		boolean isValid = validateInput(value, row, col, videoPref);
		if (isValid) {
			if (row != SEGMENT_REDUNDANCY_ROW) {
				switch (col) {
				case WARNING_COLUMN:
					videoPref.setWarningCriteria((formatValue((String) value, row, col, DECIMAL_POS)));
					break;
				case FAILURE_COLUMN:
					videoPref.setFailCriteria((formatValue((String) value, row, col, DECIMAL_POS)));
					break;
				}
			} else {
				switch (col) {
				case WARNING_COLUMN:
					videoPref.setWarningCriteriaInt(Integer.parseInt((String) value));
					break;
				case FAILURE_COLUMN:
					videoPref.setFailCriteriaInt(Integer.parseInt((String) value));
					break;
				}
			}
			updatePreferenceAndTable(row, videoPref);
		}
		fireTableRowsUpdated(row, row);
	}

	private String formatValue(String value, int row, int col, int defaultValue) {
		String formattedValue = value;
		int afterDecimalDigits = value.length() - value.indexOf('.') - 1;
		if (afterDecimalDigits > defaultValue) {
			String sError = getCellInfo(row, col) + "is limited to 4 decimal positions";
			setValidationError(sError);
			formattedValue = value.substring(0, (value.indexOf('.') + defaultValue + 1));
		}
		return String.format("%.04f", Double.parseDouble(formattedValue));
	}

	private String getCellInfo(int row, int col) {
		String cellinfo = "";
		if (row == STARTUP_DELAY_ROW) {
			if (col == WARNING_COLUMN) {
				cellinfo = "Startup Delay warning value ";
			} else {
				cellinfo = "Startup Delay failure value ";
			}
		} else if (row == STALL_DURATION_ROW) {
			if (col == WARNING_COLUMN) {
				cellinfo = "Stall Duration warning value ";
			} else {
				cellinfo = "Stall Duration failure value ";
			}
		} else if (row == SEGMENT_REDUNDANCY_ROW) {
			if (col == WARNING_COLUMN) {
				cellinfo = "Segment Redundancy warning value ";
			} else {
				cellinfo = "Segment Redundancy failure value ";
			}
		}
		return cellinfo;
	}

	private boolean validateInput(Object value, int row, int col, VideoPreferenceInfo videoPref) {
		boolean isValid = true;
		setValidationError("");
		if (col == WARNING_COLUMN || col == FAILURE_COLUMN) {
			isValid = isNumeric(value, row, col);
			if (isValid) {
				isValid = isPositive(value, row, col);
			}
			if (isValid) {
				isValid = isMax(value, row, col);
			}
			if (isValid) {
				isValid = (row != SEGMENT_REDUNDANCY_ROW) ? checkWarnFailValidation(value, row, col, videoPref)
						: checkWarnFailIntValidation(value, row, col, videoPref);
			}
		}
		return isValid;
	}

	private boolean isPositive(Object value, int row, int col) {
		boolean isPositive = true;
		if (Double.parseDouble((String) value) < 0) {
			isPositive = false;
		}
		if (isPositive) {
			setValidationError("");
		} else {
			setValidationError(getCellInfo(row, col) + "should contain only positive values");
		}
		return isPositive;
	}

	public boolean getDisableStatus() {
		return isDisable;
	}

	private boolean isMax(Object value, int row, int col) {
		String maxError = "";
		boolean isLessthanMax = true;
		if (row == STARTUP_DELAY_ROW) {
			if (Double.parseDouble((String) value) > MAXSTARTUPDELAY) {
				isLessthanMax = false;
				maxError = getCellInfo(row, col) + String.valueOf(MAXSTARTUPDELAY);
			}
		} else if (row == STALL_DURATION_ROW) {
			if (Double.parseDouble((String) value) > MAXSTALLDURATION) {
				isLessthanMax = false;
				maxError = getCellInfo(row, col) + String.valueOf(MAXSTALLDURATION);
			}
		} else if (row == SEGMENT_REDUNDANCY_ROW) {
			if (Integer.parseInt((String) value) > MAXREDUNDANCY) {
				isLessthanMax = false;
				maxError = getCellInfo(row, col) + String.valueOf(MAXREDUNDANCY);
			}
		}
		if (isLessthanMax) {
			setValidationError("");
		} else {
			setValidationError(maxError);
		}
		return isLessthanMax;
	}

	private boolean checkWarnFailValidation(Object value, int row, int col, VideoPreferenceInfo videoPref) {
		String valError = "";
		boolean isValid = true;
		if (col == WARNING_COLUMN) {
			if (Double.parseDouble(videoPref.getFailCriteria()) <= Double.parseDouble((String) value)) {
				valError = getCellInfo(row, col) + "should be less than failure value " + videoPref.getFailCriteria();
				isValid = false;
			}
		} else if (col == FAILURE_COLUMN) {
			if (Double.parseDouble(videoPref.getWarningCriteria()) >= Double.parseDouble((String) value)) {
				valError = getCellInfo(row, col) + "should be greater than warning value "
						+ videoPref.getWarningCriteria();
				isValid = false;
			}
		}
		if (isValid) {
			setValidationError("");
		} else {
			setValidationError(valError);
		}
		return isValid;
	}

	private boolean checkWarnFailIntValidation(Object value, int row, int col, VideoPreferenceInfo videoPref) {
		String intValError = "";
		boolean isValid = true;
		if (col == WARNING_COLUMN) {
			if (videoPref.getFailCriteriaInt() <= Integer.parseInt((String) value)) {
				intValError = getCellInfo(row, col) + "should be less than failure value "
						+ Integer.toString(videoPref.getFailCriteriaInt());
				isValid = false;
			}
		} else if (col == FAILURE_COLUMN) {
			if (videoPref.getWarningCriteriaInt() >= Integer.parseInt((String) value)) {
				intValError = getCellInfo(row, col) + "should be greater than warning value "
						+ Integer.toString(videoPref.getWarningCriteriaInt());
				isValid = false;
			}
		}
		if (isValid) {
			setValidationError("");
		} else {
			setValidationError(intValError);
		}
		return isValid;
	}

	private boolean isNumeric(Object value, int row, int col) {
		boolean isNumeric = true;
		if (row != SEGMENT_REDUNDANCY_ROW) {
			try {
				Double.parseDouble((String) value);
			} catch (Exception e) {
				isNumeric = false;
			}
		} else {
			try {
				Integer.parseInt((String) value);
			} catch (Exception e) {
				isNumeric = false;
			}
		}
		if (!isNumeric) {
			String numericError = "";
			numericError = getCellInfo(row, col);
			if (row != SEGMENT_REDUNDANCY_ROW) {
				numericError = numericError + "should contain only numeric floating values";
			} else {
				numericError = numericError + "should contain only numeric integer values";
			}
			setValidationError(numericError);
		} else {
			setValidationError("");
		}
		return isNumeric;
	}

	private boolean updatePreferenceAndTable(int row, VideoPreferenceInfo videoPref) {
		mapper = new ObjectMapper();
		prefs = PreferenceHandlerImpl.getInstance();
		String temp = "";
		if (videoUsagePrefs == null) {
			temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
			if (temp != null && !temp.equals("null")) {
				try {
					videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
				} catch (IOException e) {
					LOGGER.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
				}
			} else {
				try {
					videoUsagePrefs = ContextAware.getAROConfigContext().getBean("videoUsagePrefs",VideoUsagePrefs.class);
					temp = mapper.writeValueAsString(videoUsagePrefs);
					prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
				} catch (IOException e) {
					LOGGER.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
				}
			}
		}
		switch (row) {
		case STARTUP_DELAY_ROW: // StartUp DeSTARTUP_DELAY_ROWay
			videoUsagePrefs.setStartUpDelayWarnVal(videoPref.getWarningCriteria());
			videoUsagePrefs.setStartUpDelayFailVal(videoPref.getFailCriteria());
			break;
		case STALL_DURATION_ROW: // Stall Duration
			videoUsagePrefs.setStallDurationWarnVal(videoPref.getWarningCriteria());
			videoUsagePrefs.setStallDurationFailVal(videoPref.getFailCriteria());
			break;
		case SEGMENT_REDUNDANCY_ROW: // Segment Redundancy
			videoUsagePrefs.setSegmentRedundancyWarnVal(videoPref.getWarningCriteriaInt());
			videoUsagePrefs.setSegmentRedundancyFailVal(videoPref.getFailCriteriaInt());
			break;
		}
		// prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
		videoPreferenceList.set(row, videoPref);
		return true;
	}

	public VideoUsagePrefs getVideoUsagePrefs() {
		return videoUsagePrefs;
	}

	public String getValidationError() {
		return validationError;
	}

	public void setValidationError(String validationError) {
		this.validationError = validationError;
	}

	public void setDefault() {
		setValidationError("");
		for (int row = STARTUP_DELAY_ROW; row <= SEGMENT_REDUNDANCY_ROW; row++) {
			VideoPreferenceInfo videoPref = videoPreferenceList.get(row);
			if (row != SEGMENT_REDUNDANCY_ROW) {
				videoPref.setWarningCriteria(getValue(row, WARNING_COLUMN));
				videoPref.setFailCriteria(getValue(row, FAILURE_COLUMN));
			} else {
				videoPref.setWarningCriteriaInt(Integer.parseInt(getValue(row, WARNING_COLUMN)));
				videoPref.setFailCriteriaInt(Integer.parseInt(getValue(row, FAILURE_COLUMN)));
			}
			updatePreferenceAndTable(row, videoPref);
			fireTableRowsUpdated(row, row);
		}
	}

	public List<VideoPreferenceInfo> getDefaultValues() {
		List<VideoPreferenceInfo> videoPreferenceList = new ArrayList<>();
		VideoPreferenceInfo videoPref;
		videoPref = new VideoPreferenceInfo("Startup Delay (seconds)");
		videoPref.setWarningCriteria(getValue(STARTUP_DELAY_ROW, WARNING_COLUMN));
		videoPref.setFailCriteria(getValue(STARTUP_DELAY_ROW, FAILURE_COLUMN));
		videoPreferenceList.add(videoPref);

		videoPref = new VideoPreferenceInfo("Stall Duration (seconds)");
		videoPref.setWarningCriteria(getValue(STALL_DURATION_ROW, WARNING_COLUMN));
		videoPref.setFailCriteria(getValue(STALL_DURATION_ROW, FAILURE_COLUMN));
		videoPreferenceList.add(videoPref);

		videoPref = new VideoPreferenceInfo("Segment Redundancy (%)");
		videoPref.setWarningCriteriaInt(Integer.parseInt(getValue(SEGMENT_REDUNDANCY_ROW, WARNING_COLUMN)));
		videoPref.setFailCriteriaInt(Integer.parseInt(getValue(SEGMENT_REDUNDANCY_ROW, FAILURE_COLUMN)));
		videoPreferenceList.add(videoPref);
		return videoPreferenceList;
	}
	
	private String getValue(int row, int column) {
		String value = "";
		if (column == WARNING_COLUMN) {
			switch (row) {
			case STARTUP_DELAY_ROW: // StartUp DeSTARTUP_DELAY_ROWay
				value = startUpDelayWarnVal;
				break;
			case STALL_DURATION_ROW: // Stall Duration
				value = stallDurationWarnVal;
				break;
			case SEGMENT_REDUNDANCY_ROW: // Segment Redundancy
				value = segmentRedundancyWarnVal;
				break;
			}
		} else if (column == FAILURE_COLUMN) {
			switch (row) {
			case STARTUP_DELAY_ROW: // StartUp DeSTARTUP_DELAY_ROWay
				value = startUpDelayFailVal;
				break;
			case STALL_DURATION_ROW: // Stall Duration
				value = stallDurationFailVal;
				break;
			case SEGMENT_REDUNDANCY_ROW: // Segment Redundancy
				value = segmentRedundancyFailVal;
				break;
			}
		}
		return value;
	}
}