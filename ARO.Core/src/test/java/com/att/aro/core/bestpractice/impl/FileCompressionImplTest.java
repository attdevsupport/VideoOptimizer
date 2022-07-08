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
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.packetanalysis.pojo.Burst;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.packetreader.pojo.PacketDirection;

public class FileCompressionImplTest extends BaseTest{
	
	FileCompressionImpl FileCompressionImpl;
	Packet packet;
	Burst burst01;
	PacketAnalyzerResult tracedata;

	List<Session> sessionlist;
	Session session;
	HttpRequestResponseInfo req;

	/**
	 * tests for a FAIL
	 */
	@Test   
	public void runTest_1(){	
		tracedata = mock(PacketAnalyzerResult.class);
		
		session = mock(Session.class);
		sessionlist = new ArrayList<Session>();
		sessionlist.add(session);
		req = mock(HttpRequestResponseInfo.class);
		req.setDirection(HttpDirection.REQUEST);
		List<HttpRequestResponseInfo> reqList = new ArrayList<HttpRequestResponseInfo>();
		reqList.add(req);
		
		Mockito.when(session.getRequestResponseInfo()).thenReturn(reqList);
		Mockito.when(session.getDomainName()).thenReturn("mock.domain.name");
		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionlist);

		// HttpDirection.REQUEST
		Mockito.when(req.getDirection()).thenReturn(HttpDirection.REQUEST);
		Mockito.when(req.getObjName()).thenReturn("mock.obj.name");

		// condition 1
		Mockito.when(req.getPacketDirection()).thenReturn(PacketDirection.DOWNLINK);
		// condition 2
		Mockito.when(req.getContentLength()).thenReturn(1001024);
		// condition 3 & 4
		Mockito.when(req.getContentType()).thenReturn("message/http"); // "application/ecmascript" "application/json" "application/javascript" "text/javascript" "message/http"

		Mockito.when(req.getContentEncoding()).thenReturn("identity");
		
		FileCompressionImpl = (FileCompressionImpl)context.getBean("textFileCompression");
		AbstractBestPracticeResult result = FileCompressionImpl.runTest(tracedata);

		result = FileCompressionImpl.runTest(tracedata);

		assertEquals("Sending compressed files over the network will speed delivery, and unzipping files on a device is a very low overhead operation. Ensure that all your text files are compressed while being sent over the network.",result.getAboutText());
		assertEquals("FILE_COMPRESSION", result.getBestPracticeType().toString());
		assertEquals("Text File Compression",result.getDetailTitle());
		assertEquals("File Download: Text File Compression",result.getOverviewTitle());
		assertEquals(ApplicationConfig.getInstance().getAppShortName() + " detected 100% (977KB of 977KB) of text files were sent without compression. Adding compression will speed the delivery of your content to your customers. (Note: Only files larger than 850 bytes are flagged.)",result.getResultText());
		assertEquals("FAIL",result.getResultType().toString());		

	}
	
	/**
	 * tests for PASS
	 */
	@Test   
	public void runTest_2(){	
		tracedata = mock(PacketAnalyzerResult.class);
		
		session = mock(Session.class);
		sessionlist = new ArrayList<Session>();
		sessionlist.add(session);
		req = mock(HttpRequestResponseInfo.class);
		req.setDirection(HttpDirection.REQUEST);
		List<HttpRequestResponseInfo> reqList = new ArrayList<HttpRequestResponseInfo>();
		reqList.add(req);
		
		Mockito.when(session.getRequestResponseInfo()).thenReturn(reqList);
		Mockito.when(session.getDomainName()).thenReturn("mock.domain.name");
		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionlist);

		// HttpDirection.REQUEST
		Mockito.when(req.getDirection()).thenReturn(HttpDirection.REQUEST);
		Mockito.when(req.getObjName()).thenReturn("mock.obj.name");

		// condition 1
		Mockito.when(req.getPacketDirection()).thenReturn(PacketDirection.DOWNLINK);
		// condition 2
		Mockito.when(req.getContentLength()).thenReturn(1);
		// condition 3 & 4
		Mockito.when(req.getContentType()).thenReturn("application/.something.xml"); // "application/ecmascript" "application/json" "application/javascript" "text/javascript" "message/http"

		Mockito.when(req.getContentEncoding()).thenReturn("identity");
				
		FileCompressionImpl = (FileCompressionImpl)context.getBean("textFileCompression");
		AbstractBestPracticeResult result = FileCompressionImpl.runTest(tracedata);

		assertEquals("Sending compressed files over the network will speed delivery, and unzipping files on a device is a very low overhead operation. Ensure that all your text files are compressed while being sent over the network.",result.getAboutText());
		assertEquals("FILE_COMPRESSION", result.getBestPracticeType().toString());
		assertEquals("Text File Compression",result.getDetailTitle());
		assertEquals("File Download: Text File Compression",result.getOverviewTitle());
		assertEquals(ApplicationConfig.getInstance().getAppShortName() + " detected 0 text files above 850 bytes were sent without compression. Adding compression will speed the delivery of your content to your customers. (Note: Only files larger than 850 bytes are flagged.)",result.getResultText());
		assertEquals("PASS",result.getResultType().toString());		
		
	}
	
	/**
	 * tests for WARNING
	 */
	@Test   
	public void runTest_3(){	
		tracedata = mock(PacketAnalyzerResult.class);
		
		session = mock(Session.class);
		sessionlist = new ArrayList<Session>();
		sessionlist.add(session);
		req = mock(HttpRequestResponseInfo.class);
		req.setDirection(HttpDirection.REQUEST);
		List<HttpRequestResponseInfo> reqList = new ArrayList<HttpRequestResponseInfo>();
		reqList.add(req);
		
		Mockito.when(session.getRequestResponseInfo()).thenReturn(reqList);
		Mockito.when(session.getDomainName()).thenReturn("mock.domain.name");
		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionlist);

		// HttpDirection.REQUEST
		Mockito.when(req.getDirection()).thenReturn(HttpDirection.REQUEST);
		Mockito.when(req.getObjName()).thenReturn("mock.obj.name");

		// condition 1
		Mockito.when(req.getPacketDirection()).thenReturn(PacketDirection.DOWNLINK);
		// condition 2
		Mockito.when(req.getContentLength()).thenReturn(900);
		// condition 3 & 4
		Mockito.when(req.getContentType()).thenReturn("application/.something.xml"); // "application/ecmascript" "application/json" "application/javascript" "text/javascript" "message/http"

		Mockito.when(req.getContentEncoding()).thenReturn("identity");
		
		
		FileCompressionImpl = (FileCompressionImpl)context.getBean("textFileCompression");
		AbstractBestPracticeResult result = FileCompressionImpl.runTest(tracedata);

		assertEquals("Sending compressed files over the network will speed delivery, and unzipping files on a device is a very low overhead operation. Ensure that all your text files are compressed while being sent over the network.",result.getAboutText());
		assertEquals("FILE_COMPRESSION", result.getBestPracticeType().toString());
		assertEquals("Text File Compression",result.getDetailTitle());
		assertEquals("File Download: Text File Compression",result.getOverviewTitle());
		assertEquals(ApplicationConfig.getInstance().getAppShortName() + " detected 100% (0KB of 0KB) of text files were sent without compression. Adding compression will speed the delivery of your content to your customers. (Note: Only files larger than 850 bytes are flagged.)",result.getResultText());
		assertEquals("WARNING",result.getResultType().toString());
	}


}
