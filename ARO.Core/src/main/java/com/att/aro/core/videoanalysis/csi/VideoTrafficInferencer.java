package com.att.aro.core.videoanalysis.csi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.Segment;
import com.att.aro.core.video.pojo.Track;
import com.att.aro.core.video.pojo.VideoManifest;
import com.att.aro.core.videoanalysis.csi.parsers.HLSManifestParserForCSI;
import com.att.aro.core.videoanalysis.csi.pojo.CSIManifestAndState;
import com.att.aro.core.videoanalysis.impl.ManifestBuilderDASH;
import com.att.aro.core.videoanalysis.impl.VideoSegmentAnalyzer;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.Manifest.ManifestType;
import com.att.aro.core.videoanalysis.pojo.MediaType;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class VideoTrafficInferencer {

	private static final Logger LOGGER = LogManager.getLogger(VideoTrafficInferencer.class.getName());

	private static final double TRANSFER_OVERHEAD_LOW = .7;
	private static final double TRANSFER_OVERHEAD_HIGH = 1.3;
	private static final String SEGMENT_REPLACEMENT = "SEGMENT_REPLACEMENT";
	
	private double firstRequestTimeStamp;
	private StreamingVideoData streamingVideoData;
	Map<Double, HttpRequestResponseInfo> requestMap;
	Map<Integer, HttpRequestResponseInfo> possibleAudioRequestMap;
	Set <HttpRequestResponseInfo> videoRequestMap;
	Map<Integer, HttpRequestResponseInfo> nonSegmentRequestMap;
	
	@Autowired
	private IFileManager filemanager;
	
	@Autowired
	private ICSIDataHelper csiDataHelper;
	
	@Autowired
	private HLSManifestParserForCSI hlsManifestParseImpl;
	
	@Autowired 
	private VideoSegmentAnalyzer videoSegmentAnalyzer;

	public VideoTrafficInferencer() {
		super();
	}

	public StreamingVideoData inferVideoData(AbstractTraceResult result, List<Session> sessionlist, String manifestFilePath) {
		
		videoRequestMap = new HashSet<>();
		nonSegmentRequestMap = new HashMap<>();
		possibleAudioRequestMap = new TreeMap<>();
		streamingVideoData = new StreamingVideoData(result.getTraceDirectory());
		boolean flag = false;
		
		if (result.getTraceDirectory().equals(manifestFilePath)) {
			CSIManifestAndState csiState = csiDataHelper.readData(manifestFilePath + System.getProperty("file.separator") + "CSI");
			if (csiState.getAnalysisState().equals("Fail")) {
				return streamingVideoData;
			}
			manifestFilePath = csiState.getManifestFileLocation();
		} else {
			flag = true;
		}
		
		File manifestFile = new File(manifestFilePath);
		byte[] fileContent;
		VideoManifest videoManifest = new VideoManifest();
		List<Track> tracks = new ArrayList<>();
		String fileExtName = FilenameUtils.getExtension(manifestFile.getPath());
		
		
		requestMap = generateRequestMap(sessionlist);
		
		if (manifestFile.exists() && fileExtName != null) {
			switch (fileExtName) {
			case "json":
				ObjectMapper mapper = new ObjectMapper();
				mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				try {
					videoManifest = mapper.readValue(new File(manifestFilePath), VideoManifest.class);
				} catch (IOException ioe) {
					LOGGER.error("Exception while parsing Manifest JSON for CSI", ioe);
				}
				break;
			case "mpd":
				try {
					fileContent = Files.readAllBytes(manifestFile.toPath());
					ManifestBuilderDASH manifestBuilderDASH = new ManifestBuilderDASH();
					manifestBuilderDASH.create(requestMap.values().iterator().next(), fileContent, "blank");
					for (ChildManifest cManifest : manifestBuilderDASH.getManifestCollection()
							.getSegmentChildManifestListInOrder()) {
						Track sTrack = new Track();
						List<Integer> segmentSizes = new ArrayList<Integer>();
						List<Double> segmentDurations = new ArrayList<Double>();
						sTrack.setMediaType(cManifest.isVideo() ? MediaType.VIDEO : MediaType.AUDIO);
						sTrack.setMediaBandwidth((float) cManifest.getBandwidth());
						cManifest.getSegmentInfoTrie().values().forEach((segment) -> segmentSizes.add(segment.getSize()));
						cManifest.getSegmentInfoTrie().values()
								.forEach((segment) -> segmentDurations.add(segment.getDuration()));
						sTrack.setSegmentSizes(segmentSizes);
						sTrack.setSegmentDurations(segmentDurations);
						tracks.add(sTrack);
					}
				} catch (IOException ioe) {
					LOGGER.error("Exception while parsing MPD for CSI", ioe);
				}
				videoManifest.setTracks(tracks);
			case "m3u8":
				try {
				videoManifest = hlsManifestParseImpl.getManifest(videoManifest, manifestFile);
				} catch (Exception e) {
					LOGGER.error(e.getMessage());
				}
				break;
				
			default:
				break;
			}
		}

		List<Segment> candidateList = prepareCandidates(requestMap, videoManifest);
		Map<Integer, List<Segment>> edgeDistanceMap = calculateDistancesAndPopulateAdjacencyList(candidateList, requestMap);
		TreeMap<Integer, List<Integer>> lastNodes = findShortestPath(candidateList, edgeDistanceMap);
		
		if (!lastNodes.isEmpty()) {
	
			VideoStream videoStream = new VideoStream();
			videoStream.setManifest(createManifest(FilenameUtils.getBaseName(manifestFile.getPath()), ManifestType.MASTER, ContentType.MUXED));
			streamingVideoData.addVideoStream(firstRequestTimeStamp, videoStream);
			List<Segment> solution = getLikelySequences(candidateList, edgeDistanceMap, lastNodes);
			Manifest manifest;
			if (!solution.isEmpty()) {
				for (Segment segment : solution) {
					manifest = createManifest(FilenameUtils.getBaseName(manifestFile.getPath()), ManifestType.CHILD, ContentType.VIDEO);
					ChildManifest childManifest = new ChildManifest();
					childManifest.setManifest(manifest);
					VideoEvent videoEvent = new VideoEvent(getDefaultThumbnail(), manifest, segment, requestMap.get(segment.getRequestKey()));
					videoRequestMap.add(requestMap.get(segment.getRequestKey()));
					videoEvent.setChildManifest(childManifest);
					videoStream.addVideoEvent(videoEvent);
				}

				int segmentIndex = 0;
				Track audioTrack;
				if ((audioTrack = videoManifest.getTracks().stream().filter(track -> (track.getMediaType()).equals(MediaType.AUDIO)).findFirst().get()) != null) {
					for (HttpRequestResponseInfo rrInfo : possibleAudioRequestMap.values()) {
						if (!videoRequestMap.contains(rrInfo) && rrInfo.getTime() > videoStream.getFirstSegment().getDLTime()) {
							Segment audioSegment = new Segment(videoManifest, videoManifest.getAudioTrack(), ++segmentIndex, audioTrack.getSegmentSizes().get(segmentIndex - 1), rrInfo.getKey(), rrInfo.getRequestCounterCSI(), -1);
							manifest = createManifest(FilenameUtils.getBaseName(manifestFile.getPath()), ManifestType.CHILD, ContentType.AUDIO);
							ChildManifest childManifest = new ChildManifest();
							childManifest.setManifest(manifest);
							VideoEvent videoEvent = new VideoEvent(getDefaultThumbnail(), manifest, audioSegment, rrInfo);
							videoEvent.setChildManifest(childManifest);
							videoStream.addVideoEvent(videoEvent);
						}
					}
				}
			}
		}
		
		if (flag) {
			saveCSIManifestAndState(manifestFilePath);
		}
		
		videoSegmentAnalyzer.process(result, streamingVideoData);
		
		return streamingVideoData;
	}
	
	private void saveCSIManifestAndState(String manifestFilePath) {
		if (StringUtils.isNotBlank(manifestFilePath)) {
			Path manifestPath = Paths.get(manifestFilePath);
			if (manifestPath != null) {
				CSIManifestAndState csiState = new CSIManifestAndState();
				csiState.setAnalysisState(streamingVideoData.getVideoStreams().size() > 0 ? "Success" : "Fail");
				csiState.setManifestFileLocation(manifestFilePath);
				Path fileName = manifestPath.getFileName();
				csiState.setManifestFileName(fileName != null ? fileName.toString() : "");
				fileName = manifestPath.getParent();
				csiDataHelper.saveData(fileName != null ? fileName.toString() : "", csiState);
			}
		}
	}

	private Manifest createManifest(String manifestName, ManifestType manifestType, ContentType contentType) {
		Manifest manifestMaster = new Manifest();
		manifestMaster.setVideoName(manifestName);
		manifestMaster.setManifestType(manifestType);
		manifestMaster.setContentType(contentType);
		return manifestMaster;
	}
	
	private List<Segment> getLikelySequences(List<Segment> candidateList, Map<Integer, List<Segment>> edgeDistanceMap, TreeMap<Integer, List<Integer>> lastNodes) {
		
		List<List<Integer>> paths = new ArrayList<List<Integer>>();
		List<List<Integer>> shortestPath = new ArrayList<List<Integer>>();
		
		List<Integer> initialList = new ArrayList<Integer>();
		initialList.add(lastNodes.lastKey());
		paths.add(initialList);
		List<Segment> solution = new ArrayList<>();
		boolean flagToQuit = false;
		int[] total = {0};
		lastNodes.keySet().parallelStream().forEach(key -> {total[0] += ((List<Integer>)lastNodes.get(key)).size();});
		while (paths.size() > 0 && paths.size() <= 9000000 && !flagToQuit) {
			List<List<Integer>> updatedPath = new ArrayList<List<Integer>>();
			for (List<Integer> list : paths) {
				int currentNode = list.get(list.size() - 1);
				List<Integer> tempList = new ArrayList<>(list);
				for (int node : lastNodes.get(currentNode)) {
					if (node == 0) {
						shortestPath.add(list);
						flagToQuit = true;
						break;
					} else {
						tempList.add(node);
					}
				}
				updatedPath.add(tempList);
				if (flagToQuit) {
					break;
				}
			}
			paths = updatedPath;
		}
		if (!shortestPath.isEmpty()) {
			List<Integer> list = shortestPath.stream().max(Comparator.comparing(List::size)).get();
			Collections.reverse(list);
			for (int index : list) {
				solution.add(candidateList.get(index));
			}
		}
		
		return solution;
	}
	
	private TreeMap<Integer, List<Integer>> findShortestPath(List<Segment> candidateList, Map<Integer, List<Segment>> edgeDistanceMap) {
		
		int[] distance = new int[candidateList.size()];
		TreeMap<Integer, List<Integer>> lastNodes = new TreeMap<Integer, List<Integer>>();
		List<Integer> knownSet = new ArrayList<>();
		List<Integer> unknownSet = new ArrayList<>(candidateList.size());
		for (int indexi = 0; indexi < candidateList.size() ; indexi++) {
        	distance[indexi] = Integer.MAX_VALUE - 1; 
        	unknownSet.add(indexi);
		}
		
		distance[0] = 0;
		int smallestIndex = 0;
		while (unknownSet.size() > 0) {
			int closestDistance = Integer.MAX_VALUE;
			for (int index : unknownSet) {
				if (distance[index] < closestDistance) {
					smallestIndex = index;
					closestDistance = distance[index];
				}
			}
			if (closestDistance >= Integer.MAX_VALUE - 1) {
                break;
			}
			int index = smallestIndex;
			knownSet.add(index);
			int setIndex = unknownSet.indexOf(index);
			unknownSet.remove(setIndex);
			
			for (int indexi : unknownSet) {
				if (edgeDistanceMap.containsKey(index) && (edgeDistanceMap.get(index)).contains(candidateList.get(indexi))) {
					 if (distance[indexi] > distance[index]) {
						 List<Integer> list = new ArrayList<>();
						 list.add(index);
						 lastNodes.put(indexi, list);
						 distance[indexi] =  distance[index];
					 } else if (distance[indexi] == distance[index]) {
						 lastNodes.get(indexi).add(index);
					 }
				}	 
			}
		}
		
		return lastNodes;
		
	}

	private Map<Integer, List<Segment>> calculateDistancesAndPopulateAdjacencyList (List<Segment> candidateList, Map<Double, HttpRequestResponseInfo> requestMap) {
		
		Map<Integer, List<Segment>> edgeDistanceMap = new TreeMap<Integer, List<Segment>>();
		boolean segmentReplacement = Boolean.parseBoolean(PreferenceHandlerImpl.getInstance().getPref(VideoTrafficInferencer.SEGMENT_REPLACEMENT));
		for (int indexi = 0; indexi < candidateList.size(); indexi++) {
			for (int indexj = indexi+1; indexj < candidateList.size(); indexj++) {
				
				int iRequestNo = candidateList.get(indexi).getRequestNumber();
				int jRequestNo = candidateList.get(indexj).getRequestNumber();
				if (iRequestNo >= jRequestNo) {
                    continue;
				}
				
				boolean flag = false;
				for (int indexk = iRequestNo + 1; indexk < jRequestNo; indexk++) {
					if (!possibleAudioRequestMap.containsKey(indexk) && !nonSegmentRequestMap.containsKey(indexk)) {
						flag = true;
						break;
					}
					
				}
				
				if (flag) {
					break;
				}
				
				if (candidateList.get(indexi).getSegmentIndex() == -1 || candidateList.get(indexj).getSegmentIndex() == -1 || (candidateList.get(indexi).getSegmentIndex() + 1 == candidateList.get(indexj).getSegmentIndex())) {
					if (!edgeDistanceMap.containsKey(indexi)) {
						List<Segment> tempList = new ArrayList<>();
						tempList.add(candidateList.get(indexj));
						edgeDistanceMap.put(indexi, tempList);
					} else {
						List<Segment> tempList = edgeDistanceMap.get(indexi);
						tempList.add(candidateList.get(indexj));
					}
				} else if (segmentReplacement) {
					int indexDifference = candidateList.get(indexi).getSegmentIndex() -  candidateList.get(indexj).getSegmentIndex();
					if (0 < indexDifference && indexDifference <= 20) {
						if (!edgeDistanceMap.containsKey(indexi)) {
							List<Segment> tempList = new ArrayList<>();
							tempList.add(candidateList.get(indexj));
							edgeDistanceMap.put(indexi, tempList);
						} else {
							List<Segment> tempList = edgeDistanceMap.get(indexi);
							tempList.add(candidateList.get(indexj));
						}
					}
				}
			}
		}
		
		return edgeDistanceMap;
	}

	private List<Segment> prepareCandidates(Map<Double, HttpRequestResponseInfo> reqMap, VideoManifest manifest) {
		
		List<Segment> candidateList = new ArrayList<Segment>();
		candidateList.add(new Segment(manifest,null,-1,-1,-1,-1,-1));
		for (HttpRequestResponseInfo req : reqMap.values()) {
			
			// Reducing 10000 bytes in the minimum to account for lost packets in the traffic file
			double minVideoSegmentSize = (req.getContentLength() - 30000) / TRANSFER_OVERHEAD_HIGH;
			double maxVideoSegmentSize = req.getContentLength() / TRANSFER_OVERHEAD_LOW;
			
			double minAudioRequestSize = manifest.getMedianAudioTrackSize() * TRANSFER_OVERHEAD_LOW /** .95*/;
			double maxAudioRequestSize = manifest.getMedianAudioTrackSize() * TRANSFER_OVERHEAD_HIGH;
			
			double minVideoRequestSize = (manifest.getMaxVideoSegmentIndexSize() + 30000) * TRANSFER_OVERHEAD_HIGH;
			
			if (minAudioRequestSize < req.getContentLength() && req.getContentLength() < maxAudioRequestSize) {
				req.setAudioPossibility(true);
				possibleAudioRequestMap.put(req.getRequestCounterCSI(), req);
			}
			
			if (req.getContentLength() >  minVideoRequestSize) {
				req.setVideoPossibility(true);
			} else {
				nonSegmentRequestMap.put(req.getRequestCounterCSI(), req);
				continue;
			}
			
			if (manifest != null && manifest.getTracks() != null) {
				for (Track track : manifest.getTracks()) {
					if (track.getMediaType() == MediaType.VIDEO) {
						int counter = 0;
						for (int size : track.getSegmentSizes()) {
							counter++;
							if (minVideoSegmentSize <= size && size <= maxVideoSegmentSize) {
								candidateList.add(new Segment(manifest, track, counter, size, req.getKey(), req.getRequestCounterCSI(), candidateList.size() - 1));
							}
						}
					}
				}
			}
		}
		
		candidateList.add(new Segment(manifest,null, -1, -1, -1, -1, candidateList.size()-1));
		return candidateList;
	}
	
	public Map<Double, HttpRequestResponseInfo> generateRequestMap(List<Session> sessionList) {
		TreeMap<Double, HttpRequestResponseInfo> requestMap = new TreeMap<>();
		int counter = 0;
		for (Session session : sessionList) {
			List<HttpRequestResponseInfo> rriList = session.getRequestResponseInfo();
			for (HttpRequestResponseInfo rrInfo : rriList) {
				rrInfo.setSession(session);
				if (rrInfo.getContentLength() > 0 && rrInfo.getDirection() == HttpDirection.RESPONSE) {
					rrInfo.setKey(getReqInfoKey(rrInfo.getTimeStamp(), ++counter));
					requestMap.put(rrInfo.getKey(), rrInfo);
				}
			}
		}
		counter = 0;
		for (HttpRequestResponseInfo rrInfo : requestMap.values()) {
			firstRequestTimeStamp = rrInfo.getTimeStamp();
			rrInfo.setRequestCounterCSI(counter++);
		}
		
		return requestMap;
	}
	
	private Double getReqInfoKey(double timestamp, int counter) {
		return timestamp * 1000000 + counter;
	}
	
	private byte[] getDefaultThumbnail() {
		byte[] defaultThumbnail = null;
		String iconName = "aro_24.png";
		String appIconPath = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + iconName;
		if (filemanager.fileExist(appIconPath)) {
			try {
				Path path = Paths.get(appIconPath);
				defaultThumbnail = Files.readAllBytes(path);
			} catch (IOException e) {
				LOGGER.debug("getIconThumbnail IOException:" + e.getMessage());
			}
		}
		return defaultThumbnail;
	}

}
