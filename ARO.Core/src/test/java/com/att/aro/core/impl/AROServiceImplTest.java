/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.PeriodicTransferResult;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.ICacheAnalysis;
import com.att.aro.core.packetanalysis.IPacketAnalyzer;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.CacheAnalysis;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceFileResult;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.pojo.VersionInfo;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.mvc.IAROView;

public class AROServiceImplTest extends BaseTest {
	private static final int TOTAL_BPTESTS = 45;
	@InjectMocks
	AROServiceImpl aro;
	@Mock
	IPacketAnalyzer packetanalyzer;
	@Mock
	ICacheAnalysis cacheAnalyzer;
	@Mock
	IBestPractice worker;
	@Mock
	IFileManager fileManager;
	@Mock
	transient VersionInfo info;
	@Mock(name = "periodicTransfer")
	IBestPractice periodicTransfer;
	@Mock(name = "unnecessaryConnection")
	IBestPractice unnecessaryConnection;
	@Mock(name = "connectionOpening")
	IBestPractice connectionOpening;
	@Mock(name = "connectionClosing")
	IBestPractice connectionClosing;
	@Mock(name = "wifiOffloading")
	IBestPractice wifiOffloading;
	@Mock(name = "screenRotation")
	IBestPractice screenRotation;
	@Mock(name = "prefetching")
	IBestPractice prefetching;
	@Mock(name = "accessingPeripheral")
	IBestPractice accessingPeripheral;
	@Mock(name = "combineCsJss")
	IBestPractice combineCsJss;
	@Mock(name = "http10Usage")
	IBestPractice http10Usage;
	@Mock(name = "cacheControl")
	IBestPractice cacheControl;
	@Mock(name = "usingCache")
	IBestPractice usingCache;
	@Mock(name = "duplicateContent")
	IBestPractice duplicateContent;
	@Mock(name = "http4xx5xx")
	IBestPractice http4xx5xx;
	@Mock(name = "http3xx")
	IBestPractice http3xx;
	@Mock(name = "textFileCompression")
	IBestPractice textFileCompression;
	@Mock(name = "imageSize")
	IBestPractice imageSize;
	@Mock(name = "minify")
	IBestPractice minify;
	@Mock(name = "emptyUrl")
	IBestPractice emptyUrl;
	@Mock(name = "spriteImage")
	IBestPractice spriteImage;
	@Mock(name = "scripts")
	IBestPractice scripts;
	@Mock(name = "async")
	IBestPractice async;
	@Mock(name = "displaynoneincss")
	IBestPractice displaynoneincss;
	@Mock(name = "fileorder")
	IBestPractice fileorder;
	@Mock(name = "videoStall")
	IBestPractice videoStall;
	@Mock(name = "networkComparison")
	IBestPractice networkComparison;
	@Mock(name = "startupDelay")
	IBestPractice startupDelay;
	@Mock(name = "bufferOccupancy")
	IBestPractice bufferOccupancy;
	@Mock(name = "tcpConnection")
	IBestPractice tcpConnection;
	@Mock(name = "chunkPacing")
	IBestPractice chunkPacing;
	@Mock(name = "chunkSize")
	IBestPractice chunkSize;
	@Mock(name = "videoRedundancy")
	IBestPractice videoRedundancy;
	@Mock(name = "videoConcurrentSession")
	IBestPractice videoConcurrentSessions;
	@Mock(name = "videoVariableBitrate")
	IBestPractice videoVariableBitrate;
	@Mock(name = "simultaneous")
	IBestPractice simultaneous;
	@Mock(name = "multipleSimultaneous")
	IBestPractice multipleSimultaneous;
	/*@Mock(name = "adAnalytics")
	IBestPractice adAnalytics;*/
	@Mock(name = "httpsUsage")
	IBestPractice httpsUsage;
	@Mock(name = "transmissionPrivateData")
	IBestPractice transmissionPrivateData;
	

	@Before
	public void setup() {
		aro = new AROServiceImpl();
		fileManager = mock(IFileManager.class);
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void reset() {
		Mockito.reset(packetanalyzer);
		Mockito.reset(cacheAnalyzer);
		Mockito.reset(worker);
	}

	@Test
	public void getNameTest() {
		when(info.getName()).thenReturn("ARO");
		String name = aro.getName();
		assertNotNull(name);
	}

	@Test
	public void getVersionTest() {
		when(info.getVersion()).thenReturn("5.0");
		String version = aro.getVersion();
		assertNotNull(version);
	}

	@Test
	public void analyzeFileNullTest() throws IOException {
		PacketAnalyzerResult analyze = new PacketAnalyzerResult();
		TraceFileResult traceresult = new TraceFileResult();
		List<PacketInfo> allpackets = new ArrayList<PacketInfo>();
		allpackets.add(new PacketInfo(new Packet(0, 0, 0, 0, null)));
		analyze.setTraceresult(traceresult);
		List<BestPracticeType> req = new ArrayList<BestPracticeType>();
		req.add(BestPracticeType.UNNECESSARY_CONNECTIONS);
		AROTraceData testResult = aro.analyzeFile(req, "traffic.cap");
		assertEquals(null, testResult.getBestPracticeResults());
	}

	@Test
	public void analyzeFileTest() throws IOException {
		PacketAnalyzerResult analyze = new PacketAnalyzerResult();
		TraceFileResult traceresult = new TraceFileResult();
		List<PacketInfo> allpackets = new ArrayList<PacketInfo>();
		allpackets.add(new PacketInfo(new Packet(0, 0, 0, 0, null)));
		traceresult.setAllpackets(allpackets);
		analyze.setTraceresult(traceresult);
		PeriodicTransferResult periodicTransferResult = new PeriodicTransferResult();
		List<BestPracticeType> req = new ArrayList<BestPracticeType>();
		req.add(BestPracticeType.UNNECESSARY_CONNECTIONS);
		req.add(BestPracticeType.CONNECTION_CLOSING);
		req.add(BestPracticeType.CONNECTION_OPENING);
		req.add(BestPracticeType.PERIODIC_TRANSFER);
		req.add(BestPracticeType.SCREEN_ROTATION);
		req.add(BestPracticeType.ACCESSING_PERIPHERALS);
		req.add(BestPracticeType.COMBINE_CS_JSS);
		req.add(BestPracticeType.HTTP_1_0_USAGE);
		req.add(BestPracticeType.CACHE_CONTROL);
		req.add(BestPracticeType.USING_CACHE);
		req.add(BestPracticeType.DUPLICATE_CONTENT);
		req.add(BestPracticeType.HTTP_4XX_5XX);
		req.add(BestPracticeType.HTTP_3XX_CODE);
		req.add(BestPracticeType.FILE_COMPRESSION);
		req.add(BestPracticeType.IMAGE_SIZE);
		req.add(BestPracticeType.MINIFICATION);
		req.add(BestPracticeType.EMPTY_URL);
		req.add(BestPracticeType.SPRITEIMAGE);
		req.add(BestPracticeType.SCRIPTS_URL);
		req.add(BestPracticeType.ASYNC_CHECK);
		req.add(BestPracticeType.DISPLAY_NONE_IN_CSS);
		req.add(BestPracticeType.FILE_ORDER);
		req.add(BestPracticeType.MULTI_SIMULCONN);
		req.add(BestPracticeType.VIDEO_STALL);
		req.add(BestPracticeType.STARTUP_DELAY);
		req.add(BestPracticeType.BUFFER_OCCUPANCY);
		req.add(BestPracticeType.NETWORK_COMPARISON);
		req.add(BestPracticeType.TCP_CONNECTION);
		req.add(BestPracticeType.CHUNK_SIZE);
		req.add(BestPracticeType.CHUNK_PACING);
		req.add(BestPracticeType.VIDEO_REDUNDANCY);
		req.add(BestPracticeType.VIDEO_CONCURRENT_SESSION);
		req.add(BestPracticeType.VIDEO_VARIABLE_BITRATE);
		req.add(BestPracticeType.HTTPS_USAGE);
		req.add(BestPracticeType.TRANSMISSION_PRIVATE_DATA);
		req.add(BestPracticeType.DISPLAY_NONE_IN_CSS);
		packetanalyzer = Mockito.mock(IPacketAnalyzer.class);
		aro.setPacketAnalyzer(packetanalyzer);
		when(packetanalyzer.analyzeTraceFile(any(String.class), any(Profile.class), any(AnalysisFilter.class)))
				.thenReturn(analyze);
		when(worker.runTest(any(PacketAnalyzerResult.class))).thenReturn(periodicTransferResult);
		List<BestPracticeType> list = SettingsUtil.retrieveBestPractices();
		SettingsUtil.saveBestPractices(req);
		try {
			AROTraceData testResult = aro.analyzeFile(req, "traffic.cap");
			assertEquals(TOTAL_BPTESTS, testResult.getBestPracticeResults().size());
		} finally {
			SettingsUtil.saveBestPractices(list);
		}
	}

	@Test
	public void analyzeFileTest_resultIsNull() throws IOException {
		when(packetanalyzer.analyzeTraceFile(any(String.class), any(Profile.class), any(AnalysisFilter.class)))
				.thenReturn(null);
		List<BestPracticeType> req = new ArrayList<BestPracticeType>();
		AROTraceData testResult = aro.analyzeFile(req, "traffic.cap");
		assertEquals(104, testResult.getError().getCode());
		assertFalse(testResult.isSuccess());
	}

	@Test
	public void analyzeDirectoryTest() throws IOException {
		TraceDirectoryResult traceresult = new TraceDirectoryResult();
		List<PacketInfo> allpackets = new ArrayList<PacketInfo>();
		allpackets.add(new PacketInfo(new Packet(0, 0, 0, 0, null)));
		traceresult.setAllpackets(allpackets);
		PacketAnalyzerResult analyze = new PacketAnalyzerResult();
		analyze.setTraceresult(traceresult);
		CacheAnalysis cacheAnalysis = new CacheAnalysis();
		PeriodicTransferResult periodicTransferResult = new PeriodicTransferResult();
		List<BestPracticeType> req = new ArrayList<BestPracticeType>();
		req.add(BestPracticeType.UNNECESSARY_CONNECTIONS);
		req.add(BestPracticeType.CONNECTION_CLOSING);
		req.add(BestPracticeType.CONNECTION_OPENING);
		req.add(BestPracticeType.PERIODIC_TRANSFER);
		req.add(BestPracticeType.SCREEN_ROTATION);
		req.add(BestPracticeType.ACCESSING_PERIPHERALS);
		req.add(BestPracticeType.COMBINE_CS_JSS);
		req.add(BestPracticeType.HTTP_1_0_USAGE);
		req.add(BestPracticeType.CACHE_CONTROL);
		req.add(BestPracticeType.USING_CACHE);
		req.add(BestPracticeType.DUPLICATE_CONTENT);
		req.add(BestPracticeType.HTTP_4XX_5XX);
		req.add(BestPracticeType.HTTP_3XX_CODE);
		req.add(BestPracticeType.FILE_COMPRESSION);
		req.add(BestPracticeType.IMAGE_SIZE);
		req.add(BestPracticeType.MINIFICATION);
		req.add(BestPracticeType.EMPTY_URL);
		req.add(BestPracticeType.SPRITEIMAGE);
		req.add(BestPracticeType.SCRIPTS_URL);
		req.add(BestPracticeType.ASYNC_CHECK);
		req.add(BestPracticeType.DISPLAY_NONE_IN_CSS);
		req.add(BestPracticeType.FILE_ORDER);
		req.add(BestPracticeType.VIDEO_STALL);
		req.add(BestPracticeType.STARTUP_DELAY);
		req.add(BestPracticeType.BUFFER_OCCUPANCY);
		req.add(BestPracticeType.NETWORK_COMPARISON);
		req.add(BestPracticeType.TCP_CONNECTION);
		req.add(BestPracticeType.CHUNK_SIZE);
		req.add(BestPracticeType.CHUNK_PACING);
		req.add(BestPracticeType.VIDEO_REDUNDANCY);
		req.add(BestPracticeType.VIDEO_VARIABLE_BITRATE);
		req.add(BestPracticeType.HTTPS_USAGE);
		req.add(BestPracticeType.TRANSMISSION_PRIVATE_DATA);
		req.add(BestPracticeType.DISPLAY_NONE_IN_CSS);
		req.add(BestPracticeType.VIDEO_CONCURRENT_SESSION);
		req.add(BestPracticeType.AUDIO_STREAM);
		req.add(BestPracticeType.MULTI_SIMULCONN);
		List<BestPracticeType> list = SettingsUtil.retrieveBestPractices();
		SettingsUtil.saveBestPractices(req);
		when(packetanalyzer.analyzeTraceDirectory(any(String.class), any(IAROView.class), any(Profile.class), any(AnalysisFilter.class)))
				.thenReturn(analyze);
		when(worker.runTest(any(PacketAnalyzerResult.class))).thenReturn(periodicTransferResult);
		when(cacheAnalyzer.analyze(anyListOf(Session.class))).thenReturn(cacheAnalysis);
		try {
			AROTraceData testResult = aro.analyzeDirectory(req, Util.getCurrentRunningDir(), null);
			assertEquals(null, testResult.getBestPracticeResults());
		} finally {
			SettingsUtil.saveBestPractices(list);
		}
	}

	@Test
	public void analyzeDirectoryTest_resultIsNull() throws IOException {
		List<BestPracticeType> req = new ArrayList<BestPracticeType>();
		when(packetanalyzer.analyzeTraceDirectory(any(String.class), any(IAROView.class), any(Profile.class), any(AnalysisFilter.class)))
				.thenReturn(null);
		AROTraceData testResult = aro.analyzeDirectory(req, Util.getCurrentRunningDir(), null);
		assertEquals(108, testResult.getError().getCode());
		assertFalse(testResult.isSuccess());
	}
}
