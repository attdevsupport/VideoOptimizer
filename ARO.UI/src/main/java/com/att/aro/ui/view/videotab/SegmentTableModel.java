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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.packetanalysis.pojo.Termination;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class SegmentTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	DecimalFormat decimalFormat = new DecimalFormat("0.##");
	DecimalFormat decimal3Format = new DecimalFormat("0.###");
	DecimalFormat decimal6Format = new DecimalFormat("0.######");

    Formatter fmt = new Formatter();
	
	List<VideoEvent> videoEventList;
	
	public enum colNames {
		 SegmentNo        { public String toString() { return "Segment"    ;} }
		,Content       
		,DLStartTime      { public String toString() { return "DL Start Time"  ;} }
		,DLEndTime        { public String toString() { return "DL End Time"    ;} }
		,StallTime
		,PlayTime
		,StartTime
		,TrackQuality     { public String toString() { return "Track"  ;} }
		,Resolution
		,Bitrate
		,TotalBytes       { public String toString() { return "Total Bytes"    ;} }
		,Duration
		,TCPSession       { public String toString() { return "TCP Session"    ;} }
		,TCPState         { public String toString() { return "TCP State"      ;} }
		,SessionLink 
	}

	public String[] columnNames = {
			 colNames.SegmentNo     .toString() 
			,colNames.Content       .toString()
			,colNames.DLStartTime   .toString()
			,colNames.DLEndTime     .toString()
			,colNames.StallTime     .toString()
			,colNames.PlayTime      .toString()
			,colNames.StartTime     .toString()
			,colNames.TrackQuality  .toString()
			,colNames.Resolution    .toString()
			,colNames.Bitrate       .toString()
			,colNames.TotalBytes    .toString()
			,colNames.Duration      .toString()
			,colNames.TCPSession    .toString()
			,colNames.TCPState      .toString()
			,colNames.SessionLink   .toString()
			};

	public VideoEvent getEventAt(int rowIndex) {
		return this.videoEventList.get(rowIndex);
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object value = "";
		VideoEvent videoSegment = this.videoEventList.get(rowIndex);
		
		switch (columnNames[columnIndex]) {
		case "Segment"       : value = String.format("%.0f", videoSegment.getSegmentID()); break;
		case "Content"       : value = videoSegment.getSegmentInfo().getContentType().toString() ; break;
		case "DL Start Time" : value = String.format("%.3f", videoSegment.getStartTS()); break;
		case "DL End Time"   : value = String.format("%.3f", videoSegment.getEndTS()); break;
		case "StallTime"     : value = String.format("%.3f", videoSegment.getStallTime()); break;
		case "PlayTime"      : value = videoSegment.isSelected() ? String.format("%.6f", videoSegment.getPlayTime()) : " -   "; break;
		case "StartTime"     : value = String.format("%.6f", videoSegment.getSegmentStartTime()); break;
		case "Track"         : value = videoSegment.getQuality(); break;
		case "Resolution"    : value = videoSegment.getResolutionHeight() != 0 ? decimalFormat.format(videoSegment.getResolutionHeight()):"NA "; break;
		case "Bitrate"       : value = String.format("%8.0f", videoSegment.getBitrate()); break;
		case "Total Bytes"   : value = String.format("%.0f", videoSegment.getTotalBytes()); break;
		case "Duration"      : value = String.format("%.6f", videoSegment.getDuration()); break;
		case "TCP Session"   : value = String.format("%06.3f", videoSegment.getSession().getSessionStartTime()); break;
		case "TCP State"     : value = findTermination(videoSegment); break;
		case "SessionLink"   : value = videoSegment.getSession(); break;
		
		}
		return value;
	}


	SegmentTableModel(Collection<VideoEvent> videoEventList) {
		this.videoEventList = new ArrayList<>();
		for (VideoEvent event : videoEventList) {
			this.videoEventList.add(event);
		}
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
			case "AF": value = "fin";break;
			case "AR": value = "RST"; break;
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