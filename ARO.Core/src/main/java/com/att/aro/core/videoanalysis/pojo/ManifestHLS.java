package com.att.aro.core.videoanalysis.pojo;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

public class ManifestHLS extends AROManifest {

	private static final String EXTEN_TS = ".ts";
	int segCount = 0;
	
	@Override
	public int getSegIncremental() {
		return segCount++;
	}

	public ManifestHLS(HttpRequestResponseInfo req, String videoPath) {
		super(VideoType.HLS, req, videoPath);
	}
	
	public ManifestHLS(HttpRequestResponseInfo req, byte[] data, String videoPath) {
		super(VideoType.HLS, req, videoPath);
		
		content = data;
		exten = EXTEN_TS; // default for HLS
		parseManifestData(data);
	}

	public ManifestHLS(HttpRequestResponseInfo req, String videoName, byte[] data, String videoPath) {
		super(VideoType.HLS, req, videoPath);

		content = data;
		exten = EXTEN_TS; // default for HLS
		parseManifestData(data);
		if (videoName != null) {
			setVideoName(videoName);
		}
	}

	public void parseManifestData(byte[] data) {
		if (data == null || data.length == 0) {
			return;
		}
		String strData = new String(data);
		String[] sData = (new String(data)).split("[\n\r]");

		log.info("import Manifest:\n"+strData);
		
		if (encryption.isEmpty()) {
			encryption = StringParse.findLabeledDataFromString("#EXT-X-KEY:METHOD=", ",", strData);
			if (!encryption.isEmpty() && uriStr.isEmpty()) {
				uriStr = StringParse.findLabeledDataFromString("URI=\"", "\"", strData);
			}
		}
		
		if (duration == 0) {
			Double dblDuration = StringParse.findLabeledDoubleFromString("#EXTINF:", strData);
			if (dblDuration != null) {
				setDuration(dblDuration);
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

					String format = "";

					String[] capGroup = null;

					if (capGroup == null) {
						capGroup = stringParse.parse(nameLine, "channel\\/([a-z0-9]*)\\/([a-z])\\.m3u8"); // espn channel/852f3e08953a485d90f8c924a101b668/d.m3u8
						if (capGroup != null) {
							setVideoName(capGroup[0]);
							format = capGroup[1];
						}
					}
					if (capGroup == null) {
						capGroup = stringParse.parse(nameLine, "(^.+)\\/([a-zA-Z0-9]*)()\\.(.+)"); // DTV_Live 05/playlist.m3u8
						if (capGroup != null) {
							setVideoName(capGroup[1]);
							format = capGroup[0];
						}
					}

					if (capGroup == null) {
						capGroup = stringParse.parse(nameLine, "([a-zA-Z0-9]*)\\/([a-zA-Z0-9]*)\\d\\_(\\d+)\\.(.+)"); // VOD HLS0/B001950818U0_0.m3u8
						if (capGroup != null) {
							setVideoName(capGroup[1]);
							format = capGroup[0];
						}
					}
					if (capGroup == null) {
						capGroup = stringParse.parse(nameLine, "([a-zA-Z0-9]*)\\_(\\d+)\\.(.+)"); // AmzHLS 2e08c253-9a90-4db2-9e67-8baa3a152a00_v4.m3u8
						if (capGroup != null) {
							setVideoName(capGroup[0]);
							format = capGroup[1];
						}
					}
					if (capGroup == null) {
						capGroup = stringParse.parse(nameLine, "([a-zA-Z0-9]*)\\.(.+)"); // DTVNOW 03.m3u8
						if (capGroup != null) {
							setVideoName(capGroup[0]);
							format = capGroup[0];
						}
					}

					bitrateMap.put(format.toUpperCase(), bandWidth);
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
		
		loadSegments(sData);
	}


	/**
	 * Load transportStreams(.ts) into segment list
	 * 
	 * @param sData
	 */
	private void loadSegments(String[] sData) {
		String[] parsData;
		Integer lineSeg = 0;
		String segDuration = null;
		
		for (String line : sData) {
			if (line.startsWith("#EXTINF:")) {
				parsData = stringParse.parse(line, "#EXTINF:(.+),");
				if (parsData != null) {
					segDuration = parsData[0];
				}
			}
			if (line.contains(EXTEN_TS) && !segmentList.containsKey(line)) {
				parsData = stringParse.parse(line, "([a-zA-Z0-9]*\\..+)");
				if (parsData != null && !segmentList.containsKey(parsData[0])) {
					lineSeg = getSegIncremental();
					segmentList.put(parsData[0], lineSeg);
					durationList.put(lineSeg, segDuration);
				} else if (!segmentList.containsKey(line)) {
					segmentList.put(line, lineSeg);
					durationList.put(lineSeg, segDuration);
				}
			}
		}
	}

	@Override
	public Integer getSegment(String segName) {
		if (!segmentList.isEmpty()) {
			Integer val = segmentList.get(segName+EXTEN_TS);//FIXME get rid of ".ts" dependency see loadSegments(String[] sData)
			if (val != null) {
				return val;
			}
		}
		return -1;
	}
	
//	@Override
	
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("ManifestHLS:");
		strblr.append(super.toString());

		return strblr.toString();
	}

	@Override
	public int parseSegment(String fullName, VideoEventData ved) {
		if (ved.getSegment() != null) {
			return ved.getSegment();
		} else if (ved.getDateTime() != null) {
			Integer segment = getSegment(ved.getDateTime());
			ved.setSegment(segment);
			ved.setDuration(getDuration(segment));
			return ved.getSegment();
		}
		int segment = -1;
		int pos = fullName.lastIndexOf('_');
		int dot = fullName.lastIndexOf('.');
		if (pos > -1) {
			try {
				segment = Integer.valueOf(fullName.substring(pos + 1, dot));
			} catch (NumberFormatException e) {
				// don't care about error, report segment as -1
				segment = -1;
			}
		}
		ved.setSegment(segment);
		return segment;
	}
	
}