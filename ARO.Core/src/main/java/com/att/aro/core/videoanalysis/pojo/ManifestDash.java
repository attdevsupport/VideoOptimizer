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
import com.att.aro.core.videoanalysis.pojo.amazonvideo.SSMAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.SegmentListAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.SegmentURL;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.XmlManifestHelper;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.MPDPlayReady;

public class ManifestDash extends AROManifest {
	private MPDAmz mpdOut;
	@SuppressWarnings("unused")
	private SSMAmz ssmOut;
	@SuppressWarnings("unused")
	private MPDPlayReady mpdPlayReadyOut;

	public ManifestDash(HttpRequestResponseInfo resp, byte[] content, String videoPath) {
		super(VideoType.DASH, resp, videoPath);
		XmlManifestHelper mani = new XmlManifestHelper(content);
		if (mani.getManifestType().equals(XmlManifestHelper.ManifestFormat.SmoothStreamingMedia)) {
			this.ssmOut = (SSMAmz) mani.getManifest();
		} else if (mani.getManifestType().equals(XmlManifestHelper.ManifestFormat.MPD_PlayReady)) {
			this.mpdPlayReadyOut = (MPDPlayReady) mani.getManifest();
		} else {
			this.mpdOut = (MPDAmz) mani.getManifest();
		}
		parseManifestData();
	}

	public ManifestDash(MPDAmz mpdOut, HttpRequestResponseInfo resp, String videoPath) {
		super(VideoType.DASH, resp, videoPath);
		this.mpdOut = mpdOut;
		parseManifestData();
	}

	public void parseManifestData() {
		if (mpdOut == null) {
			duration = 2D;
			timeScale = 1D;
			return;
		}
		List<RepresentationAmz> videoRepresentationAmz = findAdaptationSet(mpdOut, "video");
		if (videoRepresentationAmz != null) {
			for (RepresentationAmz representationAmz : videoRepresentationAmz) {
				if (videoName.isEmpty()) {
					singletonSetVideoName(representationAmz.getUrl());
					String[] str = stringParse.parse(videoName, "(.+)_([a-zA-Z_0-9\\-]*)_\\d+(\\.[a-zA-Z_0-9]*)");
					if (str.length == 3) {
						exten = str[2];
						videoName = str[0];// +"_"+str[1];
					}
				}
				if (representationAmz.getEncodedSegment() != null) {
					if (duration == 0) {
						duration = simpleStringToDouble(representationAmz.getEncodedSegment().getDuration());
					}

					if (timeScale <= 1) {
						setTimeScale(simpleStringToDouble(representationAmz.getEncodedSegment().getTimescale()));
					}
				} else if (representationAmz.getSegmentList() != null) {
					SegmentListAmz segList = representationAmz.getSegmentList();
					duration = simpleStringToDouble(segList.getDuration());
					setTimeScale(simpleStringToDouble(segList.getTimescale()));
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

	/**
	 * Parse out Height from manifest
	 * 
	 * @param baseURL
	 * @return
	 */
	public double getHeight(String baseURL) {
		String contentType = StringParse.findLabeledDataFromString("_", "_", baseURL);
		if (mpdOut != null) {
			List<RepresentationAmz> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationAmz representationAmz : videoRepresentationAmz) {
					if (representationAmz.getUrl().endsWith(baseURL)) {
						return valueOfDouble(representationAmz.getHeight());
					}
				}
			}
		}
		return 0;
	}

	/**
	 * Convert String to Double with a default of zero 
	 * 
	 * @param strNumber
	 * @return value or 0 if not parsable
	 */
	private double valueOfDouble(String strNumber) {
		try {
			return Double.valueOf(strNumber);
		} catch (NumberFormatException e) {
			LOG.error("NumberFormatException: '" + strNumber + "' not parsable into Double");
			return 0;
		}
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
	 * @param ved - VideoEventData
	 * @return Segment - the position in byte range, -1 if not found
	 */
	@Override
	public int parseSegment(String fullName, VideoEventData ved) {
		if (ved.getSegment() != null && ved.getSegment().compareTo(0) > 0) {
			return ved.getSegment();
		}
		if (ved.getByteRange() == null) {
			return -2;
		}
		String begin = ved.getByteRange().getBeginByteHex();
		String end = ved.getByteRange().getEndByteHex();

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
										ved.setSegment(segment);
										return segment;
									} else {
										segment++;
									}
								}
							}
							
						} else {
							// SegmentURL mediaRange
							ved.getByteRange().getBeginByte();
							ved.getByteRange().getEndByte();
							String sRange;
							// check Initialization (segment zero)
							SegmentListAmz segList = representationAmz.getSegmentList();
							if (segList!= null && segList.getInitialization() != null) {
								sRange = segList.getInitialization().getRange();
								int val = matchRange(ved.getByteRange(), sRange);
								if (val == 1) {
									segment++;
								} else if (val == 0) {
									ved.setSegment(segment);
									return segment;
								} else if (val == -1) {
									return -1;
								}
							}
							
							// scan all other segment ranges
							if (segList != null && !segList.getSegmentUrlList().isEmpty()) {
								for (SegmentURL segmentURL:segList.getSegmentUrlList()){
									sRange = segmentURL.getMediaRange();
									int val = matchRange(ved.getByteRange(), sRange);
									if (val == 1) {
										segment++;
									} else if (val == 0) {
										ved.setSegment(segment);
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
	
	@Override
	public void setTimeScale(Double timeScale) {
		if (timeScale == 0){
			timeScale = 1D;
		}
		super.setTimeScale(timeScale);
	}
}
