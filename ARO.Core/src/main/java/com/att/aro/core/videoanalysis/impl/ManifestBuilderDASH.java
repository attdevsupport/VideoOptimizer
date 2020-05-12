/*
 *  Copyright 2019 AT&T
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
package com.att.aro.core.videoanalysis.impl;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.att.aro.core.AROConfig;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.parsers.DashEncodedSegmentParser;
import com.att.aro.core.videoanalysis.parsers.DashSegmentTimelineParser;
import com.att.aro.core.videoanalysis.parsers.XmlManifestHelper;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.AdaptationSetESL;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.MPDEncodedSegment;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.RepresentationESL;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.AdaptationSetTL;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.MPDSegmentTimeline;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.PeriodST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.RepresentationST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.SegmentST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.SegmentTemplateST;
import com.att.aro.core.videoanalysis.parsers.smoothstreaming.SSM;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;

import lombok.NonNull;

public class ManifestBuilderDASH extends ManifestBuilder {

	protected static final Logger LOG = LogManager.getLogger(ManifestBuilderDASH.class.getName());
	protected static final Pattern pattern = Pattern.compile("^(#[A-Z0-9\\-]*)");

	ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	IStringParse stringParse = context.getBean(IStringParse.class);

	private MPDEncodedSegment mpdOut;
	@SuppressWarnings("unused")
	private SSM ssmOut;
	private MPDSegmentTimeline mpdSegmentTimeline;
	private boolean manifestLiveUpdate;
	private boolean isDynamic;
	private String[] encodedSegmentDurationList;
	private Double segmentTimeScale;
	private Pattern nameRegex = Pattern.compile("\\/([^\\/]*)\\/$");
	private String[] urlName;

	@NonNull
	private Double lastPresentationTimeOffset = -1D;
	
	@NonNull
	private Double lastTimeLineStart = 0D;
	private double initialStartTime;

	public ManifestBuilderDASH() {
	}

	public String buildSegmentKey(String segmentFile) {
		String key = segmentFile;
		if (manifestCollection.getCommonBaseLength() > 0) {
			key = segmentFile.substring(0, manifestCollection.getCommonBaseLength());
		} else {
			int pos = segmentFile.lastIndexOf(".");
			if (pos > -1) {
				key = segmentFile.substring(0, pos);
			}
		}
		return key;
	}

	/**
	 * Locate and return a segment number.
	 * 
	 * @param request
	 * @return segment number or -1 if not found
	 */
	public SegmentInfo getSegmentInfo(HttpRequestResponseInfo request) {
		ManifestCollection manifestCollection;
		SegmentInfo segmentInfo = null;

		String key = buildKey(request);
		manifestCollection = findManifest(request);
		if (manifestCollection != null) {
			segmentInfo = manifestCollection.getSegmentTrie().get(key);
		}
		return segmentInfo;
	}

	/**
	 * Locate and return a ChildManifest.
	 * 
	 * @param request
	 * @return segment number or -1 if not found
	 */
	public ChildManifest getChildManifest(HttpRequestResponseInfo request) {
		ManifestCollection manifestCollection = findManifest(request);
		return manifestCollection.getSegmentChildManifestTrie().get(buildKey(request));
	}

	public void parseManifestData(Manifest newManifest, byte[] data) {
		if (data == null || data.length == 0) {
			return;
		}

		XmlManifestHelper manifestView = new XmlManifestHelper(data);
		String key = newManifest.getVideoName() != null ? newManifest.getVideoName() : "null";
		newManifest.setVideoFormat(VideoFormat.MPEG4);

		String encoding = manifestView.getManifest().getClass().getSimpleName();
		manifestView.getManifestType();
		
		isDynamic = "dynamic".equals(manifestView.getManifest().getType());

		switch (encoding) {
		case "MPDSegmentTimeline":{ //  DASH SegmentTimeline
			
			newManifest.setVideoType(VideoType.DASH_SEGMENTTIMELINE);
			mpdSegmentTimeline = (MPDSegmentTimeline) manifestView.getManifest();
			if ((urlName = stringParse.parse(mpdSegmentTimeline.getBaseURL(), nameRegex)) != null) {
				newManifest.setUrlName(urlName[0]);
			}
			mpdSegmentTimeline.getBaseURL();
			List<PeriodST> periods = mpdSegmentTimeline.getPeriod();
			if (periods.size() > 1) {
				LOG.debug("period count :" + periods.size() + ", " + newManifest.getUriStr());
				return;
			}
			DashSegmentTimelineParser parseDashdynamic = new DashSegmentTimelineParser(mpdSegmentTimeline, newManifest, manifestCollection, childManifest);
			
			List<AdaptationSetTL> adaptationSetList = parseDashdynamic.getAdaptationSet();
			for (AdaptationSetTL adaptationSet : adaptationSetList) {
				ContentType contentType = manifest.matchContentType(adaptationSet.getContentType());
				if (adaptationSet.getContentType().equals("text")) {
					// skipping Closed Caption for now
					continue;
				}
				SegmentTemplateST segmentTemplate = adaptationSet.getSegmentTemplate();
				List<SegmentST> segmentList = segmentTemplate.getSegmentTimeline().getSegmentList();
				
				String initialization = segmentTemplate.getInitialization(); // segment 0 'moov'
				String media = segmentTemplate.getMedia(); // segment x 'moof'
				Double presentationTimeOffset = StringParse.stringToDouble(segmentTemplate.getPresentationTimeOffset(), 0);
				Double timescale = StringParse.stringToDouble(segmentTemplate.getTimescale(), 1);
				Double timeLineStart = StringParse.stringToDouble(segmentList.get(0).getStartTime(), 0);
			
				if (isDynamic && (lastPresentationTimeOffset == -1D || ((Double.compare(lastPresentationTimeOffset, presentationTimeOffset) != 0)))) {
					lastPresentationTimeOffset = presentationTimeOffset;
					switchManifestCollection(newManifest, key, manifest.getRequestTime());
					manifestLiveUpdate = false;
				} else if (isDynamic) {
					manifestLiveUpdate = isDynamic;
					LOG.info("update manifest");
				} else {
					switchManifestCollection(newManifest, key, manifest.getRequestTime());
					manifestLiveUpdate = false;
				}
				
				manifest.setTimeScale(timescale);
				Integer qualityID = 0;

				for (RepresentationST representation : adaptationSet.getRepresentation()) {
					int segmentID = 0;
					qualityID++;
					String rid = representation.getContentID();
					String childUriName = media.replaceAll("\\$(RepresentationID)\\$", rid).replaceAll("\\$(Time)\\$", "(\\\\d+)"); // generate REGEX for locating Byte position
					
					if (manifestLiveUpdate && ((childManifest = manifestCollection.getUriNameChildMap().get(childUriName)) != null)) {
						PatriciaTrie<SegmentInfo> segmentInfoList = childManifest.getSegmentList();
						for (String segmentKey : segmentInfoList.keySet()) {
							SegmentInfo segmentInfo = segmentInfoList.get(segmentKey);
							int sid = segmentInfo.getSegmentID();
							if (segmentID < sid) {
								segmentID = sid + 1;
							}
						}
					} else {
						// segment 1-end (moof) a regex for all
						childManifest = createChildManifest(newManifest, "", childUriName);
						childManifest.setPixelHeight(StringParse.stringToDouble(representation.getHeight(), 0).intValue());
						childManifest.setPixelWidth(StringParse.stringToDouble(representation.getWidth(), 0).intValue());

						childManifest.setBandwidth(StringParse.stringToDouble(representation.getBandwidth(), 0));
						childManifest.setQuality(qualityID);

						if (!StringUtils.isEmpty(initialization)) {
							// segment 0 (moov)
							String moovUriName = initialization.replaceAll("\\$(RepresentationID)\\$", rid);
							manifestCollection.addToUriNameChildMap(moovUriName, childManifest);
						}
						manifestCollection.addToSegmentChildManifestTrie(childManifest.getUriName(), childManifest);
					}

					String segmentUriName;
					SegmentInfo segmentInfo;
					
					Double timePos    =  0D;
					Double duration   =  0D;
					Double repetition =  0D;
							
					if (segmentList.size() > 0) {
						SegmentST segment0 = segmentList.get(0);
						
						timePos = StringParse.stringToDouble(segment0.getStartTime(), 0);
						if (childManifest.getInitialStartTime() == -1) {
							initialStartTime = timePos;
							childManifest.setInitialStartTime(initialStartTime);
						}

						// segment moov
						segmentInfo = genSegmentInfo(contentType, timescale, qualityID, segmentID, timePos, 0D);
						segmentUriName = initialization.replaceAll("\\$(.*)\\$", rid);
						LOG.debug(String.format("moov >> %d :%s", segmentID, segmentUriName));
						if (newManifest.getSegUrlMatchDef() == null) {
							masterManifest.setSegUrlMatchDef(defineUrlMatching(newManifest, segmentUriName));
						}
						addToSegmentManifestCollectionMap(segmentUriName);

						// segments moof
						segmentID = 1;
						for (SegmentST segment : segmentList) {
							duration = StringParse.stringToDouble(segment.getDuration(), 0);
							repetition = StringParse.stringToDouble(segment.getRepeat(), 0);
							for (Double countdown = repetition; countdown > -1; countdown--) {
								segmentInfo = genSegmentInfo(contentType, timescale, qualityID, segmentID, timePos - initialStartTime, duration);
								segmentUriName = media.replaceAll("\\$(RepresentationID)\\$", rid).replaceAll("\\$(Time)\\$", String.format("%.0f", timePos));
								if (newManifest.getSegUrlMatchDef() == null) {
									masterManifest.setSegUrlMatchDef(defineUrlMatching(newManifest, segmentUriName));
								}
								LOG.debug(String.format("moof >> %d :%s", segmentID, segmentUriName));
								
								segmentInfo = childManifest.addSegment(segmentUriName, segmentInfo);
								timePos += duration;
								segmentID = segmentInfo.getSegmentID() + 1;
							}
						}
					}

					addToSegmentManifestCollectionMap(childManifest.getUriName());

				}
				
				lastTimeLineStart = timeLineStart;
				if (getChildManifest() == null) {
					childManifest = createChildManifest(newManifest, "", newManifest.getUriStr());
				}
			}
		}
		break;
		
		case "MPDEncodedSegment":{ // DASH-VOD EncodedSegmentList

			switchManifestCollection(newManifest, key, manifest.getRequestTime());
			newManifest.setVideoType(VideoType.DASH_ENCODEDSEGMENTLIST);
			mpdOut = (MPDEncodedSegment) manifestView.getManifest();
			DashEncodedSegmentParser dashEncodedSegmentParser = new DashEncodedSegmentParser(mpdOut, newManifest, manifestCollection, childManifest);
			

			List<AdaptationSetESL> adaptationSetList = dashEncodedSegmentParser.getAdaptationSet();
			for (AdaptationSetESL adaptationSet : adaptationSetList) {

				ContentType contentType = manifest.matchContentType(adaptationSet.getContentType());

				if (adaptationSet.getEncodedSegmentDurations() != null) {
					segmentTimeScale = StringParse.stringToDouble(adaptationSet.getEncodedSegmentDurations().getTimescale(), 1);
					encodedSegmentDurationList = adaptationSet.getEncodedSegmentDurations().getEncodedSegmentDurationList().split(";");
				}
				SortedMap<Double, RepresentationESL> sortedRepresentationESL = sortRepresentationByBandwidth(adaptationSet.getRepresentation());
				
				Integer qualityID = 0;
				for (RepresentationESL representation : sortedRepresentationESL.values()) {
					if (representation.getEncodedSegment() != null) {
						qualityID++;

						manifest.setTimeScale(StringParse.stringToDouble(representation.getEncodedSegment().getTimescale(), 1));

						LOG.info(String.format("representation.getBandwidth() %d:%s", qualityID, representation.getBandwidth()));
						generateChildManifestFromEncodedSegmentList(newManifest, contentType, qualityID, representation);
						addToSegmentManifestCollectionMap(childManifest.getUriName());
					}
				}
			}
		}
		break;
		
		default:
			LOG.error("Encoding not yet supported :" + encoding);
			break;
		}
	}

	public SegmentInfo genSegmentInfo(ContentType contentType, Double timescale, Integer qualityID, int segmentID, Double timePos, Double duration) {
		SegmentInfo tempSegmentInfo = new SegmentInfo();
		tempSegmentInfo.setDuration(duration / timescale);
		tempSegmentInfo.setStartTime(timePos / timescale);
		tempSegmentInfo.setQuality(qualityID.toString());
		tempSegmentInfo.setVideo("video".equalsIgnoreCase(contentType.toString()));
		tempSegmentInfo.setContentType(contentType);
		tempSegmentInfo.setSegmentID(segmentID++);
		return tempSegmentInfo;
	}

	public void generateChildManifestFromEncodedSegmentList(Manifest newManifest, ContentType contentType, Integer qualityID, RepresentationESL representation) {
		childManifest = createChildManifest(newManifest, "", representation.getBaseURL());
		childManifest.setBandwidth(StringParse.stringToDouble(representation.getBandwidth(), 0));
		childManifest.setCodecs(representation.getCodecs());
		childManifest.setQuality(qualityID);
		childManifest.setPixelHeight(StringParse.stringToDouble(representation.getHeight(), 0).intValue());
		childManifest.setPixelWidth(StringParse.stringToDouble(representation.getWidth(), 0).intValue());

		Double duration = StringParse.stringToDouble(representation.getEncodedSegment().getDuration(), 0);
		String[] encodedSegments = representation.getEncodedSegment().getEncodedSegmentListValue().split(";");

		childManifest.setSegmentCount(encodedSegments.length);
		childManifest.setVideo("video".equalsIgnoreCase(contentType.toString()));
		
		double timePos = 0;
		for (int idx = 0; idx < encodedSegments.length; idx++) {
			String encodedSegmentElement = encodedSegments[idx];
			if (encodedSegmentElement == null) {
				break;
			}
			duration = (idx == 0) ? 0 : calcDuration(encodedSegmentDurationList[idx - 1], duration, segmentTimeScale);

			SegmentInfo segmentInfo = new SegmentInfo();
			segmentInfo.setDuration(duration);
			segmentInfo.setStartTime(timePos);
			segmentInfo.setSegmentID(idx);
			segmentInfo.setContentType(contentType);
			segmentInfo.setVideo("video".equalsIgnoreCase(contentType.toString()));
			segmentInfo.setSize(calcSizeFromEncodedSegmentElement(encodedSegmentElement));
			segmentInfo.setQuality(qualityID.toString());
			timePos += duration;

			childManifest.addSegment(encodedSegmentElement, segmentInfo);

		}
		if (newManifest.getSegUrlMatchDef() == null) {
			masterManifest.setSegUrlMatchDef(defineUrlMatching(newManifest, childManifest.getUriName()));
		}
		manifestCollection.addToSegmentChildManifestTrie(childManifest.getUriName(), childManifest);
	}

	public double calcDuration(String hex, Double duration, Double timescale) {
		if (hex != null) {
			duration = (double) Integer.parseInt(hex, 16);
		}
		return duration / timescale;
	}

	/**
	 * <pre>
	 * Calculate size from EncodedSegmentList element
	 * 
	 * @param segment
	 * @return int size
	 */
	public int calcSizeFromEncodedSegmentElement(String encodedSegmentElement) {
		try {
			Long beginByte = Long.valueOf(encodedSegmentElement.substring(0, 16), 16);
			Long endByte = Long.valueOf(encodedSegmentElement.substring(17, 33), 16);
			int size = (int) (endByte - beginByte);
			return size;
		} catch (NumberFormatException e) {
			LOG.error("failed to convert Hex to Long :", e);
			return 0;
		}
	}

	/**<pre>
	 * Do not assume that bandwidth will always be in order
	 * 	Make sure it is in sorted order by Bandwidth so that qualityID can be assigned
	 * 	example: 100000, 150000, 200000, 300000, 500000, 800000, 1200000, 1800000
	 */
	public SortedMap<Double, RepresentationESL> sortRepresentationByBandwidth(List<RepresentationESL> representationList) {
		SortedMap<Double, RepresentationESL> sortedRepresentationAmz = new TreeMap<>();
		for (RepresentationESL representation : representationList) {
			Double bandwidth = StringParse.stringToDouble(representation.getBandwidth(), 1);
			sortedRepresentationAmz.put(bandwidth, representation);
		}
		return sortedRepresentationAmz;
	}

	@Override
	protected ChildManifest createChildManifest(Manifest manifest, String parameters, String childUriName) {
		ChildManifest childManifest = new ChildManifest();
		childManifest.setManifest(manifest);
		childManifest.setUriName(childUriName);

		childUriName = childUriName.replaceAll("%2f", "/");
		manifestCollection.addToUriNameChildMap(childUriName, childManifest);
		return childManifest;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\n\tManifestBuilderDASH :");
		strblr.append(super.toString());
		return strblr.toString();
	}

	@Override
	public String buildSegmentName(HttpRequestResponseInfo request, String extension) {
		String name = request.getObjNameWithoutParams();
		int dot = name.lastIndexOf('.');
		if (dot > -1) {
			int sep = name.substring(0, dot).lastIndexOf('/');
			if (sep > -1) {
				name = name.substring(sep + 1);
				dot = name.lastIndexOf('.');
			}
		}
		name = StringUtils.replace(name, "/", "-");
		if (dot == -1) {
			dot = name.length();
		}
		name = StringUtils.replace(name.substring(0, dot), ".", "-");
		return name + extension;
	}

}
