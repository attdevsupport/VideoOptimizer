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
package com.att.aro.core.video.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.android.AndroidApiLevel;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.datacollector.IVideoImageSubscriber;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.IScreenRecorder;
import com.att.aro.core.video.pojo.VideoOption;

public class ScreenRecorderImpl implements IScreenRecorder {

	private static final Logger LOGGER = LogManager.getLogger(ScreenRecorderImpl.class.getName());	

	@Autowired
	protected IFileManager filereader;
	
	public void setFileReader(IFileManager reader){
		this.filereader = reader;
	}
	
	@Autowired
	private IFileManager filemanager;
	
	@Autowired
	private IAdbService adbservice;
	private IAndroid android;
	@Autowired
	public void setAndroid(IAndroid android) {
		this.android = android;
	}
	
	private IExternalProcessRunner extrunner;

	@Autowired
	public void setExternalProcessRunner(IExternalProcessRunner runner) {
		this.extrunner = runner;
	}

	String localVidsFolder = null;
	public void setLocalTraceFolder(String localVidsFolder) {
		this.localVidsFolder = localVidsFolder;
	}

	String tempFolder = null;
	String payloadFileName = null;
	String remoteVideoPath = null;

	private boolean traceActive;

	private IAroDevice aroDevice;

	private String remoteExecutable;

	private IDevice device;

	/**
	 * in seconds
	 */
	private int segmentLength;

	private State currentState = State.Undefined;

	private String localTraceFolder;

	private int bitRate;

	private String screenSize;

	private String[] times;

	@Override
	public State init(IAroDevice aroDevice, String localTraceFolder, VideoOption videoOption) {

		this.aroDevice = aroDevice;
		device = (IDevice) aroDevice.getDevice();
		this.localTraceFolder = localTraceFolder + Util.FILE_SEPARATOR;
		this.localVidsFolder = this.localTraceFolder + "screenVideos";
		this.payloadFileName = "videobatch.sh";
		this.remoteVideoPath = "/sdcard/ARO/screenVideos/";
		this.remoteExecutable = remoteVideoPath + payloadFileName;

		segmentLength = 10; // in seconds
		bitRate = videoOption.getBitRate();
		screenSize = videoOption.getScreenSize();
		
		// FIXME: try to find a better way for updating resolution for kitcat
		updateResolutionForKitCat(videoOption);

		String[] res = android.getShellReturn(device, "rm " + remoteVideoPath + "*");
		for (String line : res) {
			if (line.length() > 0) {
				LOGGER.debug(">>" + line + "<<");
			}
		}

		filemanager.mkDir(localVidsFolder); // make sure folder exists
		traceActive = adbservice.installPayloadFile(aroDevice, localVidsFolder, payloadFileName, remoteExecutable);
		if (traceActive) {
			setState(State.Initialized);
		}
		return getStatus();
	}

	private void setState(State initialized) {
		currentState = initialized;
	}

	@Override
	public State getStatus() {
		return currentState;
	}

	@Override
	public void run() {
		launchVidCapture();
	}

	@Override
	public boolean stopRecording() {

		if (!currentState.equals(State.Recording) || !stopVidCapture(device)) {
			setState(State.Error);
			return false;
		}

		while (true) {
			String[] procs = findPid("screenrecord");
			if (procs == null) {
				break;
			}
		}

		try {
			pullVideos();
			compileVideo();
			setVideoTime();
			deleteSrcVideos();
		} catch (Exception e) {
			setState(State.Error);
			return false;
		}
		setState(State.Done);
		return true;
	}

	private void setVideoTime() {

		if (times.length > 0) {
			InputStream stream = new ByteArrayInputStream(times[0].getBytes());
			try {
				filemanager.saveFile(stream, localTraceFolder + "/video_time");
			} catch (IOException e) {
				LOGGER.error("Failed to set video_time "+e.getMessage());
			}
		}
	}

	@Override
	public void compileVideo() throws Exception {
		// find all the mp4 videos
		String[] vidSegments = filemanager.findFilesByExtention(localVidsFolder, ".mp4");

		times = filereader.readAllLine(localVidsFolder + "/video-time");
		if (times.length != vidSegments.length) {
			throw new Exception("Video/Time mismatch,  Time:" + times.length + ", Video:" + vidSegments.length);
		}

		VideoSegment vFiles = new VideoSegment();

		for (int i = 0; i < times.length; i++) {
			vFiles.add(vidSegments[i], Double.parseDouble(times[i]));
		}

		// execute the compile
		ArrayList<String> intCmds = vFiles.getCMDsIntermeditate();
		int vidSegment = 0;
		for (vidSegment = 0; vidSegment < intCmds.size(); vidSegment++) {
			try {
				LOGGER.info(intCmds.get(vidSegment));
				String lines = extrunner.executeCmd(intCmds.get(vidSegment));
				if (!lines.isEmpty()) {
					if (lines.contains("moov atom not found")) {
						// TODO fix moov atom
						// for now skip
						break;
					}
					LOGGER.info("ffmpeg error: " + lines);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String concatCmd = vFiles.buildConcat(vidSegment);
		LOGGER.info(concatCmd);
		LOGGER.info(concatCmd);
		extrunner.executeCmd(concatCmd);

		String finalVidCmd = vFiles.buildFinalVideo();
		LOGGER.info(finalVidCmd);
		LOGGER.info(concatCmd);
		String finalVidResults = extrunner.executeCmd(finalVidCmd);
		LOGGER.info("Movie done:" + finalVidResults);

		renameVideos("exvideo.mov", "video.mov", "video_mov_o");

		LOGGER.info("compileVideo() completed");
	}

	private void renameVideos(String srcFile, String target, String original_ext_o) {

		if (filemanager.fileExist(localTraceFolder + srcFile)) {

			File src = new File(localTraceFolder + srcFile);
			File orig = new File(localTraceFolder + target);

			if (filemanager.renameFile(orig, original_ext_o)) {
				filemanager.renameFile(src, target);
			}
		}

	}

	class VideoSegment {
		Segment head;
		Segment tail;
		
		class Segment {
			String intFile;
			String srcFile;
			Double timestamp;
			Segment next = null;
			
			String intermediateCmd;
		
			public Segment(String srcFile, Double timestamp) {
				this.srcFile = srcFile;
				this.timestamp = timestamp;
				setIntFile(srcFile);
			}
			
			private void setIntFile(String intFile) {
				int lidx = intFile.lastIndexOf('.');
				if (lidx >= 0) {
					this.intFile = intFile.substring(0, lidx)+".mpg";
				}
			}

			public Segment add(String path, Double timestamp){
				next = new Segment(path, timestamp);
				return next;
			}
			
			@Override
			public String toString() {
				return "ts:" + timestamp + " :" + srcFile;
			}

		}

		public void add(String path, Double timestamp) {
			if (head == null){
				head = new Segment(path, timestamp);
				tail = head;
			}else{
				Segment temp = tail.add(path, timestamp);
				tail = temp;
			}
		}
		
		public String buildConcat(int limit) {
			StringBuffer sb = new StringBuffer(Util.getFFMPEG() + " -i " + " concat:\"");
			Segment t1 = head;
			String local = localVidsFolder + Util.FILE_SEPARATOR;
			while (t1.next != null && --limit > 0) {
				sb.append(local);
				sb.append(t1.intFile);
				sb.append("|");
				t1 = t1.next;
			}
			sb.append(local);
			sb.append(t1.intFile);
			sb.append("\" -c copy ");
			sb.append(local);
			sb.append("intermediate_all.mpg -y");
			return sb.toString();
		}
		
		public String buildFinalVideo() {
			StringBuffer sb = new StringBuffer();
			String local = localVidsFolder + Util.FILE_SEPARATOR;
			
			sb.append(Util.getFFMPEG());
			sb.append(" -i ");
			sb.append(local);
			sb.append("intermediate_all.mpg");
			// sb.append(" -filter:v \"setpts=0.75*PTS\"");
			sb.append(" -vf scale=540:960:");
			sb.append(" -vcodec mjpeg ");
			sb.append(localTraceFolder);
			sb.append("exvideo.mov -y ");
			return sb.toString();
		}
		
		public ArrayList<String> getCMDsIntermeditate() {
			Segment t1 = head;
			String local = localVidsFolder + Util.FILE_SEPARATOR;
			ArrayList<String> cmds = new ArrayList<>();
			String cmd;
			while (true) {
				cmd = createIntCmd(t1, local);
				cmds.add(cmd);
				if (t1.next == null){
					break;
				}
				t1 = t1.next;
			}
			return cmds;
		}

		private String createIntCmd(Segment t1, String local) {
			StringBuilder sb;
			Double offset = null;
			if (t1.next != null) {
				offset = t1.next.timestamp - t1.timestamp - 0.02D;
			}

			sb = new StringBuilder();
			
			sb.append(Util.getFFMPEG());
			sb.append(" -i ");
			sb.append(local);
			sb.append(t1.srcFile);
			if (offset != null) {
				sb.append(" -to ");
				sb.append(offset);
			}
			sb.append(" -qscale:v 1  -r 20  -y ");
			sb.append(local);
			sb.append(t1.intFile);
			return sb.toString();
		}

	}

	private String[] findPid(String procName) {
		String arocmd = "ps| grep " + procName;
		String[] response = android.getShellReturn(device, arocmd);
		String[] seg = null;
		for (String line : response) {
			if (line.contains(procName)) {
				LOGGER.info(line);
				seg = line.split("\\s+");
				return seg;
			}
		}
		return seg;
	}

	/**
	 * Launch shell script on Android to capture video
	 */
	private void launchVidCapture() {

		setState(State.Recording);
		String path = adbservice.getAdbPath();
		String cmd = path
					+ " -s "
					+ aroDevice.getId()
					+ " shell"
					+ " sh "
					+ remoteExecutable 
					+ " " + Integer.toString(segmentLength) 
					+ " " + Integer.toString(bitRate)
					+ " " + screenSize
					+ " capture"
					;
		String line = extrunner.executeCmd(cmd);
		
		LOGGER.info("start screenrecord response:" + line);
		LOGGER.info("start screenrecord response:" + line);

	}
	
	/**
	 * Triggers the script to shutdown video capture processes
	 * 
	 * @param device
	 * @return
	 */
	private boolean stopVidCapture(IDevice device) {

		setState(State.Stopping);
		String cmd = "touch " + remoteVideoPath + "cmdstop";
		String[] response = android.getShellReturn(device, cmd);
		for (String line : response) {
			LOGGER.info("stop screenrecord response:" + line);
		}
		return true;
	}

	/**
	 * Delete the entire vids folder that contains all of the video segments
	 */
	@Override
	public boolean deleteSrcVideos() {
		filemanager.directoryDeleteInnerFiles(localVidsFolder);
		return filemanager.deleteFile(localVidsFolder);
	}


	/**
	 * @throws IOException 
	 * @throws AdbCommandRejectedException 
	 * @throws TimeoutException 
	 * 
	 */
	@Override
	public boolean pullVideos() throws Exception {
		SyncService service = null;

		FileEntry files = adbservice.locate(device, null, remoteVideoPath);
		if (files == null) {
			throw new Exception("Failed to locate files :" + remoteVideoPath);
		}

		filemanager.directoryDeleteInnerFiles(localVidsFolder);
		service = device.getSyncService();
		service.pull(files.getCachedChildren(), localVidsFolder, SyncService.getNullProgressMonitor());

		if (adbservice.pullFile(service, remoteVideoPath, "video-time", localVidsFolder)) {
			setState(State.PullComplete);
		}

		return true;
	}

	@Override
	public void addSubscriber(IVideoImageSubscriber vImageSubscriber) {
		// UNUSED
	}

	@Override
	public Date getVideoStartTime() {
		// UNUSED
		return null;
	}

	@Override
	public boolean isVideoCaptureActive() {
		return currentState.equals(State.Recording);
	}

	/**
	 * FIXME: update screen resolution for kitcat device for solving kitcat screen shrink problem
	 */
	private void updateResolutionForKitCat(VideoOption videoOption) {
		try {
			boolean isHD = videoOption == VideoOption.HDEF;
			String version = this.aroDevice.getApi();
			int versionNum = Integer.parseInt(version);
			if (versionNum == AndroidApiLevel.K19.levelNumber()) {
				screenSize = isHD? 
						VideoOption.KITCAT_HDEF.getScreenSize() : VideoOption.KITCAT_SDEF.getScreenSize();
			}
		} catch (Exception e) {}
	}
}
