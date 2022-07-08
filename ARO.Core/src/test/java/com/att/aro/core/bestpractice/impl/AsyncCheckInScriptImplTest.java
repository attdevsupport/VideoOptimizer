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
package com.att.aro.core.bestpractice.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;

public class AsyncCheckInScriptImplTest extends BaseTest {
	PacketAnalyzerResult tracedata;
	Session session01;
	Session session02;
	HttpRequestResponseInfo httpRequestInfo01;
	HttpRequestResponseInfo httpRequestInfo02;
	HttpRequestResponseInfo httpRequestInfo03;
	HttpRequestResponseInfo httpRequestInfo04;

	PacketInfo pktInfo01;
	PacketInfo pktInfo02;
	AsyncCheckInScriptImpl asyncCheckScriptImpl;
	IHttpRequestResponseHelper reqhelper;
	String htmlString = "<html><head><title>First parse</title><script src=\"myscripts.js\"></script></head>"
			+ "<body><p>Parsed HTML into a doc.</p></body></html>";
	String htmlString2 = "<html><head><title>First parse</title><script async src=\"myscripts.js\"></script></head>"
			+ "<body><p>Parsed HTML into a doc.</p></body></html>";

	@Before
	public void setup() {
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		session01 = Mockito.mock(Session.class);
		session02 = Mockito.mock(Session.class);
		pktInfo01 = Mockito.mock(PacketInfo.class);
		pktInfo02 = Mockito.mock(PacketInfo.class);
		httpRequestInfo01 = Mockito.mock(HttpRequestResponseInfo.class);
		httpRequestInfo02 = Mockito.mock(HttpRequestResponseInfo.class);
		httpRequestInfo03 = Mockito.mock(HttpRequestResponseInfo.class);
		httpRequestInfo04 = Mockito.mock(HttpRequestResponseInfo.class);
		reqhelper = Mockito.mock(IHttpRequestResponseHelper.class);
		asyncCheckScriptImpl = (AsyncCheckInScriptImpl) context.getBean("async");

	}

	@Test
	public void runTest_resIsFailSync() {

		Mockito.when(httpRequestInfo01.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(httpRequestInfo01.getContentType()).thenReturn("abc");
		Mockito.when(httpRequestInfo01.getContentLength()).thenReturn(1);
		Mockito.when(httpRequestInfo02.getDirection()).thenReturn(HttpDirection.REQUEST);
		Mockito.when(httpRequestInfo03.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(httpRequestInfo04.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(reqhelper.isHtml(any(String.class))).thenReturn(true);
		try {
			Mockito.when(reqhelper.getContentString(any(HttpRequestResponseInfo.class), any(Session.class)))
					.thenReturn(htmlString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<HttpRequestResponseInfo> value = new ArrayList<HttpRequestResponseInfo>();
		value.add(httpRequestInfo01);
		value.add(httpRequestInfo02);
		value.add(httpRequestInfo03);
		value.add(httpRequestInfo04);

		Mockito.when(session01.getRequestResponseInfo()).thenReturn(value);
		List<Session> sessionList = new ArrayList<Session>();

		Mockito.when(session01.getDomainName()).thenReturn("www.google.com");
		sessionList.add(session01);
		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionList);
		asyncCheckScriptImpl.setHttpRequestResponseHelper(reqhelper);
		AbstractBestPracticeResult testResult = asyncCheckScriptImpl.runTest(tracedata);
		assertEquals(BPResultType.FAIL, testResult.getResultType());

	}

	@Test
	public void runTest_resIsPassAsync() {
		Mockito.when(httpRequestInfo01.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(httpRequestInfo01.getContentType()).thenReturn("abc");
		Mockito.when(httpRequestInfo01.getContentLength()).thenReturn(1);
		Mockito.when(httpRequestInfo02.getDirection()).thenReturn(HttpDirection.REQUEST);
		Mockito.when(httpRequestInfo03.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(httpRequestInfo04.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(reqhelper.isHtml(any(String.class))).thenReturn(true);
		try {
			Mockito.when(reqhelper.getContentString(any(HttpRequestResponseInfo.class), any(Session.class)))
					.thenReturn(htmlString2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<HttpRequestResponseInfo> value = new ArrayList<HttpRequestResponseInfo>();
		value.add(httpRequestInfo01);
		value.add(httpRequestInfo02);
		value.add(httpRequestInfo03);
		value.add(httpRequestInfo04);

		Mockito.when(session01.getRequestResponseInfo()).thenReturn(value);
		List<Session> sessionList = new ArrayList<Session>();

		Mockito.when(session01.getDomainName()).thenReturn("www.google.com");
		sessionList.add(session01);
		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionList);
		asyncCheckScriptImpl.setHttpRequestResponseHelper(reqhelper);
		AbstractBestPracticeResult testResult = asyncCheckScriptImpl.runTest(tracedata);
		assertEquals(BPResultType.PASS, testResult.getResultType());
	}

	@Test
	public void runTest_resThrowException() throws Exception {
		Mockito.when(httpRequestInfo01.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(httpRequestInfo01.getContentType()).thenReturn("abc");
		Mockito.when(httpRequestInfo01.getContentLength()).thenReturn(1);
		Mockito.when(httpRequestInfo02.getDirection()).thenReturn(HttpDirection.REQUEST);
		Mockito.when(httpRequestInfo03.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(httpRequestInfo04.getDirection()).thenReturn(HttpDirection.RESPONSE);
		Mockito.when(reqhelper.isHtml(any(String.class))).thenReturn(true);

		List<HttpRequestResponseInfo> value = new ArrayList<HttpRequestResponseInfo>();
		value.add(httpRequestInfo01);
		value.add(httpRequestInfo02);
		value.add(httpRequestInfo03);
		value.add(httpRequestInfo04);

		Mockito.when(session01.getRequestResponseInfo()).thenReturn(value);
		List<Session> sessionList = new ArrayList<Session>();

		Mockito.when(session01.getDomainName()).thenReturn("www.google.com");
		sessionList.add(session01);
		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionList);

		asyncCheckScriptImpl.setHttpRequestResponseHelper(reqhelper);
		try {
			Mockito.when(reqhelper.getContentString(any(HttpRequestResponseInfo.class), any(Session.class)))
					.thenThrow(new Exception());
		} catch (Exception e) {
			e.printStackTrace();
		}

		asyncCheckScriptImpl.setHttpRequestResponseHelper(reqhelper);
		AbstractBestPracticeResult testResult = asyncCheckScriptImpl.runTest(tracedata);
		assertEquals(BPResultType.PASS, testResult.getResultType());

	}
}
