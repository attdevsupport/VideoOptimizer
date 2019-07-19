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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.att.aro.core.videoanalysis.parsers.segmenttimeline.AdaptationSetTL;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.MPDSegmentTimeline;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.PeriodST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.RepresentationST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.SegmentST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.SegmentTemplateST;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

public class DashSegmentTimelineParser extends DashParser {
	
	private MPDSegmentTimeline mpd;
	int segCount = 0;
	
	@Override
	public int getSegIncremental() {
		return segCount++;
	}
	
	public DashSegmentTimelineParser(MPDSegmentTimeline mpdSegmentTimeline, Manifest manifest, ManifestCollection manifestCollection, ChildManifest childManifest) {
		super(VideoType.DASH, manifest, manifestCollection, childManifest);
		this.mpd = mpdSegmentTimeline;
	}

	@Override
	public Double getBitrate(String key) {
		Double rate = super.getBitrate(key);
		if (rate == null) {
			key = "video_eng=" + key;
			rate = bitrateMap.get(key);
		}
		return rate;
	}

	@Override
	public void updateManifest(MpdBase manifest) {
		MPDSegmentTimeline mani = (MPDSegmentTimeline) manifest;
		AdaptationSetTL adSet = findAdaptationSet(mani, "video", null);
		loadSegments(adSet);
	}
	
	/**
	 * @param adaptationSet 
	 * 
	 */
	public void loadSegments(AdaptationSetTL adaptationSet) {
		if (adaptationSet != null) {
			SegmentTemplateST segmentTemplate = adaptationSet.getSegmentTemplate();
			if (segmentTemplate != null) {
				setTimeScale(simpleStringToDouble(segmentTemplate.getTimescale()));

				// example data <S t="3252609360000" d="60060000" r="13" />
				for (SegmentST segmentPR : segmentTemplate.getSegmentTimeline().getSegmentList()) {
					if (duration == 0) {
						duration = simpleStringToDouble(segmentPR.getDuration()) / getTimeScale();
					}

					Double segTimeLine = simpleStringToDouble(segmentPR.getStartTime());
					Double segment = segTimeLine / duration / getTimeScale();
					addSegment(segmentPR.getStartTime(), segment.intValue(), segmentPR.getDuration());
					LOG.info(String.format("base> %s :%s", videoName, timeScale));
				}
			}
		}
	}

	private void addSegment(String segTimeline, Integer segment, String segDuration) {
		int lineSeg = segment == null ? getSegIncremental() : segment;
		if (!segmentList.containsKey(segTimeline)) {
			segmentList.put(segTimeline, lineSeg);
			durationList.put(lineSeg, segDuration);
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
	private Double simpleStringToDouble(String duration) {
		Double result;
		try {
			result = Double.parseDouble(duration);
		} catch (NumberFormatException e) {
			result = 0D;
		}
		return result;
	}

	/**
	 * LocatesAdaptationSetPR with a matching mimeType
	 * 
	 * @param contentType
	 * @return
	 */
	public AdaptationSetTL findAdaptationSet(String mimeType) {
		return findAdaptationSet(this.mpd,  mimeType, null);
	}

	/**
	 * LocatesAdaptationSetPR with a matching mimeType and optionally bandwidth
	 * @param string 
	 * @param mani 
	 * 
	 * @param contentType
	 * @return
	 */
	private AdaptationSetTL findAdaptationSet(MPDSegmentTimeline mani, String mimeType, String bandwidth) {
		List<PeriodST> period = mani.getPeriod();
		for (PeriodST periodPr : period) {
			List<AdaptationSetTL> adaptationSet = periodPr.getAdaptationSet();
			if (adaptationSet != null) {
				for (AdaptationSetTL adaptationSetPR : adaptationSet) {
					if (adaptationSetPR.getMimeType().startsWith(mimeType)) {
						if (bandwidth != null) {
							for (RepresentationST videoRepresentationList:adaptationSetPR.getRepresentation()){
								if (videoRepresentationList.getBandwidth().equals(bandwidth)){
									return adaptationSetPR;
								}
							}
							
						} else {
							return adaptationSetPR;
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
		StringBuilder strblr = new StringBuilder(42);
		strblr.append("Manifest_Dash_SegmentTimeLine");
		if (mpd != null) {
			strblr.append(", Size :");
			strblr.append(mpd.getPeriod().size());
			strblr.append(", TimeScale :");
			strblr.append(numberFormat.format(timeScale));
		} else {
			strblr.append("mpdSegmentTimeline == null");
		}
		strblr.append('\n');
		strblr.append(super.toString());

		return strblr.toString();
	}

	@Override
	public void setTimeScale(Double timeScale) {
		super.setTimeScale((timeScale == null || timeScale == 0) ? 1D : timeScale);
	}
	
}
