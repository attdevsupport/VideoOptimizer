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
package com.att.aro.core.videoanalysis.impl;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;

import lombok.Getter;

public class VideoChunkPlotterImpl extends PlotHelperAbstract {

	private int key = 0;
	@Getter private Map<Integer, Double> seriesDataSets = new TreeMap<Integer, Double>();
	@Getter private List<BufferedImage> imgSeries;
	@Getter private double firstChunkTimestamp;

	@Getter private List<Double> chunkPlayStartTimesList = new ArrayList<>();
	
	/**
	 * key : segmentID
	 * value: segment play startTime
	 */
	@Getter private Map<Long, Double> segmentStartTimeMap = new TreeMap<>();

	public Map<Integer, Double> populateDataSet(StreamingVideoData streamingVideoData) {
		if (streamingVideoData != null) {
			this.streamingVideoData = streamingVideoData;
			key = 0;
			imgSeries = new ArrayList<BufferedImage>();
			seriesDataSets.clear();
			filterVideoSegment(streamingVideoData);
			filterVideoSegmentUpdated(streamingVideoData);

			if (!streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList().isEmpty()) {
				videoEventListBySegment(streamingVideoData);
				updateChunkPlayStartTimes();
			}

			getChunkCollectionDataSet();

		}
		return seriesDataSets;
	}

	private void getChunkCollectionDataSet() {
		int count = 0;
		List<VideoEvent> allSegments2 = streamingVideoData.getStreamingVideoCompiled().getAllSegments();
		if (allSegments2 == null) {
			return;
		}
		for (VideoEvent ve : allSegments2) {

			BufferedImage img = ve.getThumbnail();

			if (count == 0) { // first chunk
				firstChunkTimestamp = ve.getDLTimeStamp();
				count++;
			}
			int checkCount = 0;
			
			while (img.getHeight() > 20 && checkCount++ < 3) {
				// background processing inserted a thumbnail, too soon for the image sizing to have completed, a very rare occurance
				// wait 500 ms, sufficient time for processing to complete
				// this prevents a "thumbnail" of 2k pixels wide filling up the display
				Util.sleep(500);
				img = ve.getThumbnail();
			}
			imgSeries.add(img);

			seriesDataSets.put(key, ve.getDLTimeStamp());
			key++;
		}
	}

	public void updateChunkPlayStartTimes() {
		this.chunkPlayStartTimesList.clear();
		this.segmentStartTimeMap.clear();
		boolean filterAgain = false;

		List<VideoEvent> chunksBySegmentID = streamingVideoData.getStreamingVideoCompiled().getChunksBySegmentID();

		Map<Double, VideoEvent> playStartTimeBySegment = new TreeMap<>();

		for (int index = 0; index < chunksBySegmentID.size(); index++) {
			double playtime = (chunksBySegmentID.get(index)).getPlayTime();

			if (chunksBySegmentID.get(index).getEndTS() > playtime) {
				boolean shuffled = alterFilteredSegmentList(chunksBySegmentID.get(index), playtime);
				if (shuffled) {
					filterAgain = true;
				}
			}
			playStartTimeBySegment.put(playtime, chunksBySegmentID.get(index));
		}

		streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().forEach(x -> {
			chunkPlayStartTimesList.add((Double) x.getPlayTime());
			segmentStartTimeMap.put(((Double) (x.getSegmentID())).longValue(), (Double) x.getPlayTime());
		});

		/*
		 * removes all matching videoEvents from .getChunksBySegmentID() when videoEvent is found in filteredSegments
		 */
		// VID-TODO looks inefficient, lots of re-iterating through chunksBySegmentID
		if (filterAgain) {
			List<VideoEvent> filteredSegments = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments();
			Collections.sort(filteredSegments, new VideoEventComparator(SortSelection.END_TS));
			for (VideoEvent ve : filteredSegments) {
				if (!chunksBySegmentID.contains(ve)) {
					for (VideoEvent segment : chunksBySegmentID) {
						if (segment.getSegmentID() == ve.getSegmentID()) {
							// segment should be replaced by ve
							chunksBySegmentID.add(chunksBySegmentID.indexOf(segment), ve);
							chunksBySegmentID.remove(segment);
							break;
						}
					}
				}
			}
		}

	}

	public boolean alterFilteredSegmentList(VideoEvent ve, double segmentPlayTime) {
		List<VideoEvent> deleteChunks = streamingVideoData.getStreamingVideoCompiled().getDeleteChunkList();
		if (!deleteChunks.isEmpty()) {
			// descending order sorting by download end time.
			Collections.sort(deleteChunks, new VideoEventComparator(SortSelection.END_TS_DESCENDING));
			boolean swapedTheMinimum = false;
			int minIndex = -1;
			for (VideoEvent removeChunk : deleteChunks) {
				if (removeChunk.getSegmentID() == ve.getSegmentID() && removeChunk.getEndTS() <= segmentPlayTime) {
					// This is the correct quality level of this segment played
					// swap
					int index = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().indexOf(ve);
					streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().remove(ve);
					streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().add(index, removeChunk);
					deleteChunks.add(ve);
					deleteChunks.remove(removeChunk);
					return true;
				} else if (ve.getEndTS() > removeChunk.getEndTS() && removeChunk.getSegmentID() == ve.getSegmentID()) {
					// swap the closest
					minIndex = deleteChunks.indexOf(removeChunk);
					swapedTheMinimum = true;
				}
			}
			if (swapedTheMinimum && minIndex != -1) {
				int index = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().indexOf(ve);
				streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().remove(ve);
				streamingVideoData.getStreamingVideoCompiled().getFilteredSegments().add(index, deleteChunks.get(minIndex));
				deleteChunks.add(ve);
				deleteChunks.remove(deleteChunks.get(minIndex));
				return true;
			}
		}
		return false;
	}

	public double getSegmentPlayStartTime(VideoEvent currentChunk) {
		Double temp = segmentStartTimeMap.get((new Double(currentChunk.getSegmentID())).longValue());
		return temp != null ? temp : -1;
	}

	public Map<VideoEvent, Double> getChunkPlayTimeList() {
		return streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList();
	}
}