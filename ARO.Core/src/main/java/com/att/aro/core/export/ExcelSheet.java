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
