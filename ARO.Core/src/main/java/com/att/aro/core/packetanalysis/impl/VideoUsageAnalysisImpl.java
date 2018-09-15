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
package com.att.aro.core.packetanalysis.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.IVideoUsageAnalysis;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.preferences.IPreferenceHandler;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.settings.Settings;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoAnalysisConfigHelper;
import com.att.aro.core.videoanalysis.IVideoEventDataHelper;
import com.att.aro.core.videoanalysis.IVideoTabHelper;
import com.att.aro.core.videoanalysis.impl.RegexMatchLbl;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.ManifestDashPlayReady;
import com.att.aro.core.videoanalysis.pojo.ManifestHLS;
import com.att.aro.core.videoanalysis.pojo.ManifestSSM;
import com.att.aro.core.videoanalysis.pojo.RegexMatchResult;
import com.att.aro.core.videoanalysis.pojo.VideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoEventData;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.MPDAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.SSMAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.XmlManifestHelper;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.MPDPlayReady;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Video usage analysis
 *
 * @Barry
 */
public class VideoUsageAnalysisImpl implements IVideoUsageAnalysis {
	@Autowired
	private IFileManager filemanager;

	@Autowired
	private Settings settings;

	private static final Logger LOG = LogManager.getLogger(VideoUsageAnalysisImpl.class.getName());
	
	private IExternalProcessRunner extrunner;

	@Autowired
	public void setExternalProcessRunner(IExternalProcessRunner runner) {
		this.extrunner = runner;
	}

	@Autowired
	private IVideoAnalysisConfigHelper voConfigHelper;

	@Autowired
	private IVideoEventDataHelper voEventDataHelper;

	@Autowired
	private IVideoTabHelper videoTabHelper;

	@Autowired
	public void setHttpRequestResponseHelper(IHttpRequestResponseHelper reqhelper) {
		this.reqhelper = reqhelper;
	}

	@Autowired
	private IStringParse stringParse;
	
	Pattern extensionPattern = Pattern.compile("^\\b[a-zA-Z0-9]*\\b([\\.a-zA-Z0-9]*\\b)");
	IPreferenceHandler prefs = PreferenceHandlerImpl.getInstance();
	private IHttpRequestResponseHelper reqhelper;
	private String tracePath;
	private boolean imageExtractionRequired = false;
	private String videoPath;
	private String imagePath;
	private boolean absTimeFlag = false;
	private String htmlPath;
	private AROManifest aroManifest = null;
	private final String fileVideoSegments = "video_segments";
	private VideoUsagePrefs videoUsagePrefs;
	private VideoAnalysisConfig vConfig;
	private TreeMap<Long, HttpRequestResponseInfo> reqMap;
	private VideoUsage videoUsage;
	private double timeScale = 1;
	private String lastContentType;
	private byte[] altImage;
	private String lastVedName;
	private String lastReqObjName;
	@Value("${ga.request.timing.videoAnalysisTimings.title}")
	private String videoAnalysisTitle;
	@Value("${ga.request.timing.analysisCategory.title}")
	private String analysisCategory;

	@Autowired
	@Qualifier("videoUsagePrefs")
	public void setVideoUsagePrefs(VideoUsagePrefs videoUsagePrefs) {
		this.videoUsagePrefs = videoUsagePrefs;
	}

	/**
	 * <pre>
	 * Loads a replacement for missing thumbnails, blocked by DRM. The default
	 * is the VO App icon image User defined replacement needs to be a PNG image
	 * and should be in the VideoOptimizerLibrary and named broken_thumbnail.png
	 *
	 * @return byte[] of a png image
	 */
	private byte[] loadDefaultThumbnail() {
		byte[] data = null;
		String brokenThumbnailPath = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + "broken_thumbnail.png";
		if (filemanager.fileExist(brokenThumbnailPath)) {
			try {
				Path path = Paths.get(brokenThumbnailPath);
				data = Files.readAllBytes(path);
			} catch (IOException e) {
				LOG.debug("getThumnail IOException:" + e.getMessage());
			}
		} else {
			String iconName = "aro_24.png";
			String appIconPath = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + iconName;
			if (!filemanager.fileExist(appIconPath)) {
				Util.makeLibFilesFromJar(iconName);
			}
			if (filemanager.fileExist(appIconPath)) {
				try {
					Path path = Paths.get(appIconPath);
					data = Files.readAllBytes(path);
				} catch (IOException e) {
					LOG.debug("getIconThumnail IOException:" + e.getMessage());
				}
			}
		}
		return data;
	}

	/**
	 * Load VideoUsage Preferences
	 */
	@Override
	public void loadPrefs() {
		ObjectMapper mapper = new ObjectMapper();
		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null && !temp.equals("null")) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
			}
		} else {
			try {
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				LOG.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
			}
		}
	}

	@Override
	public IPreferenceHandler getPrefs() {
		return prefs;
	}

	@Override
	public VideoUsagePrefs getVideoUsagePrefs() {
		return videoUsagePrefs;
	}

	@Override
	public VideoUsage clearData() {
		if (videoUsage != null && videoUsage.getFilteredSegments() != null) {
			videoUsage.setFilteredSegments(null);
		}
		if (videoUsage != null && videoUsage.getAllSegments() != null) {
			videoUsage.setAllSegments(null);
		}
		videoTabHelper.resetRequestMapList();
		return new VideoUsage("");
	}

	@SuppressWarnings("checkstyle:fallthrough")
	@Override
	public VideoUsage analyze(AbstractTraceResult result, List<Session> sessionlist) {
		long analysisStartTime = System.currentTimeMillis();
		loadPrefs();
		tracePath = result.getTraceDirectory() + Util.FILE_SEPARATOR;
		LOG.info("VideoAnalysis for :" + tracePath);
		imagePath = tracePath + "Image" + Util.FILE_SEPARATOR;
		htmlPath = tracePath + "HTML" + Util.FILE_SEPARATOR;
		if (!filemanager.directoryExistAndNotEmpty(htmlPath)) {
			filemanager.mkDir(htmlPath);
			for (final Session session : sessionlist) {
				for (final HttpRequestResponseInfo req : session.getRequestResponseInfo()) {
					if (req.getDirection() == HttpDirection.RESPONSE
							&& req.getContentType() != null
							&& req.getContentType().contains("text/html")) {
						extractHtmlContent(session, req);
					}
				}
			}
		}

		altImage = loadDefaultThumbnail();
		videoPath = tracePath + fileVideoSegments + Util.FILE_SEPARATOR;
		if (!filemanager.directoryExist(videoPath)) {
			filemanager.mkDir(videoPath);
		} else {
			filemanager.directoryDeleteInnerFiles(videoPath);
		}
		if (!filemanager.directoryExist(imagePath)) {
			imageExtractionRequired = true;
			filemanager.mkDir(imagePath);
		} else {
			// Not Required but needed if in case the method is called a second time after initialization
			imageExtractionRequired = false;
		}
		// clear out old objects
		aroManifest = null;
		videoUsage = new VideoUsage(result.getTraceDirectory());
		videoUsage.setVideoUsagePrefs(videoUsagePrefs);
		loadExternalManifests();
		reqMap = collectReqByTS(sessionlist);
		String imgExtension = null;
		String filename = null;
		for (HttpRequestResponseInfo req : reqMap.values()) {
			LOG.info(req.toString());
			if (req.getAssocReqResp() == null) {
				continue;
			}
			String oName = req.getObjNameWithoutParams();
			LOG.info("oName :" + req.getObjNameWithoutParams() + "\theader :" + req.getAllHeaders() + "\tresponse :" + req.getAssocReqResp().getAllHeaders());
			String fullName = extractFullNameFromRRInfo(req);
			String extn = extractExtensionFromName(req.getFileName());
			if (extn == null || StringUtils.isEmpty(extn)) {
				if (oName.contains(".ism/")) {
					if (fullName.equals("manifest")) {
						aroManifest = extractManifestDash(req);
						continue;
					} else {
						if (fullName.contains("video")) {
							filename = truncateString(fullName, "_");
							extractVideo(req, aroManifest, filename);
						}
						continue;
					}
				}
				continue;
			}
			switch (extn) {
			// DASH - Amazon, Hulu, Netflix, Fullscreen
			case ".mpd": // Dash Manifest
				imgExtension = ".mp4";
				aroManifest = extractManifestDash(req);
				String message = aroManifest != null ? aroManifest.getVideoName() : "extractManifestDash FAILED";
				LOG.info("extract :" + message);
				break;
			case ".ism": // Dash/MPEG
				LOG.info("SSM");
			case ".mp4": // Dash/MPEG
			case ".m4v":
			case ".m4i":
			case ".dash":
				fullName = extractFullNameFromRRInfo(req);
				if (fullName.contains("_audio")) {
					continue;
				}
				vConfig = voConfigHelper.findConfig(req.getObjUri().toString());
				filename = extractNameFromRRInfo(req);
				extractVideo(req, aroManifest, filename);
				break;
			case ".m4a": // audio
				break;
			// DTV
			case ".m3u8": // HLS-DTV Manifest
				imgExtension = ".ts";
				if (!oName.contains("cc")) {
					aroManifest = extractManifestHLS(req);
				}
				break;
			case ".ts": // HLS video
				filename = "video" + imgExtension;
				extractVideo(req, aroManifest, filename);
				break;
			case ".vtt": // closed caption
				// filename = "videoTT.vtt";
				// extractVideo(req, aroManifest, filename);
				break;
			// Images
			case ".jpg":
			case ".gif":
			case ".tif":
			case ".png":
			case ".jpeg":
				if (imageExtractionRequired) {
					fullName = extractFullNameFromRRInfo(req);
					extractImage(req, aroManifest, fullName);
				}
				break;
			case ".css":
			case ".json":
			case ".html":
				continue;
			default:// items found here may need to be handled OR ignored as the above
				LOG.info("============ failed with extention :" + extn);
				break;
			}
			if (filename != null) {
				LOG.debug("filename :" + filename);
			}
		}

		updateDuration();
		updateSegments();
		validateManifests();
		if (!filemanager.directoryExistAndNotEmpty(imagePath)) {
			for (Session session : sessionlist) {
				for (HttpRequestResponseInfo reqResp : session.getRequestResponseInfo()) {
					if (reqResp.getDirection() == HttpDirection.RESPONSE && reqResp.getContentType() != null
							&& reqResp.getContentType().contains("image/")) {
						if (reqResp.getAssocReqResp().getObjName() != null) {
							String imageObject = reqResp.getAssocReqResp().getObjName();
							if (imageObject.indexOf("?v=") >= 0) {
								String fullName = Util.extractFullNameFromLink(imageObject);
						String extension = "." + reqResp.getContentType().substring(
										reqResp.getContentType().indexOf("image/") + 6,
										reqResp.getContentType().length());
								extractImage(session, reqResp, fullName + extension);
							}
						}
					}
				}
			}
		}
		// Always delete the temp segment folder
		if (!checkDevMode()) {
			filemanager.directoryDeleteInnerFiles(videoPath);
			filemanager.deleteFile(videoPath);
		}
		LOG.info("Video usage scan done");
		VideoUsage videousage = identifyInvalidManifest(videoUsage);
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsTimings(videoAnalysisTitle,
				System.currentTimeMillis() - analysisStartTime, analysisCategory);
		return videousage;
	}

	private void extractImage(Session session, HttpRequestResponseInfo response, String imageFileName) {
		if (response != null) {
			byte[] content = null;
			String fullpath;
			try {
				content = reqhelper.getContent(response, session);
				fullpath = getImagePath() + imageFileName;
				filemanager.saveFile(new ByteArrayInputStream(content), fullpath);
			} catch (Exception e) {
				// videoUsage.addFailedRequestMap(request);
				LOG.info("Failed to extract " + getTimeString(response) + imageFileName + " response: "
						+ e.getMessage());
				return;
			}
		}
	}

	private void loadExternalManifests() {
		// Go through trace-directory/download folder to load external manifests, if exist
		String downloadPath = tracePath + "downloads" + Util.FILE_SEPARATOR;
		if (!filemanager.directoryExist(downloadPath)) {
			filemanager.mkDir(downloadPath);
		}
		String[] files = filemanager.list(downloadPath, null);
		// make sure files has manifest file extension before adding it to aroManifestMap
		double keyTimestamp = 0;
		AROManifest extManifest = null;
		for (String file : files) {
			String extension = extractExtensionFromName(file);
			File manifestFile = new File(downloadPath + file);
			byte[] content;
			if (extension != null) {
				switch (extension) {
				case ".mpd":
					content = fileToByteArray(manifestFile);
					aroManifest = new ManifestDash(null, content, videoPath);
					videoUsage.add(keyTimestamp, aroManifest);
					keyTimestamp = keyTimestamp + 1000; // increment by 1ms
					break;
				case ".m3u8":
					content = fileToByteArray(manifestFile);
					if (extManifest == null) {
						try {
							extManifest = new ManifestHLS(null, content, videoPath);
							videoUsage.add(keyTimestamp, extManifest);
							keyTimestamp = keyTimestamp + 1000; // increment by 1ms
							if (aroManifest == null) {
								aroManifest = extManifest;
							}
						} catch (Exception e) {
							LOG.error("Failed to parse manifest data, Name:" + manifestFile.getName());
						}
					} else {
						try {
							((ManifestHLS) extManifest).parseManifestData(content);
						} catch (Exception e) {
							LOG.error("Failed to parse manifest data");
						}
					}
					break;
				default:// items found here may need to be handled OR ignored as the above
					LOG.debug("failed with extention :" + extension);
					break;
				}
			}
		}
	}

	private void validateManifests() {
		if (videoUsage != null) {
			TreeMap<Double, AROManifest> manifestMap = videoUsage.getAroManifestMap();
			Set<Double> manifestKeySet = manifestMap.keySet();
			for (Double key : manifestKeySet) {
				AROManifest manifest = manifestMap.get(key);
				manifest.setValid(true);
				if (manifest.getVideoEventList().isEmpty()) {
					manifest.setValid(false);
				}
				for (VideoEvent event : manifest.getVideoEventList().values()) {
					if (event.getSegment() < 0) {
						manifest.setValid(false);
					}
				}
			}
		}
	}

	private void extractHtmlContent(Session session, HttpRequestResponseInfo response) {
		if (response != null) {
			byte[] content = null;
			try {
				content = reqhelper.getContent(response, session);
				if (content != null && content.length > 0) {
					if (!filemanager.directoryExist(htmlPath)) {
						filemanager.mkDir(htmlPath);
					}
					filemanager.saveFile(new ByteArrayInputStream(content),
							htmlPath + Long.toString(System.currentTimeMillis()) + ".html");
				}
			} catch (Exception e) {
				videoUsage.addFailedRequestMap(response);
				LOG.info("Failed to extract HTML " + e.getMessage());
				return;
			}
		}
	}

	private void updateSegments() {
		LOG.info("updateSegments()");
		TreeMap<String, Integer> segmentList;
		Integer segment;
		if (videoUsage != null) {
			for (AROManifest manifest : videoUsage.getManifests()) {
				if (manifest.getDuration() > 0 || !manifest.getSegmentEventList().isEmpty()) {
					segmentList = manifest.getSegmentList();
					if (segmentList != null && !segmentList.isEmpty()) {
						for (VideoEvent videoEvent : manifest.getVideoEventList().values()) {
							// key = generateVideoEventKey(segment, timestamp, videoEvent.getQuality());
							if (videoEvent.getSegment() < 0) {
								String key = "";
								if (videoEvent.getVed().getDateTime() != null) {
									key = String.format("%s.%s", videoEvent.getVed().getDateTime(), videoEvent.getVed().getExtension());
								} else if (videoEvent.getVed().getSegmentReference() != null) {
									key = videoEvent.getVed().getSegmentReference();
								}
								segment = segmentList.get(key);
								if (segment != null) {
									videoEvent.setSegment(segment);
								}
							}
							if (videoEvent.getDuration() <= 0) {
								videoEvent.setDuration(manifest.getDuration());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * <pre>
	 * Final pass to fix update values if not set already.
	 * Examine startTime for each segment compared with next segment to determine duration when not set.
	 *
	 * Problems occur when there is a missing segment and on the last segment.
	 *  Missing segments cause an approximation by dividing the duration by the number of missing segments+1
	 *  The last segment simply repeats the previous duration, this should not skew results by much.
	 */
	private void updateDuration() {
		LOG.info("updateDuration()");
		if (videoUsage != null) {
			for (AROManifest manifest : videoUsage.getManifests()) {
				if (manifest != null) {
					NavigableMap<String, VideoEvent> eventMap = manifest.getSegmentEventList();
					if (manifest instanceof ManifestDash && !eventMap.isEmpty()) {
						int seg = 0;
						Entry<String, VideoEvent> lastEntry = eventMap.lastEntry();
						double lastSeg = lastEntry != null ? lastEntry.getValue().getSegment() : 0;
						String key = manifest.generateVideoEventKey(0, 0, "z");
						Entry<String, VideoEvent> val;
						Entry<String, VideoEvent> valn;
						double duration = 0;
						VideoEvent event;
						String segNextKey = null;
						for (seg = 1; seg <= lastSeg; seg++) {
							segNextKey = manifest.generateVideoEventKey(seg, 0, "z");
							val = eventMap.higherEntry(key);
							valn = eventMap.higherEntry(segNextKey);
							if (val == null || valn == null) {
								break;
							}
							event = val.getValue();
							VideoEvent eventNext = valn.getValue();
							duration = eventNext.getSegmentStartTime() - event.getSegmentStartTime();
							double deltaSegment = eventNext.getSegment() - event.getSegment();
							if (deltaSegment > 1) {
								duration /= deltaSegment;
							}
							updateSegmentDuration(eventMap, key, segNextKey, duration);
							key = segNextKey;
						}
						// handle any segments at the end
						val = eventMap.higherEntry(key);
						if (val != null && segNextKey != null) {
							updateSegmentDuration(eventMap, key, segNextKey, duration);
						}
					}
				}
			}
		}
	}

	/**
	 * Apply duration values to all events of the same segment number.
	 *
	 * @param eventMap	Map of VideoEvents
	 * @param key		starting key
	 * @param segNextKey	ending key
	 * @param duration - value to be applied to all VideoEvents
	 */
	public void updateSegmentDuration(NavigableMap<String, VideoEvent> eventMap, String key, String segNextKey, double duration) {
		for (VideoEvent subEvent : eventMap.subMap(key, segNextKey).values()) {
			if (subEvent.getDuration() <= 0) {
				subEvent.setDuration(duration);
			}
		}
	}

	private boolean checkDevMode() {
		return settings.checkAttributeValue("env", "dev");
	}

	/*
	 * Create a TreeMap of all pertinent Requests keyed by timestamp plus tie-breaker
	 *
	 * @param sessionlist
	 * @return Map of Requests
	 */
	private TreeMap<Long, HttpRequestResponseInfo> collectReqByTS(List<Session> sessionlist) {
		int counter = 0;
		TreeMap<Long, HttpRequestResponseInfo> reqMap = new TreeMap<>();
		for (Session session : sessionlist) {
			List<HttpRequestResponseInfo> rri = session.getRequestResponseInfo();
			for (HttpRequestResponseInfo rrInfo : rri) {
				if (rrInfo.getDirection().equals(HttpDirection.REQUEST)
						&& rrInfo.getRequestType() != null
						&& rrInfo.getRequestType().equals(HttpRequestResponseInfo.HTTP_GET)
						&& rrInfo.getObjNameWithoutParams().contains(".")) {
					rrInfo.setSession(session);
					Long key = getReqInfoKey(rrInfo, 0);
					if (reqMap.containsKey(key)) {
						do {
							key = getReqInfoKey(rrInfo, ++counter);
						} while (reqMap.containsKey(key));
						counter = 0;
					}
					reqMap.put(key, rrInfo);
				}
			}
			// Set a forward link for all packets in session to the next packet (within the session).
			// The last packet in session will not link anywhere of course!
			List<PacketInfo> packets = session.getPackets();
			for (int i = 0; i < packets.size() - 1; i++) {
				packets.get(i).getPacket().setNextPacketInSession(packets.get(i + 1).getPacket());
			}
		}
		return reqMap;
	}

	/*
	 * build key for storing requests in order with tie-breaker support
	 */
	private Long getReqInfoKey(HttpRequestResponseInfo rrInfo, int counter) {
		long key = Math.round(rrInfo.getTimeStamp() * 1000000 + counter);
		return key;
	}

	private String getDebugPath() {
		return videoPath;
	}

	private String getImagePath() {
		return imagePath;
	}

	/**
	 * Parse filename out of URI in HttpRequestResponseInfo
	 *
	 * @param rrInfo
	 *            HttpRequestResponseInfo
	 * @return
	 */
	private String extractFullNameFromRRInfo(HttpRequestResponseInfo rrInfo) {
		String URI = rrInfo.getObjNameWithoutParams();
		int pos = URI.lastIndexOf("/");
		String fullName = URI.substring(pos + 1, URI.length());
		return fullName;
	}

	/**
	 * <pre>
	 * if and extention then Match on name and exten, then append. else return
	 * all after last '/'
	 *
	 * @param rrInfo
	 * @return
	 */
	private String extractNameFromRRInfo(HttpRequestResponseInfo rrInfo) {
		String[] results = null;
		try {
			results = stringParse.parse(rrInfo.getFileName(), "([a-zA-Z0-9\\-]*)[_\\.]");
			if (results == null || results.length == 0) {
				String fullName = extractFullNameFromRRInfo(rrInfo);
				int pos = fullName.indexOf('_');
				return pos == -1 ? fullName : fullName.substring(0, pos);
			}
			return results[0];
		} catch (Exception e) {
			LOG.error("Exception :" + e.getMessage());
		}
		StringBuilder name = new StringBuilder("");
		if (results != null) {
			for (String part : results) {
				name.append(part);
			}
		} else {
			return "unknown";
		}
		return name.toString();
	}

	/**
	 * Returns string from the target to the end of the string
	 *
	 * @param src
	 * @param target
	 * @return
	 */
	private String truncateString(String src, String target) {
		int pos = src.indexOf(target);
		if (pos > -1) {
			return src.substring(pos);
		}
		return src;
	}

	/**
	 * Locate and return extension from filename
	 *
	 * @param src
	 * @return String extension with the dot(.)
	 */
	private String extractExtensionFromName(String src) {
		String[] matched = stringParse.parse(src, extensionPattern);
		if (matched != null && !matched[0].isEmpty()) {
			return matched[0];
		}
		String extension = null;
		int pos = src.lastIndexOf('.');
		extension = (pos == -1 ? null: src.substring(pos));
		return extension;
	}

	/**
	 * Extract a video from traffic data
	 *
	 * @param request
	 * @param session
	 * @param vManifest
	 * @param fileName
	 */
	public void extractVideo(HttpRequestResponseInfo request, AROManifest vManifest, String fileName) {
		HttpRequestResponseInfo response = request.getAssocReqResp();
		LOG.info("-------");
		LOG.info(request.getObjUri().toString());
		LOG.info(request.getAllHeaders());
		LOG.info(request.getAssocReqResp().getAllHeaders());
		LOG.info(String.format("extractVideo %.4f", request.getTimeStamp()));
		String quality = "";
		ArrayList<ByteRange> rangeList = new ArrayList<>();
		double bitrate = 0;
		double segment = 0;
		double duration = 0;

		String fullName = extractFullNameFromRRInfo(request);
		byte[] content = null;
		String fullpath;

		VideoEventData ved = parseRequestResponse(request);
		if (ved.getSegment() == null && ved.getSegmentReference() == null) {
			LOG.info("parse failure :" + request.getObjUri().toString() + ved);
			ved.setSegment(-4);
		}
		VideoFormat vFormat = findVideoFormat(ved.getExtension());

		LOG.debug("aroManifest :" + vManifest);

		try {
			content = extractContent(request);
			if (content.length == 0) {
				return;
			}
			vManifest = matchManifest(request, vManifest, ved, vFormat);
			vManifest.adhocSegment(ved);
		} catch (Exception e) {
			videoUsage.addFailedRequestMap(request);
			LOG.debug("Failed to extract " + getTimeString(response) + fileName + ", range: " + ved.getByteRange() + ", error: " + e.getMessage());
			return;
		}
		quality = ved.getQuality() == null ? "unknown" : ved.getQuality();
		rangeList.add(ved.getByteRange());
		segment = ved.getSegment() != null && ved.getSegment() >= 0
				? ved.getSegment()
				: vManifest.parseSegment(fullName, ved);
		// set format handled by aroManifest
		if (VideoFormat.UNKNOWN.equals(vManifest.getVideoFormat())) {
			vManifest.setVideoFormat(vFormat);
		}
		double segmentStartTime = 0;
		if (vManifest instanceof ManifestDash) {
			bitrate = ((ManifestDash) vManifest).getBandwith(truncateString(fullName, "_"));
			timeScale = vManifest.getTimeScale();
		} else if (vManifest instanceof ManifestDashPlayReady) {
			if (segment > 0) {
				bitrate = vManifest.getBitrate(ved.getQuality());
				timeScale = vManifest.getTimeScale();
			}
		} else if (vManifest instanceof ManifestHLS) {
			if (ved.getDuration() != null) {
				try {
					duration = Double.valueOf(ved.getDuration());
				} catch (NumberFormatException e) {
					duration = 0;
				}
			}
			if (duration == 0) {
				try {
					duration = Double.valueOf(vManifest.getDuration((int) segment));
				} catch (NumberFormatException e) {
					duration = 0;
				}
				if (duration == 0) {
					duration = vManifest.getDuration();
				}
			}
			timeScale = 1;
			try {
				Double temp = vManifest.getBitrate(quality);
				if (temp == null) {
					temp = vManifest.getBitrate("HLS" + quality);
				}
				bitrate = temp != null ? temp : 0D;
			} catch (Exception e) {
				LOG.info("invalid quality :" + quality);
			}
		} else {
			// TODO some other way
		}
		LOG.debug("trunk " + fileName + ", getTimeString(response) :" + getTimeString(response));
		byte[] thumbnail = null;
		Integer[] segmentMetaData = new Integer[2];
		String segName = null;
		segmentMetaData[0] = content.length;
		if (vManifest instanceof ManifestDash) {
			segmentMetaData = parsePayload(content);
		} else if (vManifest instanceof ManifestHLS) {
			segmentStartTime = 0;
		}
		if (segment < 0 && segmentMetaData[1] != null) {
			segment = segmentMetaData[1];
		}
		fullpath = constructDebugName(request, ved);
		LOG.debug(fileName + ", content.length :" + content.length);
		if (segment == 0 && vManifest.isVideoType(VideoType.DASH)) {
			VideoData vData = new VideoData(vManifest.getEventType(), quality, content);
			vManifest.addVData(vData);
		} else {
			String seg = String.format("%08d", ((Double) segment).intValue());
			segName = getDebugPath() + seg + '_' + ved.getId() + '.' + ved.getExtension();
			thumbnail = extractThumbnail(vManifest, content, ved);
		}
		try {
			filemanager.saveFile(new ByteArrayInputStream(content), fullpath);
		} catch (IOException e) {
			LOG.error("Failed to save segment " + getTimeString(response) + fileName + ", range: " + ved.getByteRange() + ", error: " + e.getMessage());
			return;
		}
		TreeMap<String, Double> metaData = null;
		if (thumbnail != null) {
			metaData = extractMetadata(segName);
			if (metaData != null) {
				Double val = metaData.get("bitrate");
				if (val != null) {
					bitrate = val;
				}
				val = metaData.get("Duration");
				if (val != null) {
					duration = val;
				}
				val = metaData.get("SegmentStart");
				if (val != null) {
					segmentStartTime = val;
				}
			}
		}
		if (segment > 0 && !(vManifest instanceof ManifestDashPlayReady)) {
			segmentStartTime = segmentMetaData[1] != null ? segmentMetaData[1].doubleValue() / timeScale : 0;
		}
		// a negative value indicates segment startTime
		// later will scan over segments & set times based on next segmentStartTime
		if (vManifest instanceof ManifestDash || vManifest instanceof ManifestDashPlayReady || duration == 0) {
			duration -= segmentStartTime;
		}
		if (thumbnail == null) {
			thumbnail = altImage;
		}
		VideoEvent vEvent = new VideoEvent(ved, thumbnail, vManifest, segment, quality, rangeList, bitrate, duration, segmentStartTime, segmentMetaData[0], response);
		vManifest.addVideoEvent(segment, response.getTimeStamp(), vEvent);

		aroManifest = vManifest;
	}

	private VideoFormat findVideoFormat(String extension) {
		if ("mp4".equals(extension) || "mpeg4".equals(extension) || extension.startsWith("m4")) {
			return VideoFormat.MPEG4;
		} else if ("ts".equals(extension)) {
			return VideoFormat.TS;
		}
		return VideoFormat.UNKNOWN;
	}

	/**
	 * @param request
	 * @return
	 */
	private VideoEventData parseRequestResponse(HttpRequestResponseInfo request) {
		String[] voValues;
		vConfig = voConfigHelper.findConfig(request.getObjUri().toString());
		VideoEventData ved = null;
		if (vConfig != null) {
			LOG.info(String.format("vConfig :%s", vConfig));
			Map<RegexMatchLbl, RegexMatchResult> results = voConfigHelper.match(vConfig, request.getObjUri().toString(),
					request.getAllHeaders(), request.getAssocReqResp().getAllHeaders());
			int resSize = results.values().stream().mapToInt(i -> i.getResult().length).sum();
			voValues = new String[resSize];
			int index = 0;
			for (RegexMatchResult value : results.values()) {
				for (String strValue : value.getResult()) {
					voValues[index] = strValue;
					index++;
				}
			}
			ved = voEventDataHelper.create(vConfig, voValues);
			LOG.info(ved.toString());
		} else {
			ved = voEventDataHelper.create(extractNameFromRRInfo(request),
					extractExtensionFromName(extractFullNameFromRRInfo(request)));
			ved = voEventDataHelper.create(extractNameFromRRInfo(request), extractExtensionFromName(request.getFileName()));
		}
		return ved;
	}

	private String constructDebugName(HttpRequestResponseInfo request, VideoEventData ved) {
		String fullpath;
		StringBuffer fname = new StringBuffer(getDebugPath());
		fname.append(ved.getId());
		fname.append(String.format("_%08d", ved.getSegment()));
		if (ved.getByteRange() != null) {
			fname.append("_R_");
			fname.append(ved.getByteRange());
		}
		fname.append("_dl_");
		fname.append(getTimeString(request.getAssocReqResp()));
		fname.append("_S_");
		fname.append(String.format("%08.0f", request.getSession().getSessionStartTime() * 1000));
		fname.append("_Q_");
		fname.append(ved.getQuality());
		fname.append('.');
		fname.append(ved.getExtension());
		fullpath = fname.toString();
		return fullpath;
	}

	/**<PRE>
	 * Creates a TreeMap with keys:
	 *    bitrate
	 *    Duration
	 *    SegmentStart
	 *
	 * @param srcpath
	 * @return
	 */
	private TreeMap<String, Double> extractMetadata(String srcpath) {
		TreeMap<String, Double> results = new TreeMap<>();
		String cmd = Util.getFFMPEG() + " -i " + "\"" + srcpath + "\"";
		String lines = extrunner.executeCmd(cmd);
		if (lines.indexOf("No such file") == -1) {
			double bitrate = getBitrate("bitrate: ", lines);
			results.put("bitrate", bitrate);
			Double duration = 0D;
			String[] time = StringParse.findLabeledDataFromString("Duration: ", ",", lines).split(":"); // 00:00:05.80
			Double start = StringParse.findLabeledDoubleFromString(", start:", ",", lines); // 2.711042
			if (time.length == 3) {
				try {
					duration = Integer.parseInt(time[0]) * 3600 + Integer.parseInt(time[1]) * 60 + Double.parseDouble(time[2]);
				} catch (NumberFormatException e) {
					LOG.error("failed to parse duration from :" + StringParse.findLabeledDataFromString("Duration: ", ",", lines));
					duration = 0D;
				}
			} else if (time.length > 0) {
				try {
					duration = Double.parseDouble(time[0]);
				} catch (NumberFormatException e) {
					LOG.error("failed to parse duration from :" + time[0]);
					duration = 0D;
				}
			}
			results.put("Duration", duration);
			results.put("SegmentStart", start);
		}
		return results;
	}

	/**
	 * verify video belongs with a manifest
	 *
	 * @param request
	 * @param aroManifest
	 * @param vFormat
	 * @param content
	 * @return correct AroManifest
	 */
	private AROManifest matchManifest(HttpRequestResponseInfo request, AROManifest aroManifest, VideoEventData ved, VideoFormat vFormat) {
		HttpRequestResponseInfo response = request.getAssocReqResp();
		String videoName = "";
		AROManifest newManifest = null;
		//FIXME need to update if reconstructed manifest ( not yet updated by a real manifest )
		if (aroManifest == null) {
			newManifest = videoUsage.findVideoInManifest(ved.getId());
			if (newManifest == null) {
				if (ved.getId().contains("_video_")) {
					newManifest = new ManifestDash(null, request, videoPath);
					videoUsage.add(request.getTimeStamp(), newManifest);
				} else if (ved.getId().contains(".ts") || ved.getExtension().equalsIgnoreCase(VideoFormat.TS.toString())) {
					newManifest = new ManifestHLS(request, ved, videoPath);
					videoUsage.add(request.getTimeStamp(), newManifest);
				} else {
					// TODO - future plans - create and use class ManifestUnknown instead
					newManifest = new AROManifest(VideoType.UNKNOWN, request, ved, videoPath);
					videoUsage.add(request.getTimeStamp(), newManifest);
				}
			} else {
				newManifest = new AROManifest(VideoType.UNKNOWN, response, ved, videoPath);
				videoUsage.add(request.getTimeStamp(), newManifest);
				ved.setSegment(0);
				ved.setQuality("unknown");
			}
			newManifest.singletonSetVideoName(ved.getId());
			videoUsage.add(request.getTimeStamp(), newManifest);
		} else {
			newManifest = aroManifest;
		}
		videoName = newManifest.getVideoName();
		// if (videoName != null && !videoName.isEmpty() && ved.getId() != null && !ved.getId().contains(videoName)) {
		if (!ved.getId().equals(videoName)) {
			LOG.info(String.format("%s -> %s", videoName, ved.getId()));
			newManifest = videoUsage.findVideoInManifest(ved.getId());
			if (!newManifest.getVideoFormat().equals(VideoFormat.UNKNOWN) && !newManifest.getVideoFormat().equals(vFormat)) {
				newManifest = new AROManifest(VideoType.UNKNOWN, response, videoPath);
				ved.setSegment(0);
				ved.setQuality("unknown");
				videoUsage.add(request.getTimeStamp(), newManifest);
			}
		}
		return newManifest;
	}

	/**
	 * Parse mp4 chunk/segment that contains one moof and one mdat.
	 *
	 * @param content
	 * @return double[] mdat payload length, time sequence
	 */
	private Integer[] parsePayload(byte[] content) {
		byte[] buf = new byte[4];
		int mdatSize = 0;
		ByteBuffer bbc = ByteBuffer.wrap(content);
		// get moof size
		double moofSize = bbc.getInt();
		bbc.get(buf);
		String moofName = new String(buf);
		int timeSequence = 0;
		if (moofName.equals("moof")) {
			// skip past mfhd
			double mfhdSize = bbc.getInt();
			bbc.get(buf);
			String mfhdName = new String(buf);
			if (mfhdName.equals("mfhd")) {
				bbc.position((int) mfhdSize + bbc.position() - 8);
				// parse into traf
				// double trafSize =
				bbc.getInt(); // skip over
				bbc.get(buf);
				String trafName = new String(buf);
				if (trafName.equals("traf")) {
					// skip tfhd
					double tfhdSize = bbc.getInt();
					bbc.get(buf);
					String tfhdName = new String(buf);
					if (tfhdName.equals("tfhd")) {
						// skip past this atom
						bbc.position((int) tfhdSize + bbc.position() - 8);
					}
					// parse tfdt
					// double tfdtSize =
					bbc.getInt(); // skip over
					bbc.get(buf);
					String tfdtName = new String(buf);
					if (tfdtName.equals("tfdt")) {
						bbc.getInt(); // skip over always 16k
						bbc.getInt(); // skip over always 0
						timeSequence = bbc.getInt();
					}
				}
			}
		} else {
			return new Integer[] { 0, 0 };
		}
		// parse mdat
		bbc.position((int) moofSize);
		mdatSize = bbc.getInt();
		bbc.get(buf, 0, 4);
		String mdatName = new String(buf);
		if (mdatName.equals("mdat")) {
			mdatSize -= 8;
		} else {
			mdatSize = 0;
		}
		return new Integer[] { mdatSize, timeSequence };
	}

	/**
	 * Extract a Thumbnail image from the first frame of a video
	 *
	 * @param aroManifest
	 * @param content
	 *
	 * @param srcpath
	 * @param segmentName
	 * @param quality
	 * @param videoData
	 * @return
	 */
	private byte[] extractThumbnail(AROManifest aroManifest, byte[] content, VideoEventData ved) {
		
		String //segName = getDebugPath() + String.format("%08d", ved.getIntSegment()) + '_' + ved.getId() + '.' + ved.getExtension();
		segName = (new StringBuilder(getDebugPath()))
				.append(String.format("%08d", ved.getSegment()))
				.append('_')
				.append(ved.getId())
				.append('.')
				.append(ved.getExtension())
				.toString();
		
		byte[] data = null;
		filemanager.deleteFile(ved.getId());
		byte[] movie = null;
		if (aroManifest.isVideoType(VideoType.DASH)) {
			VideoData vData = aroManifest.getVData(ved.getQuality());
			if (vData == null) {
				return null;
			} // join mbox0 with segment
			byte[] mbox0 = vData.getContent();
			movie = new byte[mbox0.length + content.length];
			System.arraycopy(mbox0, 0, movie, 0, mbox0.length);
			System.arraycopy(content, 0, movie, mbox0.length, content.length);
		} else {
			movie = content;
		}
		try {
			filemanager.saveFile(new ByteArrayInputStream(movie), segName);
		} catch (IOException e1) {
			LOG.error("IOException:" + e1.getMessage());
		}
		data = extractVideoFrameShell(segName);
		if (data == null) {
			filemanager.deleteFile(segName);
		}
		return data;
	}

	private byte[] extractVideoFrameShell(String segmentName) {
		byte[] data = null;
		String thumbnail = getDebugPath() + "thumbnail.png";
		filemanager.deleteFile(thumbnail);
		String cmd = Util.getFFMPEG() + " -y -i " + "\"" + segmentName + "\"" + " -ss 00:00:00   -vframes 1 " + "\""
				+ thumbnail + "\"";
		String ff_lines = extrunner.executeCmd(cmd);
		LOG.debug("ff_lines :" + ff_lines);
		if (filemanager.fileExist(thumbnail)) {
			Path path = Paths.get(thumbnail);
			try {
				data = Files.readAllBytes(path);
			} catch (IOException e) {
				LOG.debug("getThumnail IOException:" + e.getMessage());
			}
		}
		return data;
	}

	/**
	 * <pre>
	 * get a bitrate where the raw data will have a value such as: 150 kb/s 150 mb/s
	 *
	 * @param key
	 * @param ff_lines
	 * @return
	 */
	private double getBitrate(String key, String ff_lines) {
		double bitrate = 0;
		String valbr = getValue(key, "\n", ff_lines);
		if (valbr != null) {
			String[] temp = valbr.split(" ");
			try {
				bitrate = Double.valueOf(temp[0]);
			} catch (NumberFormatException e) {
				LOG.debug("Bit rate not available for key:" + key);
				return 0;
			}
			if (temp[1].equals("kb/s")) {
				bitrate *= 1024;
			} else if (temp[1].equals("mb/s")) {
				bitrate *= 1048576;
			}
		}
		return bitrate;
	}

	/**
	 * Get the value following the key up to the delimiter. return null if not found
	 *
	 * @param key
	 * @param delimeter
	 * @param ff_lines
	 * @return value or null if no key found
	 */
	private String getValue(String key, String delimeter, String ff_lines) {
		String val = null;
		int pos1 = ff_lines.indexOf(key);
		if (pos1 > -1) {
			pos1 += key.length();
			int pos2 = ff_lines.substring(pos1).indexOf(delimeter);
			if (pos2 == -1 || pos2 == 0) {
				val = ff_lines.substring(pos1);
			} else {
				val = ff_lines.substring(pos1, pos1 + pos2);
			}
		}
		return val;
	}

	/**
	 * Obtain timestamp from request formated into a string. Primarily for debugging purposes.
	 *
	 * @param response
	 * @return
	 */
	private String getTimeString(HttpRequestResponseInfo response) {
		StringBuffer strTime = new StringBuffer();
		try {
			if (absTimeFlag) {
				Packet packet = response.getFirstDataPacket().getPacket(); // request
				strTime.append(String.format("%d.%06d"
						, packet.getSeconds()
						, packet.getMicroSeconds()
						));
			} else {
				strTime.append(String.format("%09.0f", (float) response.getTimeStamp() * 1000));
			}
		} catch (Exception e) {
			LOG.error("Failed to get time from request: " + e.getMessage());
			strTime.append("Failed to get time from response->request: " + response);
		}
		return strTime.toString();
	}

	/**
	 * <pre>
	 * Extract a HLS manifest from traffic data
	 *
	 * Types:
	 *  * movie
	 *  * livetv
	 *
	 * @param request
	 * @param session
	 *            that the response belongs to.
	 * @return AROManifest
	 */
	public AROManifest extractManifestHLS(HttpRequestResponseInfo request) {
		HttpRequestResponseInfo response = request.getAssocReqResp();
		VideoEventData ved = parseRequestResponse(request);
		LOG.info(String.format("extractManifestHLS %.4f", request.getTimeStamp()));
		if (ved.getName() == null) {
			ved.setName(regexNameFromRequestHLS(request));
		}

		byte[] content = null;
		try {
			content = extractContent(request);
			if (content == null || content.length == 0) {
				return aroManifest;
			}

			LOG.info("Manifest content.length :" + content.length);
			StringBuffer fname = new StringBuffer(getDebugPath());
			fname.append(getTimeString(response));
			fname.append('_');
			fname.append(ved.getName());
			fname.append("_ManifestHLS.m3u8");
			filemanager.saveFile(new ByteArrayInputStream(content), fname.toString());

		} catch (Exception e) {
			videoUsage.addFailedRequestMap(request);
			LOG.error("Failed to get content from DTV Manifest; response: " + e.getMessage());
		}

		String reqName = request.getObjNameWithoutParams();
		// return if identical content, don't parse it again
		if (aroManifest != null && !aroManifest.checkContent(content)) {
			return aroManifest;
		}

		for (AROManifest manifest : videoUsage.getManifests()) {
			if (!manifest.checkContent(content)) {
				return manifest;
			}
		}
		
		LOG.info(reqName);
		String sanityName = reqName.replaceAll("\\.", "_");
		if (aroManifest == null
				|| (!"playlist".equals(ved.getName()) && !ved.getName().equals(aroManifest.getVideoName()))
				|| (!sanityName.contains(aroManifest.getVideoName()) && !ved.getName().equals(aroManifest.getVideoName()))
				|| (reqName.contains("channel") && !sanityName.contains(aroManifest.getVideoName()))
				|| (reqName.contains("livetv") && reqName.contains("latest"))
				|| (reqName.contains("NFL") && reqName.contains("latest")) // /NFL/10/000573/03/latest.m3u8
				|| (reqName.contains("NFL") && !reqName.contains("_") && !reqName.contains("playlist.m3u8")) // /NFL/10/000573/03/playlist.m3u8
		) {
			// NFL - to this point
			ManifestHLS manifest = null;
			try {
				manifest = new ManifestHLS(response, ved, content, videoPath);
				// manifest.setDelay(videoUsagePrefs.getStartupDelay());
				LOG.info("aroManifest :" + aroManifest);

				videoUsage.add(request.getTimeStamp(), manifest);
				return manifest;

			} catch (Exception e) {
				LOG.error("Failed to parse manifest data, absTimeStamp:" + request.getAbsTimeStamp().getTime() + ", Name:" + reqName);
			}

		} else {
			try {
				((ManifestHLS) aroManifest).parseManifestData(content);
				LOG.info("aroManifest parsed in data:" + aroManifest);
			} catch (Exception e) {
				LOG.error("Failed to parse manifest data into ManifestHLS:" + e.getMessage());
			}
		}
		return aroManifest;
	}

	/**
	 * @param request
	 * @param response
	 * @param session
	 * @return
	 * @throws Exception
	 */
	private byte[] extractContent(HttpRequestResponseInfo request) throws Exception {
		byte[] content;
		try {
			content = reqhelper.getContent(request.getAssocReqResp(), request.getSession());
		} catch (Exception e) {
			content = null;
		}
		if (content == null || content.length == 0) {
			videoUsage.addFailedRequestMap(request);
		} // else {
		videoUsage.addRequest(request);
		// }
		return content;
	}

	private String regexNameFromRequestHLS(HttpRequestResponseInfo request) {
		String videoName = null;
		
		// TODO future create regex files for manifest matching like with video segments
		String regex[] = {
				  "\\/NFL\\/\\d+\\/(\\d+)\\/latest\\.m3u8"                      		//  /NFL/10/000573/latest.m3u8
				, "livetv\\/\\d+\\/([a-zA-Z0-9]*)\\/latest\\.m3u8"                      //  /livetv/30/8249/latest.m3u8
				, "livetv\\/\\d+\\/([a-zA-Z0-9]*)\\/\\d+\\/playlist\\.m3u8"             //  /livetv/30/8249/03/playlist.m3u8
				, "\\/aav\\/.+\\/([A|B]\\d+U)\\d\\.m3u8"                                //  /aav/30/B001573958U3/B001573958U3.m3u8
				, "\\/aav\\/.+\\/HLS\\d\\/([A|B]\\d+U)\\d\\_\\d.m3u8"                   //  /aav/30/B001844891U3/HLS2/B001844891U0_2.m3u8
				, "\\/aav\\/.+\\/WebVTT\\d\\/([A|B]\\d+U)\\d\\_\\d.m3u8"				//  /aav/30/B001844891U3/WebVTT1/B001844891U0_7.m3u8
				, "\\/movie\\/.+\\/([A|B]\\d+U)\\d\\.m3u8"                              //  /c3/30/movie/2016_12/B002021484/B002021484U3/B002021484U3.m3u8
				, "\\/channel\\((.+)\\)\\/\\d+.m3u8"	                  				//  /Content/HLS_hls.pr/Live/channel(FNCHD.gmott.1080.mobile)/05.m3u8
				, "\\/channel\\((.+)\\)\\/index.m3u8"	                  				//  /Content/HLS_hls.pr/Live/channel(FNCHD.gmott.1080.mobile)/index.m3u8
				, "AEG.+\\/(AEG).+index.m3u8"	                  				//  /seg/vol2/s/AEG_CP/dyjr1008967500000002/2017-03-24-13-11-31/DYJR1008967500000003/AEG_01_128/AEG_01_128-index.m3u8
				, "AEG.+\\/(AUD).+index.m3u8"	                  				//  /seg/vol2/s/AEG_CP/dyjr1008967500000002/2017-03-24-13-11-31/DYJR1008967500000003/AUD_01_128/AEG_01_128-index.m3u8
				, "\\/([a-zA-Z0-9]*).m3u8"                             					//  /B002021484U3.m3u8
		};

		String[] results = null;
		String tempName = request.getObjNameWithoutParams();
		for (int i = 0; i < regex.length; i++) {
			results = stringParse.parse(tempName, regex[i]);
			if (results != null) {
				videoName = results[0].replaceAll("\\.", "_");
				return videoName;
			}
		}
		if (results == null) {
			videoName = "unknown";
		}
		return videoName;
	}

	private boolean updateManifestCheck(String lastVedName, String lastReqObjName, VideoEventData ved,
			String reqObjName) {
		boolean success = false;
		if (lastVedName != null && lastReqObjName != null
				&& (lastVedName.equals(ved.getName()) && lastReqObjName.equals(reqObjName))) {
			success = true;
		}
		return success;
	}
	
	/**
	 * Extract a DASH manifest from traffic data
	 *
	 * @param request
	 * @param session
	 *            session that the response belongs to.
	 * @return AROManifest
	 * @throws java.lang.Exception
	 */
	public AROManifest extractManifestDash(HttpRequestResponseInfo request) {
		HttpRequestResponseInfo response = request.getAssocReqResp();

		VideoEventData ved = parseRequestResponse(request);
		LOG.info(String.format("extractManifestDash %.4f", request.getTimeStamp()));
		byte[] content = null;
		try {
			content = extractContent(request);
			if (content == null || content.length == 0) {
				return aroManifest;
			}
			videoUsage.addRequest(request);
			LOG.info("Manifest content.length :" + content.length);

		} catch (Exception e) {
			videoUsage.addFailedRequestMap(request);
			LOG.error("Failed to parse manifest data, absTimeStamp:" + request.getAbsTimeStamp().getTime() + ", Name:" + request.getObjNameWithoutParams());
			return null;
		}
		XmlManifestHelper mani = new XmlManifestHelper(content);
		ved.setManifestType(mani.getManifestType().toString());
		// debug - save to debug folder
		saveManifestFile(request, ved, content);
		AROManifest manifest = null;
		if (mani.getManifestType().equals(XmlManifestHelper.ManifestFormat.MPD_PlayReady)) {
			boolean success = updateManifestCheck(lastVedName, lastReqObjName, ved, request.getObjName());
			if (aroManifest != null && ((lastContentType != null && lastContentType.equals(ved.getContentType())) || ved.getName().equals(aroManifest.getVideoName()))
					|| success) {
				aroManifest.updateManifest(mani.getManifest());
				LOG.info("created new manifest :" + manifest);
				return aroManifest;
			} else {
				lastContentType = ved.getContentType();
				lastVedName = ved.getName();
				lastReqObjName = request.getObjName();
				manifest = new ManifestDashPlayReady((MPDPlayReady) mani.getManifest(), response, videoPath, ved);
			}
		} else if (mani.getManifestType().equals(XmlManifestHelper.ManifestFormat.SmoothStreamingMedia)) {
			manifest = new ManifestSSM((SSMAmz) mani.getManifest(), response, videoPath);
		} else {
			if (aroManifest != null && aroManifest.getVideoName().equals(ved.getName())) {
				LOG.info("Deactivate :" + aroManifest);
				aroManifest.setActiveState(false);
			}
			manifest = new ManifestDash((MPDAmz) mani.getManifest(), response, videoPath);
			LOG.info("created new manifest :" + manifest);
		}

		if (manifest != null) {
			videoUsage.add(request.getTimeStamp(), manifest);
		}

		return manifest;
	}

	/**
	 * Saves byte[] to a file
	 *
	 * @param request
	 * @param ved
	 * @param content
	 * @throws IOException
	 */
	private void saveManifestFile(HttpRequestResponseInfo request, VideoEventData ved, byte[] content) {// throws IOException {
		StringBuffer fname = new StringBuffer(getDebugPath());
		String temp = ved.getManifestType() != null ? ved.getManifestType() : extractNameFromRRInfo(request);
		fname.append(temp.equals("manifest") ? "_SSM_manifest" : temp);
		fname.append("__");
		fname.append(getTimeString(request.getAssocReqResp()));
		if (request.getObjNameWithoutParams().endsWith("manifest")) {
			fname.append("_SSMedia.xml");
		} else {
			fname.append("_ManifestDash.mpd");
		}
		String path = fname.toString();
		try {
			filemanager.saveFile(new ByteArrayInputStream(content), path);
		} catch (IOException e) {
			LOG.error("Failed to save Manifest :" + path + ", error :" + e.getMessage());
		}
	}


	private byte[] fileToByteArray(File file) {
		byte[] content = new byte[(int) file.length()];
		FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(content);
		} catch (IOException e) {
			LOG.error("File to byte array conversion exception" + e.getMessage());
		}
		return content;
	}

	public void extractImage(HttpRequestResponseInfo request, AROManifest aroManifest, String imageFileName) {
		HttpRequestResponseInfo response = request.getAssocReqResp();
		Session session = request.getSession();
		if (response != null) {
			byte[] content = null;
			String fullpath;
			try {
				content = reqhelper.getContent(response, session);
				fullpath = getImagePath() + imageFileName;
				filemanager.saveFile(new ByteArrayInputStream(content), fullpath);
			} catch (Exception e) {
				videoUsage.addFailedRequestMap(request);
				LOG.info("Failed to extract " + getTimeString(response) + imageFileName + " response: " + e.getMessage());
				return;
			}
		}
	}

	@Override
	public VideoUsage getVideoUsage() {
		return videoUsage;
	}

	@Override
	public TreeMap<Long, HttpRequestResponseInfo> getReqMap() {
		return reqMap;
	}

	/**
	 * Iterate videoUsage, flagging manifest as invalid if no segments or segment number is less than 0
	 * @param videoUsage
	 * @return
	 */
	public VideoUsage identifyInvalidManifest(VideoUsage videoUsage) {
		if (videoUsage != null) {
			for (AROManifest manifest : videoUsage.getManifests()) {
				if (manifest != null) {
					boolean selected = manifest.getVideoEventsBySegment() != null ? true : false;
					if (!selected) {
						manifest.setSelected(false);
						continue;
					}
					if (manifest.getVideoEventsBySegment().isEmpty()
							|| ((VideoEvent) manifest.getVideoEventsBySegment().toArray()[0]).getSegment() < 0) {
						manifest.setSelected(false);
					}
				}
			}
		}
		return videoUsage;
	}
}// end class
