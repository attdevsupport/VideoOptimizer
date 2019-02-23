package com.att.aro.core.bestpractice.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoRedundancyResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoRedundancyImplTest extends BaseTest {

	private PacketAnalyzerResult tracedata;
	private TraceDirectoryResult traceResults;
	private TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();
	private VideoUsage videoUsage;
	private VideoEvent videoEvent;
	private VideoEvent videoEvent2;
	private TreeMap<String, VideoEvent> videoEventList;
	private ArrayList<AROManifest> manifests;
	private AROManifest manifest;

	private double pcapTS = 0D;
	private long traceStartTS = 0L;
	private double manReqTS = 0D;
	private double vidPlay = 0D;

	private VideoRedundancyImpl videoRedundancyImpl;
	private VideoRedundancyResult result;
	private Session session;
	private Session session2;

	@Before
	public void setup() {
		videoRedundancyImpl = (VideoRedundancyImpl) context.getBean("videoRedundancy");

		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		traceResults = Mockito.mock(TraceDirectoryResult.class);

		tracedata = Mockito.mock(PacketAnalyzerResult.class);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);

		manifest = Mockito.mock(AROManifest.class);
		manifests = new ArrayList<>();
		manifests.add(manifest);

		TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();

		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));
		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);

		videoUsage = Mockito.mock(VideoUsage.class);

		traceResults = Mockito.mock(TraceDirectoryResult.class);
		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);

		session = Mockito.mock(Session.class);
		Mockito.when(session.getSessionStartTime()).thenReturn(113456D);
		Mockito.when(session.getSessionEndTime()).thenReturn(113956D);

		session2 = Mockito.mock(Session.class);
		Mockito.when(session2.getSessionStartTime()).thenReturn(123556D);
		Mockito.when(session2.getSessionEndTime()).thenReturn(123956D);

		videoEvent = Mockito.mock(VideoEvent.class);
		videoEvent2 = Mockito.mock(VideoEvent.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(2);
		Mockito.when(videoUsage.getSegmentCount()).thenReturn(10);
		Mockito.when(videoUsage.getValidSegmentCount()).thenReturn(10);
		Mockito.when(videoUsage.getNonValidSegmentCount()).thenReturn(0);

//		double segment = 1234;
//		double timestamp = 1234567890;
//		videoEventList = new TreeMap<>();
//		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
//		videoEventList.put(String.format("%010.4f:%08.0f", timestamp + 1, segment + 1), videoEvent2);
		
		double segment = 1234;
		double timestamp = 1234567890;
		videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp++, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp++, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp++, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp++, segment), videoEvent);
		
		Mockito.when(manifest.getSegmentCount()).thenReturn((double) videoEventList.size());

		Mockito.when(videoEvent.getSession()).thenReturn(session);
		Mockito.when(videoEvent2.getSession()).thenReturn(session2);
		Mockito.when(manifest.getVideoEventList()).thenReturn(videoEventList);
	}

	@Test
	public void runTest_two_manifests_one_selected_NO_redundant_AND_2_duplicate_THEN_PASS() {
		
//		manifest = Mockito.mock(ManifestDash.class);
//		
//		manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		Mockito.when(tracedata.getTraceresult().getPcapTimeOffset()).thenReturn(1.510699853456E9D);
		Mockito.when(tracedata.getTraceresult().getVideoStartTime()).thenReturn(1.510699855132E9D);
		Mockito.when(tracedata.getTraceresult().getTraceDateTime()).thenReturn(new Date(1510699853456L));

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(null);

		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);

		Mockito.when(manifest.isSelected()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getSegment()).thenReturn(1D).thenReturn(2D);
		Mockito.when(videoEvent.getQuality()).thenReturn("A");
		
		result = (VideoRedundancyResult) videoRedundancyImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("There were no redundant segments in this video.");
		assertThat(result.getCountDuplicate()).isEqualTo(2);

		assertThat(result.getAboutText()).contains("HTTP Streaming generates multiple versions of the same content in different quality, which allows the client to display the most appropriate version. Understanding the number and quality of these versions can help a developer avoid overkill.");
		assertThat(result.getDetailTitle()).contains("Redundancy");
		assertThat(result.getOverviewTitle()).contains("Video: Redundancy");
	}
	

	@Test
	public void runTest_two_manifests_one_selected_AND_50percent_redundant_THEN_FAIL() {
		
//		manifest = Mockito.mock(ManifestDash.class);
//		
//		manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		Mockito.when(tracedata.getTraceresult().getPcapTimeOffset()).thenReturn(1.510699853456E9D);
		Mockito.when(tracedata.getTraceresult().getVideoStartTime()).thenReturn(1.510699855132E9D);
		Mockito.when(tracedata.getTraceresult().getTraceDateTime()).thenReturn(new Date(1510699853456L));

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(null);

		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);

		Mockito.when(manifest.isSelected()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getSegment()).thenReturn(1D).thenReturn(2D);
		Mockito.when(videoEvent.getQuality()).thenReturn("A").thenReturn("B").thenReturn("C");
		
		result = (VideoRedundancyResult) videoRedundancyImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.FAIL);
		assertThat(result.getResultText()).contains("There were 50% redundant segments in this video.");

		assertThat(result.getAboutText()).contains("HTTP Streaming generates multiple versions of the same content in different quality, which allows the client to display the most appropriate version. Understanding the number and quality of these versions can help a developer avoid overkill.");
		assertThat(result.getDetailTitle()).contains("Redundancy");
		assertThat(result.getOverviewTitle()).contains("Video: Redundancy");
	}
	
	@Test
	public void runTest_two_manifests_one_selected_AND_no_redundant_THEN_PASS() {

		manifests.add(manifest);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		Mockito.when(tracedata.getTraceresult().getPcapTimeOffset()).thenReturn(1.510699853456E9D);
		Mockito.when(tracedata.getTraceresult().getVideoStartTime()).thenReturn(1.510699855132E9D);
		Mockito.when(tracedata.getTraceresult().getTraceDateTime()).thenReturn(new Date(1510699853456L));

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(null);

		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);

		Mockito.when(manifest.isSelected()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoRedundancyResult) videoRedundancyImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("There were no redundant segments in this video.");

		assertThat(result.getAboutText()).contains("HTTP Streaming generates multiple versions of the same content in different quality, which allows the client to display the most appropriate version. Understanding the number and quality of these versions can help a developer avoid overkill.");
		assertThat(result.getDetailTitle()).contains("Redundancy");
		assertThat(result.getOverviewTitle()).contains("Video: Redundancy");
	}

	@Test
	public void runTest_no_manifests_THEN_NO_DATA() {

		manifestCollection.clear();
		manifests.clear();

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);

		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(0);

		result = (VideoRedundancyResult) videoRedundancyImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.NO_DATA);
		assertThat(result.getResultText()).contains("No streaming video data found.");

		assertThat(result.getAboutText()).contains("HTTP Streaming generates multiple versions of the same content in different quality, which allows the client to display the most appropriate version. Understanding the number and quality of these versions can help a developer avoid overkill.");
		assertThat(result.getDetailTitle()).contains("Redundancy");
		assertThat(result.getOverviewTitle()).contains("Video: Redundancy");
	}

	@Test
	public void runTest_two_manifests_selected_THEN_CONFIG_REQUIRED() {

		manifests.add(manifest);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		Mockito.when(tracedata.getTraceresult().getPcapTimeOffset()).thenReturn(1.510699853456E9D);
		Mockito.when(tracedata.getTraceresult().getVideoStartTime()).thenReturn(1.510699855132E9D);
		Mockito.when(tracedata.getTraceresult().getTraceDateTime()).thenReturn(new Date(1510699853456L));

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(null);

		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(2);

		Mockito.when(manifest.isSelected()).thenReturn(true).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoRedundancyResult) videoRedundancyImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("Please select only one manifest on the Video tab.  Click here to select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");

		assertThat(result.getAboutText()).contains("HTTP Streaming generates multiple versions of the same content in different quality, which allows the client to display the most appropriate version. Understanding the number and quality of these versions can help a developer avoid overkill.");
		assertThat(result.getDetailTitle()).contains("Redundancy");
		assertThat(result.getOverviewTitle()).contains("Video: Redundancy");
	}

	@Test
	public void runTest_two_invalid_manifests_selected_THEN_CONFIG_REQUIRED() {

		manifests.add(manifest);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		Mockito.when(tracedata.getTraceresult().getPcapTimeOffset()).thenReturn(1.510699853456E9D);
		Mockito.when(tracedata.getTraceresult().getVideoStartTime()).thenReturn(1.510699855132E9D);
		Mockito.when(tracedata.getTraceresult().getTraceDateTime()).thenReturn(new Date(1510699853456L));

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(null);

		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(2);

		Mockito.when(manifest.isSelected()).thenReturn(true).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoRedundancyResult) videoRedundancyImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("Invalid manifests do not have enough information for analyzing streaming video.<br /> Hint: look for ways to locate segment information with the Video Parser Wizard. Click here to select Request URL <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");

		assertThat(result.getAboutText()).contains("HTTP Streaming generates multiple versions of the same content in different quality, which allows the client to display the most appropriate version. Understanding the number and quality of these versions can help a developer avoid overkill.");
		assertThat(result.getDetailTitle()).contains("Redundancy");
		assertThat(result.getOverviewTitle()).contains("Video: Redundancy");
	}

}
