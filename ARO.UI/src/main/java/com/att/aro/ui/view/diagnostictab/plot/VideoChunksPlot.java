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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.VideoChunkPlotterImpl;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
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

	public VideoChunksPlot(){
		filteredChunks = new ArrayList<>();
		segmentsToBePlayed  = new ArrayList<>();
		videoChunkPlotter = (VideoChunkPlotterImpl) ContextAware.getAROConfigContext().getBean("videoChunkPlotterImpl",PlotHelperAbstract.class);
	}

	/**
	 * Holds selected chunk and it's play time in HashMap
	 */
    private Map<VideoEvent,Double> chunkPlayTime = new HashMap<>();

    public void refreshPlot(XYPlot plot, AROTraceData analysis, double startTime,VideoEvent selectedChunk){
    //	setFirstChunkPlayTime(startTime);
        chunkPlayTime.put(selectedChunk,startTime);

        videoChunkPlotter.setChunkPlayBackTimeList(chunkPlayTime);
    	boPlot.setChunkPlayTimeList(chunkPlayTime);
       // boTimePlot.setChunkPlayTimeList(chunkPlayTime);
        
    	populate(plot,analysis);
    	boTimePlot.populate(bufferTimePlot, analysis);

    	boPlot.populate(bufferOccupancyPlot, analysis); 
		
    	
    	AbstractBestPracticeResult startupDelayBPResult= videoChunkPlotter.refreshStartUpDelayBP(analysis);
    	AbstractBestPracticeResult stallBPResult=videoChunkPlotter.refreshVideoStallBP(analysis);
    	AbstractBestPracticeResult bufferOccupancyBPResult=videoChunkPlotter.refreshVideoBufferOccupancyBP(analysis);
    	
    	refreshBPVideoResults(analysis, startupDelayBPResult, stallBPResult,bufferOccupancyBPResult);
    }
    
    private void refreshBPVideoResults(AROTraceData model,AbstractBestPracticeResult bpResult,AbstractBestPracticeResult stallBPResult,AbstractBestPracticeResult bufferOccupancyBPResult){
    	for(AbstractBestPracticeResult bp:model.getBestPracticeResults()){
    		if(bp.getBestPracticeType() == BestPracticeType.STARTUP_DELAY){
    			bp.setAboutText(bpResult.getAboutText());
    			bp.setDetailTitle(bpResult.getDetailTitle());
    			bp.setLearnMoreUrl(bpResult.getLearnMoreUrl());
    			bp.setOverviewTitle(bpResult.getOverviewTitle());
    			bp.setResultText(bpResult.getResultText());
    			bp.setResultType(bpResult.getResultType());					
    		}
    		else if(bp.getBestPracticeType() == BestPracticeType.VIDEO_STALL){
    			bp.setAboutText(stallBPResult.getAboutText());
    			bp.setDetailTitle(stallBPResult.getDetailTitle());
    			bp.setLearnMoreUrl(stallBPResult.getLearnMoreUrl());
    			bp.setOverviewTitle(stallBPResult.getOverviewTitle());
    			bp.setResultText(stallBPResult.getResultText());
    			bp.setResultType(stallBPResult.getResultType());	
    		}else if(bp.getBestPracticeType() == BestPracticeType.BUFFER_OCCUPANCY){
    			bp.setAboutText(bufferOccupancyBPResult.getAboutText());
    			bp.setDetailTitle(bufferOccupancyBPResult.getDetailTitle());
    			bp.setLearnMoreUrl(bufferOccupancyBPResult.getLearnMoreUrl());
    			bp.setOverviewTitle(bufferOccupancyBPResult.getOverviewTitle());
    			bp.setResultText(bufferOccupancyBPResult.getResultText());
    			bp.setResultType(bufferOccupancyBPResult.getResultType());	
    		}
    	}
	}
    public void setBufferOccupancyPlot(XYPlot bufferOccupancyPlot){
    	this.bufferOccupancyPlot = bufferOccupancyPlot;
    	newTraceData();
    }
    public void setBufferTimePlot(XYPlot bufferTimePlot){
    	this.bufferTimePlot = bufferTimePlot;
    }
    
    private void newTraceData(){
    	chunkPlayTime.clear();
    }
    public void setDelayAROManifest(double seconds, Collection<AROManifest> aroManifests){
    	for (AROManifest aroManifest : aroManifests) {
			if (!aroManifest.getVideoEventList().isEmpty()) { 
				aroManifest.setDelay(seconds);
			}
    	}
    }
    
	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if (analysis != null) {		
			
			boPlot.clearPlot(this.bufferOccupancyPlot);
			boTimePlot.clearPlot(this.bufferTimePlot);
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
			seriesDataSets = videoChunkPlotter.populateDataSet(analysis.getAnalyzerResult().getVideoUsage());

			imgSeries = videoChunkPlotter.getImageSeries();
			filteredChunks = videoChunkPlotter.getFilteredSegments();
			segmentsToBePlayed.clear();
			for(VideoEvent ve: filteredChunks){
				segmentsToBePlayed.add(ve);
			}
			for(double timeStamp:seriesDataSets.values()){				
				series.add(timeStamp, 0);
			}
			
			XYSeriesCollection playTimeStartSeries;
			int first = 0;

			for (VideoEvent ve : chunkPlayTime.keySet()) {

				playTimeStartSeries = new XYSeriesCollection();

				seriesStartUpDelay= new XYSeries("StartUpDelay"+(index++));
				seriesStartUpDelay.add(ve.getDLTimeStamp(),0);
				seriesStartUpDelay.add((double)chunkPlayTime.get(ve),0);
				
				
				if(first==chunkPlayTime.keySet().size()-1){		
					VideoUsage videoUsage = analysis.getAnalyzerResult().getVideoUsage();
					TreeMap<Double, AROManifest> videoEventList = videoUsage.getAroManifestMap();
					
					setDelayAROManifest((double)chunkPlayTime.get(ve)-ve.getEndTS(), videoEventList.values());//Set start up delay in AROManifest getDLTimeStamp()

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
								 StringBuffer tooltipValue = new StringBuffer(); //"Video Chunk at: "+String.format("%.2f", xPt.doubleValue());
								 VideoEvent currentVEvent =segmentsToBePlayed.get(item); //getFilteredChunks().get(item); //filteredChunks.get(item);
									
								 DecimalFormat decimalFormat = new DecimalFormat("0.##");
								 		 
								 tooltipValue.append(decimalFormat.format(currentVEvent.getSegment())+","+String.format("%.2f",currentVEvent.getStartTS())+","+String.format("%.2f",currentVEvent.getEndTS())+",");

								 if(!chunkPlayTime.isEmpty()){
									 if(videoChunkPlotter.getChunkPlayStartTimeList().size() <= item){
										 tooltipValue.append("- ,");
									 }else{
										 tooltipValue.append(String.format("%.2f", videoChunkPlotter.getChunkPlayStartTimeList().get(item))+" s,");
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
		for(VideoEvent ve: filteredChunks){
			if(ve.getDLTimeStamp() == xDataValue && yDataValue ==0)
				return true;
		}
		
		return false;
		
	}
	
	public Map<Integer,VideoEvent> getChunk(double xdataValue){
		Map<Integer,VideoEvent> chunk = new HashMap<>();
		for(int index=0;index< filteredChunks.size();index++){
			if(filteredChunks.get(index).getDLTimeStamp() == xdataValue){
				chunk.put(index, filteredChunks.get(index));
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
		// TODO Auto-generated method stub
		return boTimePlot;
	}
	

}


