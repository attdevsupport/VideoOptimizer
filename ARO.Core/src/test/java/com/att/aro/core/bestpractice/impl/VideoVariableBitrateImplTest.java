/*
*  Copyright 2019 AT&T
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

import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.bestpractice.pojo.VideoVariableBitrateResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.videoanalysis.impl.VideoUsagePrefsManagerImpl;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoVariableBitrateImplTest extends BaseTest {

	private VideoVariableBitrateImpl videoVariableBitrateImpl;

	private PacketAnalyzerResult tracedata;
	private TraceDirectoryResult traceResults;
	private TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();
	private VideoUsage videoUsage;
	private VideoEvent videoEvent;
	private TreeMap<String, VideoEvent> videoEventList;
	private ArrayList<AROManifest> manifests;
	private AROManifest manifest;

	private double pcapTS = 0D;
	private long traceStartTS = 0L;
	private double manReqTS = 0D;
	private double vidPlay = 0D;
	private VideoVariableBitrateResult result;

	@Before
	public void setup() {
		videoVariableBitrateImpl = (VideoVariableBitrateImpl) context.getBean("videoVariableBitrate");

		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		traceResults = Mockito.mock(TraceDirectoryResult.class);

		tracedata = Mockito.mock(PacketAnalyzerResult.class);

		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);

		manifest = Mockito.mock(AROManifest.class);
		manifests = new ArrayList<>();
		manifests.add(manifest);

		manifestCollection = new TreeMap<>();
		manifestCollection.put(1D, manifest);
		
		Mockito.when(traceResults.getPcapTimeOffset()).thenReturn(pcapTS);
		Mockito.when(traceResults.getVideoStartTime()).thenReturn(vidPlay);
		Mockito.when(traceResults.getTraceDateTime()).thenReturn(new Date(traceStartTS));
		Mockito.when(manifest.getRequestTime()).thenReturn(manReqTS);

		videoUsage = Mockito.mock(VideoUsage.class);

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
	public void testRunTest_noTraceData_expect_PASS() throws Exception {
		PacketAnalyzerResult tracedata = null;
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);
		
		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}

	@Test
	public void testRunTest_no_VideoMetaDataExtracted() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);
		
		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
//		
//		AROManifest manifest = Mockito.mock(AROManifest.class);
//		ArrayList<AROManifest> manifests = new ArrayList<>();
//		manifests.add(manifest);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.SELF_TEST);
		assertThat(result.getResultText()).contains("Digital rights management (DRM) is preventing analysis of this test.");
		
		
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}
	
	@Test
	public void testRunTest_one_manifest_use_VBR_PASS() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);
		
		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
//		
//		AROManifest manifest = Mockito.mock(AROManifest.class);
//		ArrayList<AROManifest> manifests = new ArrayList<>();
//		manifests.add(manifest);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifestCollection.size()).thenReturn(1);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(true);

		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getBitrate()).thenReturn(bitrate).thenReturn(bitrate+1);
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("This stream appears to be using Variable Bitrate.");
		
		
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}

	@Test
	public void testRunTest_all_invalid_manifest_should_CONFIG_REQUIRED() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);
		
		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
		
		AROManifest manifest = Mockito.mock(AROManifest.class);
		ArrayList<AROManifest> manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(2);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests).thenReturn(manifests);
		Mockito.when(manifestCollection.size()).thenReturn(2);
		Mockito.when(manifest.isValid()).thenReturn(false).thenReturn(false);
		Mockito.when(manifest.isSelected()).thenReturn(false).thenReturn(false);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(true);
		Mockito.when(manifest.getSegmentCount()).thenReturn(2D);

		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getBitrate()).thenReturn(bitrate).thenReturn(bitrate+1);
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("Invalid manifests do not have enough information for analyzing streaming video.<br /> Hint: look for ways to locate segment information with the Video Parser Wizard. Click here to select Request URL <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}

	@Test
	public void testRunTest_no_vbr_used_should_WARNING() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);

		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);

		AROManifest manifest = Mockito.mock(AROManifest.class);
		ArrayList<AROManifest> manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);
		
//		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifestCollection.size()).thenReturn(2);
		Mockito.when(manifest.isValid()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(true);

		double segment = 1234;
		double bitrate = 0;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment), videoEvent);
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getBitrate()).thenReturn(bitrate).thenReturn(bitrate);
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.WARNING);
		assertThat(result.getResultText()).contains("This stream does not appear to be using Variable Bitrate.");		
		
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}

	@Test
	public void testRunTest_one_invalid_manifest_one_good_should_PASS() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);

		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
		
		AROManifest manifest = Mockito.mock(AROManifest.class);
		ArrayList<AROManifest> manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifestCollection.size()).thenReturn(2);
		Mockito.when(manifest.isValid()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(true);

		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getBitrate()).thenReturn(bitrate).thenReturn(bitrate+1);
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("This stream appears to be using Variable Bitrate.");		
		
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}

	@Test
	public void testRunTest_all_deselected_manifests_should_CONFIG_REQUIRED() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);

		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
		
		AROManifest manifest = Mockito.mock(AROManifest.class);
		ArrayList<AROManifest> manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);
		
		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifestCollection.size()).thenReturn(2);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isSelected()).thenReturn(false);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(true);

		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getBitrate()).thenReturn(bitrate).thenReturn(bitrate+1);
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("No manifest is selected. Please select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");		
		
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}

	@Test
	public void testRunTest_all_selected_manifests_should_CONFIG_REQUIRED() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);

		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
		
		AROManifest manifest = Mockito.mock(AROManifest.class);
		ArrayList<AROManifest> manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(2);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifestCollection.size()).thenReturn(2);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(true);

		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
		Mockito.when(videoEvent.getBitrate()).thenReturn(bitrate).thenReturn(bitrate+1);
		
		result = (VideoVariableBitrateResult)videoVariableBitrateImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("Please select only one manifest on the Video tab.  Click here to select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");		
		
		assertThat(result.getAboutText()).contains("Variable Bitrate (VBR) video encoding offers a number of advantages over Constant Bitrate (CBR) in terms of improved video quality.  It is recommended that Variable Bitrate (VBR) video encoding be used for streaming.");
		assertThat(result.getDetailTitle()).contains("Variable Bitrate");
		assertThat(result.getOverviewTitle()).contains("Video: Variable Bitrate");
	}


}
