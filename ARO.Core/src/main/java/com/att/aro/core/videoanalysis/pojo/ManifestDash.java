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

import java.util.List;

import com.android.ddmlib.Log;
import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.AdaptationSetAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.MPDAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.PeriodAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.RepresentationAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.SegmentListAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.SegmentURL;

public class ManifestDash extends AROManifest {

	private MPDAmz mpdOut;

	public ManifestDash(MPDAmz mpdOut, HttpRequestResponseInfo req) {
		super(VideoType.DASH, req);
		this.mpdOut = mpdOut;
		parseManifestData();
	}

	public void parseManifestData() {
		if (mpdOut == null) {
			duration = 2;
			timeScale = 1;
			return;
		}
		List<RepresentationAmz> videoRepresentationAmz = findAdaptationSet(mpdOut, "video");
		if (videoRepresentationAmz != null) {
			for (RepresentationAmz representationAmz : videoRepresentationAmz) {
				if (videoName.isEmpty()) {
					videoName = representationAmz.getUrl();
					int pos = videoName.lastIndexOf('_');
					if (pos != -1) {
						videoName = videoName.substring(0, pos);
					}
				}
				if (representationAmz.getEncodedSegment() != null) {
					if (duration == 0) {
						duration = simpleStringToDouble(representationAmz.getEncodedSegment().getDuration());
					}

					if (timeScale == 0) {
						timeScale = simpleStringToDouble(representationAmz.getEncodedSegment().getTimescale());
					}
				} else if (representationAmz.getSegmentList() != null) {
					SegmentListAmz segList = representationAmz.getSegmentList();
					duration = simpleStringToDouble(segList.getDuration());
					timeScale = simpleStringToDouble(segList.getTimescale());
				}
				String quality = StringParse.subString(representationAmz.getUrl(), '_', '.');
				try {
					bitrateMap.put(quality, Double.valueOf(representationAmz.getBandwidth()));
				} catch (NumberFormatException e) {
					Log.e("ManifestDash NumberFormatException:", e.getMessage());
				}
			}
		}
	}

	/**
	 * <pre>
	 * Convert a String to double.
	 * parsing errors result in returning a 0
	 * 
	 * @param duration
	 * @return converted value or default to zero on error
	 */
	private double simpleStringToDouble(String duration) {
		double result;
		try {
			result = Double.parseDouble(duration);
		} catch (NumberFormatException e) {
			result = 0;
		}
		return result;
	}

	@Override
	public String getDuration(String videoName) {

		String contentType = StringParse.findLabeledDataFromString("_", "_", videoName);
		if (mpdOut != null) {
			List<RepresentationAmz> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationAmz representationAmz : videoRepresentationAmz) {
					if (representationAmz != null && representationAmz.getUrl().endsWith(videoName)) {
						return representationAmz.getEncodedSegment().getDuration();
					}
				}
			}
		}
		// not found so report -1
		return "";
	}

	public double getBandwith(String baseURL) {
		double bitrate = 0;
		String contentType = StringParse.findLabeledDataFromString("_", "_", baseURL);
		if (mpdOut != null) {
			List<RepresentationAmz> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationAmz representationAmz : videoRepresentationAmz) {
					if (representationAmz.getUrl().endsWith(baseURL)) {
						return Double.valueOf(representationAmz.getBandwidth());
					}
				}
			}
		}
		return bitrate;
	}

	private List<RepresentationAmz> findAdaptationSet(MPDAmz mpdOut, String contentType) {
		List<PeriodAmz> period = mpdOut.getPeriod();
		for (PeriodAmz periodAmz : period) {
			List<AdaptationSetAmz> adaptationSet = periodAmz.getAdaptationSet();
			if (adaptationSet != null) {
				for (AdaptationSetAmz adaptationSetAmz : adaptationSet) {
					if (adaptationSetAmz.getContentType().equals(contentType)) {
						return adaptationSetAmz.getRepresentation();
					}
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("ManifestDash, Size :");
		if (mpdOut != null) {
			int size = mpdOut.getPeriod().size();
			strblr.append(size);
			// strblr.append(", sample contentType:"); strblr.append(mpdOut.getPeriod().get(0).getAdaptationSet().get(0).getContentType());
		} else {
			strblr.append("mpdOut == null");
		}
		strblr.append('\n');
		strblr.append(super.toString());

		return strblr.toString();
	}

	/**
	 * Scan manifest for byte range assigned to a video
	 * 
	 * @param fullName
	 * @param range
	 * @return Segment - the position in byte range, -1 if not found
	 */
	@Override
	public int parseSegment(String fullName, ByteRange range) {
		if (range == null) {
			return -2;
		}
		String begin = range.getBeginByteHex();
		String end = range.getEndByteHex();

		int segment = 0;
		String contentType = StringParse.findLabeledDataFromString("_", "_", fullName);
		if (mpdOut != null) {
			List<RepresentationAmz> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationAmz representationAmz : videoRepresentationAmz) {
					if (representationAmz != null && representationAmz.getUrl().endsWith(fullName)) {
						
						if (representationAmz.getEncodedSegment() != null) {
							String encodedSegment = representationAmz.getEncodedSegment() != null ? representationAmz.getEncodedSegment().getEncodedSegmentListValue() : "";
							String[] byteRanges = encodedSegment.split(";");
							for (String line : byteRanges) {
								int pos = line.indexOf('-');
								if (pos > 0) {
									if (line.substring(0, pos).contains(begin) && line.substring(pos + 1).contains(end)) {
										return segment;
									} else {
										segment++;
									}
								}
							}
							
						} else {
							// SegmentURL mediaRange
							range.getBeginByte();
							range.getEndByte();
							String sRange;
							// check Initialization (segment zero)
							SegmentListAmz segList = representationAmz.getSegmentList();
							if (segList.getInitialization() != null) {
								sRange = segList.getInitialization().getRange();
								int val = matchRange(range, sRange);
								if (val == 1) {
									segment++;
								} else if (val == 0) {
									return segment;
								} else if (val == -1) {
									return -1;
								}
							}
							
							// scan all other segment ranges
							if (segList != null && !segList.getSegmentUrlList().isEmpty()) {
								for (SegmentURL segmentURL:segList.getSegmentUrlList()){
									sRange = segmentURL.getMediaRange();
									int val = matchRange(range, sRange);
									if (val == 1) {
										segment++;
									} else if (val == 0) {
										return segment;
									} else if (val == -1) {
										return -1;
									}
								}
							} else {
								return -1;
							}
						}
					}
				}
			}
		}
		// not found so report -1
		return -1;
	}
	
	private int matchRange(ByteRange range, String sRange) {
		int pos = sRange.indexOf('-');
		if (pos >= 0) {
			Integer bRange = Integer.parseInt(sRange.substring(0, pos));
			Integer eRange = Integer.parseInt(sRange.substring(pos + 1));
			if (bRange.equals(range.getBeginByte()) && eRange.equals(range.getEndByte())) {
				return 0;
			} else {
				return 1;
			}
		}
		return -1;
	}

	@Override
	public String getTimeScale(String videoName) {

		String contentType = StringParse.findLabeledDataFromString("_", "_", videoName);
		if (mpdOut != null) {
			List<RepresentationAmz> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationAmz representationAmz : videoRepresentationAmz) {
					if (representationAmz != null && representationAmz.getUrl().endsWith(videoName)) {
						return representationAmz.getEncodedSegment().getTimescale();
					}
				}
			}
		}
		// not found so report -1
		return "";
	}
	
}
