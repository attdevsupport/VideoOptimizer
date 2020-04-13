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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoAnalysisConfigHelper;
import com.att.aro.core.videoanalysis.IVideoEventDataHelper;
import com.att.aro.core.videoanalysis.impl.ManifestBuilder;
import com.att.aro.core.videoanalysis.impl.ManifestBuilderDASH;
import com.att.aro.core.videoanalysis.impl.ManifestBuilderHLS;
import com.att.aro.core.videoanalysis.impl.SegmentInfo;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;

import lombok.Data;
import lombok.NonNull;

@Data
public class VideoStreamConstructor {

	private static final Logger LOG = LogManager.getLogger(VideoStreamConstructor.class.getName());

	@Autowired
	private IFileManager filemanager;
	@Autowired
	private IExternalProcessRunner extrunner;
	@Autowired
	private IVideoAnalysisConfigHelper voConfigHelper;
	@Autowired
	private IVideoEventDataHelper voEventDataHelper;
	@Autowired
	private IHttpRequestResponseHelper reqhelper;
	@Autowired
	private IStringParse stringParse;

	private boolean absTimeFlag = false;
	private VideoAnalysisConfig vConfig;

	@Value("${ga.request.timing.videoAnalysisTimings.title}")
	private String videoAnalysisTitle;
	@Value("${ga.request.timing.analysisCategory.title}")
	private String analysisCategory;

	@NonNull
	private StreamingVideoData streamingVideoData;
	private VideoStream videoStream;

	Pattern extensionPattern = Pattern.compile("(\\b[a-zA-Z0-9\\-_\\.]*\\b)(\\.[a-zA-Z0-9]*\\b)");
	private ManifestBuilder manifestBuilderHLS = new ManifestBuilderHLS();
	private ManifestBuilder manifestBuilderDASH = new ManifestBuilderDASH();
	private ManifestBuilder manifestBuilder;

	private double failed;
	private double succeeded;
	
	private SegmentInfo segmentInfo;
	private ChildManifest childManifest;
	private byte[] defaultThumbnail = null;

	public VideoStreamConstructor() {
		init();
	}

	public String extractExtensionFromRequest(HttpRequestResponseInfo req) {
		String string = extractFullNameFromRRInfo(req);
		int dot = string.lastIndexOf(".");
		if (dot > 0) {
			return string.substring(dot);
		}
		return string;
	}

	public String extractNameFromRRInfo(HttpRequestResponseInfo req) {
		String objName = req.getObjNameWithoutParams();
		String[] matched = stringParse.parse(objName, extensionPattern);
		if (matched != null && matched.length == 2 && !matched[0].isEmpty()) {
			return matched[0];
		}
		String extension = null;
		int pos = objName.lastIndexOf('.');
		extension = (pos == -1 ? null : objName.substring(pos));
		return extension;
	}

	public String extractFullNameFromRRInfo(HttpRequestResponseInfo req) {
		String objName = StringUtils.substringBefore(req.getObjNameWithoutParams(), ";");
		if (objName.contains(".")) {
			String part[] = objName.split("/");
			for (int idx = part.length - 1; idx > 0; idx--) {
				if (part[idx].contains(".")) {
					return part[idx];
				}
			}
		}
		return objName;
	}

	public void extractVideo(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request, Double timeStamp) {
		CRC32 crc32 = new CRC32();
		ChildManifest childManifest = null;

		byte[] content = extractContent(request);
		if (content == null) {
			return;
		}
		
		LOG.debug(String.format("Segment request, packetID: %d, len:%d, name:%s, TS:%.3f, URL:%s"
					, request.getAssocReqResp().getFirstDataPacket().getPacketId(), content.length, request.getFileName(), request.getTimeStamp(), request.getObjUri()));
		
		this.streamingVideoData = streamingVideoData;

		ManifestCollection manifestCollection;
		if (manifestBuilder == null || (manifestCollection = manifestBuilder.findManifest(request)) == null) {
			LOG.error("manifestCollection is null :" + request.getObjUri());
			return;
		}

		childManifest = locateChildManifestAndSegmentInfo(request, timeStamp, manifestCollection);

		if (childManifest == null) {
			LOG.error("ChildManifest wasn't found for segment request:" + request.getObjUri());
			return;
		}

		Manifest manifest = manifestCollection.getManifest();

		if ((videoStream = streamingVideoData.getVideoStream(manifestCollection.getManifest().getRequestTime())) == null) {
			videoStream = new VideoStream();
			videoStream.setManifest(manifestCollection.getManifest());
			streamingVideoData.addVideoStream(manifestCollection.getManifest().getRequestTime(), videoStream);
		}

		if (segmentInfo == null) {
			LOG.debug("segmentInfo is null :" + request.getObjUri());
			return;
		}

		if (segmentInfo.getQuality().equals("0") && manifest.isVideoTypeFamily(VideoType.DASH)) {
			LOG.error("Wrong? determine what happened and if it is a problem :" + request.getObjNameWithoutParams());
			return;
		}

		String name = manifestBuilder.buildSegmentName(request, extractExtensionFromRequest(request));

		crc32.update(content);

		manifest.setVideoName(manifestBuilder.formatKey(manifest.getVideoName()));
		byte[] thumbnail = null;
		
		if (segmentInfo.getSegmentID() == 0 && childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
			childManifest.setMoovContent(content);
			segmentInfo.setDuration(0);
		} else {
			HashMap<String, Integer> atomData = null;
			String tempClippingFullPath = buildSegmentFullPathName(streamingVideoData, request);
			atomData = parsePayload(content);
			thumbnail = extractThumbnail(childManifest, content, tempClippingFullPath);
			if (thumbnail != null || (!segmentInfo.isVideo() && childManifest.getMoovContent() != null)) {
				collectFromFFMPEG(tempClippingFullPath, childManifest, manifest);
			} else if (childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
				if (!CollectionUtils.isEmpty(atomData)) {
					segmentInfo.setSize(atomData.get("mdatSize"));
				}
			}
			if (!childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
				filemanager.deleteFile(tempClippingFullPath);
			}
		}

		if (thumbnail == null) {
			thumbnail = getDefaultThumbnail();
		}
		
		if (segmentInfo.getBitrate() == 0 && segmentInfo.getDuration() > 0) {
			segmentInfo.setBitrate(content.length/segmentInfo.getDuration());
		}

		VideoEvent videoEvent = new VideoEvent(thumbnail // imageArray
				, manifest								// Manifest
				, segmentInfo							// segmentID, quality, duration
				, childManifest							// PixelHeight
				, content.length						// segmentSize
				, request.getAssocReqResp()				// response
				, crc32.getValue());					// crc32Value

		videoStream.addVideoEvent(videoEvent);

		String fullPathName = buildPath(streamingVideoData, request, segmentInfo.getSegmentID(), segmentInfo.getQuality(), name);
		int pos1 = fullPathName.lastIndexOf(Util.FILE_SEPARATOR) + 1;
		int pos2 = fullPathName.substring(pos1).indexOf('_');
		fullPathName = String.format("%s%09.0f%s", fullPathName.substring(0, pos1), videoEvent.getEndTS() * 1000, fullPathName.substring(pos1 + pos2));
		savePayload(content, fullPathName);
	}

	public String buildSegmentFullPathName(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request) {
		String fileName = request.getFileName();
		int pos = fileName.lastIndexOf("/");
		if (pos > -1) {
			fileName = fileName.substring(pos + 1);
		}
		String segName = String.format("%sclip_%s_%08d_%s_%s", streamingVideoData.getVideoPath(), getTimeString(request), segmentInfo.getSegmentID(), segmentInfo.getQuality(), fileName);
		return segName;
	}

	public String buildPath(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request, int segmentID, String segmentQuality, String fileName) {
		if (fileName == null) {
			return "";
		}
		try {
			String name = fileName;
			name = fileName.replaceAll("://", "_").replaceAll("/", "_");
			return String.format("%s%s_%08d_%s_%s", streamingVideoData.getVideoPath(), getTimeString(request), segmentID, segmentQuality, name);
		} catch (Exception e) {
			LOG.error("Failed to build path for fileName" + fileName, e);
			return "";
		}
	}

	public void collectFromFFMPEG(String fullPath, ChildManifest childManifest, Manifest manifest) {
		if (childManifest.getPixelHeight() > 0) {
			return;
		}
		Double val = 0D;
		HashMap<String, Double> metaData = null;
		metaData = extractMetadata(fullPath);
		if (metaData != null) {
			manifest.setVideoMetaDataExtracted(true);
			if (segmentInfo.getStartTime() < 0) {
				val = metaData.get("SegmentStart");
				if (val != null && val > 0) {
					segmentInfo.setStartTime(val);
				}
			}
			if (childManifest.getPixelHeight() == 0) {
				val = metaData.get("Resolution");
				if (val != null && val > 0) {
					childManifest.setPixelHeight(metaData.get("Resolution").intValue());
				}
			}
		}
	}

	/**
	 * Locate ChildManifest using keys based on multiple storage strategies, depending on Manifest & Manifest usage
	 * 
	 * @param request
	 * @param timeStamp
	 * @param manifestCollection
	 * @return
	 */
	public ChildManifest locateChildManifestAndSegmentInfo(HttpRequestResponseInfo request, Double timeStamp, ManifestCollection manifestCollection) {
		childManifest = null;
		segmentInfo = null;

		LOG.debug("request :" + request);

		String key = manifestBuilder.locateKey(request);
		LOG.debug(key + "\nmanifestCollection.getUriNameChildMap() :" + manifestCollection.getUriNameChildMap().keySet());
		if ((childManifest = manifestCollection.getChildManifest(key)) != null) {
			String brKey = buildByteRangeKey(request);
			if (brKey != null) {
				if ((segmentInfo = childManifest.getSegmentList().get(brKey.toLowerCase())) == null) {
					segmentInfo = childManifest.getSegmentList().get(brKey.toUpperCase());
				}
			}
			if (segmentInfo == null) {
				segmentInfo = childManifest.getSegmentList().get(manifestBuilder.buildKey(request));
			}
			
		} else if (timeStamp != null) {
			childManifest = manifestCollection.getTimestampChildManifestMap().get(timeStamp);
			if (childManifest == null || !childManifest.getSegmentList().keySet().parallelStream().filter(segmentUriName -> {
				return request.getObjUri().toString().contains(segmentUriName);
			}).findFirst().isPresent()) {
				childManifest = manifestCollection.getUriNameChildMap().entrySet().stream()
						.filter(x -> {
							return x.getValue().getSegmentList().keySet().parallelStream().filter(segmentUriName -> {
								return request.getObjUri().toString().contains(segmentUriName);
							}).findFirst().isPresent();
						})
						.findFirst().map(x -> x.getValue()).orElseGet(() -> null);
			}

			if (childManifest == null) {
				LOG.debug("ChildManifest wasn't found for segment request:" + request.getObjUri());
				nullSegmentInfo();
				return null;
			}

			segmentInfo = null;
			if ((segmentInfo = childManifest.getSegmentList().get(key)) == null) {
				childManifest.getSegmentList().entrySet()
				.stream().filter(segmentInfoEntry -> segmentInfoEntry.getKey().contains(key))
				.findFirst()
				.map(segmentInfoEntry -> segmentInfo = segmentInfoEntry.getValue())
				.orElseGet(() -> nullSegmentInfo());
			}
		}
		return childManifest;
	}

	public SegmentInfo nullSegmentInfo() {
		if (segmentInfo != null) {
			LOG.debug("notnull:" + segmentInfo);
		}
		return segmentInfo = null;

	}

	public String buildByteRangeKey(HttpRequestResponseInfo request) {
		String key = null;
		String[] range = stringParse.parse(request.getAllHeaders().toString(), "Range: bytes=(\\d+)-(\\d+)");
		if (range != null) {
			try {
				key = String.format("%1$016X-%2$016X", Integer.valueOf(range[0]), Integer.valueOf(range[1]));
			} catch (NumberFormatException e) {
				LOG.error("Failed to create ByteRangeKey: ", e);
			}
		}
		return key;
	}

	/**
	 * <pre>
	 * Extract a HLS manifest from traffic data
	 *
	 * Types: movie, livetv
	 * 
	 * @param streamingVideoData
	 *
	 * @param request
	 * @param session
	 *                               that the response belongs to.
	 * @return Manifest
	 */
	public Manifest extractManifestHLS(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request) {
		manifestBuilder = manifestBuilderHLS;
		LOG.debug("\nHLS request:\n" + request.getObjUri());
		this.streamingVideoData = streamingVideoData;

		byte[] content = extractContent(request);
		if (content == null) {
			return null;
		}

		Manifest manifest = manifestBuilderHLS.create(request, content, "blank");
		savePayload(content, buildPath(streamingVideoData, request, -1, "m", manifest.getVideoName()));

		return manifest;
	}

	/**
	 * Extract a Thumbnail image from the first frame of a video
	 *
	 * @param manifest
	 * @param content
	 * @param request
	 *
	 * @param srcpath
	 * @param segmentName
	 * @param quality
	 * @param videoData
	 * @return
	 */
	private byte[] extractThumbnail(ChildManifest childManifest, byte[] content, String segName) {

		byte[] data = null;
		filemanager.deleteFile(segName);
		byte[] movie = null;
		if (childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
			byte[] mbox0 = childManifest.getMoovContent();
			if (mbox0 == null) {
				return null;
			}
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
		return data;
	}

	/**
	 * <PRE>
	 * DASH-MP4 
	 * Creates a HashMap with keys: 
	 *   bitrate
	 *   Duration
	 *   SegmentStart
	 *
	 * @param srcpath
	 * @return
	 */
	private HashMap<String, Double> extractMetadata(String srcpath) {
		HashMap<String, Double> results = new HashMap<>();
		String cmd = Util.getFFPROBE() 
				+ " -i " + "\"" + srcpath + "\""
				+ " -v quiet"
				+ " -show_format"
				+ " -of flat=s=_ -show_entries stream=height,width,nb_frames,duration,codec_name"
				;

		String lines = extrunner.executeCmd(cmd);
		if (lines.indexOf("No such file") == -1) {
			Double start = StringParse.findLabeledDoubleFromString("format_start_time=", "\"", lines);
			Double bitrate = StringParse.findLabeledDoubleFromString("format_bit_rate=", "\"", lines);
			Double duration = StringParse.findLabeledDoubleFromString("streams_stream_0_duration=", "\"", lines);
			Double resolution = StringParse.findLabeledDoubleFromString("streams_stream_0_height=", lines);
			
			results.put("bitrate", bitrate);
			results.put("SegmentStart", start);
			results.put("Duration", duration);
			results.put("Resolution", resolution);
		}
		return results;
	}

	/**
	 * Locate two numbers separated by an 'x'
	 * 
	 * @param lines
	 * @return the first of the located numbers or a 0 if no pattern found
	 */
	public double parseResolution(String lines) {
		double resolution = 0;
		String[] vDimensions = stringParse.parse(lines, " (\\d+)x(\\d+) ");
		if (vDimensions != null && vDimensions.length == 2) {
			resolution = Double.parseDouble(vDimensions[1]);
		}
		return resolution;
	}

	/**
	 * Parse mp4 chunk/segment that contains one moof and one mdat.
	 *
	 * @param content
	 * @return double[] mdat payload length, time sequence, or null if failed to parse
	 */
	private HashMap<String, Integer> parsePayload(byte[] content) {
		HashMap<String, Integer> results = new HashMap<>();
		byte[] buf = new byte[4];
		int mdatSize = 0;
		ByteBuffer bbc = ByteBuffer.wrap(content);
		// get moof size
		double moofSize = 0;
		try {
			moofSize = bbc.getInt();
		} catch (Exception e) {
			LOG.error("Failed to read moofSize:", e);
		}
		bbc.get(buf);
		String moofName = new String(buf);
		int timeSequence = 0;
		if (moofName.equals("moof")) {
			// skip past mfhd
			double mfhdSize = bbc.getInt();
			
			 //  'mfhd' - movie fragment header
			bbc.get(buf);
			String mfhdName = new String(buf);
			
			if (mfhdName.equals("mfhd")) {
				bbc.position((int) mfhdSize + bbc.position() - 8);
				// parse into traf
				bbc.getInt(); // skip over always 0
				bbc.get(buf);
				String trafName = new String(buf);
				
				if (trafName.equals("traf")) {
					// skip tfhd
					double tfhdSize = bbc.getInt();
					
					 //  'tfhd' - Track fragment decode time
					bbc.get(buf);
					String tfhdName = new String(buf);
					if (tfhdName.equals("tfhd")) {
						// skip past this atom
						bbc.position((int) tfhdSize + bbc.position() - 8);
					}
					// parse tfdt
					bbc.getInt(); // skip over always 0
					bbc.get(buf);
					String tfdtName = new String(buf);
					if (tfdtName.equals("tfdt")) {
						bbc.getInt(); // skip over always 0
						bbc.getInt(); // skip over always 0
						timeSequence = bbc.getInt();
					}
				}
			}
		} else {
			return results;
		}
		// parse mdat - media data container
		bbc.position((int) moofSize);
		mdatSize = bbc.getInt();
		bbc.get(buf, 0, 4);
		String mdatName = new String(buf);
		if (mdatName.equals("mdat")) {
			mdatSize -= 8;
		} else {
			mdatSize = 0;
		}
		results.put("mdatSize", mdatSize);
		results.put("timeSequence", timeSequence);
		return results;
	}

	private byte[] extractVideoFrameShell(String segmentName) {
		byte[] data = null;
		String thumbnail = streamingVideoData.getVideoPath() + "thumbnail.png";
		filemanager.deleteFile(thumbnail);
		String cmd = Util.getFFMPEG() + " -y -i " + "\"" + segmentName + "\"" + " -ss 00:00:00   -vframes 1 " + "\"" + thumbnail + "\"";
		String ff_lines = extrunner.executeCmd(cmd);
		LOG.debug("ff_lines :" + ff_lines);
		if (filemanager.fileExist(thumbnail)) {
			Path path = Paths.get(thumbnail);
			try {
				data = Files.readAllBytes(path);
				filemanager.deleteFile(thumbnail);
			} catch (IOException e) {
				LOG.debug("getThumbnail IOException:" + e.getMessage());
			}
		}
		return data;
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
				strTime.append(String.format("%d.%06d", packet.getSeconds(), packet.getMicroSeconds()));
			} else {
				strTime.append(String.format("%09.0f", (float) response.getTimeStamp() * 1000));
			}
		} catch (Exception e) {
			LOG.error("Failed to get time from request: " + e.getMessage());
			strTime.append("Failed to get time from response->request: " + response);
		}
		return strTime.toString();
	}
	
	public boolean savePayload(byte[] content, String pathName) {
		pathName = findPathNameTiebreaker(pathName);
		if (content != null && content.length > 0) {
			try {
				filemanager.saveFile(new ByteArrayInputStream(content), pathName);
				LOG.debug(">>>> SAVE payload :" + pathName);
				return true;
			} catch (IOException e) {
				LOG.error("Failed to save " + pathName, e);
			}
		}
		return false;
	}

	/**
	 * Rebuild a fully qualified pathname to contain a tiebreaker examples: filepath/filename.txt becomes filepath/filename(1).txt filepath/filename becomes
	 * filepath/filename(1)
	 * 
	 * attempts to resolve over 200 attempts, then resorts to logging an error
	 * 
	 * @param pathName
	 * @return pathName with embedded tiebreaker or when all else fails, appends (duplicated) to the pathName
	 */
	public String findPathNameTiebreaker(String pathName) {
		if (filemanager.fileExist(pathName)) {
			String temp = pathName;
			int pos = pathName.lastIndexOf(".");
			if (pos > 0) {
				for (int idx = 1; idx < 200; idx++) {
					temp = String.format("%s(%d)%s", pathName.substring(0, pos), idx, pathName.substring(pos));
					if (!filemanager.fileExist(temp)) {
						return temp;
					}
				}
			} else {
				for (int idx = 1; idx < 200; idx++) {
					temp = String.format("%s(%d)", pathName, idx);
					if (!filemanager.fileExist(temp)) {
						return temp;
					}
				}
			}
			LOG.error("duplicate file(s) found :" + pathName);
			return pathName + "(duplicated)";
		} else {
			return pathName;
		}

	}

	private byte[] extractContent(HttpRequestResponseInfo request) {
		byte[] content = null;
		try {
			content = reqhelper.getContent(request.getAssocReqResp(), request.getSession());
			if (content.length == 0) {
				content = null;
			}
		} catch (Exception e) {
			LOG.debug("Download FAILED :" + request.getObjUri(), e);
			content = null;
		}

		if (content == null) {
			addFailedRequest(request);
		} else {
			streamingVideoData.addRequest(request);
		}
		return content;
	}

	public void addFailedRequest(HttpRequestResponseInfo request) {
		streamingVideoData.addFailedRequestMap(request);
		this.failed++;
	}

	/**
	 * Extract a DASH manifest from traffic data
	 * 
	 * @param streamingVideoData
	 * @param request
	 * @return manifest
	 */
	public Manifest extractManifestDash(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request) {
		manifestBuilder = manifestBuilderDASH;
		this.streamingVideoData = streamingVideoData;
		byte[] content = extractContent(request);
		if (content == null) {
			return null;
		}

		Manifest manifest = manifestBuilderDASH.create(request, content, "blank");
		savePayload(content, buildPath(streamingVideoData, request, -1, "m", manifest.getVideoName()));

		return manifest;
	}

	/**
	 * <pre>
	 * Loads a replacement for missing thumbnails, blocked by DRM. 
	 * The default is the VO App icon image.
	 * 
	 * User defined replacement needs to be a PNG image and should
	 * be in the VideoOptimizerLibrary with the name "broken_thumbnail.png"
	 *
	 * @return byte[] of a png image
	 */
	private byte[] getDefaultThumbnail() {
		if (defaultThumbnail == null) {
			String brokenThumbnailPath = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + "broken_thumbnail.png";
			if (filemanager.fileExist(brokenThumbnailPath)) {
				try {
					Path path = Paths.get(brokenThumbnailPath);
					defaultThumbnail = Files.readAllBytes(path);
				} catch (IOException e) {
					LOG.debug("getThumbnail IOException:" + e.getMessage());
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
						defaultThumbnail = Files.readAllBytes(path);
					} catch (IOException e) {
						LOG.debug("getIconThumbnail IOException:" + e.getMessage());
					}
				}
			}
		}
		return defaultThumbnail;
	}

	public ManifestCollection getManifestCollectionMap() {
		return manifestBuilder.getManifestCollection();
	}

	public void init() {
		manifestBuilderHLS = new ManifestBuilderHLS();
		manifestBuilderDASH = new ManifestBuilderDASH();
		manifestBuilder = manifestBuilderDASH; 
	}

}
