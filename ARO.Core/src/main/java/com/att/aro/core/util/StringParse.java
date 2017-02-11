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
package com.att.aro.core.util;

import java.util.Scanner;

public final class StringParse {

	public static Double findLabeledDoubleFromString(String fieldSearch, byte[] data) {
		return findLabeledDoubleFromString(fieldSearch, new String(data));
	}
	
	/**
	 * Search for a field to return the value or one word of data following the match.
	 * 
	 * @param fieldSearch
	 * @param sData
	 * @return String value, empty if not found
	 */
	public static String findLabeledDataFromString(String fieldSearch, String sData) {
		String value = "";
		int pos = sData.indexOf(fieldSearch);

		if (pos != -1) {
			value = new Scanner(sData.substring(pos + fieldSearch.length())).useDelimiter("[^\\d]+").next();
		}
		return value;
	}

	public static String findLabeledDataFromString(String fieldSearch, String delimeter, String sData) {
		String value = "";
		int pos = sData.indexOf(fieldSearch);

		if (pos != -1) {
			value = new Scanner(sData.substring(pos + fieldSearch.length())).useDelimiter(delimeter).next();
		}
		return value;
	}

	public static String findLabeledDataFromString(String fieldSearch, byte[] data) {
		return findLabeledDataFromString(fieldSearch, new String(data));
	}

	public static String findLabeledDataFromString(String fieldSearch, String delimeter, byte[] data) {
		return findLabeledDataFromString(fieldSearch, delimeter, new String(data));
	}

	/**
	 * Search for a field to return the value or one word of data following the match.
	 * 
	 * @param fieldSearch
	 * @param sData
	 * @return String value, empty if not found
	 */
	public static Double findLabeledDoubleFromString(String fieldSearch, String sData) {
		Double value = null;
		String sValue = findLabeledDataFromString(fieldSearch, sData);
		if (!sValue.isEmpty()) {
			try {
				value = Double.valueOf(sValue);
			} catch (NumberFormatException e) {
				value = null;
			}
		}
		return value;
	}

	/**
	 * Search for a field to return the value or one word of data following the match.
	 * 
	 * @param fieldSearch
	 * @param sData
	 * @return String value, empty if not found
	 */
	public static Double findLabeledDoubleFromString(String fieldSearch, String delimeter, String sData) {
		Double value = null;
		String sValue = findLabeledDataFromString(fieldSearch, delimeter, sData);
		if (!sValue.isEmpty()) {
			try {
				value = Double.valueOf(sValue);
			} catch (NumberFormatException e) {
				value = null;
			}
		}
		return value;
	}

	public static String subString(String name, char mark1, char mark2) {
		int posMk1 = name.lastIndexOf(mark1);
		int poaMk2 = name.lastIndexOf(mark2);
		String quality = "0";
		if (posMk1 != -1 && poaMk2 != -1 && posMk1 < poaMk2) {
			quality = name.substring(posMk1 + 1, poaMk2);
		}
		return quality;
	}

}