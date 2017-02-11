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
package com.att.aro.core.videoanalysis.pojo;

import java.net.URI;
import java.util.Collection;
import java.util.TreeMap;

import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

public class AROManifest {

	Session session;                // session that manifest arrived on
	
	VideoType eventType;            // DASH, HLS, Unknown
	String videoName = "";               //
	double duration = 0D;
	double timeScale = 0D;
	TreeMap<String, Double> bitrateMap = new TreeMap<>();
	double timeLength;		        // in milliseconds
	double beginTime;		        // timestamp of manifest request
	double endTime;			        // timestamp of manifest session end ?why
	URI uri;				        // URI of GET request
	String uriStr = "";
	double segmentCount;			// segments range from 0 to segmentCount
	String encryption = "";
	
	TreeMap<String, VideoEvent> videoEventList = new TreeMap<>();
	TreeMap<String, VideoEvent> segmentEventList = new TreeMap<>();
	double delay;

	private TreeMap<String, VideoData> videoDataMap;

	/**
	 * Initializes an instance of the VideoEvent class, using the specified event type, 
	 * press time, and release time.
	 * 
	 * @param eventType The event type. One of the values of the VideoEventType enumeration.
	 * @param manifestTime The time at which the event was initiated (such as a key being pressed down).
	 * @param releaseTime The time at which the chunk finished downloading.
	 */
	public AROManifest(VideoType eventType, HttpRequestResponseInfo rrInfo) {
		this.eventType = eventType;
		this.endTime = 0;
		this.timeLength = 0;
		if (rrInfo != null) {
			this.beginTime = rrInfo.getTimeStamp();
			this.session = rrInfo.getSession();
			this.uri = rrInfo.getAssocReqResp().getObjUri();
		}
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("AROManifest :");
		strblr.append(eventType);
		strblr.append(", Name :");		   strblr.append(getVideoName());
		strblr.append(", Encryption :");   strblr.append(getEncryption());
		strblr.append(", URIs :");         strblr.append(uri != null ? uri.getRawPath() : "null");
		strblr.append(", segmentCount :"); strblr.append(getSegmentCount());
		strblr.append(", \n\tbitrates :"); strblr.append(bitrateMap);
		strblr.append(", \n\tvideoEvents [\n\t ");
		if (!videoEventList.isEmpty()) {
			for (VideoEvent videoEvent : videoEventList.values()) {
				strblr.append(videoEvent);
				strblr.append("\n\t ");
			}
		}
		strblr.append(", \n\tsegmentEvents [\n\t ");
		strblr.append("]");
		if (!segmentEventList.isEmpty()) {
			for (VideoEvent videoEvent : segmentEventList.values()) {
				strblr.append(videoEvent);
				strblr.append("\n\t ");
			}
		}
		strblr.append("]");
		return strblr.toString();
	}
	
	/**
	 * Add VideoEvent to both videoEventList and segmentEventList.
	 * 
	 * @param segment
	 * @param timestamp
	 * @param videoEvent
	 */
	public void addVideoEvent(double segment, double timestamp, VideoEvent videoEvent) {

		String key = String.format("%010.4f:%08.0f", timestamp,segment );
		videoEventList.put(key, videoEvent);
		if (segment != -1) {
			key = String.format("%08.0f:%f", segment, timestamp);
			segmentEventList.put(key, videoEvent);
		}
	}

	public VideoEvent getVideoEventBySegment(String key){
		return segmentEventList.get(key);
	}
	
	/**
	 * Retrive VideoEvents by segment
	 * @return
	 */
	public Collection<VideoEvent> getVideoEventsBySegment(){
		return segmentEventList.values();
	}
	
	public TreeMap<String, VideoEvent> getSegmentEventList(){
		return segmentEventList;
	}
	
	/**
	 * Retrieve VideoEvents by download timestamp
	 * @return
	 */
	public TreeMap<String, VideoEvent> getVideoEventList() {
		return videoEventList;
	}

	public VideoType getEventType() {
		return eventType;
	}

	public void setEventType(VideoType eventType) {
		this.eventType = eventType;
	}

	public double getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(double beginTime) {
		this.beginTime = beginTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public URI getUrl() {
		return uri;
	}

	public void setUrl(URI url) {
		this.uri = url;
	}

	public double getDelay() {
		return delay;
	}

	/**
	 * Set the startup delay
	 * 
	 * @param delay in seconds
	 */
	public void setDelay(double delay) {
		this.delay = delay;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public String getVideoName() {
		return videoName;
	}

	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}

	public String getEncryption() {
		return encryption;
	}

	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}

	/**
	 * For Amazon and DTV keys will look like a number from 1 and up
	 * 
	 * @param key
	 * @return bitrate
	 */
	public Double getBitrate(String key) {
		return bitrateMap.get(key);
	}

	public TreeMap<String, Double> getBitrateMap() {
		return bitrateMap;
	}

	public void setBitrateMap(TreeMap<String, Double> bitrateMap) {
		this.bitrateMap = bitrateMap;
	}

	public String getUriStr() {
		return uriStr;
	}

	public void setUriStr(String uriStr) {
		this.uriStr = uriStr;
	}

	public double getSegmentCount() {
		return segmentCount;
	}

	public void setSegmentCount(double segmentCount) {
		this.segmentCount = segmentCount;
	}

	public double getTimeLength() {
		return timeLength;
	}

	public void setTimeLength(double timeLength) {
		this.timeLength = timeLength;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public int parseSegment(String fullName, ByteRange range) {
		if (range==null){
			return -1;
		}
		return range.getBeginByte();
	}

//	public String getMinBufferTime() {
//		String bufferTime = this.mpdOut.getMinBufferTime();
//		if (bufferTime != null || (!bufferTime.trim().equals(""))) {
//			return bufferTime.substring(2, bufferTime.length() - 1);
//		}
//		return null;
//	}

	
	public void addVData(VideoData vData) {
		if (videoDataMap == null){
			videoDataMap = new TreeMap<>();
		}
		videoDataMap.put( vData.getQuality(),  vData);
	}

	public VideoData getVData(String quality) {
		if (videoDataMap!=null){
			return videoDataMap.get(quality);
		}
		return null;
	}
	public String getTimeScale(String videoName) {
		return "";
	}

	public String getDuration(String videoName) {
		return "";
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public double getTimeScale() {
		return timeScale;
	}

	public void setTimeScale(double timeScale) {
		this.timeScale = timeScale;
	}
}
