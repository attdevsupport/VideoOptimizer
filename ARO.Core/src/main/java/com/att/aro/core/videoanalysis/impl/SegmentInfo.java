/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.videoanalysis.impl;

import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class SegmentInfo {
	@Getter(AccessLevel.NONE)
	private int segmentID;
	ChildManifest childManifest = null;
	private Manifest.ContentType contentType = Manifest.ContentType.UNKNOWN;
	private double startTime;
	private double duration;
	private boolean video;
	private String quality = "";
	private int size;
	private double bitrate;
	private int resolutionHeight = 0;
	private boolean thumbnailExtracted;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(" SegmentInfo :");
		strblr.append(" segmentID :").append(getSegmentID());
		strblr.append(", contentType :").append(contentType);
		strblr.append(String.format(", startTime   :%.6f", startTime));
		strblr.append(", duration :").append(duration);
		strblr.append(", video :").append(video);
		strblr.append(", quality :").append(quality);
		strblr.append(", size :").append(size);
		strblr.append(", bitrate :").append(bitrate);
		return strblr.toString();
	}

	public int getSegmentID() {
		if (childManifest != null && childManifest.getManifestCollectionParent() != null && childManifest.getSequenceStart() > 0) {
			return segmentID + (childManifest.getSequenceStart() - childManifest.getManifestCollectionParent().getMinimumSequenceStart());
		} else {
			return segmentID;
		}
	}
}
