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
import java.net.URI;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.util.CollectionUtils;

import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.core.video.pojo.Segment;
import com.att.aro.core.videoanalysis.impl.SegmentInfo;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;

import lombok.Data;

/**
 * <pre>
 * Encapsulates the data from a video or audio Segment sometimes referred to as a chunk
 */
@Data
public class VideoEvent implements Comparable<VideoEvent>{
	
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("Content:").append(getContentType());
		
		strblr.append(", Norm:").append(isNormalSegment() ? "yes" : "no");
		strblr.append(", S:").append(isSelected() ? "yes" : "no");
		strblr.append(", f:").append(getManifest().getVideoFormat());
		
		strblr.append(", VideoType :").append(videoType.toString());
		strblr.append(", Segment ID:").append(String.format("%.0f", segmentID));
		strblr.append(", Packet ID:").append(String.format("%d", response.getFirstDataPacket().getPacketId()));
		strblr.append(", PlayTime:").append(String.format("%.03f", playTime));
		strblr.append(", EndTime:").append(String.format("%.03f", getPlayTimeEnd()));
		strblr.append(", SegmentStartTime:").append(String.format("%.03f", segmentStartTime));
		strblr.append(", duration:").append(String.format("%.6f", duration * 1e0));
		strblr.append(", Session:").append(String.format("%.4f", getSession().getSessionStartTime()));
		strblr.append(", Quality:").append(quality);
		strblr.append(", bitrate:").append((int)bitrate);
		if (getContentType().equals(ContentType.VIDEO)) {
			strblr.append(", resolutionHeight:").append(resolutionHeight);
		}
		strblr.append(", mdatSize:").append(segmentSize);
		strblr.append(", dlSize:").append(response.getContentLength());
		if (!rangeList.isEmpty() && rangeList.get(0).isValidRange()) {
			strblr.append(", Byte Range:").append(rangeList);
		}
		strblr.append(", endTS:").append(String.format("%.6f", endTS * 1e0));
		strblr.append(", file:").append(response.getAssocReqResp().getFileName());
		if (getContentType().equals(ContentType.VIDEO) && !CollectionUtils.isEmpty(audioMap)) {
			strblr.append("\nAudioMap:").append(dumpAudioMap());
		}
		strblr.append(", channels:").append(channels);		
		return strblr.toString();
	}


	// 'broken' thumbnail portrait
	private static final byte[] DEFAULTIMAGE = new byte[] { (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x0D, (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0E, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x11, (byte) 0x08, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xED, (byte) 0xC8, (byte) 0x9D, (byte) 0x9F, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x01, (byte) 0x73, (byte) 0x52, (byte) 0x47, (byte) 0x42, (byte) 0x00, (byte) 0xAE, (byte) 0xCE, (byte) 0x1C, (byte) 0xE9, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x09, (byte) 0x70, (byte) 0x48, (byte) 0x59, (byte) 0x73, (byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x13,
			(byte) 0x01, (byte) 0x00, (byte) 0x9A, (byte) 0x9C, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xA6, (byte) 0x69, (byte) 0x54, (byte) 0x58, (byte) 0x74,
			(byte) 0x58, (byte) 0x4D, (byte) 0x4C, (byte) 0x3A, (byte) 0x63, (byte) 0x6F, (byte) 0x6D, (byte) 0x2E, (byte) 0x61, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65,
			(byte) 0x2E, (byte) 0x78, (byte) 0x6D, (byte) 0x70, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3C, (byte) 0x78, (byte) 0x3A, (byte) 0x78,
			(byte) 0x6D, (byte) 0x70, (byte) 0x6D, (byte) 0x65, (byte) 0x74, (byte) 0x61, (byte) 0x20, (byte) 0x78, (byte) 0x6D, (byte) 0x6C, (byte) 0x6E, (byte) 0x73, (byte) 0x3A,
			(byte) 0x78, (byte) 0x3D, (byte) 0x22, (byte) 0x61, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x3A, (byte) 0x6E, (byte) 0x73, (byte) 0x3A, (byte) 0x6D,
			(byte) 0x65, (byte) 0x74, (byte) 0x61, (byte) 0x2F, (byte) 0x22, (byte) 0x20, (byte) 0x78, (byte) 0x3A, (byte) 0x78, (byte) 0x6D, (byte) 0x70, (byte) 0x74, (byte) 0x6B,
			(byte) 0x3D, (byte) 0x22, (byte) 0x58, (byte) 0x4D, (byte) 0x50, (byte) 0x20, (byte) 0x43, (byte) 0x6F, (byte) 0x72, (byte) 0x65, (byte) 0x20, (byte) 0x35, (byte) 0x2E,
			(byte) 0x34, (byte) 0x2E, (byte) 0x30, (byte) 0x22, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x72, (byte) 0x64, (byte) 0x66,
			(byte) 0x3A, (byte) 0x52, (byte) 0x44, (byte) 0x46, (byte) 0x20, (byte) 0x78, (byte) 0x6D, (byte) 0x6C, (byte) 0x6E, (byte) 0x73, (byte) 0x3A, (byte) 0x72, (byte) 0x64,
			(byte) 0x66, (byte) 0x3D, (byte) 0x22, (byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3A, (byte) 0x2F, (byte) 0x2F, (byte) 0x77, (byte) 0x77, (byte) 0x77,
			(byte) 0x2E, (byte) 0x77, (byte) 0x33, (byte) 0x2E, (byte) 0x6F, (byte) 0x72, (byte) 0x67, (byte) 0x2F, (byte) 0x31, (byte) 0x39, (byte) 0x39, (byte) 0x39, (byte) 0x2F,
			(byte) 0x30, (byte) 0x32, (byte) 0x2F, (byte) 0x32, (byte) 0x32, (byte) 0x2D, (byte) 0x72, (byte) 0x64, (byte) 0x66, (byte) 0x2D, (byte) 0x73, (byte) 0x79, (byte) 0x6E,
			(byte) 0x74, (byte) 0x61, (byte) 0x78, (byte) 0x2D, (byte) 0x6E, (byte) 0x73, (byte) 0x23, (byte) 0x22, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x72, (byte) 0x64, (byte) 0x66, (byte) 0x3A, (byte) 0x44, (byte) 0x65, (byte) 0x73, (byte) 0x63, (byte) 0x72,
			(byte) 0x69, (byte) 0x70, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x20, (byte) 0x72, (byte) 0x64, (byte) 0x66, (byte) 0x3A, (byte) 0x61, (byte) 0x62,
			(byte) 0x6F, (byte) 0x75, (byte) 0x74, (byte) 0x3D, (byte) 0x22, (byte) 0x22, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x78, (byte) 0x6D, (byte) 0x6C, (byte) 0x6E, (byte) 0x73, (byte) 0x3A, (byte) 0x78,
			(byte) 0x6D, (byte) 0x70, (byte) 0x3D, (byte) 0x22, (byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3A, (byte) 0x2F, (byte) 0x2F, (byte) 0x6E, (byte) 0x73,
			(byte) 0x2E, (byte) 0x61, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x2E, (byte) 0x63, (byte) 0x6F, (byte) 0x6D, (byte) 0x2F, (byte) 0x78, (byte) 0x61,
			(byte) 0x70, (byte) 0x2F, (byte) 0x31, (byte) 0x2E, (byte) 0x30, (byte) 0x2F, (byte) 0x22, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x78, (byte) 0x6D, (byte) 0x6C, (byte) 0x6E, (byte) 0x73, (byte) 0x3A,
			(byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3D, (byte) 0x22, (byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3A, (byte) 0x2F, (byte) 0x2F,
			(byte) 0x6E, (byte) 0x73, (byte) 0x2E, (byte) 0x61, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x2E, (byte) 0x63, (byte) 0x6F, (byte) 0x6D, (byte) 0x2F,
			(byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x2F, (byte) 0x31, (byte) 0x2E, (byte) 0x30, (byte) 0x2F, (byte) 0x22, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x78, (byte) 0x6D, (byte) 0x6C,
			(byte) 0x6E, (byte) 0x73, (byte) 0x3A, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x3D, (byte) 0x22, (byte) 0x68, (byte) 0x74, (byte) 0x74, (byte) 0x70,
			(byte) 0x3A, (byte) 0x2F, (byte) 0x2F, (byte) 0x6E, (byte) 0x73, (byte) 0x2E, (byte) 0x61, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x2E, (byte) 0x63,
			(byte) 0x6F, (byte) 0x6D, (byte) 0x2F, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x2F, (byte) 0x31, (byte) 0x2E, (byte) 0x30, (byte) 0x2F, (byte) 0x22,
			(byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x78,
			(byte) 0x6D, (byte) 0x70, (byte) 0x3A, (byte) 0x4D, (byte) 0x6F, (byte) 0x64, (byte) 0x69, (byte) 0x66, (byte) 0x79, (byte) 0x44, (byte) 0x61, (byte) 0x74, (byte) 0x65,
			(byte) 0x3E, (byte) 0x32, (byte) 0x30, (byte) 0x31, (byte) 0x37, (byte) 0x2D, (byte) 0x31, (byte) 0x30, (byte) 0x2D, (byte) 0x32, (byte) 0x34, (byte) 0x54, (byte) 0x31,
			(byte) 0x36, (byte) 0x3A, (byte) 0x31, (byte) 0x30, (byte) 0x3A, (byte) 0x34, (byte) 0x37, (byte) 0x3C, (byte) 0x2F, (byte) 0x78, (byte) 0x6D, (byte) 0x70, (byte) 0x3A,
			(byte) 0x4D, (byte) 0x6F, (byte) 0x64, (byte) 0x69, (byte) 0x66, (byte) 0x79, (byte) 0x44, (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x3E, (byte) 0x0A, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x78, (byte) 0x6D, (byte) 0x70, (byte) 0x3A,
			(byte) 0x43, (byte) 0x72, (byte) 0x65, (byte) 0x61, (byte) 0x74, (byte) 0x6F, (byte) 0x72, (byte) 0x54, (byte) 0x6F, (byte) 0x6F, (byte) 0x6C, (byte) 0x3E, (byte) 0x50,
			(byte) 0x69, (byte) 0x78, (byte) 0x65, (byte) 0x6C, (byte) 0x6D, (byte) 0x61, (byte) 0x74, (byte) 0x6F, (byte) 0x72, (byte) 0x20, (byte) 0x33, (byte) 0x2E, (byte) 0x36,
			(byte) 0x3C, (byte) 0x2F, (byte) 0x78, (byte) 0x6D, (byte) 0x70, (byte) 0x3A, (byte) 0x43, (byte) 0x72, (byte) 0x65, (byte) 0x61, (byte) 0x74, (byte) 0x6F, (byte) 0x72,
			(byte) 0x54, (byte) 0x6F, (byte) 0x6F, (byte) 0x6C, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x4F, (byte) 0x72, (byte) 0x69, (byte) 0x65, (byte) 0x6E,
			(byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x31, (byte) 0x3C, (byte) 0x2F, (byte) 0x74, (byte) 0x69, (byte) 0x66,
			(byte) 0x66, (byte) 0x3A, (byte) 0x4F, (byte) 0x72, (byte) 0x69, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E,
			(byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x74,
			(byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x43, (byte) 0x6F, (byte) 0x6D, (byte) 0x70, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x69,
			(byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x35, (byte) 0x3C, (byte) 0x2F, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x43, (byte) 0x6F,
			(byte) 0x6D, (byte) 0x70, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A,
			(byte) 0x52, (byte) 0x65, (byte) 0x73, (byte) 0x6F, (byte) 0x6C, (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x55, (byte) 0x6E, (byte) 0x69,
			(byte) 0x74, (byte) 0x3E, (byte) 0x32, (byte) 0x3C, (byte) 0x2F, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x52, (byte) 0x65, (byte) 0x73,
			(byte) 0x6F, (byte) 0x6C, (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x55, (byte) 0x6E, (byte) 0x69, (byte) 0x74, (byte) 0x3E, (byte) 0x0A,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x74, (byte) 0x69, (byte) 0x66,
			(byte) 0x66, (byte) 0x3A, (byte) 0x59, (byte) 0x52, (byte) 0x65, (byte) 0x73, (byte) 0x6F, (byte) 0x6C, (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E,
			(byte) 0x3E, (byte) 0x37, (byte) 0x32, (byte) 0x3C, (byte) 0x2F, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x59, (byte) 0x52, (byte) 0x65,
			(byte) 0x73, (byte) 0x6F, (byte) 0x6C, (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x58,
			(byte) 0x52, (byte) 0x65, (byte) 0x73, (byte) 0x6F, (byte) 0x6C, (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x37, (byte) 0x32,
			(byte) 0x3C, (byte) 0x2F, (byte) 0x74, (byte) 0x69, (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x58, (byte) 0x52, (byte) 0x65, (byte) 0x73, (byte) 0x6F, (byte) 0x6C,
			(byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x3A, (byte) 0x50, (byte) 0x69, (byte) 0x78, (byte) 0x65,
			(byte) 0x6C, (byte) 0x58, (byte) 0x44, (byte) 0x69, (byte) 0x6D, (byte) 0x65, (byte) 0x6E, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x31,
			(byte) 0x34, (byte) 0x3C, (byte) 0x2F, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x3A, (byte) 0x50, (byte) 0x69, (byte) 0x78, (byte) 0x65, (byte) 0x6C,
			(byte) 0x58, (byte) 0x44, (byte) 0x69, (byte) 0x6D, (byte) 0x65, (byte) 0x6E, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x0A, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66,
			(byte) 0x3A, (byte) 0x43, (byte) 0x6F, (byte) 0x6C, (byte) 0x6F, (byte) 0x72, (byte) 0x53, (byte) 0x70, (byte) 0x61, (byte) 0x63, (byte) 0x65, (byte) 0x3E, (byte) 0x31,
			(byte) 0x3C, (byte) 0x2F, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x3A, (byte) 0x43, (byte) 0x6F, (byte) 0x6C, (byte) 0x6F, (byte) 0x72, (byte) 0x53,
			(byte) 0x70, (byte) 0x61, (byte) 0x63, (byte) 0x65, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x3A, (byte) 0x50, (byte) 0x69, (byte) 0x78, (byte) 0x65, (byte) 0x6C,
			(byte) 0x59, (byte) 0x44, (byte) 0x69, (byte) 0x6D, (byte) 0x65, (byte) 0x6E, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x31, (byte) 0x37,
			(byte) 0x3C, (byte) 0x2F, (byte) 0x65, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x3A, (byte) 0x50, (byte) 0x69, (byte) 0x78, (byte) 0x65, (byte) 0x6C, (byte) 0x59,
			(byte) 0x44, (byte) 0x69, (byte) 0x6D, (byte) 0x65, (byte) 0x6E, (byte) 0x73, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x3C, (byte) 0x2F, (byte) 0x72, (byte) 0x64, (byte) 0x66, (byte) 0x3A, (byte) 0x44, (byte) 0x65, (byte) 0x73,
			(byte) 0x63, (byte) 0x72, (byte) 0x69, (byte) 0x70, (byte) 0x74, (byte) 0x69, (byte) 0x6F, (byte) 0x6E, (byte) 0x3E, (byte) 0x0A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
			(byte) 0x3C, (byte) 0x2F, (byte) 0x72, (byte) 0x64, (byte) 0x66, (byte) 0x3A, (byte) 0x52, (byte) 0x44, (byte) 0x46, (byte) 0x3E, (byte) 0x0A, (byte) 0x3C, (byte) 0x2F,
			(byte) 0x78, (byte) 0x3A, (byte) 0x78, (byte) 0x6D, (byte) 0x70, (byte) 0x6D, (byte) 0x65, (byte) 0x74, (byte) 0x61, (byte) 0x3E, (byte) 0x0A, (byte) 0x64, (byte) 0x30,
			(byte) 0x99, (byte) 0x87, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x47, (byte) 0x49, (byte) 0x44, (byte) 0x41, (byte) 0x54, (byte) 0x28, (byte) 0x15, (byte) 0x9D,
			(byte) 0x93, (byte) 0x3B, (byte) 0x4B, (byte) 0xC4, (byte) 0x40, (byte) 0x14, (byte) 0x85, (byte) 0xEF, (byte) 0x0C, (byte) 0x01, (byte) 0xB5, (byte) 0xB1, (byte) 0xD0,
			(byte) 0x62, (byte) 0x15, (byte) 0x1B, (byte) 0x2B, (byte) 0x61, (byte) 0x6D, (byte) 0xEC, (byte) 0x2C, (byte) 0xAC, (byte) 0x36, (byte) 0x3E, (byte) 0xC0, (byte) 0xCA,
			(byte) 0xCA, (byte) 0x17, (byte) 0x16, (byte) 0xDA, (byte) 0x59, (byte) 0x44, (byte) 0xFC, (byte) 0x01, (byte) 0xD6, (byte) 0xFA, (byte) 0x03, (byte) 0x2C, (byte) 0x55,
			(byte) 0x44, (byte) 0xB0, (byte) 0x51, (byte) 0xF0, (byte) 0x51, (byte) 0x6A, (byte) 0xA1, (byte) 0x0B, (byte) 0x0A, (byte) 0x6B, (byte) 0xB5, (byte) 0x20, (byte) 0xB6,
			(byte) 0x56, (byte) 0xBE, (byte) 0x1A, (byte) 0xD7, (byte) 0xC2, (byte) 0xC2, (byte) 0x05, (byte) 0xB1, (byte) 0x10, (byte) 0x12, (byte) 0xBF, (byte) 0x1B, (byte) 0x32,
			(byte) 0x30, (byte) 0x11, (byte) 0xC1, (byte) 0xB0, (byte) 0x07, (byte) 0xBE, (byte) 0xCC, (byte) 0x9C, (byte) 0x3B, (byte) 0x73, (byte) 0x93, (byte) 0x9C, (byte) 0x81,
			(byte) 0x31, (byte) 0x2B, (byte) 0xA1, (byte) 0x8C, (byte) 0x25, (byte) 0x46, (byte) 0x42, (byte) 0x49, (byte) 0xA4, (byte) 0x47, (byte) 0x8A, (byte) 0xC8, (byte) 0x48,
			(byte) 0xC3, (byte) 0x24, (byte) 0x72, (byte) 0x25, (byte) 0xD1, (byte) 0xA8, (byte) 0x6C, (byte) 0x14, (byte) 0xD9, (byte) 0xEF, (byte) 0xEF, (byte) 0xD1, (byte) 0x1E,
			(byte) 0x5B, (byte) 0xF8, (byte) 0x4B, (byte) 0x7E, (byte) 0x27, (byte) 0x7F, (byte) 0x67, (byte) 0x7D, (byte) 0x5F, (byte) 0x74, (byte) 0x1E, (byte) 0xC7, (byte) 0x72,
			(byte) 0xFB, (byte) 0xBB, (byte) 0x71, (byte) 0x81, (byte) 0xE6, (byte) 0xA1, (byte) 0x7F, (byte) 0x5E, (byte) 0x50, (byte) 0x6D, (byte) 0x37, (byte) 0x72, (byte) 0xE8,
			(byte) 0x37, (byte) 0x2E, (byte) 0xD2, (byte) 0x30, (byte) 0x0F, (byte) 0x3B, (byte) 0xD0, (byte) 0x0F, (byte) 0x7F, (byte) 0xE9, (byte) 0x8C, (byte) 0xE2, (byte) 0x76,
			(byte) 0x79, (byte) 0x40, (byte) 0x3E, (byte) 0x83, (byte) 0x6C, (byte) 0xB5, (byte) 0x8D, (byte) 0xB1, (byte) 0x0F, (byte) 0xA6, (byte) 0xA0, (byte) 0x03, (byte) 0xEE,
			(byte) 0x20, (byte) 0x84, (byte) 0x57, (byte) 0x70, (byte) 0x3A, (byte) 0x65, (byte) 0x72, (byte) 0x04, (byte) 0xC7, (byte) 0xF5, (byte) 0x67, (byte) 0xE9, (byte) 0x72,
			(byte) 0x5F, (byte) 0x9C, (byte) 0xA4, (byte) 0x50, (byte) 0xCB, (byte) 0x76, (byte) 0x7C, (byte) 0x31, (byte) 0x0E, (byte) 0xC3, (byte) 0x35, (byte) 0x74, (byte) 0x83,
			(byte) 0xEA, (byte) 0x00, (byte) 0x2E, (byte) 0x61, (byte) 0x5F, (byte) 0x4D, (byte) 0xB3, (byte) 0x53, (byte) 0x3E, (byte) 0xAC, (byte) 0x31, (byte) 0x9C, (byte) 0xAB,
			(byte) 0xC8, (byte) 0x2A, (byte) 0xDC, (byte) 0x80, (byte) 0xCB, (byte) 0xD8, (byte) 0x64, (byte) 0x3E, (byte) 0x02, (byte) 0x17, (byte) 0x70, (byte) 0x02, (byte) 0x75,
			(byte) 0xD8, (byte) 0x02, (byte) 0x55, (byte) 0xB5, (byte) 0xF4, (byte) 0x26, (byte) 0x83, (byte) 0xC1, (byte) 0xB7, (byte) 0x95, (byte) 0x75, (byte) 0xCC, (byte) 0x0B,
			(byte) 0x68, (byte) 0xC6, (byte) 0x69, (byte) 0x28, (byte) 0xC1, (byte) 0x1C, (byte) 0x3C, (byte) 0xC1, (byte) 0x0C, (byte) 0x54, (byte) 0x60, (byte) 0x0F, (byte) 0x54,
			(byte) 0x69, (byte) 0xC6, (byte) 0x38, (byte) 0x90, (byte) 0x65, (byte) 0x35, (byte) 0x06, (byte) 0x34, (byte) 0xE3, (byte) 0x9A, (byte) 0x1A, (byte) 0xA4, (byte) 0x19,
			(byte) 0xEF, (byte) 0xA1, (byte) 0x57, (byte) 0x8D, (byte) 0x27, (byte) 0xCD, (byte) 0xB8, (byte) 0xA4, (byte) 0x3E, (byte) 0x0A, (byte) 0x65, (byte) 0xB7, (byte) 0xA5,
			(byte) 0x8C, (byte) 0xDA, (byte) 0xEC, (byte) 0x4E, (byte) 0x55, (byte) 0x33, (byte) 0x8E, (byte) 0x6B, (byte) 0x21, (byte) 0x93, (byte) 0x9F, (byte) 0xF1, (byte) 0x91,
			(byte) 0x9A, (byte) 0xE6, (byte) 0x77, (byte) 0x19, (byte) 0xD3, (byte) 0x2D, (byte) 0x36, (byte) 0xAA, (byte) 0xC8, (byte) 0x2C, (byte) 0x33, (byte) 0xBF, (byte) 0x29,
			(byte) 0x5D, (byte) 0xE0, (byte) 0xF1, (byte) 0x0E, (byte) 0x9A, (byte) 0xF1, (byte) 0x1C, (byte) 0x36, (byte) 0x21, (byte) 0x27, (byte) 0x4B, (byte) 0xC2, (byte) 0x89,
			(byte) 0x5C, (byte) 0x25, (byte) 0x6F, (byte) 0x1E, (byte) 0xB0, (byte) 0xEE, (byte) 0x60, (byte) 0x72, (byte) 0x2B, (byte) 0xDA, (byte) 0xD8, (byte) 0xC8, (byte) 0x55,
			(byte) 0x8A, (byte) 0x18, (byte) 0xBD, (byte) 0x5A, (byte) 0xAD, (byte) 0xDE, (byte) 0xC7, (byte) 0x1F, (byte) 0x39, (byte) 0xCD, (byte) 0x4C, (byte) 0x69, (byte) 0x1F,
			(byte) 0x42, (byte) 0x74, (byte) 0xE4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44, (byte) 0xAE, (byte) 0x42,
			(byte) 0x60, (byte) 0x82 };

	private BufferedImage thumbnail;
	private int maxW = 20;

	private Manifest manifest;
	private ChildManifest childManifest;
	private VideoType videoType;
	private SegmentInfo segmentInfo;
	private URI uri;
	private BufferedImage imageOriginal;
	private double bitrate;
	private double packetCount ;
	private HttpRequestResponseInfo response;
	private double segmentSize;
	private ArrayList<ByteRange> rangeList = new ArrayList<>();
	private double segmentID;
	private String segmentPathName;
	/**
	 * a selected segment is considered available and is used in playtime and stall calculations
	 */
	private boolean selected = true;
	/**
	 * Segment Quality AKA Track
	 */
	private String quality;
	private int resolutionHeight = 0;
	/**
	 * Download starting timestamp
	 */
	private double startTS;
	/**
	 * Download ending timestamp
	 */
	private double endTS;
	/**
	 * duration of segment in seconds
	 */
	private double duration;
	/**
	 * Start time of segment based on manifest
	 */
	private double segmentStartTime;
	/**
	 * used to offset segmentStartTime to create the playTime
	 */
	private double startupOffset;
	/**
	 * Time when segment will play, based on startupOffset + segmentStartTime + totalStallOffset(from all prior stalls) + stallOffset(from this event plus recovery time)
	 */
	private double playTime;
	/**
	 * Amount of time that a segment was delayed, based on endTS in relation to SegmentStartTime
	 */
	private double stallTime;
	/**
	 * segment is marked as active in stream
	 */
	private boolean active;
	
	private String channels;

	private long crc32Value;

	private double playRequestedTime;
	
	private SortedMap<String, VideoEvent> audioMap = new TreeMap<>();

	private String option = "";

	private boolean defaultThumbnail = false;
	
	private boolean failedRequest = false;
	

	public boolean isMpeg() {
		return getManifest().getVideoFormat().equals(VideoFormat.MPEG4);
	}
	
	public boolean isNormalSegment() {
		if (failedRequest || (getManifest().getVideoFormat().equals(VideoFormat.MPEG4) && segmentID < 1D)) {
			return false;
		}
		return true;
	}
	
	public double getPlayTimeEnd() {
		return playTime + duration;
	}
	
	public void setStartupOffset(double startupOffset) {
		this.startupOffset = startupOffset;
		setPlayTime(getSegmentStartTime() + startupOffset);
	}

	public void setStallTime(double stallOffset) {
		setPlayTime(getPlayTime() + stallOffset);
		this.stallTime = stallOffset;
	}
	
	public void setStallTime(double targetTimestamp, double stallRecovery) {
		setStallTime(targetTimestamp - getPlayTime() + stallRecovery);
	}

	/**
	 * The VideoEvent.VideoEventType Enumeration specifies constant values that describe 
	 * different types of user generated events. This enumeration is part of the VideoEvent 
	 * class.
	 */
	public enum VideoType {
		DASH,
		DASH_IF,
		DASH_DYNAMIC,
		DASH_ENCODEDSEGMENTLIST,
		DASH_SEGMENTTIMELINE,
		HLS,
		HLS_CHILD,
		SSM,
		UNKNOWN
	}

	public String dumpAudioMap() {
		StringBuilder strblr = new StringBuilder();
		audioMap.entrySet().stream().forEach(x -> {
			strblr.append("\n")
			.append(x.getKey())
			.append(": ")
			.append(x.getValue().getSegmentID());
		});
		return strblr.toString();
	}
	
	public void setThumbnail(byte[] imgArray) {
		byte[] imageArray;
		if (imgArray == null) {
			imageArray = DEFAULTIMAGE;
			defaultThumbnail  = true;
			segmentInfo.setThumbnailExtracted(false);
		} else {
			imageArray = imgArray;
		}

		BufferedImage image = null;
		image = ImageHelper.getImageFromByte(imageArray);
		thumbnail = image;
		if (image != null) {
			int height = image.getHeight();
			int width = image.getWidth();
			imageOriginal = image;

			int targetHeight = maxW;
			int targetWidth = maxW * width / height;

			thumbnail = ImageHelper.resize(image, targetWidth, targetHeight);

			defaultThumbnail  = false;
			segmentInfo.setThumbnailExtracted(true);
			getManifest().setVideoMetaDataExtracted(true);
		}
	}
	
	/**
	 * Initializes an instance of the VideoEvent class, using the specified event type, 
	 * press time, and release time.
	 * 
	 * @param imageArray a thumbnail image from first "frame" of chunk
	 * @param manifest
	 * @param segmentInfo
	 * @param childManifest
	 * @param contentSize
	 * @param response
	 * @param crc32Value
	 */
	public VideoEvent(
			 byte[] imageArray
			, Manifest manifest2
			, SegmentInfo segmentInfo
			, ChildManifest childManifest
			, int contentSize
			, HttpRequestResponseInfo response
			, long crc32Value) {
		
		this.segmentInfo = segmentInfo;
		this.manifest = manifest2 != null ? manifest2 : new Manifest();
		this.videoType = this.manifest.getVideoType();
		setBitrate(segmentInfo.getBitrate());
		setChildManifest(childManifest);
		if (segmentInfo.getResolutionHeight() == 0) {
			segmentInfo.setResolutionHeight(childManifest.getPixelHeight());
		}
		setResolutionHeight(segmentInfo.getResolutionHeight());
		setChannels(childManifest.getChannels());
		setQuality(segmentInfo.getQuality());
		setSegmentID(segmentInfo.getSegmentID());
		setPlayTime(segmentInfo.getStartTime());
		setDuration(segmentInfo.getDuration());
		setSegmentSize(contentSize);
		getSegmentInfo().setSize(contentSize);

		double temp = 0;
		if (isNormalSegment()) {
			if (manifest.getTimeScale() > 0 && segmentInfo.getStartTime() > manifest.getTimeScale()) {
				temp = segmentInfo.getStartTime() / manifest.getTimeScale();
			} else {
				temp = segmentInfo.getStartTime();
			}
			setSegmentStartTime(temp);
			setPlayTime(temp);
		} else {
			setSegmentStartTime(0);
			setPlayTime(0);
		}
		this.response = response;
		setCrc32Value(crc32Value);

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
	
	public VideoEvent(byte[] image, Manifest manifest, Segment segment, HttpRequestResponseInfo response) {
		
		this.segmentInfo = createSegmentInfo(manifest, segment);
		
		this.manifest = manifest != null ? manifest : new Manifest();
		this.videoType = manifest.getVideoType();
		this.bitrate = segmentInfo.getBitrate();
		this.segmentID = segment.getSegmentIndex();
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
		
		this.playTime = segment.getStartPlayTime();
		this.segmentStartTime = segment.getStartPlayTime();
		this.quality = segment.getTrack().getTrackNumber() + "";
		this.duration = segment.getEndPlayTime() - segment.getStartPlayTime();
		this.segmentSize = segment.getSize();
		this.response = response;
		if (response.getContentOffsetLength() == null) {
			this.setPacketCount(0);
		} else {
			this.setPacketCount(response.getContentOffsetLength().values().size());
		}

		if (image != null) {
			setThumbnail(image);
		} else {
			setThumbnail(DEFAULTIMAGE);
		}
	}

	private SegmentInfo createSegmentInfo(Manifest manifest, Segment segment) {
		SegmentInfo segmentInfo = new SegmentInfo();
		
		segmentInfo.setSize(segment.getSize());
		segmentInfo.setDuration(segment.getEndPlayTime() - segment.getStartPlayTime());
		segmentInfo.setSegmentID(segment.getSegmentIndex());
		segmentInfo.setBitrate(segmentInfo.getDuration() != 0 ? (segment.getSize() * 8 / segmentInfo.getDuration() / 1000) : 0);
		segmentInfo.setStartTime(segment.getStartPlayTime());
		segmentInfo.setContentType(manifest.getContentType());
		
		if (manifest.getContentType() == ContentType.VIDEO) {
			segmentInfo.setVideo(true);
		} else {
			segmentInfo.setVideo(false);
		}
		return segmentInfo;
	}

	public Session getSession() {
		return response.getDirection() == HttpDirection.REQUEST ? response.getSession() : response.getAssocReqResp().getSession();
	}
	
	public HttpRequestResponseInfo getRequest() {
		return response.getAssocReqResp();
	}
	
	public ContentType getContentType() {
		if (segmentInfo!=null) {
			return segmentInfo.getContentType();
		} else {
			return manifest.getContentType();
		}
	}
	
	/**
	 * amount of time that download took
	 * 
	 * @return
	 */
	public double getDLTime() {
		return endTS - startTS;
	}
	
	/**
	 * Timestamp at beginning of download
	 * 
	 * @return
	 */
	public double getDLTimeStamp() {
		return startTS;
	}
	
	/**
	 * Timestamp of the end of download
	 * 
	 * @return
	 */
	public double getDLLastTimestamp(){
		return endTS;
	}
	
	public double getSize() {
		return segmentSize;
	}

	public void setEventType(VideoType videoType) {
		this.videoType = videoType;
	}

	public URI getUrl() {
		return uri;
	}

	public void setUrl(URI url) {
		this.uri = url;
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
	public double getTotalBytes() {
		if (segmentSize == 0) {
			return getEndByte() - getBeginByte();
		} else {
			return segmentSize;
		}
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
			if (segmentID != veObj.segmentID){
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
			if (Double.doubleToLongBits(this.getResolutionHeight()) != Double.doubleToLongBits(veObj.getResolutionHeight())){
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
		temp = Double.doubleToLongBits(segmentID);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(startTS);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public int compareTo(VideoEvent obj) {
		if (this.equals(obj)) {
			return 0;
		} else if (this.getSegmentID() > obj.getSegmentID()) {
			return 1;
		}
		return -1;
	}

	public void addAudioPart(double startTime, double audioDuration, VideoEvent audioEvent) {
		String key = String.format("%s%010.6f", VideoStream.generateTimestampKey(startTime), audioDuration);
		audioMap.put(key, audioEvent);
	}

	/**
	 * prevents setting playtime on non-playable segments like DASH-moov
	 * @param playTime
	 */
	public void setPlayTime(double playTime) {
		if (this.isNormalSegment()) {
			this.playTime = playTime;
		}
	}


}