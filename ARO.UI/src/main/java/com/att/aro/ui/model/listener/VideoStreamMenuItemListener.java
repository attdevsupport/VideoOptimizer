/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.ui.model.listener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.export.ExcelSheet;
import com.att.aro.core.export.ExcelWriter;
import com.att.aro.core.util.Util;
import com.att.aro.ui.view.videotab.SegmentTablePanel;
import com.att.aro.ui.view.videotab.SegmentTableModel;


public class VideoStreamMenuItemListener extends AbstractMenuItemListener {
	private static final Logger LOG = LoggerFactory.getLogger(VideoStreamMenuItemListener.class);

	private static final List<String> videoColumnsToSkip = Arrays.asList(SegmentTableModel.CONTENT,
			SegmentTableModel.SESSION_LINK, SegmentTableModel.CHANNELS);
	private static final List<String> audioColumnsToSkip = Arrays.asList(SegmentTableModel.CONTENT,
			SegmentTableModel.SESSION_LINK, SegmentTableModel.RESOLUTION);
	private final List<SegmentTablePanel> segmentTables;

	public VideoStreamMenuItemListener(List<SegmentTablePanel> segmentTables) {
		super(null);
		super.tableName = "video_streams";
		this.segmentTables = segmentTables;
	}


	private List<Integer> getColumnIndicesToSkip(JTable table, List<String> columnsToSkip) {
		List<Integer> columnViewIndices = new ArrayList<>();
		for (String columnName : columnsToSkip) {
			int columnModelIndex = table.getColumn(columnName).getModelIndex();
			columnViewIndices.add(table.convertColumnIndexToView(columnModelIndex));
		}

		return columnViewIndices;
	}

	@Override
	public void writeExcel(File file) throws IOException {
		LOG.info("Starting Excel export data to {}", file.getAbsolutePath());

		List<ExcelSheet> sheets = new ArrayList<>();
		int counter = 1;
		
		for (SegmentTablePanel segmentTable : segmentTables) {
			String sheetName = "Stream" + counter++;
			List<List<Object>> dataRows = new ArrayList<>();
			List<Integer> columnIndicesToSkip;

			dataRows.add(Arrays.asList("Name", segmentTable.getVideoStream().getManifest().getVideoName()));

			// Video table data
			JTable videoTable = segmentTable.getStreamTables().get(SegmentTablePanel.VIDEO_TABLE_NAME);
			if (videoTable != null) {
				// Find column's UI view indices to skip in the report
				// Columns for Video table: CONTENT, SESSION_LINK, CHANNELS
				columnIndicesToSkip = getColumnIndicesToSkip(videoTable, videoColumnsToSkip);

				dataRows.add(Arrays.asList("Video Table"));
				dataRows.add(getHeaderData(videoTable, columnIndicesToSkip, false));
				populateDataRows(videoTable, columnIndicesToSkip, dataRows);

				dataRows.add(Arrays.asList(""));
				dataRows.add(Arrays.asList(""));
				dataRows.add(Arrays.asList(""));
			}

			// Audio table data
			JTable audioTable = segmentTable.getStreamTables().get(SegmentTablePanel.AUDIO_TABLE_NAME);
			if (audioTable != null) {
				// Find column's UI view indices to skip in the report
				// Columns for Audio table: CONTENT, SESSION_LINK, RESOLUTION
				columnIndicesToSkip = getColumnIndicesToSkip(audioTable, audioColumnsToSkip);

				dataRows.add(Arrays.asList("Audio Table"));
				dataRows.add(getHeaderData(audioTable, columnIndicesToSkip, false));
				populateDataRows(audioTable, columnIndicesToSkip, dataRows);
			}

			// Create excel sheet object
			ExcelSheet sheet = new ExcelSheet(sheetName, new ArrayList<>(), dataRows);
			sheets.add(sheet);
		}

		// Write to Excel file
		ExcelWriter excelWriter = new ExcelWriter(file.getAbsolutePath());
		excelWriter.export(sheets);

		LOG.info("Finishing Excel export data to {}", file.getAbsolutePath());
	}

	@Override
	public void writeCSV(File file) throws IOException {
		LOG.info("Starting CSV export data to {}", file.getAbsolutePath());

		FileWriter writer = new FileWriter(file);
		int counter = 1;
		try {
			for (SegmentTablePanel segmentTable : segmentTables) {
				writer.append(createCSVEntry("Stream" + counter++, true)).append(Util.LINE_SEPARATOR);
				writer.append(createCSVEntry("Name", true));
				writer.append(COMMA_SEPARATOR);
				writer.append(createCSVEntry(segmentTable.getVideoStream().getManifest().getVideoName(), true)).append(Util.LINE_SEPARATOR);

				List<Integer> columnIndicesToSkip;
				String headerData;

				// Video Table data
				JTable videoTable = segmentTable.getStreamTables().get(SegmentTablePanel.VIDEO_TABLE_NAME);
				if (videoTable != null) {
					writer.append(createCSVEntry("Video Table", true)).append(Util.LINE_SEPARATOR);
					// Find column's UI view indices to skip in the report
					// Columns for Video table: CONTENT, SESSION_LINK, CHANNELS
					columnIndicesToSkip = getColumnIndicesToSkip(videoTable, videoColumnsToSkip);

					// Add header data for video table
					headerData = getHeaderData(videoTable, columnIndicesToSkip, true).stream().map(String::valueOf).collect(Collectors.joining(COMMA_SEPARATOR));
					writer.append(headerData).append(Util.LINE_SEPARATOR);
					// Add data rows for video table
					populateCSVDataRows(videoTable, writer, columnIndicesToSkip);
					writer.append(Util.LINE_SEPARATOR);
				}

				// Audio table data
				JTable audioTable = segmentTable.getStreamTables().get(SegmentTablePanel.AUDIO_TABLE_NAME);
				if (audioTable != null) {
					writer.append(createCSVEntry("Audio Table", true)).append(Util.LINE_SEPARATOR);
					// Find column's UI view indices to skip in the report
					// Columns for Audio table: CONTENT, SESSION_LINK, RESOLUTION
					columnIndicesToSkip = getColumnIndicesToSkip(audioTable, audioColumnsToSkip);

					// Add header data for video table
					headerData = getHeaderData(audioTable, columnIndicesToSkip, true).stream().map(String::valueOf).collect(Collectors.joining(COMMA_SEPARATOR));
					writer.append(headerData).append(Util.LINE_SEPARATOR);
					// Add data rows for video table
					populateCSVDataRows(audioTable, writer, columnIndicesToSkip);
					writer.append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);
				}
			}
		} finally {
			writer.close();
		}

		LOG.info("Finishing CSV export data to {}", file.getAbsolutePath());
	}

	private List<Object> getHeaderData(final JTable table, final List<Integer> columnIndicesToSkip,
			boolean isCSVExport) {
		// Get Header data row
		List<Object> headerData = new ArrayList<>();
		for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
			if (columnIndicesToSkip.contains(columnIndex)) {
				continue;
			}

			headerData.add(createCSVEntry(table.getColumnModel().getColumn(columnIndex).getHeaderValue(), isCSVExport));
		}

		return headerData;
	}

	private void populateDataRows(final JTable table, final List<Integer> columnIndicesToSkip,
			final List<List<Object>> dataRows) {
		// Get data rows
		int totalRowCount = table.getRowCount();
		for (int rowIndex = 0; rowIndex < totalRowCount; ++rowIndex) {
			List<Object> data = new ArrayList<>();

			for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
				if (columnIndicesToSkip.contains(columnIndex)) {
					continue;
				}

				Object colValue = table.getValueAt(rowIndex, columnIndex);
				data.add(colValue == null ? "" : colValue);
			}

			dataRows.add(data);
		}
	}

	private void populateCSVDataRows(JTable table, FileWriter writer, List<Integer> columnIndicesToSkip)
			throws IOException {
		// Add data rows
		for (int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex) {
			for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
				if (columnIndicesToSkip.contains(columnIndex)) {
					continue;
				}

				if (columnIndex > 0) {
					writer.append(COMMA_SEPARATOR);
				}

				writer.append(createCSVEntry(table.getValueAt(rowIndex, columnIndex), true));
			}

			writer.append(Util.LINE_SEPARATOR);
		}
	}
}
