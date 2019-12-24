
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

public class TableSortComparator implements Comparator<String> {

	private int columnIndex = -1;
	private int res;

	public TableSortComparator(int column) {
		columnIndex = column;
	}

	@Override
	public int compare(String o1, String o2) {
		switch (columnIndex) {
		case -1: res =  0;                        break;    // 
		case  0: res = compareInteger(o1, o2);    break;    // column  0 = Segment No.      - n
		case  1: res = o1.compareTo(o2);          break;    // column  1 = Content          - a
		case  2: res = compareDouble(o1, o2);     break;    // column  2 = DL Start Time    - n
		case  3: res = compareDouble(o1, o2);     break;    // column  3 = DL End Time      - n
		case  4: res = compareDouble(o1, o2);     break;    // column  4 = StallTime        - n
		case  5: res = compareDouble(o1, o2);     break;    // column  5 = PlayTime         - n
		case  6: res = compareDouble(o1, o2);     break;    // column  6 = StartTime        - n
		case  7: res = compareInteger(o1, o2);    break;    // column  7   Track Quality    - a
		case  8: res = compareDouble(o1, o2);     break;    // column  8   Resolution       - n
		case  9: res = compareDouble(o1, o2);     break;    // column  9   Bitrate          - n
		case 10: res = compareDouble(o1, o2);     break;    // column 10   Total Bytes      - n
		case 11: res = compareDouble(o1, o2);     break;    // column 11   Duration         - n
		case 12: res = compareDouble(o1, o2);     break;    // column 12   TCP Session      - n
		case 13: res = o1.compareTo(o2);          break;    // column 13   TCP State        - a
		case 14: res = 0;          				  break;    // column 14   SessionLink      - a

		default:
			res = o1.compareTo(o2); 
			break;
		}
		return res;
	}
	
	private int compareInteger(String o1, String o2) {
		int seg1 = Integer.parseInt(o1);
		int seg2 = Integer.parseInt(o2);
		if (seg1 > seg2)
			return 1;
		if (seg1 < seg2)
			return -1;
		else
			return 0;
	}
	
	private int compareDouble(String o1, String o2) {
		double seg1 = Double.parseDouble(o1);
		double seg2 = Double.parseDouble(o2);
		if (seg1 > seg2)
			return 1;
		if (seg1 < seg2)
			return -1;
		else
			return 0;
	}

}
