package com.att.aro.core.bestpractice.impl;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoNetworkComparisonResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoNetworkComparisonImplTest extends BaseTest {

	@InjectMocks
	private IBestPractice videoNetworkComp;
	private PacketAnalyzerResult tracedata;
	private Session session01;

	private VideoUsage videoUsage;
	private VideoEvent videoEvent;
	private TreeMap<String, VideoEvent> videoEventList;
	private AROManifest manifest;
	private TraceDirectoryResult traceResults;
	
	@Before
	public void setup() {
		videoNetworkComp = (VideoNetworkComparisonImpl)context.getBean("networkComparison");
  
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		traceResults = Mockito.mock(TraceDirectoryResult.class);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		manifest = Mockito.mock(AROManifest.class);
		AROManifest manifest2 = Mockito.mock(AROManifest.class);
		TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest2);

		videoUsage = Mockito.mock(VideoUsage.class);
		videoEvent = Mockito.mock(VideoEvent.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(videoUsage.getManifests()).thenReturn(manifestCollection.values());

		double segment = 1234;
		double timestamp = 1234567890;
		videoEventList = new TreeMap<String , VideoEvent>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp + 1, segment + 1), videoEvent);
		Mockito.when(manifest.getVideoEventList()).thenReturn(videoEventList);
		Mockito.when(manifest.getSegmentEventList()).thenReturn(videoEventList);
	}

	@Test
	public void runTest_all_invalid_Manifests_should_CONFIG_REQUIRED(){
		
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(2);

		VideoNetworkComparisonResult result = (VideoNetworkComparisonResult) videoNetworkComp.runTest(tracedata);
		assertThat(result.getResultText()).contains("Invalid manifests do not have enough information for analyzing streaming video.<br /> Hint: look for ways to locate segment information with the Video Parser Wizard. Click here to select Request URL <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
	}
	
	@Test
	public void runTest_all_deselected_Manifests_should_CONFIG_REQUIRED(){
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(0);
		
		VideoNetworkComparisonResult result = (VideoNetworkComparisonResult) videoNetworkComp.runTest(tracedata);
		assertThat(result.getResultText()).contains("No manifest is selected. Please select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
	}
	
	@Test
	public void runTest_all_selected_Manifests_should_CONFIG_REQUIRED(){
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(2);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(0);
		
		VideoNetworkComparisonResult result = (VideoNetworkComparisonResult) videoNetworkComp.runTest(tracedata);
		assertThat(result.getResultText()).contains("Please select only one manifest on the Video tab.  Click here to select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
	}
	
	@Test
	public void runTest_one_selected_Manifest_expect_SELFTEST(){
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(0);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		
		Date date = new Date();
		List<Session> sessionList = new ArrayList<Session>();
		session01 = Mockito.mock(Session.class);
		sessionList.add(session01);
		
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
		
		
		VideoNetworkComparisonResult result = (VideoNetworkComparisonResult) videoNetworkComp.runTest(tracedata);
		assertThat(result.getResultText()).contains("Your network trace average throughput is 8 Kbps. Your video was delivered at the average of 0 Kbps rate of speed");
		assertThat(result.getResultType()).isSameAs(BPResultType.SELF_TEST);

	}

	@Test
	public void runTest_noData(){
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(null);
		
		VideoNetworkComparisonResult result = (VideoNetworkComparisonResult) videoNetworkComp.runTest(tracedata);
		assertThat(result.getResultText()).contains("No streaming video data found.");
		assertThat(result.getResultType()).isSameAs(BPResultType.NO_DATA);
	}
}
