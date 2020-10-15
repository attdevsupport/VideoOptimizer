package com.att.aro.core.export;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ExcelSheet {
    private final String name;
    private List<Object> columnNames;
    private List<List<Object>> dataRows;

    public ExcelSheet(String name) {
        this.name = name;
        columnNames = Collections.emptyList();
        dataRows = Collections.emptyList();
    }

    public ExcelSheet(String name, List<Object> columnNames, List<List<Object>> dataRows) {
        this.name = name;
        this.columnNames = ImmutableList.copyOf(columnNames);
        this.dataRows = ImmutableList.copyOf(dataRows);
    }
}
