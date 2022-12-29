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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.export;

import com.att.aro.core.export.style.ExcelCellStyle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
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
        Map<ExcelCellStyle, CellStyle> styleMap = new HashMap<>();

        for (ExcelSheet sheet : sheets) {
            Sheet excelSheet = workbook.createSheet(sheet.getName());
            int rowNumber = 0;

            // Create a header row
            if (sheet.getColumnNames() != null && sheet.getColumnNames().size() > 0) {
                Row headerRow = excelSheet.createRow(rowNumber++);
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setFontHeightInPoints((short) 14);
                headerFont.setColor(IndexedColors.BLACK.getIndex());

                CellStyle headerCellStyle = workbook.createCellStyle();
                headerCellStyle.setFont(headerFont);

                for (int i = 0; i < sheet.getColumnNames().size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    setCellValue(workbook, cell, sheet.getColumnNames().get(i), styleMap);
                    cell.setCellStyle(headerCellStyle);
                }
            }

            int numberOfColumns = 0;
            // Creating data rows
            for (List<Object> dataRow : sheet.getDataRows()) {
                Row row = excelSheet.createRow(rowNumber++);
                numberOfColumns = Math.max(numberOfColumns, dataRow.size());

                for (int cellIndex = 0; cellIndex < dataRow.size(); ++cellIndex) {
                    Cell cell = row.createCell(cellIndex);
                    setCellValue(workbook, cell, dataRow.get(cellIndex), styleMap);
                }
            }

            // Resize all columns to fit the content size
            for (int i = 0; i < numberOfColumns; i++) {
                excelSheet.autoSizeColumn(i);
            }

            // Write the output to a file
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
        }

        styleMap.clear();
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
    private void setCellValue(Workbook workbook, Cell cell, Object content, Map<ExcelCellStyle, CellStyle> styleMap) {
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
        } else if (content instanceof ExcelCell) {
            ExcelCell excelCell = (ExcelCell) content;

            if (excelCell.getStyle() != null && workbook instanceof XSSFWorkbook) {
                if (styleMap.get(excelCell.getStyle()) != null) {
                    cell.setCellStyle(styleMap.get(excelCell.getStyle()));
                } else {
                    CellStyle style = excelCell.getStyle().getCellStyle((XSSFWorkbook) workbook);
                    cell.setCellStyle(style);
                    styleMap.put(excelCell.getStyle(), style);
                }
            }

            setCellValue(workbook, cell, excelCell.getValue(), styleMap);
        } else {
            cell.setCellValue((content!=null?content.toString():""));
        }
    }
}
