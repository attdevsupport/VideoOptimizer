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
package com.att.aro.ui.view.videotab.plot;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.diagnostictab.plot.IPlot;

import lombok.Data;

@Data
public class VideoProgressPlot implements IPlot{
	private static final Logger LOGGER = LogManager.getLogger(VideoProgressPlot.class);
	private VideoStream videoStream;
	private int graphCounter = 0;

	private List<VideoEvent> videoEventList = new ArrayList<VideoEvent>();
	private List<VideoEvent> eventList = new ArrayList<VideoEvent>();
	private List<VideoEvent> audioEventList = new ArrayList<VideoEvent>();
	SortedMap<String, VideoEvent> videoEventMap = new TreeMap<String, VideoEvent>();
	SortedMap<VideoEvent, Double> downloadProgressMap = new TreeMap<VideoEvent, Double>();
	private XYSeries videoDownloadSeries = new XYSeries(SegmentOptions.VIDEO.toString());
	private final String EMPTY_STRING = "";
	private final String BLANK_STRING = " ";
	private XYSeries downloadMockSeries = new XYSeries(EMPTY_STRING);
	private XYSeries progressMockSeries = new XYSeries(BLANK_STRING);
	private double maxYValue;
	private double maxXValue;
	private double minYValue;
	private XYSeries audioDownloadSeries = new XYSeries(SegmentOptions.AUDIO.toString());
	private XYSeries videoPlaytimeSeries = new XYSeries("Video PlayTime");
	private XYSeries audioPlaytimeSeries = new XYSeries("Audio PlayTime");
	private SegmentOptions optionSelected;
	private double minXValue;
	private boolean isMuxed = false;
	private List<Double> progressList = new ArrayList<Double>();
	private List<Double> timestampList = new ArrayList<Double>();

	public VideoProgressPlot(VideoStream videoStream, SegmentOptions optionSelected) {
		this.videoStream = videoStream;
		this.optionSelected = optionSelected;
	}

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if (analysis == null) {
			LOGGER.info("no trace data here");
		} else {
			XYItemRenderer videoRenderer = plot.getRenderer();
			videoRenderer.setBaseToolTipGenerator(new XYToolTipGenerator() {

				@Override
				public String generateToolTip(XYDataset dataset, int series, int item) {
					if (dataset.getSeriesCount() > 1) {
						if (series == 0 && item < videoEventList.size()) {
							VideoEvent videoEvent = videoEventList.get(item);
							return getToolTip(videoEvent);
						} else if (series == 1 && item < audioEventList.size()) {
							VideoEvent audioEvent = audioEventList.get(item);
							return getToolTip(audioEvent);
						} else if (series == 1 && isMuxed && item < videoEventList.size() || (series == 2 && item < videoEventList.size())) {
							VideoEvent event = videoEventList.get(item);
							return getPlaytimeProgressToolTip(event);
						}  else if (series == 2 && isMuxed && item < audioEventList.size() || (series == 3 && item < audioEventList.size())) {
							VideoEvent event = audioEventList.get(item);
							return getPlaytimeProgressToolTip(event);
						} else {
							return "";
						}
					} else {
						if (item < eventList.size()) {
							return getToolTip(eventList.get(item));
						} else {
							return "";
						}
					}
				}

				private String getToolTip(VideoEvent event) {
					StringBuffer tooltipValue = new StringBuffer();
					tooltipValue.append(String.format("%.0f,%s, %.2f,%.2f,%.3f,%.3f", (double) event.getSegmentID(),
							event.getQuality(), event.getDLTimeStamp(), event.getDLLastTimestamp(), event.getDuration(),
							downloadProgressMap.get(event)));
					String[] value = tooltipValue.toString().split(",");
					return (MessageFormat.format(
							ResourceBundleHelper.getDefaultBundle().getString("videotab.progress.tooltip"), value[0],
							value[1], value[2], value[3], value[4], value[5]));

				}
				
				private String getPlaytimeProgressToolTip(VideoEvent event) {
					StringBuffer tooltipValue = new StringBuffer();
					tooltipValue.append(String.format("%.0f,%s, %.2f,%.2f,%.3f,%.3f, %s,", (double) event.getSegmentID(),
							event.getQuality(), event.getPlayTime(), event.getPlayTimeEnd(), event.getDuration(),
							downloadProgressMap.get(event), event.getContentType()));
					String[] value = tooltipValue.toString().split(",");
					return (MessageFormat.format(
							ResourceBundleHelper.getDefaultBundle().getString("videotab.playTimeProgress.tooltip"), value[0],
							value[1], value[2], value[3], value[4], value[5], value[6]));

				}
			});
		}

		XYSeriesCollection collection = new XYSeriesCollection();

		addSeries(collection, videoDownloadSeries, audioDownloadSeries, downloadMockSeries, "Download_Progress");
		addSeries(collection, videoPlaytimeSeries, audioPlaytimeSeries, progressMockSeries, "PlayTime_Progress");
		
		plot.setDataset(collection);
	}

	

	private void addSeries(XYSeriesCollection collection, XYSeries videoSeries, XYSeries audioSeries,
			XYSeries mockSeries, String graphName) {
		if (!isMuxed) {
			if (optionSelected == SegmentOptions.DEFAULT) {
				collection.addSeries(videoSeries);
				collection.addSeries(audioSeries);
			} else {
				if (optionSelected == SegmentOptions.VIDEO) {
					collection.addSeries(videoSeries);
					collection.addSeries(mockSeries);
				} else {
					collection.addSeries(mockSeries);
					collection.addSeries(audioSeries);
				}
			}
		} else {
			collection.addSeries(createMuxedSeries(!videoSeries.isEmpty() ? videoSeries : audioSeries, graphName));
		}
	}
	

	private XYSeries createMuxedSeries(XYSeries eventSeries, String graphName) {
		XYSeries muxedSeries = new XYSeries(graphName);
		List<?> items = eventSeries.getItems();
		
		for (int i = 0; i < items.size(); i++) {
			XYDataItem dataItem = (XYDataItem) items.get(i);
			double dlTimeStamp = dataItem.getXValue();
			double progress = dataItem.getYValue();
			muxedSeries.add(dlTimeStamp, progress);
			progressList.add(progress);
			timestampList.add(dlTimeStamp);
		}
		if (!CollectionUtils.isEmpty(progressList) && !CollectionUtils.isEmpty(timestampList)) {
			minYValue = progressList.stream().findFirst().get();
			maxYValue = progressList.stream().reduce((first, second) -> second).get();
			minXValue = timestampList.stream().findFirst().get();
			maxXValue = timestampList.stream().reduce((first, second) -> second).get();
		}
		return muxedSeries;

	}

	public void calculateProgress(SegmentOptions option) {
		videoDownloadSeries.clear();
		audioDownloadSeries.clear();
		videoPlaytimeSeries.clear();
		audioPlaytimeSeries.clear();
		if (videoStream != null) {
			optionSelected = option;
			double dlTimeStamp = 0.0;
			if (option == SegmentOptions.DEFAULT || option == SegmentOptions.VIDEO) {
				for (Entry<String, VideoEvent> videoEventEntry : videoStream.getVideoStartTimeMap().entrySet()) {
					VideoEvent videoEvent = videoEventEntry.getValue();
					if (videoEvent.isSelected() && videoEvent.isNormalSegment()) {
						videoEvent.setOption(SegmentOptions.VIDEO.toString());
						videoEventList.add(videoEvent);
						eventList.add(videoEvent);
						double videoDownloadProgress = getProgress(videoStream.getVideoStartTimeMap(), videoEvent, true);
						progressList.add(videoDownloadProgress);
						dlTimeStamp = videoEvent.getDLTimeStamp();
						timestampList.add(dlTimeStamp);
						videoDownloadSeries.add(dlTimeStamp, videoDownloadProgress);
					}
				}
			}

			if (option == SegmentOptions.DEFAULT || option == SegmentOptions.AUDIO) {
				for (Entry<String, VideoEvent> audioEventEntry : videoStream.getAudioStartTimeMap().entrySet()) {
					VideoEvent audioEvent = audioEventEntry.getValue();
					if (audioEvent.isSelected() && audioEvent.isNormalSegment()) {
						audioEvent.setOption(SegmentOptions.AUDIO.toString());
						audioEventList.add(audioEvent);
						eventList.add(audioEvent);
						double audioDownloadProgress = getProgress(videoStream.getAudioStartTimeMap(), audioEvent, false);
						progressList.add(audioDownloadProgress);
						dlTimeStamp = audioEvent.getDLTimeStamp();
						timestampList.add(dlTimeStamp);
						audioDownloadSeries.add(dlTimeStamp, audioDownloadProgress);
					}
				}
			}


			if (optionSelected != SegmentOptions.AUDIO && optionSelected != SegmentOptions.VIDEO) {
				if (!videoDownloadSeries.isEmpty() && !audioDownloadSeries.isEmpty()) {
					isMuxed = false;
				} else if (videoDownloadSeries.isEmpty() || audioDownloadSeries.isEmpty()) {
					isMuxed = true;
				}
			}
			Collections.sort(progressList);
			Collections.sort(timestampList);
			if (!CollectionUtils.isEmpty(progressList) && !CollectionUtils.isEmpty(timestampList)) {
				minYValue = progressList.stream().findFirst().get();
				maxYValue = progressList.stream().reduce((first, second) -> second).get();
				minXValue = timestampList.stream().findFirst().get();
				maxXValue = timestampList.stream().reduce((first, second) -> second).get();
			}
		}
	}
	

	private double getProgress(SortedMap<String, VideoEvent> segmentMap, VideoEvent segment, boolean isVideo) {
		double downloadProgress = 0.0;
		double playtimeProgress = 0.0;
		for (VideoEvent event : segmentMap.values()) {
			double duration = event.getDuration();
			downloadProgress = downloadProgress + duration;
			if (event.isSelected() && event.isNormalSegment()) {
				playtimeProgress = playtimeProgress + duration;
			}
			if (segment.equals(event)) {
				break;
			}
		}
		downloadProgressMap.put(segment, downloadProgress);
		if (isVideo) {
			videoPlaytimeSeries.add(segment.getPlayTime(), playtimeProgress);
		} else {
			audioPlaytimeSeries.add(segment.getPlayTime(), playtimeProgress);
		}
		
		timestampList.add(segment.getPlayTime());
		progressList.add(downloadProgress);
		return downloadProgress;
	}
	
}