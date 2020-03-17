package com.att.aro.core.bestpractice.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.PrivateDataType;
import com.att.aro.core.bestpractice.pojo.TransmissionPrivateDataResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;

public class TransmissionPrivateDataTest extends BaseTest {

	private IBestPractice bestPractice;
	private PacketAnalyzerResult packetAnalyzerResult;
	private TransmissionPrivateDataResult result;
	
	@Before
	public void setUp() throws Exception {
		bestPractice = (TransmissionPrivateDataImpl) context.getBean("transmissionPrivateData");
		packetAnalyzerResult = new PacketAnalyzerResult();
	}

	@Test
	public void testEmptyKeyword() {
		Map<String, String> keywords = new HashMap<String, String>();
		String text = "GET / HTTP/1.1 Host: www.google.com, Connection: keep-alive";
		
		packetAnalyzerResult = getPacketAnalyzerResult(keywords, text);
		result = (TransmissionPrivateDataResult) ((TransmissionPrivateDataImpl) bestPractice).runTest(packetAnalyzerResult);
		
		assertEquals(0, result.getResults().size());
	}
	
	@Test
	public void testDetectingDefaultPatterns() {
		Map<String, String> keywords = new HashMap<String, String>();
		String text = "GET / HTTP/1.1 Host: www.google.com, phone number=(832)-288-7246&Connection: keep-alive&credit=4962754637607482";
		
		packetAnalyzerResult = getPacketAnalyzerResult(keywords, text);
		result = (TransmissionPrivateDataResult) ((TransmissionPrivateDataImpl) bestPractice).runTest(packetAnalyzerResult);
		
		assertEquals(0, result.getResults().size());
	}

	@Test
	public void testPassResult() {
		Map<String, String> keywords = new HashMap<String, String>();
		keywords.put("amazon", PrivateDataType.regex_other.toString());
		String text = "GET / HTTP/1.1 Host: www.google.com, Connection: keep-alive";
		
		packetAnalyzerResult = getPacketAnalyzerResult(keywords, text);
		result = (TransmissionPrivateDataResult) ((TransmissionPrivateDataImpl) bestPractice).runTest(packetAnalyzerResult);
		
		assertEquals(0, result.getResults().size());
		assertEquals(BPResultType.PASS, result.getResultType());
	}
	
	@Test
	public void testWarningResult() {
		Map<String, String> keywords = new HashMap<String, String>();
		keywords.put("google", PrivateDataType.regex_other.toString());
		keywords.put("D1234567", PrivateDataType.regex_other.toString());
		String text = "GET / HTTP/1.1 Host: www.google.com, Connection: keep-alive, ID=D1234567";
		
		packetAnalyzerResult = getPacketAnalyzerResult(keywords, text);
		result = (TransmissionPrivateDataResult) ((TransmissionPrivateDataImpl) bestPractice).runTest(packetAnalyzerResult);
		
		assertEquals(0, result.getResults().size());
		assertEquals(BPResultType.PASS, result.getResultType());
	}
	
	@Test
	public void testFailResult() {
		Map<String, String> keywords = new HashMap<String, String>();
		keywords.put("google", PrivateDataType.regex_other.toString());
		keywords.put("D1234567", PrivateDataType.regex_other.toString());
		keywords.put("GET", PrivateDataType.regex_other.toString());
		keywords.put("HTTP", PrivateDataType.regex_other.toString());
		keywords.put("alive", PrivateDataType.regex_other.toString());
		keywords.put("ID", PrivateDataType.regex_other.toString());
		String text = "GET / HTTP/1.1 Host: www.google.com, Connection: keep-alive, ID=D1234567";
		
		packetAnalyzerResult = getPacketAnalyzerResult(keywords, text);
		result = (TransmissionPrivateDataResult) ((TransmissionPrivateDataImpl) bestPractice).runTest(packetAnalyzerResult);
		
		assertEquals(0, result.getResults().size());
		assertEquals(BPResultType.PASS, result.getResultType());
	}
	
	private PacketAnalyzerResult getPacketAnalyzerResult(Map<String, String> keywords, String text) {
		Session session = mock(Session.class);
		
		InetAddress address = mock(InetAddress.class);
		String ipAddress = "127.0.0.1";
		when(address.getHostAddress()).thenReturn(ipAddress);
		
		int port = 8080;
		
		byte[] storage = text.getBytes();
		when(session.getStorageUl()).thenReturn(storage);
		when(session.getRemoteIP()).thenReturn(address);
		when(session.getRemotePort()).thenReturn(port);
		
		List<Session> sessions = new ArrayList<>();
		sessions.add(session);
		
		packetAnalyzerResult.setSessionlist(sessions);
		packetAnalyzerResult.setDeviceKeywords(keywords);
		
		return packetAnalyzerResult;
	}
}
