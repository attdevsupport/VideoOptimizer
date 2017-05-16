package com.att.aro.core.videoanalysis.pojo.amazonvideo;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * <pre>
 *  <QualityLevel
 *  	Index="0" 
 *  	Bitrate="2000000" 
 *  	CodecPrivateData="00000001674D401E965281684FCB2E02D100000303E90000BB80E080007A120001312DFC6383B4345B2C0000000168E9093520" 
 *  	MaxWidth="712" 
 *  	MaxHeight="296" 
 *  	FourCC="AVC1">
 *  </QualityLevel>
 *     
 */
public class SSMQualityLevelAmz {

	String index              ;  // ="0"         ="1"                                                                                                            
	String bitrate            ;  // ="2000000"   ="1350000"                                                                                                          
	String codecPrivateData   ;  // ="00000001674D401E965281684FCB2E02D100000303E90000BB80E080007A120001312DFC6383B4345B2C0000000168E9093520"              
	String maxWidth           ;  // ="712"                                                                                                                   
	String maxHeight          ;  // ="296"                                                                                                                   
	String fourCC             ;  // ="AVC1">   video codec, compression format, color or pixel format                                                               

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("SSMQualityLevelAmz :"); 
		strblr.append(" index:");                               strblr.append(index           );                
		strblr.append(", bitrate:");                            strblr.append(bitrate         );                
//		strblr.append(", codecPrivateData:");                   strblr.append(codecPrivateData);                
		strblr.append(", maxWidth:");                           strblr.append(maxWidth        );                
		strblr.append(", maxHeight:");                          strblr.append(maxHeight       );                
		strblr.append(", fourCC:");                             strblr.append(fourCC          );     
		
		return strblr.toString();
	}
	
	
	@XmlAttribute(name = "Index")
	public String getIndex() {
		return index;
	}

	@XmlAttribute(name = "Bitrate")
	public String getBitrate() {
		return bitrate;
	}

	@XmlAttribute(name = "CodecPrivateData")
	public String getCodecPrivateData() {
		return codecPrivateData;
	}

	@XmlAttribute(name = "MaxWidth")
	public String getMaxWidth() {
		return maxWidth;
	}

	@XmlAttribute(name = "MaxHeight")
	public String getMaxHeight() {
		return maxHeight;
	}

	@XmlAttribute(name = "FourCC")
	public String getFourCC() {
		return fourCC;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public void setBitrate(String bitrate) {
		this.bitrate = bitrate;
	}

	public void setCodecPrivateData(String codecPrivateData) {
		this.codecPrivateData = codecPrivateData;
	}

	public void setMaxWidth(String maxWidth) {
		this.maxWidth = maxWidth;
	}

	public void setMaxHeight(String maxHeight) {
		this.maxHeight = maxHeight;
	}

	public void setFourCC(String fourCC) {
		this.fourCC = fourCC;
	}

	
}
