/*
 *  Copyright 2020 AT&T
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
package com.att.aro.core.export.style;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public enum FontStyle implements ExcelCellStyle {
    DEFAULT_BOLD(XSSFFont.DEFAULT_FONT_NAME, true, false, Font.U_NONE, XSSFFont.DEFAULT_FONT_SIZE, IndexedColors.BLACK);

    private String name;
    private boolean isBold;
    private boolean isItalic;
    private byte underline;
    private int fontSize;
    private IndexedColors fontColor;

    FontStyle(String name, boolean isBold, boolean isItalic, byte underline, short fontSize, IndexedColors fontColor) {
        this.name = name;
        this.isBold = isBold;
        this.isItalic = isItalic;
        this.underline = underline;
        this.fontSize = fontSize;
        this.fontColor = fontColor;
    }

    @Override
    public CellStyle getCellStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName(name);
        font.setBold(isBold);
        font.setItalic(isItalic);
        font.setUnderline(underline);
        font.setFontHeightInPoints((short) fontSize);
        font.setColor(fontColor.getIndex());
        style.setFont(font);

        return style;
    }
}
