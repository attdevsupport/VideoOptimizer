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
package com.att.aro.core.searching.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.CreditCardValidator;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.bestpractice.pojo.PrivateDataType;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.searching.ISearchingHandler;
import com.att.aro.core.searching.pojo.PatternInfo;
import com.att.aro.core.searching.pojo.PatternInfo.OffsetPair;
import com.att.aro.core.searching.pojo.SearchingContent;
import com.att.aro.core.searching.pojo.SearchingPattern;
import com.att.aro.core.searching.pojo.SearchingResult;
import com.att.aro.core.searching.pojo.SearchingResultBuilder;

@SuppressWarnings("deprecation")
public class PatternSearchingHandler implements ISearchingHandler {

	private static final Logger LOGGER = LogManager.getLogger(PatternSearchingHandler.class.getName());

	private CreditCardValidator validator;
	
	public PatternSearchingHandler() {
		initCreditCardValidator();
	}
	
	private void initCreditCardValidator() {
		validator = new CreditCardValidator(CreditCardValidator.AMEX 
											+ CreditCardValidator.DISCOVER 
											+ CreditCardValidator.MASTERCARD
											+ CreditCardValidator.VISA);
	}
	
	@Override
	public SearchingResult search(SearchingPattern pattern, SearchingContent content) {
		SearchingResultBuilder resultBuilder = new SearchingResultBuilder();
		if (!isValidPattern(pattern) || !isValidContent(content)) {
			return resultBuilder.build();
		}

		Map<Character, List<PatternInfo>> pivotCharMap = initPivotCharMap(pattern);
		String text = content.get();

		for(int i = 0; i < text.length(); i++) {
			char pivotChar = text.charAt(i);
			
			if (!hasRelatedPattern(pivotChar, pivotCharMap)) {
				continue;
			}
			List<PatternInfo> relatedPattern = pivotCharMap.get(pivotChar);
			
			for(PatternInfo info : relatedPattern) {
				if (isValidCandidatePattern(info, text, i)) {
					String candidate = getValidCandidatePattern(info, pivotChar, text, i);
					if (candidate == null) {
						continue;
					}
					if (compare(candidate, info)) {
						resultBuilder.add(candidate, info.getType());
					}
				}
			}
		}
		
		return resultBuilder.build();
	}
	
	/**
	 * check if the current scanning character has related possible pattern
	 * @param pivotChar
	 * @param pivotCharMap
	 * @return
	 */
	private boolean hasRelatedPattern(char pivotChar, Map<Character, List<PatternInfo>> pivotCharMap) {
		return pivotCharMap.containsKey(pivotChar);
	}
	
	/**
	 * get valid candidate substring in text
	 * @param info
	 * @param pivotChar
	 * @param text
	 * @param index
	 * @return
	 */
	private String getValidCandidatePattern(PatternInfo info, char pivotChar, String text, int index) {
		int start = getStartIndex(info, pivotChar, index);
		if (start + info.getLength() > text.length()) {
			return null;
		}
		return text.substring(start, start + info.getLength());
	}
	
	/**
	 * get start index in text for potential candidate
	 * @param info
	 * @param pivotChar
	 * @param index
	 * @return
	 */
	private int getStartIndex(PatternInfo info, char pivotChar, int index) {
		return index - info.getCharToOffsets().get(pivotChar).get(0).getOffsetToHead();
	}
	
	/**
	 * compare candidate string and pattern expression
	 * @param candidate
	 * @param info
	 * @return
	 */
	private boolean compare(String candidate, PatternInfo info) {
		Pattern patt = Pattern.compile(info.getExpression());
		Matcher matcher = patt.matcher(candidate);
		if (matcher.matches()) {
			if (isCreditCardType(info)) {
				return validator.isValid(candidate);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * check if the given pattern is a valid candidate around index in the text
	 * 
	 * @param info
	 * @param text
	 * @param index
	 * @return
	 */
	private boolean isValidCandidatePattern(PatternInfo info, String text, int index) {
		Map<Character, List<PatternInfo.OffsetPair>> charToOffsets = info.getCharToOffsets();
		
		char pivotChar = text.charAt(index);
		List<OffsetPair> pairs = charToOffsets.get(pivotChar);
		int start = index - pairs.get(0).getOffsetToHead();
		int end = index + pairs.get(0).getOffsetToEnd();
		
		if (start < 0 || end >= text.length()) {
			return false;
		}
		
		// validate credit card candidate
		if (isCreditCardType(info) && !isValidCreditCardCandidate(start, end, text)) {
			return false;
		}
		
		Set<Character> keySet = charToOffsets.keySet();
		for(char key : keySet) {
			pairs = charToOffsets.get(key);
			for(OffsetPair pair : pairs) {
				if (start + pair.getOffsetToHead() >= text.length()) {
					return false;
				}
				
				if (text.charAt(start + pair.getOffsetToHead()) != key) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * generate in-memory map (pivot character -> list of potential pattern information)
	 * 
	 * @param pattern
	 * @return
	 */
	private Map<Character, List<PatternInfo>> initPivotCharMap(SearchingPattern pattern) {
		Map<Character, List<PatternInfo>> pivotCharMap = new HashMap<Character, List<PatternInfo>>();

		List<String> words = pattern.getWords();
		List<String> types = pattern.getTypes();

		for (int i = 0; i < words.size(); i++) {
			PatternInfo info = parse(words.get(i), types.get(i));
			if (info == null) {
				continue;
			}

			if (!pivotCharMap.containsKey(info.getPivotChar())) {
				pivotCharMap.put(info.getPivotChar(), new LinkedList<PatternInfo>());
			}
			pivotCharMap.get(info.getPivotChar()).add(info);
		}

		return pivotCharMap;
	}
	
	/**
	 * parse value from properties file into PatternInfo
	 * the value will always follow the format: 
	 * [regular expression],[character],[offset to head],[offset to end],[character],[offset to head],[offset to end]..;
	 * 
	 * @param patttern
	 * @return
	 */
	private PatternInfo parse(String patttern, String dataType) {
		try {
			String[] tokens = patttern.split(TraceDataConst.PrivateData.COLUMN_DELIMITER);
			if (tokens.length < 4) {
				throw new IllegalArgumentException("Regex should have at least one pivot character setup properly.");
			}
			return getPatternInfo(tokens, dataType);
		} catch (Exception e) {
			StringBuilder builder = new StringBuilder(100);
			builder.append("Invalid regex pattern in properties file, caused by: ")
				   .append(e.getMessage())
				   .append(", pattern: ")
				   .append(patttern);
			LOGGER.error(builder.toString());
			return null;
		}
	}
	
	/**
	 * get PatternInfo based on parsed tokens
	 * @param tokens
	 * @param dataType
	 * @return
	 */
	private PatternInfo getPatternInfo(String[] tokens, String dataType) throws Exception {
		PatternInfo info = new PatternInfo(dataType);	// set data type
		
		info.setExpression(tokens[0].trim());	// set regular expression
		
		Map<Character, List<PatternInfo.OffsetPair>> charToOffsets = info.getCharToOffsets();	// set char to offsets map
		boolean isFirstTime = true;
		for(int i = 1; i < tokens.length - 2; i += 3) {
			if (tokens[i].length() != 1) {
				throw new IllegalArgumentException("Length of pivot character should be 1.");
			}
			
			int offsetToHead = Integer.parseInt(tokens[i + 1].trim());
			int offsetToEnd = Integer.parseInt(tokens[i + 2].trim());
			OffsetPair pair = info.new OffsetPair(offsetToHead, offsetToEnd);
			if (!charToOffsets.containsKey(tokens[i].charAt(0))) {
				charToOffsets.put(tokens[i].charAt(0), new LinkedList<PatternInfo.OffsetPair>());
			}
			charToOffsets.get(tokens[i].charAt(0)).add(pair);
			
			if (isFirstTime) {
				info.setLength(offsetToHead + offsetToEnd + 1);	// set length
				info.setPivotChar(tokens[i].charAt(0));	// set pivot char
				isFirstTime = false;
			}
		}
		
		return info;
	}
	
	/**
	 * check if the pattern is credit card type
	 * @param info
	 * @return
	 */
	private boolean isCreditCardType(PatternInfo info) {
		return PrivateDataType.regex_credit_card_number.toString().equals(info.getType());
	}
	
	/**
	 * check if the input is a valid credit card candidate
	 * @param start
	 * @param end
	 * @param info
	 * @param text
	 * @return
	 */
	private boolean isValidCreditCardCandidate(int start, int end, String text) {
		// check one character before the pattern
		if (start - 1 >= 0 && Character.isDigit(text.charAt(start - 1))) {
			return false;
		}
		// check one character after the pattern
		if (end + 1 < text.length() && Character.isDigit(text.charAt(end + 1))) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isValidPattern(SearchingPattern pattern) {
		return pattern != null && !pattern.isEmpty();
	}

	@Override
	public boolean isValidContent(SearchingContent content) {
		return content != null && !content.isEmpty();
	}
}
