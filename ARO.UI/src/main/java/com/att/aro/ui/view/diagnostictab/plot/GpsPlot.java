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

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.att.aro.core.ILogger;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.GpsInfo;
import com.att.aro.core.peripheral.pojo.GpsInfo.GpsState;
import com.att.aro.core.peripheral.pojo.LocationEvent;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class GpsPlot implements IPlot{
	
	private ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);	
	XYIntervalSeriesCollection gpsData = new XYIntervalSeriesCollection();
	XYSeriesCollection locationData = new XYSeriesCollection();
	ArrayList<LocationEvent> listLocationEvent;
	private XYSeries series;
	private BufferedImage image;

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {
		if (analysis == null) {
			logger.info("analysis data is null");
			return;
		} 
			
		gpsData.removeAllSeries();
		locationData.removeAllSeries();
		
		TraceResultType resultType = analysis.getAnalyzerResult()
				.getTraceresult().getTraceResultType();
		if (resultType.equals(TraceResultType.TRACE_FILE)) {
			logger.info("didn't get analysis trace data!");

		} else {
		
			try {
				image = ImageIO.read(GpsPlot.class.getResourceAsStream("/images/location.png"));
				image = ImageHelper.resize(image, 12, 12);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			// create the GPS dataset...
			Map<GpsState, XYIntervalSeries> seriesMap = new EnumMap<GpsState, XYIntervalSeries>(
					GpsState.class);
			for (GpsState eventType : GpsState.values()) {
				XYIntervalSeries series = new XYIntervalSeries(eventType);
				seriesMap.put(eventType, series);
				gpsData.addSeries(series);
			}
			
			series = new XYSeries("location");
			TraceDirectoryResult traceresult = (TraceDirectoryResult) analysis.getAnalyzerResult().getTraceresult();
			listLocationEvent = (ArrayList<LocationEvent>) traceresult.getLocationEventInfos();
			for (int idx = 0; idx < listLocationEvent.size(); idx++) {
				series.add(listLocationEvent.get(idx).getTimeRecorded(), 0.5);
			}
			locationData.addSeries(series);
			
			Iterator<GpsInfo> iter = analysis.getAnalyzerResult().getTraceresult().getGpsInfos().iterator();
			if (iter.hasNext()) {
				while (iter.hasNext()) {
					GpsInfo gpsEvent = iter.next();
					if (gpsEvent.getGpsState() != GpsState.GPS_DISABLED) {
						seriesMap.get(gpsEvent.getGpsState())
								.add(gpsEvent.getBeginTimeStamp(), gpsEvent.getBeginTimeStamp(),
										gpsEvent.getEndTimeStamp(), 0.5, 0, 1);
					}
				}
			}

			XYItemRenderer renderer = plot.getRenderer(0);
			// 0 - is the default renderer from XYItem renderer.
			// Looks like renderer is using the index descending order, so setting the index of the GPS background as 2 & location information index as 1.
			if(renderer == null ){
				renderer = plot.getRenderer(2);
			}
			renderer.setSeriesPaint(gpsData.indexOf(GpsState.GPS_STANDBY), Color.YELLOW);
			renderer.setSeriesPaint(gpsData.indexOf(GpsState.GPS_ACTIVE), new Color(34, 177, 76));

			// Assign ToolTip to renderer
			renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
				@Override
				public String generateToolTip(XYDataset dataset, int series, int item) {
					GpsState eventType = (GpsState) gpsData.getSeries(series).getKey();
					return MessageFormat.format(ResourceBundleHelper.getMessageString("gps.tooltip"),
							dataset.getX(series, item),
							ResourceBundleHelper.getEnumString(eventType));
				}
			});
			plot.setRenderer(2,renderer);
			
			// Assign ToolTip to renderer
			LocationImageRenderer renderer_loc = new LocationImageRenderer();
			plot.setRenderer(1, renderer_loc);
			renderer_loc.setBaseToolTipGenerator(new XYToolTipGenerator() {
				@Override
				public String generateToolTip(XYDataset dataset, int series, int item) {
					// Update tooltip of location data
					LocationEvent event = listLocationEvent.get(item);
					StringBuffer displayInfo = new StringBuffer(
							ResourceBundleHelper.getMessageString("location.tooltip.prefix"));
					displayInfo.append(MessageFormat.format(
							ResourceBundleHelper.getMessageString("location.tooltip.content"),
							event.getTimeRecorded(), event.getLatitude(), event.getLongitude(), event.getProvider(), event.getLocality()));
					displayInfo.append(ResourceBundleHelper.getMessageString("location.tooltip.suffix"));

					return displayInfo.toString();
				}
			});
		}
		plot.setDataset(2, gpsData);
		plot.setDataset(1, locationData);
	}
	
	class LocationImageRenderer extends StandardXYItemRenderer{
		public LocationImageRenderer()
		{
			super(StandardXYItemRenderer.IMAGES, null);
		}
		@Override
		protected Image getImage(Plot plot, int series, int item, double x, double y) {
			return image;
		}
	}

}
