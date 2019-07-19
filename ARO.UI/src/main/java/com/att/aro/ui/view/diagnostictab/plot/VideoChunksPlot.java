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
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.SortSelection;
import com.att.aro.core.videoanalysis.impl.VideoChunkPlotterImpl;
import com.att.aro.core.videoanalysis.impl.VideoEventComparator;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoCompiled;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class VideoChunksPlot implements IPlot{
	
	private static final String VIDEOCHUNK_TOOLTIP = ResourceBundleHelper.getMessageString("videochunk.tooltip");
	private final Shape shape  = new Rectangle2D.Double(0,0,50,50);

	private XYSeriesCollection videoChunksData = new XYSeriesCollection();
	private List<XYSeriesCollection> startUpDelayCollection = new ArrayList<XYSeriesCollection>();
    private XYSeries series;
    private XYSeries seriesStartUpDelay;

    private List<BufferedImage> imgSeries;

    private BufferOccupancyPlot boPlot = new BufferOccupancyPlot();
    private BufferInSecondsPlot boTimePlot = new BufferInSecondsPlot();
    
    private XYPlot bufferOccupancyPlot;
    private XYPlot bufferTimePlot;
    
	private List<VideoEvent> filteredChunks;
    private static List<VideoEvent> segmentsToBePlayed;
	VideoChunkPlotterImpl videoChunkPlotter;
	Map<Integer,Double> seriesDataSets; 
	boolean isReDraw = false;
	private Map<Integer,AbstractBestPracticeResult> removeList = new HashMap<>();

	public VideoChunksPlot(){
		filteredChunks = new ArrayList<>();
		segmentsToBePlayed  = new ArrayList<>();
		videoChunkPlotter = (VideoChunkPlotterImpl) ContextAware.getAROConfigContext().getBean("videoChunkPlotterImpl",PlotHelperAbstract.class);
	}

	public VideoChunkPlotterImpl getVideoChunkPlotterReference() {
		return videoChunkPlotter;
	}

	/**
	 * Holds selected chunk and it's play time in HashMap
	 */
    private SortedMap<VideoEvent,Double> chunkPlayTime = new TreeMap<>();

	public AROTraceData refreshPlot(XYPlot plot, AROTraceData analysis, double startTime, VideoEvent selectedChunk) {
		chunkPlayTime.clear();
		chunkPlayTime.put(selectedChunk, startTime);

		videoChunkPlotter.setChunkPlayTimeList(chunkPlayTime);
		setChunkPlayBackTimeCollection(analysis);
		boPlot.setChunkPlayTimeList(chunkPlayTime);
		boTimePlot.setChunkPlayTimeList(chunkPlayTime);

		populate(plot, analysis);
		AbstractBestPracticeResult startupDelayBPResult = videoChunkPlotter.refreshStartUpDelayBP(analysis);
		
		if (analysis.getAnalyzerResult().getStreamingVideoData().getStreamingVideoCompiled().getChunksBySegment().isEmpty()) {
			return refreshBPVideoResults(analysis, startupDelayBPResult, null, null);
		}
		
		boTimePlot.populate(bufferTimePlot, analysis);

		boPlot.populate(bufferOccupancyPlot, analysis);
		
		refreshVCPlot(plot, analysis);

		AbstractBestPracticeResult stallBPResult = null;
		AbstractBestPracticeResult bufferOccupancyBPResult = null;
		List<BestPracticeType> bpList = SettingsUtil.retrieveBestPractices();
		if (bpList.contains(BestPracticeType.VIDEO_STALL)) {
			stallBPResult = videoChunkPlotter.refreshVideoStallBP(analysis);
		}
		if (bpList.contains(BestPracticeType.BUFFER_OCCUPANCY)) {
			bufferOccupancyBPResult = videoChunkPlotter.refreshVideoBufferOccupancyBP(analysis);
		}
		return refreshBPVideoResults(analysis, startupDelayBPResult, stallBPResult, bufferOccupancyBPResult);		
	}

	private void setChunkPlayBackTimeCollection(AROTraceData analysis) {
		if(analysis != null && analysis.getAnalyzerResult().getStreamingVideoData() != null){
			StreamingVideoData videoData = analysis.getAnalyzerResult().getStreamingVideoData();
			videoData.getStreamingVideoCompiled().setChunkPlayTimeList(chunkPlayTime);
		}
	}

	/**
	 * This method redraws the Video Chunk plot with updated values
	 * @param isReDraw to clear the video buffer plots
	 * 
	 */
	public void refreshVCPlot(XYPlot plot, AROTraceData analysis) {
	    isReDraw = true;
		populate(plot, analysis);		
	}
    
	private void updateAbstractBestPracticeParameters(AbstractBestPracticeResult bp, AbstractBestPracticeResult videoBPResult, int index){
		bp.setAboutText(videoBPResult.getAboutText());
		bp.setDetailTitle(videoBPResult.getDetailTitle());
		bp.setLearnMoreUrl(videoBPResult.getLearnMoreUrl());
		bp.setOverviewTitle(videoBPResult.getOverviewTitle());
		bp.setResultText(videoBPResult.getResultText());
		bp.setResultType(videoBPResult.getResultType());
		removeList.put(index, bp);		
	}
	
    private AROTraceData refreshBPVideoResults(AROTraceData model,AbstractBestPracticeResult bpResult,AbstractBestPracticeResult stallBPResult,AbstractBestPracticeResult bufferOccupancyBPResult){
		removeList.clear();
    	AROTraceData trace = model;
    	for(AbstractBestPracticeResult bp:model.getBestPracticeResults()){
    		if(bp.getBestPracticeType() == BestPracticeType.STARTUP_DELAY){
    			updateAbstractBestPracticeParameters(bp,bpResult,model.getBestPracticeResults().indexOf(bp));	
    		}
    		else if(stallBPResult != null && bp.getBestPracticeType() == BestPracticeType.VIDEO_STALL){
    			updateAbstractBestPracticeParameters(bp,stallBPResult,model.getBestPracticeResults().indexOf(bp));	
    		}else if(bufferOccupancyBPResult != null && bp.getBestPracticeType() == BestPracticeType.BUFFER_OCCUPANCY){
    			updateAbstractBestPracticeParameters(bp,bufferOccupancyBPResult,model.getBestPracticeResults().indexOf(bp));	
    		}
    	}
		if (!removeList.isEmpty()) {
			for (int index : removeList.keySet()) {
				AbstractBestPracticeResult bp = removeList.get(index);
				trace.getBestPracticeResults().remove(bp);
				AbstractBestPracticeResult result = null;
				if (bp.getBestPracticeType() == BestPracticeType.STARTUP_DELAY) {
					result = bpResult;
				} else if (bp.getBestPracticeType() == BestPracticeType.VIDEO_STALL) {
					result = stallBPResult;
				} else if (bp.getBestPracticeType() == BestPracticeType.BUFFER_OCCUPANCY) {
					result = bufferOccupancyBPResult;
				}
				trace.getBestPracticeResults().add(index, result);
			}
		}
    	return trace;
	}
    
    public void setBufferOccupancyPlot(XYPlot bufferOccupancyPlot) {
    	this.bufferOccupancyPlot = bufferOccupancyPlot;
    	newTraceData();
    }
    public void setBufferTimePlot(XYPlot bufferTimePlot){
    	this.bufferTimePlot = bufferTimePlot;
    }
    
    private void newTraceData(){
    	chunkPlayTime.clear();
    }
    public void setDelayVideoStream(double seconds, Collection<VideoStream> videoStreams){
    	for (VideoStream videoStream : videoStreams) {
			if (videoStream.isSelected() && !videoStream.getVideoEventList().isEmpty()) { 
				videoStream.getManifest().setDelay(seconds);
			}
    	}
    }
    
	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if (analysis != null) {		
			StreamingVideoData streamingVideoData = analysis.getAnalyzerResult().getStreamingVideoData();
			if(!isReDraw) {
				boPlot.clearPlot(this.bufferOccupancyPlot);
				boTimePlot.clearPlot(this.bufferTimePlot);
				analysis.getAnalyzerResult().setBufferTimeResult(null);
				analysis.getAnalyzerResult().setBufferOccupancyResult(null);
			}
			
			videoChunksData.removeAllSeries();
			for(XYSeriesCollection seriesColl : startUpDelayCollection){
				seriesColl.removeAllSeries();
			}

			startUpDelayCollection.clear();
			imgSeries = new ArrayList<BufferedImage>();

			// create the dataset...
			int index = 0;

			series = new XYSeries("Chunks");
			seriesDataSets = new TreeMap<>();
			seriesDataSets = videoChunkPlotter.populateDataSet(analysis.getAnalyzerResult().getStreamingVideoData());

			imgSeries = videoChunkPlotter.getImgSeries();
			filteredChunks = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments();
			segmentsToBePlayed.clear();
			if(streamingVideoData.getStreamingVideoCompiled().getAllSegments() != null) {
				for(VideoEvent ve: streamingVideoData.getStreamingVideoCompiled().getAllSegments()){
					segmentsToBePlayed.add(ve);
				}
			}
			for(double timeStamp:seriesDataSets.values()){				
				series.add(timeStamp, 0);
			}
			
			XYSeriesCollection playTimeStartSeries;
			int first = 0;
			
			List<VideoEvent> chunkPlayBackTimeList = new ArrayList<VideoEvent>(chunkPlayTime.keySet());
			Collections.sort(chunkPlayBackTimeList, new VideoEventComparator(SortSelection.SEGMENT));
			for (VideoEvent ve : chunkPlayBackTimeList) {

				playTimeStartSeries = new XYSeriesCollection();

				seriesStartUpDelay= new XYSeries("StartUpDelay"+(index++));
				seriesStartUpDelay.add(ve.getDLTimeStamp(),0);
				Double playTime = chunkPlayTime.get(ve);
				if(playTime != null){
						seriesStartUpDelay.add((double)playTime,0);
				}
				
				
				if (first == 0) {	
					StreamingVideoData videoData = analysis.getAnalyzerResult().getStreamingVideoData();
					SortedMap<Double, VideoStream> videoEventList = videoData.getVideoStreamMap();
					Double segPlayTime = chunkPlayTime.get(ve);
					if(segPlayTime != null){
						setDelayVideoStream((double)segPlayTime-ve.getEndTS(), videoEventList.values());
					}
				}
				playTimeStartSeries.addSeries(seriesStartUpDelay);

				startUpDelayCollection.add(playTimeStartSeries);
				first++;
			}

					videoChunksData.addSeries(series);	
							
					VideoChunckImageRenderer renderer = new VideoChunckImageRenderer();	
					XYLineAndShapeRenderer rendererDelay = new XYLineAndShapeRenderer( );
					for (int i=0;i<startUpDelayCollection.size();i++){
						rendererDelay.setSeriesStroke(i , new BasicStroke(1.0f) );
						rendererDelay.setSeriesPaint(i, Color.red);
					}
										
					XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator()
					 {		
					     @Override
					     public String generateToolTip(XYDataset dataset, int series, int item)
					     {
								 StringBuffer tooltipValue = new StringBuffer(); 
								 VideoEvent currentVEvent =segmentsToBePlayed.get(item);
									
								 DecimalFormat decimalFormat = new DecimalFormat("0.##");
								 		 
								 tooltipValue.append(decimalFormat.format(currentVEvent.getSegmentID())+","+String.format("%.2f",currentVEvent.getStartTS())+","+String.format("%.2f",currentVEvent.getEndTS())+",");

								 if(!chunkPlayTime.isEmpty()){
									 if(videoChunkPlotter.getSegmentPlayStartTime(currentVEvent) == -1){ 
										 tooltipValue.append("- ,");
									 }else{
										 tooltipValue.append(String.format("%.2f", videoChunkPlotter.getSegmentPlayStartTime(currentVEvent))+" s,");
									 }
								 }else{
									 tooltipValue.append("- ,");
								 }
								 tooltipValue.append(currentVEvent.getTotalBytes()/1000); //Converting to KB
								 String[] value = tooltipValue.toString().split(",");
								 return (MessageFormat.format(VIDEOCHUNK_TOOLTIP,value[0],value[1],value[2],value[3],value[4] ));
								
					     }
			 
					 };

					renderer.setBaseToolTipGenerator(xyToolTipGenerator);
					renderer.setSeriesShape(0,shape);
					

				    plot.setRenderer(index,renderer);
				    for(int i=0;i<startUpDelayCollection.size();i++)
				    	plot.setRenderer(i,rendererDelay);
		       }
		isReDraw = false;
		int seriesIndex = 0;
		for (XYSeriesCollection seriesColl : startUpDelayCollection) {
			plot.setDataset(seriesIndex, seriesColl);
			seriesIndex++;
		}

		plot.setDataset(seriesIndex,videoChunksData);
  }

	
	/**
	 * Validates if x & y data values represent the first video chunk
	 */
	public boolean isFirstDataItemPoint(double xDataValue, double yDataValue) { 
		if(videoChunkPlotter.getFirstChunkTimestamp() == xDataValue && yDataValue==0){
			return true;
		}else
			return false;
		
	}
	
	/**
	 * Validates if x & y data values represent the video chunk
	 */
	public boolean isDataItemPoint(double xDataValue, double yDataValue) { 
		for(VideoEvent ve: getAllChunks()){//filteredChunks
			if(ve.getDLTimeStamp() == xDataValue && yDataValue ==0)
				return true;
		}
		
		return false;
		
	}
	
	public Map<Integer,VideoEvent> getChunk(double xdataValue){
		Map<Integer,VideoEvent> chunk = new HashMap<>();
		for(int index=0;index< getAllChunks().size();index++){
			if(getAllChunks().get(index).getDLTimeStamp() == xdataValue){
				chunk.put(index, getAllChunks().get(index));
				return chunk;
			}
		}
		return null;
	}
	public Map<Integer,VideoEvent> getSegmentToPlayLocation(VideoEvent ve){
		Map<Integer,VideoEvent> chunk = new HashMap<>();
		for(int index=0;index< filteredChunks.size();index++){ 
			if(filteredChunks.get(index).equals(ve)){
				chunk.put(index, ve);
				return chunk;
			}
		}
		return null;
	}

	class VideoChunckImageRenderer extends StandardXYItemRenderer{
		private static final long serialVersionUID = 2689805190362715164L;

		public VideoChunckImageRenderer()
		{
			super(StandardXYItemRenderer.IMAGES, null);
		}

		@Override
		protected Image getImage(Plot plot, int series, int item, double x, double y) {
			BufferedImage chunkImage = imgSeries.get(item);

			return chunkImage; 
		}
	}

	public  BufferInSecondsPlot getBufferTimePlot() {
		return boTimePlot;
	}
	
	public List<VideoEvent> getAllChunks(){
		StreamingVideoCompiled streamingVideoCompiled = videoChunkPlotter.getStreamingVideoData().getStreamingVideoCompiled();
		return streamingVideoCompiled.getAllSegments();
	}

	public List<XYSeriesCollection> getStartUpDelayCollection() {
		return startUpDelayCollection;
	}
 
}


