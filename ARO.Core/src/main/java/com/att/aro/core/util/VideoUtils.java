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
package com.att.aro.core.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

public class VideoUtils {

	private static final String TRAFFIC_CAP = "traffic.cap";
	private static final String SECURE_TRAFFIC_CAP = "secure_traffic.cap";
	public static final String TRAFFIC = "traffic";
	public static final String[] TRAFFIC_EXTENTIONS = {".cap", ".pcap"};
	public static final String VIDEO = "video";
	public static final String[] VIDEO_EXTENTIONS = {".mp4", ".mov"};

	/**<pre>
	 * Examine traceFolder for .cap, .pcap, .pcapng, .mov, .mp4
	 *  Keys:video & traffic
	 * 
	 * If Map is not 2 entries then traceFolder is not eligible  to be handled as a VO trace folder
	 * 
	 * @param traceFolder
	 * @return Map<String, String[]>
	 */
	static public Map<String, String[]> validateFolder(File traceFolder) {
		Map<String, String[]> fileMap = new HashMap<String, String[]>();
		if (traceFolder.isDirectory()) {
			fileMap.putAll(validateFolder(traceFolder, VIDEO, VIDEO_EXTENTIONS));
			fileMap.putAll(validateFolder(traceFolder, TRAFFIC, TRAFFIC_EXTENTIONS));
		}
		return fileMap;
	}

	static public Map<String, String[]> validateFolder(File traceFolder, String key, String... extensions ) {
		Map<String, String[]> fileMap = new HashMap<String, String[]>();
		if (traceFolder.isDirectory()) {
			String[] temp = traceFolder.list(new CustomFilenameFilter(extensions));
			if (TRAFFIC.equals(key) && temp.length == 2) {
				// special case filter, if traffic and secure_traffic then forget secure_traffic
				if (temp[0].equals(TRAFFIC_CAP) && temp[1].equals(SECURE_TRAFFIC_CAP) || temp[1].equals(TRAFFIC_CAP) && temp[0].equals(SECURE_TRAFFIC_CAP)) {
					temp = new String[] { TRAFFIC_CAP };
				}
			}
			if (temp != null && temp.length > 0) {
				fileMap.put(key, temp);
			}
		}
		return fileMap;
	}
	
	static class CustomFilenameFilter implements FilenameFilter {

		private String[] extension;

		public CustomFilenameFilter(String... extension) {
			this.extension = extension;
		}

		@Override
		public boolean accept(File dir, String fileName) {
			if (!fileName.startsWith(".")) { // skip anything starting with a dot
				for (int idx = 0; idx < extension.length; idx++) {
					if (fileName.toLowerCase().contains(extension[idx])) {
						return true;
					}
				}
			}
			return false;
		}
	}
	
}
