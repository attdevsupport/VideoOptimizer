/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.searching.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.PrivateDataType;
import com.att.aro.core.searching.ISearchingHandler;
import com.att.aro.core.searching.pojo.SearchingContent;
import com.att.aro.core.searching.pojo.SearchingPattern;
import com.att.aro.core.searching.pojo.SearchingPatternBuilder;
import com.att.aro.core.searching.pojo.SearchingResult;

public class PatternSearchingHandlerTest extends BaseTest {

	@InjectMocks
	private ISearchingHandler searchingHandler;
	
	@Before
	public void setUp() throws Exception {
		searchingHandler = context.getBean("patternSearchingHandler", ISearchingHandler.class);
	}

	@Test
	public void testUnknownSearchingPattern() {
		SearchingPattern pattern = null;
		SearchingContent content = new SearchingContent("abcde");
		
		SearchingResult result = searchingHandler.search(pattern, content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}

	@Test
	public void testEmptyContent() {
		SearchingPatternBuilder pattenBuilder = new SearchingPatternBuilder();
		pattenBuilder.add("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4},-,3,8,-,7,4", PrivateDataType.regex_phone_number.name());
		SearchingContent content = new SearchingContent(null);
		
		SearchingResult result = searchingHandler.search(pattenBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}
	
	@Test
	public void testResultNotFound() {
		SearchingPatternBuilder pattenBuilder = new SearchingPatternBuilder();
		pattenBuilder.add("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4},-,3,8,-,7,4", PrivateDataType.regex_phone_number.name());
		
		String text = "https://www.google.com/login/username=fakeName&password=1234&id=423456-7865";
		SearchingContent content = new SearchingContent(text);
		
		SearchingResult result = searchingHandler.search(pattenBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}
	
	@Test
	public void testResultFound() {
		SearchingPatternBuilder pattenBuilder = new SearchingPatternBuilder();
		pattenBuilder.add("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4},-,3,8,-,7,4", PrivateDataType.regex_phone_number.name());
		
		String text = "https://www.google.com/login/username=fakeName&password=1234&phone=443-237-7431";
		SearchingContent content = new SearchingContent(text);
		
		SearchingResult result = searchingHandler.search(pattenBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(1, result.getWords().size());
		assertEquals("443-237-7431", result.getWords().get(0));
	}
}
