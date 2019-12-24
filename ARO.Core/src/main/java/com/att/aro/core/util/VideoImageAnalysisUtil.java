/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.util;

import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;

public final class VideoImageAnalysisUtil {
	private static Logger LOG = LogManager.getLogger(VideoImageAnalysisUtil.class.getName());

	public static String getTimeString(HttpRequestResponseInfo response) {
		StringBuffer strTime = new StringBuffer();
		try {
			strTime.append(String.format("%09.0f", (float) response.getTimeStamp() * 1000));
		} catch (Exception e) {
			LOG.error("Failed to get time from request: " + e.getMessage());
			strTime.append("Failed to get time from response->request: " + response);
		}
		return strTime.toString();
	}

	/**
	 * Locate and return extension from filename
	 *
	 * @param src
	 * @return String extension with the dot(.)
	 */
	public static String extractExtensionFromName(String src, IStringParse stringParse) {
		Pattern extensionPattern = Pattern.compile("^\\b[a-zA-Z0-9]*\\b([\\.a-zA-Z0-9]*\\b)");

		String[] matched = stringParse.parse(src, extensionPattern);
		if (matched != null && !matched[0].isEmpty()) {
			return matched[0];
		}
		String extension = null;
		int pos = src.lastIndexOf('.');
		extension = (pos == -1 ? null : src.substring(pos));
		return extension;
	}
	
	/**
	 * Parse filename out of URI in HttpRequestResponseInfo
	 *
	 * @param rrInfo HttpRequestResponseInfo
	 * @return
	 */
	public static String extractFullNameFromRRInfo(HttpRequestResponseInfo rrInfo) {
		String URI = rrInfo.getObjNameWithoutParams();
		int pos = URI.lastIndexOf("/");
		String fullName = URI.substring(pos + 1, URI.length());
		return fullName;
	}
}