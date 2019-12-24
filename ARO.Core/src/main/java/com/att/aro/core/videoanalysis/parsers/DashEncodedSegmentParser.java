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
package com.att.aro.core.videoanalysis.parsers;

import java.util.List;

import org.springframework.util.CollectionUtils;

import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.AdaptationSetESL;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.MPDEncodedSegment;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.PeriodESL;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.RepresentationESL;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.MPDSegmentTimeline;
import com.att.aro.core.videoanalysis.parsers.smoothstreaming.SSM;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

public class DashEncodedSegmentParser extends DashParser {
	private MPDEncodedSegment mpdOut;
	@SuppressWarnings("unused")
	private SSM ssmOut;
	@SuppressWarnings("unused")
	private MPDSegmentTimeline mpdSegmentTimeline;
	
	public DashEncodedSegmentParser(MPDEncodedSegment mpdOut, Manifest manifest, ManifestCollection manifestCollection, ChildManifest childManifest) {
		super(VideoType.DASH, manifest, manifestCollection, childManifest);
		this.mpdOut = mpdOut;
	}
	
	public List<AdaptationSetESL> getAdaptationSet() {
		List<PeriodESL> period = mpdOut.getPeriod();
		return CollectionUtils.isEmpty(period) ? null : period.get(0).getAdaptationSet();
	}

	public List<RepresentationESL> getRepresentationAmz(String contentType) {
		List<RepresentationESL> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
		return videoRepresentationAmz;
	}
	
	@Override
	public String getDuration(String videoName) {

		String contentType = StringParse.findLabeledDataFromString("_", "_", videoName);
		if (mpdOut != null) {
			List<RepresentationESL> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationESL representationAmz : videoRepresentationAmz) {
					if (representationAmz != null && representationAmz.getBaseURL().endsWith(videoName)) {
						return representationAmz.getEncodedSegment().getDuration();
					}
				}
			}
		}
		return "";
	}

	public double getBandwith(String baseURL) {
		double bitrate = 0;
		String contentType = StringParse.findLabeledDataFromString("_", "_", baseURL);
		if (mpdOut != null) {
			List<RepresentationESL> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationESL representationAmz : videoRepresentationAmz) {
					if (representationAmz.getBaseURL().endsWith(baseURL)) {
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
			List<RepresentationESL> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationESL representationAmz : videoRepresentationAmz) {
					if (representationAmz.getBaseURL().endsWith(baseURL)) {
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

	private List<RepresentationESL> findAdaptationSet(MPDEncodedSegment mpdOut, String contentType) {
		List<PeriodESL> period = mpdOut.getPeriod();
		for (PeriodESL periodAmz : period) {
			List<AdaptationSetESL> adaptationSet = periodAmz.getAdaptationSet();
			if (adaptationSet != null) {
				for (AdaptationSetESL adaptationSetAmz : adaptationSet) {
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
		StringBuilder strblr = new StringBuilder("Manifest_Dash_Static, Size :");
		if (mpdOut != null) {
			int size = mpdOut.getPeriod().size();
			strblr.append(size);
		} else {
			strblr.append("mpdOut == null");
		}
		strblr.append('\n');
		strblr.append(super.toString());

		return strblr.toString();
	}

	public String getTimeScale(String videoName) {

		String contentType = StringParse.findLabeledDataFromString("_", "_", videoName);
		if (mpdOut != null) {
			List<RepresentationESL> videoRepresentationAmz = findAdaptationSet(mpdOut, contentType);
			if (videoRepresentationAmz != null) {
				for (RepresentationESL representationAmz : videoRepresentationAmz) {
					if (representationAmz != null && representationAmz.getBaseURL().endsWith(videoName)) {
						return representationAmz.getEncodedSegment().getTimescale();
					}
				}
			}
		}
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
