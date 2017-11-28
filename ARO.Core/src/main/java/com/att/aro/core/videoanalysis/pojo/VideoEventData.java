package com.att.aro.core.videoanalysis.pojo;

import com.att.aro.core.packetanalysis.pojo.ByteRange;

public class VideoEventData {
	
	String cdn;
	String name;
	Integer segment;
	String segmentReference;
	String byteStart;
	String byteEnd;
	String quality;
	String segmentStartTime;
	String bitrate;
	String mdatSize;
	String manifestType;
	String duration;
	String rateCode;
	String position;
	double timestamp;
	String dateTime;
	ByteRange byteRange = null;
	double dtTime;
	String extension = "";
	String contentType;
	double contentLength;
	double contentSize;
	double contentStart;
	double contentEnd;
	private String failure ="";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(60);
		strblr.append("VideoEventData ");
		if (cdn              != null) {strblr.append("\n\t cdn : ");              strblr.append(cdn                                                  );}
		if (name             != null) {strblr.append("\n\t name : ");             strblr.append(name                                                 );}
		if (!extension.isEmpty()    ) {strblr.append("\n\t extension : ");        strblr.append(extension                                            );}
		if (segment          != null) {strblr.append("\n\t segment : ");          strblr.append(segment                                              );}
		if (segmentReference != null) {strblr.append("\n\t segmentReference : "); strblr.append(segmentReference                                     );}
		if (byteStart        != null) {strblr.append("\n\t byteStart : ");        strblr.append(byteStart                                            );}
		if (byteEnd          != null) {strblr.append("\n\t byteEnd : ");          strblr.append(byteEnd                                              );}
		if (quality          != null) {strblr.append("\n\t quality : ");          strblr.append(quality                                              );}
		if (segmentStartTime != null) {strblr.append("\n\t segmentStartTime : "); strblr.append(segmentStartTime                                     );}
		if (bitrate          != null) {strblr.append("\n\t bitrate : ");          strblr.append(bitrate                                              );}
		if (mdatSize         != null) {strblr.append("\n\t mdatSize : ");         strblr.append(mdatSize                                             );}
		if (duration         != null) {strblr.append("\n\t duration : ");         strblr.append(duration                                             );}
		if (rateCode         != null) {strblr.append("\n\t rateCode : ");         strblr.append(rateCode                                             );}
		if (position         != null) {strblr.append("\n\t position : ");         strblr.append(position                                             );} 
		if (timestamp        != 0)    {strblr.append("\n\t timestamp : ");        strblr.append(String.format("%d",((Double)timestamp).intValue())   );} 
		if (dateTime         != null) {strblr.append("\n\t dateTime : ");         strblr.append(dateTime                               				 );} 
		if (dtTime            > 0)    {strblr.append("\n\t dtTime : ");           strblr.append(String.format("%.0f",dtTime)                         );}
		if (byteRange        != null) {strblr.append("\n\t byteRange : ");        strblr.append(byteRange                                            );}
		if (contentType      != null) {strblr.append("\n\t contentType : ");      strblr.append(contentType                                          );}
		if (contentLength    != 0)    {strblr.append("\n\t contentLength : ");    strblr.append(contentLength                                        );}
		if (contentSize      != 0)    {strblr.append("\n\t contentSize : ");      strblr.append(contentSize                                          );}
		if (contentStart     != 0)    {strblr.append("\n\t contentStart : ");     strblr.append(contentStart                                         );}
		if (contentEnd       != 0)    {strblr.append("\n\t contentEnd : ");       strblr.append(contentEnd                                           );}
		if (!failure.isEmpty())       {strblr.append("\n\t failure : ");          strblr.append(failure                                              );}

		strblr.append(" }");
		strblr.replace(14, 16, ":{\n");
		return strblr.toString();
	}

	public VideoEventData() {
	}

	public ByteRange getByteRange() {
		if (byteRange == null) {
			if (byteStart != null && byteEnd != null) {
				byteRange = new ByteRange(byteStart, byteEnd);
			} else {
				return new ByteRange(0, 0);
			}
		}

		return byteRange;
	}

	public String getCdn() {
		return cdn;
	}

	/**
	 * Name of video
	 * returns empty string when null
	 * 
	 * @return
	 */
	public String getId() {
		return name != null ? name : "";
	}

	public String getExtension() {
		return extension;
	}

	public Integer getSegment() {
		return segment;
	}

	public void setSegment(Integer segment) {
		this.segment = segment;
	}

	public String getSegmentReference() {
		return segmentReference;
	}

	public void setSegmentReference(String segmentReference) {
		this.segmentReference = segmentReference;
	}

	/**
	 * populated from converting dateTime
	 * 
	 * @return a UTC timestamp in milliseconds
	 */
	public double getDtTime() {
		return dtTime;
	}

	public String getByteStart() {
		return byteStart;
	}

	public String getByteEnd() {
		return byteEnd;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getSegmentStartTime() {
		return segmentStartTime;
	}

	public String getBitrate() {
		return bitrate;
	}

	public String getMdatSize() {
		return mdatSize;
	}

	public String getDuration() {
		return duration;
	}
	
	public String getManifestType() {
		return manifestType;
	}

	public void setManifestType(String manifestType) {
		this.manifestType = manifestType;
	}

	public String getRateCode() {
		return rateCode;
	}

	public String getPosition() {
		return position;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public String getDateTime() {
		return dateTime;
	}

	public Double getContentLength() {
		return contentLength;
	}

	public Double getContentSize() {
		return contentSize;
	}

	public String getContentType() {
		return contentType;
	}

	public Double getContentStart() {
		return contentStart;
	}

	public Double getContentEnd() {
		return contentEnd;
	}
	// ----------------

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFailure() {
		return failure;
	}

	public void setFailure(String failure) {
		this.failure = failure;
	}

	public void setCdn(String cdn) {
		this.cdn = cdn;
	}

	public void setByteStart(String byteStart) {
		this.byteStart = byteStart;
	}

	public void setByteEnd(String byteEnd) {
		this.byteEnd = byteEnd;
	}

	public void setSegmentStartTime(String segmentStartTime) {
		this.segmentStartTime = segmentStartTime;
	}

	public void setBitrate(String bitrate) {
		this.bitrate = bitrate;
	}

	public void setMdatSize(String mdatSize) {
		this.mdatSize = mdatSize;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
	
	public void setRateCode(String rateCode) {
		this.rateCode = rateCode;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}

	public void setByteRange(ByteRange byteRange) {
		this.byteRange = byteRange;
	}

	public void setDtTime(double dtTime) {  
		this.dtTime = dtTime;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setContentLength(double contentLength) {
		this.contentLength = contentLength;
	}

	public void setContentSize(double contentSize) {
		this.contentSize = contentSize;
	}

	public void setContentStart(double contentStart) {
		this.contentStart = contentStart;
	}

	public void setContentEnd(double contentEnd) {
		this.contentEnd = contentEnd;
	}

}
