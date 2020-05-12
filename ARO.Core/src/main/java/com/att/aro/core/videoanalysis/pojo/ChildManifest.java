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

import java.util.Iterator;

import org.apache.commons.collections4.trie.PatriciaTrie;

import com.att.aro.core.videoanalysis.impl.SegmentInfo;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;

import lombok.Data;

@Data
public class ChildManifest {
	private String uriName = "";
	private double bandwidth;
	private String codecs = "";
	private int quality;
	private int pixelWidth;
	private int pixelHeight;
	private int segmentCount = 0;
	private double initialStartTime = -1D;
	private boolean video;
	private Manifest manifest;
	private double segmentStartTime;
	protected ContentType contentType = ContentType.UNKNOWN;
	
	private byte[] moovContent;

	/**<pre>
	 * PatriciaTrie of SegmentInfo
	 * Key: segmentUriName
	 */
	private PatriciaTrie<SegmentInfo> segmentList = new PatriciaTrie<>();

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\n\tChildManifest :");
		strblr.append("\n\t\t\tVideoName        :").append(manifest == null ? "-" : manifest.getVideoName());
		strblr.append("\n\t\t\tReqTimestamp     :").append(manifest == null ? "-" : String.format("%.4f:", manifest.getRequestTime()));
		strblr.append("\n\t\t\tProgramDateTime  :").append(manifest == null ? "-" : String.format("%.3f:", manifest.getProgramDateTime()));
		strblr.append("\n\t\t\tSegmentCount     :").append(segmentCount);
		strblr.append("\n\t\t\tSegmentStartTime :").append(String.format("%.3f:", segmentStartTime));
		strblr.append("\n\t\t\tUriName          :").append(uriName);
		strblr.append("\n\t\t\tVideo            :").append(video);
		strblr.append("\n\t\t\tContentType      :").append(getContentType());
		strblr.append("\n\t\t\tBandwidth        :").append(bandwidth);
		strblr.append("\n\t\t\tCodecs           :").append(codecs);
		strblr.append("\n\t\t\tQuality          :").append(quality);
		strblr.append("\n\t\t\tPixelWidth       :").append(pixelWidth);
		strblr.append("\n\t\t\tPixelHeight      :").append(pixelHeight);
		strblr.append(dumpSegmentList());
		strblr.append(dumpManifest(700));

		return strblr.toString();
	}

	public void setStreamProgramDateTime(double programDateTime) {
		manifest.updateStreamProgramDateTime(programDateTime);
	}
	
	public int getNextSegmentID() {
		return segmentCount++;
	}
	
	private void incrementSegmentStartTime(double duration) {
		segmentStartTime += duration;
	}
	

	/**
	 * Add to segmentList if not already there.
	 * 
	 * @param segmentUriName
	 * @param segmentInfo
	 * @return true if can add to segmentList, false is already there
	 */
	public SegmentInfo addSegment(String segmentUriName, SegmentInfo segmentInfo) {
		if (!segmentList.containsKey(segmentUriName)) {
			if (segmentInfo.getStartTime() == 0) {
				segmentInfo.setStartTime(getSegmentStartTime());
				incrementSegmentStartTime(segmentInfo.getDuration());
			}
			segmentList.put(segmentUriName, segmentInfo);
			if (segmentInfo.getContentType().equals(ContentType.UNKNOWN)) {
				segmentInfo.setContentType(getContentType());
			}
			return segmentInfo;
	}
		return segmentList.get(segmentUriName);

	}

	public String dumpManifest(int cutoff) {
		StringBuilder strblr = new StringBuilder();
		if (manifest != null && manifest.getContent() != null) {
			String strContent = new String(manifest.getContent());
			int len = strContent.length();
			if (cutoff > 0 && len > cutoff) {
				strContent = strContent.substring(0, cutoff) + "...\ttruncated\n";
			}
			strblr.append("\nManifest file contents:\n" + strContent);
		} else {
			strblr.append("No manifest data");
		}
		return strblr.toString();
	}

	public String dumpSegmentList() {
		StringBuilder strblr = new StringBuilder("SegmentList:");
		Iterator<String> keys = segmentList.keySet().iterator();
		while (keys.hasNext()) {// && limit-- > 0) {
			String key = keys.next();
			strblr.append("\n\t\t\t\t<").append(key + ">: " + segmentList.get(key));
		}
		strblr.append("\n");
		return strblr.toString();
	}
	
	public void setContentType(ContentType contentType) {
		if (manifest != null) {
			manifest.setContentType(contentType);
		} 
		this.contentType = contentType;
	}
	
	public ContentType getContentType() {
		if (manifest == null) {
			return this.contentType;
		}
		return manifest.getContentType();
	}
	
	public void setManifest(Manifest manifest) {
		this.manifest = manifest;
		if (!contentType.equals(ContentType.UNKNOWN)) {
			manifest.setContentType(this.contentType);
		}
	}
}
