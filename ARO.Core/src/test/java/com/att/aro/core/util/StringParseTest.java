package com.att.aro.core.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;


public class StringParseTest {

	
	// #USP-X-MEDIA:BANDWIDTH=1000,AVERAGE-BANDWIDTH=1000,TYPE=SUBTITLES,GROUP-ID="subtitle-webvtt",LANGUAGE="en-US",NAME="sdh",AUTOSELECT=YES,CODECS=""
	
	@Test
	public void testFindLabeledDataFromString_whenEmptyQuotesInString_thenReturnEmptyString() throws Exception {
		String sData = "#USP-X-MEDIA:BANDWIDTH=1000,AVERAGE-BANDWIDTH=1000,TYPE=SUBTITLES,GROUP-ID=\"subtitle-webvtt\",LANGUAGE=\"en-US\",NAME=\"sdh\",AUTOSELECT=YES,CODECS=\"\"";
		String result = StringParse.findLabeledDataFromString("CODECS=", "\"", sData);
		Assertions.assertThat(result).isEmpty();
	}

	
	@Test
	public void testFindLabeledDataFromString_whenNoDelimiter_thenReturnsValue() throws Exception {
		Assertions.assertThat(StringParse.findLabeledDataFromString("EXTINF:", " 6: #EXTINF:9.002666, no desc"))
		.isEqualTo("9") 
		;
		
	}

	@Test
	public void testFindLabeledDoubleFromString() throws Exception {
		Assertions.assertThat(StringParse.findLabeledDoubleFromString("EXTINF:", ",", " 6: #EXTINF:9.002665, no desc"))
		.isEqualTo(9.002665)
		;
	}



}
