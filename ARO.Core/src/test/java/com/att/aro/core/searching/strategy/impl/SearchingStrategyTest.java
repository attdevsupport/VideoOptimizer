package com.att.aro.core.searching.strategy.impl;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.PrivateDataType;
import com.att.aro.core.searching.pojo.SearchingPatternBuilder;
import com.att.aro.core.searching.pojo.SearchingContent;
import com.att.aro.core.searching.pojo.SearchingResult;
import com.att.aro.core.searching.strategy.ISearchingStrategy;

public class SearchingStrategyTest extends BaseTest {
	
	@InjectMocks
	private ISearchingStrategy searchStrategy;

	@Before
	public void setUp() throws Exception {
		searchStrategy = context.getBean(ISearchingStrategy.class);
	}
	
	@Test
	public void testEmptyPattern() {
		SearchingContent content = new SearchingContent("abcabeabd");
		
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("", null);
		
		SearchingResult result = searchStrategy.applySearch(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}
	
	@Test
	public void testEmptyContent() {
		SearchingContent content = new SearchingContent("");
		
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("abc", PrivateDataType.regex_credit_card_number.name());
		
		SearchingResult result = searchStrategy.applySearch(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}

	@Test
	public void testSinglePatternMatchFound() {
		SearchingContent content = new SearchingContent("abcabeabd");
		
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("abe", PrivateDataType.regex_credit_card_number.name());
		
		SearchingResult result = searchStrategy.applySearch(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(1, result.getWords().size());
		assertEquals("abe", result.getWords().get(0));
	}
	
	@Test
	public void testSinglePatternMatchNotFound() {
		SearchingContent content = new SearchingContent("abcabeabd");
		
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("abf", PrivateDataType.regex_credit_card_number.name());
		
		SearchingResult result = searchStrategy.applySearch(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}
	
	@Test
	public void testMultiPatternMatchFound() {
		SearchingContent content = new SearchingContent("abcde");
		
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("abf", PrivateDataType.regex_credit_card_number.name())
					  .add("bcd", PrivateDataType.regex_credit_card_number.name())
					  .add("def", PrivateDataType.regex_credit_card_number.name());
		
		SearchingResult result = searchStrategy.applySearch(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(1, result.getWords().size());
	}
	
	@Test
	public void testMultiPatternMatchNotFound() {
		SearchingContent content = new SearchingContent("abcde");
		
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		patternBuilder.add("abe", PrivateDataType.regex_credit_card_number.name())
					  .add("bca", PrivateDataType.regex_credit_card_number.name())
					  .add("def", PrivateDataType.regex_credit_card_number.name());
		
		SearchingResult result = searchStrategy.applySearch(patternBuilder.build(), content);
		
		assertNotNull(result);
		assertEquals(0, result.getWords().size());
	}

}
