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

/**
 * <pre>
 * Contains: - width and height in pixels - bandwidth
 */
@Data
public class ChildManifest {
	String uriName = "";
	double bandwidth;
	String codecs = "";
	int quality;
	int pixelWidth;
	int pixelHeight;
	int segmentCount = 0;
	boolean video;
	Manifest manifest;
	byte[] moovContent;

	/**<pre>
	 * PatriciaTrie of SegmentInfo
	 * Key: segmentUriName
	 */
	PatriciaTrie<SegmentInfo> segmentList = new PatriciaTrie<>();

	public int getNextSegmentID() {
		return segmentCount++;
	}

	public boolean addSegment(String segmentUriName, SegmentInfo segmentInfo) {
		if (!segmentList.containsKey(segmentUriName)) {
			segmentList.put(segmentUriName, segmentInfo);
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\n\tChildManifest :");
		if (manifest != null) {
			strblr.append("\n\t\t\tVideoName   :").append(manifest.getVideoName());
			strblr.append("\n\t\t\tTimestamp   :").append(String.format("%.4f:", manifest.getRequestTime()));
		}
		strblr.append("\n\t\t\tUriName     :").append(uriName);
		strblr.append("\n\t\t\tVideo       :").append(video);
		strblr.append("\n\t\t\tContentType :").append(getContentType());
		strblr.append("\n\t\t\tSegmentCount:").append(segmentCount);
		strblr.append("\n\t\t\tBandwidth   :").append(bandwidth);
		strblr.append("\n\t\t\tCodecs      :").append(codecs);
		strblr.append("\n\t\t\tQuality     :").append(quality);
		strblr.append("\n\t\t\tPixelWidth  :").append(pixelWidth);
		strblr.append("\n\t\t\tPixelHeight :").append(pixelHeight);
		strblr.append(dumpSegmentList());
		if (manifest != null && manifest.getContent() != null) {
			String strContent = new String(manifest.getContent());
			int len = strContent.length();
			if (len > 700) {
				strContent = strContent.substring(0, 700) + "...\ttruncated\n";
			}
			strblr.append("\nManifest file contents:\n" + strContent);
		}

		return strblr.toString();
	}

	public String dumpSegmentList() {
		StringBuilder strblr = new StringBuilder();
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
	}
	
	public ContentType getContentType() {
		if (manifest==null) {
			return ContentType.UNKNOWN;
		}
		return manifest.getContentType();
	}
}
