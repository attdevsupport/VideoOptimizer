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
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.Manifest.ManifestType;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

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

	public Manifest create(HttpRequestResponseInfo request, byte[] data, String videoPath) {
		crc32 = new CRC32();
		manifest = new Manifest();
		manifest.setRequest(request);
		manifest.setVideoName(StringUtils.substringBefore(request.getObjNameWithoutParams(),";"));
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

	/**
	 * Locate and return a segment number.
	 * 
	 * @param request
	 * @return segment number or -1 if not found
	 */
	public int getSegmentNumber(HttpRequestResponseInfo request) {
		SegmentInfo segmentInfo = getSegmentInfo(request);
		int segmentNumber = -1;
		if (segmentInfo != null) {
			segmentNumber = segmentInfo.getSegmentID();
		}
		return segmentNumber;
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
	
	public abstract void parseManifestData(Manifest newManifest, byte[] data);

	protected void assignQuality(ManifestCollection manifestCollection) {
		int quality = 1;
		for ( ChildManifest ChildManifest: manifestCollection.getBandwidthMap().values()) {
			ChildManifest.setQuality(quality++);
		}
	}
	
	protected ChildManifest locateChildManifest(Manifest manifest) {
		childManifest = null;
		HttpRequestResponseInfo request = manifest.getRequest();
		String uriReferencePath = request.getObjNameWithoutParams();
		int sectionCount = manifestCollection.getChildUriNameSectionCount();
		
		referenceKey = uriReferencePath;
		if (!CollectionUtils.isEmpty(manifestCollection.getUriNameChildMap())) {
			int[] matchPoints = getStringPositions(uriReferencePath, "/");
			int count = matchPoints.length;
			int point = count - sectionCount - 1;
			if (point < 0) {
				point = 0;
			}
			referenceKey = uriReferencePath.substring(matchPoints[point] + 1);
			childManifest = manifestCollection.getUriNameChildMap().entrySet().stream().filter(x -> x.getKey().contains(referenceKey)).findFirst().map(x-> x.getValue()).orElseGet(()-> null);
		}
		if (childManifest == null) {
			// create an adhoc childManifest
			childManifest = createChildManifest(manifest, "", referenceKey);
			manifestCollection.addToTimestampChildManifestMap(request.getTimeStamp(), childManifest);
			childManifest.setVideo(false);
		}
		return childManifest;
	}

	private int[] getStringPositions(String inputStr, String matchStr) {
		int count = StringUtils.countMatches(inputStr, matchStr);
		if (count >= 0) {
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
		segmentInfo.setVideo(childManifest.isVideo());
		segmentInfo.setDuration(StringParse.findLabeledDoubleFromString(":", ",", parameters));
		segmentInfo.setSegmentID(segmentID);
		segmentInfo.setQuality(String.format("%.0f", (double) childManifest.getQuality()));
		if (segmentInfo.isVideo() && (segmentInfo.getSegmentID() == 0 && manifest.isVideoTypeFamily(VideoType.DASH))) {
			segmentInfo.setVideo(false);
		}
		return segmentInfo;
	}

	/**
	 * Switch Manifest if there is no manifest for key and timestamp
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
		}
	}

	protected ChildManifest createChildManifest(Manifest manifest, String parameters, String childUriName) {
		ChildManifest childManifest = new ChildManifest();
		childManifest.setManifest(manifest);
		childManifest.setUriName(childUriName);
		childUriName = childUriName.replaceAll("%2f", "/");
		manifestCollection.addToUriNameChildMap(StringUtils.countMatches(childUriName, "/"), childUriName, childManifest);
		return childManifest;
	}

	public boolean isSegmentOrder() {
		return manifestCollection.isSegmentOrder();
	}

	public ManifestCollection findManifest(HttpRequestResponseInfo request) {
		
		LOG.debug("locating manifest for :" + request.getObjUri());
		
		identifiedManifestRequestTime = 0;
		byteRangeKey = "";
		
		key = keyMatch(request, buildKey(request));
		if (StringUtils.isEmpty(key) && !segmentManifestCollectionMap.selectKey("#EXT-X-BYTERANGE:").isEmpty()) {
			String[] range = stringParse.parse(request.getAllHeaders(), "Range: bytes=(\\d+)-(\\d+)");
			if (range != null) {
				String segName = String.format("#EXT-X-BYTERANGE:%.0f@%s", StringParse.stringToDouble(range[1], 0D) + 1, range[0]); // #EXT-X-BYTERANGE:207928@0 //
				key = segmentManifestCollectionMap.selectKey(segName);
				if (key.startsWith(segName)) {
					key = keyMatch(request, segName);
					byteRangeKey = segName;
				} else {
					LOG.debug(String.format("Bad key match <%s> %s<>", segName, key));
					key = "";
				}
			}
		}
		String key1 = StringUtils.substringBefore(key, "|");
		LOG.debug("Looking for +<" + key + ">+ VALUE=" + segmentManifestCollectionMap.get(key));
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
						if (innerMap.getValue().getSegmentChildManifestTrie().containsKey(key1)) {
							LOG.debug("found :"+innerMap.getValue().getSegmentChildManifestTrie().get(key1).getUriName());
						}
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
			} else {
				return null;
			}

		} catch (Exception e) {
			LOG.error("Failed to locate manifestCollection: ", e);
		}
		return manifestCollectionToBeReturned;
	}

	public String keyMatch(HttpRequestResponseInfo request, String targetKey) {
		String key = "";
		Set<Entry<String, String>> eSet = segmentManifestCollectionMap.entrySet();
		for (Entry<String, String> sKey : eSet) {
			String[] found = stringParse.parse(targetKey, StringUtils.substringBefore(sKey.getKey(), "|"));
			if (found != null || sKey.getKey().contains(targetKey)) {
				String[] tsString;
				if ((tsString = stringParse.parse(sKey.getKey(), "\\|(.*)\\|")) != null) {
					double keyTS = Double.valueOf(tsString[0]);
					if (request.getTimeStamp()<keyTS) {
						continue;
					}
				}
				key = sKey.getKey();
			} else {
				if (!StringUtils.isEmpty(key)) {
					break;
				}
			}
		}
		return key;
	}
	
	public String locateKey(HttpRequestResponseInfo request) {
		if (!StringUtils.isEmpty(byteRangeKey)) {
			return byteRangeKey;
		}
		String key = keyMatch(request, buildKey(request));
		key = StringUtils.substringBefore(key, "|");
		return key;
	}
	
	protected String formatTimeKey(double reqTimestamp) {
		return String.format("%.3f", reqTimestamp);
	}
	
	protected void addToSegmentManifestCollectionMap(String segmentUriName) {
		String key = segmentUriName 
				+ "|" + formatTimeKey(manifest.getRequestTime()) 
				+ "|" + manifestCollection.getManifest().getVideoName();
		if (!segmentManifestCollectionMap.containsKey(key)) {
			if (key.startsWith("|")) {
				LOG.error("problem: no segmenUriName");
			}
			segmentManifestCollectionMap.put(key, manifestCollection.getManifest().getVideoName());
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
