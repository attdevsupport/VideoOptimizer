
/*
*  Copyright 2017 AT&T
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

package com.att.aro.ui.view.videotab;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

public class TableSortComparator implements Comparator<String> {

	private int columnIndex = -1;
	private int res;
	private String notApplicableString;

	public TableSortComparator(int column) {
		this(column, null);
	}

	public TableSortComparator(int column, String notApplicableString) {
		this.notApplicableString = notApplicableString;
		columnIndex = column;
	}

	@Override
	public int compare(String objectA, String objectB) {
		switch (columnIndex) {
		case -1: res =  0;                        			break;    // 
		case  0: res = compareInteger(objectA, objectB);    break;    // column  0 = Segment No.      - n
		case  1: res = objectA.compareTo(objectB);          break;    // column  1 = Content          - a
		case  2: res = compareDouble(objectA, objectB);     break;    // column  2 = DL Start Time    - n
		case  3: res = compareDouble(objectA, objectB);     break;    // column  3 = DL End Time      - n
		case  4: res = compareDouble(objectA, objectB);     break;    // column  4 = StallTime        - n
		case  5: res = compareDouble(objectA, objectB);     break;    // column  5 = PlayTime         - n
		case  6: res = compareDouble(objectA, objectB);     break;    // column  6 = StartTime        - n
		case  7: res = compareInteger(objectA, objectB);    break;    // column  7   Track Quality    - a
		case  8: res = compareDouble(objectA, objectB);     break;    // column  8   Resolution       - n
		case  9: res = compareDouble(objectA, objectB);     break;    // column  9   Bitrate          - n
		case 10: res = compareDouble(objectA, objectB);     break;    // column 10   Total Bytes      - n
		case 11: res = compareDouble(objectA, objectB);     break;    // column 11   Duration         - n
		case 12: res = compareDouble(objectA, objectB);     break;    // column 12   TCP Session      - n
		case 13: res = objectA.compareTo(objectB);          break;    // column 13   TCP State        - a
		case 14: res = 0;          				  			break;    // column 14   SessionLink      - a

		default:
			res = objectA.compareTo(objectB); 
			break;
		}
		return res;
	}
	
	private int compareInteger(String objectA, String objectB) {
		if (isNormalCompare(objectA, objectB)) {
			int val1 = Integer.parseInt(objectA);
			int val2 = Integer.parseInt(objectB);
			if (val1 > val2) {
				return 1;
			}
			if (val1 < val2) {
				return -1;
			}
		} 
		return objectA.compareTo(objectB);
	}

	private int compareDouble(String objectA, String objectB) {
		if (isNormalCompare(objectA, objectB)) {
			double val1 = Double.parseDouble(objectA);
			double val2 = Double.parseDouble(objectB);
			if (val1 > val2) {
				return 1;
			}
			if (val1 < val2) {
				return -1;
			}
		}
		return objectA.compareTo(objectB);
	}

	/**
	 * If either object contains a dash, it is deemed to be not comparable
	 * 
	 * @param objectA
	 * @param objectB
	 * @return
	 */
	public boolean isNormalCompare(String objectA, String objectB) {
		if (StringUtils.isEmpty(notApplicableString)) {
			return true;
		}
		return !( objectA.contains(notApplicableString) || objectB.contains(notApplicableString));
	}
	
}
