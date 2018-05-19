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
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.pojo.BufferTimeBPResult;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.BufferInSecondsCalculatorImpl;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class BufferInSecondsPlot implements IPlot{
	
	private static final String BUFFER_TIME_OCCUPANCY_TOOLTIP = ResourceBundleHelper.getMessageString("bufferTimeoccupancy.tooltip");
	
	XYSeriesCollection bufferFillDataCollection = new XYSeriesCollection();
	XYSeries seriesBufferFill;
	Map<Integer,String> seriesDataSets; 
	private Shape shape  = new Ellipse2D.Double(0,0,10,10);
	private List<Double> bufferTimeList = new ArrayList<>();

	BufferInSecondsCalculatorImpl bufferInSecondsCalculatorImpl= (BufferInSecondsCalculatorImpl) ContextAware.getAROConfigContext().getBean("bufferInSecondsCalculatorImpl",PlotHelperAbstract.class);
	
	Map<VideoEvent,Double> chunkPlayTimeList;
	
	public void setChunkPlayTimeList(Map<VideoEvent,Double> chunkPlayTime){
		this.chunkPlayTimeList = chunkPlayTime;
	}
	
	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if(analysis != null){
			VideoUsage videoUsage = analysis.getAnalyzerResult().getVideoUsage();
			bufferFillDataCollection.removeAllSeries();
			seriesBufferFill = new XYSeries("Buffer Against Play Time");
			seriesDataSets = new TreeMap<>();
			
			seriesDataSets = bufferInSecondsCalculatorImpl.populate(videoUsage,chunkPlayTimeList);
			//updating video stall result in packetAnalyzerResult
			analysis.getAnalyzerResult().setVideoStalls(bufferInSecondsCalculatorImpl.getVideoStallResult());
			analysis.getAnalyzerResult().setNearStalls(bufferInSecondsCalculatorImpl.getVideoNearStallResult());
			
			bufferTimeList.clear();
			double xCoordinate,yCoordinate;
			String ptCoordinate[] = new String[2]; // to hold x & y values
			if(!seriesDataSets.isEmpty()){

				for(int key :seriesDataSets.keySet()){
					ptCoordinate = seriesDataSets.get(key).trim().split(",");
					xCoordinate = Double.parseDouble(ptCoordinate[0]);
					yCoordinate = Double.parseDouble(ptCoordinate[1]);
					bufferTimeList.add(yCoordinate);
					
					seriesBufferFill.add(xCoordinate,yCoordinate);
				}			
			}
			
			Collections.sort(bufferTimeList);
			BufferTimeBPResult bufferTimeResult = bufferInSecondsCalculatorImpl.updateBufferTimeResult(bufferTimeList);
			analysis.getAnalyzerResult().setBufferTimeResult(bufferTimeResult);
			// populate collection
			bufferFillDataCollection.addSeries(seriesBufferFill);
			
			XYItemRenderer renderer = new StandardXYItemRenderer();
			renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
				
		
				@Override
				public String generateToolTip(XYDataset dataset, int series, int item) {

					// Tooltip value
					Number timestamp = dataset.getX(series, item);
					Number bufferTime = dataset.getY(series, item);
					StringBuffer tooltipValue = new StringBuffer();

					Map<Double, Long> segmentEndTimeMap = bufferInSecondsCalculatorImpl.getSegmentEndTimeMap();
					Map<Long, Double> segmentStartTimeMap = bufferInSecondsCalculatorImpl.getSegmentStartTimeMap();
					double firstSegmentNo = videoUsage.getChunksBySegmentNumber().get(0).getSegment();

					DecimalFormat decimalFormat = new DecimalFormat("0.##");
					if (segmentStartTimeMap == null || segmentStartTimeMap.isEmpty()) {
						return "-,-,-";
					}

					List<Long> segmentList = new ArrayList<Long>(segmentEndTimeMap.values());
					Collections.sort(segmentList);
					Long lastSegmentNo =segmentList.get(segmentList.size()-1);
					
					Long segmentNumber = 0L;
					boolean isSegmentPlaying = false;
					boolean startup = false;
					boolean endPlay = false;

					for (double segmentEndTime : segmentEndTimeMap.keySet()) {
						if (segmentEndTime > timestamp.doubleValue()) {
							segmentNumber = segmentEndTimeMap.get(segmentEndTime);
							if (segmentNumber == firstSegmentNo) {
								startup = true;
							}
							if (segmentStartTimeMap.get(segmentNumber) <= timestamp.doubleValue()) {
								tooltipValue.append(decimalFormat.format(segmentNumber) + ",");
								isSegmentPlaying = true;
								startup = false;
							}
						} else if (lastSegmentNo.equals(segmentEndTimeMap.get(segmentEndTime))
								&& segmentEndTime == timestamp.doubleValue()) {
							endPlay = true;
						}
					}

					if (endPlay || startup) {
						tooltipValue.append("-,");
					} else if (!isSegmentPlaying && !startup) {
						tooltipValue.append("Stall,");
					}
					
					tooltipValue.append(String.format("%.2f", bufferTime) + "," + String.format("%.2f", timestamp));
					
					String[] value = tooltipValue.toString().split(",");
					return (MessageFormat.format(BUFFER_TIME_OCCUPANCY_TOOLTIP, value[0], value[1], value[2]));
				}

			});
			renderer.setSeriesStroke(0, new BasicStroke(2.0f));
			renderer.setSeriesPaint(0, Color.MAGENTA);
			
			renderer.setSeriesShape(0, shape);
			
			plot.setRenderer(renderer);

		}
		plot.setDataset(bufferFillDataCollection);
	}


	public void clearPlot(XYPlot bufferTimePlot) {
		bufferTimePlot.setDataset(null);	
	}
	
	public VideoEvent isDataItemStallPoint(double xDataValue,double yDataValue){
		VideoEvent segmentToPlay=null;
		List<VideoStall> videoStallResults =bufferInSecondsCalculatorImpl.getVideoStallResult();
		if(videoStallResults != null && (!videoStallResults.isEmpty())){
			VideoStall stallPoint =videoStallResults.get(videoStallResults.size()-1);
			double lastDataSet_YValue = Double.parseDouble(seriesDataSets.get(seriesDataSets.keySet().size()-1).split(",")[1]);
			if((stallPoint.getStallEndTimeStamp()==0 || stallPoint.getStallStartTimeStamp()==stallPoint.getStallEndTimeStamp()) && stallPoint.getStallStartTimeStamp() == xDataValue &&  lastDataSet_YValue== yDataValue){
				segmentToPlay = stallPoint.getSegmentTryingToPlay();
			}
		}
		return segmentToPlay;
	}

}
