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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.util;

import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.att.aro.core.BaseTest;

public class StringParseTest extends BaseTest {

	private StringParse stringParse;
	private String stringRegex;
	private Pattern pattern;

	@Before
	public void setup() {
		stringParse = (StringParse) context.getBean(StringParse.class);
		stringRegex = "com\\.att\\.aro.+\\.([A-Z].*)\\.(.+)\\(.+:(\\d+)";
		pattern = Pattern.compile(stringRegex);

	}

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

	@Test
	public void testExtract_inside_quotes_sep_by_LF() {
		String stack = "myData = \"I want this in first match\"\n\"this will not be found\"";
		String[] result = stringParse.parse(stack, "\"(.*)\"");
		Assertions.assertThat(result.length).isEqualTo(1);
		Assertions.assertThat(result[0]).isEqualTo("I want this in first match");
	}

	@Test
	public void testExtract_quotes_within_quotes() {
		String stack = "myData = \"I want this in first match\"\"this will be included\"";
		String[] result = stringParse.parse(stack, "\"(.*)\"");
		Assertions.assertThat(result.length).isEqualTo(1);
		Assertions.assertThat(result[0]).isEqualTo("I want this in first match\"\"this will be included");
	}

	@Test
	public void testParseString_Using_Pattern_via_static() {
		String stack = "	at com.att.aro.core.packetreader.pojo.Packet.<init>(Packet.java:40)\n";
		String[] result = stringParse.parse(stack, pattern);
		Assertions.assertThat(result.length).isEqualTo(3);
		//[Packet, <init>, 40]
		Assertions.assertThat(result[0]).isEqualTo("Packet");
		Assertions.assertThat(result[1]).isEqualTo("<init>");
		Assertions.assertThat(result[2]).isEqualTo("40");
	}

	@Test
	public void testParseString_Using_StringRegex_via_static() {
		String stack = "	at com.att.aro.core.packetreader.pojo.Packet.<init>(Packet.java:40)\n";
		String[] result = stringParse.parse(stack, stringRegex);
		Assertions.assertThat(result.length).isEqualTo(3);
		//[Packet, <init>, 40]
		Assertions.assertThat(result[0]).isEqualTo("Packet");
		Assertions.assertThat(result[1]).isEqualTo("<init>");
		Assertions.assertThat(result[2]).isEqualTo("40");
	}

	@Test
	public void testParseString_Using_Pattern() {
		String stack = "	at com.att.aro.core.packetreader.pojo.Packet.<init>(Packet.java:40)\n";
		String[] result = stringParse.parse(stack, pattern);
		Assertions.assertThat(result.length).isEqualTo(3);
		//[Packet, <init>, 40]
		Assertions.assertThat(result[0]).isEqualTo("Packet");
		Assertions.assertThat(result[1]).isEqualTo("<init>");
		Assertions.assertThat(result[2]).isEqualTo("40");
	}

	@Test
	public void testParseString_Using_String() {
		String stack = "	at com.att.aro.core.packetreader.pojo.Packet.<init>(Packet.java:40)\n";
		String[] result = stringParse.parse(stack, stringRegex);
		Assertions.assertThat(result.length).isEqualTo(3);
		//[Packet, <init>, 40]
		Assertions.assertThat(result[0]).isEqualTo("Packet");
		Assertions.assertThat(result[1]).isEqualTo("<init>");
		Assertions.assertThat(result[2]).isEqualTo("40");
	}

}
