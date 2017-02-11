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
package com.att.aro.core.searching.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternInfo {
	
	private String type;
	private String expression;
	private int length;
	private char pivotChar;
	private Map<Character, List<OffsetPair>> charToOffsets;
	
	public class OffsetPair {
		int offsetToHead;
		int offsetToEnd;
		
		public OffsetPair(int offsetToHead, int offsetToEnd) {
			this.offsetToHead = offsetToHead;
			this.offsetToEnd = offsetToEnd;
		}

		public int getOffsetToHead() {
			return offsetToHead;
		}

		public void setOffsetToHead(int offsetToHead) {
			this.offsetToHead = offsetToHead;
		}

		public int getOffsetToEnd() {
			return offsetToEnd;
		}

		public void setOffsetToEnd(int offsetToEnd) {
			this.offsetToEnd = offsetToEnd;
		}
	}
	
	public PatternInfo(String type) {
		this.type = type;
		charToOffsets = new HashMap<Character, List<PatternInfo.OffsetPair>>();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Map<Character, List<OffsetPair>> getCharToOffsets() {
		return charToOffsets;
	}

	public void setCharToOffsets(Map<Character, List<OffsetPair>> charToOffsets) {
		this.charToOffsets = charToOffsets;
	}

	public char getPivotChar() {
		return pivotChar;
	}

	public void setPivotChar(char pivotChar) {
		this.pivotChar = pivotChar;
	}
}
