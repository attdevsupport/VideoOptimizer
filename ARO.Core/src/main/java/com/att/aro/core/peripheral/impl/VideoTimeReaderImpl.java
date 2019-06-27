/*
 *  Copyright 2014 AT&T
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;

import com.att.aro.core.commandline.IExternalProcessRunner;
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
	private static final String VALIDATED = "//Validated-";

	@Autowired
	private IExternalProcessRunner extrunner;
	
	@Getter @Setter
	private String[] videoTimeContent = null;
	private String nativeVideoFileOnDevice = "video.mp4";
	
	@Override
	public VideoTime readData(String directory, Date traceDateTime) {
		boolean exVideoFound = false;
		boolean exVideoTimeFileNotFound = false;
		double videoStartTime = 0.0;
		boolean nativeVideo = false;
		String exVideoDisplayFileName = "exvideo.mov";
		String filePath = directory + Util.FILE_SEPARATOR + exVideoDisplayFileName;
		String nativeVideoDisplayfile = "video.mov";
		
		if (filereader.fileExist(filePath) || isExternalVideoSourceFilePresent(nativeVideoFileOnDevice, nativeVideoDisplayfile, false, directory)) {
			exVideoFound = true;
			exVideoTimeFileNotFound = false;
			filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.EXVIDEO_TIME_FILE;

			if (!filereader.fileExist(filePath)) {
				exVideoTimeFileNotFound = true;
				exVideoFound = false;
			} else {
				videoStartTime += readVideoStartTime(filePath, traceDateTime);
			}
		} else {
			exVideoFound = false;
			exVideoTimeFileNotFound = false;
			nativeVideo = true;
			filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.VIDEO_TIME_FILE;
			videoStartTime = updateVideoStartTimeForMP4(directory, filePath, traceDateTime);
		}
		VideoTime vtime = new VideoTime();
		vtime.setExVideoFound(exVideoFound);
		vtime.setExVideoTimeFileNotFound(exVideoTimeFileNotFound);
		vtime.setNativeVideo(nativeVideo);
		vtime.setVideoStartTime(videoStartTime);
		return vtime;
	}
	
	public double updateVideoStartTimeForMP4(String directory, String filepath, Date traceDateTime) {
		double videoStartTime = readVideoStartTime(filepath, traceDateTime);
		String videoPath = directory + Util.FILE_SEPARATOR + nativeVideoFileOnDevice;
		if (filereader.fileExist(videoPath)) {
			String[] lines = getVideoTimeContent();
			if (lines != null && lines.length > 1) {
				String line = lines[1];
				if (!line.startsWith(VALIDATED)) {
					double startTime = getFfmpegVideoStartTime(videoPath);
					videoStartTime = startTime != -1 ? startTime : videoStartTime;
					updateVideoTimeFile(videoStartTime, filepath);
				}
			} else {
				double startTime = getFfmpegVideoStartTime(videoPath);
				videoStartTime = startTime != -1 ? startTime : videoStartTime;
				updateVideoTimeFile(videoStartTime, filepath);
			}
		}
		return videoStartTime;
	}
	
	public double getFfmpegVideoStartTime(String videoPath) {
		double ffmpegStartTime = -1;
		String cmd = Util.getFFMPEG() + " -i " + "\"" + videoPath + "\"";
		String result = extrunner.executeCmd(cmd);
		String[] timestamp = StringParse.findLabeledDataFromString("Duration: ", ",", result).split(":");
		double duration = (StringUtils.isEmpty(timestamp[0])? 0 : Integer.parseInt(timestamp[0])) * 3600 + (timestamp.length <= 1 || StringUtils.isEmpty(timestamp[1])? 0 : Integer.parseInt(timestamp[1]) * 60)
				+ (timestamp.length <= 2 || StringUtils.isEmpty(timestamp[2])? 0.0 : Double.parseDouble(timestamp[2]));
		if (result.contains("creation_time")) {
			String creationTime = StringParse.findLabeledDataFromString("creation_time   :", "\n", result).trim();
			long creationtimeStamp = Util.parseForUTC(creationTime);
			creationtimeStamp = creationtimeStamp / 1000;
			ffmpegStartTime = creationtimeStamp - duration;
		}

		return ffmpegStartTime;
	}

	public void updateVideoTimeFile(double newVideoStartTime, String filePath) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filePath));
			bufferedWriter.write(String.valueOf(newVideoStartTime));
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
	
	double readVideoStartTime(String filepath, Date traceDateTime){
		double videoStartTime = 0;
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filepath);
			setVideoTimeContent(lines);
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
					// For emulator only, tcpdumpLocalStartTime is
					// start
					// time started according to local pc/laptop.
					// getTraceDateTime is time according to
					// emulated device
					// -- the tcpdumpDeviceVsLocalTimeDetal is
					// difference
					// between the two and is added as an offset
					// to videoStartTime so that traceEmulatorTime
					// and
					// videoStartTime are in sync.
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
	boolean isExternalVideoSourceFilePresent(String nativeVideoFileOnDevice, String nativeVideoDisplayfile,boolean isPcap, String traceDirectory){
		
		int index =0;
		String[] matches;
		if(isPcap){
			matches = filereader.list(traceDirectory, new FilenameFilter()
			{
				public boolean accept(File dir, String name) {			
					return isVideoFile(name);
				  }
			});
		}else{
			matches = filereader.list(traceDirectory, new FilenameFilter()
			{
				public boolean accept(File dir, String name) {
					
					return isVideoFile(name);
				  }
			});
		}
		
		if(matches!= null){
			List<String> matchesList = new ArrayList<>();
			for (int str = 0; str < matches.length; str++) {
				if (!matches[str].startsWith("._")) {
					matchesList.add(matches[str]);
				}
			}
			matches = matchesList.toArray(new String[matchesList.size()]);
			while(index < matches.length){
				if(matches.length == 1){
					// If trace directory contains any one file video.mp or video.mov , we allow normal native video flow.
/*					if(nativeVideoFileOnDevice.equals(matches[index]) || nativeVideoDisplayfile.equals(matches[index])){
						return false;
					}else{
						return true;
					}*/
					return (!(nativeVideoFileOnDevice.equals(matches[index]) || nativeVideoDisplayfile.equals(matches[index])));
				}else {
					// If the trace directory contains video.mp4 and video.mov , we allow normal native video flow.
					if((matches.length == 2) && ((index + 1)!=2)
						&& (nativeVideoFileOnDevice.equals(matches[index]) || nativeVideoDisplayfile.equals(matches[index]))
						&& (nativeVideoFileOnDevice.equals(matches[index+1]) || nativeVideoDisplayfile.equals(matches[index+1]))	){
						return false;
					} else{
						// if trace directory contains video.mp4 or video.
						//mov along with external video file, we give preference to external video file.
						if(nativeVideoFileOnDevice.equals(matches[index]) || nativeVideoDisplayfile.equals(matches[index])){
							return true;
						}
					}
				}
				
				index+=1;	
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
