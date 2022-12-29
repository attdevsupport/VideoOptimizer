
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

import org.apache.commons.lang3.StringUtils;

public class TableSortComparator implements Comparator<Object> {

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
	public int compare(Object objectA, Object objectB) {
		if (objectA instanceof String && objectB instanceof String) {
			String stringA = (String)objectA;
			String stringB = (String)objectB;
			switch (columnIndex) {
			case -1:
			case 15: // column 15   SessionLink     - don't sort
				res =  0; break;
			case  0:// column  0 = Segment No.      - n
			case  1:// column  1   Track Quality    - n
				res = compareInteger(stringA, stringB);     break;
			case  4:// column  4   Resolution       - n
			case  5:// column  5   Bitrate          - n
			case  6:// column  6   Total Bytes      - n
			case  7:// column  7 = StartTime        - n
			case  8:// column  8   Duration         - n
			case  9:// column  9 = DL Start Time    - n
			case 10:// column 10 = DL End Time      - n
			case 11:// column 11 = PlayTime         - n
			case 12:// column 12 = StallTime        - n
			case 13:// column 13   TCP Session      - n
				res = compareDouble(stringA, stringB);     break;
			case  2:// column  2 = Content          - a
			case  3:// column  3 = CHANNELS,		- a
			case 14:// column 14   TCP State        - a
				res = stringA.compareTo(stringB);          break;
	
			default:
				res = stringA.compareTo(stringB); 
				break;
			}
		} else if (objectA instanceof Integer && objectB instanceof Integer){
			Integer intA = (Integer)objectA;
			Integer intB = (Integer)objectB;
			res = intA.compareTo(intB);
		} else if (objectA instanceof Double && objectB instanceof Double) {
			Double doubleA = (Double)objectA;
			Double doubleB = (Double)objectB;
			res = doubleA.compareTo(doubleB);
		} else {
			return 0;
		}
		return res;
	}
	
	private int compareInteger(String objectA, String objectB) {
		return (isNormalCompare(objectA, objectB)) 
				? Integer.compare(Integer.parseInt(objectA), Integer.parseInt(objectB)) 
				: objectA.compareTo(objectB);
	}

	private int compareDouble(String objectA, String objectB) {
		return (isNormalCompare(objectA, objectB)) 
				? Double.compare(Double.parseDouble(objectA), Double.parseDouble(objectB)) 
				: objectA.compareTo(objectB);
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
		return !((objectA.contains(notApplicableString) || objectB.contains(notApplicableString)) || (objectA.contains("NA") || objectB.contains("NA")));
	}
	
}
