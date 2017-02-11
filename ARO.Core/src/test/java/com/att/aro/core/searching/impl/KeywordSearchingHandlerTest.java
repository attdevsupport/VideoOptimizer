package com.att.aro.core.searching.impl;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.PrivateDataType;
import com.att.aro.core.searching.ISearchingHandler;
import com.att.aro.core.searching.pojo.SearchingPatternBuilder;
import com.att.aro.core.searching.pojo.SearchingContent;
import com.att.aro.core.searching.pojo.SearchingPattern;
import com.att.aro.core.searching.pojo.SearchingResult;
import com.att.aro.core.searching.strategy.ISearchingStrategy;

public class KeywordSearchingHandlerTest extends BaseTest {

	@InjectMocks
	private ISearchingHandler searchingHandler;
	
	@Mock
	private ISearchingStrategy searchingStrategy;
	
	@Before
	public void setUp() throws Exception {
		searchingStrategy = context.getBean(ISearchingStrategy.class);
		searchingHandler = context.getBean("keywordSearchingHandler", ISearchingHandler.class);
	}

	@Test
	public void testSingleSearchingPattern() {
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("abc", PrivateDataType.regex_credit_card_number.name());
		
		SearchingContent content = new SearchingContent("abcde");
		
		SearchingResult result = searchingHandler.search(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals("abc", result.getWords().get(0));
	}

	@Test
	public void testMultiSearchingPattern() {
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("abc", PrivateDataType.regex_credit_card_number.name())
					  .add("bcd", PrivateDataType.regex_credit_card_number.name())
					  .add("def", PrivateDataType.regex_credit_card_number.name());
		
		SearchingContent content = new SearchingContent("abcde");
		
		SearchingResult result = searchingHandler.search(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(2, result.getWords().size());
	}
	
	@Test
	public void testUnknownSearchingPattern() {
		SearchingPattern pattern = null;
		SearchingContent content = new SearchingContent("abcde");
		
		SearchingResult result = searchingHandler.search(pattern, content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}
}
