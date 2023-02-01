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
package com.att.aro.core.videoanalysis.parsers;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.att.aro.core.AROConfig;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;

import lombok.Data;

@Data
public class DashParser {

	ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	protected static final Logger LOG = LogManager.getLogger(DashParser.class.getName());	
	IStringParse stringParse = context.getBean(IStringParse.class);
	
	Manifest manifest;
	ManifestCollection manifestCollection;
	ChildManifest childManifest;
	IHttpRequestResponseHelper reqhelper = (IHttpRequestResponseHelper) context.getBean("httpRequestResponseHelper");
	Session session;                					// session that manifest arrived on
	VideoFormat videoFormat = VideoFormat.UNKNOWN;
	VideoType videoType = VideoType.UNKNOWN;            // DASH, HLS, Unknown
	String videoName = "";
	String exten = "";
	Double duration = 0D;
	Double timeScale = 0D;
	TreeMap<String, Double> bitrateMap = new TreeMap<>();
	/**
	 *  milliseconds UTC
	 */
	double requestTime;
	double timeLength;		                           // in milliseconds

	protected HttpRequestResponseInfo response;
	double beginTime;		                           // timestamp of manifest request          
	double endTime;			                           // timestamp of manifest download completed
	URI uri;				                           // URI of GET request                     
	String uriStr = "";
	protected boolean autoCount =  false;
	private double segmentCount;			           // segments range from 0 to segmentCount
	String encryption = "";
	
	/** <pre>
	 * key definition timestamp-segment
	 * value VideoEvent
	 */
	TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
	
	/** <pre>
	 * key definition segment-quality-timestamp
	 * value VideoEvent
	 */
	TreeMap<String, VideoEvent> segmentEventList = new TreeMap<>();
	
	/** <pre>
	 * key videoName.extention
	 * value segment number
	 */
	TreeMap<String, Integer> segmentList = new TreeMap<>();
	
	TreeMap<Integer, String> durationList = new TreeMap<>();
	
	/**
	 * delay is point in seconds video.mp4 or mov for when first segment starts to play
	 */
	double delay;
	
	/**
	 * The delay from request of manifest to first segment starts to play
	 */
	double startupDelay;

	boolean valid = true;
	
	byte[] content;
	private String videoPath;

	VideoAnalysisConfig vConfig;

	private boolean selected = false;
	private boolean activeState = true;
	
	/**
	 * Segment associated with StartupDelay
	 */
	private VideoEvent videoEvent;
	
	/**
	 * Indicates if video segment metadata such as bitrate has been extracted successfully
	 */
	private boolean videoMetaDataExtracted = false;
	
	/**
	 * 
	 * @param videoType
	 * @param manifest
	 * @param manifestCollection 
	 * @param childManifest 
	 */
	public DashParser(VideoType videoType, Manifest manifest, ManifestCollection manifestCollection, ChildManifest childManifest) {
		this.manifestCollection = manifestCollection;
		this.manifest = manifest;
		this.childManifest = childManifest;
		this.response = manifest.getRequest().getAssocReqResp();
		this.videoType = videoType;
		this.endTime = 0;
		this.timeLength = 0;
		this.videoPath = manifest.getVideoPath();
		setVideoName(manifest.getVideoName());
		if (response != null) {
			PacketInfo fdp = response.getAssocReqResp().getFirstDataPacket();
			this.requestTime = fdp != null ? fdp.getPacket().getTimeStamp() : 0; // milliseconds UTC
			if (this.beginTime == 0) {
				this.beginTime = response.getTimeStamp();
			}
			if (response.getLastDataPacket() != null) {
				setEndTime(this.beginTime + estimateResponseEnd(response));
				this.session = response.getSession();
				this.uri = response.getAssocReqResp().getObjUri();
			}
		}
	}

	/**
	 * Estimate Response download time duration (delta)
	 * @param resp
	 * @return duration delta
	 */
	public double estimateResponseEnd(HttpRequestResponseInfo response) {
		double delta = 0;
		Packet packet = response.getLastDataPacket().getPacket().getNextPacketInSession();
		if (packet != null) {
			delta = packet.getTimeStamp() - response.getLastDataPacket().getPacket().getTimeStamp();
		}
		if (delta == 0.0) {
			delta = .004;
		}
		return delta;
	}

	public void updateManifest(MpdBase manifest) {
		// do nothing here
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\n\tAROManifest :");
		strblr.append(videoType);
		strblr.append(", Name :");
		strblr.append(getVideoName());
		strblr.append(", Encryption :");
		strblr.append(getEncryption());
		strblr.append(", URIs :");
		strblr.append(uri != null ? uri.getRawPath() : "null");
		if (!segmentList.isEmpty()) {
			strblr.append("\n\t, segmentList  :");
			strblr.append(segmentList.size());
			strblr.append(segmentList);
			strblr.append("\n\t, durationList  :");
			strblr.append(durationList.size());
			strblr.append(durationList);
		}
		strblr.append("\n\t, segmentCount :");
		strblr.append(getSegmentCount());
		strblr.append(", duration :");
		strblr.append(getDuration());
		strblr.append("\n\t, bitrates :");
		strblr.append(bitrateMap);

		strblr.append(", \n\tvideoEvents [\n\t ");
		if (!videoEventList.isEmpty()) {
			for (VideoEvent videoEvent : videoEventList.values()) {
				strblr.append(videoEvent);
				strblr.append("\n\t ");
			}
		}
		strblr.append("]");
		
		strblr.append(", \n\tsegmentEvents [\n\t ");
		if (!segmentEventList.isEmpty()) {
			for (VideoEvent videoEvent : segmentEventList.values()) {
				strblr.append(videoEvent);
				strblr.append("\n\t ");
			}
		}
		strblr.append("]");
		
		return strblr.toString();
	}
	
	public boolean checkContent(byte[] data) {
		// FIXME - this needs a real comparison for the two byte arrays instead of through String comparison
		return !(Arrays.equals(content, data));//new String(content)).equals(new String(data)));
	}
		
	/**
	 * Add VideoEvent to both videoEventList and segmentEventList.
	 * 
	 * @param segment
	 * @param timestamp
	 * @param videoEvent
	 */
	public void addVideoEvent(double segment, double timestamp, VideoEvent videoEvent) {

		String key = generateEventKey(timestamp, segment);
		videoEventList.put(key, videoEvent);
		this.selected=true;
		if (segment != -1) {
//			key = String.format("%08.0f:%010.4f", segment, timestamp); // original key format
			key = generateVideoEventKey(segment, timestamp, videoEvent.getQuality());
			segmentEventList.put(key, videoEvent);
		}
	}

	/**
	 * <pre>
	 * Generates a key
	 *	 s = segment len = 10
	 *	 t = timestamp len = 11
	 *   format sssssssssstttttttt
	 *   
	 * @param segment
	 * @param timestamp
	 * @param quality
	 * @return
	 */
	public static String generateEventKey(double segment, double timestamp) {
		return String.format("%010.4f:%08.0f", timestamp, segment );
	}

	/**
	 * <pre>
	 * Generates a key
	 *	 s = segment len = 8
	 *	 Q = quality variable length
	 *	 t = timestamp len = 11
	 *   format ssssssssQualitytttttt.tttt
	 *   
	 * @param segment
	 * @param timestamp
	 * @param quality
	 * @return
	 */
	public String generateVideoEventKey(double segment, double timestamp, String quality) {
		String key;
		key = String.format("%08.0f:%s:%010.4f", segment, quality, timestamp);
		return key;
	}
	/**
	 * Uses singleton pattern. First to assign name that is no null wins.
	 * @param videoName
	 */
	public void singletonSetVideoName(String newVideoName) {
		if (StringUtils.isNotEmpty(newVideoName) && this.videoName.isEmpty()) {
			this.videoName = newVideoName;
		}
	}

	/**
	 * For Amazon and DTV keys will look like a number from 1 and up
	 * 
	 * @param key
	 * @return bitrate
	 */
	public Double getBitrate(String key) {
		Double rate;
		try {
			rate = bitrateMap.get(key.toUpperCase());
		} catch (Exception e) {
			rate = 0D;
		}
		return rate;
	}

	public double getSegmentCount() {
		return segmentCount > 0 ? segmentCount : segmentEventList.size();
	}

	public String getDuration(String segment) {
		Integer seg;
		try {
			seg = Integer.valueOf(segment);
		} catch (NumberFormatException e) {
			return "-1";
		}
		return getDuration(seg);
	}

	public String getDuration(Integer segName) {
		if (!segmentList.isEmpty()) {
			String val = durationList.get(segName);
			if (val != null) {
				return val;
			}
		}
		return "0";
	}

	public boolean isVideoType(VideoType eventType) {
		return this.videoType.equals(eventType);
	}

	public int getSegIncremental() {
		return 0;
	}

	public Integer getSegment(String segName) {
		return -1;
	}

	public Integer getSegment(String[] voValues) {
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DashParser){
			if (this == obj){
				return true;
			}		
			DashParser manifestObj = (DashParser) obj;
			if (!videoName.equals(manifestObj.getVideoName())){
				return false;
			}
			if(!getVideoEventsBySegment().equals(manifestObj.getVideoEventsBySegment())){
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;

		result = prime * result + videoName.hashCode();
		result = prime * result + uriStr.hashCode();
		temp = Double.doubleToLongBits(beginTime);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		
		return result;
	}

	/**<pre>
	 * Set to true if videoMetaDataExtracted is true
	 * Will not set to false, guards against unsetting if last segment cannot be read for metadata
	 * 
	 * @param videoMetaDataExtracted
	 */
	public void setVideoMetaDataExtracted(boolean videoMetaDataExtracted) {
		if (!this.videoMetaDataExtracted) {
			this.videoMetaDataExtracted = videoMetaDataExtracted;
		}
	}
	
	public VideoEvent getVideoEventBySegment(String key){
		return segmentEventList.get(key);
	}
	
	/**
	 * Retrieve VideoEvents by segment
	 * @return
	 */
	public Collection<VideoEvent> getVideoEventsBySegment(){
		return segmentEventList.values();
	}
}
