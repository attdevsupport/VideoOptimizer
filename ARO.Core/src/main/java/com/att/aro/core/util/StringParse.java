/*
 *   Copyright 2017 AT&T
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

import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class StringParse implements IStringParse{

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
			Scanner scanner = new Scanner(sData.substring(pos + fieldSearch.length())).useDelimiter("[^\\d]+");
			value = scanner.next();
		}
		return value;
	}

	public static String findLabeledDataFromString(String fieldSearch, String delimeter, String sData) {
		String value = "";
		int pos = sData.indexOf(fieldSearch);

		if (pos != -1) {
			Scanner scanner = new Scanner(sData.substring(pos + fieldSearch.length())).useDelimiter(delimeter);
			value = scanner.next();
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
		String sValue = findLabeledDataFromString(fieldSearch, sData);
		return stringToDouble(sValue);
	}

	/**
	 * Search for a field to return the value or one word of data following the match.
	 * 
	 * @param fieldSearch
	 * @param sData
	 * @return String value, empty if not found
	 */
	public static Double findLabeledDoubleFromString(String fieldSearch, String delimeter, String sData) {
		String sValue = findLabeledDataFromString(fieldSearch, delimeter, sData);
		return stringToDouble(sValue);
	}

	/**
	 * <pre>
	 * Convert string to Integer
	 * 
	 * @param strValue
	 * @param defaultValue
	 * @return Integer or null if fails to parse
	 */
	public static Integer stringToInteger(String sValue) {
		Integer value = null;
		if (!StringUtils.isEmpty(sValue)) {
			try {
				value = Integer.valueOf(sValue);
			} catch (NumberFormatException e) {
				value = null;
			}
		}
		return value;
	}
	

	/**
	 * <pre>
	 * Convert string to Integer or defaultValue if fails to parse
	 * 
	 * @param strValue
	 * @param defaultValue
	 * @return Integer
	 */
	public static Integer stringToInteger(String strValue, int defaultValue) {
		Integer value = stringToInteger(strValue);
		return value != null ? value : defaultValue;
	}

	/**
	 * <pre>
	 * Convert string to Double
	 * 
	 * @param strValue
	 * @param defaultValue
	 * @return Double or null if fails to parse
	 */
	public static Double stringToDouble(String sValue) {
		Double value = null;
		if (!StringUtils.isEmpty(sValue)) {
			try {
				value = Double.valueOf(sValue);
			} catch (NumberFormatException e) {
				value = null;
			}
		}
		return value;
	}
	
	/**
	 * <pre>
	 * Convert string to Double or defaultValue if fails to parse
	 * 
	 * @param strValue
	 * @param defaultValue
	 * @return Double
	 */
	public static Double stringToDouble(String strValue, double defaultValue) {
		Double value = stringToDouble(strValue);
		return value != null ? value : defaultValue;
	}

	/**
	 * <pre>
	 * method not ready for prime time!!
	 * mark1 and mark2 must be unique
	 * 
	 * @param name
	 * @param mark1
	 * @param mark2
	 * @return data between mark[12] or returns "0" 
	 */
	public static String subString(String name, char mark1, char mark2) {
		int posMk1 = name.lastIndexOf(mark1);
		int posMk2 = name.lastIndexOf(mark2);
		String val = "0";
		if (posMk1 != -1 && posMk2 != -1 && posMk1 < posMk2) {
			val = name.substring(posMk1 + 1, posMk2);
		}
		return val;
	}

	@Override
	public String[] parse(String targetString, String regex) {
		
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		return parse(targetString, pattern);
	}
	
	/**
	 * regex match search of all lines in targetString
	 * 
	 * @param targetString
	 * @param regex
	 * @return String[], of first match found in targetString
	 */
	@Override
	public String[] parse(String targetString, Pattern pattern) {

		String[] temp = null;

		Matcher matcher = pattern.matcher(targetString);
		if (matcher.find() && matcher.groupCount() > 0) {
			temp = new String[matcher.groupCount()];
			for (int index = 0; index < matcher.groupCount();) {
				temp[index++] = matcher.group(index);
			}
		}
		return temp;
	}

	/**
	 * regex match search of all lines in targetString
	 * 
	 * @param results		usually this should be an empty list
	 * @param targetString
	 * @param regex			regex may have multiple capture groups, resulting String[]'s will be merged
	 * @return ArrayList<String>, of all matches found in targetString
	 */
	@Override
	public List<String> parse(List<String> results, String targetString, String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		return parse(results, targetString, pattern);
	}

	/**
	 * regex match search of all lines in targetString
	 * 
	 * @param results		usually this should be an empty list
	 * @param targetString
	 * @param pattern		pattern may have multiple capture groups, resulting String[]'s will be merged
	 * @return ArrayList<String>, of all matches found in targetString
	 */
	@Override
	public List<String> parse(List<String> results, String targetString, Pattern pattern) {
		Matcher matcher = pattern.matcher(targetString);
		while (matcher.find()) {
			String line = "";
			for (int i = 1; i <= matcher.groupCount(); i++) {
				if (matcher.group(i) != null) {
					line += matcher.group(i);
				}
			}
			results.add(line);
		}
		return results;
	}

	public static int[] getStringPositions(String inputStr, String matchStr) {
		int count = StringUtils.countMatches(inputStr, matchStr);
		if (count > 0) {
			int[] position = new int[count];
			int idx = 0;
			int index = inputStr.indexOf(matchStr);
			position[idx++] = index;
			while ((index = inputStr.indexOf(matchStr, index + 1)) >= 0) {
				position[idx++] = index;
			}
			return position;
		}
		return new int[0];
	}

}