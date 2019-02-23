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
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.VideoResolutionQualityResult;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.pojo.DeviceDetail;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class VideoResolutionQualityImplTest extends BaseTest {

	private VideoResolutionQualityImpl videoResolutionQualityImpl;

	private PacketAnalyzerResult tracedata;
	private TraceDirectoryResult traceResults;

	private VideoResolutionQualityResult result;

	private ArrayList<AROManifest> manifests;
	private AROManifest manifest;
	private VideoEvent videoEvent;
	private VideoUsage videoUsage;
	private DeviceDetail deviceDetail;
	private TreeMap<Double, AROManifest> manifestCollection;
	TreeMap<String, VideoEvent> videoEventList;
	
	@Before
	public void setup() {
		videoResolutionQualityImpl = (VideoResolutionQualityImpl) context.getBean("videoResolutionQuality");

		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		
		manifestCollection = Mockito.mock(TreeMap.class);
		
		manifest = Mockito.mock(AROManifest.class);
		manifests = new ArrayList<>();
		manifests.add(manifest);
		manifests.add(manifest);
		
		videoUsage = Mockito.mock(VideoUsage.class);
		traceResults = Mockito.mock(TraceDirectoryResult.class);
		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		
		deviceDetail = Mockito.mock(DeviceDetail.class);
		
		videoEvent = Mockito.mock(VideoEvent.class);
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getNonValidSegmentCount()).thenReturn(1);
		Mockito.when(videoUsage.getSegmentCount()).thenReturn(10);
		Mockito.when(videoUsage.getValidSegmentCount()).thenReturn(10);
		Mockito.when(videoUsage.getNonValidSegmentCount()).thenReturn(0);
		
		Mockito.when(traceResults.getDeviceDetail()).thenReturn(deviceDetail );
		
		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventList()).thenReturn(videoEventList);
		Mockito.when(manifest.getSegmentCount()).thenReturn((double)videoEventList.size());
	}

	@Test
	public void testRunTest_android_phone_720P_should_PASS() throws Exception {

		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android");
		
		Mockito.when(videoEvent.getResolutionHeight())
				.thenReturn(220.0).thenReturn(720.0)
				.thenReturn(220.0).thenReturn(720.0);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("There were no segments beyond 720p in this video.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}

	@Test
	public void testRunTest_android_phone_1_Manifest_selected_1_notSelected_should_PASS() throws Exception {
		
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android");

		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventList()).thenReturn(videoEventList);
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(720.0).thenReturn(720.0);
		Mockito.when(manifest.isSelected()).thenReturn(false).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("There were no segments beyond 720p in this video.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}
	
	@Test
	public void testRunTest_iPad_and_1080P_should_PASS() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);
		
		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
		
		AROManifest manifest = Mockito.mock(AROManifest.class);
		ArrayList<AROManifest> manifests = new ArrayList<>();
		manifests.add(manifest);
		
		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		traceResults = Mockito.mock(TraceDirectoryResult.class);
		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		
		DeviceDetail deviceDetail = Mockito.mock(DeviceDetail.class);
		Mockito.when(traceResults.getDeviceDetail()).thenReturn(deviceDetail );
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("iPad ARO");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("iOS 42.0.1");
		
		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventList()).thenReturn(videoEventList);
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(1080.0).thenReturn(721.0);

		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getNonValidSegmentCount()).thenReturn(1);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("There were 2 segments beyond 720p in this video.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}
	
	@Test
	public void testRunTest_no_DPI_iPad_phone_and_1080P_should_PASS() throws Exception {
		
		DeviceDetail deviceDetail = Mockito.mock(DeviceDetail.class);
		Mockito.when(traceResults.getDeviceDetail()).thenReturn(deviceDetail );
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("iPad ARO");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getOsType()).thenReturn("iOS 42.0.1");
		
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(1080.0).thenReturn(721.0);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true).thenReturn(false);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("There were 2 segments beyond 720p in this video.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}
	
	@Test
	public void testRunTest_no_DPI_android_tablet_and_1080P_should_PASS() throws Exception {
		PacketAnalyzerResult tracedata = Mockito.mock(PacketAnalyzerResult.class);
		
		TreeMap<Double, AROManifest> manifestCollection = Mockito.mock(TreeMap.class);
		
		AROManifest manifest = Mockito.mock(AROManifest.class);
		ArrayList<AROManifest> manifests = new ArrayList<>();
		manifests.add(manifest);
		
		VideoUsage videoUsage = Mockito.mock(VideoUsage.class);
		traceResults = Mockito.mock(TraceDirectoryResult.class);
		Mockito.when(tracedata.getTraceresult()).thenReturn(traceResults);
		
		DeviceDetail deviceDetail = Mockito.mock(DeviceDetail.class);
		Mockito.when(traceResults.getDeviceDetail()).thenReturn(deviceDetail );
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("1080*7200");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(34.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android 42");
		
		double segment = 1234;
		double bitrate = 2300;
		double timestamp = 1234567890;
		
		VideoEvent videoEvent = Mockito.mock(VideoEvent.class);
		TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp, segment), videoEvent);
		videoEventList.put(String.format("%010.4f:%08.0f", timestamp+1, segment+1), videoEvent);
		Mockito.when(manifest.getVideoEventList()).thenReturn(videoEventList);
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(1080.0).thenReturn(721.0);


		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(1);
		Mockito.when(videoUsage.getNonValidSegmentCount()).thenReturn(1);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.PASS);
		assertThat(result.getResultText()).contains("There were 2 segments beyond 720p in this video.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}
	
	// SELFTEST
	@Test
	public void testRunTest_android_phone_no_resolution_should_SELFTEST() throws Exception {
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android");
		
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(0.0).thenReturn(0.0);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true).thenReturn(false);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.SELF_TEST);
		assertThat(result.getResultText()).contains("No video resolution data was found for device tested.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}

	@Test
	public void testRunTest_no_DPI_android_phone_and_1080P_should_SELFTEST() throws Exception {
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("1080*7200");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(0.0);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android");
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(1080.0).thenReturn(721.0);

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.SELF_TEST);
		assertThat(result.getResultText()).contains("There were 2 segments beyond 720p in this video. Screen dots per inch (DPI) had not been collected on this trace, therefore unable to distinguish between tablet and phone.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}
	
	// FAIL	
	@Test
	public void testRunTest_android_phone_and_1080P_should_FAIL() throws Exception {
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android");
		
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(1080.0).thenReturn(721.0);

		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.FAIL);
		assertThat(result.getResultText()).contains("There were 2 segments beyond 720p in this video.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}
	
	@Test
	public void testRunTest_iPhone_and_1080P_should_FAIL() throws Exception {
		
		DeviceDetail deviceDetail = Mockito.mock(DeviceDetail.class);
		Mockito.when(traceResults.getDeviceDetail()).thenReturn(deviceDetail );
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("iPhone ARO");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("iOS 42.0.1");
		
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(1080.0).thenReturn(721.0);

		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(true);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.FAIL);
		assertThat(result.getResultText()).contains("There were 2 segments beyond 720p in this video.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}
	
	// NO_DATA
	@Test
	public void testRunTest_android_phone_no_Manifests_should_NO_DATA() throws Exception {
		
		manifests = new ArrayList<>();
		manifestCollection = new TreeMap<Double, AROManifest>();

		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android");
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.NO_DATA);
		assertThat(result.getResultText()).contains("No streaming video data found.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}

	// CONFIG_REQUIRED
	@Test
	public void testRunTest_android_phone_no_Manifests_selected_should_CONFIG_REQUIRED() throws Exception {
		
		DeviceDetail deviceDetail = Mockito.mock(DeviceDetail.class);
		Mockito.when(traceResults.getDeviceDetail()).thenReturn(deviceDetail );
		Mockito.when(deviceDetail.getDeviceModel()).thenReturn("AroPhone");
		Mockito.when(deviceDetail.getScreenSize()).thenReturn("720*1040");
		Mockito.when(deviceDetail.getScreenDensity()).thenReturn(340.56);
		Mockito.when(deviceDetail.getOsType()).thenReturn("android");
		
		Mockito.when(videoEvent.getResolutionHeight()).thenReturn(0.0).thenReturn(0.0);
		
		Mockito.when(tracedata.getVideoUsage()).thenReturn(videoUsage);
		Mockito.when(videoUsage.getAroManifestMap()).thenReturn(manifestCollection);
		Mockito.when(manifestCollection.values()).thenReturn(manifests);
		Mockito.when(manifest.isSelected()).thenReturn(false);
		Mockito.when(manifest.isValid()).thenReturn(true);
		Mockito.when(manifest.isVideoMetaDataExtracted()).thenReturn(false);
		
		Mockito.when(videoUsage.getSelectedManifestCount()).thenReturn(0);
		Mockito.when(videoUsage.getNonValidSegmentCount()).thenReturn(0);
		
		result = (VideoResolutionQualityResult)videoResolutionQualityImpl.runTest(tracedata);

		assertThat(result.getResultType()).isSameAs(BPResultType.CONFIG_REQUIRED);
		assertThat(result.getResultText()).contains("Invalid manifests do not have enough information for analyzing streaming video.<br /> Hint: look for ways to locate segment information with the Video Parser Wizard. Click here to select Request URL <a href=\"selectManifest\"><b> Manifest</b></a> on the Video tab.");
		
		assertThat(result.getAboutText()).contains("Studies show that users do not see an improvement in quality by going beyond 720p on smart phones.");
		assertThat(result.getDetailTitle()).contains("Resolution and Perception");
		assertThat(result.getOverviewTitle()).contains("Video: Resolution and Perception");
	}

	
}
