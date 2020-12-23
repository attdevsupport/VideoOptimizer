package com.att.aro.ui.model.listener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.export.ExcelSheet;
import com.att.aro.core.export.ExcelWriter;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.diagnostic.TCPUDPFlowsTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.google.common.collect.Lists;

/**
 * Listener for TCPFlowsDataTable export items
 * 
 * @author arpitbansal
 *
 */
public class TCPFlowsTableMenuItemListener extends AbstractMenuItemListener {
    private static final Logger LOG = LoggerFactory.getLogger(TCPFlowsTableMenuItemListener.class);

    public TCPFlowsTableMenuItemListener(DataTable<?> table) {
        super(table);
    }

    /**
     * Find all rows for which a checkbox is selected
     * 
     * @return
     */
    private List<Integer> getSelectedRowsIndices(int columnIndexToSkip) {
        List<Integer> selectedRowsIndices = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex) {
            if ((Boolean) table.getValueAt(rowIndex, columnIndexToSkip)) {
                selectedRowsIndices.add(rowIndex);
            }
        }

        LOG.info("Total data rows to export {}", selectedRowsIndices.size());
        return selectedRowsIndices;
    }

    @Override
    public void writeExcel(File file) throws IOException {
        LOG.info("Starting export data to {}", file.getAbsolutePath());
        // Get index of checkbox column which is to skip while exporting
        int columnIndexToSkip = table.convertColumnIndexToView(TCPUDPFlowsTableModel.CHECKBOX_COL);
        LOG.info("Checkbox column view index {}", columnIndexToSkip);

        List<Integer> selectedRows = getSelectedRowsIndices(columnIndexToSkip);
        if (selectedRows.size() == 0) {
            MessageDialogFactory.showMessageDialog(null,
                    ResourceBundleHelper.getMessageString("tcp.error.noRowSelected"));
            return;
        }

        List<List<Object>> dataRows = null;
        List<Object> columnNames = null;

        columnNames = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
            if (columnIndex == columnIndexToSkip) {
                continue;
            }

            columnNames.add(table.getColumnModel().getColumn(columnIndex).getHeaderValue());
        }

        // Add data rows
        dataRows = new ArrayList<>();
        for (int rowIndex : selectedRows) {
            List<Object> data = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
                if (columnIndex == columnIndexToSkip) {
                    continue;
                }

                Object colValue = table.getValueAt(rowIndex, columnIndex);
                data.add(colValue == null ? "" : colValue);
            }

            dataRows.add(data);

        }

        // Write to excel file
        String sheetName = table.getName() == null ? "Sheet1" : table.getName();
        ExcelSheet sheet = new ExcelSheet(sheetName, columnNames, dataRows);
        ExcelWriter excelWriter = new ExcelWriter(file.getAbsolutePath());
        excelWriter.export(Lists.newArrayList(sheet));

        LOG.info("Finish export data to {}", file.getAbsolutePath());
    }

    @Override
    public void writeCSV(File file) throws IOException {
        LOG.info("Starting export data to {}", file.getAbsolutePath());

        boolean isCSV = true;
        // Get index of checkbox column which is to skip while exporting
        int columnIndexToSkip = table.convertColumnIndexToView(TCPUDPFlowsTableModel.CHECKBOX_COL);
        LOG.info("Checkbox column index {}", columnIndexToSkip);

        List<Integer> selectedRows = getSelectedRowsIndices(columnIndexToSkip);
        if (selectedRows.size() == 0) {
            MessageDialogFactory.showMessageDialog(null,
                    ResourceBundleHelper.getMessageString("tcp.error.noRowSelected"));
            return;
        }

        FileWriter writer = new FileWriter(file);
        try {
            createCSVHeader(writer, columnIndexToSkip);

            for (int rowIndex : selectedRows) {
                for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
                    if (columnIndex == columnIndexToSkip) {
                        continue;
                    }
                    if (columnIndex > 0) {
                        writer.append(COMMA_SEPARATOR);
                    }
                    writer.append(createCSVEntry(table.getValueAt(rowIndex, columnIndex), isCSV));
                }
                writer.append(Util.LINE_SEPARATOR);

            }
        } finally {
            writer.close();
        }

        LOG.info("Finish export data to {}", file.getAbsolutePath());
    }

    private void createCSVHeader(FileWriter writer, int columnIndexToSkip) throws IOException {
        for (int columnIndex = 0; columnIndex < table.getColumnCount(); ++columnIndex) {
            if (columnIndex == columnIndexToSkip) {
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
