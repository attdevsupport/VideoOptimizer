/*
 *  Copyright 2020 AT&T
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

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.diagnostictab.plot.IPlot;

import lombok.Data;

@Data
public class VideoThroughputPlot implements IPlot {
	private static final Logger LOGGER = LogManager.getLogger(VideoThroughputPlot.class);
	private VideoStream videoStream;
	private int graphCounter = 0;

	private List<VideoEvent> videoEventList = new ArrayList<VideoEvent>();
	private List<VideoEvent> eventList = new ArrayList<VideoEvent>();
	private List<VideoEvent> audioEventList = new ArrayList<VideoEvent>();
	SortedMap<String, VideoEvent> videoEventMap = new TreeMap<String, VideoEvent>();
	private XYSeries videoEventSeries = new XYSeries(SegmentOptions.VIDEO.toString());
	private double maxYValue;
	private double maxXValue;
	private double minYValue;
	private XYSeries audioEventSeries = new XYSeries(SegmentOptions.AUDIO.toString());
	private SegmentOptions optionSelected;
	private double minXValue;
	private boolean isMuxed = false;

	public VideoThroughputPlot(VideoStream videoStream, SegmentOptions optionSelected) {
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
							getThroughput(event)));
					String[] value = tooltipValue.toString().split(",");
					return (MessageFormat.format(
							ResourceBundleHelper.getDefaultBundle().getString("videotab.throughput.tooltip"), value[0],
							value[1], value[2], value[3], value[4], value[5]));

				}
			});
		}

		XYSeriesCollection collection = new XYSeriesCollection();

		if (!isMuxed) {
			if (optionSelected == SegmentOptions.DEFAULT) {
				collection.addSeries(videoEventSeries);
				collection.addSeries(audioEventSeries);
			} else {
				collection.addSeries(optionSelected == SegmentOptions.VIDEO ? videoEventSeries : audioEventSeries);
			}
			plot.setDataset(collection);
		} else {
			collection.addSeries(createMuxedSeries(!videoEventSeries.isEmpty() ? videoEventSeries : audioEventSeries));
			plot.setDataset(collection);
		}

	}

	private XYSeries createMuxedSeries(XYSeries eventSeries) {
		XYSeries muxedSeries = new XYSeries(SegmentOptions.MUXED.toString());
		List<?> items = eventSeries.getItems();
		List<Double> throughPutList = new ArrayList<Double>();
		List<Double> timestampList = new ArrayList<Double>();
		for (int i = 0; i < items.size(); i++) {
			XYDataItem dataItem = (XYDataItem) items.get(i);
			double dlTimeStamp = dataItem.getXValue();
			double throughPut = dataItem.getYValue();
			muxedSeries.add(dlTimeStamp, throughPut);
			throughPutList.add(throughPut);
			timestampList.add(dlTimeStamp);
		}
		minYValue = throughPutList.stream().findFirst().get();
		maxYValue = throughPutList.stream().reduce((first, second) -> second).get();
		minXValue = timestampList.stream().findFirst().get();
		maxXValue = timestampList.stream().reduce((first, second) -> second).get();
		return muxedSeries;

	}

	private double getThroughput(VideoEvent videoEvent) {
		return (videoEvent.getTotalBytes() / 1000000) / videoEvent.getDLTime();
	}

	public void calculateThroughPut(SegmentOptions option) {
		videoEventSeries.clear();
		audioEventSeries.clear();
		if (videoStream != null) {
			List<Double> throughPutList = new ArrayList<Double>();
			List<Double> timestampList = new ArrayList<Double>();
			optionSelected = option;
			double dlTimeStamp = 0.0;
			if (option == SegmentOptions.DEFAULT || option == SegmentOptions.VIDEO) {
				for (Entry<String, VideoEvent> videoEventEntry : videoStream.getVideoEventMap().entrySet()) {
					VideoEvent videoEvent = videoEventEntry.getValue();
					videoEvent.setOption(SegmentOptions.VIDEO.toString());
					videoEventList.add(videoEvent);
					eventList.add(videoEvent);
					double throughPut = getThroughput(videoEvent);
					throughPutList.add(throughPut);
					dlTimeStamp = videoEvent.getDLTimeStamp();
					timestampList.add(dlTimeStamp);
					videoEventSeries.add(dlTimeStamp, throughPut);
				}
			}
			if (option == SegmentOptions.DEFAULT || option == SegmentOptions.AUDIO) {
				for (Entry<String, VideoEvent> videoEventEntry : videoStream.getAudioEventMap().entrySet()) {
					VideoEvent audioEvent = videoEventEntry.getValue();
					audioEvent.setOption(SegmentOptions.AUDIO.toString());
					audioEventList.add(audioEvent);
					eventList.add(audioEvent);
					double throughPut = getThroughput(audioEvent);
					throughPutList.add(throughPut);
					dlTimeStamp = audioEvent.getDLTimeStamp();
					timestampList.add(dlTimeStamp);
					audioEventSeries.add(dlTimeStamp, throughPut);
				}
			}
			
			if (optionSelected != SegmentOptions.AUDIO && optionSelected != SegmentOptions.VIDEO) {
				if (!videoEventSeries.isEmpty() && !audioEventSeries.isEmpty()) {
					isMuxed = false;
				} else if (videoEventSeries.isEmpty() || audioEventSeries.isEmpty()) {
					isMuxed = true;
				}
			}
			Collections.sort(throughPutList);
			minYValue = throughPutList.stream().findFirst().get();
			maxYValue = throughPutList.stream().reduce((first, second) -> second).get();
			minXValue = timestampList.stream().findFirst().get();
			maxXValue = timestampList.stream().reduce((first, second) -> second).get();
		}
	}
}