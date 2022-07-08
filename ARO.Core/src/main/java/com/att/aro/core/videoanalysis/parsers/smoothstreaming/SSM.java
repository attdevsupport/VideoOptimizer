/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis.parsers.smoothstreaming;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.att.aro.core.videoanalysis.parsers.MpdBase;

/* sampling of manifest
 * 
 * <SmoothStreamingMedia 
 *     MajorVersion="2" 
 *     MinorVersion="0" 
 *     TimeScale= "10000000"	// 10000000 = 1 second
 *     Duration="1161386667"	// 116.1386667 seconds
 *     >
 *     <StreamIndex 
 *             Type="video" 
 *             Name="video" 
 *             Chunks="58" 
 *             QualityLevels="8" 
 *             Url="QualityLevels({bitrate})/Fragments(video={start time})" 
 *             Language="eng" 
 *             MaxWidth="712" 
 *             MaxHeight="296" 
 *             DisplayWidth="712" 
 *             DisplayHeight="296">
 *         <QualityLevel Index="0" Bitrate="2000000" CodecPrivateData="00000001674D401E965281684FCB2E02D100000303E90000BB80E080007A120001312DFC6383B4345B2C0000000168E9093520" MaxWidth="712" MaxHeight="296" FourCC="AVC1"></QualityLevel>
 *         <QualityLevel Index="1" Bitrate="1350000" CodecPrivateData="00000001674D401E965281684FCB2E02D100000303E90000BB80E06000526500019BFDFC6383B4345B2C0000000168E9093520" MaxWidth="712" MaxHeight="296" FourCC="AVC1"></QualityLevel>
 *         <QualityLevel Index="2" Bitrate="900000"  CodecPrivateData="00000001674D401E965281684FCB2E02D100000303E90000BB80E06000DBB800044AA7F18E0ED0D16CB00000000168E9093520" MaxWidth="712" MaxHeight="296" FourCC="AVC1"></QualityLevel>
 *         <QualityLevel Index="3" Bitrate="600000"  CodecPrivateData="00000001674D401E965281684FCB2E02D100000303E90000BB80E04000927C0005B8DFF18E0ED0D16CB00000000168E9093520" MaxWidth="712" MaxHeight="296" FourCC="AVC1"></QualityLevel>
 *         <QualityLevel Index="4" Bitrate="450000"  CodecPrivateData="00000001674D4016965283C37F2E02D100000303E90000BB80E04001B77000112A9FC6383B4305B2C00000000168E9093520"   MaxWidth="480" MaxHeight="200" FourCC="AVC1"></QualityLevel>
 *         <QualityLevel Index="5" Bitrate="300000"  CodecPrivateData="00000001674D4016965283C37F2E02D100000303E90000BB80E0200124F00016E37FC6383B4305B2C00000000168E9093520"   MaxWidth="480" MaxHeight="200" FourCC="AVC1"></QualityLevel>
 *         <QualityLevel Index="6" Bitrate="200000"  CodecPrivateData="00000001674D4016965283C37F2E02D100000303E90000BB80E000030D40007A127F18E0ED0C16CB0000000168E9093520"     MaxWidth="480" MaxHeight="200" FourCC="AVC1"></QualityLevel>
 *         <QualityLevel Index="7" Bitrate="150000"  CodecPrivateData="00000001674D4016965283C37F2E02D100000303E90000BB80E0000249C0005B8DFF18E0ED0C16CB0000000168E9093520"     MaxWidth="480" MaxHeight="200" FourCC="AVC1"></QualityLevel>
 *         <c d="20020000" t="0"></c>
 *         <c d="20020000"></c>
 *         <c d="20020000"></c>
 *         <c d="20020000"></c>
 *         ...
 *     </StreamIndex>
 * </SmoothStreamingMedia>
 */
@XmlRootElement(name = "SmoothStreamingMedia")
public class SSM implements MpdBase{

	List<SSMStreamIndex> streamIndex = new ArrayList<>();
	String majorVersion;
	String minorVersion;
	String duration;
	String timeScale;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("SSMAmz :"); 
		strblr.append(" majorVersion :").append(majorVersion);
		strblr.append(",  minorVersion :").append(minorVersion);
		strblr.append(",  duration :").append(duration);
		
		if (!streamIndex.isEmpty()) {
			strblr.append("\n\tstreamIndex:");
			for (SSMStreamIndex streamIdx : streamIndex) {
				strblr.append("\n\t\t");
				strblr.append(streamIdx);
			}
		}
		return strblr.toString();
	}
	
	@XmlElement(name = "StreamIndex")
	public List<SSMStreamIndex> getStreamIndex() {
		return streamIndex;
	}

	public void setStreamIndex(List<SSMStreamIndex> url) {
		this.streamIndex = url;
	}

	@Override
	@XmlAttribute(name = "MajorVersion")
	public String getMajorVersion() {
		return majorVersion;
	}

	public void setMajorVersion(String majorVersion) {
		this.majorVersion = majorVersion;
	}

	@XmlAttribute(name = "MinorVersion")
	public String getMinorVersion() {
		return minorVersion;
	}

	public void setMinorVersion(String minorVersion) {
		this.minorVersion = minorVersion;
	}

	@XmlAttribute(name = "Duration")
	public String getMediaPresentationDuration() {
		return duration;
	}

	public void setMediaPresentationDuration(String mediaPresentationDuration) {
		this.duration = mediaPresentationDuration;
	}

	@Override
	public int getSize() {
		return getStreamIndex().size();
	}

	@Override
	public String getVideoName() {
		return null;
	}

	@Override
	public String getType() {
		return "";
	}
}