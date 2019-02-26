
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

import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoChunkPacingResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoSegmentPacingImplTest extends BaseTest{

	private VideoSegmentPacingImpl videoSegmentPacingImpl;
	private PacketAnalyzerResult tracedata;
	private AROManifest manifest;
	private VideoUsage videoUsage;
	private VideoEvent videoEvent;
	private TreeMap<String, VideoEvent> videoEventList;

	@Before
	public void setup() {
		videoSegmentPacingImpl = (VideoSegmentPacingImpl) context.getBean("chunkPacing");
		
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
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
		Mockito.when(manifest.getVideoEventsBySegment()).thenReturn(videoEventList.values());
	}
	
	@Test
	public void runTest_all_invalid_Manifests_should_CONFIG_REQUIRED(){
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(2);
		
		VideoChunkPacingResult result = (VideoChunkPacingResult) videoSegmentPacingImpl.runTest(tracedata);
		assertThat(result.getResultText()).contains("Invalid manifests do not have enough information for analyzing streaming video.<br /> Hint: look for ways to locate segment information with the Video Parser Wizard. Click here to select Request URL <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
	}
	
	@Test
	public void runTest_all_deselected_Manifests_should_CONFIG_REQUIRED(){
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(0);
		
		VideoChunkPacingResult result = (VideoChunkPacingResult) videoSegmentPacingImpl.runTest(tracedata);
		assertThat(result.getResultText()).contains("No manifest is selected. Please select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
	}
	
	@Test
	public void runTest_all_orMorethanOne_selected_Manifests_should_CONFIG_REQUIRED(){
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(2);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(0);
		
		VideoChunkPacingResult result = (VideoChunkPacingResult) videoSegmentPacingImpl.runTest(tracedata);
		assertThat(result.getResultText()).contains("Please select only one manifest on the Video tab.  Click here to select a <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
	}
	
	@Test
	public void runTest_one_selected_Manifest_expect_SELFTEST(){
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getInvalidManifestCount()).thenReturn(0);
		Mockito.when(manifest.isSelected()).thenReturn(true);

		VideoChunkPacingResult result = (VideoChunkPacingResult) videoSegmentPacingImpl.runTest(tracedata);
		assertThat(result.getResultText()).contains("There were 2 different chunks which were delivered with pace of 0 seconds. Consider reducing/increasing the pace of chunks you are sending.");
		assertThat(result.getResultType()).isSameAs(BPResultType.SELF_TEST);	
	}
	
	@Test
	public void runTest_noData(){
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(null);
		VideoChunkPacingResult result = (VideoChunkPacingResult) videoSegmentPacingImpl.runTest(tracedata);
		assertThat(result.getResultText()).contains("No streaming video data found.");
		assertThat(result.getResultType()).isSameAs(BPResultType.NO_DATA);
	}
}

