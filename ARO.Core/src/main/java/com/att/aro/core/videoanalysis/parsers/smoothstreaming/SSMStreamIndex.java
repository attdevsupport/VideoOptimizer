package com.att.aro.core.videoanalysis.parsers.smoothstreaming;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/*
 * 
 *  <StreamIndex 
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
 *     
 */
public class SSMStreamIndex {
	private String type;
	private String name;
	private String chunks;
	private String qualityLevels;
	private String url;
	private String language;
	private String maxWidth;
	private String maxHeight;
	private String displayWidth;
	private String displayHeight;
	private ArrayList<SSMQualityLevel> qualityLevelList;
	private ArrayList<SSMSegmentDuration> segDurationList;
	
	
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("SSMStreamIndexAmz :");
		strblr.append(" type :");
		strblr.append(type);
		strblr.append(", name :");
		strblr.append(name);
		strblr.append(", chunks :");
		strblr.append(chunks);
		strblr.append(", qualityLevels :");
		strblr.append(qualityLevels);
		strblr.append(", maxWidth :");
		strblr.append(maxWidth);
		strblr.append(", maxHeight :");
		strblr.append(maxHeight);
		if (!qualityLevelList.isEmpty()) {
			strblr.append("\n\t\t\tqualityLevelList:");
			for (SSMQualityLevel qlevel : qualityLevelList) {
				strblr.append("\n\t\t\t\t");
				strblr.append(qlevel);
			}
		}
		strblr.append("\n\t\t\tsegDurationList:");
		strblr.append(segDurationList);
		return strblr.toString();
	}
	

	@XmlAttribute(name = "Type")
	public String getType() {
		return type;
	}

	@XmlAttribute(name = "Name")
	public String getName() {
		return name;
	}

	@XmlAttribute(name = "Chunks")
	public String getChunks() {
		return chunks;
	}

	@XmlAttribute(name = "QualityLevels")
	public String getQualityLevels() {
		return qualityLevels;
	}

	@XmlAttribute(name = "Url")
	public String getUrl() {
		return url;
	}

	@XmlAttribute(name = "Language")
	public String getLanguage() {
		return language;
	}

	@XmlAttribute(name = "MaxWidth")
	public String getMaxWidth() {
		return maxWidth;
	}

	@XmlAttribute(name = "MaxHeight")
	public String getMaxHeight() {
		return maxHeight;
	}

	@XmlAttribute(name = "DisplayWidth")
	public String getDisplayWidth() {
		return displayWidth;
	}

	@XmlAttribute(name = "DisplayHeight")
	public String getDisplayHeight() {
		return displayHeight;
	}

	@XmlElement(name = "QualityLevel")
	public ArrayList<SSMQualityLevel> getQualityLevelList() {
		return qualityLevelList;
	}
	
	@XmlElement(name = "c")
	public ArrayList<SSMSegmentDuration> getSegDurationList() {
		return segDurationList;
	}


	public void setType(String type) {
		this.type = type;
	}


	public void setName(String name) {
		this.name = name;
	}


	public void setChunks(String chunks) {
		this.chunks = chunks;
	}


	public void setQualityLevels(String qualityLevels) {
		this.qualityLevels = qualityLevels;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public void setLanguage(String language) {
		this.language = language;
	}


	public void setMaxWidth(String maxWidth) {
		this.maxWidth = maxWidth;
	}


	public void setMaxHeight(String maxHeight) {
		this.maxHeight = maxHeight;
	}


	public void setDisplayWidth(String displayWidth) {
		this.displayWidth = displayWidth;
	}


	public void setDisplayHeight(String displayHeight) {
		this.displayHeight = displayHeight;
	}


	public void setQualityLevelList(ArrayList<SSMQualityLevel> qualityLevelList) {
		this.qualityLevelList = qualityLevelList;
	}


	public void setSegDurationList(ArrayList<SSMSegmentDuration> segDurationList) {
		this.segDurationList = segDurationList;
	}

	
}
