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
package com.att.aro.core.videoanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.videoanalysis.impl.SortSelection;
import com.att.aro.core.videoanalysis.impl.VideoEventComparator;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs.DUPLICATE_HANDLING;

import lombok.Data;

@Data
public abstract class PlotHelperAbstract {

	private static final Logger LOGGER = LogManager.getLogger(PlotHelperAbstract.class.getName());
	protected StreamingVideoData streamingVideoData;

	private List<VideoEvent> chunkDownload;
	protected Map<VideoEvent, Double> chunkPlayTimeList = new TreeMap<>();

	@Autowired
	private IVideoUsagePrefsManager videoPrefManager;

	public List<VideoEvent> filterVideoSegment(StreamingVideoData videoData) {
		this.streamingVideoData = videoData;
		Map<VideoEvent, VideoStream> veManifestList = new HashMap<>();
		chunkDownload = new ArrayList<>();
		List<VideoEvent> duplicateChunks = new ArrayList<>();
		List<VideoEvent> allSegments = new ArrayList<>();
		
		if (videoData != null && !CollectionUtils.isEmpty(videoData.getVideoStreamMap())) {
			for (VideoStream videoStream : videoData.getVideoStreamMap().values()) {
				// don't count if no videos with manifest, or only one video
				if (videoStream.isSelected() && !videoStream.getVideoEventMap().isEmpty()) {
					TreeMap<String, VideoEvent> segmentEventList = (TreeMap<String, VideoEvent>) videoStream.getVideoSegmentEventList();
					Entry<String, VideoEvent> segmentValue = segmentEventList.higherEntry("00000000:z");
					double firstSeg = segmentValue != null ? segmentValue.getValue().getSegmentID() : 0;
					
					VideoEvent first = null;
					for (VideoEvent videoEvent : videoStream.getVideoEventMap().values()) {
						if (videoEvent.getSegmentID() == firstSeg) {
							first = videoEvent;
						}
						if (videoEvent.isNormalSegment() && (!chunkDownload.contains(videoEvent))) {

							for (VideoEvent video : chunkDownload) {
								if ((videoEvent.getSegmentID() != firstSeg) && video.getSegmentID() == videoEvent.getSegmentID()) {
									duplicateChunks.add(video);
								}

								if (videoEvent.getSegmentID() == firstSeg) {
									if (!videoEvent.equals(first)) {
										duplicateChunks.add(videoEvent);
									}
								}
							}
							veManifestList.put(videoEvent, videoStream);
							chunkDownload.add(videoEvent);
							allSegments.add(videoEvent);
						}
					}
				}
			}
		}

		for (VideoEvent ve : duplicateChunks) {
			veManifestList.keySet().remove(ve);
			chunkDownload.remove(ve);
		}

		return chunkDownload;
	}

	public List<VideoEvent> filterSegmentByVideoPref(StreamingVideoData videoData) {
		this.streamingVideoData = videoData;
		Map<VideoEvent, VideoStream> veManifestList = new HashMap<>();
		chunkDownload = new ArrayList<>();
		List<VideoEvent> allSegments = new ArrayList<>();
		streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList().clear();
		DUPLICATE_HANDLING segmentFilterChoice = videoPrefManager.getVideoUsagePreference().getDuplicateHandling();

		for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()) {
			// don't count if no videos with manifest, or only one video
			if (videoStream != null && videoStream.isSelected() && !videoStream.getVideoEventMap().isEmpty()) {
				for (VideoEvent videoEvent : videoStream.getVideoEventMap().values()) {
					if (!(videoEvent.getSegmentID() == 0 && videoStream.getManifest().getVideoFormat().equals(VideoFormat.MPEG4)) && (!chunkDownload.contains(videoEvent))) {

						switch (segmentFilterChoice) {
						case FIRST:
							filterByFirst(chunkDownload, videoEvent);
							break;
						case LAST:
							filterByLast(chunkDownload, videoEvent);
							break;
						case HIGHEST:
							filterByHighest(chunkDownload, videoEvent);
							break;
						default:
						}

						veManifestList.put(videoEvent, videoStream);
						chunkDownload.add(videoEvent);
						allSegments.add(videoEvent);
					}
				}
			}
		}

		if (segmentFilterChoice == DUPLICATE_HANDLING.FIRST || segmentFilterChoice == DUPLICATE_HANDLING.LAST) {
			for (VideoEvent ve : streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList()) {
				veManifestList.keySet().remove(ve);
				chunkDownload.remove(ve);
			}
		}
		streamingVideoData.getStreamingVideoCompiled().setAllSegments(allSegments);

		return chunkDownload;
	}

	private void filterByFirst(List<VideoEvent> chunkDownloadList, VideoEvent videoEvent) {
		for (VideoEvent video : chunkDownloadList) {
			if (video.getSegmentID() == videoEvent.getSegmentID()) {
				streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList().add(videoEvent); // Adding the segments that came in LAST to the remove list
			}
		}
	}

	private void filterByLast(List<VideoEvent> chunkDownloadList, VideoEvent videoEvent) {
		for (VideoEvent video : chunkDownloadList) {
			if (video.getSegmentID() == videoEvent.getSegmentID()) {
				streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList().add(video); // Adding the segments that came in FIRST to the remove list
			}
		}
	}

	private void filterByHighest(List<VideoEvent> chunkDownloadList, VideoEvent videoEvent) {
		for (VideoEvent video : chunkDownloadList) {
			if (video.getSegmentID() == videoEvent.getSegmentID()) {
				try {
					Integer videoQuality = video.getQuality().isEmpty() || video.getQuality() == null || video.getQuality().matches(".*[A-Za-z].*") ? 0
							: Integer.parseInt(video.getQuality());
					Integer videoEventQuality = videoEvent.getQuality().isEmpty() || videoEvent.getQuality() == null || videoEvent.getQuality().matches(".*[A-Za-z].*") ? 0
							: Integer.parseInt(videoEvent.getQuality());
					if (videoQuality.compareTo(videoEventQuality) < 0) {
						streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList().add(video);

					} else {
						streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList().add(videoEvent);
					}
				} catch (NumberFormatException e) {
					StackTraceElement[] stack = e.getStackTrace();
					LOGGER.error("NumberFormatException : " + e + " @ " + ((stack != null && stack.length > 0) ? stack[0] : ""));
				} catch (Exception e) {
					StackTraceElement[] stack = e.getStackTrace();
					LOGGER.error("Exception : " + e + " @ " + ((stack != null && stack.length > 0) ? stack[0] : ""));
				}
			}
		}
	}

	public List<VideoEvent> filterVideoSegmentUpdated(StreamingVideoData streamingVideoData) {
		streamingVideoData.getStreamingVideoCompiled().setFilteredSegments(filterSegmentByVideoPref(streamingVideoData));
		return chunkDownload;
	}

	public List<VideoEvent> videoEventListBySegment(StreamingVideoData streamingVideoData) {
		this.streamingVideoData = streamingVideoData;
		List<VideoEvent> chunksBySegmentID = streamingVideoData.getStreamingVideoCompiled().getChunksBySegmentID(); // new ArrayList<>();
		chunksBySegmentID.clear();
		for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()) {
			if (videoStream.isSelected() && !videoStream.getVideoEventsBySegment().isEmpty()) {
				for (VideoEvent videoEvent : videoStream.getVideoEventsBySegment()) {
					if (videoEvent.isNormalSegment()) {
						chunksBySegmentID.add(videoEvent);
					}
				}
			}
		}
		for (VideoEvent ve : streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList()) {
			chunksBySegmentID.remove(ve);
		}

		Collections.sort(chunksBySegmentID, new VideoEventComparator(SortSelection.SEGMENT_ID));
		return chunksBySegmentID;
	}
}
