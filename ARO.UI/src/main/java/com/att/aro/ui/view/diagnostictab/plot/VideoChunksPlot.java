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
package com.att.aro.ui.view.diagnostictab.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.SortSelection;
import com.att.aro.core.videoanalysis.impl.VideoChunkPlotterImpl;
import com.att.aro.core.videoanalysis.impl.VideoEventComparator;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoChunksPlot implements IPlot {

	private static final Logger LOG = LogManager.getLogger(VideoChunksPlot.class);	
	private static final String VIDEOCHUNK_TOOLTIP = ResourceBundleHelper.getMessageString("videochunk.tooltip");
	private final Shape shape = new Rectangle2D.Double(0, 0, 50, 50);

	private XYSeriesCollection videoChunksData = new XYSeriesCollection();
	private List<XYSeriesCollection> startUpDelayCollection = new ArrayList<XYSeriesCollection>();
	private XYSeries series;
	private XYSeries seriesStartUpDelay;
	private List<BufferedImage> imgSeries;

	private BufferOccupancyPlot bufferOccupancyPlot = new BufferOccupancyPlot();
	private BufferInSecondsPlot bufferInSecondsPlot = new BufferInSecondsPlot();

	private XYPlot bufferOccupancyXYPlot;
	private XYPlot bufferTimeXYPlot;

	private List<VideoEvent> filteredChunks;
	private static List<VideoEvent> segmentsToBePlayed;
	private VideoChunkPlotterImpl videoChunkPlotter;
	private Map<Integer, Double> seriesDataSets;
	boolean isReDraw = false;

	public VideoChunksPlot() {
		filteredChunks = new ArrayList<>();
		segmentsToBePlayed = new ArrayList<>();
		videoChunkPlotter = (VideoChunkPlotterImpl) ContextAware.getAROConfigContext().getBean("videoChunkPlotterImpl", PlotHelperAbstract.class);
	}

	public VideoChunkPlotterImpl getVideoChunkPlotterReference() {
		return videoChunkPlotter;
	}

	/**
	 * Holds selected chunk and it's play time in SortedMap
	 */
	private TreeMap<VideoEvent, Double> chunkPlayTime = new TreeMap<>();

	// StartupDelay calculations
	public void refreshPlot(XYPlot plot, AROTraceData traceData, double startTime, VideoEvent selectedChunk) {
		chunkPlayTime.clear();
		chunkPlayTime.put(selectedChunk, startTime);

		videoChunkPlotter.setChunkPlayTimeList(chunkPlayTime);
		setChunkPlayBackTimeCollection(traceData);
		bufferInSecondsPlot.setChunkPlayTimeMap(chunkPlayTime);

		populate(plot, traceData);

		bufferInSecondsPlot.populate(bufferTimeXYPlot, traceData);
		bufferOccupancyPlot.populate(bufferOccupancyXYPlot, traceData);

		refreshVCPlot(plot, traceData);
	}

	private void setChunkPlayBackTimeCollection(AROTraceData traceData) {
		if (traceData != null && traceData.getAnalyzerResult().getStreamingVideoData() != null) {
			traceData.getAnalyzerResult().getStreamingVideoData().getStreamingVideoCompiled().setChunkPlayTimeList(chunkPlayTime);
		}
	}

	/**
	 * This method redraws the Video Chunk plot with updated values
	 * 
	 * @param isReDraw to clear the video buffer plots
	 * @param plot
	 * @param traceData
	 */
	public void refreshVCPlot(XYPlot plot, AROTraceData traceData) {
		isReDraw = true;
		populate(plot, traceData);
	}

	public void setBufferOccupancyPlot(XYPlot bufferOccupancyPlot) {
		this.bufferOccupancyXYPlot = bufferOccupancyPlot;
		newTraceData();
	}

	public void setBufferTimePlot(XYPlot bufferTimePlot) {
		this.bufferTimeXYPlot = bufferTimePlot;
	}

	private void newTraceData() {
		chunkPlayTime.clear();
	}

	public void setDelayVideoStream(double seconds, Collection<VideoStream> videoStreams) {
		for (VideoStream videoStream : videoStreams) {
			if (videoStream.isSelected() && !videoStream.getVideoEventMap().isEmpty()) {
				videoStream.getManifest().setDelay(seconds);
			}
		}
	}

	@Override
	public void populate(XYPlot plot, AROTraceData traceData) {
		if (traceData != null) {
			StreamingVideoData streamingVideoData = traceData.getAnalyzerResult().getStreamingVideoData();
			if (!isReDraw) {
				bufferOccupancyPlot.clearPlot(this.bufferOccupancyXYPlot);
				bufferInSecondsPlot.clearPlot(this.bufferTimeXYPlot);
				traceData.getAnalyzerResult().setBufferTimeResult(null);
				traceData.getAnalyzerResult().setBufferOccupancyResult(null);
			}

			videoChunksData.removeAllSeries();
			for (XYSeriesCollection seriesColl : startUpDelayCollection) {
				seriesColl.removeAllSeries();
			}

			startUpDelayCollection.clear();
			imgSeries = new ArrayList<BufferedImage>();

			// create the dataset...
			int index = 0;
			series = new XYSeries("Chunks");
			seriesDataSets = new TreeMap<>();
			seriesDataSets = videoChunkPlotter.populateDataSet(traceData.getAnalyzerResult().getStreamingVideoData());

			imgSeries = videoChunkPlotter.getImgSeries();
			filteredChunks = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments();
			segmentsToBePlayed.clear();
			for (VideoEvent ve : streamingVideoData.getStreamingVideoCompiled().getAllSegments()) {
				segmentsToBePlayed.add(ve);
			}
			for (double timeStamp : seriesDataSets.values()) {
				series.add(timeStamp, 0);
			}

			XYSeriesCollection playTimeStartSeries = new XYSeriesCollection();
			int first = 0;

			List<VideoEvent> chunkPlayBackTimeList = new ArrayList<VideoEvent>(chunkPlayTime.keySet());
			Collections.sort(chunkPlayBackTimeList, new VideoEventComparator(SortSelection.SEGMENT_ID));
			if (CollectionUtils.isNotEmpty(chunkPlayBackTimeList)) {
				VideoEvent ve = chunkPlayBackTimeList.get(0);

				seriesStartUpDelay = new XYSeries("StartUpDelay" + (index++));
				seriesStartUpDelay.add(ve.getDLTimeStamp(), 0);
				Double playTime = chunkPlayTime.get(ve);
				if (playTime != null) {
					seriesStartUpDelay.add((double) playTime, 0);
				}

				if (first == 0) {
					StreamingVideoData videoData = traceData.getAnalyzerResult().getStreamingVideoData();
					SortedMap<Double, VideoStream> videoEventList = videoData.getVideoStreamMap();
					Double segPlayTime = chunkPlayTime.get(ve);
					if (segPlayTime != null) {
						setDelayVideoStream((double) segPlayTime - ve.getEndTS(), videoEventList.values());
					}
				}
				playTimeStartSeries.addSeries(seriesStartUpDelay);

				startUpDelayCollection.add(playTimeStartSeries);
				first++;
			}

			for (VideoStream videoStream : streamingVideoData.getVideoStreams()) {
				if (videoStream.isSelected()) {
					for (VideoStall videoStall :videoStream.getVideoStallList()) {
						playTimeStartSeries = new XYSeriesCollection();
						seriesStartUpDelay = new XYSeries("StartUpDelay" + (index++));
						seriesStartUpDelay.add(videoStall.getStallStartTimestamp(), 0);
						seriesStartUpDelay.add(videoStall.getStallEndTimestamp(), 0);
						playTimeStartSeries.addSeries(seriesStartUpDelay);
						startUpDelayCollection.add(playTimeStartSeries);					}
				}
			}

			videoChunksData.addSeries(series);

			// Startup and stalls
			VideoChunkImageRenderer renderer = new VideoChunkImageRenderer();
			XYLineAndShapeRenderer rendererDelay = new XYLineAndShapeRenderer();
			
			for (int idx = 0; idx < startUpDelayCollection.size(); idx++) {
				rendererDelay.setSeriesStroke(idx, new BasicStroke(2.0f));
				rendererDelay.setSeriesPaint(idx, Color.RED);
			}

			renderer.setBaseToolTipGenerator(toolTipGenerator());
			renderer.setSeriesShape(0, shape);

			plot.setRenderer(index, renderer);
			for (int i = 0; i < startUpDelayCollection.size(); i++) {
				plot.setRenderer(i, rendererDelay);
			}
		}
		isReDraw = false;
		int seriesIndex = 0;
		for (XYSeriesCollection seriesColl : startUpDelayCollection) {
			plot.setDataset(seriesIndex, seriesColl);
			seriesIndex++;
		}

		plot.setDataset(seriesIndex, videoChunksData);
	}

	public XYToolTipGenerator toolTipGenerator() {
		XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator() {
			@Override
			public String generateToolTip(XYDataset dataset, int series, int item) {
				VideoEvent currentVEvent = segmentsToBePlayed.get(item);

				String toolTip = null;
				try {
					toolTip = (MessageFormat.format(VIDEOCHUNK_TOOLTIP
										, String.format("%.0f", currentVEvent.getSegmentID())
										, String.format("%.3f", currentVEvent.getStartTS())
										, String.format("%.3f", currentVEvent.getEndTS())
										, String.format("%.3f s", currentVEvent.getPlayTime())
										, String.format("%.2f", currentVEvent.getTotalBytes() / 1000)
									));
				} catch (Exception e) {
					LOG.error("ToolTipException:", e);
					toolTip = "";
				}
				return toolTip;
			}

		};
		return xyToolTipGenerator;
	}

	/**
	 * Validates if x & y data values represent the video chunk
	 */
	public boolean isDataItemPoint(double xDataValue, double yDataValue) {
		for (VideoEvent ve : getAllChunks()) {// filteredChunks
			if (ve.getDLTimeStamp() == xDataValue && yDataValue == 0)
				return true;
		}

		return false;

	}

	public Map<Integer, VideoEvent> getChunk(double xdataValue) {
		Map<Integer, VideoEvent> chunk = new HashMap<>();
		for (int index = 0; index < getAllChunks().size(); index++) {
			if (getAllChunks().get(index).getDLTimeStamp() == xdataValue) {
				chunk.put(index, getAllChunks().get(index));
				return chunk;
			}
		}
		return null;
	}

	public Map<Integer, VideoEvent> getSegmentToPlayLocation(VideoEvent ve) {
		Map<Integer, VideoEvent> chunk = new HashMap<>();
		for (int index = 0; index < filteredChunks.size(); index++) {
			if (filteredChunks.get(index).equals(ve)) {
				chunk.put(index, ve);
				return chunk;
			}
		}
		return null;
	}

	class VideoChunkImageRenderer extends StandardXYItemRenderer {
		private static final long serialVersionUID = 2689805190362715164L;

		public VideoChunkImageRenderer() {
			super(StandardXYItemRenderer.IMAGES, null);
		}

		@Override
		protected Image getImage(Plot plot, int series, int item, double x, double y) {
			BufferedImage chunkImage = imgSeries.get(item);

			return chunkImage;
		}
	}

	public BufferInSecondsPlot getBufferTimePlot() {
		return bufferInSecondsPlot;
	}

	public List<VideoEvent> getAllChunks() {
		return videoChunkPlotter.getStreamingVideoData().getStreamingVideoCompiled().getAllSegments();
	}

	public List<XYSeriesCollection> getStartUpDelayCollection() {
		return startUpDelayCollection;
	}

}
