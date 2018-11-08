
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

import static org.junit.Assert.assertEquals;

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
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoStartUpDelayImplTest extends BaseTest{

//	private IVideoUsagePrefsManager videoPref;
	private double startupDelay;
	private VideoStartUpDelayImpl startupDelayImpl;
	private PacketAnalyzerResult tracedata;
	
	@Before
	public void setup() {
		startupDelayImpl = (VideoStartUpDelayImpl) context.getBean("startupDelay");
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
	}
	
	@Test
	public void runTest(){
		VideoStartUpDelayResult result = new VideoStartUpDelayResult();
		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(null);
		
		//result.setResultText(textResultEmpty);
		result.setResultType(BPResultType.PASS);
		AbstractBestPracticeResult returnResult = startupDelayImpl.runTest(tracedata);
		assertEquals(returnResult.getBestPracticeType(), result.getBestPracticeType());
	}
	
	@Test
	public void runTest_resTypeFail(){
		VideoStartUpDelayResult result = new VideoStartUpDelayResult();
		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		TreeMap<Double, AROManifest> manifestCollection = new TreeMap<>();
		AROManifest manifest = Mockito.mock(AROManifest.class);
		manifestCollection.put(1.0, manifest);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Map<VideoEvent,Double> map = Mockito.mock(HashMap.class);
		Mockito.when(tracedata.getVideoUsage().getChunkPlayTimeList()).thenReturn(map);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.getVideoEventList()).thenReturn(new TreeMap<>());
		startupDelay = 10.0;
		Mockito.when(manifest.getDelay()).thenReturn(startupDelay);
		result.setResultType(BPResultType.PASS);
		startupDelayImpl.runTest(tracedata);
	}
}

