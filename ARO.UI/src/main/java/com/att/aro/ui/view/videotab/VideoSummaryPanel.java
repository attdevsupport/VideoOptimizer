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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoBufferData;
import com.att.aro.core.videotab.pojo.VideoResultSummary;
import com.att.aro.ui.commonui.TabPanelCommon;
import com.att.aro.ui.commonui.TabPanelCommonAttributes;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoSummaryPanel extends TabPanelJPanel{
	
	private enum LabelKeys {
		videoSummary_stalls,
		videoSummary_startUpDelay,
		videoSummary_bufferOccupancy,
		videoSummary_networkComparison,
		videoSummary_tcpConnections,
		videoSummary_segmentSize,
		videoSummary_segmentPacing,
		videoSummary_redundancy,
		videoSummary_duplicate,
		videoSummary_concurrentSessions,
		videoSummary_ipSessions,
		videoSummary_ipAddresses,
		videoSummary_segmentCount,
		videoSummary_mbytesOfMovie,
		videoSummary_mbytesTotal
	}
	
	JTable bufferTable;
	String[][] bufferData;
	private static final long serialVersionUID = 1L;
	DecimalFormat decimalFormat = new DecimalFormat("0.##");
	private final TabPanelCommon tabPanelCommon = new TabPanelCommon();
	private VideoResultSummary videoResultSummary;
	
	public VideoSummaryPanel() {
		tabPanelCommon.initTabPanel(this);
		this.setLayout(new GridBagLayout());

		JLabel titleLabel = getTitle();
		JScrollPane bufferPane = getBufferTable();
		int line = 0;
		add(titleLabel		 , new GridBagConstraints(0, line++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 1, 10, 1), 0, 0));
		add(bufferPane		 , new GridBagConstraints(0, line++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 1, 10, 1), 0, 0));
		add(layoutDataPanel(), new GridBagConstraints(0, line++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 1, 15, 1), 0, 0));
	}

	public JLabel getTitle() {
		JLabel VideoResultSummaryLabel = new JLabel(ResourceBundleHelper.getMessageString("videoSummary.title"));
		VideoResultSummaryLabel.setFont(new Font("HeaderFont", Font.BOLD, 18));
		return VideoResultSummaryLabel;
	}
	
	public JScrollPane getBufferTable(){
		bufferData= new String[2][4];
		bufferData[0][0] = ResourceBundleHelper.getMessageString("videoSummary.buffer.byte");
		bufferData[1][0] = ResourceBundleHelper.getMessageString("videoSummary.buffer.time");
		
		String[] columnNames = { ResourceBundleHelper.getMessageString("videoSummary.buffer.title"),
				ResourceBundleHelper.getMessageString("videoSummary.buffer.average"),
				ResourceBundleHelper.getMessageString("videoSummary.buffer.minimum"),
				ResourceBundleHelper.getMessageString("videoSummary.buffer.maximum") };
		
		bufferTable = new JTable(bufferData, columnNames);
		
		// Table Property settings
		bufferTable.setPreferredScrollableViewportSize(bufferTable.getPreferredSize());
		bufferTable.setFocusable(false);
		bufferTable.setRowSelectionAllowed(false);
		bufferTable.setEnabled(false);
		((DefaultTableCellRenderer)bufferTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
		DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
		cellRenderer.setHorizontalAlignment(JLabel.CENTER);
		bufferTable.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
		bufferTable.setGridColor(Color.LIGHT_GRAY);
		return (new JScrollPane(bufferTable));
	}
	
	public void updateBufferTableData(List<VideoBufferData> bufferDataList) {
		if (bufferDataList == null || bufferDataList.size() == 0) {
			return;
		}

		int rowIdx = 0;
		for (VideoBufferData bf : bufferDataList) {
			int colIdx = 1;
			if (rowIdx == 1 || bf.getBufferType().equals(ResourceBundleHelper.getMessageString("videoSummary.buffer.time"))) {
				rowIdx = 1;
			}
			bufferData[rowIdx][colIdx++] = decimalFormat.format(bf.getAverage());
			bufferData[rowIdx][colIdx++] = decimalFormat.format(bf.getMinimum());
			bufferData[rowIdx][colIdx++] = decimalFormat.format(bf.getMaximum());
		}
	}
	
	@Override
	public JPanel layoutDataPanel() {
		JPanel pane = new JPanel(new BorderLayout());
		
		if (tabPanelCommon == null) {
			return null;
		}
		
		TabPanelCommonAttributes attributes = null;
		for (LabelKeys keys : LabelKeys.values()) {
			if (keys.equals(LabelKeys.videoSummary_stalls)) {
				attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().enumKey(keys).build());
			} else {
				attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(keys).build());
			}
		}
		pane.add(tabPanelCommon.getTabPanel(), BorderLayout.WEST);
		pane.setBackground(Color.WHITE);

		return pane;
	}
	
	private void clearVideoSummary() {
		for (int rowIdx = 0; rowIdx <= 1; rowIdx++) {
			for (int colIdx = 1; colIdx <= 3; colIdx++) {
				bufferData[rowIdx][colIdx] = "";
			}
		}
		for (LabelKeys keys : LabelKeys.values()) {
			tabPanelCommon.setText(keys, "");
		}
	}
	
	public void refreshSummaryInfo(AROTraceData trace) {

		if (null == trace) {
			return;
		}

		videoResultSummary = new VideoResultSummary(trace);
		updateBufferTableData(videoResultSummary.getVideoBufferDataList());

		tabPanelCommon.setText(LabelKeys.videoSummary_stalls, String.valueOf(videoResultSummary.getStalls()));
		if (!videoResultSummary.isStartupDelayStatus()) {
			tabPanelCommon.setText(LabelKeys.videoSummary_startUpDelay
					, ResourceBundleHelper.getMessageString("videoSummary.startupDelayStatus"));
		} else {
			tabPanelCommon.setText(LabelKeys.videoSummary_startUpDelay
					, MessageFormat.format(ResourceBundleHelper.getMessageString("videoSummary.seconds")
							, decimalFormat.format(videoResultSummary.getStartUpDelay())
							, videoResultSummary.getStartUpDelay() == 1 ? "" : "s"));
		}
		
		tabPanelCommon.setText(LabelKeys.videoSummary_bufferOccupancy
				, MessageFormat.format(ResourceBundleHelper.getMessageString("videoSummary.mb")
						, decimalFormat.format(videoResultSummary.getBufferOccupancy())));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_networkComparison
				, MessageFormat.format(ResourceBundleHelper.getMessageString("videoSummary.average.kbps")
						, decimalFormat.format(videoResultSummary.getNtkComparison())));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_tcpConnections
				, String.valueOf(videoResultSummary.getTcpConnection()));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_segmentSize
				, MessageFormat.format(ResourceBundleHelper.getMessageString("videoSummary.kb")
						, decimalFormat.format(videoResultSummary.getSegmentSize())));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_segmentPacing
				, MessageFormat.format(ResourceBundleHelper.getMessageString("videoSummary.seconds")
						, decimalFormat.format(videoResultSummary.getSegmentPacing())
						, videoResultSummary.getSegmentPacing() == 1 ? "" : "s"));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_redundancy
				, String.format("%.0f", videoResultSummary.getRedundancy()) + "%");
		
		tabPanelCommon.setText(LabelKeys.videoSummary_duplicate
				, String.valueOf(videoResultSummary.getDuplicate()));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_concurrentSessions
				, String.valueOf(videoResultSummary.getConcurrentSessions()));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_ipSessions, String.valueOf(videoResultSummary.getIpSessions()));
		tabPanelCommon.setText(LabelKeys.videoSummary_ipAddresses, String.valueOf(videoResultSummary.getIpAddress()));
		tabPanelCommon.setText(LabelKeys.videoSummary_segmentCount, String.valueOf(videoResultSummary.getSegmentCount()));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_mbytesOfMovie
				, MessageFormat.format(ResourceBundleHelper.getMessageString("videoSummary.mb")
						, decimalFormat.format(videoResultSummary.getMovieMBytes())));
		
		tabPanelCommon.setText(LabelKeys.videoSummary_mbytesTotal
				, MessageFormat.format(ResourceBundleHelper.getMessageString("videoSummary.mb")
						, decimalFormat.format(videoResultSummary.getTotalMBytes())));
	}

	@Override
	public void refresh(AROTraceData analyzerResult) {
		clearVideoSummary();
		refreshSummaryInfo(analyzerResult);
	}
}
