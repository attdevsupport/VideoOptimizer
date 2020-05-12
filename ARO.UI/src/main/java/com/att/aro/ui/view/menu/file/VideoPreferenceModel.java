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
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VideoPreferenceModel {
	
	private static final int WARNING_COLUMN = 1;
	private static final int FAILURE_COLUMN = 2;
	private static final int STARTUP_DELAY_ROW = 0;
	private static final int STALL_DURATION_ROW = 1;
	private static final int SEGMENT_REDUNDANCY_ROW = 2;
	
	private VideoUsagePrefs videoUsagePrefs;
	private ObjectMapper mapper;
	private PreferenceHandlerImpl prefs;
	private String validationError = "";
	private int componentRowNumber = 0;
	private static final Logger LOGGER = LogManager.getLogger(VideoPreferenceModel.class.getName());
	
	private List<VideoPreferenceInfo> videoPreferenceList = new ArrayList<>();
	private String segmentRedundancyWarnVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultSegmentRedundancyWarnVal");
	private String startUpDelayWarnVal = ResourceBundleHelper.getMessageString("preferences.video.defaultStartUpDelayWarnVal");
	private String stallDurationWarnVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultStallDurationWarnVal");
	private String stallDurationFailVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultStallDurationFailVal");
	private String segmentRedundancyFailVal = ResourceBundleHelper
			.getMessageString("preferences.video.defaultSegmentRedundancyFailVal");

	private String getWarnFailText(int row, int col) {
		String cellinfo = "";
		if (row == STALL_DURATION_ROW) {
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

	public boolean stallDurationValidation(double stallDurationWarn, double stallDurationFail) {
		String valError = "";
		boolean isValid = true;

		if (stallDurationFail <= stallDurationWarn) {
			valError = getWarnFailText(STALL_DURATION_ROW, WARNING_COLUMN) + "should be less than failure value "
					+ stallDurationFail;
			setErrorComponent(STALL_DURATION_ROW);
			isValid = false;
		}
		if (isValid) {
			if (stallDurationWarn >= stallDurationFail) {
				valError = getWarnFailText(STALL_DURATION_ROW, FAILURE_COLUMN) + "should be greater than warning value "
						+ stallDurationWarn;
				setErrorComponent(STALL_DURATION_ROW);
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

	public boolean segmentRedundancyValidation( double segmentRedundancyWarn, double segmentRedundancyFail) {
		String intValError = "";
		boolean isValid = true;

		if (segmentRedundancyFail <= segmentRedundancyWarn) {
			intValError = getWarnFailText(SEGMENT_REDUNDANCY_ROW, WARNING_COLUMN) + "should be less than failure value "
					+ segmentRedundancyFail;
			setErrorComponent(SEGMENT_REDUNDANCY_ROW);
			isValid = false;
		}
		if (isValid) {
			if (segmentRedundancyWarn >= segmentRedundancyFail) {
				intValError = getWarnFailText(SEGMENT_REDUNDANCY_ROW, FAILURE_COLUMN)
						+ "should be greater than warning value "
						+ segmentRedundancyWarn;
				setErrorComponent(SEGMENT_REDUNDANCY_ROW);
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
					videoUsagePrefs = ContextAware.getAROConfigContext().getBean("videoUsagePrefs",
							VideoUsagePrefs.class);
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
	
	public int getErrorComponent() {
		return componentRowNumber;
	}
	
	public void setErrorComponent(int componentRowNumber) {
		this.componentRowNumber = componentRowNumber;
	}

	public void setDefault() {
		setValidationError("");
		for (int row = STARTUP_DELAY_ROW; row <= SEGMENT_REDUNDANCY_ROW; row++) {
			VideoPreferenceInfo videoPref = videoPreferenceList.get(row);
			if (row != SEGMENT_REDUNDANCY_ROW) {
				videoPref.setWarningCriteria(Double.valueOf(getValue(row, WARNING_COLUMN)));
				videoPref.setFailCriteria(Double.valueOf(getValue(row, FAILURE_COLUMN)));
			} else {
				videoPref.setWarningCriteriaInt(Integer.parseInt(getValue(row, WARNING_COLUMN)));
				videoPref.setFailCriteriaInt(Integer.parseInt(getValue(row, FAILURE_COLUMN)));
			}
			updatePreferenceAndTable(row, videoPref);
		}
	}

	public List<VideoPreferenceInfo> getDefaultValues() {
		List<VideoPreferenceInfo> videoPreferenceList = new ArrayList<>();
		VideoPreferenceInfo videoPref;
		videoPref = new VideoPreferenceInfo("Startup Delay (seconds)");
		videoPref.setWarningCriteria(Double.valueOf(getValue(STARTUP_DELAY_ROW, WARNING_COLUMN)));
		videoPref.setFailCriteria(Double.valueOf(getValue(STARTUP_DELAY_ROW, FAILURE_COLUMN)));
		videoPreferenceList.add(videoPref);

		videoPref = new VideoPreferenceInfo("Stall Duration (seconds)");
		videoPref.setWarningCriteria(Double.valueOf(getValue(STALL_DURATION_ROW, WARNING_COLUMN)));
		videoPref.setFailCriteria(Double.valueOf(getValue(STALL_DURATION_ROW, FAILURE_COLUMN)));
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