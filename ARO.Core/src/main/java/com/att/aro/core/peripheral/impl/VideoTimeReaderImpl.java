/*
 *  Copyright 2014, 2022 AT&T
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
package com.att.aro.core.peripheral.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.IVideoTimeReader;
import com.att.aro.core.peripheral.pojo.VideoTime;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;

import lombok.Getter;
import lombok.Setter;

/**
 * Method to read times from the video time trace file and store video time
 * variables.
 * Date: October 7, 2014
 */
public class VideoTimeReaderImpl extends PeripheralBase implements IVideoTimeReader {

	private static final Logger LOGGER = LogManager.getLogger(VideoTimeReaderImpl.class.getName());
	
	private static final String VALIDATED = "//Validated_2.2-";

	@Autowired
	private IExternalProcessRunner extrunner;
	
	@Getter @Setter
	private String[] videoTimeContent = null;
	private String videoOfDeviceScreen = TraceDataConst.FileName.VIDEO_MP4_FILE;
	
	@Override
	public VideoTime readData(String directory, AbstractTraceResult result, String... deviceVideoFile) {
		videoTimeContent = null;
		if (deviceVideoFile != null && deviceVideoFile.length > 0) {
			videoOfDeviceScreen = deviceVideoFile[0];
		}
		boolean exVideoFound = false;
		boolean exVideoTimeFileNotFound = false;
		double videoStartTime = 0.0;
		boolean nativeVideo = false;
		String exVideoDisplayFileName = "exvideo.mov";
		String filePath = directory + Util.FILE_SEPARATOR + exVideoDisplayFileName;
		String movOfDeviceScreen = TraceDataConst.FileName.VIDEO_MOV_FILE;

		if (filereader.fileExist(filePath) || isExternalVideoSourceFilePresent(videoOfDeviceScreen, movOfDeviceScreen, false, directory)) {
			exVideoFound = true;
			exVideoTimeFileNotFound = false;
			filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.EXVIDEO_TIME_FILE;

			if (!filereader.fileExist(filePath)) {
				exVideoTimeFileNotFound = true;
				exVideoFound = false;
			} else {
				videoStartTime += readVideoStartTime(filePath, result.getTraceDateTime());
			}
		} else {
			exVideoFound = false;
			exVideoTimeFileNotFound = false;
			nativeVideo = true;
			filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.VIDEO_TIME_FILE;
			videoStartTime = updateVideoStartTime(directory, filePath, result, result.getTraceDateTime());
		}
		VideoTime vtime = new VideoTime();
		vtime.setExVideoFound(exVideoFound);
		vtime.setExVideoTimeFileNotFound(exVideoTimeFileNotFound);
		vtime.setDeviceScreenVideo(nativeVideo);
		vtime.setVideoStartTime(videoStartTime);
		return vtime;
	}
	
	public double updateVideoStartTime(String directory, String filepath, AbstractTraceResult result, Date traceDateTime) {
		double videoStartTime = readVideoStartTime(filepath, traceDateTime);
		String videoPath = directory + Util.FILE_SEPARATOR + videoOfDeviceScreen;
		if (filereader.fileExist(videoPath)) {
			String[] lines = getVideoTimeContent();
			if ((lines == null || lines.length == 1 || (lines.length > 1 && (!lines[1].startsWith(VALIDATED))))) {
				double startTime;
				// updates if video_time:
				//   1) does not exist
				//   2) does not contain a second line that is matched with VideoTimeReaderImpl.VALIDATED
				if (videoPath.endsWith("mov")) {
					if (lines != null) {
						String str = lines[lines.length - 1].replaceAll("^.+-", "");
						startTime = "blank".equals(str) ? getStartTime(videoPath) : Double.valueOf(str.replaceAll(" .*", ""));
					} else {
						startTime = getStartTime(videoPath);
					}
				} else {
					startTime = getFfmpegVideoStartTime(videoPath);
				}
				videoStartTime = startTime != -1 ? startTime : videoStartTime;

				double adjustedTime = videoStartTime;
				if (videoStartTime - result.getPcapTime0() > 60 || videoStartTime - result.getPcapTime0() < -60) {
					long zoneOffset = Math.round((videoStartTime - result.getPcapTime0()) / 3600);
					adjustedTime = videoStartTime - (zoneOffset * 3600);
				}
				if (adjustedTime + result.getTraceDuration() < result.getPcapTime0() || adjustedTime > result.getTraceDuration() + result.getPcapTime0()) {
					// video is out of trace timeline bounds
					LOGGER.error("video is out of trace timeline bounds. Setting video start to match trace time");
					videoStartTime = result.getPcapTime0();
				} else {
					// video is within trace timeline bounds
					videoStartTime = adjustedTime;
				}
				updateVideoTimeFile(videoStartTime, filepath);
			}
		}
		return videoStartTime;
	}
	
	/**<pre>
	 * VO's video.mov file has a modification time stemming from the beginning of the file being written, at the  start of the video.mov frame capture.
	 * This modification time is not changed with each slide being added.
	 * 
	 * @param videoPath
	 * @return double
	 */
	private double getStartTime(String videoPath) {
		double startTime = 0;
		try {
			// obtain File modification time, which works as creationtime for video.mov on Windows, Mac & Linux
			startTime = (double) Files.readAttributes(Paths.get(videoPath), BasicFileAttributes.class).lastModifiedTime().toMillis();
		} catch (IOException e) {
			startTime = getFfmpegVideoStartTime(videoPath);
		}
		return startTime;
	}

	public double getFfmpegVideoStartTime(String videoPath) {
		double ffmpegStartTime = -1;
		String cmd = Util.getFFMPEG() + " -i " + "\"" + videoPath + "\"";
		String result = extrunner.executeCmd(cmd);
		String durationStr = StringParse.findLabeledDataFromString("Duration: ", ",", result);
		double duration = 0;
		try {
			duration = durationStr != null ? Util.parseTimeOfDay(durationStr, false) : 0;
		} catch (Exception e) {
			LOGGER.error("Failed to parse duration: " + durationStr + " in " + videoPath + ", defaulted to zero", e);
			duration = 0;
		}

		if (result.contains("creation_time")) {
			String creationTime = StringParse.findLabeledDataFromString("creation_time   :", "\n", result).trim();
			double creationtimeStamp = ((double) Util.parseForUTC(creationTime)) / 1000.0;
			ffmpegStartTime = creationtimeStamp - duration;
		}

		return ffmpegStartTime;
	}

	public void updateVideoTimeFile(double newVideoStartTime, String filePath) {
		try {
			if (videoTimeContent == null) {
				// video_time file is missing, creating blank content to avoid null exception
				videoTimeContent = new String[] { "blank" };
			}
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filePath));
			bufferedWriter.write(String.format("%.3f", newVideoStartTime));
			bufferedWriter.newLine();
			bufferedWriter.append(VALIDATED);
			for (String line : videoTimeContent) {
				bufferedWriter.append(line);
			}
			bufferedWriter.close();
		} catch (IOException e) {
			LOGGER.error("Failed to write to video time file", e);
		}
	}
	
	double readVideoStartTime(String filepath, Date traceDateTime) {
		double videoStartTime = 0;
		String[] lines = null;
		try {
			if (filereader.fileExist(filepath)) {
				lines = filereader.readAllLine(filepath);
				setVideoTimeContent(lines);
			}
		} catch (IOException e1) {
			LOGGER.error("failed reading video time file", e1);
		}
		if (lines != null && lines.length > 0) {
			String line = lines[0];
			String[] strValues = line.split(" ");
			if (strValues.length > 0) {
				try {
					videoStartTime = Double.parseDouble(strValues[0]);
				} catch (NumberFormatException e) {
					LOGGER.error("Cannot determine actual video start time", e);
				}
				if (strValues.length > 1) {
					// For emulator only, tcpdumpLocalStartTime is start time started according to local pc/laptop. 
					// -- getTraceDateTime is time according to emulated device 
					// -- the tcpdumpDeviceVsLocalTimeDetal is difference between the two 
					// and is added as an offset to videoStartTime 
					// so that traceEmulatorTime and videoStartTime are in sync. 
					double tcpdumpLocalStartTime = Double.parseDouble(strValues[1]);
					double tcpdumpDeviceVsLocalTimeDelta = (traceDateTime.getTime() / 1000.0) - tcpdumpLocalStartTime;
					videoStartTime += tcpdumpDeviceVsLocalTimeDelta;
				}
			}
		}
		return videoStartTime;
	}
	/**
	 * Checks for external video source.
	 * 
	 * @param nativeVideoSourcefile,isPcap
	 * 			the native video source file i.e video.mp4 
	 * 			pcap file loaded or not.
	 * @return boolean
	 * 			return false if only native video file is present , otherwise true.
	 */			
	boolean isExternalVideoSourceFilePresent(String videoOfDeviceScreen, String nativeVideoDisplayfile, boolean isPcap, String traceDirectory) {

		int index = 0;
		String[] matches;
		if (isPcap) {
			matches = filereader.list(traceDirectory, new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return isVideoFile(name);
				}
			});
		} else {
			matches = filereader.list(traceDirectory, new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return isVideoFile(name);
				}
			});
		}

		if (matches != null) {
			List<String> matchesList = new ArrayList<>();
			for (int str = 0; str < matches.length; str++) {
				if (!matches[str].startsWith("._")) {
					matchesList.add(matches[str]);
				}
			}
			matches = matchesList.toArray(new String[matchesList.size()]);
			while (index < matches.length) {
				if (matches.length == 1) {
					// If trace directory contains any one file video.mp or video.mov , we allow normal native video flow.
					return (!(videoOfDeviceScreen.equals(matches[index]) || nativeVideoDisplayfile.equals(matches[index])));
				} else {
					// If the trace directory contains video.mp4 and video.mov , we allow normal native video flow.
					if ((matches.length == 2) 
							&& ((index + 1) != 2) 
							&& (videoOfDeviceScreen.equals(matches[index]) || nativeVideoDisplayfile.equals(matches[index]))
							&& (videoOfDeviceScreen.equals(matches[index + 1]) || nativeVideoDisplayfile.equals(matches[index + 1]))
							) {
						return false;
					} else {
						// if trace directory contains video.mp4 or video.
						// mov along with external video file, we give preference to external video file.
						if (videoOfDeviceScreen.equals(matches[index]) || nativeVideoDisplayfile.equals(matches[index])) {
							return true;
						}
					}
				}
				index += 1;
			}
		}
		return false;
	}
	boolean isVideoFile(String name){
		return (name.toLowerCase().endsWith(".mp4") || name.toLowerCase().endsWith(".wmv")
				||name.toLowerCase().endsWith(".qt") || name.toLowerCase().endsWith(".wma")
				|| name.toLowerCase().endsWith(".mpeg") || name.toLowerCase().endsWith(".3gp")
				|| name.toLowerCase().endsWith(".asf") || name.toLowerCase().endsWith(".avi")
				|| name.toLowerCase().endsWith(".dv") || name.toLowerCase().endsWith(".mkv")
				|| name.toLowerCase().endsWith(".mpg") || name.toLowerCase().endsWith(".rmvb")
				|| name.toLowerCase().endsWith(".vob") || name.toLowerCase().endsWith(".mov"));
	}
}//end
