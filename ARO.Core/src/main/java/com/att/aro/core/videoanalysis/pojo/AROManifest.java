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

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.att.aro.core.AROConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;

public class AROManifest {

	ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	ILogger log = (ILogger) context.getBean("logger");
	
	IStringParse stringParse = context.getBean(IStringParse.class);
	
	IHttpRequestResponseHelper reqhelper = (IHttpRequestResponseHelper) context.getBean("httpRequestResponseHelper");
	Session session;                // session that manifest arrived on
	
	VideoType eventType;            // DASH, HLS, Unknown
	String videoName = "";          //
	String exten = "";
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
	
	double delay;

	private TreeMap<String, VideoData> videoDataMap;
	byte[] content;
	private String videoPath;

	VideoAnalysisConfig vConfig;


	/**
	 * Initializes an instance of the VideoEvent class, using the specified event type, 
	 * press time, and release time.
	 * 
	 * @param eventType The event type. One of the values of the VideoEventType enumeration.
	 * @param manifestTime The time at which the event was initiated (such as a key being pressed down).
	 * @param releaseTime The time at which the chunk finished downloading.
	 */
	public AROManifest(VideoType eventType, HttpRequestResponseInfo req, String videoPath) {

		this.eventType = eventType;
		this.endTime = 0;
		this.timeLength = 0;
		this.videoPath = videoPath;
		if (req != null) {
			this.beginTime = req.getTimeStamp();
			this.session = req.getSession();
			this.uri = req.getAssocReqResp().getObjUri();
		}
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("AROManifest :");
		strblr.append(eventType);
		strblr.append(", Name :");		   strblr.append(getVideoName());
		strblr.append(", Encryption :");   strblr.append(getEncryption());
		strblr.append(", URIs :");         strblr.append(uri != null ? uri.getRawPath() : "null");
		strblr.append(", segmentList  :"); strblr.append(segmentList.size());
		strblr.append(", segmentCount :"); strblr.append(getSegmentCount());
		strblr.append(", duration :")    ; strblr.append(getDuration());
		strblr.append(", \n\tbitrates :"); strblr.append(bitrateMap);
		
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
		// FIXME - this needs a real comparison for two byte arrays
		return !(content == null || (new String(content)).equals(new String(data)));
	}
		
	/**
	 * Add VideoEvent to both videoEventList and segmentEventList.
	 * 
	 * @param segment
	 * @param timestamp
	 * @param videoEvent
	 */
	public void addVideoEvent(double segment, double timestamp, VideoEvent videoEvent) {

		String key = String.format("%010.4f:%08.0f", timestamp, segment );
		videoEventList.put(key, videoEvent);
		if (segment != -1) {
//			key = String.format("%08.0f:%010.4f", segment, timestamp); // original key format
			key = generateVideoEventKey(segment, timestamp, videoEvent.getQuality());
			segmentEventList.put(key, videoEvent);
		}
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

	public int parseSegment(String fullName, VideoEventData ved) {
		if (ved.getByteStart() == null) {
			return -1;
		}
		return Integer.valueOf(ved.getByteStart());//TODO handle exception, double check logic
	}
	
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

	public boolean isVideoType(VideoType eventType) {
		return this.eventType.equals(eventType);
	}

	public int getSegIncremental() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Integer getSegment(String segName) {
		// TODO Auto-generated method stub
		return -1;
	}

	public Integer getSegment(String[] voValues) {
		return -1;
		
	}

	public void setConfig(VideoAnalysisConfig vConfig) {
		this.vConfig = vConfig;
		
	}

	
}
