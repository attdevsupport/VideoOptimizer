package com.att.aro.core.bestpractice.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoNetworkComparisonImplTest extends BaseTest {

	@InjectMocks
	IBestPractice videoNetworkComp;
 
	PacketAnalyzerResult tracedata;
	Session session01;
	Session session02;
	HttpRequestResponseInfo httpRequestInfo01 ;

	@Before
	public void setup() {
		videoNetworkComp = (VideoNetworkComparisonImpl)context.getBean("networkComparison");
  
	}

	@Test
	public void runTest_resTypeIsPass() {
		Date date = new Date();
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		session01 = Mockito.mock(Session.class);
		session02 = Mockito.mock(Session.class);


		VideoNetworkComparisonImpl videoNetworkComp = new VideoNetworkComparisonImpl();
		List<Session> sessionList = new ArrayList<Session>();
		sessionList.add(session01);
		sessionList.add(session02);
		
		List<PacketInfo> packets = new ArrayList<PacketInfo>();
		Mockito.mock(PacketInfo.class);
		PacketInfo pktInfo01 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo01.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo01.getTimeStamp()).thenReturn(date.getTime() + 5000.0);
		packets.add(pktInfo01);

		PacketInfo pktInfo02 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo02.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo02.getTimeStamp()).thenReturn(date.getTime() + 8000.0);
		packets.add(pktInfo02);

		PacketInfo pktInfo03 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo03.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo03.getTimeStamp()).thenReturn(date.getTime() + 22000.0);
		packets.add(pktInfo03);

		PacketInfo pktInfo04 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo04.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo04.getTimeStamp()).thenReturn(date.getTime() + 23000.0);
		packets.add(pktInfo04);

		PacketInfo pktInfo05 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo05.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo05.getTimeStamp()).thenReturn(date.getTime() + 70000.0);
		packets.add(pktInfo05);

		PacketInfo pktInfo06 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo06.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo06.getTimeStamp()).thenReturn(date.getTime() + 72000.0);
		packets.add(pktInfo06);

		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionList);
		Mockito.when(session01.getPackets()).thenReturn(packets);

		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		AROManifest aroManifest = Mockito.mock(AROManifest.class);
		
		TreeMap<Double, AROManifest> videoEventList = new TreeMap<Double, AROManifest>();		
		httpRequestInfo01 = Mockito.mock(HttpRequestResponseInfo.class);
		Mockito.when(httpRequestInfo01.getTimeStamp()).thenReturn(100000000.0);
		videoEventList.put(httpRequestInfo01.getTimeStamp(),aroManifest);
		Mockito.when(httpRequestInfo01.getVersion()).thenReturn(HttpRequestResponseInfo.HTTP11);
		
		videoNetworkComp.setTextResults("Your network trace average throughput is {0} Kbps. Your video was delivered at average {1} rate of speed.");
		videoNetworkComp.setTextResultPass("Your network trace average throughput is {0} Kbps.Your video was delivered at average {1} rate of speed and passes the test.");
		videoNetworkComp.setLearnMoreUrl("http://developer.att.com/{0}/BestPractices/NetworkComparison");
		AbstractBestPracticeResult result = videoNetworkComp.runTest(tracedata);
		
		assertEquals(BPResultType.SELF_TEST, result.getResultType());

	}

	@Test
//	@Ignore
	public void runTest_resTypeIsFail() {
		Date date = new Date();
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		session01 = Mockito.mock(Session.class);
		session02 = Mockito.mock(Session.class);


		VideoNetworkComparisonImpl videoNetworkComp = new VideoNetworkComparisonImpl();
		List<Session> sessionList = new ArrayList<Session>();
		sessionList.add(session01);
		sessionList.add(session02);
		
		List<PacketInfo> packets = new ArrayList<PacketInfo>();
		
		PacketInfo pktInfo01 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo01.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo01.getTimeStamp()).thenReturn(date.getTime() + 5000.0);
		packets.add(pktInfo01);

		PacketInfo pktInfo02 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo02.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo02.getTimeStamp()).thenReturn(date.getTime() + 8000.0);
		packets.add(pktInfo02);

		PacketInfo pktInfo03 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo03.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo03.getTimeStamp()).thenReturn(date.getTime() + 22000.0);
		packets.add(pktInfo03);

		PacketInfo pktInfo04 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo04.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo04.getTimeStamp()).thenReturn(date.getTime() + 23000.0);
		packets.add(pktInfo04);

		PacketInfo pktInfo05 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo05.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo05.getTimeStamp()).thenReturn(date.getTime() + 70000.0);
		packets.add(pktInfo05);

		PacketInfo pktInfo06 = Mockito.mock(PacketInfo.class);
		Mockito.when(pktInfo06.getPayloadLen()).thenReturn(1024);
		Mockito.when(pktInfo06.getTimeStamp()).thenReturn(date.getTime() + 72000.0);
		packets.add(pktInfo06);

		Mockito.when(tracedata.getSessionlist()).thenReturn(sessionList);
		Mockito.when(session01.getPackets()).thenReturn(packets);

		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		
		AROManifest aroManifest = Mockito.mock(AROManifest.class);
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		
		Mockito.when(videoEvent.getBitrate()).thenReturn(1000000.0);
		Mockito.when(videoEvent.getSegment()).thenReturn(1.0);

				
		TreeMap<Double, AROManifest> videoEventList = new TreeMap<Double, AROManifest>();		
		TreeMap<String, VideoEvent> videoEvents = new TreeMap<String, VideoEvent>();
		videoEvents.put("1000000.0", videoEvent);
		Mockito.when(aroManifest.getVideoEventList()).thenReturn(videoEvents);
		httpRequestInfo01 = Mockito.mock(HttpRequestResponseInfo.class);

		videoEventList.put(httpRequestInfo01.getTimeStamp(),aroManifest);

		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(videoEventList);

		Mockito.when(httpRequestInfo01.getTimeStamp()).thenReturn(10000000000.0);
		Mockito.when(httpRequestInfo01.getVersion()).thenReturn(HttpRequestResponseInfo.HTTP11);
		
		videoNetworkComp.setTextResults("Your network trace average throughput is {0} Kbps. Your video was delivered at average {1} rate of speed.");
		videoNetworkComp.setTextResultPass("Your network trace average throughput is {0} Kbps.Your video was delivered at average {1} rate of speed and passes the test.");
		videoNetworkComp.setLearnMoreUrl("http://developer.att.com/{0}/BestPractices/NetworkComparison");
		AbstractBestPracticeResult result = videoNetworkComp.runTest(tracedata);
		
		assertEquals(BPResultType.SELF_TEST, result.getResultType());
	}

}
