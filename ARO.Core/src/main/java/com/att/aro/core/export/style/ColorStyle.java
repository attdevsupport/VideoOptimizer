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
package com.att.aro.core.export.style;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public enum ColorStyle implements ExcelCellStyle {
    LIME(IndexedColors.LIME),
    CORAL(IndexedColors.CORAL),
    YELLOW(IndexedColors.YELLOW),
    SKY_BLUE(IndexedColors.SKY_BLUE),
    LIGHT_TURQUOISE(IndexedColors.LIGHT_TURQUOISE),
    LIGHT_GREY(IndexedColors.GREY_25_PERCENT),
    RED(IndexedColors.RED);

    private IndexedColors color;

    ColorStyle(IndexedColors color) {
        this.color = color;
    }

    @Override
    public CellStyle getCellStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();

        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        XSSFColor color = new XSSFColor(this.color, colorMap);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }
}
