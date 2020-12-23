
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

package com.att.aro.core.videotab.pojo;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BufferOccupancyResult;
import com.att.aro.core.bestpractice.pojo.VideoChunkPacingResult;
import com.att.aro.core.bestpractice.pojo.VideoChunkSizeResult;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSessionResult;
import com.att.aro.core.bestpractice.pojo.VideoNetworkComparisonResult;
import com.att.aro.core.bestpractice.pojo.VideoRedundancyResult;
import com.att.aro.core.bestpractice.pojo.VideoStallResult;
import com.att.aro.core.bestpractice.pojo.VideoStartUpDelayResult;
import com.att.aro.core.bestpractice.pojo.VideoTcpConnectionResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoBufferData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

// this is populated in the wrong place

public class VideoResultSummary {
	private static final double MEGABYTE = 1048576;
	private int stalls;
	private double startUpDelay;
	private double bufferOccupancy; // buffer set or buffer reached
	private double ntkComparison;
	private int tcpConnection;
	private double segmentSize;
	private double segmentPacing;
	private double redundancy;
	private int duplicate;

	private int concurrentSessions;
	private int ipSessions;
	private int ipAddress;
	private int segmentCount;
	private double movieMBytes;
	private double totalMBytes;

	private double avgBufferTime; // Sum of all points in the buffer time graph starting with
	private double minBufferTime; // Lowest point on the buffer seconds graph
	private double maxBufferTime; // Highest point on the buffer seconds graph

	private double avgBufferByte;
	private double minBufferByte;
	private double maxBufferByte;
	List<VideoBufferData> videoBufferDataList;
	private boolean startupDelayStatus;

	public VideoResultSummary(AROTraceData trace) {
		populateSummary(trace);
	}

	private void populateSummary(AROTraceData trace) {
		for (AbstractBestPracticeResult bpResult : trace.getBestPracticeResults()) {
			if (bpResult.getClass().getName().contains("AROServiceImpl")) {
				continue;
			}
			BestPracticeType bpType = bpResult.getBestPracticeType();

			switch (bpType) {
			case VIDEO_STALL:
				VideoStallResult result = (VideoStallResult) bpResult;
				stalls = result.getStallResult();
				break;
			case NETWORK_COMPARISON:
				VideoNetworkComparisonResult ntkResult = (VideoNetworkComparisonResult) bpResult;
				ntkComparison = ntkResult.getAvgKbps();
				break;
			case TCP_CONNECTION:
				VideoTcpConnectionResult tcpResult = (VideoTcpConnectionResult) bpResult;
				tcpConnection = tcpResult.getTcpConnections();
				break;
			case BUFFER_OCCUPANCY:
				BufferOccupancyResult bufferResult = (BufferOccupancyResult) bpResult;
				bufferOccupancy = bufferResult.getMaxBuffer();
				populateBufferResult(bufferResult);
				break;
			case CHUNK_SIZE:
				VideoChunkSizeResult segmentSizeResult = (VideoChunkSizeResult) bpResult;
				segmentSize = segmentSizeResult.getSegmentSize();
				segmentCount = segmentSizeResult.getSegmentCount();
				break;
			case CHUNK_PACING:
				VideoChunkPacingResult segmentPacingResult = (VideoChunkPacingResult) bpResult;
				segmentPacing = segmentPacingResult.getChunkPacing();
				break;
			case VIDEO_REDUNDANCY:
				VideoRedundancyResult redundancyResult = (VideoRedundancyResult) bpResult;
				duplicate = redundancyResult.getCountDuplicate();
				redundancy = redundancyResult.getRedundantPercentage();
				break;
			case STARTUP_DELAY:
				VideoStartUpDelayResult startupDelayResult = (VideoStartUpDelayResult) bpResult;
				startUpDelay = startupDelayResult.getStartUpDelay();
				break;
			case VIDEO_CONCURRENT_SESSION:
				VideoConcurrentSessionResult concurrentSessionResult = (VideoConcurrentSessionResult) bpResult;
				concurrentSessions = concurrentSessionResult.getMaxConcurrentSessionCount();
				break;
			default:
				break;
			}
		}
		List<Session> allSessions = trace.getAnalyzerResult().getSessionlist();
		Map<InetAddress, List<Session>> ipSessionsMap = new HashMap<InetAddress, List<Session>>();
		for (Session session : allSessions) {
			InetAddress ipAddress = session.getRemoteIP();

			if (ipSessionsMap.containsKey(ipAddress)) {
				ipSessionsMap.get(ipAddress).add(session);
			} else {
				List<Session> sess = new ArrayList<Session>();
				sess.add(session);
				ipSessionsMap.put(ipAddress, sess);
			}
		}
		ipAddress = ipSessionsMap.keySet().size();
		ipSessions = allSessions.size();

		StreamingVideoData streamingVideoData;
		if ((streamingVideoData = trace.getAnalyzerResult().getStreamingVideoData()) == null) {
			return;
		}
		Collection<VideoStream> selectedVideoStreams = streamingVideoData.getVideoStreams();
		movieMBytes = calculateMBytes(selectedVideoStreams, false);
		totalMBytes = calculateMBytes(selectedVideoStreams, true);

		if (trace.getAnalyzerResult().getStreamingVideoData().getStreamingVideoCompiled().getChunkPlayTimeList().isEmpty()) {
			startupDelayStatus = false;
		} else {
			startupDelayStatus = true;
		}
	}

	private double calculateMBytes(Collection<VideoStream> manifests, boolean allMovie) {

		double sumtotalBytes = 0;
		if (manifests != null && !manifests.isEmpty()) {
			if (allMovie) {
				for ( VideoStream manifest : manifests) {
					if (manifest != null) {
						Collection<VideoEvent> videoEvents = manifest.getVideoEventMap() != null ? manifest.getVideoEventMap().values() : new ArrayList<>();
						for (VideoEvent videoEvent : videoEvents) {
							sumtotalBytes += videoEvent.getTotalBytes();
						}
					}
				}
			} else {
				for ( VideoStream manifest : manifests) {
					if (manifest != null && manifest.isSelected() && (manifest.getVideoEventMap() != null)) {
						for (VideoEvent videoEvent : manifest.getVideoEventMap().values()) {
							sumtotalBytes += videoEvent.getTotalBytes();
						}
					}
				}
			}
		}
		return sumtotalBytes / MEGABYTE;
	}

	private void populateBufferResult(BufferOccupancyResult bufferResult) {
		videoBufferDataList = new ArrayList<>();
		avgBufferByte = bufferResult.getAvgBufferByte();
		minBufferByte = bufferResult.getMinBufferByte();
		maxBufferByte = bufferResult.getMaxBuffer();
		VideoBufferData byteData = new VideoBufferData("Byte", avgBufferByte, minBufferByte, maxBufferByte);
		videoBufferDataList.add(byteData);

		minBufferTime = bufferResult.getMinBufferTime();
		maxBufferTime = bufferResult.getMaxBufferTime();
		avgBufferTime = bufferResult.getAvgBufferTime();
		VideoBufferData timeData = new VideoBufferData("Time", avgBufferTime, minBufferTime, maxBufferTime);
		videoBufferDataList.add(timeData);
	}

	public int getStalls() {
		return stalls;
	}

	public double getStartUpDelay() {
		return startUpDelay;
	}

	public double getBufferOccupancy() {
		return bufferOccupancy;
	}

	public double getNtkComparison() {
		return ntkComparison;
	}

	public int getTcpConnection() {
		return tcpConnection;
	}

	public double getSegmentSize() {
		return segmentSize;
	}

	public double getSegmentPacing() {
		return segmentPacing;
	}

	public double getRedundancy() {
		return redundancy;
	}

	public int getDuplicate() {
		return duplicate;
	}

	public int getConcurrentSessions() {
		return concurrentSessions;
	}

	public int getIpSessions() {
		return ipSessions;
	}

	public int getIpAddress() {
		return ipAddress;
	}

	public int getSegmentCount() {
		return segmentCount;
	}

	public double getMovieMBytes() {
		return movieMBytes;
	}

	public double getTotalMBytes() {
		return totalMBytes;
	}

	public double getAvgBufferTime() {
		return avgBufferTime;
	}

	public double getMinBufferTime() {
		return minBufferTime;
	}

	public double getMaxBufferTime() {
		return maxBufferTime;
	}

	public double getAvgBufferByte() {
		return avgBufferByte;
	}

	public double getMinBufferByte() {
		return minBufferByte;
	}

	public double getMaxBufferByte() {
		return maxBufferByte;
	}

	public List<VideoBufferData> getVideoBufferDataList() {
		return videoBufferDataList;
	}

	public boolean isStartupDelayStatus() {
		return startupDelayStatus;
	}
}
