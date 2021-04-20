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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.SegmentIndexBox;
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
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.Manifest.ManifestType;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.UrlMatchDef;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;

import lombok.Data;
import lombok.NonNull;

@Data
public class VideoStreamConstructor {

	private static final Logger LOG = LogManager.getLogger(VideoStreamConstructor.class.getName());
	private static final Random RANDOM = new Random();

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

	private ManifestCollection manifestCollection;

	// Used for tracking segments by request object name to avoid repeated analysis
	// Inner map is mapped from segment's byte range as key and duration as value
	private Map<ChildManifest, Map<String, Double>> segmentInformationByFile = new HashMap<>();

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
		
		LOG.debug(String.format(">>Segment request, packetID: %d, len:%d, name:%s, TS:%.3f, URL:%s"
					, request.getAssocReqResp().getFirstDataPacket().getPacketId(), content.length, request.getFileName(), request.getTimeStamp(), request.getObjUri()));
		
		this.streamingVideoData = streamingVideoData;

		if (manifestBuilder == null || (manifestCollection = manifestBuilder.findManifest(request)) == null) {
			LOG.debug("manifestCollection is null :" + request.getObjUri());
			extractedSaveUnhandledContent(streamingVideoData, request, content);	
			return;
		}

		if ((childManifest = locateChildManifestAndSegmentInfo(request, content, timeStamp, manifestCollection)) == null) {
			LOG.debug("ChildManifest wasn't found for segment request:" + request.getObjUri());
			extractedSaveUnhandledContent(streamingVideoData, request, content);	
			return;
		}
		
		if ((videoStream = streamingVideoData.getVideoStream(manifestCollection.getManifest().getRequestTime())) == null) {
			videoStream = new VideoStream();
			videoStream.setManifest(manifestCollection.getManifest());
			streamingVideoData.addVideoStream(manifestCollection.getManifest().getRequestTime(), videoStream);
		}

		if (segmentInfo == null) {
			LOG.debug("segmentInfo is null :" + request.getObjUri());
			extractedSaveUnhandledContent(streamingVideoData, request, content);			
			return;
		}

		if (segmentInfo.getQuality().equals("0") && manifestCollection.getManifest().isVideoTypeFamily(VideoType.DASH)) {
			LOG.debug("Found DASH Track 0, determine what happened and if it is a problem :" + request.getObjNameWithoutParams());
			extractedSaveUnhandledContent(streamingVideoData, request, content);	
			return;
		}

		String name = manifestBuilder.buildSegmentName(request, extractExtensionFromRequest(request));

		crc32.update(content);

		updateVideoNameSize();
		
		byte[] thumbnail = null;
		
		if (segmentInfo.getSegmentID() == 0 && childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
			childManifest.setMoovContent(content);
			segmentInfo.setDuration(0);
		} else {
			String tempClippingFullPath = buildSegmentFullPathName(streamingVideoData, request);
			if (segmentInfo.getSize() == 0 && childManifest.getManifest().isVideoFormat(VideoFormat.MPEG4)) {
				HashMap<String, Integer> atomData = null;
				atomData = parsePayload(content);
				Integer mdatSize;
				if ((mdatSize = atomData.get("mdatSize")) != null) {
					segmentInfo.setSize(mdatSize);
				}
			}
			if (segmentInfo.isVideo()) {
				thumbnail = extractThumbnail(segmentInfo, childManifest, content, tempClippingFullPath);
			}
			if (!childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
				filemanager.deleteFile(tempClippingFullPath);
			}
		}

		if (thumbnail == null) {
			thumbnail = getDefaultThumbnail();
			segmentInfo.setThumbnailExtracted(false);
		} else {
			segmentInfo.setThumbnailExtracted(true);
		}
		
		if (segmentInfo.getDuration() > 0) {
			double bitRate = content.length * 8 / segmentInfo.getDuration();
			segmentInfo.setBitrate(bitRate / 1000);
		}

		VideoEvent videoEvent = new VideoEvent(thumbnail // imageArray
				, manifestCollection.getManifest()		// Manifest
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

	public void extractedSaveUnhandledContent(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request, byte[] content) {
		String fullPathName = filemanager.createFile(streamingVideoData.getVideoPath(), "[unhandled_video]_"+request.getFileName()).toString();
		savePayload(content, fullPathName);
	}

	/**
	 * Control the length of a VideoName so that is 40 characters or less.
	 */
	public void updateVideoNameSize() {
		String tempVideoName = manifestCollection.getManifest().getVideoName();
		if (tempVideoName.startsWith("http") || tempVideoName.length() > 40) {
			tempVideoName = shortenNameByParts(tempVideoName, "/", 3);
			manifestCollection.getManifest().setVideoName(tempVideoName);
		}
	}

	/**<pre>
	 * Locate and return the n substrings defined by the matchStr
	 * Example: "one:two:three:four:five"
	 * 	matchStr = ":"
	 * 	selectedPosition = 2
	 *  returns "four:five"
	 * 
	 * if matchStr is empty or null return srcString
	 * if srcString is empty or null return srcString
	 * if selectedPosition is less than or equal to zero then return srcString
	 * 
	 * @param srcString
	 * @param matchStr
	 * @param groupCount
	 * @return
	 */
	public String shortenNameByParts(String srcString, String matchStr, int groupCount) {
		if (!StringUtils.isEmpty(matchStr) && !StringUtils.isEmpty(srcString)) {
			int[] pos = StringParse.getStringPositions(srcString, matchStr);
			if (groupCount > 0 && pos.length > groupCount) {
				return srcString.substring(pos[pos.length - groupCount] + 1);
			}
		}
		return srcString;
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
			if (name.length() > 200) {
				name = shortenNameByParts(name, "/", 2);
				if (name.length() > 200) {
					name = name.substring(name.length() - 40);
				}
			}
			// cannot have slashes (from URL) in file name
			name = name.replaceAll("://", "_").replaceAll("/", "_");
			return String.format("%s%s_%08.0f_%s_%s_%s"
					, streamingVideoData.getVideoPath()
					, getTimeString(request)
					, request.getSession().getSessionStartTime() * 1000
					, segmentID < 0 ? "________" : String.format("%08d", segmentID)
					, segmentQuality
					, name);
		} catch (Exception e) {
			LOG.error("Failed to build path for fileName" + fileName, e);
			return "";
		}
	}

	/**
	 * build a new key if incoming key is a regex string and it can be applied to request
	 * 
	 * @param key
	 * @param request
	 * @return
	 */
	private String genKey(String key, HttpRequestResponseInfo request, UrlMatchDef urlMatchDef) {
		if (key.contains("(")) {
		    String requestStr = manifestBuilder.buildUriNameKey(urlMatchDef, request);
		    String[] matched = stringParse.parse(requestStr, key);
		    if (matched != null) {
		        key = requestStr;
		    }
		}

		return key;
	}
	
	/**
	 * Locate ChildManifest using keys based on multiple storage strategies, depending on Manifest & Manifest usage
	 * 
	 * @param request
	 * @param timeStamp
	 * @param manifestCollection
	 * @return
	 */
	public ChildManifest locateChildManifestAndSegmentInfo(HttpRequestResponseInfo request, byte[] content, Double timeStamp, ManifestCollection manifestCollection) {
		childManifest = null;
		segmentInfo = null;
		
		for (UrlMatchDef urlMatchDef : manifestCollection.getManifest().getSegUrlMatchDef()) {
		    String key0 = manifestBuilder.locateKey(urlMatchDef, request);
		    String key = genKey(key0, request, urlMatchDef);
    
    		Double timeKey =  timeStamp;
    		if ((childManifest = manifestCollection.getTimestampChildManifestMap().get(timeKey)) != null) {
    			LOG.debug("childManifest :" + childManifest.dumpManifest(600));
    			if ((segmentInfo = childManifest.getSegmentList().get(key)) != null) {
    				return childManifest;
    			}
    		}
    
    		timeKey = request.getTimeStamp();
    		do {
    			if ((timeKey = manifestCollection.getTimestampChildManifestMap().lowerKey(timeKey)) != null) {
    				childManifest = manifestCollection.getTimestampChildManifestMap().get(timeKey);
    			}
    		} while (timeKey != null && (segmentInfo = childManifest.getSegmentList().get(key)) == null);
    		
    		if ((childManifest = manifestCollection.getSegmentChildManifestTrie().get(key0)) != null || (childManifest = manifestCollection.getChildManifest(key0)) != null) {
    		    boolean encoded = childManifest.getSegmentList().size() != 0 
    		    		&& VideoType.DASH_ENCODEDSEGMENTLIST.equals(childManifest.getManifest().getVideoType())
    		    		? true : false;
    			String brKey = buildByteRangeKey(request, encoded);
    
    			if (brKey != null) {
    			    // If Segment list is empty, it is possible that the manifest is part of standard dash implementation.
    			    // As part of standard dash implementation, we assume that the segment information exists in sidx box in the fragmented mp4 received in the response of the request.
    		        populateSegmentInformation(request, content, childManifest);
    
    				if ((segmentInfo = childManifest.getSegmentList().get(brKey.toLowerCase())) == null) {
    					segmentInfo = childManifest.getSegmentList().get(brKey.toUpperCase());
    				}
    			}
    			
				if (segmentInfo == null && (segmentInfo = childManifest.getSegmentList().get(key)) == null) {
					if (!encoded && brKey != null) {
        			    Map.Entry<String, SegmentInfo> entry = childManifest.getSegmentList().select(brKey);
        			    if (entry.getKey().split("-")[1].equals(brKey.split("-")[1])) {
        			        segmentInfo = entry.getValue();
        			    }
    			    } else {
						segmentInfo = childManifest.getSegmentList().get(key);
        					}
        				}
 
    			
    			LOG.debug("childManifest: " + childManifest.getUriName() + ", URL match def: " + urlMatchDef);
				if (segmentInfo != null) {
					break;
				}    		} else if (timeStamp != null) {
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
    				LOG.debug("ChildManifest wasn't found for segment request: " + request.getObjUri() + ", URL match def: " + urlMatchDef);
    				nullSegmentInfo();
    				continue;
    			}
    
    			segmentInfo = null;
    			if ((segmentInfo = childManifest.getSegmentList().get(key)) == null) {
    				childManifest.getSegmentList().entrySet()
    				.stream().filter(segmentInfoEntry -> segmentInfoEntry.getKey().contains(key))
    				.findFirst()
    				.map(segmentInfoEntry -> segmentInfo = segmentInfoEntry.getValue())
    				.orElseGet(() -> nullSegmentInfo());
    			}
				if (segmentInfo != null) {
						break;
					}
				}
		}
		return childManifest;
	}

	/**
	 * Try to populate segment information to child manifest by locating sidx box in the fragmented mp4 type files
	 * @param request
	 * @param content
	 * @param childManifest
	 */
	private void populateSegmentInformation(HttpRequestResponseInfo request, byte[] content, ChildManifest childManifest) {
	    // Either the segment list could be empty initially or it could have one initialization segment information.
	    // We only proceed to look for a sidx box if the condition is satisfied
        if (childManifest.getSegmentList().size() < 2) {
	        // Make sure corresponding entry map isn't already populated
	        Map<String, Double> segmentMap = segmentInformationByFile.get(childManifest);
	        if (segmentMap != null && segmentMap.size() != 0 ) {
	            return;
	        }

	        if (segmentMap == null) {
	            segmentMap = new LinkedHashMap<>();

	            // Populate the chunk range to duration map with the first segment if it exists
	            if (childManifest.getSegmentList().size() == 1) {
	                segmentMap.put(childManifest.getSegmentList().firstKey(), 0.0d);
	            }

	            segmentInformationByFile.put(childManifest, segmentMap);
	        }

	        processSidxBox(content, childManifest);
        }
	}

	private void processSidxBox(byte[] content, ChildManifest childManifest) {
	    LOG.info("Processing sidx information for URI: " + childManifest.getUriName());

	    File tempFile = null;
	    IsoFile isoFile = null;
	    try {
	        // Write content data (mp4 file) to a temporary file 
            tempFile = File.createTempFile("temp" + RANDOM.nextInt(), ".mp4");
            FileUtils.writeByteArrayToFile(tempFile, content);

            // Parse mp4 file and process Segment Box
            isoFile = new IsoFile(tempFile);
            for (Box box : isoFile.getBoxes()) {
                if (box instanceof SegmentIndexBox) {
                    SegmentIndexBox sidxBox = (SegmentIndexBox) box;

                    int lastSegmentIndex = -1;
                    // Update last segment index to last entry in the segment list for the corresponding child manifest's segments
                    if (!childManifest.getSegmentList().isEmpty()) {
                        lastSegmentIndex = Integer.valueOf(childManifest.getSegmentList().lastKey().split("-")[1]);
                    }

                    double timePos = 0;
                    int idx = childManifest.getSegmentList().size();
                    Map<String, Double> segmentInformationMap = segmentInformationByFile.get(childManifest);
                    // Iterate through Segment Index Box entries
                    for (SegmentIndexBox.Entry entry : sidxBox.getEntries()) {
                        String key = String.valueOf((lastSegmentIndex + 1)) + "-" + String.valueOf((lastSegmentIndex + entry.getReferencedSize()));
                        segmentInformationMap.put(key, (double)entry.getSubsegmentDuration()/sidxBox.getTimeScale());

                        // Add SegmentInfo to Child Manifest
                        SegmentInfo segmentInfo = new SegmentInfo();
                        segmentInfo.setDuration((double)entry.getSubsegmentDuration()/sidxBox.getTimeScale());
                        segmentInfo.setStartTime(timePos);
                        segmentInfo.setSegmentID(idx++);
                        segmentInfo.setContentType(childManifest.getContentType());
                        segmentInfo.setVideo(childManifest.isVideo());
                        segmentInfo.setSize(entry.getReferencedSize());
                        segmentInfo.setQuality(String.valueOf(childManifest.getQuality()));
                        childManifest.addSegment(key, segmentInfo);

                        timePos += segmentInfo.getDuration();
                        lastSegmentIndex += entry.getReferencedSize();
                    }

                    // Not expecting and will not process another Segment Index Box
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("Something went wrong while reading sidx box", e);
        } finally {
            if (tempFile != null && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }

            if (isoFile != null) {
                try {
                    isoFile.close();
                } catch (IOException e) {
                    LOG.warn("Unable to close iso file resource", e);
                }
            }
        }
	}

	public SegmentInfo nullSegmentInfo() {
		return segmentInfo = null;
	}

	private String buildByteRangeKey(HttpRequestResponseInfo request, boolean encoded) {
		String key = null;

		String[] range = stringParse.parse(request.getAllHeaders().toString(), "Range: bytes=(\\d+)-(\\d+)");
		if (range != null) {
			try {
			    if (encoded) {
			        key = String.format("%1$016X-%2$016X", Integer.valueOf(range[0]), Integer.valueOf(range[1]));
			    } else {
			        key = range[0] + "-" + range[1];
			    }
			} catch (NumberFormatException e) {
				LOG.error("Failed to create ByteRangeKey: ", e);
			}
		}

		return key;
	}

	/**
	 * <pre>Extract a HLS manifest from traffic data
	 *
	 * Types: movie, livetv
	 * </pre>
	 * @param streamingVideoData
	 * @param request
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
		
		String fileName;
		if (!manifest.getManifestType().equals(ManifestType.MASTER)) {
			fileName = manifestBuilder.buildUriNameKey(request);
		} else {
			fileName = manifest.getVideoName();
		}
		request.getSession().getSessionStartTime();
		fileName = StringUtils.substringBefore(fileName, ";");
		savePayload(content, buildPath(streamingVideoData, request, -1, "m", fileName));
 		
		return manifest;
	}

	/**
	 * Extract a Thumbnail image from the first frame of a video
	 *
	 * @param segmentInfo
	 * @param childManifest
	 * @param segmentContent
	 * @param segName
	 * @return
	 */
	private byte[] extractThumbnail(SegmentInfo segmentInfo, ChildManifest childManifest, byte[] segmentContent, String segName) {

		filemanager.deleteFile(segName);
		byte[] movie = null;
		if (childManifest.getManifest().isVideoTypeFamily(VideoType.DASH)) {
			byte[] mbox0 = childManifest.getMoovContent();
			if (mbox0 == null) {
				return null;
			}
			movie = new byte[mbox0.length + segmentContent.length];
			System.arraycopy(mbox0, 0, movie, 0, mbox0.length);
			System.arraycopy(segmentContent, 0, movie, mbox0.length, segmentContent.length);
		} else {
			movie = segmentContent;
		}
		try {
			filemanager.saveFile(new ByteArrayInputStream(movie), segName);
		} catch (IOException e) {
			LOG.debug("IOException:" + e.getMessage());
		}
		return extractDataFromSegment(segmentInfo, childManifest, segName);
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

	/**
	 * Extract IFrame, Frame height, detect audio is present.
	 * if height is extracted it will be placed in segmentInfo.resolutionHeight and childManifest.pixelWidth
	 * if Audio is detected in a Video segment then segmentInfo.contentType will be set to ContentType.MUXED
	 * 
	 * @param segmentInfo
	 * @param childManifest
	 * @param segmentName
	 * @return byte[] first IFrame "thumbnail" in video segment
	 */
	private byte[] extractDataFromSegment(SegmentInfo segmentInfo, ChildManifest childManifest, String segmentName) {
		byte[] data = null;
		String thumbnail = streamingVideoData.getVideoPath() + "thumbnail.png";
		filemanager.deleteFile(thumbnail);
		String cmd = Util.getFFMPEG() + " -y -i " + "\"" + segmentName + "\"" + " -ss 00:00:00   -vframes 1 " + "\"" + thumbnail + "\"";
		String lines = extrunner.executeCmd(cmd, true, true);
		if (filemanager.fileExist(thumbnail)) {
			Path path = Paths.get(thumbnail);
			try {
				data = Files.readAllBytes(path);
				filemanager.deleteFile(thumbnail);
			} catch (IOException e) {
				LOG.debug("getThumbnail IOException:" + e.getMessage());
			}
		}
		
		if (!StringUtils.isEmpty(lines)) {
			String[] height = stringParse.parse(lines, "\\d{2,4}x(\\d+)");
			if (segmentInfo.getResolutionHeight() == 0 && height != null) {
				segmentInfo.setResolutionHeight(StringParse.stringToInteger(height[0], 0));
				childManifest.setPixelHeight(segmentInfo.getResolutionHeight());
			}

			if ((stringParse.parse(lines, "Stream #0.*Audio: ([A-Za-z0-9]*) ")) != null) {
				if (segmentInfo.isVideo() && segmentInfo.getContentType().equals(ContentType.VIDEO)) {
					segmentInfo.setContentType(ContentType.MUXED);
				}
				if (childManifest.getChannels() == null) {
					String chnls = "";
					if (lines.contains(" stereo,")) {
						chnls = "2";
					} else if (lines.contains(" mono,")) {
						chnls = "1";
					}
					childManifest.setChannels(chnls);
				}
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
			LOG.error("Failed to get time from request: ", e);
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

	/**<pre>
	 * Rebuild a fully qualified pathname to contain a tie-breaker 
	 * 
	 * examples: filepath/filename.txt becomes filepath/filename(1).txt filepath/filename becomes
	 * filepath/filename(1)
	 * 
	 * attempts to resolve over 200 attempts, then resorts to logging an error
	 * </pre>
	 * @param pathName
	 * @return pathName with embedded tie-breaker or when all else fails, appends (duplicated) to the pathName
	 */
	public String findPathNameTiebreaker(String pathName) {
		if (filemanager.fileExist(pathName)) {
			String temp = pathName;
			int pos = pathName.lastIndexOf(".");
			if (pos > 0) {
				for (int idx = 1; idx < 200; idx++) {
					temp = String.format("%s(%03d)%s", pathName.substring(0, pos), idx, pathName.substring(pos));
					if (!filemanager.fileExist(temp)) {
						return temp;
					}
				}
			} else {
				for (int idx = 1; idx < 200; idx++) {
					temp = String.format("%s(%03d)", pathName, idx);
					if (!filemanager.fileExist(temp)) {
						return temp;
					}
				}
			}
			LOG.debug("duplicate file(s) found :" + pathName);
			return pathName + "(duplicated)";
		} else {
			return pathName;
		}

	}

	private byte[] extractContent(HttpRequestResponseInfo request) {
		LOG.debug(String.format(" %d: %s",request.getFirstDataPacket().getPacketId(), request.getFileName()));
		byte[] content = null;
		try {
			content = reqhelper.getContent(request.getAssocReqResp(), request.getSession());
			if (content.length == 0) {
				content = null;
			}
		} catch (Exception e) {
			LOG.error(String.format("Download FAILED :%s :%s", request.getObjUri(), e.getMessage()));
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
	 * <pre>Extract a DASH manifest from traffic data
	 * 
	 * @param streamingVideoData
	 * @param request
	 * @return manifest
	 */
	public Manifest extractManifestDash(StreamingVideoData streamingVideoData, HttpRequestResponseInfo request) {
		LOG.debug("extractManifestDash - request :" + request);
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