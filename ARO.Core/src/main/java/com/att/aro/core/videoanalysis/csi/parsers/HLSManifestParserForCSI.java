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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis.csi.parsers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.video.pojo.Track;
import com.att.aro.core.video.pojo.VideoManifest;

@Lazy
@Component
public class HLSManifestParserForCSI {

	public HLSManifestParserForCSI() {
		super();
	}

	private static final Logger LOGGER = LogManager.getLogger(HLSManifestParserForCSI.class.getName());
	protected static final Pattern pattern = Pattern.compile("^(#[A-Z0-9\\-]*)");
	
	@Autowired
	private IStringParse stringParse;

	public VideoManifest getManifest(VideoManifest videoManifest, File manifestFile) {

		List<String> listCManifestInFolder = listMInFolder(manifestFile);// list all m3u8 files in the folder
		String[] sData = readFileToStringArray(manifestFile);
		List<Track> listTrack = parseHLSMasterManifest(sData);
		List<String> stringTrackName = listTrack.stream().map(track -> track.getTrackName())
				.collect(Collectors.toList());// list extract from master manifest
		Map<String, List<String>> mInFolder = new HashMap<String, List<String>>();

		for (String trackName : stringTrackName) {
			mInFolder.put(trackName, new ArrayList<String>());
		}

		for (String listMFile : listCManifestInFolder) {
			for (String trackName : mInFolder.keySet()) {
				if (listMFile.contains(trackName)) {
					mInFolder.get(trackName).add(listMFile);
				}
			}
		}

		for (Track track : listTrack) {
			if (mInFolder.containsKey(track.getTrackName())) {
				if (mInFolder.get(track.getTrackName()).isEmpty())
					continue;
				String fileName = mInFolder.get(track.getTrackName()).iterator().next();
				LOGGER.debug("fileName: " + fileName);
				parseHLSChildManifest(track, readFileToStringArray(new File(fileName)));
			}
		}

		videoManifest.setTracks(listTrack);
		return videoManifest;
	}

	public List<String> listMInFolder(File manifestFile) {

		List<String> listFResult = new ArrayList<String>();
		try (Stream<Path> paths = Files.walk(Paths.get(manifestFile.getParent()))) {
			listFResult = paths.map(file -> file.toString()).filter(file -> file.endsWith("m3u8"))
					.collect(Collectors.toList());
		} catch (IOException ioe) {
			LOGGER.error(ioe.getMessage());
		}
		return listFResult;
	}

	public String[] readFileToStringArray(File manifestFile) {
		Path path = Paths.get(manifestFile.getPath());
		String[] arr = {};
		try {
			arr = Files.readAllLines(path).stream().toArray(String[]::new);
		} catch (IOException ioe) {
			LOGGER.error(ioe.getMessage());
		}
		return arr;
	}

	public List<Track> parseHLSMasterManifest(String[] sDataArray) {
		List<Track> tracks = new ArrayList<>();

		if (sDataArray == null || sDataArray.length == 0) {
			LOGGER.debug("Manifest file has no content");

		} else {

			for (int i = 0; i < sDataArray.length; i++) {
				if (sDataArray[i] == null || !sDataArray[i].startsWith("#"))
					continue;
				String[] flag = stringParse.parse(sDataArray[i], pattern);
				if (flag == null || flag.length == 0)
					continue;
				String childUriName = "";
				switch (flag[0]) {

				case "#EXT-X-STREAM-INF": // master
					String mediaBandwidth = getStrBwn(sDataArray[i], '=', ',');
					childUriName = sDataArray[++i];
					Track trackVideo = new Track();
					trackVideo.setTrackName(childUriName);
					trackVideo.setMediaBandwidth(Float.valueOf(mediaBandwidth));
					tracks.add(trackVideo);
					break;

				case "#EXT-X-MEDIA": // master
					childUriName = StringParse.findLabeledDataFromString("URI=", "\"", sDataArray[i]);
					if (StringUtils.isNotEmpty(childUriName)) {
						LOGGER.info("MEDIA childUriName :" + childUriName);
					}
					Track trackAudio = new Track();
					trackAudio.setTrackName(childUriName);
					tracks.add(trackAudio);
					break;

				default:
					break;
				}// end switch

			} // end for loop
		}

		return tracks;
	}

	public void parseHLSChildManifest(Track track, String[] sDataArray) {
		if (sDataArray == null || sDataArray.length == 0)
			return;
		List<Integer> segmentSizes = new ArrayList<Integer>();
		List<Double> segmentDurations = new ArrayList<Double>();
		try {
			for (int i = 0; i < sDataArray.length; i++) {
				String sData = sDataArray[i];
				if (sData.startsWith("#EXTINF")) {
					String segmentDuration = getStrBwn(sData, ':', ',');
					segmentDurations.add(Double.valueOf(segmentDuration));
				} else if (sData.startsWith("#EXT-X-BYTERANGE")) {
					String segmentSize = getStrBwn(sData, ':', '@');
					segmentSizes.add(Integer.valueOf(segmentSize));
					i++;
				} else if (sData.startsWith("http")) {
					String[] flag1 = sData.split("/");
					if (Arrays.stream(flag1).anyMatch("range"::equals)) {
						String[] flag2 = flag1[flag1.length - 1].split("-");
						if (flag2.length > 1) {
							try {
								Integer number = Integer.valueOf(flag2[flag2.length - 1])
										- Integer.valueOf(flag2[flag2.length - 2]);
								segmentSizes.add(number);
							} catch (IndexOutOfBoundsException iobe) {
								LOGGER.error(iobe.getMessage());
							}
						}
					}
				} else {
					LOGGER.debug("no match to pass");
				}

			} // end for loop
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		track.setSegmentDurations(segmentDurations);
		track.setSegmentSizes(segmentSizes);

	}

	public String getStrBwn(String str, char fst, char snd) {
		
		String result = "";
		int startIndex = str.indexOf(fst);
		if (startIndex != -1) {
			int endIndex = str.indexOf(snd, startIndex + 1);
			if (endIndex != -1) {
				result = str.substring(startIndex + 1, endIndex);
			}
		}
		return result;
	}

}
