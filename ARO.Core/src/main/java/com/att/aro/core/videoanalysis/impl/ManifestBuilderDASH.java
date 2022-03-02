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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.parsers.DashEncodedSegmentParser;
import com.att.aro.core.videoanalysis.parsers.DashIFParser;
import com.att.aro.core.videoanalysis.parsers.DashSegmentTimelineParser;
import com.att.aro.core.videoanalysis.parsers.XmlManifestHelper;
import com.att.aro.core.videoanalysis.parsers.dashif.AdaptationSet;
import com.att.aro.core.videoanalysis.parsers.dashif.MPD;
import com.att.aro.core.videoanalysis.parsers.dashif.Representation;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.AdaptationSetESL;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.MPDEncodedSegment;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.RepresentationESL;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.AdaptationSetTL;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.MPDSegmentTimeline;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.PeriodST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.RepresentationST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.SegmentST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.SegmentTemplateST;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.SegmentTimeLineST;
import com.att.aro.core.videoanalysis.pojo.ChildManifest;
import com.att.aro.core.videoanalysis.pojo.Manifest;
import com.att.aro.core.videoanalysis.pojo.Manifest.ContentType;
import com.att.aro.core.videoanalysis.pojo.ManifestCollection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;

import lombok.NonNull;

public class ManifestBuilderDASH extends ManifestBuilder {

	protected static final Logger LOG = LogManager.getLogger(ManifestBuilderDASH.class.getName());

	ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	private IStringParse stringParse = context.getBean(IStringParse.class);

	
	@NonNull
	private Double lastPresentationTimeOffset = -1D;

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
		
		boolean isDynamic = "dynamic".equals(manifestView.getManifest().getType());

		switch (encoding) {
    		case "MPDSegmentTimeline":
    			processSegmentTimeline(newManifest, manifestView, key, isDynamic);
    			break;
    		case "MPDEncodedSegment":
    			processEncodedSegment(newManifest, manifestView, key);
    			break;
    		case "MPD":
    			processDashIF(newManifest, manifestView, key);
                break;
    		default:
    			LOG.error("Encoding not yet supported :" + encoding);
		}
	}

	public void processDashIF(Manifest newManifest, XmlManifestHelper manifestView, String key) {
		// DASH-IF parser implementation
		switchManifestCollection(newManifest, key, manifest.getRequestTime());
		newManifest.setVideoType(VideoType.DASH_IF);
		MPD mpd = (MPD) manifestView.getManifest();
		DashIFParser dashIFParser = new DashIFParser(VideoType.DASH, mpd, newManifest, manifestCollection, childManifest);
		List<AdaptationSet> adaptationList = dashIFParser.getAdaptationSet();

		if (adaptationList != null) {
		    int audioQualityId = 0;
		    int videoQualityId = 0;
		    for (AdaptationSet adaptation : adaptationList) {
		        ContentType contentType = manifest.matchContentType(adaptation.getContentType());

		        // Sort the representations by bandwidth to assign incremental quality id
		        List<Representation> sortedRepresentations = adaptation.getRepresentations().stream().sorted(new Comparator<Representation>() {
		                                                         @Override
		                                                         public int compare(Representation o1, Representation o2) {
		                                                             if (o1 == null || o2 == null) {
		                                                                 return 0;
		                                                             }
		                                                             return o1.getBandwidth().compareTo(o2.getBandwidth());
		                                                         }
		                                                     }).collect(Collectors.toList());
		        for (Representation representation : sortedRepresentations) {
		            if (representation.getSegmentBase() != null) {
		                if (representation.getSegmentBase().getTimescale() != null) {
		                    // TODO: Setting timescale value for manifest is overwritten every time a child manifest is created for individual representation. \
		                    // Timescale should be set on the Child Manifest level. Revisit this some time later.
		                    manifest.setTimeScale(representation.getSegmentBase().getTimescale().doubleValue());
		                }

		                // Generate child manifests for each individual representation
		                childManifest = createChildManifest(newManifest, "", representation.getBaseURL());
		                if (ContentType.AUDIO.equals(contentType)) {
		                    childManifest.setQuality(++audioQualityId);
							if (adaptation.getContentType().equals("audio")) {
								if (representation.getAudioChannelConfiguration() != null) {
									childManifest.setChannels(representation.getAudioChannelConfiguration().getValue());
								} else if (adaptation.getAudioChannelConfiguration() != null) {
									childManifest.setChannels(adaptation.getAudioChannelConfiguration().getValue());
								}
							}
		                } else if (ContentType.VIDEO.equals(contentType)) {
		                    childManifest.setQuality(++videoQualityId);
		                }
		                childManifest.setBandwidth(representation.getBandwidth());
		                childManifest.setCodecs(representation.getCodecs() != null ? representation.getCodecs() : adaptation.getCodecs());
		                childManifest.setVideo(ContentType.VIDEO.equals(contentType));
		                childManifest.setContentType(contentType);
		                if (representation.getHeight() != null) {
		                    childManifest.setPixelHeight(representation.getHeight());
		                }
		                if (representation.getWidth() != null) {
		                    childManifest.setPixelWidth(representation.getWidth());
		                }
		                if (representation.getAudioChannelConfiguration() != null) {
		                    childManifest.setChannels(representation.getAudioChannelConfiguration().getValue());
		                }

		                // Set first segment info to the child manifest
		                SegmentInfo segmentInfo = new SegmentInfo();
		                segmentInfo.setDuration(0);
		                segmentInfo.setStartTime(0);
		                segmentInfo.setSegmentID(0);
		                segmentInfo.setContentType(contentType);
		                segmentInfo.setVideo(childManifest.isVideo());
		                segmentInfo.setQuality(String.valueOf(childManifest.getQuality()));
		                // Add chunk size for the first initializing segment
		                int chunkSize = 0;
		                if (representation.getSegmentBase().getIndexRange() != null) {
		                    chunkSize += calcSizeFromSegmentElement(representation.getSegmentBase().getIndexRange(), false);
		                }
		                if (representation.getSegmentBase().getInitialization() != null) {
		                    chunkSize += calcSizeFromSegmentElement(representation.getSegmentBase().getInitialization().getRange(), false);
		                }
		                segmentInfo.setSize(chunkSize);
		                // Add segment to child manifest segment list
		                childManifest.addSegment("0-" + String.valueOf(chunkSize != 0 ? chunkSize-1 : 0), segmentInfo);

		                masterManifest.getSegUrlMatchDef().add(defineUrlMatching(childManifest.getUriName()));
		                manifestCollection.addToSegmentChildManifestTrie(childManifest.getUriName(), childManifest);
		                addToSegmentManifestCollectionMap(childManifest.getUriName());
		            } else if (representation.getSegmentList() != null || representation.getSegmentTemplate() != null) {
		                // TODO: Implement Segment List or Template parser
		                LOG.warn("MPD: Represenation segment base is null but segment list or template is not null!");
		            }
		        }
		    }
		}
	}

	public void processEncodedSegment(Manifest newManifest, XmlManifestHelper manifestView, String key) {
		// DASH-VOD EncodedSegmentList
		switchManifestCollection(newManifest, key, manifest.getRequestTime());
		newManifest.setVideoType(VideoType.DASH_ENCODEDSEGMENTLIST);
		MPDEncodedSegment mpdEncodedSegment = (MPDEncodedSegment) manifestView.getManifest();
		DashEncodedSegmentParser dashEncodedSegmentParser = new DashEncodedSegmentParser(mpdEncodedSegment, newManifest, manifestCollection, childManifest);
		
		List<AdaptationSetESL> adaptationSetList = dashEncodedSegmentParser.getAdaptationSet();
		String[] encodedSegmentDurationList = null;
		Double segmentTimeScale = null;
		
		/* 
		 * Sort Adaptations by minBandwidth, for the purpose of assigning track numbers.
		 * Video tracks will start with track 0
		 * Audio tracks will start with track 0, also
		 */
		SortedMap<Double, AdaptationSetESL> sortedAdaptationSet;
		Map<String, SortedMap<Double, AdaptationSetESL>> sortedAdaptationSets = new HashMap<>();
		Map<String, Integer> trackMap = new HashMap<>();
		Integer track;
		
		Double bandWidth;
		for (AdaptationSetESL adaptation : adaptationSetList) {
			List<RepresentationESL> representation = adaptation.getRepresentation();
			if (sortedAdaptationSets.containsKey(adaptation.getContentType())) {
				sortedAdaptationSet = sortedAdaptationSets.get(adaptation.getContentType());
			} else {
				sortedAdaptationSet = new TreeMap<>();
				sortedAdaptationSets.put(adaptation.getContentType(), sortedAdaptationSet);
				trackMap.put(createKey(adaptation), 0);
			}
			
			if ((bandWidth = adaptation.getMinBandwidth()) == null || bandWidth == 0) {
				bandWidth = Double.MAX_VALUE;
				for (RepresentationESL repSet : representation) {
					if (repSet.getBandwidth() == null) {
						bandWidth = 1.0;
						break;
					}
					if (repSet.getBandwidth() < bandWidth) {
						bandWidth = repSet.getBandwidth();
					}
				}
			}
			sortedAdaptationSet.put(bandWidth, adaptation);
		}
		
		for (SortedMap<Double, AdaptationSetESL> sortedMap : sortedAdaptationSets.values()) {
			for (AdaptationSetESL adaptation : sortedMap.values()) {

				ContentType contentType = manifest.matchContentType(adaptation.getContentType());

				if (adaptation.getEncodedSegmentDurations() != null && adaptation.getEncodedSegmentDurations().getEncodedSegmentDurationList() != null) {
					segmentTimeScale = StringParse.stringToDouble(adaptation.getEncodedSegmentDurations().getTimescale(), 1);
					encodedSegmentDurationList = adaptation.getEncodedSegmentDurations().getEncodedSegmentDurationList().split(";");
				}

				track = trackMap.get(createKey(adaptation));
				
				SortedMap<Double, RepresentationESL> sortedRepresentationESL = sortRepresentationByBandwidth(adaptation.getRepresentation());
				
				for (RepresentationESL representation : sortedRepresentationESL.values()) {
					if (representation.getEncodedSegment() != null) {
						track++;

						manifest.setTimeScale(StringParse.stringToDouble(representation.getEncodedSegment().getTimescale(), 1));

						LOG.debug(String.format("representation.getBandwidth() %d:%s", track, representation.getBandwidth()));
						generateChildManifestFromEncodedSegmentList(newManifest, contentType, track, representation, encodedSegmentDurationList, segmentTimeScale);

						if (representation.getRepresentationACC() != null) { // audio
							childManifest.setChannels(representation.getRepresentationACC().getValue());
						}

						addToSegmentManifestCollectionMap(childManifest.getUriName());
					}
				}
				trackMap.put(createKey(adaptation), track);
			}
		}
	}

	public String createKey(AdaptationSetESL adaptation) {
		return String.format("%s:%s", adaptation.getContentType(), adaptation.getAudioTrackId());
	}
	
	/**<pre>
	 * Do not assume that bandwidth will always be in order
	 * 	Make sure it is in sorted order by Bandwidth so that qualityID can be assigned
	 * 	example: 100000, 150000, 200000, 300000, 500000, 800000, 1200000, 1800000
	 */
	public SortedMap<Double, RepresentationESL> sortRepresentationByBandwidth(List<RepresentationESL> representationList) {
		SortedMap<Double, RepresentationESL> sortedRepresentationAmz = new TreeMap<>();
		for (RepresentationESL representation : representationList) {
			Double bandwidth = representation.getBandwidth().doubleValue();
			sortedRepresentationAmz.put(bandwidth, representation);
		}
		return sortedRepresentationAmz;
	}

	public void processSegmentTimeline(Manifest newManifest, XmlManifestHelper manifestView, String key, boolean isDynamic) {
		//  DASH SegmentTimeline
		newManifest.setVideoType(VideoType.DASH_SEGMENTTIMELINE);
		MPDSegmentTimeline mpdSegmentTimeline = (MPDSegmentTimeline) manifestView.getManifest();
		long mediaPresentationDuration = isoConvertDurationTime(mpdSegmentTimeline.getMediaPresentationDuration());
		
		Pattern nameRegex = Pattern.compile("\\/([^\\/]*)\\/$");
		String[] urlName;
		if ((urlName = stringParse.parse(mpdSegmentTimeline.getBaseURL(), nameRegex)) != null) {
			newManifest.setUrlName(urlName[0]);
		}

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
			SegmentTimeLineST segmentTimelineST = segmentTemplate.getSegmentTimeline();
			List<SegmentST> segmentList;
			if (segmentTimelineST != null && segmentTimelineST.getSegmentList() != null) {
				segmentList = segmentTimelineST.getSegmentList();
			} else {
				segmentList = new ArrayList<>();
			}
			String initialization = segmentTemplate.getInitialization(); // segment 0 'moov'
			String media = segmentTemplate.getMedia(); // segment x 'moof'
			Double presentationTimeOffset = StringParse.stringToDouble(segmentTemplate.getPresentationTimeOffset(), 0);
			Double timescale = StringParse.stringToDouble(segmentTemplate.getTimescale(), 1);
			
			boolean manifestLiveUpdate;
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
					PatriciaTrie<SegmentInfo> segmentInfoList = childManifest.getSegmentInfoTrie();
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
					segmentInfo = genSegmentInfo(contentType, timescale, qualityID, segmentID, timePos - initialStartTime, 0D);
					segmentUriName = initialization.replaceAll("\\$(.*)\\$", rid);
					LOG.debug(String.format("moov >> %d :%s", segmentID, segmentUriName));

					masterManifest.getSegUrlMatchDef().add(defineUrlMatching(segmentUriName));
					segmentInfo = childManifest.addSegment(segmentUriName, segmentInfo);
					addToSegmentManifestCollectionMap(segmentUriName);

					// segments moof
					segmentID = 1;
					for (SegmentST segment : segmentList) {
						duration = StringParse.stringToDouble(segment.getDuration(), 0);
						repetition = StringParse.stringToDouble(segment.getRepeat(), 0);
						for (Double countdown = repetition; countdown > -1; countdown--) {
							segmentInfo = genSegmentInfo(contentType, timescale, qualityID, segmentID, timePos - initialStartTime, duration);
							segmentUriName = media.replaceAll("\\$(RepresentationID)\\$", rid).replaceAll("\\$(Time)\\$", String.format("%.0f", timePos));
						    masterManifest.getSegUrlMatchDef().add(defineUrlMatching(segmentUriName));
						    addToSegmentManifestCollectionMap(segmentUriName);
						    LOG.debug(String.format("moof >> %d :%s", segmentID, segmentUriName));
							
							segmentInfo = childManifest.addSegment(segmentUriName, segmentInfo);
							timePos += duration;
							segmentID = segmentInfo.getSegmentID() + 1;
						}
					}
				}
   
				addToSegmentManifestCollectionMap(childManifest.getUriName());
   
			}

			if (getChildManifest() == null) {
				childManifest = createChildManifest(newManifest, "", newManifest.getUriStr());
			}
			if (adaptationSet.getContentType().equals("audio")) {
				if (adaptationSet.getAudioChannelConfiguration() != null) {
					childManifest.setChannels(adaptationSet.getAudioChannelConfiguration().getValue());
				}
			}
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

	public void generateChildManifestFromEncodedSegmentList(Manifest newManifest
															, ContentType contentType
															, Integer qualityID
															, RepresentationESL representation
															, String[] encodedSegmentDurationList
															, Double segmentTimeScale) {
		
		childManifest = createChildManifest(newManifest, "", representation.getBaseURL());
		childManifest.setBandwidth(representation.getBandwidth());
		childManifest.setCodecs(representation.getCodecs());
		childManifest.setQuality(qualityID);
		childManifest.setPixelHeight(StringParse.stringToDouble(representation.getHeight(), 0).intValue());
		childManifest.setPixelWidth(StringParse.stringToDouble(representation.getWidth(), 0).intValue());
		if (representation.getRepresentationACC() != null) {
			LOG.debug("Parsing audio channels values: " + representation.getRepresentationACC().getValue());
			childManifest.setChannels(representation.getRepresentationACC().getValue());
		}
		Double duration = StringParse.stringToDouble(representation.getEncodedSegment().getDuration(), 0);
		String[] encodedSegments = representation.getEncodedSegment().getEncodedSegmentListValue().split(";");

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
			segmentInfo.setSize(calcSizeFromSegmentElement(encodedSegmentElement, true));
			segmentInfo.setQuality(qualityID.toString());
			timePos += duration;

			childManifest.addSegment(encodedSegmentElement, segmentInfo);

		}

		masterManifest.getSegUrlMatchDef().add(defineUrlMatching(childManifest.getUriName()));
		manifestCollection.addToSegmentChildManifestTrie(childManifest.getUriName(), childManifest);
	}

	private double calcDuration(String hex, Double duration, Double timescale) {
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
	private int calcSizeFromSegmentElement(String segmentElement, boolean encoded) {
	    int size = 0;
		try {
		    if (encoded) {
    			Long beginByte = Long.valueOf(segmentElement.substring(0, 16), 16);
    			Long endByte = Long.valueOf(segmentElement.substring(17, 33), 16);
    			size = (int) (endByte - beginByte);
		    } else {
		        String[] rangeValues = segmentElement.split("-");
                size += Integer.valueOf(rangeValues[1]) - Integer.valueOf(rangeValues[0]) + 1;
		    }
		} catch (NumberFormatException e) {
			LOG.error("failed to convert Hex to Long :", e);
		}

		return size;
	}

	@Override
	protected ChildManifest createChildManifest(Manifest manifest, String parameters, String childUriName) {
		childUriName = Util.decodeUrlEncoding(childUriName);
		ChildManifest childManifest = new ChildManifest();
		childManifest.setManifest(manifest);
		childManifest.setUriName(childUriName);
		childManifest.setManifestCollectionParent(manifestCollection);
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

	/**
	 * Convert a simple Duration time of seconds
	 * example: PT1800.000000S
	 * 
	 * P Duration
	 * T Time
	 * S seconds
	 * 
	 * @param mediaPresentationDuration
	 * @return time ins seconds, or 0 if invalid format
	 */
	public long isoConvertDurationTime(String mediaPresentationDuration) {
		long duration = 0L;
		if (!StringUtils.isEmpty(mediaPresentationDuration)) {
			duration  = (Duration.parse(mediaPresentationDuration)).getSeconds();
		}
		return duration;
	}

}