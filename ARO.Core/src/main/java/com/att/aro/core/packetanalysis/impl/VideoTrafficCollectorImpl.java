/*
 *  Copyright 2017, 2021 AT&T
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
package com.att.aro.core.packetanalysis.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.IVideoTrafficCollector;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoTabHelper;
import com.att.aro.core.videoanalysis.impl.VideoSegmentAnalyzer;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.Manifest.ManifestType;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;

/**
 * Video Streaming Analysis
 */
public class VideoTrafficCollectorImpl implements IVideoTrafficCollector {
	private static final Logger LOG = LogManager.getLogger(VideoTrafficCollectorImpl.class.getName());

	@Autowired private IFileManager filemanager;
	@Autowired private IVideoTabHelper videoTabHelper;
	@Autowired private VideoStreamConstructor videoStreamConstructor;
	@Autowired private VideoSegmentAnalyzer videoSegmentAnalyzer;
	
	private String tracePath;
	private String videoPath;
	private final String fileVideoSegments = "video_segments";
	
	@Value("${ga.request.timing.videoAnalysisTimings.title}")
	private String videoAnalysisTitle;
	@Value("${ga.request.timing.analysisCategory.title}")
	private String analysisCategory;

	private StreamingVideoData streamingVideoData;

	private SortedMap<Integer, HttpRequestResponseInfo> segmentRequests = new TreeMap<>();

	private Manifest manifest;
	private double trackManifestTimeStamp;
	
	/**
	 * KEY: video segment request
	 * DATA: timestamp of last child manifest
	 */
	private Map<String, Double> manifestReqMap = new HashMap<>();
	
	@Override
	public StreamingVideoData collect(AbstractTraceResult result, List<Session> sessionlist, SortedMap<Double, HttpRequestResponseInfo> requestMap) {
		
		long analysisStartTime = System.currentTimeMillis();
		
		tracePath = result.getTraceDirectory() + Util.FILE_SEPARATOR;
		LOG.debug("\n**** VideoAnalysis for :" + tracePath + "****");
	      
		init();
		
		videoPath = tracePath + fileVideoSegments + Util.FILE_SEPARATOR;
		if (!filemanager.directoryExist(videoPath)) {
			filemanager.mkDir(videoPath);
		} else {
			filemanager.directoryDeleteInnerFiles(videoPath);
		}
		
		streamingVideoData = new StreamingVideoData(result.getTraceDirectory());
		videoStreamConstructor.setStreamingVideoData(streamingVideoData);

		LOG.debug("\n**** processRequests(requestMap) ****");
		processRequests(requestMap);
		
		LOG.debug("\n**** processSegments() ****");
		processSegments();
		
		videoStreamConstructor.processFailures();
		
		videoStreamConstructor.clear();
		manifestReqMap.clear();
	
		streamingVideoData.scanVideoStreams();
		
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsTimings(videoAnalysisTitle, System.currentTimeMillis() - analysisStartTime, analysisCategory);

		LOG.debug("\nFinal results: \n" + streamingVideoData);
		LOG.debug("\nTraffic HEALTH:\n" + displayFailedRequests());
		LOG.debug("\n**** FIN **** " + tracePath + "\n\n");
		
		videoSegmentAnalyzer.process(result, streamingVideoData);

		LOG.debug(String.format("\nVideo Analysis Elapsed time: %.6f", (double) (System.currentTimeMillis() - analysisStartTime) / 1000));

		return streamingVideoData;
	}

	private String displayFailedRequests() {
		StringBuilder strbldr = new StringBuilder();
		double failed = streamingVideoData.getFailedRequestMap().size();
		double succeeded = streamingVideoData.getRequestMap().size();
		if (failed!=0 || succeeded!=0) {
			strbldr.append(String.format("Video network traffic health : %2.0f%% Requests succeeded", succeeded * 100 / (succeeded + failed)));
		}
		if (failed != 0) {
			strbldr.append("\nFailed requests list by timestamp");
			streamingVideoData.getFailedRequestMap().entrySet().stream().forEach(x -> {
				strbldr.append(String.format("\n%10.3f %s", x.getKey(), x.getValue()));
			});
		}
		return strbldr.toString();
	}

	public void init() {
		LOG.info("\n**** INITIALIZED **** " + tracePath);
		videoStreamConstructor.init();
		manifestReqMap.clear();
		clearData();
	}

	public void processRequests(SortedMap<Double, HttpRequestResponseInfo> requestMap) {
		for (HttpRequestResponseInfo req : requestMap.values()) {
			LOG.debug(req.toString());
			
			if (req.getAssocReqResp() == null) {
				LOG.debug("NO Associated rrInfo (skipped):" + req.getTimeStamp() + ": " + req.getObjUri());
				streamingVideoData.addFailedRequestMap(req);
				continue;
			}
			
			String fname = videoStreamConstructor.extractFullNameFromRRInfo(req);
			LOG.debug("oName :" + req.getObjNameWithoutParams() + "\theader :" + req.getAllHeaders() + "\tresponse :" + req.getAssocReqResp().getAllHeaders());

			String extn = videoStreamConstructor.extractExtensionFromRequest(req);
			if (extn == null) {
				if (fname.contains("audio")) {
					extn = "audio";
				} else {
					extn = fname;
				}
			}
			
			// catch offset filename, usually before a byte range, example: 4951221a-709b-4cb9-8f52-7bd7abb4b5c9_v5.ts/range/340280-740719
			if (fname.contains(".ts\\/")) {
				extn = ".ts";
			}
			if (fname.contains(".aac\\/")) {
				extn = ".aac";
			}

			switch (extn) {
			
			// DASH - Amazon, Hulu, Netflix, Fullscreen
			case ".mpd": // Dash Manifest
				processManifestDASH(req);
				break;
				
			case ".m3u8": // HLS Manifest, iOS, DTV
				processManifestHLS(req);
				break;

			case ".m4a": // audio
			case ".mp4a": // audio
			case ".aac":
			case ".ac3":
			case "audio":
			case ".dash":
			case ".mp2t": // MPEG
			case ".mp4":
			case ".mp4v":
			case ".m4v":
			case ".m4i":
			case ".m4s":	
			case ".ts": // TransportStream
				segmentRequests.put(req.getFirstDataPacket().getPacketId(), req);
				LOG.debug("\tsegment: " + trackManifestTimeStamp+": "+req.getObjNameWithoutParams());
				manifestReqMap.put(req.getObjNameWithoutParams(), trackManifestTimeStamp);
				break;
				
			case ".ism": // SSM
				LOG.debug("skipping SmoothStreamingMedia :" + fname);
				break;
			case ".vtt":
				LOG.debug("skipping closed caption :" + fname);
				break;
				
			default:// items found here may need to be handled OR ignored as the above
				LOG.debug("skipped :" + fname);
				break;
			}
		}
	}

	public void processManifestDASH(HttpRequestResponseInfo req) {
		if ((manifest = videoStreamConstructor.extractManifestDash(streamingVideoData, req)) != null) {
			if (manifest.getManifestType().equals(ManifestType.CHILD) && manifest.getContentType().equals(ContentType.VIDEO)) {
				trackManifestTimeStamp = req.getTimeStamp();
			}
			LOG.debug("extracted :" + manifest.getVideoName());
		} else {
			LOG.error("Failed to extract manifest:" + req);
		}
	}
	
	public void processManifestHLS(HttpRequestResponseInfo req) {
		if ((manifest = videoStreamConstructor.extractManifestHLS(streamingVideoData, req)) != null) {
			LOG.debug(String.format("(%s) Manifest videoName:%s, timestamp: %.3f", manifest.getManifestType(), manifest.getVideoName(), manifest.getRequestTime()));
			if (manifest.getManifestType().equals(ManifestType.CHILD)
					&& (manifest.getContentType().equals(ContentType.VIDEO) || manifest.getContentType().equals(ContentType.MUXED))) {
				LOG.debug("childManifest videoName:" + manifest.getVideoName() + ", timestamp: " + manifest.getRequestTime());
				trackManifestTimeStamp = req.getTimeStamp();
			} else if (manifest.getManifestType().equals(ManifestType.MASTER)) {
				LOG.debug("Manifest videoName:" + manifest.getVideoName() + ", timestamp: " + manifest.getRequestTime());
			}
			LOG.debug("extract :" + manifest.getVideoName());
			LOG.debug(manifest.displayContent(true, 20));
		}
	}
	
	public void processSegments() {
		Double trackManifestTimestamp = null;
		LOG.debug("\n>>>>>>>>>> segmentRequests: " + segmentRequests);
		for (HttpRequestResponseInfo request : segmentRequests.values()) {
			LOG.debug("\n>>>>>>>>>> Segment: " + request.getObjNameWithoutParams());
			trackManifestTimestamp = manifestReqMap.get(request.getObjNameWithoutParams());
			videoStreamConstructor.extractVideo(streamingVideoData, request, trackManifestTimestamp);
		}
	}
	
	@Override
	public StreamingVideoData clearData() {
		this.segmentRequests.clear();
		if (streamingVideoData != null ) {
			streamingVideoData.getStreamingVideoCompiled().clear();
		}
		videoTabHelper.resetRequestMapList();
		streamingVideoData = new StreamingVideoData("");
		return streamingVideoData;
	}

	@Override
	public StreamingVideoData getStreamingVideoData() {
		return streamingVideoData;
	}
}