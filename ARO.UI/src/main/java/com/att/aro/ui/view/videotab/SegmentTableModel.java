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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.packetanalysis.pojo.Termination;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class SegmentTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	public static final String SEGMENT_POSITION = ResourceBundleHelper.getMessageString("video.tab.segment.SegmentPosition");
	public static final String PLAYBACK_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.PlayBackTime");
	public static final String STALL_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.StallTime");
	public static final String DURATION = ResourceBundleHelper.getMessageString("video.tab.segment.Duration");
	public static final String DL_END_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.DLEndTime");
	public static final String SEGMENT_NO = ResourceBundleHelper.getMessageString("video.tab.segment.Segment");
	public static final String TCP_SESSION = ResourceBundleHelper.getMessageString("video.tab.segment.TCPSession");
	public static final String TOTAL_BYTES = ResourceBundleHelper.getMessageString("video.tab.segment.TotalBytes");
	public static final String BIT_RATE = ResourceBundleHelper.getMessageString("video.tab.segment.Bitrate");
	public static final String CONTENT = ResourceBundleHelper.getMessageString("video.tab.segment.Content");
	public static final String RESOLUTION = ResourceBundleHelper.getMessageString("video.tab.segment.Resolution");
	public static final String DL_START_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.DLStartTime");
	public static final String SESSION_LINK = ResourceBundleHelper.getMessageString("video.tab.segment.SessionLink");
	public static final String TCP_STATE = ResourceBundleHelper.getMessageString("video.tab.segment.TCPState");
	public static final String TRACK = ResourceBundleHelper.getMessageString("video.tab.segment.Track");
	public static final String CHANNELS = ResourceBundleHelper.getMessageString("video.tab.segment.Channels");
	public static final String STARTUP_DELAY = ResourceBundleHelper.getMessageString("video.userevent.dlStartDelay");
	public static final String PLAYBACK_DELAY= ResourceBundleHelper.getMessageString("video.userevent.playBackDelay");
	
	List<VideoEvent> videoEventList;
	
	public String[] columnNames = {
        SEGMENT_NO,
		TRACK,
		CONTENT,
		CHANNELS,
		RESOLUTION,
		BIT_RATE,
		TOTAL_BYTES,
		SEGMENT_POSITION,
		DURATION,
		DL_START_TIME,
		DL_END_TIME,
		PLAYBACK_TIME,
		STALL_TIME,
		TCP_SESSION,
		TCP_STATE,
		STARTUP_DELAY, 
		PLAYBACK_DELAY,
		SESSION_LINK
	};
	private double playRequestedTime = 0.0;

	public VideoEvent 	EventAt(int rowIndex) {
		return this.videoEventList.get(rowIndex);
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		VideoEvent videoSegment = this.videoEventList.get(rowIndex);
		return
        (
            SEGMENT_NO.equals(columnNames[columnIndex])       ? Integer.valueOf(String.format("%.0f", videoSegment.getSegmentID())) :
            CONTENT.equals(columnNames[columnIndex])          ? videoSegment.getSegmentInfo().getContentType().toString() :
            DL_START_TIME.equals(columnNames[columnIndex])    ? Double.valueOf(String.format("%.3f", videoSegment.getStartTS())) :
            DL_END_TIME.equals(columnNames[columnIndex])      ? Double.valueOf(String.format("%.3f", videoSegment.getEndTS())) :
            STALL_TIME.equals(columnNames[columnIndex])       ? Double.valueOf(String.format("%.3f", videoSegment.getStallTime())) :
            PLAYBACK_TIME.equals(columnNames[columnIndex])    ? (videoSegment.isSelected() && videoSegment.isNormalSegment() ? Double.valueOf(String.format("%.6f", videoSegment.getPlayTime())) : " -   ") :
            SEGMENT_POSITION.equals(columnNames[columnIndex]) ? Double.valueOf(String.format("%.6f", videoSegment.getSegmentStartTime())) :
            TRACK.equals(columnNames[columnIndex])            ? videoSegment.getQuality() :
            RESOLUTION.equals(columnNames[columnIndex])       ? (videoSegment.getResolutionHeight() != 0 ? videoSegment.getResolutionHeight() : "NA ") :
            BIT_RATE.equals(columnNames[columnIndex])         ? Integer.valueOf(String.format("%.0f", calculateBitRate(videoSegment))) :
            TOTAL_BYTES.equals(columnNames[columnIndex])      ? Integer.valueOf(String.format("%.0f", videoSegment.getTotalBytes())) :
            DURATION.equals(columnNames[columnIndex])         ? Double.valueOf(String.format("%.6f", videoSegment.getDuration())) :
            TCP_SESSION.equals(columnNames[columnIndex])      ? Double.valueOf(String.format("%06.3f", videoSegment.getSession().getSessionStartTime())) :
            TCP_STATE.equals(columnNames[columnIndex])        ? findTermination(videoSegment) :
            SESSION_LINK.equals(columnNames[columnIndex])     ? videoSegment.getSession() :
            CHANNELS.equals(columnNames[columnIndex])         ? videoSegment.getChannels() == null ? "NA " : videoSegment.getChannels() :
            STARTUP_DELAY.equals(columnNames[columnIndex])    ? (playRequestedTime != 0.0 ? (String.format("%.3f",(videoSegment.getDLTimeStamp() - playRequestedTime))) : "-") :
            PLAYBACK_DELAY.equals(columnNames[columnIndex])   ? (playRequestedTime != 0.0 ? (String.format("%.3f", (videoSegment.getPlayTime() - playRequestedTime))): "-") :
            ""
        );
	}


	private Object calculateBitRate(VideoEvent videoSegment) {
		return videoSegment.getBitrate();
	}

	SegmentTableModel(Collection<VideoEvent> videoEventList, double playRequestedTime) {
		this.videoEventList = new ArrayList<>();
		for (VideoEvent event : videoEventList) {
			this.videoEventList.add(event);
		}
		this.playRequestedTime = playRequestedTime;
	}

	public List<VideoEvent> getVideoEventCollection() {
		return videoEventList;
	}

	@Override
	public int getRowCount() {
		return this.videoEventList.size();
	}

	public Object findTermination(VideoEvent videoSegment) {
		Object value = "";
		Termination termination;
		if ((termination = videoSegment.getSession().getSessionTermination()) != null) {
			switch (termination.getPacket().getTcpFlagString()) {
			case "AF": case "F": value = "FIN";break;
			case "AR": case "R": value = "RST"; break;
			default: break;
			}
		}
		return value;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}
}