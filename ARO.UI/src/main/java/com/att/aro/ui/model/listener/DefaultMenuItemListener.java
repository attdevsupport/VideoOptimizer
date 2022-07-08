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
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.export.ExcelSheet;
import com.att.aro.core.export.ExcelWriter;
import com.att.aro.core.util.Util;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTableModel;
import com.google.common.collect.Lists;


/**
 * Default listener for all export menu items
 * @author arpitbansal
 *
 */
public class DefaultMenuItemListener extends AbstractMenuItemListener {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMenuItemListener.class);

    public DefaultMenuItemListener(DataTable<?> table) {
        super(table);
    }

    /**
     * Method to return the table column indices whose names are empty string, null or blank spaces
     */
    private List<Integer> findEmptyColumns() {
        DataTableModel<?> model = (DataTableModel<?>)table.getModel();
        List<Integer> emptyColumnIndices = new ArrayList<>();

        for (int i = 0; i < model.getColumnCount(); i++) {
            if (StringUtils.isBlank(model.getColumnName(i))) {
                int viewIndex = table.convertColumnIndexToView(i);
                LOG.info("Blank column view index {}", viewIndex);
                emptyColumnIndices.add(table.convertColumnIndexToView(i));
            }
        }

        return emptyColumnIndices;
    }

    /**
     * Get indices of selected rows in the table. If no rows are selected, returns indices of all the rows.
     * @param table
     * @return selected rows indices
     */
    private int[] getSelectedRowsIndices() {
        int[] selectedRows = table.getSelectedRows();

        if (selectedRows.length == 0) {
            selectedRows = IntStream.range(0, table.getRowCount()).toArray();
        }

        LOG.info("Total data rows to export {}", selectedRows.length);
        return selectedRows;
    }

    @Override
    public void writeExcel(File file) throws IOException {
        LOG.info("Starting export data to {}", file.getAbsolutePath());

        // Get selected rows or return all rows if no row is selected
        int[] selectedRows = getSelectedRowsIndices();
        // Find columns with blank names to skip in the report
        List<Integer> columnIndicesToSkip = findEmptyColumns();

        List<List<Object>> dataRows = null;
        List<Object> columnNames = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
            if (columnIndicesToSkip.contains(columnIndex)) {
                continue;
            }

            columnNames.add(table.getColumnModel().getColumn(columnIndex).getHeaderValue());
        }

        if (selectedRows.length > 0) {
            dataRows = new ArrayList<>();

            for (int rowIndex : selectedRows) {
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

            // Write to Excel file
            String sheetName = table.getName() == null ? "Sheet1" : table.getName();
            ExcelSheet sheet = new ExcelSheet(sheetName, columnNames, dataRows);
            ExcelWriter excelWriter = new ExcelWriter(file.getAbsolutePath());
            excelWriter.export(Lists.newArrayList(sheet));
        }

        LOG.info("Finish export data to {}", file.getAbsolutePath());
    }


    @Override
    public void writeCSV(File file) throws IOException {
        LOG.info("Starting export data to {}", file.getAbsolutePath());

        // Get selected rows or return all rows if no row is selected
        int[] selectedRows = getSelectedRowsIndices();
        // Find columns with blank names to skip in the report
        List<Integer> columnIndicesToSkip = findEmptyColumns();

        FileWriter writer = new FileWriter(file);
        try {
            createCSVHeader(writer, columnIndicesToSkip);

            for (int rowIndex : selectedRows) {
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
        } finally {
            writer.close();
        }

        LOG.info("Finish export data to {}", file.getAbsolutePath());
    }

    private void createCSVHeader(FileWriter writer, List<Integer> columnIndicesToSkip) throws IOException {
        for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
            if (columnIndicesToSkip.contains(columnIndex)) {
                continue;
            }

            if (columnIndex > 0) {
                writer.append(COMMA_SEPARATOR);
            }

            writer.append(createCSVEntry(table.getColumnModel().getColumn(columnIndex).getHeaderValue(), true));
        }

        writer.append(Util.LINE_SEPARATOR);
    }
}
