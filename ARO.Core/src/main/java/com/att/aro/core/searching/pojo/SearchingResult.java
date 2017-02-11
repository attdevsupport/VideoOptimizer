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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * immutable searching result class contains list of string (keywords found in content)
 * 
 *
 *
 */
public class SearchingResult {
	private final List<String> words;
	private final List<String> types;
	
	SearchingResult() {
		words = new LinkedList<String>();
		types = new LinkedList<String>();
	}
	
	public List<String> getWords() {
		return new ArrayList<>(words);
	}
	
	public List<String> getTypes() {
		return new ArrayList<>(types);
	}
	
	void add(String word, String type) {
		words.add(word);
		types.add(type);
	}
	
	public boolean isEmpty() {
		return words.isEmpty();
	}
}
