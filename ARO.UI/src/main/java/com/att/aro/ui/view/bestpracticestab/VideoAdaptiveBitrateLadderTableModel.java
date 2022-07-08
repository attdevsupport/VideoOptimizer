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
package com.att.aro.ui.view.bestpracticestab;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.videoanalysis.pojo.QualityTime;

public class VideoAdaptiveBitrateLadderTableModel extends AbstractTableModel{

	private static final long serialVersionUID = 1L;
	private List<QualityTime> data = new ArrayList<>();

	public void setData(List<QualityTime> newData) {
		this.data.clear();
		if (newData != null) {
			this.data.addAll(newData);
		}
		fireTableDataChanged();
	}

	public List<QualityTime> getData() {
		return data;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return null;
	}

	@Override
	public int getColumnCount() {
		return 0;
	}



}
