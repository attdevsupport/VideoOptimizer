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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.AROConfig;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.Manifest.ManifestType;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.UrlMatchDef;
import com.att.aro.core.videoanalysis.pojo.UrlMatchDef.UrlMatchType;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;

import lombok.Data;
import lombok.NonNull;

@Data
public abstract class ManifestBuilder {

	protected static final Logger LOG = LogManager.getLogger(ManifestBuilder.class.getName());
	protected static final Pattern pattern = Pattern.compile("^(#[A-Z0-9\\-]*)");

	protected int manifestCount;
	protected Manifest manifest;

	@NonNull
	ChildManifest childManifest = null;
	@NonNull
	ChildManifest tempChildManifest = null;
	SegmentInfo segmentInfo = null;

	/**
	 * Key1: MasterManifest "Name" request.getObjUri().getRawPath()
	 * Key2: Timestamp seconds from start of trace
	 * Value: ManifestCollection
	 */
	@NonNull
	protected Map<String, Map<Double, ManifestCollection>> manifestCollectionMap = new HashMap<>();
	
	/**<pre>
	 * A Trie of ManifestCollection
	 * Key: segmentUriName(HLS) or BaseURL(DASH)
	 */
	protected PatriciaTrie<String> segmentManifestCollectionMap = new PatriciaTrie<>();

	@NonNull
	protected ManifestCollection manifestCollection;
	
	protected double mostRecentTimestamp = 0;
	protected double identifiedManifestRequestTime = 0;
	protected ManifestCollection manifestCollectionToBeReturned;
	protected String videoName;
	
	protected String referenceKey;
	
	ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	IStringParse stringParse = context.getBean(IStringParse.class);

	private CRC32 crc32;
	private String key;
	Manifest masterManifest;
	private String byteRangeKey;
	
	public ManifestBuilder() {

	}

	/**
	 * <pre>
	 * Create a Manifest object from byte[] data.
	 *  if manifest is a master, a new ManifestCollection will be created to hold the masterManifest
	 *  if manifest is a "child" it will be attached to it's master
	 * 
	 * @param request - object
	 * @param data - byte[]
	 * @param videoPath
	 * @return manifest, that was just created, master or child
	 */
	public Manifest create(HttpRequestResponseInfo request, byte[] data, String videoPath) {
		crc32 = new CRC32();
		manifest = new Manifest();
		manifest.setRequest(request);
		manifest.setVideoName(StringUtils.substringBefore(request.getObjNameWithoutParams(), ";"));
		manifest.setUri(request.getObjUri());
		manifest.setUriStr(request.getObjUri().toString());
		manifest.setSession(request.getAssocReqResp().getSession());
		manifest.setVideoPath(videoPath);
		manifest.setContent(data);
		crc32.update(data);
		manifest.setChecksumCRC32(crc32.getValue());
		manifest.setRequestTime(request.getTimeStamp());
		manifest.setEndTime(request.getAssocReqResp().getLastDataPacket().getTimeStamp());
 		parseManifestData(manifest, data);
		return manifest;
	}

	public String shortFileName(String rawName) {
		String objName = StringUtils.substringBefore(rawName, ";");
		objName = StringUtils.substringAfterLast(objName, "/");
		return objName;
	}


	protected void validateManifestVideoName(Manifest manifest, String childName) {
		int pos = 0;
		String manifestName = manifest.getVideoName();
		int idms = manifestName.lastIndexOf("/") + 1;

		int idcs = childName.lastIndexOf("/") + 1;
		int idcx = childName.lastIndexOf(".");
		if (idcx < 0) {
			idcx = childName.length();
		}
		while (--idcx > 0) {
			pos = manifestName.substring(idms).indexOf(childName.substring(idcs, idcx));
			if (pos != -1) {
				break;
			}
		}
		manifest.setVideoName(manifestName.substring(idms + pos));
		manifest.setVideoNameValidated(true);
	}
	
	/**
	 * Locate and return a segment number.
	 * 
	 * @param request
	 * @return segment number or -1 if not found
	 */
	public SegmentInfo getSegmentInfo(HttpRequestResponseInfo request) {
		ManifestCollection manifestCollection;
		SegmentInfo segmentInfo = null;
		
		String key = buildKey(request);
		manifestCollection = findManifest(request);
		if (manifestCollection != null) {
			segmentInfo = manifestCollection.getSegmentTrie().get(key);
			if (segmentInfo == null) {
				segmentInfo = manifestCollection.getSegmentTrie().get(request.getObjUri().toString());
			}
		}
		return segmentInfo;
	}

	/**
	 * Locate and return a ChildManifest.
	 * 
	 * @param request
	 * @return segment number or -1 if not found
	 */
	public ChildManifest getChildManifest(HttpRequestResponseInfo request) {
		ChildManifest childManifest;
		ManifestCollection manifestCollection;
		manifestCollection = findManifest(request);
		childManifest = manifestCollection.getSegmentChildManifestTrie().get(buildKey(request));
		if (childManifest == null) {
			childManifest = manifestCollection.getSegmentChildManifestTrie().get(request.getObjUri().toString());
		}
		return childManifest;
	}

	public String buildKey(HttpRequestResponseInfo request) {
		if (StringUtils.isEmpty(byteRangeKey)) {
			return StringUtils.substringAfterLast(request.getObjNameWithoutParams(), "/");
		} else {
			return byteRangeKey;
		}
	}

	public String formatKey(String uriString) {
		String key = uriString;
		int dot = key.lastIndexOf('.');
		if (dot > -1) {
			dot = key.length();
		}
		int sep = key.substring(0, dot).lastIndexOf('/');
		if (sep > -1) {
			key = key.substring(sep + 1);
		}
		return key;
	}
	
	/**
	 * Parse out Video Streaming information from the data object
	 * 
	 * @param newManifest
	 * @param data
	 */
	public abstract void parseManifestData(Manifest newManifest, byte[] data);

	protected void assignQuality(ManifestCollection manifestCollection) {
		int quality = 1;
		for ( ChildManifest ChildManifest: manifestCollection.getBandwidthMap().values()) {
			ChildManifest.setQuality(quality++);
		}
	}
	
	protected ChildManifest locateChildManifest(Manifest manifest) {
		childManifest = null;

		for (UrlMatchDef urlMatchDef : manifest.getMasterManifest().getSegUrlMatchDef()) {
	        referenceKey =  Util.decodeUrlEncoding(buildUriNameKey(urlMatchDef, manifest.getRequest()));

    		if ((childManifest = manifestCollection.getUriNameChildMap().get(referenceKey)) == null) {
                if (!CollectionUtils.isEmpty(manifestCollection.getUriNameChildMap())) {
                    childManifest = manifestCollection.getUriNameChildMap().entrySet().stream()
                            .filter(x -> x.getKey().contains(referenceKey))
                            .findFirst().map(x -> x.getValue())
                            .orElseGet(() -> null);
                }
            }

    		if (childManifest != null) {
    		    break;
    		}
		}

		if (childManifest == null) {
			// create an adhoc childManifest
			childManifest = createChildManifest(manifest, "", referenceKey);
			manifestCollection.addToTimestampChildManifestMap(manifest.getRequest().getTimeStamp(), childManifest);
			childManifest.setVideo(false);
		}

		return childManifest;
	}

	protected UrlMatchDef defineUrlMatching(String uriName) {
		UrlMatchDef urlMatchDef = new UrlMatchDef();
		if (uriName.startsWith("http")) {
			urlMatchDef.setUrlMatchType(UrlMatchType.FULL);
		} else {
			String name = StringUtils.substringBeforeLast(uriName, ".");
			int countSlash = StringUtils.countMatches(name, "/");
			urlMatchDef.setUrlMatchLen(countSlash);
			urlMatchDef.setUrlMatchType(UrlMatchType.COUNT);
			urlMatchDef.setPrefix(uriName.startsWith("/"));
		}

		return urlMatchDef;
	}

	/**
	 * builds a key for childUriName from request info
	 *
	 * @param request
	 * @return
	 */
	public String buildUriNameKey(HttpRequestResponseInfo request) {
		return buildUriNameKey(new UrlMatchDef(), request);
	}

	/**
	 * Builds a key from request info, utilizing UrlMatchDef
	 * 
	 * @param targetUrlMatchDef
	 * @param request
	 * @return
	 */
	public String buildUriNameKey(UrlMatchDef targetUrlMatchDef, HttpRequestResponseInfo request) {
		if (targetUrlMatchDef == null) {
			return "";
		}
		return applyUriMatchDef(request, targetUrlMatchDef);
	}

	public String applyUriMatchDef(HttpRequestResponseInfo request, UrlMatchDef targetUrlMatchDef) {
		String key = "";

		switch (targetUrlMatchDef.getUrlMatchType()) {
    		case FULL:
    			if (request.getObjUri() != null) {
    				key = StringUtils.substringBefore(request.getObjUri().toString(), "?");
    			}
    			break;
    		case UNKNOWN:
    			key = request.getFileName();
    			break;
    		case COUNT: {
    			key = request.getObjNameWithoutParams();
    			int count = StringUtils.countMatches(key, "/");
    			int matchCount;
    			int skipSlash = targetUrlMatchDef.isPrefix() ? 0 : 1;
    			if (count != (matchCount = targetUrlMatchDef.getUrlMatchLen())) {
    				int[] pos = getStringPositions(key, "/");
    				if (pos.length >= matchCount + 1) {
    				    key = key.substring(pos[pos.length - 1 - matchCount] + skipSlash);
    				}
    			}
    			break;
    		}
    		default:
    			break;
		}

		LOG.debug(String.format("key: %s | Request -> objUri: %s, fileName: %s, objNameWithoutParams: %s | UrlMatchDef -> %s",
		        key, request.getObjUri(), request.getFileName(), request.getObjNameWithoutParams(), targetUrlMatchDef.toString()));
		return key;
	}
		
	public int[] getStringPositions(String inputStr, String matchStr) {
		int count = StringUtils.countMatches(inputStr, matchStr);
		if (count > 0) {
			int[] position = new int[count];
			int idx = 0;
			int index = inputStr.indexOf(matchStr);
			position[idx++] = index;
			while ((index = inputStr.indexOf(matchStr, index + 1)) >= 0) {
				position[idx++] = index;
			}
			return position;
		}
		return new int[0];
	}

	protected SegmentInfo createSegmentInfo(ChildManifest childManifest, String parameters, String segmentUriName) {
		int segmentID = childManifest.getNextSegmentID();
		SegmentInfo segmentInfo = new SegmentInfo();
		segmentInfo.setChildManifest(childManifest);
		segmentInfo.setVideo(childManifest.isVideo());
		segmentInfo.setDuration(StringParse.findLabeledDoubleFromString(":", "\\,", parameters));
		segmentInfo.setSegmentID(segmentID);
		segmentInfo.setQuality(String.format("%.0f", (double) childManifest.getQuality()));
		if (segmentInfo.isVideo() && (segmentInfo.getSegmentID() == 0 && manifest.isVideoFormat(VideoFormat.MPEG4))) {
			segmentInfo.setVideo(false);
		}
		return segmentInfo;
	}

	/**
	 * Switch/Create a Manifest if there is no manifestCollection or key/timestamp for a manifest in manifestCollectionMap.
	 * 
	 * @param tmpManifest
	 * @param key
	 * @param timestamp
	 */
	protected void switchManifestCollection(Manifest tmpManifest, String key, double timestamp) {
		if (!manifestCollectionMap.containsKey(key)) {
			manifestCollectionMap.put(key, new HashMap<Double, ManifestCollection>());
		}
		if ((manifestCollection = manifestCollectionMap.get(key).get(timestamp)) == null) {
			tmpManifest.setManifestType(ManifestType.MASTER);
			tmpManifest.setContentType(ContentType.UNKNOWN);
			childManifest = null;
			manifestCollection = new ManifestCollection();
			manifestCollection.setManifest(tmpManifest);
			masterManifest = tmpManifest;
			manifestCollectionMap.get(key).put(timestamp, manifestCollection);
		} else {
			LOG.error(String.format("Two Manifests, same key, same timestamp : %s, %.3f\n", key, timestamp));
		}
	}

	protected ChildManifest createChildManifest(Manifest manifest, String parameters, String childUriName) {
		childUriName = Util.decodeUrlEncoding(childUriName);
		ChildManifest childManifest = new ChildManifest();
		childManifest.setManifest(manifest);
		childManifest.setUriName(childUriName);
		childManifest.setManifestCollectionParent(manifestCollection);
		manifest.getSegUrlMatchDef().add(defineUrlMatching(childUriName));
		manifestCollection.addToUriNameChildMap(childUriName, childManifest);
		return childManifest;
	}

	public boolean isSegmentOrder() {
		return manifestCollection.isSegmentOrder();
	}

	/**
	 * 
	 * @param request
	 * @return ManifestCollection, null if not found
	 */
	public ManifestCollection findManifest(HttpRequestResponseInfo request) {
		LOG.debug(String.format("locating manifest for|<%s>|", request.getObjUri().toString()));
		if (CollectionUtils.isEmpty(manifestCollectionMap)) {
			return null;
		}
		
		manifestCollectionToBeReturned = null;//initial
		identifiedManifestRequestTime = 0;
		byteRangeKey = "";
		double manifestReqTime = 0;
		double lastManifestReqTime = 0;
		for (Map<Double, ManifestCollection> innerMap : manifestCollectionMap.values()) {
			for (ManifestCollection manifestCollection : innerMap.values()) {
				if (manifestCollection.getManifest().getRequestTime() < request.getTimeStamp()) {
					manifestReqTime = manifestCollection.getManifest().getRequestTime();
					if (lastManifestReqTime == 0 || lastManifestReqTime < manifestReqTime) {
						for (UrlMatchDef urlMatchDef : manifestCollection.getManifest().getMasterManifest().getSegUrlMatchDef()) {
							key = buildUriNameKey(urlMatchDef, request);
							// VID-TODO find #EXT-X-MAP:URI="5b3733a4-e7db-4700-975b-4e842c158274/3cec-BUMPER/02/1200K/map.mp4"
							key += "|" + formatTimeKey(manifestCollection.getManifest().getRequestTime());
							if (!StringUtils.isEmpty(key)) {
								if (locatePatKey(segmentManifestCollectionMap, key, request, urlMatchDef) != null) {
									manifestCollectionToBeReturned = manifestCollection;
									lastManifestReqTime = manifestReqTime;
									return manifestCollectionToBeReturned;
								}
							}
						}
					}
				}
			}
		}

		for (UrlMatchDef urlMatchDef : masterManifest.getSegUrlMatchDef()) {
			key = buildUriNameKey(urlMatchDef, request);

			if (StringUtils.isEmpty(key) && segmentManifestCollectionMap.selectKey("#EXT-X-BYTERANGE:") != null) {
				String[] range = stringParse.parse(request.getAllHeaders(), "Range: bytes=(\\d+)-(\\d+)");
				if (range != null) {
					String segName = String.format("#EXT-X-BYTERANGE:%.0f@%s", StringParse.stringToDouble(range[1], 0D) + 1, range[0]); // #EXT-X-BYTERANGE:207928@0 //
					key = segmentManifestCollectionMap.selectKey(segName);
					if (key.startsWith(segName)) {
						key = keyMatch(request, segName, urlMatchDef);
						byteRangeKey = segName;
					} else {
						LOG.error(String.format("Bad key match <%s> %s<>", segName, key));
						key = "";
					}
				}
			}
			String key1 = StringUtils.substringBefore(key, "|");
			try {
				List<Entry<String, String>> tempList = segmentManifestCollectionMap.entrySet().parallelStream()
						.filter(segmentManifestCollectionMapEntry -> segmentManifestCollectionMapEntry.getKey().contains(key))
						.collect(Collectors.toList());	

				if (tempList.size() > 1) {
					tempList.stream().forEach(x -> {
						manifestCollectionMap.entrySet().stream()
						.filter(y -> y.getKey().contains(x.getValue()))
						.forEach(y -> {
							y.getValue().entrySet().stream()
							.forEach( z -> {
								if (request.getTimeStamp() > z.getKey() && z.getKey() > identifiedManifestRequestTime) {
									identifiedManifestRequestTime = z.getKey();
									manifestCollectionToBeReturned = z.getValue();
								}
							});
						});
					});
				} else if (tempList.size() == 1 && !manifestCollectionMap.isEmpty()) {
					String key = tempList.get(0).getValue();

					manifestCollectionMap.entrySet().stream().filter(f -> f.getKey().endsWith(key)).forEach(outerMap -> {
						outerMap.getValue().entrySet().stream()
						.filter(f -> f.getKey() <= request.getTimeStamp() 
						&& f.getValue().getSegmentChildManifestTrie().containsKey(key1)
								)
						.forEach(innerMap -> {
							manifestCollectionToBeReturned = innerMap.getValue();
						});
					});
					if (manifestCollectionToBeReturned == null) {
						manifestCollectionMap.entrySet().stream().filter(f -> f.getKey().endsWith(key)).forEach(outerMap -> {
							outerMap.getValue().entrySet().stream()
							.filter(f -> f.getKey() <= request.getTimeStamp() 
									)
							.forEach(innerMap -> { 
								manifestCollectionToBeReturned = innerMap.getValue();
							});
						});}
				}
			} catch (Exception e) {
				LOG.error("Failed to locate manifestCollection: ", e);
			}

			if (manifestCollectionToBeReturned != null) {
				break;
			}
		}

		return manifestCollectionToBeReturned;
	}

	
	public String locatePatChildManifestKey(PatriciaTrie<ChildManifest> patMap, String key) {
		String foundKey = patMap.selectKey(key);
		if (foundKey.startsWith(key)) {
			return foundKey;
		} else {
			String priorKey = patMap.previousKey(foundKey);
			if (priorKey != null && priorKey.startsWith(key)) {
				return priorKey;
			}
		}
		return null;
	}
	
	private String locatePatKey(PatriciaTrie<String> patMap, String key, HttpRequestResponseInfo request, UrlMatchDef urlMatchDef) {
		if (patMap.size() > 0) {
			String foundKey = patMap.selectKey(key);
			if (foundKey != null && foundKey.startsWith(key) && matchKey(key, foundKey, request, urlMatchDef)) {
			    return foundKey;
			}
		}

		return null;
	}

	private boolean matchKey(String keyStr, String foundKey, HttpRequestResponseInfo request, UrlMatchDef urlMatchDef) {
		String[] matched = null;
		if (foundKey != null) {
    		if (foundKey.contains("(")) {
    			matched = stringParse.parse(keyStr, foundKey);
    			return matched != null;
    		} else if (request.getObjNameWithoutParams() != null && UrlMatchType.COUNT.equals(urlMatchDef.getUrlMatchType())) {
    		    // Matches the request's referenced url with the referenced manifest url (present as a suffix in foundKey)
    		    // Currently, there are only two types of UrlMatchDef -> FULL and COUNT
    		    // For FULL type, the current matching is skipped as we will only rely on the full URL and manifest timestamp, which is being validated in "locatePatKey" method
    		    //
    		    // COUNT or other types: matching the referenced urls after stripping off the UrlMatchDef length
    		    String manifestUrl = foundKey.split("\\|")[2];
    		    int manifestUrlPos = StringUtils.lastOrdinalIndexOf(manifestUrl, "/", 1);
    		    int requestUrlPos = StringUtils.lastOrdinalIndexOf(request.getObjNameWithoutParams(), "/", urlMatchDef.getUrlMatchLen() + 1);
    		    // Validate if we have the valid indexes
    		    if (manifestUrlPos == -1 || manifestUrlPos == manifestUrl.length() || requestUrlPos == -1 ||
    		            requestUrlPos == request.getObjNameWithoutParams().length()) {
    		        return false;
    		    }

    		    return manifestUrl.substring(0, manifestUrlPos).contentEquals(request.getObjNameWithoutParams().substring(0, requestUrlPos));
		    }
		}

		return true;
	}

	public String keyMatch(HttpRequestResponseInfo request, String targetKey, UrlMatchDef urlMatchDef) {
		String key = "";
		String[] tsString;
		String[] matched;
		String scanKey = selectKey(request, urlMatchDef);
		if (CollectionUtils.isEmpty(segmentManifestCollectionMap)) {
			LOG.error("segmentManifestCollectionMap is empty");
			return key;
		}
		if (scanKey.toUpperCase().startsWith(buildUriNameKey(urlMatchDef, request).toUpperCase())) {
			while ((key = segmentManifestCollectionMap.nextKey(scanKey)) != null && key.startsWith(StringUtils.substringBefore(scanKey, "|"))) {
				scanKey = key;
			}
			return scanKey;
		}
		
		scanKey = segmentManifestCollectionMap.selectKey(targetKey);
		
		if (scanKey.contains("(")) {
			matched = stringParse.parse(targetKey, StringUtils.substringBefore(scanKey, "|"));
			if (matched!=null) {
				return scanKey;
			}
		}
		if (scanKey.startsWith(targetKey)) {
			do {
				if ((tsString = stringParse.parse(scanKey, "\\|(.*)\\|")) != null) {
					double keyTS = Double.valueOf(tsString[0]);
					if (keyTS < request.getTimeStamp() && scanKey.startsWith(targetKey)) {
						if (!key.isEmpty()) {
							return key;
						} else {
							return scanKey;
						}
					} else {
						key = scanKey;
					}
				}
			} while ((scanKey = segmentManifestCollectionMap.nextKey(scanKey)) != null);
		}
		return key;
	}

	public String selectKey(HttpRequestResponseInfo request, UrlMatchDef urlMatchDef) {
		String key = buildUriNameKey(urlMatchDef, request);
		String scanKey = segmentManifestCollectionMap.selectKey(key);

		// TODO: Figure out the correct logic
		if (!domainSafeCompare(key, scanKey)) {
			String pKey;
			if ((pKey = segmentManifestCollectionMap.previousKey(scanKey)) != null) {
				scanKey = pKey;
			}
		}

		return scanKey;
	}
	
	private boolean domainSafeCompare(String key, String scanKey) {
		if (!scanKey.startsWith("http")) {
			return scanKey.startsWith(key);
		}

		int pos1 = StringUtils.ordinalIndexOf(scanKey, "/", 3);
		int pos2 = StringUtils.ordinalIndexOf(key, "/", 3);
		if (pos1 != pos2 || !scanKey.substring(0, pos1).toLowerCase().contentEquals(key.substring(0, pos2).toLowerCase())) {
			return false;
		} else {
			return scanKey.substring(pos1).contains(key.substring(pos2));
		}
	}

	protected String cleanUriName(String segmentUriName) {
		if (!segmentUriName.startsWith("http")) {
			return segmentUriName;
		}

		int pos = StringUtils.ordinalIndexOf(segmentUriName, "/", 3);
		if (pos > -1) {
			return segmentUriName.substring(0, pos).toLowerCase() + segmentUriName.substring(pos);
		}
		return segmentUriName.toLowerCase();
	}

	//VID-TODO maybe enum this too
	public String locateKey(UrlMatchDef urlMatchDef, HttpRequestResponseInfo request) {
		if (!StringUtils.isEmpty(byteRangeKey)) {
			return byteRangeKey;
		}
		String key = keyMatch(request, buildKey(request), urlMatchDef);
		if (key.isEmpty()) {
			key = buildUriNameKey(urlMatchDef, request);
		}
		key = StringUtils.substringBefore(key, "|");
		return key;
	}
	
	protected String formatTimeKey(double reqTimestamp) {
		return String.format("%8.3f", reqTimestamp);
	}
	
	protected void addToSegmentManifestCollectionMap(String segmentUriName) {
		if (segmentUriName != null) {
			// Sanity check before we create a key. Extract URI without query parameters.
			segmentUriName = Util.decodeUrlEncoding(segmentUriName);
			segmentUriName = StringUtils.substringBefore(segmentUriName, "?");
			
			String key = segmentUriName
					+ "|" + formatTimeKey(manifestCollection.getManifest().getRequestTime())
					+ "|" + manifestCollection.getManifest().getVideoName();

			if (!segmentManifestCollectionMap.containsKey(key)) {
				if (key.startsWith("|")) {
					LOG.error("problem: no segmenUriName");
				}
				segmentManifestCollectionMap.put(key, manifestCollection.getManifest().getVideoName());
			}
		}
	}
	
	public String dumpSegmentManifestCollectionTrie() {
		StringBuilder strbldr = new StringBuilder("segment_ManifestCollectionTrie:");
		strbldr.append(segmentManifestCollectionMap.size());
		strbldr.append(" entries");

		Set<Entry<String, String>> eset = segmentManifestCollectionMap.entrySet();
		
		eset.stream().forEach(x -> {
			strbldr.append("\nURL :" + x.getKey());
			strbldr.append("\t, manifest name:" + x.getValue());
		});

		return strbldr.toString();
	}
	
	public String dumpManifestCollection() {
		StringBuilder strbldr = new StringBuilder("ManifestCollection:");
		strbldr.append("Map<String, ManifestCollection> manifestCollectionMap\n  size:" + manifestCollectionMap.size());
		Iterator<Entry<String, Map<Double, ManifestCollection>>> itr = manifestCollectionMap.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<String, Map<Double, ManifestCollection>> val = itr.next();
			Map<Double, ManifestCollection> innerMap = val.getValue();
			Iterator<Entry<Double, ManifestCollection>> innerItr = innerMap.entrySet().iterator();
			strbldr.append("\n\tVideoStream:" + val.getKey() + manifestCollection);
			while (innerItr.hasNext()) {
				Entry<Double, ManifestCollection> innerVal = innerItr.next();
				ManifestCollection manifestCollection = innerVal.getValue();
				strbldr.append("\n\tVideoStream:" + innerVal.getKey() + manifestCollection);
			}			
		}			
		
		return strbldr.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder strbldr = new StringBuilder();
		strbldr.append(dumpManifestCollection());
		strbldr.append(dumpSegmentManifestCollectionTrie());
		return strbldr.toString();
	}

	public abstract String buildSegmentName(HttpRequestResponseInfo request, String extension);

}