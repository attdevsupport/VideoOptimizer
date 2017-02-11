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

import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

public class ManifestDTV extends AROManifest {

	public ManifestDTV(HttpRequestResponseInfo req) {
		super(VideoType.HLS, req);
	}

	public ManifestDTV(HttpRequestResponseInfo req, byte[] data) {
		super(VideoType.HLS, req);
		parseManifestData(data);
	}

	public void parseManifestData(byte[] data) {
		String strData = new String(data);
		String[] sData = (new String(data)).split("\n");

		if (encryption.isEmpty()) {
			encryption = StringParse.findLabeledDataFromString("#EXT-X-KEY:METHOD=", ",", strData);
			if (!encryption.isEmpty() && uriStr.isEmpty()) {
				uriStr = StringParse.findLabeledDataFromString("URI=\"", "\"", strData);
			}
		}
		
		if (duration == 0) {
			Double dblDuration = StringParse.findLabeledDoubleFromString("TARGETDURATION:", strData);
			if (dblDuration != null) {
				duration = dblDuration;
				timeScale = 1;
			}
		}

		if (bitrateMap.isEmpty()) {
			for (int itr = 0; itr < sData.length; itr++) {
				String line = sData[itr];
				// bandwidth
				Double bandWidth = StringParse.findLabeledDoubleFromString("BANDWIDTH=", line);
				if (bandWidth != null && ++itr < sData.length) {
					String nameLine = sData[itr];
					int sep = nameLine.indexOf('/');
					int uScore = nameLine.indexOf('_');
					int dot = nameLine.indexOf('.');

					String format = nameLine.substring(uScore + 1, dot);
					bitrateMap.put(format, bandWidth);
					if (videoName.isEmpty()) {
						if (uScore > sep) {
							videoName = nameLine.substring(sep + 1, uScore);
						} else if (dot > sep){
							videoName = nameLine.substring(sep + 1, dot);
						}
					}

				}
			}
		} else {
			String val = StringParse.findLabeledDataFromString("_", "\\.", sData[sData.length - 2]);
			int pos = val.indexOf('_');
			if (pos > -1) {
				try {
					double dVal = Double.valueOf(val.substring(pos + 1));
					segmentCount = dVal;
				} catch (NumberFormatException e) {
					segmentCount = 0;
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("ManifestDTV:");
		strblr.append(super.toString());

		return strblr.toString();
	}

	@Override
	public int parseSegment(String fullName, ByteRange range) {
		int segment = -1;
		int pos = fullName.lastIndexOf('_');
		int dot = fullName.lastIndexOf('.');
		if (pos > -1) {
			try {
				segment = Integer.valueOf(fullName.substring(pos + 1, dot));
			} catch (NumberFormatException e) {
				// don't care about error, report segment as 0
				segment = -1;
			}
		}
		return segment;
	}
}