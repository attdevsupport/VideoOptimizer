package com.att.aro.core.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;


public class StringParseTest {

	@Test
	public void testFindLabeledDataFromString_whenNoDelimiter_thenReturnsValue() throws Exception {
		String val = StringParse.findLabeledDataFromString("EXTINF:", " 6: #EXTINF:9.002666, no desc");
		Assertions.assertThat(StringParse.findLabeledDataFromString("EXTINF:", " 6: #EXTINF:9.002666, no desc"))
		.isEqualTo("9") 
		;
		
	}

	@Test
	public void testFindLabeledDoubleFromString() throws Exception {
		Double val = StringParse.findLabeledDoubleFromString("EXTINF:", ",", " 6: #EXTINF:9.002665, no desc");
		Assertions.assertThat(StringParse.findLabeledDoubleFromString("EXTINF:", ",", " 6: #EXTINF:9.002665, no desc"))
		.isEqualTo(9.002665)
		;
	}



}
