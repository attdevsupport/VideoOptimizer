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
import java.util.LinkedHashMap;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.PacketDirection;
import com.att.aro.core.pojo.AROTraceData;

public class PacketPlot implements IPlot{
	
	boolean isDownloadPacket = false;
	
	public boolean isDownloadPacket() {
		return isDownloadPacket;
	}

	public void setDownloadPacket(boolean isDownloadPacket) {
		this.isDownloadPacket = isDownloadPacket;
	}

	public void populate(XYPlot plot, AROTraceData analysis,boolean isDownload){		
		setDownloadPacket(isDownload);
		populate(plot, analysis);
	}

	@Override
	public void populate(XYPlot plot, AROTraceData analysis) {

		LinkedHashMap<Color, PacketSeries> dlDatasets = new LinkedHashMap<Color, PacketSeries>();

		AnalysisFilter filter = null;
		if (analysis != null) {
			filter = analysis.getAnalyzerResult().getFilter();
 			for(Session session :analysis.getAnalyzerResult().getSessionlist()){
 				addSeries(session,dlDatasets,filter );
			}
		}
		// Create the XY data set
		YIntervalSeriesCollection coll = new YIntervalSeriesCollection();
		XYItemRenderer renderer = plot.getRenderer();
		for (PacketSeries series : dlDatasets.values()) {
			coll.addSeries(series);
			renderer.setSeriesPaint(coll.indexOf(series.getKey()), series.getColor());
		}

		// Create tooltip generator
		renderer.setBaseToolTipGenerator(new PacketToolTipGenerator());

		plot.setDataset(coll);
	}
	
	private void addSeries(Session session,LinkedHashMap<Color, PacketSeries> dlDatasets,AnalysisFilter filter ){
		Session thisSession = session;
		
		for(PacketInfo packet : session.getAllPackets()){
			if (packet.getDir() == null) {
				continue;
			}			
			if(isDownloadPacket()&&packet.getDir()==PacketDirection.DOWNLINK){
				// Add the packet to the proper series based on color
				
				Color color = filter.getPacketColor(packet);
				PacketSeries series = dlDatasets.get(color);
				if (series == null) {
					series = new PacketSeries(color);
					dlDatasets.put(color, series);
				}
				series.add(new PacketDataItem(thisSession,packet));
			}else if(!isDownloadPacket()&&packet.getDir()==PacketDirection.UPLINK){
				// Add the packet to the proper series based on color
				
				Color color = filter.getPacketColor(packet);
				PacketSeries series = dlDatasets.get(color);
				if (series == null) {
					series = new PacketSeries(color);
					dlDatasets.put(color, series);
				}
				series.add(new PacketDataItem(thisSession,packet));
			}else{
				continue;
			}
		}
	}
 	/**
	 * Used to represent a series of packets which is all packets related to a
	 * single application.
	 */
	private class PacketSeries extends YIntervalSeries {
		private static final long serialVersionUID = 1L;

		private Color color;
		
		public PacketSeries(Color color) {
			super(color.getRGB(), false, true);
			this.color = color;
		}

		/**
		 * @return the color
		 */
		public Color getColor() {
			return color;
		}

		public void add(PacketDataItem item) {
			super.add(item, true);
		}

		/**
		 * @see org.jfree.data.xy.YIntervalSeries#getDataItem(int)
		 */
		@Override
		public PacketDataItem getDataItem(int index) {
			return (PacketDataItem) super.getDataItem(index);
		}

	}
	
	/**
	 * Tooltip generator for a hovered packet
	 */
	private class PacketToolTipGenerator implements XYToolTipGenerator {

		@Override
		public String generateToolTip(XYDataset dataset, int series, int item) {

			PacketSeries pSeries = (PacketSeries) ((YIntervalSeriesCollection) dataset)
					.getSeries(series);
			return pSeries.getDataItem(item).getTooltip();
		}

	}


}
