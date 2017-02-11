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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
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
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.ManifestDTV;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;
import com.att.aro.core.videoanalysis.pojo.VideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.MPDAmz;

/**
 * Video usage analysis
 * 
 * @Barry
 */
public class VideoUsageAnalysisImpl implements IVideoUsageAnalysis {

	@Autowired
	private IFileManager filemanager;

	@InjectLogger
	private static ILogger log;

	private IExternalProcessRunner extrunner;

	@Autowired
	public void setExternalProcessRunner(IExternalProcessRunner runner) {
		this.extrunner = runner;
	}

	private IHttpRequestResponseHelper reqhelper;
	
	private String tracePath;
	
	private boolean imageExtractionRequired = false;

	private String debugpath;
	
	private String imagePath;
	
	private boolean absTimeFlag = false;

	private TreeMap<Double, AROManifest> aroManifestMap;

	private AROManifest aroManifest = null;

	private VideoUsage videoUsage;
	
	private final String fileVideoSegments = "video_segments";

	@Autowired
	public void setHttpRequestResponseHelper(IHttpRequestResponseHelper reqhelper) {
		this.reqhelper = reqhelper;
	}

	IPreferenceHandler prefs = PreferenceHandlerImpl.getInstance();

	private VideoUsagePrefs videoUsagePrefs;
	
	public static double round(double value, int places) {
		if (places < 0) {
			throw new IllegalArgumentException();
		}
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	/**
	 * Load VideoUsage Preferences
	 */
	private void loadPrefs() {
		ObjectMapper mapper = new ObjectMapper();

		String temp = prefs.getPref(VideoUsagePrefs.VIDEO_PREFERENCE);
		if (temp != null) {
			try {
				videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			} catch (IOException e) {
				log.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
			}
		} else {
			try {
				videoUsagePrefs = new VideoUsagePrefs();
				temp = mapper.writeValueAsString(videoUsagePrefs);
				prefs.setPref(VideoUsagePrefs.VIDEO_PREFERENCE, temp);
			} catch (IOException e) {
				log.error("VideoUsagePrefs failed to serialize :" + e.getMessage());
			}
		}
	}

	@Override
	public VideoUsage analyze(AbstractTraceResult result, List<Session> sessionlist) {

		loadPrefs();

		tracePath = result.getTraceDirectory() + Util.FILE_SEPARATOR;

		debugpath = tracePath + fileVideoSegments + Util.FILE_SEPARATOR;
		if (!filemanager.directoryExist(debugpath)) {
			filemanager.mkDir(debugpath);
		} else {
			filemanager.directoryDeleteInnerFiles(debugpath);
		}

		imagePath = tracePath + "Image" + Util.FILE_SEPARATOR;

		if (!filemanager.directoryExist(imagePath)) {
			imageExtractionRequired = true;
			filemanager.mkDir(imagePath);
		} else {
			//Not Required but needed if in case the method is called a second time after initialization
			imageExtractionRequired = false;
		}

		videoUsage = new VideoUsage(result.getTraceDirectory());
		videoUsage.setVideoUsagePrefs(videoUsagePrefs);

		aroManifestMap = videoUsage.getAroManifestMap();

		TreeMap<Double, HttpRequestResponseInfo> reqMap = collectReqByTS(sessionlist);
		String imgExtension = null;

		for (HttpRequestResponseInfo req : reqMap.values()) {
			if (req.getAssocReqResp() == null) {
				continue;
			}

			// is there an extension involved
			String fullName = extractFullNameFromRRInfo(req);
			String extn = extractExtensionFromName(fullName);
			if (extn == null) {
				continue;
			}

			String filename = null;

			switch (extn) {

			// DASH - Amazon, Hulu, Netflix, Fullscreen
			case ".mpd": // Dash Manifest
				imgExtension = ".mp4";
				aroManifest = extractManifestDash(req);
				break;

			case ".mp4": // Dash/MPEG
				fullName = extractFullNameFromRRInfo(req);
				if (fullName.contains("_audio")) {
					continue;
				}
				filename = truncateString(fullName, "_");

				extractVideo(req, aroManifest, filename);

				break;

			// DTV
			case ".m3u8": // HLS-DTV Manifest
				imgExtension = ".ts";
				aroManifest = extractManifestDTV(req);
				break;

			case ".ts": // HLS video
				filename = "video" + imgExtension;
				extractVideo(req, aroManifest, filename);
				break;

			case ".vtt": // closed caption
				filename = "videoTT.vtt";
				// extractVideo(req, aroManifest, filename;
				break;

			// Images
			case ".jpg":
			case ".gif":
			case ".tif":
			case ".png":
			case ".jpeg":
				
				if (imageExtractionRequired) {
					extractImage(req, aroManifest, fullName);
				}
				break;

			case ".css":
			case ".json":
			case ".html":
				continue;

			default:// items found here may need to be handled OR ignored as the above
				log.debug("failed with extention :" + extn);
				break;
			}

			if (filename != null) {
				log.debug("filename :" + filename);
			}

		}
		if (!filemanager.fileExist(Util.getAroLibrary() + Util.FILE_SEPARATOR + fileVideoSegments)) {
			filemanager.directoryDeleteInnerFiles(getDebugPath());
			filemanager.deleteFile(getDebugPath());
		}
		log.debug(videoUsage.toString());
		return videoUsage;
	}

	/**
	 * Create a TreeMap of all pertinent Requests keyed by timestamp
	 * 
	 * @param sessionlist
	 * @return Map of Requests
	 */
	private TreeMap<Double, HttpRequestResponseInfo> collectReqByTS(List<Session> sessionlist) {
		TreeMap<Double, HttpRequestResponseInfo> reqMap = new TreeMap<>();
		for (Session session : sessionlist) {
			List<HttpRequestResponseInfo> rri = session.getRequestResponseInfo();
			for (HttpRequestResponseInfo rrInfo : rri) {
				if (rrInfo.getDirection().equals(HttpDirection.REQUEST) 
						&& rrInfo.getRequestType() != null 
						&& rrInfo.getRequestType().equals(HttpRequestResponseInfo.HTTP_GET)
						&& rrInfo.getObjNameWithoutParams().contains(".")) {
					rrInfo.setSession(session);
					reqMap.put(rrInfo.getTimeStamp(), rrInfo);
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

	private String getDebugPath() {
		return debugpath;
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
	
	private String extractNameFromRRInfo(HttpRequestResponseInfo rrInfo) {
		String fullName = extractFullNameFromRRInfo(rrInfo);
		int pos = fullName.indexOf('_');
		return pos == -1 ? fullName : fullName.substring(0, pos);
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
	 * @return String extension
	 */
	private String extractExtensionFromName(String src) {
		int pos = src.lastIndexOf('.');
		if (pos > 0) {
			return src.substring(pos);
		}
		return null;
	}

	/**
	 * Extract a video from traffic data
	 * 
	 * @param request
	 * @param session
	 * @param aroManifest
	 * @param fileName
	 */
	public void extractVideo(HttpRequestResponseInfo request, AROManifest aroManifest, String fileName) {
		HttpRequestResponseInfo response = request.getAssocReqResp();
		Session session = request.getSession();

		if (response != null) {
			byte[] content = null;
			String fullpath;

			log.debug("GET:" + request.getAbsTimeStamp().getTime() 
					+ ", Response:" + response.getAbsTimeStamp().getTime() 
					+ ", _video_" + StringParse.findLabeledDataFromString("_video_", ".mp4", request.getObjUri().toString()) 
					+ ", Range: bytes=:" + StringParse.findLabeledDataFromString("Range: bytes=", " ", request.getAllHeaders()));
			
			aroManifest = matchManifest(request, aroManifest);

			String quality = "";
			double bitrate = 0;
			String fullName = extractFullNameFromRRInfo(request);
			ArrayList<ByteRange> rangeList = extractByteRangesFromHeader("Range: bytes=", response.getAssocReqResp());
			ByteRange range = null;
			if (!rangeList.isEmpty()) {
				range = rangeList.get(0);
			}

			double segment = aroManifest.parseSegment(fullName, range);

			if (aroManifest instanceof ManifestDash) {
				int pos = fullName.lastIndexOf('_') + 1;
				int dot = fullName.lastIndexOf('.');
				if (pos > 0 && dot > pos) {
					quality = fullName.substring(pos, dot);
				}

				bitrate = ((ManifestDash) aroManifest).getBandwith(truncateString(fullName, "_"));
			
			} else if (aroManifest instanceof ManifestDTV) {
				int pos = fullName.indexOf('_') + 1;
				int dot = fullName.lastIndexOf('_');
				if (pos != -1 && dot > pos) {
					quality = fullName.substring(pos, dot);
					if (quality.matches("[-+]?\\d*\\.?\\d+")) {
						Double temp = aroManifest.getBitrate(quality);
						bitrate = temp != null ? temp : 0D;
					} else {
						//TODO need to handle different format of Request for an AD perhaps
					}
				}
			} else {
				// TODO some other way
				System.out.println("what to do?");
			}

			log.debug("trunk " + fileName + ", getTimeString(response) :" + getTimeString(response));
			byte[] thumbnail = null;
			double[] segmentMetaData = new double[2];

			String segmentName = getDebugPath() + "segment.mp4";

			try {
				content = reqhelper.getContent(response, session);
				if (content.length == 0) {
					return;
				}
				
				if (aroManifest instanceof ManifestDash){
					segmentMetaData = parsePayload(content);
				} else if (aroManifest instanceof ManifestDTV){
					segmentMetaData[0] = content.length;
				}
				
				if (segment <0 ){
					segment = segmentMetaData[1];
				}
				
				fullpath = constructDebugName(request, fileName, response, range, segment, quality);
				
				log.debug("video content.length :" + content.length);
				log.debug(fileName + ", length :" + content.length);

				if (segment == 0) {
					VideoData vData = new VideoData(aroManifest.getEventType(), quality, content);
					aroManifest.addVData(vData);
				} else {
					thumbnail = extractThumbnail(aroManifest, content, fullpath, segmentName, quality);
				}
				// TODO remove reliance on a real file
				filemanager.saveFile(new ByteArrayInputStream(content), fullpath);

			} catch (Exception e) {
				log.error("Failed to extract " + getTimeString(response) + fileName + ", range: " + range + ", error: " + e.getMessage());
				return;
			}


			double duration = 0;
			double segmentStartTime = 0;
			TreeMap<String, Double> metaData = null;
			if (thumbnail != null) {
				metaData = extractMetadata(segmentName);
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

			
			VideoEvent vEvent = new VideoEvent(thumbnail, aroManifest.getEventType(), segment, quality, rangeList, bitrate, duration, segmentStartTime, segmentMetaData[0], response);
			aroManifest.addVideoEvent(segment, response.getTimeStamp(), vEvent);

		}
	}

	private String constructDebugName(HttpRequestResponseInfo request, String fileName, HttpRequestResponseInfo response, ByteRange range, double segment, String quality) {
		String fullpath;
		StringBuffer fname = new StringBuffer(getDebugPath());
		fname.append(extractNameFromRRInfo(request));
		fname.append('_');
		fname.append(String.format("%08.0f", segment * 1e0));
		if (range != null) {
			fname.append("_R_");
			fname.append(range);
		}
		fname.append("_dl_");
		fname.append(getTimeString(response));
		fname.append("_S_");
		fname.append(String.format("%8.4f", response.getAssocReqResp().getSession().getSessionStartTime()));
		fname.append("_Q_");
		fname.append(quality);
		fname.append('_');
		fname.append(fileName);
		fullpath = fname.toString();
		return fullpath;
	}

	/**
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
		String cmd = Util.getFFMPEG() + " -i " + srcpath;
		String lines = extrunner.executeCmd(cmd);
		if (lines.indexOf("No such file") == -1) {
			double bitrate = getBitrate("bitrate: ", lines);
			results.put("bitrate", bitrate);
			String[] time = StringParse.findLabeledDataFromString("Duration: ", ",", lines).split(":"); // 00:00:02.75
			double start = StringParse.findLabeledDoubleFromString(", start:", ",", lines);
			Double duration = 0D;
			if (time.length == 3) {
				try {
					duration = Integer.parseInt(time[0]) * 3600 + Integer.parseInt(time[1]) * 60 + Double.parseDouble(time[2]);
					duration -= start;
				} catch (NumberFormatException e) {
					log.error("failed to parse duration from :" + StringParse.findLabeledDataFromString("Duration: ", ",", lines));
					duration = 0D;
				}
			} else if (time.length > 0) {
				try {
					duration = Double.parseDouble(time[0]);
					duration -= start;
				} catch (NumberFormatException e) {
					log.error("failed to parse duration from :" + time[0]);
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
	 * @return correct AroManifest
	 */
	private AROManifest matchManifest(HttpRequestResponseInfo request, AROManifest aroManifest) {
		HttpRequestResponseInfo response = request.getAssocReqResp();

		String objName = extractFullNameFromRRInfo(request);
		String videoName = "";
		AROManifest manifest;
		if (aroManifest == null) {
			aroManifest = videoUsage.findVideoInManifest(objName);
			if (aroManifest == null) {

				if (objName.contains("_video_")) {
					aroManifest = new ManifestDash(null, request);
					
					String test = aroManifest.toString();
					System.out.println("test :" + test);
				} else {
					aroManifest = new AROManifest(VideoType.UNKNOWN, response);
				}
				aroManifest.setVideoName(objName);
				videoUsage.add(request.getTimeStamp(), aroManifest);
			}
		}

		videoName = aroManifest.getVideoName();

		if (!videoName.isEmpty() && !objName.contains(videoName)) {
			manifest = videoUsage.findVideoInManifest(objName);
			aroManifest = manifest != null ? manifest : aroManifest;
			videoName = aroManifest.getVideoName();
		}
		return aroManifest;
	}

	/**
	 * Parse mp4 chunk/segment that contains one moof and one mdat.
	 * 
	 * @param content
	 * @return double[]  mdat payload length, time sequence
	 */
	private double[] parsePayload(byte[] content) {
		byte[] buf = new byte[4];
		double mdatSize = 0;
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
				//double trafSize = 
				bbc.getInt(); //skip over
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
					//double tfdtSize = 
					bbc.getInt(); //skip over
					bbc.get(buf);
					String tfdtName = new String(buf);
					if (tfdtName.equals("tfdt")) {
						bbc.getInt(); // skip over
						bbc.getInt(); // skip over
						timeSequence = bbc.getInt();
					}
				}
			}
		} else {
			return new double[] { 0, 0 };
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
		
		return new double[] { mdatSize, timeSequence };
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
	private byte[] extractThumbnail(AROManifest aroManifest, byte[] content, String srcpath, String segmentName, String quality) {
		byte[] data = null;
		filemanager.deleteFile(segmentName);

		VideoData vData = aroManifest.getVData(quality);
		if (vData == null) {
			return null;
		}

		// join mbox0 with segment
		byte[] mbox0 = vData.getContent();
		byte[] movie = new byte[mbox0.length + content.length];
		System.arraycopy(mbox0, 0, movie, 0, mbox0.length);
		System.arraycopy(content, 0, movie, mbox0.length, content.length);

		try {
			filemanager.saveFile(new ByteArrayInputStream(movie), segmentName);
		} catch (IOException e1) {
		}

		if (Util.isWindowsOS()) {
			data = extractVideoFrameShell(segmentName);
		} else {
			data = extractVideoFrameShell(segmentName);
		}
		return data;
	}

	private byte[] extractVideoFrameShell(String segmentName) {
		byte[] data = null;
		String thumbnail = getDebugPath() + "thumbnail.png";
		filemanager.deleteFile(thumbnail);

		String cmd = Util.getFFMPEG() + " -y -i " + segmentName + " -ss 00:00:00   -vframes 1 " + thumbnail;
		String ff_lines = extrunner.executeCmd(cmd);
		log.debug("ff_lines :" + ff_lines);

		Path path = Paths.get(thumbnail);
		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			log.debug("getThumnail IOException:" + e.getMessage());
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
			} catch(NumberFormatException e) {
				log.debug("Bit rate not available for key:" + key);
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
	 * Locate key in header, returning value as String
	 * @param fieldSearch
	 * @param rrInfo
	 * @return value of key-value located
	 */
	private ArrayList<ByteRange> extractByteRangesFromHeader(String fieldSearch, HttpRequestResponseInfo rrInfo) {
		ArrayList<ByteRange> rangeList = new ArrayList<>();
		String found = StringParse.findLabeledDataFromString(fieldSearch, "[abc]|[A-Z]", rrInfo.getAllHeaders());
		if (found.isEmpty() || found.equals("0- ")) {
			return rangeList;
		}
		Scanner scanner = new Scanner(found);
		Scanner stringScanner = scanner.useDelimiter("-|, |\\D");
		while (stringScanner.hasNext()) {
			try {
				rangeList.add(new ByteRange(Integer.valueOf(stringScanner.next()), Integer.valueOf(stringScanner.next())));
			} catch (NumberFormatException e) {
				log.error("NumberFormatException :" + e.getMessage());
			} catch (Exception e) {
				log.error("Exception :" + e.getMessage());
			}
		}
		scanner.close();

		return rangeList;
	}

	/**
	 * Obtain timestamp from request formated into a string. Primarily for debugging purposes.
	 * 
	 * @param response
	 * @return
	 */
	private String getTimeString(HttpRequestResponseInfo response) {
		double time;
		StringBuffer strTime = new StringBuffer();
		try {
			if (absTimeFlag) {
				Packet packet = response.getFirstDataPacket().getPacket(); // request
				time = packet.getSeconds();
				strTime.append(String.format("%.0f", time));
				strTime.append('.');
				time = packet.getMicroSeconds();
				strTime.append(String.format("%06.0f", time));
			} else {
				time = (float) response.getTimeStamp();
				strTime.append(String.format("%09.4f", time * 1e0));
			}
		} catch (Exception e) {
			log.error("Failed to get time from request: " + e.getMessage());
			strTime.append("Failed to get time from response->request: " + response);
		}
		return strTime.toString();
	}

	/**
	 * Extract a DTV/HLS manifest from traffic data
	 * 
	 * @param request
	 * @param session
	 *            that the response belongs to.
	 * @return AROManifest
	 */
	public AROManifest extractManifestDTV(HttpRequestResponseInfo request) {
		HttpRequestResponseInfo response = request.getAssocReqResp();
		Session session = request.getSession();

		byte[] content = null;
		try {
			content = reqhelper.getContent(response, session);
			if (content.length == 0) {
				return null;
			}
			log.debug("Manifest content.length :" + content.length);
			// String dtvMan = new String(content);

			// debug - save to debug folder
			StringBuffer fname = new StringBuffer(getDebugPath());
			fname.append(extractNameFromRRInfo(request));
			fname.append("__");
			fname.append(getTimeString(response));
			fname.append("_ManifestDTV.m3u8");
			filemanager.saveFile(new ByteArrayInputStream(content), fname.toString());

		} catch (Exception e) {
			log.error("Failed to get content from DTV Manifest; response: " + e.getMessage());
		}

		String getName = request.getObjNameWithoutParams();
		if (getName.indexOf('_') == -1 || aroManifest == null) {
			ManifestDTV manifest = null;
			try {
				manifest = new ManifestDTV(response, content);
				manifest.setDelay(videoUsagePrefs.getStartupDelay());
				log.debug("aroManifest :" + aroManifest);
				
				aroManifestMap.put(request.getTimeStamp(), manifest);
				return manifest;

			} catch (Exception e) {
				log.error("Failed to parse manifest data, absTimeStamp:" + request.getAbsTimeStamp().getTime() + ", Name:" + getName);
			}
		} else {
			((ManifestDTV) aroManifest).parseManifestData(content);
		}

		return aroManifest;
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
		Session session = request.getSession();
		byte[] content = null;

		MPDAmz mpdOut = null;
		try {
			content = reqhelper.getContent(response, session);
			if (content.length == 0) {
				return null;
			}
			mpdOut = xml2JavaJaxB(new ByteArrayInputStream(content));
			log.debug("Manifest content.length :" + content.length);
		
			// debug - save to debug folder
			StringBuffer fname = new StringBuffer(getDebugPath());
			fname.append(extractNameFromRRInfo(request));
			fname.append("__");
			fname.append(getTimeString(response));
			fname.append("_ManifestDash.mpd"); 
			filemanager.saveFile(new ByteArrayInputStream(content), fname.toString());

		} catch (Exception e) {
			log.error("Failed to parse manifest data, absTimeStamp:" + request.getAbsTimeStamp().getTime() + ", Name:" + request.getObjNameWithoutParams());
		}

		AROManifest manifest = null;
		try {
			manifest = new ManifestDash(mpdOut, response);
			manifest.setDelay(videoUsagePrefs.getStartupDelay());
			log.debug("aroManifest :" + aroManifest);
			
			for (AROManifest checkManifest : videoUsage.getAroManifestMap().values()) {
				if (checkManifest.getVideoName().equals(manifest.getVideoName())) {
					// don't create duplicates
					return checkManifest;
				}
			}

		} catch (Exception e) {
			log.error("Failed to parse manifest data:"+e.getMessage());
		}

		aroManifestMap.put(request.getTimeStamp(), manifest);

		return manifest;
	}

	/**
	 * Load Amazon manifest into an MPDAmz object
	 * 
	 * @param xmlByte
	 * @return an MPDAmz object
	 */
	private MPDAmz xml2JavaJaxB(ByteArrayInputStream xmlByte) {
		JAXBContext context;
		Unmarshaller unMarshaller;
		MPDAmz mpdOutput = new MPDAmz();

		try {
			context = JAXBContext.newInstance(MPDAmz.class);
			
			unMarshaller = context.createUnmarshaller();
			mpdOutput = (MPDAmz) unMarshaller.unmarshal(xmlByte);
			if (context == null || mpdOutput.getPeriod().isEmpty()) {
				log.error("MPD NULL");
			}
		} catch (JAXBException e) {
			log.error("JAXBException" + e.getMessage());
		} catch (Exception ex) {
			log.error("JaxB parse Exception" + ex.getMessage());
		}
		return mpdOutput;
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

				log.info("Failed to extract " + getTimeString(response) + imageFileName + " response: "
						+ e.getMessage());
				return;

			}

		}

	}


}// end class
