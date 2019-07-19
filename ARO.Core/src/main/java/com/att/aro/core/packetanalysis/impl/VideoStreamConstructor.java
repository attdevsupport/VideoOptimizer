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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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
import lombok.Getter;
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
	@Getter private double failed;
	@Getter private double succeeded;
	private SegmentInfo segmentInfo;
	private ChildManifest childManifest;
	private byte[] defaultThumbnail = null;
	
	public VideoStreamConstructor() {
		
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
		extension = (pos == -1 ? null: objName.substring(pos));
		return extension;
	}

	public String extractFullNameFromRRInfo(HttpRequestResponseInfo req) {
		String objName = req.getObjNameWithoutParams();
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
		
		LOG.info("Segment request:" + request.getFileName() +" : "+ request.getTimeStamp()+" : "+request.getObjUri());
		LOG.info("Segment request:" + request.getFileName() +" : "+ request.getTimeStamp()+" : "+request.getObjUri());
		
		this.streamingVideoData = streamingVideoData;

		ManifestCollection manifestCollection;
		if (manifestBuilder == null || (manifestCollection = manifestBuilder.findManifest(request)) == null) {
			LOG.error("manifestCollection is null :" + request.getObjUri());
			return;
		}
		
		childManifest = locateChildManifestAndSegmentInfo(request, timeStamp, manifestCollection);
		
		if  (childManifest == null) {
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
		
		if (!segmentInfo.isVideo()) {
			LOG.info("\"Video\" segment type not being processed or recognized YET (Coming soon):"+request.getObjNameWithoutParams());
			return;
		}
		if (segmentInfo.getQuality().equals("0") && manifest.isVideoTypeFamily(VideoType.DASH)) {
			LOG.error("Wrong? determine what happened and if it is a problem :"+request.getObjNameWithoutParams());
			return;
		}
		
		String name = manifestBuilder.buildSegmentName(request, extractExtensionFromRequest(request));
		
		int segmentID = segmentInfo.getSegmentID();

		savePayload(content, buildPath(streamingVideoData, request, segmentInfo.getQuality() + "_" + name));
		
		crc32.update(content);

		manifest.setVideoName(manifestBuilder.formatKey(manifest.getVideoName()));
		byte[] thumbnail = null;
		
		if (segmentInfo.getSegmentID() == 0 && childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
			childManifest.setMoovContent(content);
		} else {
			thumbnail = extractThumbnail(childManifest, content, buildSegmentFullPathName(streamingVideoData, request));
			if (thumbnail != null) {
				collectFromFFMPEG(streamingVideoData, childManifest, manifest);
			} else if (childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
				Integer[] segmentData = parsePayload(content);
				if (segmentData != null) {
					segmentInfo.setSize(segmentData[0]);
					segmentInfo.setStartTime(segmentData[1]);
				}
			}
		}

		if (segmentInfo.getBitrate()==0) {
			segmentInfo.setBitrate(childManifest.getBandwidth());
		}
		if (thumbnail == null) {
			thumbnail = getDefaultThumbnail();
		}
		
		VideoEvent videoEvent = new VideoEvent(
				 thumbnail                               // imageArray            
				, manifest						          // aroManifest             
				, segmentInfo				              // segmentID, quality, duration   
				, childManifest					          // PixelHeight
				, content.length                          // segmentSize              
				, request.getAssocReqResp()               // response              
				, crc32.getValue());                      // crc32Value   
		
		videoStream.addVideoEvent(segmentID, request.getTimeStamp(), videoEvent);

	}
	
	public void collectFromFFMPEG(StreamingVideoData streamingVideoData, ChildManifest childManifest, Manifest manifest) {
		HashMap<String, Double> metaData = null;
		metaData = extractMetadata(streamingVideoData.getVideoPath());
		if (metaData != null) {
			manifest.setVideoMetaDataExtracted(true);
			Double val = metaData.get("bitrate");
			if (val != null && val > 0) {
				segmentInfo.setBitrate(val);
			} else {
				segmentInfo.setBitrate(childManifest.getBandwidth());
			}
			val = metaData.get("Duration");
			if (val != null && val > 0) {
				segmentInfo.setDuration(val);
			}
			val = metaData.get("SegmentStart");
			if (val != null && val > 0) {
				segmentInfo.setStartTime(val);
			}
			if (childManifest.getPixelHeight() == 0) {
				val = metaData.get("Resolution");
				if (val != null && val > 0) {
					childManifest.setPixelHeight(metaData.get("Resolution").intValue());
				}
			}
		}
	}

	public String buildSegmentFullPathName(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request) {
		String fileName = request.getFileName();
		int pos = fileName.lastIndexOf("/");
		if (pos > -1) {
			fileName = fileName.substring(pos + 1);
		}
		String segName = String.format("%sclip_%s_%08d_%s", streamingVideoData.getVideoPath(), getTimeString(request), segmentInfo.getSegmentID(), fileName);
		return segName;
	}

	public ChildManifest locateChildManifestAndSegmentInfo(HttpRequestResponseInfo request, Double timeStamp, ManifestCollection manifestCollection) {
		String key = manifestBuilder.buildKey(request);
		if ((childManifest = manifestCollection.getChildManifest(key)) != null) {
			String brKey = buildByteRangeKey(request);
			if ((segmentInfo = childManifest.getSegmentList().get(brKey.toLowerCase())) == null) {
				segmentInfo = childManifest.getSegmentList().get(brKey.toUpperCase());
			}
		} else if ((childManifest == null || segmentInfo == null) && timeStamp != null) {
			childManifest = manifestCollection.getTimestampChildManifestMap().get(timeStamp);
			if (childManifest == null || !childManifest.getSegmentList().keySet().parallelStream()
					.filter(segmentUriName -> {return request.getObjUri().toString().contains(segmentUriName);})
					.findFirst().isPresent()) {
				childManifest = manifestCollection
						.getUriNameChildMap().entrySet().stream()
						.filter(x -> {
							LOG.info("Child Manifest Key: " + x.getKey() + "\t Request Key in Use: " + request.getObjNameWithoutParams());
							return x.getValue().getSegmentList().keySet().parallelStream()
									.filter(segmentUriName -> {
										return request.getObjUri().toString().contains(segmentUriName);
									}).findFirst().isPresent();
						}).findFirst().map(x -> x.getValue()).orElseGet(() -> null);
			}

			if  (childManifest == null) {
				LOG.info("ChildManifest wasn't found for segment request:" + request.getObjUri());
				segmentInfo = null;
				return null;
			}

			segmentInfo = null;
			if ((segmentInfo = childManifest.getSegmentList().get(key)) == null) {
				childManifest.getSegmentList().entrySet().stream().filter(segmentInfoEntry -> segmentInfoEntry.getKey().contains(key)).findFirst()
				.map(segmentInfoEntry -> segmentInfo = segmentInfoEntry.getValue()).orElseGet(() -> segmentInfo = null);
			}
		}
		return childManifest;
	}

	public String buildByteRangeKey(HttpRequestResponseInfo request) {
		String key = null;
		String[] range = stringParse.parse(request.getAllHeaders().toString(), "Range: bytes=(\\d+)-(\\d+)");
		if (range != null) {
			try {
				key = String.format("%1$016X-%2$016X",Integer.valueOf(range[0]), Integer.valueOf(range[1]));
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
	 * Types:
	 *  * movie
	 *  * livetv
	 * @param streamingVideoData 
	 *
	 * @param request
	 * @param session
	 *            that the response belongs to.
	 * @return Manifest
	 */
	public Manifest extractManifestHLS(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request) {
		manifestBuilder = manifestBuilderHLS;
		LOG.info("\nHLS request:\n" + request.getObjUri());
		this.streamingVideoData = streamingVideoData;

		byte[] content = extractContent(request);
		if (content == null) {
			return null;
		}
		
		Manifest manifest = manifestBuilderHLS.create(request, content, "blank");
		savePayload(content, buildPath(streamingVideoData, request, manifest.getVideoName()));
		
		return manifest;
	}

	public String buildPath(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request, String fileName) {
		if (fileName == null) {
			return "";
		}
		try {
			String name = fileName;
			name = fileName.replaceAll("://", "_").replaceAll("/", "_");
			return String.format("%s%s_%s", streamingVideoData.getVideoPath(), getTimeString(request), name);
		} catch (Exception e) {
			LOG.error("Failed to build path for fileName" + fileName, e);
			return "";
		}
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
		filemanager.deleteFile(segName);
		return data;
	}

	/**<PRE>
	 * DASH-MP4
	 * Creates a HashMap with keys:
	 *    bitrate
	 *    Duration
	 *    SegmentStart
	 *
	 * @param srcpath
	 * @return
	 */
	private HashMap<String, Double> extractMetadata(String srcpath) {
		HashMap<String, Double> results = new HashMap<>();
		String cmd = Util.getFFMPEG() + " -i " + "\"" + srcpath + "\"";
		String lines = extrunner.executeCmd(cmd);
		if (lines.indexOf("No such file") == -1) {
			double bitrate = getBitrate("bitrate: ", lines);
			results.put("bitrate", bitrate);
			Double duration = 0D;
			double resolution = parseResolution(lines);
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
	private Integer[] parsePayload(byte[] content) {
		byte[] buf = new byte[4];
		int mdatSize = 0;
		ByteBuffer bbc = ByteBuffer.wrap(content);
		// get moof size
		double moofSize = 0;
		try {
			moofSize = bbc.getInt();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			return null;
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
	
	private boolean savePayload(byte[] content, String pathName) {
		if (content != null && content.length > 0) {
			try {
				filemanager.saveFile(new ByteArrayInputStream(content), pathName);
				LOG.info(">>>> SAVE payload :" + pathName);
				return true;
			} catch (IOException e) {
				LOG.error("Failed to save " + pathName, e);
			}
		}
		return false;
	}

	private byte[] extractContent(HttpRequestResponseInfo request) {
		byte[] content = null;
		if (request.getAssocReqResp().getContentLength() == 0) {
			LOG.error("no AssocReqResp()  :" + request.getObjUri());
		} else {
			try {
				content = reqhelper.getContent(request.getAssocReqResp(), request.getSession());
				if (content.length == 0) {
					content = null;
				}
			} catch (Exception e) {
				LOG.error("Download FAILED :" + request.getObjUri(), e);
				content = null;
			}
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
	 * @param streamingVideoData 
	 *
	 * @param request
	 * @param session
	 *            session that the response belongs to.
	 * @return Manifest
	 * @throws java.lang.Exception
	 */
	public Manifest extractManifestDash(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request) {
		manifestBuilder = manifestBuilderDASH;
		LOG.info("extractManifestDash :" + request);
		LOG.info("\nDASH request:\\n" + request.getObjUri());
		this.streamingVideoData = streamingVideoData;

		byte[] content = extractContent(request);
		if (content == null) {
			return null;
		}
		
		Manifest manifest = manifestBuilderDASH.create(request, content, "blank");
		savePayload(content, buildPath(streamingVideoData, request, manifest.getVideoName()));
		
		return manifest;
	}

	/**
	 * <pre>
	 * Loads a replacement for missing thumbnails, blocked by DRM. The default
	 * is the VO App icon image User defined replacement needs to be a PNG image
	 * and should be in the VideoOptimizerLibrary and named broken_thumbnail.png
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
		manifestBuilder = null;
	}

}
