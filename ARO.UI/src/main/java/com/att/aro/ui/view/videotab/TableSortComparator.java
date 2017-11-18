
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

	public TableSortComparator(int column) {
		columnIndex = column;
	}

	@Override
	public int compare(String o1, String o2) {
		/*
		 * column 0 = segment No
		 * column 1 = DL Start Time
		 * column 2 = DL End Time
		 * column 3 = Quality
		 * column 4 = Bitrate
		 * column 5 = Total Byte
		 * column 6 = Duration
		 */
		if (columnIndex != -1) {
			if(columnIndex == 0 || columnIndex == 3){
				return compareInteger(o1,o2);
			}else {
				return compareDouble(o1,o2);
			}
			
		}
		return 0;
	}
	
	private int compareInteger(String o1,String o2){
		int seg1= Integer.parseInt(o1);
		int seg2 = Integer.parseInt(o2);
		if(seg1>seg2)
			return 1;
		if(seg1<seg2)
			return -1;
		else 
			return 0; 
	}
	
	private int compareDouble(String o1,String o2){
		double seg1= Double.parseDouble(o1);
		double seg2 = Double.parseDouble(o2);
		if(seg1>seg2)
			return 1;
		if(seg1<seg2)
			return -1;
		else 
			return 0; 
	}

}
