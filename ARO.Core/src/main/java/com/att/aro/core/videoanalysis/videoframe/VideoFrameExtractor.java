/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.videoanalysis.videoframe;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.AROConfig;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.videoframe.FrameRequest.JobType;

import lombok.Data;

@Data
public class VideoFrameExtractor implements Runnable{

	private static final Logger LOG = LogManager.getLogger(VideoFrameExtractor.class.getName());

	private ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	private IStringParse stringParse = context.getBean(IStringParse.class);
	private IFileManager fileManager = context.getBean(FileManagerImpl.class);
	private IExternalProcessRunner externalProcessRunner = context.getBean(ExternalProcessRunnerImpl.class);

	private ArrayList<FrameRequest> queue = new ArrayList<>();
	private TreeMap<Double, FrameRequest> dropMap = new TreeMap<>();
	
	// PreLoad
	private Integer frameSkip;

	// Initialization must have data
	private String videoFrameFolder;
	private String deviceVideoPath;
	private TreeMap<Double, BufferedImage> frameMap;
	private Integer fWidth;
	private Integer fHeight;

	private boolean running = false;

	private boolean halt = false;
	
	/**
	 * Collect frames from startTime for a given count of frames.
	 * Frames are added to a TreeMap<Double, BufferedImage> frameMap
	 * 
	 * Note: If a startTime is calculated to retrieve a specific frame
	 *	this can fail if the ffmpeg fails to extract one or more frames depending on the state of the video file.
	 *  If accuracy is important, the results should be examined, and adjustments should be made to handle missing frames. 
	 *  Usually this results in pulling frames beyond the target.
	 * 
	 * @param startTime
	 * @param frameCount
	 * @param resultSubscriber
	 * @throws Exception if prerequisites are incomplete of not instantiated
	 */
	public void addJob(FrameRequest frameRequest) throws Exception { // double startTime, Integer frameCount, FrameReceiver resultSubscriber
		if (!running) {
			run();
		}
		checkConfig();
		queue.add(frameRequest);
	}
	
	public void shutdown() {
		halt = true;		// disallow restart
		running = false;	// interrupt run loop
	}

	@Override
	public void run() {
		if (!halt) {
			queue = new ArrayList<>();
			running = true;

			Runnable processQueue = () -> {
				while (running && !halt) {
					if (queue.size() > 0) {

						FrameRequest request;
						FrameRequest frameRequest = queue.remove(0);

						if ((request = dropMap.get(frameRequest.getStartTimeStamp())) != null) {
							request.setJobType(JobType.DROP);
						}

						switch (frameRequest.getJobType()) {

						case PRELOAD:
							sendResults(frameRequest, preloadFrames(frameRequest));
							break;

						case COLLECT_FRAMES:
							sendResults(frameRequest, collectFrames(frameRequest));
							break;

						default:
							LOG.debug("Skipping dropped request :" + frameRequest);
							break;
						}
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							LOG.debug("interupted");
						}
					}
				}
				destroy();
				LOG.debug("Video frame extractor HALTED");
			};
			new Thread(processQueue, "Video frame extractor").start();
		}
	}

	/**
	 * Close out and delete videoFrameFolder, if not flagged (preserveFrames=yes) to preserve.
	 */
	public void destroy() {
		queue.clear();
		dropMap.clear();
		queue = null;
		if (!Util.checkMode("preserveFrames", "yes") && fileManager.directoryExist(videoFrameFolder)) {
			fileManager.deleteFolderAndContents(videoFrameFolder);
		}
	}

	private void sendResults(FrameRequest frameRequest, FrameStatus frameStatus) {
		if (frameStatus.getAddedCount() == 0) {
			for (FrameRequest request : queue) {
				if (request.getStartTimeStamp() == frameRequest.getStartTimeStamp()) {
					dropMap.put(request.getStartTimeStamp(), request);
					request.setJobType(JobType.DROP);
					LOG.debug("Dropped :" + request);
				}
			}
			return;
		}
		if (frameRequest.getFrameReceiver() != null) {
			frameStatus.setFrameRequest(frameRequest);
			frameRequest.getFrameReceiver().receiveResults(this.getClass(), frameStatus);
		}
	}

	public String query() {
		return String.format("running %s\nqueue size :%d "
				, running?"yes":"no"
				, queue.size() );
	}
	
	/**
	 * Extracts a frame every frameSkip count. Results depend on the video. Do not depend on the frame skip to be an exact.
	 * 
	 * @param frameSkip
	 */
	public FrameStatus preloadFrames(FrameRequest frameRequest) {

		String cmd = String.format("%s -i \"%s\" -vf \"select=not(mod(n\\,%d)), scale=%d:%d\" -vsync 0 -frame_pts true %s "
				, Util.getFFMPEG()
				, deviceVideoPath
				, frameRequest.getCount() // skip frame count
				, fWidth
				, fHeight
				, "frame-%05d.png");

		LOG.debug(String.format("PRE LOAD every %d frames (more or less)", frameRequest.getCount()));
		String results = externalProcessRunner.executeCmd(new File(videoFrameFolder), cmd, true, true);
		FrameStatus frameStatus = loadFrames(videoFrameFolder);
		
		return prepareFrameStatus(results, frameStatus);
	}

	/**
	 * 
	 * @param startTime
	 * @param frameCount
	 */
	private FrameStatus collectFrames(FrameRequest frameRequest) {
		String cmd = String.format("%s -i \"%s\" -vf \"select=gte(t\\,%f), scale=%d:%d\" -vsync 0 -frame_pts true %s %s "
				, Util.getFFMPEG()
				, deviceVideoPath
				, frameRequest.getStartTimeStamp()
				, fWidth
				, fHeight
				, frameRequest.getCount() != null ? String.format("-vframes %d", frameRequest.getCount()) : ""
				, "frame-%05d.png");

		LOG.debug(String.format("LOADING %d frames from %.03f ", frameRequest.getCount().intValue(), frameRequest.getStartTimeStamp()));
		String results = externalProcessRunner.executeCmd(new File(videoFrameFolder), cmd, true, true);
		FrameStatus frameStatus = loadFrames(videoFrameFolder);
		
		return prepareFrameStatus(results, frameStatus);
	}

	private FrameStatus loadFrames(String strPath) {
		FrameStatus frameStatus = new FrameStatus();
		List<String> frameFiles = Arrays.asList(fileManager.findFilesByExtention(strPath, ".png"));
		Collections.sort(frameFiles);

		if (!CollectionUtils.isEmpty(frameFiles)) {
			Double firstFrame = StringParse.findLabeledDoubleFromString("frame-", ".png", frameFiles.get(0));
			frameStatus.setFirstFrame(firstFrame.intValue());

			Double tsKey = null;
			BufferedImage bImage;
			File framePath = null;
			int duplicates = 0, additions = 0;
			try {
				for (String frameFile:frameFiles) {
					tsKey = StringParse.findLabeledDoubleFromString("frame-", ".png", frameFile);
					framePath = fileManager.createFile(strPath, frameFile);
					if (!frameMap.containsKey(tsKey)) {
						bImage = ImageIO.read(framePath);
						frameMap.put(tsKey, bImage);
						framePath.delete();
						additions++;
					} else {
						duplicates++;
						framePath.delete();
					}
				}
				frameStatus.setSuccess(true);
			} catch (IOException e) {
				LOG.error("Exception:", e);
			}

			frameStatus.setAddedCount(additions);
			frameStatus.setDuplicates(duplicates);
		}
		
		return frameStatus;
	}

	public FrameStatus prepareFrameStatus(String results, FrameStatus frameStatus) {
		// process results for inclusion to frameStatus

		int index = results.lastIndexOf("frame=");
		Double frameCount;
		if (index > 0 && (frameCount = StringParse.findLabeledDoubleFromString("frame=", results.substring(index))) != null) {
			stringParse.parse(results.substring(index), ".*\n");
			int index2 = results.substring(index).indexOf("\n");
			results = results.substring(index, index + index2);
		} else {
			// convert null to 0
			frameCount = 0D;
		}
		
		if (frameStatus.getAddedCount() + frameStatus.getDuplicates() != frameCount.intValue()) {
			frameStatus.setSuccess(false);
			results += "\nframeCount wrong";
		}
		if (frameStatus.getDuplicates() == frameCount.intValue()) {
			frameStatus.setSuccess(false);
			results += "\nno new frames";
		}
		frameStatus.setExecutionResults(results);
		frameStatus.setFrameCount(frameCount.intValue());
		return frameStatus;
	}

	public void initialize(String videoFrameFolder, String deviceVideoPath, TreeMap<Double, BufferedImage> frameMap, Integer fWidth, Integer fHeight) throws Exception {
		this.videoFrameFolder = videoFrameFolder;
		this.deviceVideoPath = deviceVideoPath;
		this.frameMap =  frameMap;
		this.fWidth = fWidth;
		this.fHeight = fHeight;
		checkConfig();
		halt = false;
	}
	
	private void checkConfig() throws Exception {
		if (StringUtils.isEmpty(videoFrameFolder) || StringUtils.isEmpty(deviceVideoPath) || !fileManager.fileExist(deviceVideoPath) || frameMap == null) {
			throw new Exception("VideoFrameExtractor initialization parameters are invalid<" + videoFrameFolder + ", " + deviceVideoPath + ", " + frameMap + ", " + fWidth + ", " + fHeight + ">");
		}
	}
}
