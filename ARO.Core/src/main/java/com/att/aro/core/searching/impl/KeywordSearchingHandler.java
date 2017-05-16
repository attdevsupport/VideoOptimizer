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

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.searching.ISearchingHandler;
import com.att.aro.core.searching.pojo.SearchingContent;
import com.att.aro.core.searching.pojo.SearchingPattern;
import com.att.aro.core.searching.pojo.SearchingResult;
import com.att.aro.core.searching.pojo.SearchingResultBuilder;
import com.att.aro.core.searching.strategy.ISearchingStrategy;

public class KeywordSearchingHandler implements ISearchingHandler {

	@Autowired
	private ISearchingStrategy searchingStrategy;

	@Override
	public SearchingResult search(SearchingPattern pattern, SearchingContent content) {
		if (!isValidPattern(pattern) || !isValidContent(content)) {
			return new SearchingResultBuilder().build();
		}
		return searchingStrategy.applySearch(pattern, content);
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
