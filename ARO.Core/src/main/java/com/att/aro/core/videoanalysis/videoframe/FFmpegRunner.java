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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

public class FFmpegRunner implements Runnable{

	private static final Logger LOG = LogManager.getLogger(FFmpegRunner.class.getName());

	private IStringParse stringParse;
	private IFileManager filemanager;
	private IExternalProcessRunner externalProcessRunner;

	private static final Queue<VideoEvent> queue = new PriorityQueue<>();
	
	private static final HashMap<VideoEvent, String> clipMap = new HashMap<>();
	
	private StreamingVideoData streamingVideoData;

	private byte[] defaultThumbnail;
	private int timeout;
	private int processed;
	private boolean running = false;
	private boolean halt = false;

	public FFmpegRunner(StreamingVideoData streamingVideoData, byte[] defaultThumbnail
			, IFileManager filemanager, IExternalProcessRunner externalProcessRunner,IStringParse stringParse) {
		this.filemanager = filemanager;
		this.externalProcessRunner = externalProcessRunner;
		this.stringParse = stringParse;
		this.defaultThumbnail = defaultThumbnail;
		this.streamingVideoData = streamingVideoData;
		halt = false;
		running = false;
		processed = 0;
	}
	
	public void addJob(VideoEvent videoEvent, String tempClippingFullPath) {
		if (!running) {
			run();
		}
		LOG.debug("addJob: " + videoEvent);
		queue.add(videoEvent);
		clipMap.put(videoEvent, tempClippingFullPath);
	}
	
	@Override
	public void run() {
		halt = false;
		if (!halt) {
			running = true;
			timeout = 100;

			Runnable processQueue = () -> {
				streamingVideoData.setFinished(false);

				while (running && !halt && timeout > 0) {
					if (!queue.isEmpty()) {
						timeout = 100; // reset to 10 seconds with every item found in queue
						
						try {
							extractSegmentData(queue.remove());
						} catch (Exception e) {
							LOG.debug("extractSegmentData() failure", e);
						}
						processed++;
						LOG.debug(String.format("FFmpegRunner processed:%d, remaining:%d", processed, queue.size()));
					} else {
						try {
							timeout -= 10; // counting down, still allows for additions to queue
							LOG.debug(String.format("FFmpegRunner count down:%d", timeout/10));
							Thread.sleep(100);
						} catch (InterruptedException e) {
							LOG.debug("interrupted");
						}
					}
				}

				LOG.debug(String.format("FFmpegRunner HALTED. processed:%d", processed));
				streamingVideoData.setFinished(true);
			};
			new Thread(processQueue, "ffmpegRunner").start();
		}
	}

	/**
	 * 
	 * @param startTime
	 * @param frameCount
	 */
	private boolean extractSegmentData(VideoEvent videoEvent) {

		String clippingFile;
		if ((clippingFile = clipMap.get(videoEvent)) != null) {
			clipMap.remove(videoEvent);
		} else {
			clippingFile = buildSegmentFullPathName(videoEvent);
		} 

		byte[] data = null;
		String thumbnailFile = streamingVideoData.getVideoPath() + "thumbnail.png";
		filemanager.deleteFile(thumbnailFile);
		String cmd = Util.getFFMPEG() + " -y -i " + "\"" + clippingFile + "\"" + " -ss 00:00:00   -vframes 1 " + "\"" + thumbnailFile + "\"";
		String lines = externalProcessRunner.executeCmd(cmd, true, true);
		if (filemanager.fileExist(thumbnailFile)) {
			Path path = Paths.get(thumbnailFile);
			try {
				data = Files.readAllBytes(path);
				filemanager.deleteFile(thumbnailFile);
				videoEvent.setThumbnail(data);
				videoEvent.getSegmentInfo().setThumbnailExtracted(true);
			} catch (IOException e) {
				LOG.debug("getThumbnail IOException:" + e.getMessage());
			}
		}
		if (data == null) {
			videoEvent.getSegmentInfo().setThumbnailExtracted(false);
			if (videoEvent.getChildManifest().isVideo()) {
				videoEvent.setThumbnail(defaultThumbnail);
			}
		}
		filemanager.deleteFile(clippingFile);

		if (!StringUtils.isEmpty(lines)) {
			String[] height = stringParse.parse(lines, "\\d{2,4}x(\\d+)");
			if (videoEvent.getResolutionHeight() == 0 && height != null) {
				videoEvent.setResolutionHeight(StringParse.stringToInteger(height[0], 0));
				videoEvent.getChildManifest().setPixelHeight(videoEvent.getResolutionHeight());
			}

			if ((stringParse.parse(lines, "Stream #0.*Audio: ([A-Za-z0-9]*) ")) != null) {
				if (videoEvent.getSegmentInfo().isVideo() && videoEvent.getSegmentInfo().getContentType().equals(ContentType.VIDEO)) {
					videoEvent.getSegmentInfo().setContentType(ContentType.MUXED);
				}
				if (videoEvent.getChannels() == null) {
					String chnls = "";
					if (lines.contains(" stereo,")) {
						chnls = "2";
					} else if (lines.contains(" mono,")) {
						chnls = "1";
					}
					if (videoEvent.getChildManifest().getChannels() == null) {
						videoEvent.getChildManifest().setChannels(chnls);
					}
					videoEvent.setChannels(chnls);
				}
			}
		} else {
			// false for meta data
			return false;
		}

		return true;
	}
	
	private String buildSegmentFullPathName(VideoEvent videoEvent) {
		String fileName = videoEvent.getRequest().getFileName();
		int pos = fileName.lastIndexOf("/");
		if (pos > -1) {
			fileName = fileName.substring(pos + 1);
		}
		String segName = String.format("%sclip_%09.0f_%08d_%s_%s", streamingVideoData.getVideoPath(), videoEvent.getRequest().getTimeStamp(), (int)videoEvent.getSegmentID(), videoEvent.getQuality(), fileName);
		return segName;
	}
}
