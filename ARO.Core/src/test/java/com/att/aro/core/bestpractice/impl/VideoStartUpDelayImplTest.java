/*
*  Copyright 2017 AT&T
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

package com.att.aro.core.bestpractice.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoStartUpDelayResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.videoanalysis.impl.VideoUsagePrefsManagerImpl;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoStartUpDelayImplTest extends BaseTest {
	private double startupDelay;
	private VideoStartUpDelayImpl startupDelayImpl;
	private PacketAnalyzerResult tracedata;
	private TraceDirectoryResult traceResults;
	private TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();
	private VideoUsage videoUsage;
	private VideoEvent videoEvent;
	private TreeMap videoEventList;
	private ArrayList<AROManifest> manifests;
	private AROManifest manifest;
	private TreeMap chunkPlayTimeList;

	private double pcapTS = 0D;
	private long traceStartTS = 0L;
	private double manReqTS = 0D;
	private double vidPlay = 0D;
	private double manDelTS = 0D;

	private VideoUsagePrefsManagerImpl videoPref;
	private double warnVal;
	private double failVal;
	private VideoStartUpDelayResult result;

	@Before
	public void setup() {
		startupDelayImpl = (VideoStartUpDelayImpl) context.getBean("startupDelay");
		result = new VideoStartUpDelayResult();
		
		videoPref = (VideoUsagePrefsManagerImpl) context.getBean("videoUsagePrefsManagerImpl");
		warnVal = Double.parseDouble(videoPref.getVideoUsagePreference().getStartUpDelayWarnVal());
		failVal = Double.parseDouble(videoPref.getVideoUsagePreference().getStartUpDelayFailVal());

		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		traceResults = Mockito.mock(TraceDirectoryResult.class);

		tracedata = Mockito.mock(PacketAnalyzerResult.class);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);

		manifest = Mockito.mock(AROManifest.class);
		manifests = new ArrayList<>();
		manifests.add(manifest);

		TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();
		manifestCollection.put(1D, manifest);
		
		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));
		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);

		videoUsage = Mockito.mock(VideoUsage.class);

		chunkPlayTimeList = Mockito.mock(TreeMap.class);

		traceResults = Mockito.mock(TraceDirectoryResult.class);
		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);

		videoEvent = Mockito.mock(VideoEvent.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(2);
		Mockito.when(videoUsage.getSegmentCount()).thenReturn(10);
		Mockito.when(videoUsage.getValidSegmentCount()).thenReturn(10);
		Mockito.when(videoUsage.getNonValidSegmentCount()).thenReturn(0);

		double segment = 1234;
		double timestamp = 1234567890;
		videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp + 1, segment + 1), videoEvent);
		Mockito.when(manifest.getVideoEventList()).thenReturn(videoEventList);
		Mockito.when(manifest.getSegmentCount()).thenReturn((double) videoEventList.size());

	}

	@Test
	public void runTest() {
		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(null);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		Mockito.when(tracedata.getTraceresult().getPcapTimeOffset()).thenReturn(1.510699853456E9D);
		Mockito.when(tracedata.getTraceresult().getVideoStartTime()).thenReturn(1.510699855132E9D);
		Mockito.when(tracedata.getTraceresult().getTraceDateTime()).thenReturn(new Date(1510699853456L));

		result.setResultType(BPResultType.PASS);
		AbstractBestPracticeResult returnResult = startupDelayImpl.runTest(tracedata);
		assertEquals(returnResult.getBestPracticeType(), result.getBestPracticeType());
	}

	@Test
	public void runTest_resTypeFail() {
		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		Mockito.when(tracedata.getTraceresult().getPcapTimeOffset()).thenReturn(1.510699853456E9D);
		Mockito.when(tracedata.getTraceresult().getVideoStartTime()).thenReturn(1.510699855132E9D);
		Mockito.when(tracedata.getTraceresult().getTraceDateTime()).thenReturn(new Date(1510699853456L));

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();
		AROManifest manifest = Mockito.mock(AROManifest.class);
		manifestCollection.put(1.0, manifest);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		@SuppressWarnings("unchecked")
		Map<VideoEvent, Double> map = Mockito.mock(HashMap.class);
		Mockito.when(tracedata.getVideoUsage().getChunkPlayTimeList()).thenReturn(map);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.getVideoEventList()).thenReturn(new TreeMap<>());
		startupDelay = 10.0;
		Mockito.when(manifest.getDelay()).thenReturn(startupDelay);
		result.setResultType(BPResultType.PASS);
		startupDelayImpl.runTest(tracedata);
	}

	@Test
	public void runTest_two_manifests_selected_delays_not_set_CONFIG_REQUIRED() {

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

		Mockito.when(manifest.isSelected()).thenReturn(true).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoStartUpDelayResult) startupDelayImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("The actual startup delay has not yet been set. Click to set <a href=\"startup\"><b>Startup Delay</b></a>. To change startup preferences go to <a href=\"preferences\"><b>File->Preferences->Video</b></a>.");

		assertThat(result.getAboutText()).contains(
				"Streaming video requires a startup delay for smooth delivery. In order to manage buffer occupancy, it is important to understand the startup delay and determine a way to cover this delay for the user with messaging.");
		assertThat(result.getDetailTitle()).contains("Start-up Delay");
		assertThat(result.getOverviewTitle()).contains("Video: Start-up Delay");
	}

	@Test
	public void runTest_two_manifests_one_selected_delay_set_PASS() {

		manifests.add(manifest);

		pcapTS = 1546474227.484D;
		traceStartTS = 1546474227484L;
		manReqTS = 1546474279.885D;
		manDelTS = 52.59599995613098D;
		vidPlay = manDelTS + (warnVal / 2); // 58.76D;

		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));

		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);
		Mockito.when(manifest.getEndTime()).thenReturn(manDelTS);
		Mockito.when(manifest.getVideoEventSegment()).thenReturn(videoEvent);

		Mockito.when(videoEvent.getPlayTime()).thenReturn(vidPlay);

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(chunkPlayTimeList);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);

		Mockito.when(manifest.isSelected()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoStartUpDelayResult) startupDelayImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).startsWith("Your video had a startup delay of 1.195 seconds and passes the test.");

		assertThat(result.getAboutText()).contains(
				"Streaming video requires a startup delay for smooth delivery. In order to manage buffer occupancy, it is important to understand the startup delay and determine a way to cover this delay for the user with messaging.");
		assertThat(result.getDetailTitle()).contains("Start-up Delay");
		assertThat(result.getOverviewTitle()).contains("Video: Start-up Delay");
	}

	@Test
	public void runTest_two_manifests_one_selected_delay_set_WARNING() {

		manifests.add(manifest);

		pcapTS = 1546474227.484D;
		traceStartTS = 1546474227484L;
		manReqTS = 1546474279.885D;
		manDelTS = 52.59599995613098D;
		vidPlay = manDelTS + warnVal + ((failVal - warnVal) / 2); // 58.76D;

		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));

		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);
		Mockito.when(manifest.getEndTime()).thenReturn(manDelTS);
		Mockito.when(manifest.getVideoEventSegment()).thenReturn(videoEvent);

		Mockito.when(videoEvent.getPlayTime()).thenReturn(vidPlay);

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(chunkPlayTimeList);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);

		Mockito.when(manifest.isSelected()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoStartUpDelayResult) startupDelayImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.WARNING);
		assertThat(result.getResultText()).startsWith("Your video had a startup delay of 2.695 seconds, Your warning is set to");

		assertThat(result.getAboutText()).contains(
				"Streaming video requires a startup delay for smooth delivery. In order to manage buffer occupancy, it is important to understand the startup delay and determine a way to cover this delay for the user with messaging.");
		assertThat(result.getDetailTitle()).contains("Start-up Delay");
		assertThat(result.getOverviewTitle()).contains("Video: Start-up Delay");
	}

	@Test
	public void runTest_two_manifests_not_selected_delay_set_CONFIG_REQUIRED() {

		manifests.add(manifest);

		pcapTS = 1546474227.484D;
		traceStartTS = 1546474227484L;
		manReqTS = 1546474279.885D;
		manDelTS = 52.59599995613098D;
		vidPlay = manDelTS + failVal + 1; // 58.76D;

		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));

		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);
		Mockito.when(manifest.getEndTime()).thenReturn(manDelTS);
		Mockito.when(manifest.getVideoEventSegment()).thenReturn(videoEvent);

		Mockito.when(videoEvent.getPlayTime()).thenReturn(vidPlay);

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(chunkPlayTimeList);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);

		Mockito.when(manifest.isSelected()).thenReturn(false);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoStartUpDelayResult) startupDelayImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("No manifest is selected. Please select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");

		assertThat(result.getAboutText()).contains(
				"Streaming video requires a startup delay for smooth delivery. In order to manage buffer occupancy, it is important to understand the startup delay and determine a way to cover this delay for the user with messaging.");
		assertThat(result.getDetailTitle()).contains("Start-up Delay");
		assertThat(result.getOverviewTitle()).contains("Video: Start-up Delay");
	}

	@Test
	public void runTest_two_manifests_not_selected_delay_not_set_CONFIG_REQUIRED() {

		manifests.add(manifest);

		pcapTS = 1546474227.484D;
		traceStartTS = 1546474227484L;
		manReqTS = 1546474279.885D;
		manDelTS = 52.59599995613098D;
		vidPlay = manDelTS + failVal + 1; // 58.76D;

		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));

		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);
		Mockito.when(manifest.getEndTime()).thenReturn(manDelTS);
		Mockito.when(manifest.getVideoEventSegment()).thenReturn(videoEvent);

		Mockito.when(videoEvent.getPlayTime()).thenReturn(vidPlay);

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(chunkPlayTimeList);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);

		Mockito.when(manifest.isSelected()).thenReturn(false);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoStartUpDelayResult) startupDelayImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("No manifest is selected. Please select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");

		assertThat(result.getAboutText()).contains(
				"Streaming video requires a startup delay for smooth delivery. In order to manage buffer occupancy, it is important to understand the startup delay and determine a way to cover this delay for the user with messaging.");
		assertThat(result.getDetailTitle()).contains("Start-up Delay");
		assertThat(result.getOverviewTitle()).contains("Video: Start-up Delay");
	}

	@Test
	public void runTest_two_manifests_selected_delay_set_CONFIG_REQUIRED() {

		manifests.add(manifest);

		pcapTS = 1546474227.484D;
		traceStartTS = 1546474227484L;
		manReqTS = 1546474279.885D;
		manDelTS = 52.59599995613098D;
		vidPlay = manDelTS + failVal + 1; // 58.76D;

		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));

		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);
		Mockito.when(manifest.getEndTime()).thenReturn(manDelTS);
		Mockito.when(manifest.getVideoEventSegment()).thenReturn(videoEvent);

		Mockito.when(videoEvent.getPlayTime()).thenReturn(vidPlay);

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getChunkPlayTimeList()).thenReturn(null);


		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		manifestCollection.put(1D, manifest);
		manifestCollection.put(2D, manifest);

		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);

		result = (VideoStartUpDelayResult) startupDelayImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("The actual startup delay has not yet been set. Click to set <a href=\"startup\"><b>Startup Delay</b></a>. To change startup preferences go to <a href=\"preferences\"><b>File->Preferences->Video</b></a>.");

		assertThat(result.getAboutText()).contains(
				"Streaming video requires a startup delay for smooth delivery. In order to manage buffer occupancy, it is important to understand the startup delay and determine a way to cover this delay for the user with messaging.");
		assertThat(result.getDetailTitle()).contains("Start-up Delay");
		assertThat(result.getOverviewTitle()).contains("Video: Start-up Delay");
	}

}
