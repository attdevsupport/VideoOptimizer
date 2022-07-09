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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis.pojo;

import com.att.aro.core.packetanalysis.pojo.ByteRange;

import lombok.Data;

@Data
public class VideoEventData {
	                                         
	private String cdn;
	private String name="";
	private Integer segment;
	private String segmentReference;
	private Double byteStart;
	private Double byteEnd;
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
	private boolean segmentAutoCount = false;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(60);
		strblr.append("VideoEventData ");
		if (cdn != null) {
			strblr.append("\n\t cdn : ").append(cdn);
		}
		if (!name.isEmpty()) {
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

	public ByteRange getByteRange() {
		if (byteRange == null) {
			if (byteStart != null && byteEnd != null) {
				byteRange = new ByteRange(byteStart.intValue(), byteEnd.intValue());
			} else {
				return new ByteRange(0, 0);
			}
		}

		return byteRange;
	}

}
