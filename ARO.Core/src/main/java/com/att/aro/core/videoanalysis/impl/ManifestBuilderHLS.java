/*
 *  Copyright 2019 AT&T
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
package com.att.aro.core.videoanalysis.impl;

import java.util.regex.Pattern;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.att.aro.core.AROConfig;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.Manifest.ManifestType;
import com.att.aro.core.videoanalysis.pojo.UrlMatchDef;
import com.att.aro.core.videoanalysis.pojo.UrlMatchDef.UrlMatchType;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;

public class ManifestBuilderHLS extends ManifestBuilder {

	protected static final Logger LOG = LogManager.getLogger(ManifestBuilderHLS.class.getName());
	protected static final Pattern pattern = Pattern.compile("^(#[A-Z0-9\\-]*)");

	enum HlsType {
		Parent, VodChild, LiveChild
	}

	ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	IStringParse stringParse = context.getBean(IStringParse.class);
	private String childUriName;
	private boolean adjustDurationNeeded = false;
	private Double mediaSequence;

	public ManifestBuilderHLS() {
	}
	
	@Override
	public String buildSegmentName(HttpRequestResponseInfo request, String extension) {
		String name = request.getObjNameWithoutParams();
		int dot = name.lastIndexOf('.');
		if (StringUtils.isNotEmpty(extension) && name.endsWith(extension)) {
			name = name.substring(0, name.length() - extension.length());
		}
		if (dot > -1) {
			int sep = name.substring(0, dot).lastIndexOf('/');
			if (sep > -1) {
				name = name.substring(sep + 1);
			}
		}
		name = StringUtils.replace(name, "/", "-");
		name = StringUtils.replace(name, ".", "-");
		return name + extension;
	}

	public String buildSegmentKey(String segmentFile) {
		String key = segmentFile;
		if (manifestCollection.getCommonBaseLength() > 0) {
			key = segmentFile.substring(0, manifestCollection.getCommonBaseLength());
		} else {
			int pos = segmentFile.lastIndexOf(".");
			if (pos > -1) {
				key = segmentFile.substring(0, pos);
			}
		}
		return key;
	}
	
	public void parseManifestData(Manifest newManifest, byte[] data) {

		if (data == null || data.length == 0) {
			LOG.debug("Manifest file has no content");
			return;
		}
		String strData = new String(data);
		String[] sData = (new String(data)).split("\r\n");
		if (sData == null || sData.length == 1) {
			sData = (new String(data)).split("[\n\r]");
		}

		int scanLength = strData.length() > 500 ? 20 : strData.length();
		if (strData.substring(0, scanLength).contains("#EXTM3U")) {
			newManifest.setVideoType(VideoType.HLS);
			newManifest.setVideoFormat(VideoFormat.TS);
		} else {
			LOG.debug(String.format("Unrecognized Manifest:%s \ndata:%s",newManifest.getRequest().getObjNameWithoutParams(), strData));
			return;
		}
		adjustDurationNeeded = false;
		String[] flag;
		
		for (int itr = 0; itr < sData.length; itr++) {
			
			if (!sData[itr].startsWith("#") || (flag = stringParse.parse(sData[itr], pattern)) == null) {continue;}

			ContentType contentType;
			switch (flag[0]) {
			
			// Master & Child -------------------------------
			case "#EXTM3U": // FirstLine of a manifest
				//VID-TODO revisit this, want to know the proper key structure
				String key = StringUtils.substringBefore(newManifest.getRequest().getObjNameWithoutParams(), ";");
				
				if (strData.contains("#EXT-X-STREAM-INF:")) { // master manifest
					// dealing with a master manifest here, hence need a new ManifestCollection
					LOG.debug("******* Parsing new Master Manifest (Video Stream)");
					switchManifestCollection(newManifest, key, manifest.getRequestTime());
				} else if (strData.contains("#EXTINF:")) { // child manifest
					LOG.debug(" ****** Parsing new Child Manifest");
					if (manifestCollection == null) {
						// special handling of childManifest when there is no parent manifest
						switchManifestCollection(newManifest, key, manifest.getRequestTime());
					}
					
					newManifest.setManifestType(ManifestType.CHILD);
					newManifest.setMasterManifest(masterManifest);
					
					childManifest = locateChildManifest(newManifest);
					if (childManifest.getManifest() != null && childManifest.getManifest().getChecksumCRC32() == newManifest.getChecksumCRC32()) {
						LOG.debug("Identical VOD child manifest found, skipping..." + newManifest.getVideoName());
						return;
					}
					
					if (childManifest.getManifest() == null) {
						childManifest.setManifest(newManifest);
					}
					// set name on (child)manifest
					childManifest.getManifest().setVideoName(StringUtils.substringBefore(buildUriNameKey(childManifest.getManifest().getRequest()), ";"));
					
					if (childManifest.isVideo()) {
						if (childManifest.getCodecs().contains(",")) {
							childManifest.setContentType(ContentType.MUXED);
						} else {
							childManifest.setContentType(ContentType.VIDEO);
						}
					}
				} else {
					LOG.debug("Unknown HLS manifest:\n" + strData);
					return;
				}
				break;
				
			case "#EXT-X-VERSION": // Indicates the compatibility version of the playlist file
				break;

			// Master only -------------------------------
			case "#EXT-X-MEDIA": // Alternate MediaPlaylist
				// #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="eng",DEFAULT=YES,AUTOSELECT=YES,LANGUAGE="eng",URI="06.m3u8"
				// #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="caption_1",DEFAULT=NO,AUTOSELECT=YES,LANGUAGE="ENG",URI="114.m3u8"

				childUriName = StringParse.findLabeledDataFromString("URI=", "\"", sData[itr]);

				childUriName = decodeUrlEncoding(childUriName);
				
				if (StringUtils.isNotEmpty(childUriName)) {
					LOG.debug("MEDIA childUriName :" + childUriName);
					UrlMatchDef urlMatchDef = defineUrlMatching(childUriName);
	                manifest.getSegUrlMatchDef().add(urlMatchDef);
	                LOG.debug("EXT-X-MEDIA defineUrlMatching(childUriName): " + urlMatchDef);

					childManifest = createChildManifest(null, "", childUriName);
					childManifest.setVideo(false);
					switch (StringParse.findLabeledDataFromString("TYPE=", ",", sData[itr])) {
					case "AUDIO":
						contentType = ContentType.AUDIO;
						String channels = StringParse.findLabeledDataFromString("CHANNELS=", ",", sData[itr]);
						if (StringUtils.isNotEmpty(channels)) {
							childManifest.setChannels(
									channels.startsWith("") ? channels.substring(1, channels.length()-1) : channels);
						}
						break;
					case "CLOSED-CAPTIONS":
					case "SUBTITLES":
						contentType = ContentType.SUBTITLES;
						break;
					default:
						contentType = ContentType.UNKNOWN;
						break;
					}
					childManifest.setContentType(contentType);
					manifest.setContentType(contentType);
				}				
				break;

			case "#EXT-X-STREAM-INF": // ChildManifest Map itr:metadata-Info, ++itr:childManifestName
				String childParameters = sData[itr];
				childUriName = decodeUrlEncoding(sData[++itr]);

				UrlMatchDef urlMatchDef = defineUrlMatching(childUriName);
				manifest.getSegUrlMatchDef().add(urlMatchDef);
				LOG.debug("EXT-X-STREAM-INF defineUrlMatching(childUriName): " + urlMatchDef);
				
				childManifest = manifestCollection.getChildManifest(childUriName);
				
				if (childManifest == null) {
					childManifest = createChildManifest(null, childParameters, childUriName); // stored in childMap
				}
				break;

			case "#EXT-X-I-FRAME-STREAM-INF": // "keyframes" I_FrameInfo for fast-forward
				break;

			// child manifest only -------------------------------
			case "#EXT-X-PLAYLIST-TYPE":
				if ("VOD".equals(StringParse.findLabeledDataFromString("#EXT-X-PLAYLIST-TYPE", ":", sData[itr]))) {
					newManifest.setPlayListType(Manifest.StreamType.VOD);
				}
				break;

			case "#EXT-X-INDEPENDENT-SEGMENTS":
				break;
				
			case "#EXTINF": // Segment duration + media-file
				if (childManifest == null) {
					LOG.debug("failed to locate child manifest " + sData[++itr]);
				} else {
					String parameters = sData[itr];
					String segmentUriName;

					if (sData[itr + 1].startsWith("#EXT-X-BYTERANGE:")) {
						itr = itr + 2;
						segmentUriName = sData[itr];
					} else {
						segmentUriName = sData[++itr];
					}
					segmentUriName = cleanUriName(segmentUriName);

    				urlMatchDef = defineUrlMatching(segmentUriName);
    				// Build reference URL and URL match definition for the segment using corresponding child manifest
					if (segmentUriName.length() > 0 && segmentUriName.charAt(0) != '/') {
						if (UrlMatchType.COUNT.equals(urlMatchDef.getUrlMatchType())) {
							UrlMatchDef segmentManifestUrlMatchDef = defineUrlMatching(childManifest.getUriName());

							if (segmentManifestUrlMatchDef.getUrlMatchLen() != 0) {
								urlMatchDef.setUrlMatchLen(
										urlMatchDef.getUrlMatchLen() + segmentManifestUrlMatchDef.getUrlMatchLen());
								segmentUriName = StringUtils.substringBeforeLast(childManifest.getUriName(), "/") + "/"
										+ segmentUriName;
							}
						}
					}

    				masterManifest.getSegUrlMatchDef().add(urlMatchDef);
    				newManifest.getSegUrlMatchDef().add(urlMatchDef);
    				LOG.debug("EXTINF defineUrlMatching(childUriName): " + urlMatchDef);
					
					segmentInfo = createSegmentInfo(childManifest, parameters, segmentUriName);

					String[] segmentMatch = stringParse.parse(segmentUriName, "-([0-9]*T[0-9]*)-([0-9]*)-");
					if (segmentMatch != null) {
						newManifest.getTimeScale();
						segmentInfo.setStartTime(0);
					}

					segmentUriName = decodeUrlEncoding(segmentUriName);
			        segmentUriName = StringUtils.substringBefore(segmentUriName, "?");
					if (childManifest.addSegment(segmentUriName, segmentInfo).equals(segmentInfo)) {
						manifestCollection.addToSegmentTrie(segmentUriName, segmentInfo);
						manifestCollection.addToTimestampChildManifestMap(manifest.getRequest().getTimeStamp(), childManifest);
						if (!manifestCollection.getSegmentChildManifestTrie().containsKey(segmentUriName)) {
							manifestCollection.addToSegmentChildManifestTrie(segmentUriName, childManifest);
							addToSegmentManifestCollectionMap(segmentUriName);
						}
					}
				}
				break;
				
			case "#EXT-X-ENDLIST": // Segment ENDLIST for VOD (STATIC)
				break;

			case "#EXT-X-KEY": // Segment is encrypted using [METHOD=____]
				break;

			case "#EXT-X-MEDIA-SEQUENCE": // Indicates the sequence number of the first URL that appears in a playlist file
				mediaSequence = StringParse.findLabeledDoubleFromString("#EXT-X-MEDIA-SEQUENCE", ":", sData[itr]);
				if (mediaSequence != null) {
					childManifest.setSegmentCount(mediaSequence.intValue());
				}
				break;

			case "#EXT-X-TARGETDURATION": // Specifies the maximum media-file duration.
				// processExtXtargetDuration(line);
				break;

			case "#EXT-X-PROGRAM-DATE-TIME": // YYYY-MM-DDTHH:MM:sss
				String dateString = StringParse.findLabeledDataFromString("#EXT-X-PROGRAM-DATE-TIME:", "\\$", sData[itr]);
				dateString = dateString.replaceAll("\\+00\\:00", "000Z");
				long programDateTime = Util.parseForUTC(dateString);
				if (masterManifest.updateStreamProgramDateTime(programDateTime)) {
					// VID-TODO need to resync segments across ManifestCollection
					LOG.debug("Program time changed, need to resync segments across ManifestCollection");
				}
				if (childManifest.getSegmentStartTime() == 0) {
					childManifest.setStreamProgramDateTime(programDateTime);
				}
				break;

			case "#USP-X-MEDIA":
				double bandwidth = StringParse.findLabeledDoubleFromString("BANDWIDTH=", ",", sData[itr]);
				childManifest.setBandwidth(bandwidth);
				childManifest.setCodecs(StringParse.findLabeledDataFromString("CODECS=", "\"", sData[itr]));
				String content = StringParse.findLabeledDataFromString("TYPE=", ",", sData[itr]);
				if (StringUtils.isNotEmpty(content)) {
					if ("AUDIO".equals(content)) {
						childManifest.setContentType(ContentType.AUDIO);
						String val = StringParse.findLabeledDataFromString("CHANNELS=", "\"", sData[itr]);
						if(val.contentEquals("")) {
							val = "NA ";
						}
						LOG.debug("Parsing audio channels values: " + val);					
						childManifest.setChannels(val);
					} else if ("VIDEO".equals(content)) {
						childManifest.setContentType(ContentType.VIDEO);
					}
				}
				
				break;

			case "#USP-X-TIMESTAMP-MAP": // #USP-X-TIMESTAMP-MAP:MPEGTS=900000,LOCAL=1970-01-01T00:00:00Z
				break;

			default:
				break;
			}
		}
		if (manifest.getManifestType().equals(Manifest.ManifestType.MASTER)) {
			assignQuality(manifestCollection);
		}
		if (adjustDurationNeeded) {
			adjustDurations(childManifest);
		}
	}

	/**
	 * Adjust duration values per segment
	 * @param childManifest
	 */
	public void adjustDurations(ChildManifest childManifest) {
		if (!childManifest.getSegmentList().isEmpty()) {
			PatriciaTrie<SegmentInfo> segmentList = childManifest.getSegmentList();
			SegmentInfo priorSegment = null;
			SegmentInfo segment;
			String key = segmentList.firstKey();
			String nkey;

			while ((nkey = segmentList.nextKey(key)) != null) {
				segment = segmentList.get(nkey);
				if (priorSegment != null) {
					priorSegment.setDuration(segment.getStartTime() - priorSegment.getStartTime());
				}
				key = nkey;
				priorSegment = segment;
			}
		}
	}
	
	@Override
	protected ChildManifest createChildManifest(Manifest manifest, String parameters, String childUriName) {
		ChildManifest childManifest = new ChildManifest();
		childManifest.setManifest(manifest);
		childManifest.setUriName(childUriName);

		if (StringUtils.isNotEmpty(parameters)) {
			Double bandwidth = StringParse.findLabeledDoubleFromString("BANDWIDTH=", parameters);
			if (bandwidth != null) {
				manifestCollection.addToBandwidthMap(bandwidth, childManifest);
				childManifest.setBandwidth(bandwidth);
			}

			String resolution = StringParse.findLabeledDataFromString("RESOLUTION=", ",", parameters);
			if (StringUtils.isNotEmpty(resolution)) {
				String[] resolutionNumbers = resolution.split("[Xx]");
				if (resolutionNumbers.length == 2) {
					try {
						childManifest.setPixelWidth(Integer.valueOf(resolutionNumbers[0]));
						childManifest.setPixelHeight(Integer.valueOf(resolutionNumbers[1]));
					} catch (NumberFormatException e) {
						LOG.debug(String.format("failed to parse \"%s\" or \"%s\"", resolutionNumbers[0], resolutionNumbers[1]));
						childManifest.setPixelWidth(0);
						childManifest.setPixelHeight(0);
					}
					childManifest.setVideo(true);
					childManifest.setContentType(ContentType.VIDEO);
				}
			}
			
			String codecs = StringParse.findLabeledDataFromString("CODECS=", "\"", parameters);
			childManifest.setCodecs(codecs);
			
			if (codecs.contains(",")) {
				childManifest.setVideo(true);
				childManifest.setContentType(ContentType.MUXED);
			} else if (codecs.contains("v")) {
				childManifest.setVideo(true);
				childManifest.setContentType(ContentType.VIDEO);
			} else if (codecs.contains("a")) {
				childManifest.setVideo(false);
				childManifest.setContentType(ContentType.AUDIO);
			} else {
				childManifest.setVideo(false);
				childManifest.setContentType(ContentType.UNKNOWN);
			}
		}

		childUriName = StringUtils.substringBefore(childUriName, "?");
		manifestCollection.addToUriNameChildMap(childUriName, childManifest);
		return childManifest;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\nManifestBuilderHLS :\n");
		strblr.append(super.toString());
		return strblr.toString();
	}

}
