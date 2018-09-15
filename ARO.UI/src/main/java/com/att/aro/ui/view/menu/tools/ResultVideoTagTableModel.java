/*
 *  Copyright 2018 AT&T
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
package com.att.aro.ui.view.menu.tools;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

import com.att.aro.core.videoanalysis.impl.RegexMatchLbl;
import com.att.aro.core.videoanalysis.pojo.RegexMatchResult;
import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;

public class ResultVideoTagTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;

	private String[] columnNames = { "Data", "XREF"};
	
	private Object[][] data = new Object[0][2];

	private EnumSet<VideoDataTags> vdt;

	public Object[][] getData(){
		return this.data;
	}
	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.length;
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		Object str = data[row][col];
		if (col == 0 && str != null && str instanceof HashMap) {
			HashMap<RegexMatchLbl, String> value = (HashMap<RegexMatchLbl, String>) str;
			String cellValue = value.values().stream().findFirst().get();
			return cellValue;
		}
		return data[row][col];
	}

	public Class<?> getColumnClass(int col) {
		Class<? extends Object> classObj = null;
		classObj = getValueAt(0, col).getClass();
		return classObj;
	}

	public boolean isCellEditable(int row, int col) {
		if (col < 0) {
			return false;
		} else {
			return true;
		}
	}

	/**<pre>
	 * Column 0 is String data,
	 * Column 1 is VideoDataTag
	 */
	public void setValueAt(Object value, int row, int col) {
		if (row < 0 || col < 0 || row >= data.length || col >= data[row].length) {
			return;
		}
		if (col == 1 && value.getClass().equals(String.class)) {
			data[row][col] = getVideoDataTag((String) value);
		} else {
			data[row][col] = value;
		}
		fireTableCellUpdated(row, col);
	}

	/**<pre>
	 * Locate and return VideoDataTag that matches value
	 * This method only called due to user action in a combobox, so efficiency is not of high importance.
	 * 
	 * @param value
	 * @return
	 */
	private VideoDataTags getVideoDataTag(String value) {
		if (vdt == null) {
			vdt = EnumSet.allOf(VideoDataTags.class);
		}
		for (VideoDataTags tag : vdt) {
			if (value.equals(tag.toString())){
				return tag;
			}
		}
		return VideoDataTags.unknown;
	}

	public VideoDataTags[] getVideoDataTags() {
		VideoDataTags[] tags = new VideoDataTags[data.length];
		for (int idx = 0; idx < tags.length; idx++) {
			tags[idx] = (VideoDataTags) data[idx][1];
		}
		return tags;
	}

	public Map<RegexMatchLbl, VideoDataTags[]> getVideoDataTagsMap() {
		Map<RegexMatchLbl, VideoDataTags[]> resultMap = new LinkedHashMap<>();
		RegexMatchLbl category = RegexMatchLbl.REQUEST;
		List<VideoDataTags> tagList = new ArrayList<>();

		for (int idx = 0; idx < data.length; idx++) {
			if(!(data[idx][0] instanceof HashMap)) {
				continue;
			}
			Map<RegexMatchLbl, String> value = (HashMap<RegexMatchLbl, String>) data[idx][0];
			if (value != null) {
				RegexMatchLbl lbl = value.keySet().stream().findFirst().get();
				if (category == lbl) {
					tagList.add((VideoDataTags) data[idx][1]);
				} else if (category != lbl && (!tagList.isEmpty())) {
					resultMap.put(category, tagList.toArray(new VideoDataTags[tagList.size()]));
					category = lbl;
					tagList.clear();
					tagList.add((VideoDataTags) data[idx][1]);
				} else {
					category = lbl;
					tagList.add((VideoDataTags) data[idx][1]);
				}
			}
		}
		if (!tagList.isEmpty()) {
			resultMap.put(category, tagList.toArray(new VideoDataTags[tagList.size()]));
		}
		return resultMap;
	}

	public void update(Map<RegexMatchLbl, RegexMatchResult> results,
			Map<RegexMatchLbl, VideoDataTags[]> videoDataTags) {
		if (results != null && videoDataTags != null && results.size() > 0) {

			int resSize = results.values().stream().mapToInt(i -> i.getResult().length).sum();
			int vdtSize = videoDataTags.values().stream().mapToInt(i -> i.length).sum(); 
			int maxCount = resSize > vdtSize ? resSize : vdtSize;
			data = new Object[maxCount][2];
			String[] res = new String[resSize];
			int index = 0;
			for (RegexMatchResult value : results.values()) {
				for (String strValue : value.getResult()) {
					res[index] = strValue;
					index++;
				}
			}
			int prevSum = 0;
			for (int idx = 0; idx < resSize; idx++) {
				RegexMatchLbl category = null;
				int lenSum = 0;

				for (RegexMatchResult matchResult : results.values()) {
					lenSum = lenSum + matchResult.getResult().length;
					if (lenSum > idx) {
						for (Entry<RegexMatchLbl, RegexMatchResult> entry : results.entrySet()) {
							if (entry.getValue().equals(matchResult)) {
								category = entry.getKey();
								break;
							}
						}
						break;
					} else {
						prevSum = lenSum;
					}
				}
				Map<RegexMatchLbl, String> map = new HashMap<>();
				map.put(category, res[idx]);
				data[idx][0] = map;

				if (category != null) {
					VideoDataTags[] xrefs = videoDataTags.get(category);
					if (xrefs != null) {
						int len = xrefs.length;
						VideoDataTags tag = (xrefs[(idx - prevSum) % len] != null) ? xrefs[(idx - prevSum) % len]
								: VideoDataTags.unknown;
						data[idx][1] = tag;
					} else {
						data[idx][1] = VideoDataTags.unknown;
					}
				}
			}
		} else {
			data = new Object[0][2];
		}
		fireTableDataChanged();
	}
}
