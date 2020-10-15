package com.att.aro.core.export;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;



public class ExcelWriter {
    private String filePath;

    /**
     * Throws IllegalArgumentException if file name does not have a valid Excel file extension
     * @param filePath
     */
    public ExcelWriter(String filePath) {
        if (!filePath.endsWith(".xlsx") && !filePath.endsWith(".xls")) {
            throw new IllegalArgumentException("The specified file is not an Excel file");
        }

        this.filePath = filePath;
    }

    public void export(List<ExcelSheet> sheets) throws IOException {
        if (CollectionUtils.isEmpty(sheets)) {
            return;
        }

        Workbook workbook = getWorkbook();
        for (ExcelSheet sheet : sheets) {
            Sheet excelSheet = workbook.createSheet(sheet.getName());

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            if (sheet.getColumnNames() != null) {
                int rowNumber = 0;

                // Create a header row
                if (sheet.getColumnNames().size() > 0) {
                    Row headerRow = excelSheet.createRow(rowNumber++);
                    for (int i = 0; i < sheet.getColumnNames().size(); i++) {
                        Cell cell = headerRow.createCell(i);
                        setCellValue(cell, sheet.getColumnNames().get(i));
                        cell.setCellStyle(headerCellStyle);
                    }
                }
    
                // Creating data rows
                for (List<Object> dataRow : sheet.getDataRows()) {
                    Row row = excelSheet.createRow(rowNumber++);
                    for (int cellIndex = 0; cellIndex < dataRow.size(); ++cellIndex) {
                        Cell cell = row.createCell(cellIndex);
                        setCellValue(cell, dataRow.get(cellIndex));
                    }
                }

                // Resize all columns to fit the content size
                for (int i = 0; i < sheet.getColumnNames().size(); i++) {
                    excelSheet.autoSizeColumn(i);
                }
    
                // Write the output to a file
                FileOutputStream fileOut = new FileOutputStream(filePath);
                workbook.write(fileOut);
                fileOut.close();
            }
        }

        workbook.close();
    }

    private Workbook getWorkbook() {
        Workbook workbook = filePath.endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
        return workbook;
    }

    /**
     * Sets the cell value to the provided content type 
     * @param cell
     * @param content
     */
    private void setCellValue(Cell cell, Object content) {
        if (content instanceof String) {
            String str = (String) content;
            if (str.startsWith("=")) {
                cell.setCellFormula(str.substring(1));
            } else {
                cell.setCellValue(str);
            }
        } else if (content instanceof Boolean) {
            cell.setCellValue((Boolean) content);
        } else if (content instanceof Number) {
            cell.setCellValue(((Number) content).doubleValue());
        } else if (content instanceof Date) {
            cell.setCellValue((Date) content);
        } else if (content instanceof Calendar) {
            cell.setCellValue((Calendar) content);
        } else if (content instanceof RichTextString) {
            cell.setCellValue((RichTextString) content);
        } else {
            cell.setCellValue(content.toString());
        }
    }
}
