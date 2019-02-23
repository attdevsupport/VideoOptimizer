package com.att.aro.core.videoanalysis.pojo;

import com.att.aro.core.packetanalysis.pojo.ByteRange;

public class VideoEventData {
	                                         
	private String cdn;
	private String name;
	private Integer segment;
	private String segmentReference;
	private String byteStart;
	private String byteEnd;
	private String quality;
	private String segmentStartTime;
	private String bitrate;
	private String mdatSize;
	private String manifestType;
	private String duration;
	private String rateCode;
	private String position;
	private double timestamp;
	private String dateTime;
	private ByteRange byteRange = null;
	private double dtTime;
	private String extension = "";
	private String contentType;
	private double contentLength;
	private double contentSize;
	private double contentStart;
	private double contentEnd;
	private String failure = "";

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(60);
		strblr.append("VideoEventData ");
		if (cdn != null) {
			strblr.append("\n\t cdn : ").append(cdn);
		}
		if (name != null) {
			strblr.append("\n\t name : ").append(name);
		}
		if (!extension.isEmpty()) {
			strblr.append("\n\t extension : ").append(extension);
		}
		if (segment != null) {
			strblr.append("\n\t segment : ").append(segment);
		}
		if (segmentReference != null) {
			strblr.append("\n\t segmentReference : ").append(segmentReference);
		}
		if (byteStart != null) {
			strblr.append("\n\t byteStart : ").append(byteStart);
		}
		if (byteEnd != null) {
			strblr.append("\n\t byteEnd : ").append(byteEnd);
		}
		if (quality != null) {
			strblr.append("\n\t quality : ").append(quality);
		}
		if (segmentStartTime != null) {
			strblr.append("\n\t segmentStartTime : ").append(segmentStartTime);
		}
		if (bitrate != null) {
			strblr.append("\n\t bitrate : ").append(bitrate);
		}
		if (mdatSize != null) {
			strblr.append("\n\t mdatSize : ").append(mdatSize);
		}
		if (duration != null) {
			strblr.append("\n\t duration : ").append(duration);
		}
		if (rateCode != null) {
			strblr.append("\n\t rateCode : ").append(rateCode);
		}
		if (position != null) {
			strblr.append("\n\t position : ").append(position);
		}
		if (timestamp != 0) {
			strblr.append("\n\t timestamp : ").append(String.format("%d", ((Double) timestamp).intValue()));
		}
		if (dateTime != null) {
			strblr.append("\n\t dateTime : ").append(dateTime);
		}
		if (dtTime > 0) {
			strblr.append("\n\t dtTime : ").append(String.format("%.0f", dtTime));
		}
		if (byteRange != null) {
			strblr.append("\n\t byteRange : ").append(byteRange);
		}
		if (contentType != null) {
			strblr.append("\n\t contentType : ").append(contentType);
		}
		if (contentLength != 0) {
			strblr.append("\n\t contentLength : ").append(contentLength);
		}
		if (contentSize != 0) {
			strblr.append("\n\t contentSize : ").append(contentSize);
		}
		if (contentStart != 0) {
			strblr.append("\n\t contentStart : ").append(contentStart);
		}
		if (contentEnd != 0) {
			strblr.append("\n\t contentEnd : ").append(contentEnd);
		}
		if (!failure.isEmpty()) {
			strblr.append("\n\t failure : ").append(failure);
		}

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
