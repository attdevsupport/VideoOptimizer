/*
 *  Copyright 2014 AT&T
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.util.ImageHelper;

/**
 * <pre>
 * Encapsulates the data from a video event.
 * A Video Event will contain a manifest and one to many video chunks
 *
 * 2016
 *
 *
 */
public class VideoEvent /*implements Serializable*/ {
//	private static final long serialVersionUID = 1L;
	
	private byte[] defaultImage = new byte[] {
    		73,73,42,0,120,0,0,0,-128,57,31,-128,16,3,-4,6,22,0,66,92,1,7,-16,0,6,-17,10,-127,-33,-32,7,80,76,0,-3,2,59,64,-79,112,35,-92,36,121,62,-125,93,111,-16,64,37,-28,-10,86,-99,-46,-60,-9,64,16,16,-77,35,59,-116,-83,18,-70,17,-60,22,117,18,-108,15,18,-53,52,-122,-103,1,-71,20,12,50,10,-107,8,103,9,45,-47,-114,-107,-128,68,30,69,102,-119,
    		-33,-51,50,50,-66,18,0,104,60,31,96,16,40,40,92,-4,0,0,96,32,21,0,0,1,3,0,1,0,0,0,10,0,0,0,1,1,3,0,1,0,0,0,10,0,0,0,2,1,3,0,1,0,0,0,8,0,0,0,3,1,3,0,1,0,0,0,5,0,0,0,6,1,3,0,1,0,0,0,1,0,0,0,10,1,3,0,1,0,0,0,1,0,0,0,13,1,2,0,124,0,
    		0,0,-54,1,0,0,14,1,2,0,18,0,0,0,70,2,0,0,17,1,4,0,1,0,0,0,8,0,0,0,18,1,3,0,1,0,0,0,1,0,0,0,21,1,3,0,1,0,0,0,1,0,0,0,22,1,3,0,1,0,0,0,51,3,0,0,23,1,4,0,1,0,0,0,112,0,0,0,26,1,5,0,1,0,0,0,122,1,0,0,27,1,5,0,1,0,0,0,-126,1,
    		0,0,28,1,3,0,1,0,0,0,1,0,0,0,40,1,3,0,1,0,0,0,3,0,0,0,41,1,3,0,2,0,0,0,0,0,1,0,61,1,3,0,1,0,0,0,2,0,0,0,62,1,5,0,2,0,0,0,-70,1,0,0,63,1,5,0,6,0,0,0,-118,1,0,0,0,0,0,0,-1,-1,-1,-1,96,-33,42,2,-1,-1,-1,-1,96,-33,42,2,0,10,-41,-93,-1,-1,
    		-1,-1,-128,-31,122,84,-1,-1,-1,-1,0,-51,-52,76,-1,-1,-1,-1,0,-102,-103,-103,-1,-1,-1,-1,-128,102,102,38,-1,-1,-1,-1,-16,40,92,15,-1,-1,-1,-1,-128,27,13,80,-1,-1,-1,-1,0,88,57,84,-1,-1,-1,-1,47,115,114,118,47,119,119,119,47,118,104,111,115,116,115,47,111,110,108,105,110,101,45,99,111,110,118,101,114,116,46,99,111,109,47,115,97,118,101,47,113,117,
    		101,117,101,100,47,48,47,56,47,97,47,48,56,97,100,53,56,99,57,100,55,99,56,50,97,52,102,53,102,101,49,97,54,98,97,51,100,100,102,98,101,54,99,47,105,110,116,101,114,109,101,100,105,97,116,101,49,47,111,95,98,51,98,50,48,97,100,97,48,99,48,101,49,51,57,52,46,116,105,102,102,0,67,114,101,97,116,101,100,32,119,105,116,104,32,71,73,77,80,0	
    };

	private BufferedImage thumbnail;
//	private int maxH = 20;
	private int maxW = 20;

	private VideoType eventType;
	private URI uri;			// address  of manifest request
	private BufferedImage imageOriginal;
	private double bitrate;
	private double packetCount ;
	private HttpRequestResponseInfo response;
	private double mdatSize;
	private ArrayList<ByteRange> rangeList;
	private double segment;
	private String quality;
	private double startTS;
	private double endTS;
	private double duration;
	private double segmentStartTime;
	
	/**
	 * The VideoEvent.VideoEventType Enumeration specifies constant values that describe 
	 * different types of user generated events. This enumeration is part of the VideoEvent 
	 * class.
	 */
	public enum VideoType {
		DASH, HLS, UNKNOWN
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("VideoType :"); 
		strblr.append(eventType.toString());
//		strblr.append(", URI:"); strblr.append(response.getAssocReqResp().getObjUri());
		strblr.append(", Segment:"); strblr.append(segment);
		strblr.append(", Quality:"); strblr.append(quality);
		strblr.append(", Byte Range:"); strblr.append(rangeList);
		strblr.append(", bitrate:"); strblr.append(bitrate);
		strblr.append(", mdatSize:"); strblr.append(mdatSize);
		strblr.append(", duration:"); strblr.append(duration);
		strblr.append(", dlSize:"); strblr.append(response.getContentLength());
		strblr.append(", dlTime:"); strblr.append(String.format("%.6f", getDLTime()*1e0));
		return strblr.toString();
	}
	
	public BufferedImage getThumbnail() {
//		System.out.println("thumbnail  h/w :" + thumbnail.getHeight() + "/" + thumbnail.getWidth());
		return thumbnail;
	}

	public void setThumbnail(byte[] imgArray) {
		byte[] imageArray = imgArray == null ? defaultImage : imgArray;

		BufferedImage image = null;
		try {
			image = ImageHelper.getImageFromByte(imageArray);
		} catch (IOException e) {
			if (imageArray != defaultImage) {
				try {
					image = ImageHelper.getImageFromByte(defaultImage);
				} catch (IOException e1) {
					return;
				}
			}
		}
		thumbnail = image;
		 if (image != null) {
		 int height = image.getHeight();
		 int width = image.getWidth();
		 imageOriginal = image;

		 // int ratio = height / width;
//		 int targetWidth = maxH;
//		 int targetHeight = maxH * height / width;
		 int targetHeight = maxW;
		 int targetWidth = maxW * width / height;
		 
		 thumbnail = ImageHelper.resize(image, targetWidth, targetHeight);
		 }
	}


	/**
	 * Initializes an instance of the VideoEvent class, using the specified event type, 
	 * press time, and release time.
	 * 
	 * @param eventType The event type. One of the values of the VideoEventType enumeration.
	 * @param imageArray a thumbnail image from first "frame" of chunk
	 * @param segment 
	 * @param quality 
	 * @param beginByte
	 * @param endByte
	 * @param bitrate
	 * @param mdatSize
	 * @param double1 
	 * @param response
	 */
	public VideoEvent(byte[] imageArray
					, VideoType eventType
					, double segment
					, String quality
					, ArrayList<ByteRange> rangeList
					, double bitrate
					, double duration
					, double segmentStartTime
					, double mdatSize
					, HttpRequestResponseInfo response
					) {
		this.eventType = eventType;
		this.rangeList = rangeList;
		this.bitrate = bitrate;
		this.quality = quality;
		this.segment = segment;
		this.mdatSize = mdatSize;
		this.duration = duration;
		this.segmentStartTime = segmentStartTime;
		this.response = response;

		if (response.getContentOffsetLength() == null) {
			this.setPacketCount(0);
		} else {
			this.setPacketCount(response.getContentOffsetLength().values().size());
		}

		this.startTS = response.getTimeStamp();
		double delta = 0;
		Packet packet = response.getLastDataPacket().getPacket().getNextPacketInSession();
		if (packet != null) {
			delta = packet.getTimeStamp() - response.getLastDataPacket().getPacket().getTimeStamp();
		}
		if (delta == 0.0) {
			delta = .004;
		}
		this.endTS = response.getLastDataPacket().getTimeStamp() + delta;

		setThumbnail(imageArray);

	}	

	public Session getSession() {
		return response.getDirection() == HttpDirection.REQUEST ? response.getSession() : response.getAssocReqResp().getSession();
	}
	
	/**
	 * amount of time that download took
	 * 
	 * @return
	 */
	public double getDLTime() {
//		return response.getLastDataPacket().getTimeStamp()-response.getTimeStamp();
		return endTS - startTS;
	}
	
	/**
	 * Timestamp at beginning of download
	 * 
	 * @return
	 */
	public double getDLTimeStamp() {
//		return response.getTimeStamp();
		return startTS;
	}
	
	/**
	 * Timestamp of the end of download
	 * 
	 * @return
	 */
	public double getDLLastTimestamp(){
//		return response.getLastDataPacket().getTimeStamp();
		return endTS;
	}
	
	public double getSize() {
		return mdatSize;
	}

	public VideoType getEventType() {
		return eventType;
	}

	public void setEventType(VideoType eventType) {
		this.eventType = eventType;
	}

	public URI getUrl() {
		return uri;
	}

	public void setUrl(URI url) {
		this.uri = url;
	}

	public double getBitrate() {
		return bitrate;
	}

	public void setBitrate(double bitrate) {
		this.bitrate = bitrate;
	}

	/**
	 * Returns the duration in seconds
	 * @return seconds
	 */
	public double getDuration() {
		return duration;
	}

	public double getSegmentStartTime() {
		return segmentStartTime;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public double getSegment() {
		return segment;
	}

	public void setSegment(int segment) {
		this.segment = segment;
	}

	private double getBeginByte() {
		if (rangeList.isEmpty()){
			return 0;
		}
		return rangeList.get(0).getBeginByte();
	}

	private double getEndByte() {
		if (rangeList.isEmpty()){
			return 0;
		}
		return rangeList.get(0).getEndByte();
	}

	/**
	 * Deliver size in bytes, may come from mdat size instead of byte range
	 * @return size in bytes
	 */
	public double getTotalBytes(){
		if (mdatSize==0){
			return getEndByte()-getBeginByte();
		}else {
			return mdatSize;
		}
	}
	
	public ArrayList<ByteRange> getRangeList() {
		return rangeList;
	}

	public void setRangeList(ArrayList<ByteRange> rangeList) {
		this.rangeList = rangeList;
	}

	public double getPacketCount() {
		return packetCount;
	}

	public void setPacketCount(double packetCount) {
		this.packetCount = packetCount;
	}

	public double getStartTS() {
		return startTS;
	}

	public double getEndTS() {
		return endTS;
	}
	
	public BufferedImage getImageOriginal(){
		return imageOriginal;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof VideoEvent){
			if (this == obj){
				return true;
			}
		
			VideoEvent veObj = (VideoEvent) obj;
			if (Double.doubleToLongBits(endTS) != Double.doubleToLongBits(veObj.endTS)){
				return false;
			}
			if (segment != veObj.segment){
				return false;
			}
			if (Double.doubleToLongBits(startTS) != Double.doubleToLongBits(veObj.startTS)){
				return false;
			}
			if (Double.doubleToLongBits(bitrate) != Double.doubleToLongBits(veObj.bitrate)){
				return false;
			}
			if (Double.doubleToLongBits(this.getEndByte()) != Double.doubleToLongBits(veObj.getEndByte())){
				return false;
			}
			if (Double.doubleToLongBits(this.getBeginByte()) != Double.doubleToLongBits(veObj.getBeginByte())){
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
		temp = Double.doubleToLongBits(bitrate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(endTS);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int)segment;
		temp = Double.doubleToLongBits(startTS);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
}
