package com.att.aro.core.bestpractice.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.WeakCipherResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.TCPPacket;

public class WeakCipherImplTest extends BaseTest {

	private IBestPractice bestPractice;
	private PacketAnalyzerResult packetAnalyzerResult;
	private WeakCipherResult result;

	@Before
	public void setUp() throws Exception {
		bestPractice = (WeakCipherImpl) context.getBean("weakCipher");
		packetAnalyzerResult = new PacketAnalyzerResult();
	}

	@Test
	public void testEmptyWeakCipherSuites() {
		Set<String> weakCipherSuites = new HashSet<>();
		packetAnalyzerResult = getPacketAnalyzerResult(weakCipherSuites);
		result = (WeakCipherResult) ((WeakCipherImpl) bestPractice).runTest(packetAnalyzerResult);
		assertEquals(0, result.getResults().size());
	}

	@Test
	public void testNonEmptyWeakCipherSuites() {
		Set<String> weakCipherSuites = new HashSet<>();
		weakCipherSuites.add("0x0002");
		weakCipherSuites.add("0x000e");
		packetAnalyzerResult = getPacketAnalyzerResult(weakCipherSuites);
		result = (WeakCipherResult) ((WeakCipherImpl) bestPractice).runTest(packetAnalyzerResult);
		assertEquals(2, result.getResults().size());
	}

	private PacketAnalyzerResult getPacketAnalyzerResult(Set<String> weakCipherSuites) {
		Session session = mock(Session.class);
		PacketInfo info = mock(PacketInfo.class);
		TCPPacket tcpPacket = mock(TCPPacket.class);
		InetAddress address = mock(InetAddress.class);

		String ipAddress = "127.0.0.1";
		int port = 8080;
		List<Session> sessions = new ArrayList<>();
		sessions.add(session);
		List<PacketInfo> packetInfos = new LinkedList<>();
		packetInfos.add(info);
		packetAnalyzerResult.setSessionlist(sessions);

		when(session.getPackets()).thenReturn(packetInfos);
		when(info.getPacket()).thenReturn(tcpPacket);
		when(tcpPacket.getWeakCipherSuites()).thenReturn(weakCipherSuites);

		when(address.getHostAddress()).thenReturn(ipAddress);
		when(session.getRemoteIP()).thenReturn(address);
		when(session.getRemotePort()).thenReturn(port);

		return packetAnalyzerResult;
	}
}
