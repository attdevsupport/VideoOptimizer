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
package com.att.aro.core.peripheral.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.att.aro.core.BaseTest;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.pojo.VideoTime;
import com.att.aro.core.util.Util;

public class VideoTimeReaderImplTest extends BaseTest {
	
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	private File exVideoDisplayFileName;
	private File nativeVideoDisplayfile;
	private File VIDEO_TIME_FILE;
	private File EXVIDEO_TIME_FILE;
	private String tracePath;
	private File nativeVideoFileOnDevice;
	private AbstractTraceResult traceResult;

	@InjectMocks
	VideoTimeReaderImpl videoTimeReaderImpl;

	@InjectMocks
	private FileManagerImpl fileReader;

	@Mock
	private IExternalProcessRunner extRunner;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		videoTimeReaderImpl.setFileReader(fileReader);
		tracePath = folder.getRoot().toString();
		traceResult = new TraceDirectoryResult();
	}

	@After
	public void destroy() {
		folder.delete();
	}
	
	/**
	 * create & populate file
	 * 
	 * @param fileNameStr
	 * @param string_data to populate file
	 * @return filename
	 */
	private File makeFile(String fileNameStr, String[] strings) {
		File fileName = null;
		BufferedWriter out;
		try {
			fileName = folder.newFile(fileNameStr);
			if (strings != null) {
				out = new BufferedWriter(new FileWriter(fileName));
				for (String dataLine : strings) {
					out.write(dataLine);
				}
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileName;
	}


	/*
	 * no exVideo_time to find
	 */
	@Test
	public void readData0() throws IOException {
		

		Date traceDateTime = new Date((long) 1414092264446.0);
		VideoTime videoTime = null;
		traceResult.setTraceDateTime(traceDateTime);

		nativeVideoDisplayfile = makeFile("exvideo.mov", null);
		videoTime = videoTimeReaderImpl.readData(tracePath, traceResult);
		assertEquals(true, videoTime.isExVideoTimeFileNotFound());
		nativeVideoDisplayfile.delete();
		nativeVideoDisplayfile = null;
	}

	/*
	 * run test via Mockito with full data "File", has start and stop time
	 * mock: "video_time"
	 */
	@Test
	public void readData1() throws IOException {
		

		Date traceDateTime = new Date((long) 1414092264446.0);
		traceResult.setTraceDateTime(traceDateTime);
		VideoTime videoTime = null;

		VIDEO_TIME_FILE = makeFile("video_time", new String[] { "1.41409226371E9 1.414092261198E9" });
		videoTime = videoTimeReaderImpl.readData(tracePath, traceResult);
		assertEquals(1.4140922669580002E9, videoTime.getVideoStartTime(), 0);
		assertEquals(true, videoTime.isDeviceScreenVideo());
		assertEquals(false, videoTime.isExVideoFound());
		assertEquals(false, videoTime.isExVideoTimeFileNotFound());
		VIDEO_TIME_FILE.delete();
		VIDEO_TIME_FILE = null;

	}

	/*
	 * run test via Mockito with full data "File", has start and stop time
	 * mock: "video.mov" "video_time"
	 */
	@Test
	public void readData2() throws IOException {
		

		Date traceDateTime = new Date((long) 1414092264446.0);
		traceResult.setTraceDateTime(traceDateTime);
		VideoTime videoTime = null;

		nativeVideoDisplayfile = makeFile("video.mov", null);
		VIDEO_TIME_FILE = makeFile("video_time", new String[] { "1.41409226371E9 1.414092261198E9" });
		videoTime = videoTimeReaderImpl.readData(tracePath, traceResult);
		assertEquals(1.4140922669580002E9, videoTime.getVideoStartTime(), 0);
		assertEquals(true, videoTime.isDeviceScreenVideo());
		assertEquals(false, videoTime.isExVideoFound());
		assertEquals(false, videoTime.isExVideoTimeFileNotFound());
		nativeVideoDisplayfile.delete();
		nativeVideoDisplayfile = null;
		VIDEO_TIME_FILE.delete();
		VIDEO_TIME_FILE = null;

	}
	
	/*
	 * run test via Mockito with full data "File", has start and stop time
	 * mock: "exvideo.mov" "exVideo_time"
	 */
	@Test
	public void readData3() throws IOException {
		

		Date traceDateTime = new Date((long) 1414092264446.0);
		traceResult.setTraceDateTime(traceDateTime);
		VideoTime videoTime = null;

		
		exVideoDisplayFileName = makeFile("exvideo.mov", null);
		EXVIDEO_TIME_FILE = makeFile("exVideo_time", new String[] { "1.41409226371E9 1.414092261198E9" });
		videoTime = videoTimeReaderImpl.readData(tracePath, traceResult);
		assertEquals(1.4140922669580002E9, videoTime.getVideoStartTime(), 0);
		assertEquals(false, videoTime.isDeviceScreenVideo());
		assertEquals(true, videoTime.isExVideoFound());
		assertEquals(false, videoTime.isExVideoTimeFileNotFound());
		videoTimeReaderImpl.isExternalVideoSourceFilePresent("video.mp4", "video.mov", true, tracePath);
		exVideoDisplayFileName.delete();
		exVideoDisplayFileName = null;
		EXVIDEO_TIME_FILE.delete();
		EXVIDEO_TIME_FILE = null;
	}
	
	/*
	 * testing an embedded protected function
	 * boolean isExternalVideoSourceFilePresent(String nativeVideoFileOnDevice, String nativeVideoDisplayfile,boolean isPcap, String traceDirectory)
	 */
	@Test
	public void readData4() throws IOException {
		makeFile("video.dv", null);
		makeFile("video.qt", null);
		makeFile("video.mev", null);
		makeFile("video.m4", null);
		makeFile("video_time", new String[] { "1.41409226371E9 1.414092261198E9" });

		boolean r = videoTimeReaderImpl.isExternalVideoSourceFilePresent("video.mp4", "video.mov", true, tracePath);
		assertTrue("should not have found \"video.mov\" or \"video.mp4\"", !r);

		makeFile("video.mov", null);
		r = videoTimeReaderImpl.isExternalVideoSourceFilePresent("video.mp4", "video.mov", true, tracePath);
		assertTrue("should have found \"video.mov\"", r);

	}
	
	/**
	 *  test to compute & update video start time from the video.mp4 ffmpeg
	 *  Using (1)traceDateTime
	 *        (2)trace start time (pcap0Time)
	 *        (3)Video creationTime (time when video was closed
	 *        (4)Video duration
	 *        
	 *  VideoCreation time was deliberately altered to look as if the phone's time zone was off from the computer
	 *  video_time file was setup with a bad timestamp to be replaced with best recalculation, to align with traffic time
	 *  
	 */
	@Test
	public void readData5(){
		Date traceDateTime = new Date((long) 1479853.857);
		traceResult.setTraceDateTime(traceDateTime);
		traceResult.setPcapTime0(1.479853857E9);
		VideoTime videoTime = null;
		String videoFileName = "video.mp4";

		nativeVideoFileOnDevice = makeFile(videoFileName, null);
		VIDEO_TIME_FILE = makeFile("video_time", new String[] { "1.4798539802E9" });
		
		String durCreationStrings = " creation_time   : 2016-11-22T13:31:05.000000Z  \n Duration: 00:00:51.80, start: 0.000000, bitrate: 217 kb/s";

		String cmd = Util.getFFMPEG() + " -i " + "\"" + nativeVideoFileOnDevice.getAbsolutePath() +"\"";
		Mockito.when(extRunner.executeCmd(cmd)).thenReturn(durCreationStrings);
		
		videoTime = videoTimeReaderImpl.readData(tracePath, traceResult, videoFileName);
		assertEquals(1.479853857E9, videoTime.getVideoStartTime(), 0);
		
		nativeVideoFileOnDevice.delete();
		nativeVideoFileOnDevice = null;
		VIDEO_TIME_FILE.delete();
		VIDEO_TIME_FILE = null;
	}
}
