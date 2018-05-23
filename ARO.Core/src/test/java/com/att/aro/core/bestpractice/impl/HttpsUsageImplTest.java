package com.att.aro.core.bestpractice.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.HttpsUsageEntry;
import com.att.aro.core.bestpractice.pojo.HttpsUsageResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.TCPPacket;

public class HttpsUsageImplTest extends BaseTest {
	private HttpsUsageImpl httpsUsageImpl;
	private PacketAnalyzerResult pktAnalyzerResult;
	private HttpsUsageResult httpsUsageResult;
	private List<HttpsUsageEntry> httpsUsageEntries;

	@Before
	public void setup() {
		httpsUsageImpl = (HttpsUsageImpl) context.getBean("httpsUsage");
		pktAnalyzerResult = new PacketAnalyzerResult();
	}

	@Test
	public void testParentDomainNameUsed_DifferentSubdomains() {
		pktAnalyzerResult = domainNameTestSetup("157.56.19.80", "216.58.194.196", "www.gstatic.com", "ssl.gstatic.com",
				"www.arotest.com");
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		domainNameTestVerification(httpsUsageEntries);
	}

	@Test
	public void testParentDomainNameUsed_MultiLevelDomainNames() {
		pktAnalyzerResult = domainNameTestSetup("157.56.19.80", "216.58.194.196", "bogusa.bogusb.gstatic.com",
				"bogusa.gstatic.com", "www.arotest.com");
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		domainNameTestVerification(httpsUsageEntries);
	}

	@Test
	public void testDomainNameFieldShowsBlankWhenDomainNameIsIP() {
		Session session1 = mock(Session.class);
		Session session2 = mock(Session.class);
		InetAddress ipv4 = mock(InetAddress.class);
		when(ipv4.getHostAddress()).thenReturn("157.56.19.80");
		InetAddress ipv6 = mock(InetAddress.class);
		when(ipv6.getHostAddress()).thenReturn("2001:cdba:0000:0000:0000:0000:3257:9652");
		when(session1.getRemoteIP()).thenReturn(ipv4);
		when(session1.getDomainName()).thenReturn("www.hotmail.com");
		when(session2.getRemoteIP()).thenReturn(ipv6);
		when(session2.getDomainName()).thenReturn("2001:cdba:0000:0000:0000:0000:3257:9652");
		TCPPacket tcpPacket1 = mock(TCPPacket.class);
		TCPPacket tcpPacket2 = mock(TCPPacket.class);
		when(tcpPacket1.containsSSLRecord()).thenReturn(false);
		when(tcpPacket2.containsSSLRecord()).thenReturn(false);
		PacketInfo packetInfo1 = mock(PacketInfo.class);
		PacketInfo packetInfo2 = mock(PacketInfo.class);
		when(packetInfo1.getPayloadLen()).thenReturn(1);
		when(packetInfo1.getPacket()).thenReturn(tcpPacket1);
		when(packetInfo2.getPayloadLen()).thenReturn(1);
		when(packetInfo2.getPacket()).thenReturn(tcpPacket2);
		List<PacketInfo> packetsInfo1 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo2 = new ArrayList<PacketInfo>();
		packetsInfo1.add(packetInfo1);
		packetsInfo2.add(packetInfo2);
		when(session1.getPackets()).thenReturn(packetsInfo1);
		when(session2.getPackets()).thenReturn(packetsInfo2);
		List<Session> sessions = new ArrayList<Session>();
		sessions.add(session1);
		sessions.add(session2);
		pktAnalyzerResult.setSessionlist(sessions);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals("hotmail.com", httpsUsageEntries.get(0).getParentDomainName());
			assertEquals("", httpsUsageEntries.get(1).getParentDomainName());
		} else {
			assertEquals("", httpsUsageEntries.get(0).getParentDomainName());
			assertEquals("hotmail.com", httpsUsageEntries.get(1).getParentDomainName());
		}
	}

	@Test
	public void testParentDomainNameUsed_IPDomainNameMix() {
		pktAnalyzerResult = domainNameTestSetup("157.56.19.80", "216.58.194.196", "157.56.19.80", "www.gstatic.com",
				"www.arotest.com");
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		domainNameTestVerification(httpsUsageEntries);
	}

	@Test
	public void testDomainNameNoSeparator() {
		String domainNameUnderTest = "bzsvdcwfxtqfy";
		pktAnalyzerResult = domainNameTestSetup("157.56.19.80", "216.58.194.196", "157.56.19.80", "www.gstatic.com",
				domainNameUnderTest);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		domainNameTestVerification(httpsUsageEntries, domainNameUnderTest);
	}

	@Test
	public void testNullDomainName() {
		String domainNameUnderTest = null;
		pktAnalyzerResult = domainNameTestSetup("157.56.19.80", "216.58.194.196", "157.56.19.80", "www.gstatic.com",
				domainNameUnderTest);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		domainNameTestVerification(httpsUsageEntries, "");
	}

	@Test
	public void testEmptyDomainName() {
		String domainNameUnderTest = "";
		pktAnalyzerResult = domainNameTestSetup("157.56.19.80", "216.58.194.196", "157.56.19.80", "www.gstatic.com",
				domainNameUnderTest);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		domainNameTestVerification(httpsUsageEntries, domainNameUnderTest);
	}

	@Test
	public void testUDPSessionNotConsidered() {
		Session session1 = mock(Session.class);
		Session session2 = mock(Session.class);
		InetAddress ipAddr = mock(InetAddress.class);
		when(ipAddr.getHostAddress()).thenReturn("157.56.19.80");
		when(session1.isUDP()).thenReturn(true);
		when(session2.getRemoteIP()).thenReturn(ipAddr);
		when(session2.getDomainName()).thenReturn("www.hotmail.com");
		TCPPacket tcpPacket1 = mock(TCPPacket.class);
		TCPPacket tcpPacket2 = mock(TCPPacket.class);
		when(tcpPacket1.containsSSLRecord()).thenReturn(false);
		when(tcpPacket2.containsSSLRecord()).thenReturn(false);
		PacketInfo packetInfo_tcp1 = mock(PacketInfo.class);
		PacketInfo packetInfo_tcp2 = mock(PacketInfo.class);
		when(packetInfo_tcp1.getPayloadLen()).thenReturn(1);
		when(packetInfo_tcp1.getPacket()).thenReturn(tcpPacket1);
		when(packetInfo_tcp2.getPayloadLen()).thenReturn(1);
		when(packetInfo_tcp2.getPacket()).thenReturn(tcpPacket2);
		List<PacketInfo> packetsInfo = new ArrayList<PacketInfo>();
		packetsInfo.add(packetInfo_tcp1);
		packetsInfo.add(packetInfo_tcp2);
		when(session2.getPackets()).thenReturn(packetsInfo);
		List<Session> sessions = new ArrayList<Session>();
		sessions.add(session1);
		sessions.add(session2);
		pktAnalyzerResult.setSessionlist(sessions);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(1, httpsUsageEntries.size());
	}

	@Test
	public void testIsSSLSessionsAlteration() {
		Session session1 = mock(Session.class);
		Session session2 = mock(Session.class);
		Session session3 = mock(Session.class);
		InetAddress ipAddrA = mock(InetAddress.class);
		InetAddress ipAddrB = mock(InetAddress.class);
		when(ipAddrA.getHostAddress()).thenReturn("157.56.19.80");
		when(ipAddrB.getHostAddress()).thenReturn("216.58.194.196");
		when(session1.getRemoteIP()).thenReturn(ipAddrA);
		when(session1.getDomainName()).thenReturn("www.gstatic.com");
		when(session2.getRemoteIP()).thenReturn(ipAddrA);
		when(session2.getDomainName()).thenReturn("ssl.gstatic.com");
		when(session3.getRemoteIP()).thenReturn(ipAddrB);
		when(session3.getDomainName()).thenReturn("www.arotest.com");
		TCPPacket tcpPacket1_1 = mock(TCPPacket.class);
		TCPPacket tcpPacket1_2 = mock(TCPPacket.class);
		TCPPacket tcpPacket2 = mock(TCPPacket.class);
		TCPPacket tcpPacket3 = mock(TCPPacket.class);
		when(tcpPacket1_1.containsSSLRecord()).thenReturn(false);
		when(tcpPacket1_2.containsSSLRecord()).thenReturn(true);
		when(tcpPacket2.containsSSLRecord()).thenReturn(false);
		when(tcpPacket3.containsSSLRecord()).thenReturn(true);
		PacketInfo packetInfo1_1 = mock(PacketInfo.class);
		PacketInfo packetInfo1_2 = mock(PacketInfo.class);
		PacketInfo packetInfo2 = mock(PacketInfo.class);
		PacketInfo packetInfo3 = mock(PacketInfo.class);
		when(packetInfo1_1.getPayloadLen()).thenReturn(1);
		when(packetInfo1_1.getPacket()).thenReturn(tcpPacket1_1);
		when(packetInfo1_2.getPayloadLen()).thenReturn(1);
		when(packetInfo1_2.getPacket()).thenReturn(tcpPacket1_2);
		when(packetInfo2.getPayloadLen()).thenReturn(1);
		when(packetInfo2.getPacket()).thenReturn(tcpPacket2);
		when(packetInfo3.getPayloadLen()).thenReturn(1);
		when(packetInfo3.getPacket()).thenReturn(tcpPacket3);
		List<PacketInfo> packetsInfo1 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo2 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo3 = new ArrayList<PacketInfo>();
		packetsInfo1.add(packetInfo1_1);
		packetsInfo1.add(packetInfo1_2);
		packetsInfo2.add(packetInfo2);
		packetsInfo3.add(packetInfo3);
		when(session1.getPackets()).thenReturn(packetsInfo1);
		when(session2.getPackets()).thenReturn(packetsInfo2);
		when(session3.getPackets()).thenReturn(packetsInfo3);
		List<Session> sessions = new ArrayList<Session>();
		sessions.add(session1);
		sessions.add(session2);
		sessions.add(session3);
		pktAnalyzerResult.setSessionlist(sessions);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(1, httpsUsageEntries.size());
		assertEquals("gstatic.com", httpsUsageEntries.get(0).getParentDomainName());
	}

	@Test
	public void testNonSSLSessionNoPayload() {
		PacketAnalyzerResult pktAnalyzerResult = sessionPacketsPayloadTestSetup(0, 0, 0, 0);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(0, httpsUsageEntries.size());
	}

	@Test
	public void testNonSSLSessionHasPayload() {
		PacketAnalyzerResult pktAnalyzerResult = sessionPacketsPayloadTestSetup(1, 0, 1, 1);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
	}

	@Test
	public void testConnectionsGroupedByIP() {
		pktAnalyzerResult = resultTestSetup(8);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(3, httpsUsageEntries.size());
	}

	@Test
	public void testNumHttpConnections() {
		pktAnalyzerResult = resultTestSetup(5);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals(3, httpsUsageEntries.get(0).getTotalNumHttpConnections());
			assertEquals(2, httpsUsageEntries.get(1).getTotalNumHttpConnections());
		} else {
			assertEquals(2, httpsUsageEntries.get(0).getTotalNumHttpConnections());
			assertEquals(3, httpsUsageEntries.get(1).getTotalNumHttpConnections());
		}
	}

	@Test
	public void testNumConnections() {
		pktAnalyzerResult = resultTestSetup(5);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals(3, httpsUsageEntries.get(0).getTotalNumConnections());
			assertEquals(4, httpsUsageEntries.get(1).getTotalNumConnections());
		} else {
			assertEquals(4, httpsUsageEntries.get(0).getTotalNumConnections());
			assertEquals(3, httpsUsageEntries.get(1).getTotalNumConnections());
		}
	}

	@Test
	public void testHttpConnectionsPercentage() {
		pktAnalyzerResult = resultTestSetup(5);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals("100", String.valueOf(httpsUsageEntries.get(0).getHttpConnectionsPercentage()));
			assertEquals("50", String.valueOf(httpsUsageEntries.get(1).getHttpConnectionsPercentage()));
		} else {
			assertEquals("50", String.valueOf(httpsUsageEntries.get(0).getHttpConnectionsPercentage()));
			assertEquals("100", String.valueOf(httpsUsageEntries.get(1).getHttpConnectionsPercentage()));
		}
	}

	@Test
	public void testHttpTrafficSize() {
		pktAnalyzerResult = resultTestSetup(5);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals("0.293", httpsUsageEntries.get(0).getTotalHttpTrafficInKB());
			assertEquals("0.098", httpsUsageEntries.get(1).getTotalHttpTrafficInKB());
		} else {
			assertEquals("0.098", httpsUsageEntries.get(0).getTotalHttpTrafficInKB());
			assertEquals("0.293", httpsUsageEntries.get(1).getTotalHttpTrafficInKB());
		}
	}

	@Test
	public void testTrafficSize() {
		pktAnalyzerResult = resultTestSetup(5);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals("0.293", httpsUsageEntries.get(0).getTotalTrafficInKB());
			assertEquals("0.391", httpsUsageEntries.get(1).getTotalTrafficInKB());
		} else {
			assertEquals("0.391", httpsUsageEntries.get(0).getTotalTrafficInKB());
			assertEquals("0.293", httpsUsageEntries.get(1).getTotalTrafficInKB());
		}
	}

	@Test
	public void testHttpTrafficPercentage() {
		PacketAnalyzerResult pktAnalyzerResult = resultTestSetup(5);
		HttpsUsageResult httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		List<HttpsUsageEntry> httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals("100", String.valueOf(httpsUsageEntries.get(0).getTotalHttpTrafficPercentage()));
			assertEquals("25", String.valueOf(httpsUsageEntries.get(1).getTotalHttpTrafficPercentage()));
		} else {
			assertEquals("25", String.valueOf(httpsUsageEntries.get(0).getTotalHttpTrafficPercentage()));
			assertEquals("100", String.valueOf(httpsUsageEntries.get(1).getTotalHttpTrafficPercentage()));
		}
	}

	@Test
	public void testResultPass() {
		pktAnalyzerResult = resultTestSetup(0);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(0, httpsUsageEntries.size());
		assertEquals(BPResultType.PASS, httpsUsageResult.getResultType());
		assertEquals(
				ApplicationConfig.getInstance().getAppShortName()
						+ " discovered 0 non-HTTPS connection and it passes the test.",
				httpsUsageResult.getResultText());
	}

	@Test
	public void testResultWarn() {
		pktAnalyzerResult = resultTestSetup(1);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(1, httpsUsageEntries.size());
		assertEquals(BPResultType.WARNING, httpsUsageResult.getResultType());
		assertEquals(
				ApplicationConfig.getInstance().getAppShortName()
						+ " discovered 1 non-HTTPS connections, which is 13% of all the TCP sessions.",
				httpsUsageResult.getResultText());
	}

	@Test
	public void testResultFail_NumHttpConnThree() {
		pktAnalyzerResult = resultTestSetup(3);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(1, httpsUsageEntries.size());
		assertEquals(BPResultType.WARNING, httpsUsageResult.getResultType());
		assertEquals(
				ApplicationConfig.getInstance().getAppShortName()
						+ " discovered 3 non-HTTPS connections, which is 38% of all the TCP sessions.",
				httpsUsageResult.getResultText());
	}

	@Test
	public void testResultFail_NumHttpConnMoreThanThree() {
		pktAnalyzerResult = resultTestSetup(4);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(2, httpsUsageEntries.size());
		assertEquals(BPResultType.WARNING, httpsUsageResult.getResultType());
		assertEquals(
				ApplicationConfig.getInstance().getAppShortName()
						+ " discovered 4 non-HTTPS connections, which is 50% of all the TCP sessions.",
				httpsUsageResult.getResultText());
	}

	@Test
	public void testResultFail_HttpConnGreaterThanTwentyFivePercent() {
		pktAnalyzerResult = resultTestSetup(3);
		httpsUsageResult = (HttpsUsageResult) httpsUsageImpl.runTest(pktAnalyzerResult);
		httpsUsageEntries = httpsUsageResult.getResults();
		assertEquals(1, httpsUsageEntries.size());
		assertEquals(BPResultType.WARNING, httpsUsageResult.getResultType());
		assertEquals(
				ApplicationConfig.getInstance().getAppShortName()
						+ " discovered 3 non-HTTPS connections, which is 38% of all the TCP sessions.",
				httpsUsageResult.getResultText());
	}

	private PacketAnalyzerResult domainNameTestSetup(String ipAddressA, String ipAddressB, String sessionDomainName1,
			String sessionDomainName2, String sessionDomainName3) {
		Session session1 = mock(Session.class);
		Session session2 = mock(Session.class);
		Session session3 = mock(Session.class);
		InetAddress ipAddrA = mock(InetAddress.class);
		InetAddress ipAddrB = mock(InetAddress.class);
		when(ipAddrA.getHostAddress()).thenReturn(ipAddressA);
		when(ipAddrB.getHostAddress()).thenReturn(ipAddressB);
		when(session1.getRemoteIP()).thenReturn(ipAddrA);
		when(session1.getDomainName()).thenReturn(sessionDomainName1);
		when(session2.getRemoteIP()).thenReturn(ipAddrA);
		when(session2.getDomainName()).thenReturn(sessionDomainName2);
		when(session3.getRemoteIP()).thenReturn(ipAddrB);
		when(session3.getDomainName()).thenReturn(sessionDomainName3);
		TCPPacket tcpPacket1_1 = mock(TCPPacket.class);
		TCPPacket tcpPacket1_2 = mock(TCPPacket.class);
		TCPPacket tcpPacket2 = mock(TCPPacket.class);
		TCPPacket tcpPacket3 = mock(TCPPacket.class);
		when(tcpPacket1_1.containsSSLRecord()).thenReturn(false);
		when(tcpPacket1_2.containsSSLRecord()).thenReturn(false);
		when(tcpPacket2.containsSSLRecord()).thenReturn(false);
		when(tcpPacket3.containsSSLRecord()).thenReturn(false);
		PacketInfo packetInfo1_1 = mock(PacketInfo.class);
		PacketInfo packetInfo1_2 = mock(PacketInfo.class);
		PacketInfo packetInfo2 = mock(PacketInfo.class);
		PacketInfo packetInfo3 = mock(PacketInfo.class);
		when(packetInfo1_1.getLen()).thenReturn(2);
		when(packetInfo1_1.getPayloadLen()).thenReturn(1);
		when(packetInfo1_1.getPacket()).thenReturn(tcpPacket1_1);
		when(packetInfo1_2.getLen()).thenReturn(2);
		when(packetInfo1_2.getPayloadLen()).thenReturn(1);
		when(packetInfo1_2.getPacket()).thenReturn(tcpPacket1_2);
		when(packetInfo2.getLen()).thenReturn(2);
		when(packetInfo2.getPayloadLen()).thenReturn(1);
		when(packetInfo2.getPacket()).thenReturn(tcpPacket2);
		when(packetInfo3.getLen()).thenReturn(2);
		when(packetInfo3.getPayloadLen()).thenReturn(1);
		when(packetInfo3.getPacket()).thenReturn(tcpPacket3);
		List<PacketInfo> packetsInfo1 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo2 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo3 = new ArrayList<PacketInfo>();
		packetsInfo1.add(packetInfo1_1);
		packetsInfo1.add(packetInfo1_2);
		packetsInfo2.add(packetInfo2);
		packetsInfo3.add(packetInfo3);
		when(session1.getPackets()).thenReturn(packetsInfo1);
		when(session2.getPackets()).thenReturn(packetsInfo2);
		when(session3.getPackets()).thenReturn(packetsInfo3);
		List<Session> sessions = new ArrayList<Session>();
		sessions.add(session1);
		sessions.add(session2);
		sessions.add(session3);
		pktAnalyzerResult.setSessionlist(sessions);
		return pktAnalyzerResult;
	}

	private void domainNameTestVerification(List<HttpsUsageEntry> httpsUsageEntries) {
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals("gstatic.com", httpsUsageEntries.get(0).getParentDomainName());
			assertEquals("arotest.com", httpsUsageEntries.get(1).getParentDomainName());
		} else {
			assertEquals("arotest.com", httpsUsageEntries.get(0).getParentDomainName());
			assertEquals("gstatic.com", httpsUsageEntries.get(1).getParentDomainName());
		}
	}

	private void domainNameTestVerification(List<HttpsUsageEntry> httpsUsageEntries, String domainNameUnderTest) {
		assertEquals(2, httpsUsageEntries.size());
		if (httpsUsageEntries.get(0).getIPAddress().equals("157.56.19.80")) {
			assertEquals("gstatic.com", httpsUsageEntries.get(0).getParentDomainName());
			assertEquals(domainNameUnderTest, httpsUsageEntries.get(1).getParentDomainName());
		} else {
			assertEquals(domainNameUnderTest, httpsUsageEntries.get(0).getParentDomainName());
			assertEquals("gstatic.com", httpsUsageEntries.get(1).getParentDomainName());
		}
	}

	private PacketAnalyzerResult sessionPacketsPayloadTestSetup(int payloadSize1, int payloadSize2, int payloadSize3,
			int payloadSize4) {
		Session session1 = mock(Session.class);
		Session session2 = mock(Session.class);
		Session session3 = mock(Session.class);
		InetAddress ipAddrA = mock(InetAddress.class);
		InetAddress ipAddrB = mock(InetAddress.class);
		InetAddress ipAddrC = mock(InetAddress.class);
		when(ipAddrA.getHostAddress()).thenReturn("157.56.19.80");
		when(ipAddrB.getHostAddress()).thenReturn("216.58.194.196");
		when(ipAddrC.getHostAddress()).thenReturn("216.58.194.198");
		when(session1.getRemoteIP()).thenReturn(ipAddrA);
		when(session1.getDomainName()).thenReturn("www.gstatic.com");
		when(session2.getRemoteIP()).thenReturn(ipAddrB);
		when(session2.getDomainName()).thenReturn("ssl.gstatic.com");
		when(session3.getRemoteIP()).thenReturn(ipAddrC);
		when(session3.getDomainName()).thenReturn("www.arotest.com");
		TCPPacket tcpPacket1_1 = mock(TCPPacket.class);
		TCPPacket tcpPacket1_2 = mock(TCPPacket.class);
		TCPPacket tcpPacket2 = mock(TCPPacket.class);
		TCPPacket tcpPacket3 = mock(TCPPacket.class);
		when(tcpPacket1_1.containsSSLRecord()).thenReturn(false);
		when(tcpPacket1_2.containsSSLRecord()).thenReturn(false);
		when(tcpPacket2.containsSSLRecord()).thenReturn(false);
		when(tcpPacket3.containsSSLRecord()).thenReturn(true);
		PacketInfo packetInfo1_1 = mock(PacketInfo.class);
		PacketInfo packetInfo1_2 = mock(PacketInfo.class);
		PacketInfo packetInfo2 = mock(PacketInfo.class);
		PacketInfo packetInfo3 = mock(PacketInfo.class);
		when(packetInfo1_1.getPayloadLen()).thenReturn(payloadSize1);
		when(packetInfo1_1.getPacket()).thenReturn(tcpPacket1_1);
		when(packetInfo1_2.getPayloadLen()).thenReturn(payloadSize2);
		when(packetInfo1_2.getPacket()).thenReturn(tcpPacket1_2);
		when(packetInfo2.getPayloadLen()).thenReturn(payloadSize3);
		when(packetInfo2.getPacket()).thenReturn(tcpPacket2);
		when(packetInfo3.getPayloadLen()).thenReturn(payloadSize4);
		when(packetInfo3.getPacket()).thenReturn(tcpPacket3);
		List<PacketInfo> packetsInfo1 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo2 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo3 = new ArrayList<PacketInfo>();
		packetsInfo1.add(packetInfo1_1);
		packetsInfo1.add(packetInfo1_2);
		packetsInfo2.add(packetInfo2);
		packetsInfo3.add(packetInfo3);
		when(session1.getPackets()).thenReturn(packetsInfo1);
		when(session2.getPackets()).thenReturn(packetsInfo2);
		when(session3.getPackets()).thenReturn(packetsInfo3);
		List<Session> sessions = new ArrayList<Session>();
		sessions.add(session1);
		sessions.add(session2);
		sessions.add(session3);
		pktAnalyzerResult.setSessionlist(sessions);
		return pktAnalyzerResult;
	}

	/*
	 * 8 connections/sessions are set up in this method.
	 *
	 * Connections 1 - 3 share the same IP. Connections 4 - 7 share the same IP,
	 * different from the one shared by Connections 1 - 3. Connections 8 has a
	 * different IP than Connections 1 - 7.
	 *
	 * numHttpConn is expected to be any number between 1 and 8, inclusively. If
	 * it is not, then all connections/sessions will be set up as HTTPS.
	 *
	 * numHttpConn = 1 - C1 is HTTP Connection numHttpConn = 2 - C1, C2 are HTTP
	 * Connections numHttpConn = 3 - C1, C2, C3 are HTTP Connections numHttpConn
	 * = 4 - C1, C2, C3, C4 are HTTP Connections ... same logic follows till
	 * numHttpConn = 8
	 *
	 * Number of TCP Packets in each connection: C1 - 2 C2 - 3 C3 - 1 C4 - 1 C5
	 * - 1 C6 - 5 C7 - 1 C8 - 1
	 *
	 * Each TCP Packet is 50 bytes.
	 */
	private PacketAnalyzerResult resultTestSetup(int numHttpConn) {
		boolean containsSSLRecord_1a = (numHttpConn >= 1 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_1b = (numHttpConn >= 1 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_2a = (numHttpConn >= 2 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_2b = (numHttpConn >= 2 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_2c = (numHttpConn >= 2 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_3 = (numHttpConn >= 3 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_4 = (numHttpConn >= 4 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_5 = (numHttpConn >= 5 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_6a = (numHttpConn >= 6 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_6b = (numHttpConn >= 6 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_6c = (numHttpConn >= 6 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_6d = (numHttpConn >= 6 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_6e = (numHttpConn >= 6 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_7 = (numHttpConn >= 7 && numHttpConn <= 8) ? false : true;
		boolean containsSSLRecord_8 = (numHttpConn == 8) ? false : true;
		Session session1 = mock(Session.class);
		Session session2 = mock(Session.class);
		Session session3 = mock(Session.class);
		Session session4 = mock(Session.class);
		Session session5 = mock(Session.class);
		Session session6 = mock(Session.class);
		Session session7 = mock(Session.class);
		Session session8 = mock(Session.class);
		InetAddress ipAddrA = mock(InetAddress.class);
		InetAddress ipAddrB = mock(InetAddress.class);
		InetAddress ipAddrC = mock(InetAddress.class);
		when(ipAddrA.getHostAddress()).thenReturn("157.56.19.80");
		when(ipAddrB.getHostAddress()).thenReturn("216.58.194.196");
		when(ipAddrC.getHostAddress()).thenReturn("72.21.92.82");
		when(session1.getRemoteIP()).thenReturn(ipAddrA);
		when(session1.getDomainName()).thenReturn("www.hotmail.com");
		when(session2.getRemoteIP()).thenReturn(ipAddrA);
		when(session2.getDomainName()).thenReturn("www.hotmail.com");
		when(session3.getRemoteIP()).thenReturn(ipAddrA);
		when(session3.getDomainName()).thenReturn("www.hotmail.com");
		when(session4.getRemoteIP()).thenReturn(ipAddrB);
		when(session4.getDomainName()).thenReturn("www.arotest.com");
		when(session5.getRemoteIP()).thenReturn(ipAddrB);
		when(session5.getDomainName()).thenReturn("www.arotest.com");
		when(session6.getRemoteIP()).thenReturn(ipAddrB);
		when(session6.getDomainName()).thenReturn("www.arotest.com");
		when(session7.getRemoteIP()).thenReturn(ipAddrB);
		when(session7.getDomainName()).thenReturn("www.arotest.com");
		when(session8.getRemoteIP()).thenReturn(ipAddrC);
		when(session8.getDomainName()).thenReturn("www.att.com");
		TCPPacket tcpPacket1a = mock(TCPPacket.class);
		TCPPacket tcpPacket1b = mock(TCPPacket.class);
		TCPPacket tcpPacket2a = mock(TCPPacket.class);
		TCPPacket tcpPacket2b = mock(TCPPacket.class);
		TCPPacket tcpPacket2c = mock(TCPPacket.class);
		TCPPacket tcpPacket3 = mock(TCPPacket.class);
		TCPPacket tcpPacket4 = mock(TCPPacket.class);
		TCPPacket tcpPacket5 = mock(TCPPacket.class);
		TCPPacket tcpPacket6a = mock(TCPPacket.class);
		TCPPacket tcpPacket6b = mock(TCPPacket.class);
		TCPPacket tcpPacket6c = mock(TCPPacket.class);
		TCPPacket tcpPacket6d = mock(TCPPacket.class);
		TCPPacket tcpPacket6e = mock(TCPPacket.class);
		TCPPacket tcpPacket7 = mock(TCPPacket.class);
		TCPPacket tcpPacket8 = mock(TCPPacket.class);
		when(tcpPacket1a.containsSSLRecord()).thenReturn(containsSSLRecord_1a);
		when(tcpPacket1b.containsSSLRecord()).thenReturn(containsSSLRecord_1b);
		when(tcpPacket2a.containsSSLRecord()).thenReturn(containsSSLRecord_2a);
		when(tcpPacket2b.containsSSLRecord()).thenReturn(containsSSLRecord_2b);
		when(tcpPacket2c.containsSSLRecord()).thenReturn(containsSSLRecord_2c);
		when(tcpPacket3.containsSSLRecord()).thenReturn(containsSSLRecord_3);
		when(tcpPacket4.containsSSLRecord()).thenReturn(containsSSLRecord_4);
		when(tcpPacket5.containsSSLRecord()).thenReturn(containsSSLRecord_5);
		when(tcpPacket6a.containsSSLRecord()).thenReturn(containsSSLRecord_6a);
		when(tcpPacket6b.containsSSLRecord()).thenReturn(containsSSLRecord_6b);
		when(tcpPacket6c.containsSSLRecord()).thenReturn(containsSSLRecord_6c);
		when(tcpPacket6d.containsSSLRecord()).thenReturn(containsSSLRecord_6d);
		when(tcpPacket6e.containsSSLRecord()).thenReturn(containsSSLRecord_6e);
		when(tcpPacket7.containsSSLRecord()).thenReturn(containsSSLRecord_7);
		when(tcpPacket8.containsSSLRecord()).thenReturn(containsSSLRecord_8);
		PacketInfo packetInfo1a = mock(PacketInfo.class);
		PacketInfo packetInfo1b = mock(PacketInfo.class);
		PacketInfo packetInfo2a = mock(PacketInfo.class);
		PacketInfo packetInfo2b = mock(PacketInfo.class);
		PacketInfo packetInfo2c = mock(PacketInfo.class);
		PacketInfo packetInfo3 = mock(PacketInfo.class);
		PacketInfo packetInfo4 = mock(PacketInfo.class);
		PacketInfo packetInfo5 = mock(PacketInfo.class);
		PacketInfo packetInfo6a = mock(PacketInfo.class);
		PacketInfo packetInfo6b = mock(PacketInfo.class);
		PacketInfo packetInfo6c = mock(PacketInfo.class);
		PacketInfo packetInfo6d = mock(PacketInfo.class);
		PacketInfo packetInfo6e = mock(PacketInfo.class);
		PacketInfo packetInfo7 = mock(PacketInfo.class);
		PacketInfo packetInfo8 = mock(PacketInfo.class);
		when(packetInfo1a.getLen()).thenReturn(50);
		when(packetInfo1a.getPayloadLen()).thenReturn(10);
		when(packetInfo1a.getPacket()).thenReturn(tcpPacket1a);
		when(packetInfo1b.getLen()).thenReturn(50);
		when(packetInfo1b.getPayloadLen()).thenReturn(10);
		when(packetInfo1b.getPacket()).thenReturn(tcpPacket1b);
		when(packetInfo2a.getLen()).thenReturn(50);
		when(packetInfo2a.getPayloadLen()).thenReturn(10);
		when(packetInfo2a.getPacket()).thenReturn(tcpPacket2a);
		when(packetInfo2b.getLen()).thenReturn(50);
		when(packetInfo2b.getPayloadLen()).thenReturn(10);
		when(packetInfo2b.getPacket()).thenReturn(tcpPacket2b);
		when(packetInfo2c.getLen()).thenReturn(50);
		when(packetInfo2c.getPayloadLen()).thenReturn(10);
		when(packetInfo2c.getPacket()).thenReturn(tcpPacket2c);
		when(packetInfo3.getLen()).thenReturn(50);
		when(packetInfo3.getPayloadLen()).thenReturn(10);
		when(packetInfo3.getPacket()).thenReturn(tcpPacket3);
		when(packetInfo4.getLen()).thenReturn(50);
		when(packetInfo4.getPayloadLen()).thenReturn(10);
		when(packetInfo4.getPacket()).thenReturn(tcpPacket4);
		when(packetInfo5.getLen()).thenReturn(50);
		when(packetInfo5.getPayloadLen()).thenReturn(10);
		when(packetInfo5.getPacket()).thenReturn(tcpPacket5);
		when(packetInfo6a.getLen()).thenReturn(50);
		when(packetInfo6a.getPayloadLen()).thenReturn(10);
		when(packetInfo6a.getPacket()).thenReturn(tcpPacket6a);
		when(packetInfo6b.getLen()).thenReturn(50);
		when(packetInfo6b.getPayloadLen()).thenReturn(10);
		when(packetInfo6b.getPacket()).thenReturn(tcpPacket6b);
		when(packetInfo6c.getLen()).thenReturn(50);
		when(packetInfo6c.getPayloadLen()).thenReturn(10);
		when(packetInfo6c.getPacket()).thenReturn(tcpPacket6c);
		when(packetInfo6d.getLen()).thenReturn(50);
		when(packetInfo6d.getPayloadLen()).thenReturn(10);
		when(packetInfo6d.getPacket()).thenReturn(tcpPacket6d);
		when(packetInfo6e.getLen()).thenReturn(50);
		when(packetInfo6e.getPayloadLen()).thenReturn(10);
		when(packetInfo6e.getPacket()).thenReturn(tcpPacket6e);
		when(packetInfo7.getLen()).thenReturn(50);
		when(packetInfo7.getPayloadLen()).thenReturn(10);
		when(packetInfo7.getPacket()).thenReturn(tcpPacket7);
		when(packetInfo8.getLen()).thenReturn(50);
		when(packetInfo8.getPayloadLen()).thenReturn(10);
		when(packetInfo8.getPacket()).thenReturn(tcpPacket8);
		List<PacketInfo> packetsInfo1 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo2 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo3 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo4 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo5 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo6 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo7 = new ArrayList<PacketInfo>();
		List<PacketInfo> packetsInfo8 = new ArrayList<PacketInfo>();
		packetsInfo1.add(packetInfo1a);
		packetsInfo1.add(packetInfo1b);
		packetsInfo2.add(packetInfo2a);
		packetsInfo2.add(packetInfo2b);
		packetsInfo2.add(packetInfo2c);
		packetsInfo3.add(packetInfo3);
		packetsInfo4.add(packetInfo4);
		packetsInfo5.add(packetInfo5);
		packetsInfo6.add(packetInfo6a);
		packetsInfo6.add(packetInfo6b);
		packetsInfo6.add(packetInfo6c);
		packetsInfo6.add(packetInfo6d);
		packetsInfo6.add(packetInfo6e);
		packetsInfo7.add(packetInfo7);
		packetsInfo8.add(packetInfo8);
		when(session1.getPackets()).thenReturn(packetsInfo1);
		when(session2.getPackets()).thenReturn(packetsInfo2);
		when(session3.getPackets()).thenReturn(packetsInfo3);
		when(session4.getPackets()).thenReturn(packetsInfo4);
		when(session5.getPackets()).thenReturn(packetsInfo5);
		when(session6.getPackets()).thenReturn(packetsInfo6);
		when(session7.getPackets()).thenReturn(packetsInfo7);
		when(session8.getPackets()).thenReturn(packetsInfo8);
		List<Session> sessions = new ArrayList<Session>();
		sessions.add(session1);
		sessions.add(session2);
		sessions.add(session3);
		sessions.add(session4);
		sessions.add(session5);
		sessions.add(session6);
		sessions.add(session7);
		sessions.add(session8);
		pktAnalyzerResult.setSessionlist(sessions);
		return pktAnalyzerResult;
	}
}
