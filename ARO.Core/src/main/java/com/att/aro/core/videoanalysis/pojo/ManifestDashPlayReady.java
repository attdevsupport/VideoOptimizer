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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.android.ddmlib.Log;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.MpdBase;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.AdaptationSetPR;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.MPDPlayReady;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.PeriodPR;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.RepresentationPR;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.SegmentPR;
import com.att.aro.core.videoanalysis.pojo.mpdplayerady.SegmentTemplatePR;

public class ManifestDashPlayReady extends AROManifest {
	
	private MPDPlayReady mpd;

	public ManifestDashPlayReady(MPDPlayReady mpd, HttpRequestResponseInfo resp, String videoPath) {
		super(VideoType.DASH, resp, null, videoPath);
		this.mpd = mpd;
		parseManifestData();
	}
	
	@Override
	public void updateManifest(MpdBase manifest) {
		MPDPlayReady mani = (MPDPlayReady) manifest;
		AdaptationSetPR adSet = findAdaptationSet(mani, "video", null);
		loadSegments(adSet);
		
//		SegmentPR topSegment = adSet.getSegmentTemplate().getSegmentTimeline().getSegmentList().get(0);
//		
//		log.info(String.format("update  : %s :%s", adSet.getRepresentation().get(0).getId(), topSegment.getTimeline()));
	}
	
	public void parseManifestData() {
		if (mpd == null) {
			duration = 2D;
			timeScale = 1D;
			return;
		}

		AdaptationSetPR adaptationSetVideo = findAdaptationSet("video");
		if (adaptationSetVideo == null) {
			return;
		}
		List<RepresentationPR> videoRepresentationList = adaptationSetVideo.getRepresentation();

		loadSegments(adaptationSetVideo);

		if (videoRepresentationList != null) {
			for (RepresentationPR representationPR : videoRepresentationList) {

				String[] names = stringParse.parse(representationPR.getId(), "(.+)-(\\d*)(.+)"); // id="1500581441685item-1item"
				if (names == null || names.length < 3) {
					return;
				}
				setVideoName(names[0]);
				mpd.setName(videoName);

				try {
					bitrateMap.put(names[1], Double.valueOf(representationPR.getBandwidth()));
				} catch (NumberFormatException e) {
					Log.e("ManifestDash NumberFormatException:", e.getMessage());
				}
			}

		}
	}
	
	/**
	 * @param adaptationSet 
	 * 
	 */
	public void loadSegments(AdaptationSetPR adaptationSet) {
		if (adaptationSet != null) {
			SegmentTemplatePR segmentTemplate = adaptationSet.getSegmentTemplate();
			if (segmentTemplate != null) {
				setTimeScale(simpleStringToDouble(segmentTemplate.getTimescale()));

				// <S t="3252609360000" d="60060000" r="13" />
				for (SegmentPR segmentPR : segmentTemplate.getSegmentTimeline().getSegmentList()) {
//					Double repeat = simpleStringToDouble(segmentPR.getRepeat());
					if (duration == 0) {
						duration = simpleStringToDouble(segmentPR.getDuration()) / getTimeScale();
					}

					Double segTimeLine = simpleStringToDouble(segmentPR.getStartTime());
					Double segment = segTimeLine / duration / getTimeScale();
					addSegment(segmentPR.getStartTime(), segment.intValue(), segmentPR.getDuration());
					log.info(String.format("base> %s :%s", videoName, timeScale));
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
	
	@Override
	public void adhocSegment(VideoEventData ved) {
		if (ved.getSegmentReference() != null) {
			Integer segment = "init".equals(ved.getSegmentReference()) ? 0 : (int) (simpleStringToDouble(ved.getSegmentReference()) / (getDuration()*getTimeScale()));
			addSegment(ved.getSegmentReference(), segment, String.format("%f", getDuration()));
		}
	}
	
	@Override
	public Integer getSegment(VideoEventData ved) {
		Integer segment = null;
		if (ved != null && ved.getSegmentReference() != null) {
			String ref = ved.getSegmentReference();
			segment = segmentList.get(ref);
		}
		if (segment == null && ved != null) {
			segment = ved.getSegment();
		}

		return segment == null ? -1 : segment;
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
	private AdaptationSetPR findAdaptationSet(String mimeType) {
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
	private AdaptationSetPR findAdaptationSet(MPDPlayReady mani, String mimeType, String bandwidth) {
		List<PeriodPR> period = mani.getPeriod();
		for (PeriodPR periodPr : period) {
			List<AdaptationSetPR> adaptationSet = periodPr.getAdaptationSet();
			if (adaptationSet != null) {
				for (AdaptationSetPR adaptationSetPR : adaptationSet) {
					if (adaptationSetPR.getMimeType().startsWith(mimeType)) {
						if (bandwidth != null) {
							for (RepresentationPR videoRepresentationList:adaptationSetPR.getRepresentation()){
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
		strblr.append("ManifestPlayReady");
		if (mpd != null) {
			strblr.append(", Size :"); strblr.append(mpd.getPeriod().size());
			strblr.append(", TimeScale :"); strblr.append(numberFormat.format(timeScale));
		} else {
			strblr.append("mpdPlayReadyOut == null");
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
	 * @return Segment# - from segmentList
	 */
	@Override
	public int parseSegment(String fullName, VideoEventData ved) {
		return getSegment(ved);
	}

	@Override
	public void setTimeScale(Double timeScale) {
		super.setTimeScale((timeScale == null || timeScale == 0) ? 1D : timeScale);
	}
	
}
